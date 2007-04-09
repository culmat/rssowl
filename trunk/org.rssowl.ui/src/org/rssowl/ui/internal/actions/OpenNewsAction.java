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

package org.rssowl.ui.internal.actions;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.CacheService;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The <code>OpenNewsAction</code> will open a given Selection of
 * <code>INews</code> in the <code>FeedView</code> opening the related
 * BookMark and adjusting the selection.
 *
 * @author bpasero
 */
public class OpenNewsAction extends Action {
  private IStructuredSelection fSelection;
  private CacheService fCacheService;
  private Shell fShellToMinimize;

  /**
   * @param selection
   */
  public OpenNewsAction(IStructuredSelection selection) {
    this(selection, null);
  }

  /**
   * @param selection
   * @param shellToMinimize The <code>Shell</code> to minimize (e.g. a Dialog)
   * when executing this action, or <code>NULL</code> if none.
   */
  public OpenNewsAction(IStructuredSelection selection, Shell shellToMinimize) {
    Assert.isTrue(selection != null && !selection.isEmpty());
    fSelection = selection;
    fShellToMinimize = shellToMinimize;
    fCacheService = Controller.getDefault().getCacheService();

    setText("Open");
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    internalRun();
  }

  /*
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void runWithEvent(Event event) {
    internalRun();
  }

  private void internalRun() {

    /* Require a Page */
    IWorkbenchPage page = RSSOwlUI.getPage();
    if (page == null)
      return;

    int openedEditors = 0;
    int maxOpenEditors = EditorUtils.getOpenEditorLimit();
    IEditorPart lastOpenedEditor = null;

    /* Convert selection to List of News (1 per Feed) */
    List< ? > list = fSelection.toList();
    List<FeedLinkReference> handledFeeds = new ArrayList<FeedLinkReference>(list.size());
    List<INews> newsToOpen = new ArrayList<INews>(list.size());
    for (Object selection : list) {
      if (selection instanceof INews) {
        INews news = (INews) selection;
        FeedLinkReference feedRef = news.getFeedReference();

        /* Check if already Handled */
        if (!handledFeeds.contains(feedRef)) {
          newsToOpen.add(news);
          handledFeeds.add(feedRef);
        }
      }
    }

    /* Minimize Shell if present */
    if (newsToOpen.size() > 0 && fShellToMinimize != null)
      fShellToMinimize.setMinimized(true);

    /* Open Bookmarks belonging to the News */
    for (int i = 0; i < newsToOpen.size() && openedEditors < maxOpenEditors; i++) {
      INews news = newsToOpen.get(i);

      /* Receive the first Bookmark belonging to the News and open it */
      Set<IBookMark> bookmarks = fCacheService.getBookMarks(news.getFeedReference());
      if (!bookmarks.isEmpty()) {
        IBookMark mark = bookmarks.iterator().next();

        /* Select this News in the FeedView */
        PerformAfterInputSet perform = PerformAfterInputSet.selectNews(new NewsReference(news.getId()));
        perform.setActivate(false);

        /* Open this Bookmark */
        FeedViewInput fvInput = new FeedViewInput(mark, perform);
        try {
          FeedView feedview = null;

          /* First check if input already shown */
          IEditorPart existingEditor = page.findEditor(fvInput);
          if (existingEditor != null && existingEditor instanceof FeedView) {
            feedview = (FeedView) existingEditor;

            /* Set Selection */
            feedview.setSelection(new StructuredSelection(news));
          }

          /* Otherwise open the Input in a new Editor */
          else
            feedview = (FeedView) page.openEditor(fvInput, FeedView.ID, false);

          openedEditors++;
          lastOpenedEditor = feedview;
        } catch (PartInitException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }
    }

    /* Activate the last opened editor */
    if (lastOpenedEditor != null)
      page.activate(lastOpenedEditor);
  }
}