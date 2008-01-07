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
import org.rssowl.ui.internal.util.ModelUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

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

  /* Delay before Progress is shown */
  private static final int SHOW_PROGRESS_THRESHOLD = 2000;

  private final INewsCounterDAO fNewsCounterDao;

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
    createAndSaveCounterIfNecessary();
  }

  /**
   * Stops the News-Service and saves all data.
   */
  public void stopService() {
    // Do nothing
  }

  /**
   * Method only used by Tests!
   */
  public void testDirtyShutdown() {
    createAndSaveCounterIfNecessary();
  }

  private NewsCounter createAndSaveCounterIfNecessary() {

    /* Load from DB */
    NewsCounter counter = fNewsCounterDao.load();

    /* Perform initial counting */
    if (counter == null) {
      counter = countAll();
      fNewsCounterDao.save(counter);
    }

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
}