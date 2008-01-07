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

import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A service that provides access to cached entities. The service ensures to
 * always provide non-stale versions by listening to entity-events and updating
 * the cache accordingly.
 *
 * @author bpasero
 */
public class CacheService {
  private final IBookMarkDAO fBookMarkDAO;

  /**
   * Registers Listeners to cache the entities that are of interest for this
   * Service.
   */
  public CacheService() {
    fBookMarkDAO = DynamicDAO.getDAO(IBookMarkDAO.class);
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
}