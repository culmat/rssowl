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

package org.rssowl.ui.internal;

import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.FolderListener;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service that provides access to cached entities. The service ensures to
 * always provide non-stale versions by listening to entity-events and updating
 * the cache accordingly.
 *
 * @author bpasero
 */
public class CacheService {
  private static final int CONCURRENT_WRITERS = 2;
  private static final float LOAD_FACTOR = 0.75f;
  private final Map<IFolder, Object> fRootFolders;

  /* Dummy value to associate with an Object in the maps */
  private static final Object PRESENT = new Object();

  /* Listeners */
  private FolderListener fFolderListener;

  private final IBookMarkDAO fBookMarkDAO;

  /**
   * Registers Listeners to cache the entities that are of interest for this
   * Service.
   */
  public CacheService() {
    fBookMarkDAO = DynamicDAO.getDAO(IBookMarkDAO.class);

    fRootFolders = new ConcurrentHashMap<IFolder, Object>(16, LOAD_FACTOR, CONCURRENT_WRITERS);

    registerListeners();
  }

  private void registerListeners() {

    /* Listen to Folder Events */
    fFolderListener = new FolderListener() {

      /* Folders got added */
      public void entitiesAdded(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          IFolder folder = folderEvent.getEntity();
          if (folder.getParent() == null)
            fRootFolders.put(folder, PRESENT);
        }
      }

      /* Folders got Deleted */
      public void entitiesDeleted(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          IFolder folder = folderEvent.getEntity();
          if (folder.getParent() == null)
            fRootFolders.remove(folder);
        }
      }

      /* Folders got Updated */
      public void entitiesUpdated(Set<FolderEvent> events) {
      /* Not handled */
      }
    };


    /* Add Listeners */
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);
  }

  void stopService() {
    unregisterListeners();
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
  }

  /**
   * Returns a Set of <code>IFolder</code>s, having no parent Folder set,
   * thereby being Root-Folders.
   *
   * @return a Set of all Root-Folders.
   */
  public Set<IFolder> getRootFolders() {
    return fRootFolders.keySet();
  }

  /**
   * Returns a Set of all Links that are added as Bookmarks.
   *
   * @return Returns a Set of all Links that are added as Bookmarks.
   */
  public Set<String> getFeedLinks() {
    Collection<IBookMark> bookMarks = fBookMarkDAO.loadAll();
    Set<String> links = new HashSet<String>(bookMarks.size());

    for (IBookMark bookmark : bookMarks) {
      links.add(bookmark.getFeedLinkReference().getLink().toString());
    }

    return links;
  }

  /**
   * Returns the first <code>IBookMark</code> that references the same feed as
   * <code>feedRef</code> or <code>null</code> if none.
   *
   * @param feedRef The desired Feed.
   * @return Returns the first <code>IBookMark</code> that references the
   * given Feed or <code>null</code> if none.
   */
  public IBookMark getBookMark(FeedLinkReference feedRef) {
    Collection<IBookMark> bookMarks = fBookMarkDAO.loadAll();
    for (IBookMark bookMark : bookMarks) {
      if (bookMark.getFeedLinkReference().equals(feedRef))
        return bookMark;
    }

    return null;
  }

  void cacheRootFolders() {
    Collection<IFolder> rootFolders = Owl.getPersistenceService().getDAOService().getFolderDAO().loadRoots();
    for (IFolder rootFolder : rootFolders) {
      cache(rootFolder);
    }
  }

  private void cache(IFolder folder) {

    /* Cache Root-Folders */
    if (folder.getParent() == null)
      fRootFolders.put(folder, PRESENT);

    /* Recursively refresh cache of sub-folders */
    List<IFolder> subFolders = folder.getFolders();
    for (IFolder subFolder : subFolders)
      cache(subFolder);
  }
}