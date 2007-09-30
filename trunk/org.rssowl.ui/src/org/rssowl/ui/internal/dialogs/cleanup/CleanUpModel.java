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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.SearchHit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Creates the collection of <code>CleanUpTask</code> that the user may choose
 * to perform as clean up.
 *
 * @author bpasero
 */
class CleanUpModel {

  /* One Day in millis */
  private static final long DAY = 24 * 60 * 60 * 1000;

  private List<CleanUpTask> fTasks;
  private final CleanUpOperations fOps;
  private final Collection<IBookMark> fBookmarks;
  private IModelFactory fFactory;
  private IModelSearch fModelSearch;
  private ISearchField fLocationField;
  private ISearchField fAgeInDaysField;

  CleanUpModel(CleanUpOperations operations, Collection<IBookMark> bookmarks) {
    fOps = operations;
    fBookmarks = bookmarks;
    fTasks = new ArrayList<CleanUpTask>();
    fFactory = Owl.getModelFactory();
    fModelSearch = Owl.getPersistenceService().getModelSearch();

    String newsName = INews.class.getName();
    fLocationField = fFactory.createSearchField(INews.LOCATION, newsName);
    fAgeInDaysField = fFactory.createSearchField(INews.AGE_IN_DAYS, newsName);
  }

  /* Returns the Tasks */
  List<CleanUpTask> getTasks() {
    return fTasks;
  }

  private ISearchCondition getLocationCondition(IBookMark mark) {
    Long[][] value = new Long[2][1];
    value[1][0] = mark.getId();
    return fFactory.createSearchCondition(fLocationField, SearchSpecifier.IS, value);
  }

  void generate() {
    String name = INews.class.getName();
    Set<IBookMark> bookmarksToDelete = new HashSet<IBookMark>();
    Map<IBookMark, Set<NewsReference>> newsToDelete = new HashMap<IBookMark, Set<NewsReference>>();

    /* 1.) Delete BookMarks that have Last Visit > X Days ago */
    if (fOps.deleteFeedByLastVisit()) {
      int days = fOps.getLastVisitDays();
      long maxLastVisitDate = DateUtils.getToday().getTimeInMillis() - (days * DAY);

      for (IBookMark mark : fBookmarks) {
        Date date = mark.getLastVisitDate();

        /* Use Creation Date if mark was never visited */
        if (date == null)
          date = mark.getCreationDate();

        if (date == null || date.getTime() <= maxLastVisitDate)
          bookmarksToDelete.add(mark);
      }
    }

    /* 2.) Delete BookMarks that have not updated in X Days */
    if (fOps.deleteFeedByLastUpdate()) {
      ISearchField stateField = fFactory.createSearchField(INews.STATE, name);
      EnumSet<State> visibleStates = EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED, INews.State.READ);
      ISearchCondition stateCondition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, visibleStates);
      ISearchCondition ageCond = fFactory.createSearchCondition(fAgeInDaysField, SearchSpecifier.IS_LESS_THAN, fOps.getLastUpdateDays());

