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
package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.util.Collection;
import java.util.EnumSet;

public final class NewsCounterService2 {

  private final NewsCounter fNewsCounter;
  private final INewsCounterDAO fNewsCounterDAO;

  public NewsCounterService2(INewsCounterDAO newsCounterDAO) {
    fNewsCounter = newsCounterDAO.load();
    fNewsCounterDAO = newsCounterDAO;
  }

  public void onNewsAdded(Collection<NewsEvent> newsEvents) {
    if (newsEvents.isEmpty())
      return;

    synchronized (fNewsCounter) {
      boolean newsCounterUpdated = false;
      for (NewsEvent newsEvent : newsEvents) {
        INews news = newsEvent.getEntity();

        if (news.getParentId() != 0)
          continue;

        FeedLinkReference feedRef = news.getFeedReference();

        NewsCounterItem newsCounterItem = fNewsCounter.get(feedRef.getLink());

        /* Create Counter if not yet done */
        if (newsCounterItem == null) {
          newsCounterItem = new NewsCounterItem();
          fNewsCounter.put(feedRef.getLink(), newsCounterItem);
        }

        /* Update Counter */
        if (news.getState() == INews.State.NEW)
          newsCounterItem.incrementNewCounter();
        if (isUnread(news.getState()))
          newsCounterItem.incrementUnreadCounter();
        if (news.isFlagged())
          newsCounterItem.incrementStickyCounter();

        newsCounterUpdated = true;
      }
      if (newsCounterUpdated)
        fNewsCounterDAO.save();
    }
  }

  public void onNewsUpdated(Collection<NewsEvent> newsEvents) {
    synchronized (fNewsCounter) {
      boolean newsCounterUpdated = false;
      for (NewsEvent event : newsEvents) {
        INews currentNews = event.getEntity();

        if (currentNews.getParentId() != 0)
          continue;

        INews oldNews = event.getOldNews();
        Assert.isNotNull(oldNews, "oldNews cannot be null on newsUpdated");

        boolean oldStateUnread = isUnread(oldNews.getState());
        boolean currentStateUnread = isUnread(currentNews.getState());

        boolean oldStateNew = INews.State.NEW.equals(oldNews.getState());
        boolean currentStateNew = INews.State.NEW.equals(currentNews.getState());

        boolean oldStateSticky = oldNews.isFlagged();
        boolean newStateSticky = currentNews.isFlagged() && currentNews.isVisible();

        /* No Change - continue */
        if (oldStateUnread == currentStateUnread && oldStateNew == currentStateNew && oldStateSticky == newStateSticky)
          continue;

        NewsCounterItem counterItem = fNewsCounter.get(currentNews.getFeedReference().getLink());

        /* News became read */
        if (oldStateUnread && !currentStateUnread)
          counterItem.decrementUnreadCounter();

        /* News became unread */
        else if (!oldStateUnread && currentStateUnread)
          counterItem.incrementUnreadCounter();

        /* News no longer New */
        if (oldStateNew && !currentStateNew)
          counterItem.decrementNewCounter();

        /* News became New */
        else if (!oldStateNew && currentStateNew)
          counterItem.incrementNewCounter();

        /* News became unsticky */
        if (oldStateSticky && !newStateSticky)
          counterItem.decrementStickyCounter();

        /* News became sticky */
        else if (!oldStateSticky && newStateSticky)
          counterItem.incrementStickyCounter();

        newsCounterUpdated = true;
      }
      if (newsCounterUpdated)
        fNewsCounterDAO.save();
    }
  }

  public void onNewsRemoved(Collection<NewsEvent> newsEvents) {
    synchronized (fNewsCounter) {
      boolean newsCounterUpdated = false;
      for (NewsEvent newsEvent : newsEvents) {
        INews news = newsEvent.getEntity();

        if (news.getParentId() != 0)
          continue;

        NewsCounterItem counterItem = fNewsCounter.get(news.getFeedReference().getLink());

        /* Update Counter */
        if (news.getState() == INews.State.NEW)
          counterItem.decrementNewCounter();
        if (isUnread(news.getState()))
          counterItem.decrementUnreadCounter();
        if (news.isFlagged() && (!EnumSet.of(INews.State.DELETED, INews.State.HIDDEN).contains(news.getState())))
          counterItem.decrementStickyCounter();

        newsCounterUpdated = true;
      }
      if (newsCounterUpdated)
        fNewsCounterDAO.save();
    }
  }

  public void onFeedRemoved(Collection<FeedEvent> feedEvents) {
    if (feedEvents.isEmpty())
      return;

    synchronized (fNewsCounter) {
      for (FeedEvent feedEvent : feedEvents) {
        IFeed feed = feedEvent.getEntity();
        fNewsCounter.remove(feed.getLink());
      }
      fNewsCounterDAO.save();
    }
  }

  //FIXME Copied from ModelUtils, maybe add it to INews.State to avoid duplication
  private static boolean isUnread(INews.State state) {
    return state == INews.State.NEW || state == INews.State.UPDATED || state == INews.State.UNREAD;
  }
}
