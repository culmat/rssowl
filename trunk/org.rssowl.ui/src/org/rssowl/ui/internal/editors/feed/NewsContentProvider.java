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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.EntityGroupItem;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsContentProvider implements ITreeContentProvider {
  private NewsBrowserViewer fBrowserViewer;
  private NewsTableViewer fTableViewer;
  private NewsGrouping fGrouping;
  private NewsListener fNewsListener;
  private SearchMarkAdapter fSearchMarkListener;
  private IMark[] fInput;
  private FeedView fFeedView;
  private boolean fDisposed;

  /* Cache displayed News */
  private Set<INews> fCachedNews;

  /**
   * @param tableViewer
   * @param browserViewer
   * @param feedView
   */
  public NewsContentProvider(NewsTableViewer tableViewer, NewsBrowserViewer browserViewer, FeedView feedView) {
    fTableViewer = tableViewer;
    fBrowserViewer = browserViewer;
    fFeedView = feedView;
    fGrouping = feedView.getGrouper();
  }

  /*
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    List<Object> elements = new ArrayList<Object>();

    /* Wrap into Object Array */
    if (!(inputElement instanceof Object[]))
      inputElement = new Object[] { inputElement };

    /* Foreach Object */
    Object[] objects = (Object[]) inputElement;
    for (Object object : objects) {

      /* This is a News */
      if (object instanceof INews && ((INews) object).isVisible()) {
        elements.add(object);
      }

      /* This is a NewsReference */
      else if (object instanceof NewsReference) {
        NewsReference newsRef = (NewsReference) object;
        INews news = obtainFromCache(newsRef);
        if (news != null)
          elements.add(news);
      }

      /* This is a FeedReference */
      else if (object instanceof FeedLinkReference) {
        Collection<INews> news = fCachedNews;
        if (news != null) {
          if (fGrouping.getType() == NewsGrouping.Type.NO_GROUPING)
            elements.addAll(news);
          else
            elements.addAll(fGrouping.group(news));
        }
      }

      /* This is a Bookmark */
      else if (object instanceof BookMarkReference) {
        Collection<INews> news = fCachedNews;
        if (news != null) {
          if (fGrouping.getType() == NewsGrouping.Type.NO_GROUPING)
            elements.addAll(news);
          else
            elements.addAll(fGrouping.group(news));
        }
      }

      /* This is a SearchMark */
      else if (object instanceof SearchMarkReference) {
        Collection<INews> news = fCachedNews;
        if (news != null) {
          if (fGrouping.getType() == NewsGrouping.Type.NO_GROUPING)
            elements.addAll(news);
          else
            elements.addAll(fGrouping.group(news));
        }
      }

      /* This is a EntityGroup */
      else if (object instanceof EntityGroup) {
        EntityGroup group = (EntityGroup) object;

        List<EntityGroupItem> items = group.getItems();
        for (EntityGroupItem item : items) {
          if (((INews) item.getEntity()).isVisible())
            elements.add(item.getEntity());
        }
      }
    }

    return elements.toArray();
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  public Object[] getChildren(Object parentElement) {
    List<Object> children = new ArrayList<Object>();

    /* Handle EntityGroup */
    if (parentElement instanceof EntityGroup) {
      List<EntityGroupItem> items = ((EntityGroup) parentElement).getItems();
      for (EntityGroupItem item : items)
        children.add(item.getEntity());
    }

    return children.toArray();
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  public Object getParent(Object element) {

    /* Handle Grouping specially */
    if (fGrouping.isActive() && element instanceof INews) {
      List<EntityGroup> groups = fGrouping.group(Collections.singletonList((INews) element));
      if (groups.size() == 1)
        return groups.get(0);
    }

    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  public boolean hasChildren(Object element) {
    return element instanceof EntityGroup;
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    fDisposed = true;
    unregisterListeners();
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  /* Ignore - Input changes are handled via refreshCache(Object input) */
  }

  boolean isGroupingEnabled() {
    return fGrouping.getType() != NewsGrouping.Type.NO_GROUPING;
  }

  /* Returns the news that have been added since the last refresh */
  List<INews> refreshCache(IMark[] input, boolean onlyAdd) throws PersistenceException {
    List<INews> addedNews = Collections.emptyList();

    /* Update Input */
    fInput = input;

    /* Register Listeners if not yet done */
    if (fNewsListener == null)
      registerListeners();

    /* Clear old Data if required */
    if (fCachedNews == null)
      fCachedNews = new HashSet<INews>();
    else if (!onlyAdd)
      fCachedNews.clear();

    /* Check if ContentProvider was already disposed */
    if (fDisposed)
      return addedNews;

    /* Obtain the News */
    addedNews = new ArrayList<INews>();
    for (IMark mark : input) {

      /* Obtain this BookMark's Feed */
      if (mark instanceof IBookMark) {
        IFeed feed = ((IBookMark) mark).getFeedLinkReference().resolve();
        addedNews.addAll(feed.getVisibleNews());
      }

      /* Obtain this SearchMark's News */
      else if (mark instanceof ISearchMark) {
        ISearchMark searchMark = (ISearchMark) mark;
        List<NewsReference> matchingNews = searchMark.getResult();
        for (NewsReference newsRef : matchingNews) {

          /* Avoid to resolve an already shown News */
          if (onlyAdd && hasCachedNews(newsRef))
            continue;

          /* Resolve and Add News */
          INews resolvedNews = newsRef.resolve();
          if (resolvedNews != null) //TODO Remove once Bug 173 is fixed
            addedNews.add(resolvedNews);
        }
      }
    }

    /* Add into Cache */
    synchronized (this) {
      fCachedNews.addAll(addedNews);
    }

    return addedNews;
  }

  synchronized Set<INews> getCachedNews() {
    return fCachedNews;
  }

  synchronized boolean hasCachedNews() {
    return fCachedNews != null && !fCachedNews.isEmpty();
  }

  private synchronized boolean hasCachedNews(NewsReference ref) {
    if (fCachedNews == null)
      return false;

    for (INews news : fCachedNews) {
      if (ref.references(news))
        return true;
    }

    return false;
  }

  private synchronized INews obtainFromCache(NewsReference ref) {
    for (INews cachedNews : fCachedNews) {
      if (ref.references(cachedNews))
        return cachedNews;
    }

    return null;
  }

  private void registerListeners() {

    /* Saved Search Listener */
    fSearchMarkListener = new SearchMarkAdapter() {
      @Override
      public void resultsChanged(final Set<SearchMarkEvent> events) {
        for (SearchMarkEvent event : events) {
          ISearchMark searchMark = event.getEntity();

          for (final IMark inputMark : fInput) {
            if (inputMark.equals(searchMark)) {
              JobRunner.runUIUpdater(new UIBackgroundJob(fFeedView.getEditorControl()) {
                private List<INews> fAddedNews;

                @Override
                protected void runInBackground(IProgressMonitor monitor) {
                  fAddedNews = refreshCache(new IMark[] { inputMark }, true);
                }

                @Override
                protected void runInUI(IProgressMonitor monitor) {

                  /* Event not interesting for us or we are disposed */
                  if (fAddedNews == null || fAddedNews.size() == 0)
                    return;

                  /* Ask Group */
                  if (fGrouping.needsRefresh(ISearchMark.class, events, false)) {
                    fFeedView.refresh(true, false);
                    return;
                  }

                  /* Add to Table-Viewer if Visible */
                  if (fFeedView.isTableViewerVisible())
                    fTableViewer.add(fTableViewer.getInput(), fAddedNews.toArray());

                  /* Add to Browser-Viewer if showing entire Feed */
                  if (fBrowserViewer.getInput() instanceof BookMarkReference)
                    fBrowserViewer.add(fBrowserViewer.getInput(), fAddedNews.toArray());
                }
              });

              /* Done */
              return;
            }
          }
        }
      }
    };

    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);

    /* News Listener */
    fNewsListener = new NewsAdapter() {

      /* News got Added */
      @Override
      public void entitiesAdded(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            List<INews> addedNews = null;

            /* Filter News which are from a different Feed than displayed */
            for (NewsEvent event : events) {
              if (isInputRelatedTo(event.getEntity(), EventType.PERSIST)) {
                if (addedNews == null)
                  addedNews = new ArrayList<INews>();

                INews news = event.getEntity();
                addedNews.add(news);

                /* Update Cache */
                synchronized (fCachedNews) {
                  fCachedNews.add(news);
                }
              }
            }

            /* Event not interesting for us or we are disposed */
            if (addedNews == null || addedNews.size() == 0)
              return;

            /* Ask Group */
            if (fGrouping.needsRefresh(INews.class, events, false)) {
              fFeedView.refresh(true, false);
              return;
            }

            /* Add to Table-Viewer if Visible */
            if (fFeedView.isTableViewerVisible())
              fTableViewer.add(fTableViewer.getInput(), addedNews.toArray());

            /* Add to Browser-Viewer if showing entire Feed */
            if (fBrowserViewer.getInput() instanceof BookMarkReference)
              fBrowserViewer.add(fBrowserViewer.getInput(), addedNews.toArray());
          }
        });
      }

      /* News got Updated */
      @Override
      public void entitiesUpdated(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            List<INews> deletedNews = null;
            List<INews> updatedNews = null;

            /* Filter News which are from a different Feed than displayed */
            for (NewsEvent event : events) {
              if (isInputRelatedTo(event.getEntity(), EventType.UPDATE)) {
                INews news = event.getEntity();

                /* News got Deleted */
                if (!news.isVisible()) {
                  if (deletedNews == null)
                    deletedNews = new ArrayList<INews>();
                  deletedNews.add(news);

                  synchronized (fCachedNews) {
                    fCachedNews.remove(news);
                  }
                }

                /* News got Updated */
                else {
                  if (updatedNews == null)
                    updatedNews = new ArrayList<INews>();
                  updatedNews.add(news);
                }
              }
            }

            /* Event not interesting for us or we are disposed */
            if (deletedNews == null && updatedNews == null)
              return;

            /* News got Deleted */
            if (deletedNews != null && !isGroupingEnabled()) {

              /* Remove from Table-Viewer */
              if (fFeedView.isTableViewerVisible())
                fTableViewer.remove(deletedNews.toArray());

              /* Remove from Browser-Viewer */
              if (contains(fBrowserViewer.getInput(), deletedNews))
                fBrowserViewer.remove(deletedNews.toArray());
            }

            /* Ask Group */
            if (fGrouping.needsRefresh(INews.class, events, true))
              fFeedView.refresh(false, false);

            /* News got Updated */
            if (updatedNews != null) {

              /* Update in Table-Viewer */
              if (fFeedView.isTableViewerVisible())
                fTableViewer.update(updatedNews.toArray(), null);

              /* Update in Browser-Viewer */
              if (contains(fBrowserViewer.getInput(), updatedNews))
                fBrowserViewer.update(updatedNews.toArray(), null);
            }
          }
        });
      }

      /* News got Deleted */
      @Override
      public void entitiesDeleted(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            List<INews> deletedNews = null;

            /* Filter News which are from a different Feed or invisible */
            for (NewsEvent event : events) {
              INews news = event.getEntity();
              if (news.isVisible() && isInputRelatedTo(news, EventType.REMOVE)) {
                if (deletedNews == null)
                  deletedNews = new ArrayList<INews>();
                deletedNews.add(news);

                synchronized (fCachedNews) {
                  fCachedNews.remove(news);
                }
              }
            }

            /* Event not interesting for us or we are disposed */
            if (deletedNews == null)
              return;

            /* News got Deleted */
            if (!isGroupingEnabled()) {

              /* Remove from Table-Viewer */
              if (fFeedView.isTableViewerVisible())
                fTableViewer.remove(deletedNews.toArray());

              /* Remove from Browser-Viewer */
              if (contains(fBrowserViewer.getInput(), deletedNews))
                fBrowserViewer.remove(deletedNews.toArray());
            }

            /* Ask Group */
            if (fGrouping.needsRefresh(INews.class, events, false))
              fFeedView.refresh(false, false);
          }
        });
      }
    };

    DynamicDAO.addEntityListener(INews.class, fNewsListener);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
  }

  private boolean isInputRelatedTo(INews news, EventType type) {
    for (IMark mark : fInput) {

      /* Check if BookMark references the News' Feed */
      if (mark instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) mark;
        if (bookmark.getFeedLinkReference().equals(news.getFeedReference()))
          return true;
      }

      /* TODO This is a workaround! */
      else if (type != EventType.PERSIST && mark instanceof ISearchMark) {
        return true;
      }
    }

    return false;
  }

  private boolean contains(Object input, List<INews> list) {

    /* Can only belong to this Feed since filtered before already */
    if (input instanceof BookMarkReference)
      return true;

    /* TODO Handle searchmarks properly */
    else if (input instanceof SearchMarkReference)
      return true;

    else if (input instanceof INews)
      return list.contains(input);

    else if (input instanceof EntityGroup) {
      List<EntityGroupItem> items = ((EntityGroup) input).getItems();
      for (EntityGroupItem item : items) {
        if (list.contains(item.getEntity()))
          return true;
      }
    }

    else if (input instanceof Object[]) {
      Object inputNews[] = (Object[]) input;
      for (Object inputNewsItem : inputNews) {
        if (list.contains(inputNewsItem))
          return true;
      }
    }

    return false;
  }
}