/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2006 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.core.internal.persist.dao;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.FolderListener;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.ReparentInfo;

import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A data-access-object for <code>IFolder</code>s.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public final class FolderDAOImpl extends AbstractEntityDAO<IFolder, FolderListener, FolderEvent> implements IFolderDAO {

  /** Default constructor using the specific IPersistable for this DAO */
  public FolderDAOImpl() {
    super(Folder.class, false);
  }

  @Override
  protected final FolderEvent createDeleteEventTemplate(IFolder entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final FolderEvent createSaveEventTemplate(IFolder entity) {
    return new FolderEvent(entity, null, true);
  }

  //  public void deleteFolders(List<IFolder> folders) {
  //    fWriteLock.lock();
  //    try {
  //      for (IFolder folder : folders) {
  //        FolderEvent event = new FolderEvent(folder, null, true);
  //        DBHelper.putEventTemplate(event);
  //      }
  //      for (IFolder folder : folders)
  //        fDb.delete(folder);
  //
  //      fDb.commit();
  //    } catch (Db4oException e) {
  //      throw new PersistenceException(e);
  //    } finally {
  //      fWriteLock.unlock();
  //    }
  //    DBHelper.cleanUpAndFireEvents();
  //  }

  public Collection<IFolder> loadRoots() {
    try {
      Query query = fDb.query();
      query.constrain(fEntityClass);
      query.descend("fParent").constrain(null); //$NON-NLS-1$
      ObjectSet<IFolder> folders = getObjectSet(query);
      activateAll(folders);
      return new ArrayList<IFolder>(folders);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public final void reparent(List<ReparentInfo<IFolder, IFolder>> folderInfos, List<ReparentInfo<IMark, IFolder>> markInfos) {

    Assert.isLegal(folderInfos != null || markInfos != null, "Either folderInfos or markInfos must be non-null"); //$NON-NLS-1$
    fWriteLock.lock();
    try {
      List<FolderEvent> folderEvents = createFolderEvents(folderInfos);

      List<BookMarkEvent> bookMarkEvents = Collections.emptyList();
      List<SearchMarkEvent> searchMarkEvents = Collections.emptyList();
      if (markInfos != null) {
        bookMarkEvents = new ArrayList<BookMarkEvent>(markInfos.size() * (2 / 3));
        searchMarkEvents = new ArrayList<SearchMarkEvent>(markInfos.size() / 3);
        fillMarkEvents(markInfos, bookMarkEvents, searchMarkEvents);
      }

      for (FolderEvent event : folderEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getParent();
        if (newParent == null)
          fDb.set(event.getEntity());
        else
          fDb.set(newParent);
      }

      for (BookMarkEvent event : bookMarkEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getFolder();
        fDb.set(newParent);
      }

      for (SearchMarkEvent event : searchMarkEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getFolder();
        fDb.set(newParent);
      }

      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();

  }

  private List<FolderEvent> createFolderEvents(List<ReparentInfo<IFolder, IFolder>> folderInfos) {
    if (folderInfos == null)
      return Collections.emptyList();

    List<FolderEvent> folderEvents = new ArrayList<FolderEvent>(folderInfos.size());
    for (ReparentInfo<IFolder, IFolder> folderInfo : folderInfos) {
      IFolder folder = folderInfo.getObject();
      IFolder newParent = folderInfo.getNewParent();
      IFolder oldParent = folder.getParent();
      IFolder newPosition = folderInfo.getNewPosition();
      synchronized (folder) {
        removeFolderFromParent(folder);
        addFolder(newParent, folder);
        if (newPosition != null) {
          List<IFolder> folderList = new ArrayList<IFolder>(1);
          folderList.add(folder);
          newParent.reorderFolders(folderList, newPosition, folderInfo.isAfter().booleanValue());
        }
      }
      FolderEvent eventTemplate = new FolderEvent(folder, oldParent, true);
      folderEvents.add(eventTemplate);
      DBHelper.putEventTemplate(eventTemplate);
    }
    return folderEvents;
  }

  private void addFolder(IFolder parent, IFolder child) {
    child.setParent(parent);
    /* The new parent may be null. It becomes a root folder */
    if (parent != null)
      parent.addFolder(child);
  }

  private IFolder removeFolderFromParent(IFolder folder) {
    IFolder oldParent = folder.getParent();
    oldParent.removeFolder(folder);
    return oldParent;
  }

  private IFolder removeMarkFromParent(IMark mark) {
    IFolder oldParent = mark.getFolder();
    oldParent.removeMark(mark);
    return oldParent;
  }

  private void addMarkToFolder(IFolder parent, IMark child) {
    child.setFolder(parent);
    parent.addMark(child);
  }

  private void fillMarkEvents(List<ReparentInfo<IMark, IFolder>> markInfos, List<BookMarkEvent> bookMarkEvents, List<SearchMarkEvent> searchMarkEvents) {
    for (ReparentInfo<IMark, IFolder> markInfo : markInfos) {
      IMark mark = markInfo.getObject();
      IFolder newParent = markInfo.getNewParent();
      IFolder oldParent = mark.getFolder();
      IMark newPosition = markInfo.getNewPosition();
      synchronized (mark) {
        removeMarkFromParent(mark);
        addMarkToFolder(newParent, mark);
        if (newPosition != null) {
          List<IMark> markList = new ArrayList<IMark>(1);
          markList.add(mark);
          newParent.reorderMarks(markList, newPosition, markInfo.isAfter().booleanValue());
        }
      }
      if (mark instanceof IBookMark) {
        BookMarkEvent event = new BookMarkEvent((IBookMark) mark, oldParent, true);
        bookMarkEvents.add(event);
        DBHelper.putEventTemplate(event);
      } else if (mark instanceof ISearchMark) {
        SearchMarkEvent event = new SearchMarkEvent((ISearchMark) mark, oldParent, true);
        searchMarkEvents.add(event);
        DBHelper.putEventTemplate(event);
      } else
        throw new IllegalArgumentException("Uknown mark subclass found: " + mark.getClass()); //$NON-NLS-1$

    }
  }
}