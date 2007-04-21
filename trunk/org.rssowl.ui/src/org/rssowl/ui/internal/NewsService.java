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

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.events.FeedAdapter;
import org.rssowl.core.persist.events.FeedEvent;
import org.rssowl.core.persist.events.NewsAdapter;
import org.rssowl.core.persist.events.NewsEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * An internal Service helping Viewers to deal with the presentation of
 * <code>INews</code>. Current implemented features:
 * <ul>
 * <li>Fast access to Unread-Count of News from a Feed</li>
 * <li>Fast access to New-Count of News from a Feed</li>
 * <li>Fast access to Sticky-Count of News from a Feed</li>
 * </ul>
 * <p>
 * TODO Introduce a Listener that allows others to register on changes to one of
 * the fields. This reduces possible errors in case some plugin manages to
 * register as News-Listener right before this service.
 * </p>
 *
 * @author bpasero
 */
public class NewsService {

  /* The Counter for various aspects of News, the key is the feed link */
  private NewsCounter fCounter;

  private INewsCounterDAO fNewsCounterDao;

  NewsService() {
    fNewsCounterDao = DynamicDAO.getDAO(INewsCounterDAO.class);
    fCounter = loadCounter();
    registerListeners();
  }

  /**
   * Returns the number of unread News for the Feed referenced by
   * <code>feedLinkRef</code>.
   *
   * @param feedLinkRef The reference to the link of the Feed.
   * @return the number of unread News for the Feed having the given Id.
   */
  public int getUnreadCount(FeedLinkReference feedLinkRef) {
    synchronized (this) {
      NewsCounterItem counter = getFromCounter(feedLinkRef);

      /* Feed has no news */
      if (counter == null)
        return 0;

      return counter.getUnreadCounter();
    }
  }

  /**
   * Returns the number of new News for the Feed referenced by
   * <code>feedLinkRef</code>.
   *
   * @param feedLinkRef The reference to the link of the Feed.
   * @return the number of unread News for the Feed having the given link.
   */
  public int getNewCount(FeedLinkReference feedLinkRef) {
    synchronized (this) {
      NewsCounterItem counter = getFromCounter(feedLinkRef);

      /* Feed has no news */
      if (counter == null)
        return 0;

      return counter.getNewCounter();
    }
  }

  /**
   * Returns the number of sticky News for the Feed referenced by
   * <code>feedLinkRef</code>.
   *
   * @param feedLinkRef The reference to the link of the Feed.
   * @return the number of sticky News for the Feed having the given Id.
   */
  public int getStickyCount(FeedLinkReference feedLinkRef) {
    synchronized (this) {
      NewsCounterItem counter = getFromCounter(feedLinkRef);

      /* Feed has no news */
      if (counter == null)
        return 0;

      return counter.getStickyCounter();
    }
  }

  /**
   * Stops the News-Service and saves all data.
   */
  public void stopService() {
    synchronized (this) {
      saveState();
    }
  }

  /**
   * Method only used by Tests!
   */
  public void testDirtyShutdown() {
    synchronized (this) {
      fCounter = loadCounter();
    }
  }

  private synchronized NewsCounter loadCounter() {

    /* Load from DB */
    NewsCounter counter = fNewsCounterDao.load();

    /* Perform initial counting */
    if (counter == null)
      counter = countAll();

    /* Delete it to force recount on dirty shutdown */
    else
      fNewsCounterDao.delete();

    return counter;
  }

  private NewsCounter countAll() {
    NewsCounter newsCounter = new NewsCounter();
    Collection<IFeed> feeds = DynamicDAO.loadAll(IFeed.class);
    for (IFeed feed : feeds)
      newsCounter.put(feed.getLink(), count(feed));

    return newsCounter;
  }

  private void putInCounter(FeedLinkReference feedRef, NewsCounterItem counterItem) {
    fCounter.put(feedRef.getLink(), counterItem);
  }

  private NewsCounterItem getFromCounter(FeedLinkReference feedRef) {
    return fCounter.get(feedRef.getLink());
  }

  private NewsCounterItem count(IFeed feed) {
    NewsCounterItem counterItem = new NewsCounterItem();

    List<INews> newsList = feed.getVisibleNews();
    for (INews news : newsList) {
      if (isUnread(news.getState()))
        counterItem.incrementUnreadCounter();
      if (INews.State.NEW.equals(news.getState()))
        counterItem.incrementNewCounter();
      if (news.isFlagged())
        counterItem.incrementStickyCounter();
    }

    return counterItem;
  }

