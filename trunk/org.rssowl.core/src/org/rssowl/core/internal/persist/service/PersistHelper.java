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

import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.event.runnable.EventRunnable;
import org.rssowl.core.persist.event.runnable.FeedEventRunnable;
import org.rssowl.core.persist.event.runnable.NewsEventRunnable;

import java.util.List;

public final class PersistHelper {

  private PersistHelper() {
    super();
  }

  //FIXME Add this method to INewsCounterDAO?
  public static void updateNewsCounter(INewsCounterDAO newsCounterDAO,
      EventsMap2 eventsMap) {
    List<EventRunnable<?>> eventRunnables = eventsMap.getEventRunnables();
    NewsCounterService2 newsCounterService = new NewsCounterService2(newsCounterDAO);
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

  public static NewsEventRunnable getNewsEventRunnables(List<EventRunnable<?>> eventRunnables)  {
    for (EventRunnable<?> eventRunnable : eventRunnables) {
      if (eventRunnable instanceof NewsEventRunnable)
        return (NewsEventRunnable) eventRunnable;
    }
    return null;
  }
}
