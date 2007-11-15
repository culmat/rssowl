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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.event.FeedAdapter;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.ui.internal.util.ModelUtils;

import java.lang.reflect.InvocationTargetException;
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

  /* Delay before Progress is shown */
  private static final int SHOW_PROGRESS_THRESHOLD = 2000;

  private INewsCounterDAO fNewsCounterDao;

  /* Subclass of a Progress Monitor Dialog to show progress after a Crash */
  private static class NewsServiceProgressMonitorDialog extends ProgressMonitorDialog {
    NewsServiceProgressMonitorDialog(Shell parent) {
      super(parent);
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#getInitialLocation(org.eclipse.swt.graphics.Point)
     */
    @Override
    protected Point getInitialLocation(Point initialSize) {
      Rectangle displayBounds = getParentShell().getDisplay().getPrimaryMonitor().getBounds();
      Point shellSize = getInitialSize();
      int x = displayBounds.x + (displayBounds.width - shellSize.x) >> 1;
      int y = displayBounds.y + (displayBounds.height - shellSize.y) >> 1;

      return new Point(x, y);
    }

    /*
     * @see org.eclipse.jface.dialogs.ProgressMonitorDialog#getInitialSize()
     */
    @Override
    protected Point getInitialSize() {
      int minWidth = 380;
      int minHeight = getShell().computeSize(minWidth, SWT.DEFAULT).y;

      return new Point(minWidth, minHeight);
    }
  }

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
    final NewsCounter newsCounter = new NewsCounter();
    final long start = System.currentTimeMillis();
    final Collection<IFeed> feeds = DynamicDAO.loadAll(IFeed.class);

    final ProgressMonitorDialog dialog = new NewsServiceProgressMonitorDialog(new Shell(Display.getDefault(), SWT.NONE));
    dialog.setOpenOnRun(false);

    /* Runnable will open the Dialog after SHOW_PROGRESS_THRESHOLD ms */
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      private boolean fDialogOpened;

      public void run(IProgressMonitor monitor) {
        int worked = 0;
        for (IFeed feed : feeds) {
          newsCounter.put(feed.getLink(), count(feed));

          /* Open the Dialog if exceeded SHOW_PROGRESS_THRESHOLD ms */
          if (System.currentTimeMillis() - start > SHOW_PROGRESS_THRESHOLD && !fDialogOpened) {
            dialog.open();
            monitor.beginTask("RSSOwl was not shutdown properly. Restoring data...", feeds.size() - worked);
            fDialogOpened = true;
          }

          /* Worked a bit again... */
          if (fDialogOpened)
            monitor.worked(1);

          /* Remember the worked items */
          worked++;
        }

        /* Completed */
        monitor.done();
      }
    };

    /* Execute the Runnable */
    try {
      dialog.run(false, false, runnable);
    } catch (InvocationTargetException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    } catch (InterruptedException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

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
      if (ModelUtils.isUnread(news.getState()))
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
    DynamicDAO.addEntityListener(INews.class, new NewsListener() {
      public void entitiesAdded(Set<NewsEvent> events) {
        onNewsAdded(events);
      }

      public void entitiesUpdated(Set<NewsEvent> events) {
        onNewsUpdated(events);
      }

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
        if (ModelUtils.isUnread(news.getState()))
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
          if (ModelUtils.isUnread(news.getState()))
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

      boolean oldStateUnread = ModelUtils.isUnread(oldNews.getState());
      boolean currentStateUnread = ModelUtils.isUnread(currentNews.getState());

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
}