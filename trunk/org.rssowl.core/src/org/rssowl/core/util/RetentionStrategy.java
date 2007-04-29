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

package org.rssowl.core.util;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.service.PersistenceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * This class is a helper to perform a clean-up of News based on properties set
 * for the related BookMark. Clean-Up may occur by any of number, age and state
 * of News.
 *
 * @author bpasero
 */
public class RetentionStrategy {

  /* One Day in millis */
  private static final long DAY = 24 * 60 * 60 * 1000;

  /**
   * Runs the Retention on the given <code>IFolder</code>.
   *
   * @param folder The <code>IFolder</code> to run the Retention on.
   */
  public static void process(IFolder folder) {
    List<INews> newsToDelete = new ArrayList<INews>();
    internalProcess(folder, newsToDelete);

    /* Perform Deletion */
    if (newsToDelete.size() > 0)
      Owl.getPersistenceService().getDAOService().getNewsDAO().setState(newsToDelete, INews.State.DELETED, false, false);
  }

  private static void internalProcess(IFolder folder, List<INews> newsToDelete) throws PersistenceException {

    /* BookMarks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) mark;
        List<INews> visibleNews = bookmark.getFeedLinkReference().resolve().getVisibleNews();
        newsToDelete.addAll(getNewsToDelete(bookmark, visibleNews));
      }
    }

    /* Recursively go through Child Folders */
    List<IFolder> childFolders = folder.getFolders();
    for (IFolder childFolder : childFolders)
      internalProcess(childFolder, newsToDelete);
  }

  /**
   * Runs the Retention on the given <code>IBookMark</code>.
   *
   * @param bookmark The <code>IBookMark</code> to run the Retention on.
   */
  public static void process(IBookMark bookmark) {
    IFeed feed = bookmark.getFeedLinkReference().resolve();
    if (feed != null)
      process(bookmark, feed.getVisibleNews());
  }

  /**
   * Runs the Retention on the given <code>IBookMark</code>. The second
   * argument speeds up this method, since it provides all the
   * <code>INews</code> belonging to the Feed the Bookmark is referencing.
   *
   * @param bookmark The <code>IBookMark</code> to run the Retention on.
   * @param news A List of <code>INews</code> belonging to the Feed the
   * Bookmark is referencing.
   * @return Returns a List of News that have been deleted due to the Retention
   * Processing.
   */
  public static List<INews> process(IBookMark bookmark, Collection<INews> news) {
    List<INews> newsToDelete = getNewsToDelete(bookmark, news);

    /* Perform Deletion */
    if (newsToDelete.size() > 0)
      Owl.getPersistenceService().getDAOService().getNewsDAO().setState(newsToDelete, INews.State.DELETED, false, false);

    return newsToDelete;
  }

  /**
   * Runs the Retention on the given <code>IBookMark</code> and
   * <code>IFeed</code>. The third parameter indicates the number of added
   * News in the Feed. That is, those News that the user has not yet seen. The
   * Retention will not remove those to give the user a chance to read them.
   * This is important for Feeds that serve more News than the retention
   * strategy is set to keep.
   *
   * @param bookmark The <code>IBookMark</code> to run the Retention on.
   * @param feed The <code>IFeed</code> to run the Retention on.
   * @param addedNewsCount The number of added News. The Retention will not
   * remove the added News as part of its work to avoid removing News that the
   * user has never seen.
   * @return Returns a List of <code>INews</code> whose state has been changed
   * to DELETED during the process. It's important that the caller persists
   * these changes to the persistence layer.
   */
  public static List<INews> process(IBookMark bookmark, IFeed feed, int addedNewsCount) {
    List<INews> newsToDelete = getNewsToDelete(bookmark, feed.getVisibleNews(), addedNewsCount);

    for (INews news : newsToDelete)
      news.setState(INews.State.DELETED);

    return newsToDelete;
  }

  private static List<INews> getNewsToDelete(IBookMark bookmark, Collection<INews> targetNews) {
    return getNewsToDelete(bookmark, targetNews, -1);
  }

  private static List<INews> getNewsToDelete(IBookMark bookmark, Collection<INews> targetNews, int minCountToKeep) {
    List<INews> newsToDelete = new ArrayList<INews>();
    IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(bookmark);

    /* Delete Read News if set */
    if (prefs.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE))
      fillReadNewsToDelete(targetNews, newsToDelete);

    /* Delete by Age if set */
    if (prefs.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE))
      fillNewsToDeleteByAge(targetNews, newsToDelete, prefs.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE));

    /* Delete by Count if set */
    if (prefs.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE))
      fillNewsToDeleteByCount(targetNews, newsToDelete, Math.max(minCountToKeep, prefs.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE)));

    return newsToDelete;
  }

  private static void fillReadNewsToDelete(Collection<INews> targetNews, List<INews> newsToDelete) {
    for (INews newsItem : targetNews) {
      if (!newsItem.isFlagged() && newsItem.getState() == INews.State.READ)
        newsToDelete.add(newsItem);
    }
  }

  private static void fillNewsToDeleteByAge(Collection<INews> targetNews, List<INews> newsToDelete, int days) {
    long maxAge = DateUtils.getToday().getTimeInMillis() - (days * DAY);
    for (INews newsItem : targetNews) {
      if (!newsItem.isFlagged() && !newsToDelete.contains(newsItem) && DateUtils.getRecentDate(newsItem).getTime() <= maxAge)
        newsToDelete.add(newsItem);
    }
  }

  private static void fillNewsToDeleteByCount(Collection<INews> targetNews, List<INews> newsToDelete, int limit) {

    /* Ignore News that are in List of Deleted News already */
    int actualSize = targetNews.size() - newsToDelete.size();

    /* First check if this rule applies at all */
    if (actualSize <= limit)
      return;

    /* Fill actual items into Array */
    INews newsArray[] = new INews[actualSize];
    int i = 0;
    for (INews news : targetNews) {
      if (!newsToDelete.contains(news)) {
        newsArray[i] = news;
        i++;
      }
    }

    /* Sort by Date */
    Arrays.sort(newsArray, new Comparator<INews>() {
      public int compare(INews news1, INews news2) {
        return DateUtils.getRecentDate(news1).compareTo(DateUtils.getRecentDate(news2));
      }
    });

    /* Delete oldest elements that exceed limit and are not sticky */
    int toDeleteValue = actualSize - limit;
    int deletedCounter = 0;
    for (i = 0; i < newsArray.length && deletedCounter != toDeleteValue; i++) {
      if (!newsArray[i].isFlagged()) {
        newsToDelete.add(newsArray[i]);
        deletedCounter++;
      }
    }
  }
}