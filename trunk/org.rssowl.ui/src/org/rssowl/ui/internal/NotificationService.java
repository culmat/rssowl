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
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.BatchedBuffer;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <code>NotificationService</code> listens on News being downloaded and
 * opens the <code>NotificationPopup</code> to show them in case the
 * preferences are set to show notifications.
 *
 * @author bpasero
 */
public class NotificationService {

  /* Batch News-Events for every 5 seconds */
  private static final int BATCH_INTERVAL = 5000;

  private final NewsListener fNewsAdapter;
  private final IPreferenceScope fGlobalPreferences;
  private final BatchedBuffer<NewsEvent> fBatchedBuffer;

  /* Singleton instance */
  private static NotificationPopup fgNotificationPopup;

  /** Creates a new Notification Service */
  public NotificationService() {
    BatchedBuffer.Receiver<NewsEvent> receiver = new BatchedBuffer.Receiver<NewsEvent>() {
      public void receive(Set<NewsEvent> objects) {
        showNews(objects);
      }
    };

    fBatchedBuffer = new BatchedBuffer<NewsEvent>(receiver, BATCH_INTERVAL);
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fNewsAdapter = registerListeners();
  }

  /** Shutdown this Service */
  public void stopService() {
    DynamicDAO.removeEntityListener(INews.class, fNewsAdapter);
  }

  private boolean isPopupVisible() {
    return fgNotificationPopup != null;
  }

  /* Startup this Service */
  private NewsListener registerListeners() {
    NewsListener listener = new NewsAdapter() {
      @Override
      public void entitiesAdded(final Set<NewsEvent> events) {
        onNewsAdded(events);
      }
    };

    DynamicDAO.addEntityListener(INews.class, listener);
    return listener;
  }

  private void onNewsAdded(Set<NewsEvent> events) {

    /* Return if Notification is disabled */
    if (!fGlobalPreferences.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP))
      return;

    /* Filter Events if user decided to show Notifier only for selected Elements */
    if (fGlobalPreferences.getBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION)) {
      List<FeedLinkReference> enabledFeeds = new ArrayList<FeedLinkReference>();

      /* TODO This can be slow, try to optimize performance! */
      Set<IBookMark> bookMarks = Controller.getDefault().getCacheService().getBookMarks();
      for (IBookMark bookMark : bookMarks) {
        IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(bookMark);
        if (prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER))
          enabledFeeds.add(bookMark.getFeedLinkReference());
      }

      events = filterEvents(events, enabledFeeds);
    }

    /* Add into Buffer */
    if (!isPopupVisible())
      fBatchedBuffer.addAll(events);

    /* Show Directly */
    else
      showNews(events);
  }

  private Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<FeedLinkReference> enabledFeeds) {
    Set<NewsEvent> filteredEvents = new HashSet<NewsEvent>();

    for (NewsEvent event : events) {
      if (enabledFeeds.contains(event.getEntity().getFeedReference()))
        filteredEvents.add(event);
    }

    return filteredEvents;
  }

  /* Show Notification in UI Thread */
  private void showNews(final Set<NewsEvent> events) {
    JobRunner.runInUIThread(OwlUI.getPrimaryShell(), new Runnable() {
      public void run() {

        /* Return if Notification should only show when minimized */
        ApplicationWorkbenchWindowAdvisor advisor = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
        boolean minimized = advisor != null && (advisor.isMinimizedToTray() || advisor.isMinimized());
        if (!minimized && fGlobalPreferences.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED))
          return;

        /* Extract News */
        List<INews> news = new ArrayList<INews>();
        for (NewsEvent event : events)
          news.add(event.getEntity());

        /* Show News in Popup */
        showNewsInPopup(news);
      }
    });
  }

  /*
   * Opens the <code>NotificationPopup</code> if not yet opened and shows the
   * given List of News.
   */
  private synchronized void showNewsInPopup(List<INews> news) {

    /* Not yet opened */
    if (fgNotificationPopup == null) {
      fgNotificationPopup = new NotificationPopup(news.size()) {
        @Override
        public boolean close() {
          fgNotificationPopup = null;
          return super.close();
        }
      };
      fgNotificationPopup.open();
    }

    /* Show News */
    fgNotificationPopup.makeVisible(news);
  }
}