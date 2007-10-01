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
  private final Map<IBookMark, Object> fBookMarks;
  private final Map<ISearchMark, Object> fSearchMarks;

  /* Dummy value to associate with an Object in the maps */
  private static final Object PRESENT = new Object();

  /* Listeners */
  private FolderListener fFolderListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkListener fSearchMarkListener;

  /**
   * Registers Listeners to cache the entities that are of interest for this
   * Service.
   */
  public CacheService() {
    fRootFolders = new ConcurrentHashMap<IFolder, Object>(16, LOAD_FACTOR, CONCURRENT_WRITERS);
    fBookMarks = new ConcurrentHashMap<IBookMark, Object>(32, LOAD_FACTOR, CONCURRENT_WRITERS);
    fSearchMarks = new ConcurrentHashMap<ISearchMark, Object>(16, LOAD_FACTOR, CONCURRENT_WRITERS);

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

    /* Listen to BookMark Events */
    fBookMarkListener = new BookMarkListener() {

      /* BookMarks got Added */
      public void entitiesAdded(Set<BookMarkEvent> events) {
        for (BookMarkEvent bookMarkEvent : events) {
          fBookMarks.put(bookMarkEvent.getEntity(), PRESENT);
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
          fSearchMarks.put(searchMarkEvent.getEntity(), PRESENT);
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

      /* Results Changed */
      public void resultsChanged(Set<SearchMarkEvent> events) {
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
    return fRootFolders.keySet();
  }

  /**
   * Returns a Set of all <code>IBookMark</code>s stored in the persistence
   * layer.
   *
   * @return a Set of all <code>IBookMark</code>s stored in the persistence
   * layer.
   */
  public Set<IBookMark> getBookMarks() {
    return fBookMarks.keySet();
  }

  /**
   * Returns a Set of all Links that are added as Bookmarks.
   *
   * @return Returns a Set of all Links that are added as Bookmarks.
   */
  public Set<String> getFeedLinks() {
    Set<String> links = new HashSet<String>(fBookMarks.size());

    for (IBookMark bookmark : getBookMarks()) {
      links.add(bookmark.getFeedLinkReference().getLink().toString());
    }

    return links;
  }

  /**
   * Returns a Set of all <code>ISearchMark</code>s stored in the persistence
   * layer.
   *
   * @return a Set of all <code>ISearchMark</code>s stored in the persistence
   * layer.
   */
  public Set<ISearchMark> getSearchMarks() {
    return fSearchMarks.keySet();
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

    for (IBookMark bookMark : getBookMarks()) {
      if (bookMark.getFeedLinkReference().equals(feedRef))
        bookmarks.add(bookMark);
    }

    return bookmarks;
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

    /* Cache Marks */
    List<IMark> subMarks = folder.getMarks();
    for (IMark subMark : subMarks) {
      if (subMark instanceof IBookMark)
        fBookMarks.put((IBookMark) subMark, PRESENT);
      else if (subMark instanceof ISearchMark)
        fSearchMarks.put((ISearchMark) subMark, PRESENT);
    }

    /* Recursively refresh cache of sub-folders */
    List<IFolder> subFolders = folder.getFolders();
    for (IFolder subFolder : subFolders)
      cache(subFolder);
  }
}