      /* For each selected Bookmark */
      for (IBookMark mark : fBookmarks) {

        /* Ignore if Bookmark gets already deleted */
        if (bookmarksToDelete.contains(mark))
          continue;

        List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(3);
        conditions.add(getLocationCondition(mark));
        conditions.add(ageCond);
        conditions.add(stateCondition);

        List<SearchHit<NewsReference>> results = fModelSearch.searchNews(conditions, true);
        if (results.isEmpty())
          bookmarksToDelete.add(mark);
      }
    }

    /* 3.) Delete BookMarks that have Connection Error */
    if (fOps.deleteFeedsByConError()) {
      for (IBookMark mark : fBookmarks) {
        if (!bookmarksToDelete.contains(mark) && mark.isErrorLoading())
          bookmarksToDelete.add(mark);
      }
    }

    /* Reusable State Condition */
    EnumSet<State> states = fOps.keepUnreadNews() ? EnumSet.of(INews.State.READ) : EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED, INews.State.READ);
    ISearchField stateField = fFactory.createSearchField(INews.STATE, name);
    ISearchCondition stateCondition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, states);

    /* Reusable Sticky Condition */
    ISearchField stickyField = fFactory.createSearchField(INews.IS_FLAGGED, name);
    ISearchCondition stickyCondition = fFactory.createSearchCondition(stickyField, SearchSpecifier.IS_NOT, true);

    /* 4.) Delete News that exceed a certain limit in a Feed */
    if (fOps.deleteNewsByCount()) {

      /* For each selected Bookmark */
      for (IBookMark mark : fBookmarks) {

        /* Ignore if Bookmark gets already deleted */
        if (bookmarksToDelete.contains(mark))
          continue;

        List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(3);
        conditions.add(getLocationCondition(mark));
        conditions.add(stickyCondition);
        conditions.add(stateCondition);

        /* Check if result count exceeds limit */
        List<SearchHit<NewsReference>> results = fModelSearch.searchNews(conditions, true);
        if (results.size() > fOps.getMaxNewsCountPerFeed()) {
          int toDeleteValue = results.size() - fOps.getMaxNewsCountPerFeed();

          /* Resolve News */
          List<INews> resolvedNews = new ArrayList<INews>(results.size());
          for (SearchHit<NewsReference> result : results)
            resolvedNews.add(result.getResult().resolve());

          /* Sort by Date */
          Collections.sort(resolvedNews, new Comparator<INews>() {
            public int compare(INews news1, INews news2) {
              return DateUtils.getRecentDate(news1).compareTo(DateUtils.getRecentDate(news2));
            }
          });

          Set<NewsReference> newsOfMarkToDelete = new HashSet<NewsReference>();
          for (int i = 0; i < resolvedNews.size() && i < toDeleteValue; i++)
            newsOfMarkToDelete.add(new NewsReference(resolvedNews.get(i).getId()));

          if (!newsOfMarkToDelete.isEmpty())
            newsToDelete.put(mark, newsOfMarkToDelete);
        }
      }
    }

    /* 5.) Delete News with an age > X Days */
    if (fOps.deleteNewsByAge()) {
      ISearchField ageInDaysField = fFactory.createSearchField(INews.AGE_IN_DAYS, name);
      ISearchCondition ageCond = fFactory.createSearchCondition(ageInDaysField, SearchSpecifier.IS_GREATER_THAN, fOps.getMaxNewsAge());

      /* For each selected Bookmark */
      for (IBookMark mark : fBookmarks) {

        /* Ignore if Bookmark gets already deleted */
        if (bookmarksToDelete.contains(mark))
          continue;

        List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(4);
        conditions.add(getLocationCondition(mark));
        conditions.add(ageCond);
        conditions.add(stateCondition);
        conditions.add(stickyCondition);

        List<SearchHit<NewsReference>> results = fModelSearch.searchNews(conditions, true);
        Set<NewsReference> newsOfMarkToDelete = new HashSet<NewsReference>();
        for (SearchHit<NewsReference> result : results)
          newsOfMarkToDelete.add(result.getResult());

        //TODO Keep separation in mind (subtract)
        if (!newsOfMarkToDelete.isEmpty()) {
          Collection<NewsReference> existingNewsOfMarkToDelete = newsToDelete.get(mark);
          if (existingNewsOfMarkToDelete == null)
            newsToDelete.put(mark, newsOfMarkToDelete);
          else
            existingNewsOfMarkToDelete.addAll(newsOfMarkToDelete);
        }
      }
    }

    /* 6.) Delete Read News */
    if (fOps.deleteReadNews()) {
      EnumSet<State> readState = EnumSet.of(INews.State.READ);
      ISearchCondition stateCond = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, readState);

      /* For each selected Bookmark */
      for (IBookMark mark : fBookmarks) {

        /* Ignore if Bookmark gets already deleted */
        if (bookmarksToDelete.contains(mark))
          continue;

        List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(3);
        conditions.add(getLocationCondition(mark));
        conditions.add(stateCond);
        conditions.add(stickyCondition);

        List<SearchHit<NewsReference>> results = fModelSearch.searchNews(conditions, true);
        Set<NewsReference> newsOfMarkToDelete = new HashSet<NewsReference>();
        for (SearchHit<NewsReference> result : results)
          newsOfMarkToDelete.add(result.getResult());

        //TODO Keep separation in mind (subtract)
        if (!newsOfMarkToDelete.isEmpty()) {
          Collection<NewsReference> existingNewsOfMarkToDelete = newsToDelete.get(mark);
          if (existingNewsOfMarkToDelete == null)
            newsToDelete.put(mark, newsOfMarkToDelete);
          else
            existingNewsOfMarkToDelete.addAll(newsOfMarkToDelete);
        }
      }
    }

    /* Create Tasks */
    fTasks.add(new DefragDatabaseTask());
    fTasks.add(new OptimizeSearchTask());

    for (IBookMark bookMarkToDelete : bookmarksToDelete) {
      CleanUpTask task = new BookMarkTask(bookMarkToDelete);
      fTasks.add(task);
    }

    Set<Entry<IBookMark, Set<NewsReference>>> entries = newsToDelete.entrySet();
    for (Entry<IBookMark, Set<NewsReference>> entry : entries) {
      CleanUpTask task = new NewsTask(entry.getKey(), entry.getValue());
      fTasks.add(task);
    }
  }
}