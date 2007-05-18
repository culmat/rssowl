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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.NewsService;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author bpasero
 */
public class MarkReadAction extends Action implements IWorkbenchWindowActionDelegate {
  private IStructuredSelection fSelection;
  private NewsService fNewsService;
  private INewsDAO fNewsDAO;

  /**
   *
   */
  public MarkReadAction() {
    this(StructuredSelection.EMPTY);
  }

  /**
   * @param selection
   */
  public MarkReadAction(IStructuredSelection selection) {
    fSelection = selection;
    fNewsService = Controller.getDefault().getNewsService();
    fNewsDAO = DynamicDAO.getDAO(INewsDAO.class);
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    JobRunner.runInBackgroundWithBusyIndicator(new Runnable() {
      public void run() {
        MarkReadAction.this.internalRun();
      }
    });
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    JobRunner.runInBackgroundWithBusyIndicator(new Runnable() {
      public void run() {
        MarkReadAction.this.internalRun();
      }
    });
  }

  private void internalRun() {

    /* Only consider Entities */
    List<IEntity> entities = ModelUtils.getEntities(fSelection);

    /* Retrieve any Folder that is to be marked read */
    Set<IFolder> folders = null;
    for (Object element : entities) {
      if (element instanceof IFolder) {
        if (folders == null)
          folders = new HashSet<IFolder>();
        folders.add((IFolder) element);
      }
    }

    /* Relax */
    if (folders != null)
      for (IFolder folder : folders)
        ModelUtils.normalize(folder, entities);

    /* Use Map for follow-up Retention */
    Map<IBookMark, List<INews>> retentionHelperMap = new HashMap<IBookMark, List<INews>>();

    /* Retrieve affected News */
    List<INews> news = new ArrayList<INews>();
    for (IEntity element : entities) {
      if (element instanceof IFolder)
        fillNews((IFolder) element, news, retentionHelperMap);
      else if (element instanceof IBookMark)
        fillNews((IBookMark) element, news, retentionHelperMap);
      else if (element instanceof ISearchMark)
        fillNews((ISearchMark) element, news);
      else if (element instanceof INews)
        news.add((INews) element);
    }

    /* Apply the state to the NewsItems for Retention to handle them properly */
    for (INews newsItem : news) {
      newsItem.setState(INews.State.READ);
    }

    /* See if Retention is required for each BookMark */
    Set<Entry<IBookMark, List<INews>>> entries = retentionHelperMap.entrySet();
    for (Entry<IBookMark, List<INews>> entry : entries) {
      IBookMark bookmark = entry.getKey();
      IPreferenceScope bookMarkPreferences = Owl.getPreferenceService().getEntityScope(bookmark);

      /* Delete News that are now marked as Read */
      if (bookMarkPreferences.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE)) {
        List<INews> deletedNews = RetentionStrategy.process(bookmark, entry.getValue());

        /*
         * This is an optimization to the process. Any News that is marked as
         * read is getting deleted here. Thus, there is no need in marking the
         * News as Read.
         */
        news.removeAll(deletedNews);
      }
    }

    /* Mark News Read */
    if (news.size() > 0) {

      /* Only affect equivalent News if not all News are affected */
      boolean affectEquivalentNews = !equalsRootFolders(folders);

      /* Mark Saved Search Service as in need for a quick Update */
      Controller.getDefault().getSavedSearchService().forceQuickUpdate();

      /* Peform Op */
      Owl.getPersistenceService().getDAOService().getNewsDAO().setState(news, INews.State.READ, affectEquivalentNews, true);
    }
  }

  private boolean equalsRootFolders(Set<IFolder> folders) {
    Set<IFolder> rootFolders = Controller.getDefault().getCacheService().getRootFolders();
    return folders != null && folders.equals(rootFolders);
  }

  /* TODO This Method is currently ignoring SearchMarks */
  private void fillNews(IFolder folder, List<INews> news, Map<IBookMark, List<INews>> bookMarkNewsMap) {
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {
      if (child instanceof IBookMark && containsUnread(((IBookMark) child)))
        fillNews((IBookMark) child, news, bookMarkNewsMap);
      else if (child instanceof IFolder)
        fillNews((IFolder) child, news, bookMarkNewsMap);
    }
  }

  private boolean containsUnread(IBookMark mark) {
    return fNewsService.getUnreadCount(mark.getFeedLinkReference()) != 0;
  }

  private void fillNews(IBookMark bookmark, List<INews> news, Map<IBookMark, List<INews>> bookMarkNewsMap) {
    IFeed feed = bookmark.getFeedLinkReference().resolve();

    news.addAll(feed.getNewsByStates(EnumSet.of(INews.State.UNREAD, INews.State.UPDATED, INews.State.NEW)));
    bookMarkNewsMap.put(bookmark, feed.getVisibleNews());
  }

  private void fillNews(ISearchMark searchmark, List<INews> news) {
    List<NewsReference> matchingNews = searchmark.getResult(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
    for (NewsReference newsRef : matchingNews) {
      INews resolvedNews = newsRef.resolve();
      if (resolvedNews != null) //TODO Remove once Bug 173 is fixed
        news.add(resolvedNews);
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {}
}