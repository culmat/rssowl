/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sorts the elements of the feed view based on the choices provided by the
 * user.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
public class NewsComparator extends ViewerComparator implements Comparator<INews> {
  private NewsColumn fSortBy;
  private boolean fAscending;

  /* A cache for the Location Column */
  private Map<Long, String> fMapBinIdToLocation = new HashMap<Long, String>();
  private Map<String, String> fMapFeedLinkToLocation = new HashMap<String, String>();

  /**
   * @return Returns the ascending.
   */
  public boolean isAscending() {
    return fAscending;
  }

  /**
   * @param ascending The ascending to set.
   */
  public void setAscending(boolean ascending) {
    fAscending = ascending;
  }

  /**
   * @return Returns the sortBy.
   */
  public NewsColumn getSortBy() {
    return fSortBy;
  }

  /**
   * @param sortBy The sortBy to set.
   */
  public void setSortBy(NewsColumn sortBy) {
    fSortBy = sortBy;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {

    /* Can only be an EntityGroup then */
    if (!(e1 instanceof INews) || !(e2 instanceof INews))
      return 0;

    /* Proceed comparing News */
    return compare((INews) e1, (INews) e2);
  }

  /*
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(INews news1, INews news2) {
    int result = 0;

    switch (fSortBy) {

      /* Sort by Date */
      case DATE:
        return compareByDate(news1, news2, false);

        /* Sort by Title */
      case TITLE:
        result = compareByTitle(CoreUtils.getHeadline(news1), CoreUtils.getHeadline(news2));
        break;

      /* Sort by Author */
      case AUTHOR:
        result = compareByAuthor(news1.getAuthor(), news2.getAuthor());
        break;

      /* Sort by Category */
      case CATEGORY:
        result = compareByCategory(news1.getCategories(), news2.getCategories());
        break;

      /* Sort by Stickyness */
      case STICKY:
        result = compareByStickyness(news1.isFlagged(), news2.isFlagged());
        break;

      /* Sort by Feed */
      case FEED:
        result = compareByFeed(news1.getFeedLinkAsText(), news2.getFeedLinkAsText());
        break;

      /* Sort by "Has Attachments" */
      case ATTACHMENTS:
        result = compareByHasAttachments(!news1.getAttachments().isEmpty(), !news2.getAttachments().isEmpty());
        break;

      /* Sort by Labels */
      case LABELS:
        result = compareByLabels(CoreUtils.getSortedLabels(news1), CoreUtils.getSortedLabels(news2));
        break;

      /* Sort by Status */
      case STATUS:
        result = compareByStatus(news1.getState(), news2.getState());
        break;

      /* Sort by Location */
      case LOCATION:
        result = compareByLocation(news1, news2);
        break;
    }

    /* Fall Back to default sort if result is 0 */
    if (result == 0)
      result = compareByDate(news1, news2, true);

    return result;
  }

  private int compareByFeed(String feedLink1, String feedLink2) {
    int result = feedLink1.compareTo(feedLink2);

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByDate(INews news1, INews news2, boolean forceDescending) {
    int result = 0;

    Date date1 = DateUtils.getRecentDate(news1);
    Date date2 = DateUtils.getRecentDate(news2);

    result = date1.compareTo(date2);

    /* Respect ascending / descending Order */
    return fAscending && !forceDescending ? result : result * -1;
  }

  private int compareByTitle(String title1, String title2) {
    int result = compareByString(title1, title2);

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByStatus(INews.State s1, INews.State s2) {
    int result = 0;

    if (s1 != s2) {
      if (s1 == State.NEW)
        result = -1;
      else if (s2 == State.NEW)
        result = 1;
      else if (s1 == State.UPDATED)
        result = -1;
      else if (s2 == State.UPDATED)
        result = 1;
      else if (s1 == State.UNREAD)
        result = -1;
      else if (s2 == State.UNREAD)
        result = 1;
      else
        result = s1.compareTo(s2);
    }

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByLocation(INews n1, INews n2) {
    int result = compareByString(getLocation(n1), getLocation(n2));

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private String getLocation(INews news) {

    /* Location: Bin */
    if (news.getParentId() > 0) {
      String location = fMapBinIdToLocation.get(news.getParentId());
      if (location == null) {
        NewsBinReference ref = new NewsBinReference(news.getParentId());
        INewsBin bin = ref.resolve();
        location = bin.getName();
        fMapBinIdToLocation.put(news.getParentId(), location);
      }

      return location;
    }

    /* Location: Bookmark */
    String location = fMapFeedLinkToLocation.get(news.getFeedLinkAsText());
    if (location == null) {
      IBookMark bookmark = CoreUtils.getBookMark(news.getFeedReference());
      if (bookmark != null) {
        location = bookmark.getName();
        fMapFeedLinkToLocation.put(news.getFeedLinkAsText(), location);
      }
    }

    return location;
  }

  private int compareByHasAttachments(boolean hasAttachments1, boolean hasAttachments2) {
    int result = 0;

    if (hasAttachments1 && !hasAttachments2)
      result = 1;

    else if (!hasAttachments1 && hasAttachments2)
      result = -1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByLabels(Set<ILabel> labels1, Set<ILabel> labels2) {
    int result = compareByString(toString(labels1), toString(labels2));

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private String toString(Set<ILabel> labels) {
    if (!labels.isEmpty())
      return labels.iterator().next().getName();

    return "";
  }

  private int compareByAuthor(IPerson author1, IPerson author2) {
    int result = 0;

    if (author1 != null && author2 != null) {
      String value1 = author1.getName();
      if (value1 == null && author1.getEmail() != null)
        value1 = author1.getEmail().toString();
      else if (value1 == null && author1.getUri() != null)
        value1 = author1.getUri().toString();

      String value2 = author2.getName();
      if (value2 == null && author2.getEmail() != null)
        value2 = author2.getEmail().toString();
      else if (value2 == null && author2.getUri() != null)
        value2 = author2.getUri().toString();

      result = compareByString(value1, value2);
    }

    else if (author1 != null)
      result = -1;

    else if (author2 != null)
      result = 1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByCategory(List<ICategory> categories1, List<ICategory> categories2) {
    int result = 0;

    if (categories1 != null && categories1.size() > 0 && categories2 != null && categories2.size() > 0) {
      ICategory category1 = categories1.get(0);
      ICategory category2 = categories2.get(0);

      String value1 = category1.getName();
      if (value1 == null)
        value1 = category1.getDomain();

      String value2 = category2.getName();
      if (value2 == null)
        value2 = category2.getName();

      result = compareByString(value1, value2);
    }

    else if (categories1 != null && categories1.size() > 0)
      result = -1;

    else if (categories2 != null && categories2.size() > 0)
      result = 1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByStickyness(boolean sticky1, boolean sticky2) {
    int result = 0;

    if (sticky1 && !sticky2)
      result = 1;

    else if (!sticky1 && sticky2)
      result = -1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByString(String str1, String str2) {
    if (str1 != null && str2 != null)
      return str1.compareTo(str2);
    else if (str1 != null)
      return -1;

    return 1;
  }
}