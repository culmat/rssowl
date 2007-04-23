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
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.FolderListener;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.event.SearchMarkListener;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A service that provides access to cached entities. The service ensures to
 * always provide non-stale versions by listening to entity-events and updating
 * the cache accordingly.
 *
 * @author bpasero
 */
public class CacheService {
  private Set<IFolder> fRootFolders;
  private Set<IBookMark> fBookMarks;
  private Set<ISearchMark> fSearchMarks;

  /* Listeners */
  private FolderListener fFolderListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkListener fSearchMarkListener;

  CacheService() {
    fRootFolders = Collections.synchronizedSet(new HashSet<IFolder>());
    fBookMarks = Collections.synchronizedSet(new HashSet<IBookMark>());
    fSearchMarks = Collections.synchronizedSet(new HashSet<ISearchMark>());

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
            fRootFolders.add(folder);
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

    /* Listen to BookMark Events */
    fBookMarkListener = new BookMarkListener() {

      /* BookMarks got Added */
      public void entitiesAdded(Set<BookMarkEvent> events) {
        for (BookMarkEvent bookMarkEvent : events) {
          fBookMarks.add(bookMarkEvent.getEntity());
        }
      }

      /* Bookmarks got Deleted */
      public void entitiesDeleted(Set<BookMarkEvent> events) {
        for (BookMarkEvent bookMarkEvent : events) {
          fBookMarks.remove(bookMarkEvent.getEntity());
        }
      }

      /* Bookmarks got Updated */
      public void entitiesUpdated(Set<BookMarkEvent> events) {
      /* Not handled */
      }
    };

    /* Listen to SearchMark Events */
    fSearchMarkListener = new SearchMarkListener() {

      /* SearchMark got Added */
      public void entitiesAdded(Set<SearchMarkEvent> events) {
        for (SearchMarkEvent searchMarkEvent : events) {
          fSearchMarks.add(searchMarkEvent.getEntity());
        }
      }

      /* SearchMark got Deleted */
      public void entitiesDeleted(Set<SearchMarkEvent> events) {
        for (SearchMarkEvent searchMarkEvent : events) {
          fSearchMarks.remove(searchMarkEvent.getEntity());
        }
      }

      /* SearchMark got Updated */
      public void entitiesUpdated(Set<SearchMarkEvent> events) {
      /* Not handled */
      }
    };

    /* Add Listeners */
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);
  }

  void stopService() {
    unregisterListeners();
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
  }

  /**
   * Returns a Set of <code>IFolder</code>s, having no parent Folder set,
   * thereby being Root-Folders.
   *
   * @return a Set of all Root-Folders.
   */
  public Set<IFolder> getRootFolders() {
    return Collections.unmodifiableSet(new HashSet<IFolder>(fRootFolders));
  }

  /**
   * Returns an unmodifiable Set of all <code>IBookMark</code>s stored in the
   * persistence layer.
   *
   * @return an unmodifiable Set of all <code>IBookMark</code>s stored in the
   * persistence layer.
   */
  public Set<IBookMark> getBookMarks() {
    return Collections.unmodifiableSet(new HashSet<IBookMark>(fBookMarks));
  }

  /**
   * Returns an unmodifiable Set of all <code>ISearchMark</code>s stored in
   * the persistence layer.
   *
   * @return an unmodifiable Set of all <code>ISearchMark</code>s stored in
   * the persistence layer.
   */
  public Set<ISearchMark> getSearchMarks() {
    return Collections.unmodifiableSet(new HashSet<ISearchMark>(fSearchMarks));
  }

  /**
   * Returns a Set of <code>IBookMark</code>s that reference the same feed as
   * <code>feedRef</code>.
   *
   * @param feedRef The desired Feed.
   * @return Returns a List of <code>IBookMark</code>s that reference the
   * given Feed.
   */
  public Set<IBookMark> getBookMarks(FeedLinkReference feedRef) {
    Set<IBookMark> bookmarks = new HashSet<IBookMark>();

    synchronized (this) {
      for (IBookMark bookMark : fBookMarks) {
        if (bookMark.getFeedLinkReference().equals(feedRef))
          bookmarks.add(bookMark);
      }
    }

    return bookmarks;
  }

  void cacheRootFolders() {
    Collection<IFolder> rootFolders = Owl.getPersistenceService().getDAOService().getFolderDAO().loadRoots();
    for (IFolder rootFolder : rootFolders) {
      cache(rootFolder);
    }
  }

  private synchronized void cache(IFolder folder) {

    /* Cache Root-Folders */
    if (folder.getParent() == null)
      fRootFolders.add(folder);

    /* Cache Marks */
    List<IMark> subMarks = folder.getMarks();
    for (IMark subMark : subMarks) {
      if (subMark instanceof IBookMark)
        fBookMarks.add((IBookMark) subMark);
      else if (subMark instanceof ISearchMark)
        fSearchMarks.add((ISearchMark) subMark);
    }

    /* Recursively refresh cache of sub-folders */
    List<IFolder> subFolders = folder.getFolders();
    for (IFolder subFolder : subFolders)
      cache(subFolder);
  }
}