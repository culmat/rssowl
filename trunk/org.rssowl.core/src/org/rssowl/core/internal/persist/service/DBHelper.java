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
package org.rssowl.core.internal.persist.service;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.LazySet;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.INewsBin.StatesUpdateInfo;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.runnable.EventRunnable;
import org.rssowl.core.persist.event.runnable.FeedEventRunnable;
import org.rssowl.core.persist.event.runnable.NewsEventRunnable;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.persist.service.UniqueConstraintException;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DBHelper {

  public static final List<EventRunnable<?>> cleanUpEvents() {
    List<EventRunnable<?>> eventNotifiers = EventsMap.getInstance().removeEventRunnables();
    EventsMap.getInstance().removeEventTemplatesMap();
    EventManager.getInstance().clear();
    return eventNotifiers;
  }
  public static final void cleanUpAndFireEvents() {
    fireEvents(cleanUpEvents());
  }

  public static final void fireEvents(List<EventRunnable<?>> eventNotifiers) {
    if (eventNotifiers == null) {
      return;
    }
    for (EventRunnable<?> runnable : eventNotifiers) {
      runnable.run();
    }
  }

  public static final void putEventTemplate(ModelEvent modelEvent) {
    EventsMap.getInstance().putEventTemplate(modelEvent);
  }

  public static final void saveFeed(ObjectContainer db, IFeed feed) {
    if (feed.getId() == null && feedExists(db, feed.getLink()))
      throw new UniqueConstraintException("link", feed);

    ModelEvent feedEventTemplate = new FeedEvent(feed, true);
    DBHelper.putEventTemplate(feedEventTemplate);
    saveAndCascadeAllNews(db, feed.getNews(), false);
    saveEntities(db, feed.getCategories());
    saveEntity(db, feed.getAuthor());
    saveEntity(db, feed.getImage());

    db.ext().set(feed, 2);
  }

  private static void saveEntity(ObjectContainer db, IPersistable entity) {
    if (entity != null)
      db.set(entity);
  }


  private static void saveEntities(ObjectContainer db, List<? extends IEntity> entities) {
    for (IEntity entity : entities)
      db.ext().set(entity, 1);
  }

  static void saveAndCascadeAllNews(ObjectContainer db, Collection<INews> newsCollection, boolean root) {
    for (INews news : newsCollection)
      ((News) news).acquireReadLockSpecial();

    try {
      for (INews news : newsCollection)
        saveAndCascadeNews(db, news, root);
    } finally {
      for (INews news : newsCollection)
        ((News) news).releaseReadLockSpecial();
    }
  }

  public static final INews peekPersistedNews(ObjectContainer db, INews news) {
    INews oldNews = db.ext().peekPersisted(news, 2, true);
    if (oldNews instanceof News) {
      ((News) oldNews).init();
    }
    return oldNews;
  }

  public static final void saveNews(ObjectContainer db, INews news) {
    saveNews(db, news, 2);
  }

  public static final void saveNews(ObjectContainer db, INews news, Integer updateDepth) {
    INews oldNews = peekPersistedNews(db, news);
    if (oldNews != null) {
      ModelEvent newsEventTemplate = new NewsEvent(oldNews, news, false);
      DBHelper.putEventTemplate(newsEventTemplate);
    }
    db.ext().set(news, updateDepth);
  }

  static final boolean feedExists(ObjectContainer db, URI link) {
    return !getFeeds(db, link).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static List<Feed> getFeeds(ObjectContainer db, URI link){
    Query query = db.query();
    query.constrain(Feed.class);
    query.descend("fLinkText").constrain(link.toString()); //$NON-NLS-1$
    List<Feed> set = query.execute();
    return set;
  }

  public static final Feed loadFeed(ObjectContainer db, URI link, Integer activationDepth) {
    try {
      List<Feed> feeds = getFeeds(db, link);
      if (!feeds.isEmpty()) {
        Feed feed = feeds.iterator().next();
        if (activationDepth != null)
          db.ext().activate(feed, activationDepth.intValue());

        return feed;
      }
      return null;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public static final void saveAndCascadeNews(ObjectContainer db, INews news, boolean root) {
    INews oldNews = peekPersistedNews(db, news);
    if (oldNews != null || root) {
      ModelEvent event = new NewsEvent(oldNews, news, root);
      putEventTemplate(event);
    }
    saveEntities(db, news.getCategories());
    saveEntity(db, news.getAuthor());
    saveEntities(db, news.getAttachments());
    saveEntity(db, news.getSource());
    db.ext().set(news, 2);
  }

  public static void preCommit(ObjectContainer db) {
    updateNewsCounter(db);
    updateNewsBins(db);
  }

  private static NewsEventRunnable getNewsEventRunnables(List<EventRunnable<?>> eventRunnables)  {
    for (EventRunnable<?> eventRunnable : eventRunnables) {
      if (eventRunnable instanceof NewsEventRunnable)
        return (NewsEventRunnable) eventRunnable;
    }
    return null;
  }

  private static void updateNewsBins(ObjectContainer db) {
    NewsEventRunnable newsEventRunnable = getNewsEventRunnables(EventsMap.getInstance().getEventRunnables());

    if (newsEventRunnable == null)
      return;

    List<StatesUpdateInfo> statesUpdateInfos = new ArrayList<StatesUpdateInfo>();
    for (NewsEvent newsEvent : newsEventRunnable.getUpdateEvents()) {
      if (newsEvent.getEntity().isCopy() && (newsEvent.getOldNews().getState() != newsEvent.getEntity().getState())) {
        statesUpdateInfos.add(new StatesUpdateInfo(newsEvent.getOldNews().getState(), newsEvent.getEntity().getState(), newsEvent.getEntity().toReference()));
      }
    }
    if (!statesUpdateInfos.isEmpty()) {
      List<NewsReference> removedNewsRefs = new ArrayList<NewsReference>();
      for (INewsBin newsBin : DynamicDAO.loadAll(INewsBin.class)) {
        if (newsBin.updateNewsStates(statesUpdateInfos)) {
          removedNewsRefs.addAll(newsBin.removeNews(EnumSet.of(INews.State.DELETED)));
          putEventTemplate(new NewsBinEvent(newsBin, null, true));
          db.ext().set(newsBin, Integer.MAX_VALUE);
        }
      }
      removeNewsAndFeedsAfterNewsBinUpdate(db, removedNewsRefs);
    }
  }

  static void removeNewsAndFeedsAfterNewsBinUpdate(ObjectContainer db,
      List<NewsReference> removedNewsRefs) {

    Set<FeedLinkReference> feedRefs = new HashSet<FeedLinkReference>(removedNewsRefs.size());
    for (NewsReference newsRef : removedNewsRefs) {
      INews news = newsRef.resolve();
      feedRefs.add(news.getFeedReference());
      db.delete(news);
    }
    NewsCounter newsCounter = DynamicDAO.getDAO(INewsCounterDAO.class).load();
    boolean changed = false;
    for (FeedLinkReference feedRef : feedRefs) {
      if ((countBookMarkReference(db, feedRef) == 0) && !feedHasNewsWithCopies(db, feedRef)) {
          db.delete(feedRef.resolve());
          newsCounter.remove(feedRef.getLink());
          changed = true;
      }
    }
    if (changed)
      db.ext().set(newsCounter, Integer.MAX_VALUE);
  }

  static int countBookMarkReference(ObjectContainer db, FeedLinkReference feedRef) {
    Collection<IBookMark> marks = loadAllBookMarks(db, feedRef);
    return marks.size();
  }

  @SuppressWarnings("unchecked")
  public static Collection<IBookMark> loadAllBookMarks(ObjectContainer db, FeedLinkReference feedRef)  {
    Query query = db.query();
    query.constrain(BookMark.class);
    query.descend("fFeedLink").constrain(feedRef.getLink().toString()); //$NON-NLS-1$
    return query.execute();
  }

  static boolean feedHasNewsWithCopies(ObjectContainer db, FeedLinkReference feedRef) {
    Query query = db.query();
    query.constrain(News.class);
    query.descend("fFeedLink").constrain(feedRef.getLink().toString());
    query.descend("fCopy").constrain(true);
    return !query.execute().isEmpty();
  }

  public static void updateNewsCounter(ObjectContainer db) {
    List<EventRunnable<?>> eventRunnables = EventsMap.getInstance().getEventRunnables();
    NewsCounterService newsCounterService = new NewsCounterService(Owl.getPersistenceService().getDAOService().getNewsCounterDAO(), db);
    NewsEventRunnable newsEventRunnable = getNewsEventRunnables(eventRunnables);
    if (newsEventRunnable != null) {
      newsCounterService.onNewsAdded(newsEventRunnable.getPersistEvents());
      newsCounterService.onNewsRemoved((newsEventRunnable.getRemoveEvents()));
      newsCounterService.onNewsUpdated(newsEventRunnable.getUpdateEvents());
    }
    for (EventRunnable<?> eventRunnable : eventRunnables) {
      if (eventRunnable instanceof FeedEventRunnable) {
        FeedEventRunnable feedEventRunnable = (FeedEventRunnable) eventRunnable;
        newsCounterService.onFeedRemoved(feedEventRunnable.getRemoveEvents());
        break;
      }
    }
  }
  public static Collection<IFeed> loadAllFeeds(ObjectContainer db) {
    ObjectSet<? extends IFeed> entities = db.query(Feed.class);
    return new LazySet<IFeed>(entities, db);
  }
}