  private void saveState() {
    fNewsCounterDao.save(fCounter);
  }

  private void registerListeners() {
    DynamicDAO.addEntityListener(INews.class, new NewsAdapter() {
      @Override
      public void entitiesAdded(Set<NewsEvent> events) {
        onNewsAdded(events);
      }

      @Override
      public void entitiesUpdated(Set<NewsEvent> events) {
        onNewsUpdated(events);
      }

      @Override
      public void entitiesDeleted(Set<NewsEvent> events) {
        onNewsDeleted(events);
      }
    });

    DynamicDAO.addEntityListener(IFeed.class, new FeedAdapter() {
      @Override
      public void entitiesDeleted(Set<FeedEvent> events) {
        onFeedDeleted(events);
      }
    });
  }

  private void onNewsAdded(Set<NewsEvent> events) {
    for (NewsEvent event : events) {
      INews news = event.getEntity();
      FeedLinkReference feedRef = news.getFeedReference();

      synchronized (this) {
        NewsCounterItem counter = getFromCounter(feedRef);

        /* Create Counter if not yet done */
        if (counter == null) {
          counter = new NewsCounterItem();
          putInCounter(feedRef, counter);
        }

        /* Update Counter */
        if (news.getState() == INews.State.NEW)
          counter.incrementNewCounter();
        if (isUnread(news.getState()))
          counter.incrementUnreadCounter();
        if (news.isFlagged())
          counter.incrementStickyCounter();
      }
    }
  }

  private void onNewsDeleted(Set<NewsEvent> events) {
    for (NewsEvent event : events) {
      INews news = event.getEntity();

      synchronized (this) {
        NewsCounterItem counter = getFromCounter(news.getFeedReference());
        if (counter != null) {

          /* Update Counter */
          if (news.getState() == INews.State.NEW)
            counter.decrementNewCounter();
          if (isUnread(news.getState()))
            counter.decrementUnreadCounter();
          if (news.isFlagged())
            counter.decrementStickyCounter();
        }
      }
    }
  }

  private void onNewsUpdated(Set<NewsEvent> events) {
    for (NewsEvent event : events) {
      INews currentNews = event.getEntity();
      INews oldNews = event.getOldNews();
      Assert.isNotNull(oldNews, "oldNews cannot be null on newsUpdated");
      FeedLinkReference feedRef = currentNews.getFeedReference();

      boolean oldStateUnread = isUnread(oldNews.getState());
      boolean currentStateUnread = isUnread(currentNews.getState());

      boolean oldStateNew = INews.State.NEW.equals(oldNews.getState());
      boolean currentStateNew = INews.State.NEW.equals(currentNews.getState());

      boolean oldStateSticky = oldNews.isFlagged();
      boolean newStateSticky = currentNews.isFlagged() && currentNews.isVisible();

      /* No Change - continue */
      if (oldStateUnread == currentStateUnread && oldStateNew == currentStateNew && oldStateSticky == newStateSticky)
        continue;

      synchronized (this) {
        NewsCounterItem counter = getFromCounter(feedRef);

        /* News became read */
        if (oldStateUnread && !currentStateUnread)
          counter.decrementUnreadCounter();

        /* News became unread */
        else if (!oldStateUnread && currentStateUnread)
          counter.incrementUnreadCounter();

        /* News no longer New */
        if (oldStateNew && !currentStateNew)
          counter.decrementNewCounter();

        /* News became New */
        else if (!oldStateNew && currentStateNew)
          counter.incrementNewCounter();

        /* News became unsticky */
        if (oldStateSticky && !newStateSticky)
          counter.decrementStickyCounter();

        /* News became sticky */
        else if (!oldStateSticky && newStateSticky)
          counter.incrementStickyCounter();
      }
    }
  }

  private void onFeedDeleted(Set<FeedEvent> events) {
    for (FeedEvent event : events) {
      URI feedLink = event.getEntity().getLink();
      synchronized (this) {
        removeFromCounter(feedLink);
      }
    }
  }

  private void removeFromCounter(URI feedLink) {
    fCounter.remove(feedLink);
  }

  private boolean isUnread(INews.State state) {
    return state == INews.State.NEW || state == INews.State.UPDATED || state == INews.State.UNREAD;
  }
}