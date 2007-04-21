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

package org.rssowl.ui.internal.views.explorer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.events.BookMarkEvent;
import org.rssowl.core.persist.events.BookMarkListener;
import org.rssowl.core.persist.events.FolderEvent;
import org.rssowl.core.persist.events.FolderListener;
import org.rssowl.core.persist.events.NewsAdapter;
import org.rssowl.core.persist.events.NewsEvent;
import org.rssowl.core.persist.events.NewsListener;
import org.rssowl.core.persist.events.SearchMarkEvent;
import org.rssowl.core.persist.events.SearchMarkListener;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author bpasero
 */
public class BookMarkContentProvider implements ITreeContentProvider {

  /* Delay in ms before updating Selection on Events */
  private static final int SELECTION_DELAY = 20;

  /* Listener */
  private FolderListener fFolderListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkListener fSearchMarkListener;
  private NewsListener fNewsListener;

  /* Viewer Related */
  private IFolder fInput;
  private TreeViewer fViewer;
  private BookMarkFilter fBookmarkFilter;
  private BookMarkGrouping fBookmarkGrouping;

  /*
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof IFolder) {
      IFolder rootFolder = (IFolder) inputElement;

      /* No Grouping */
      if (!fBookmarkGrouping.isActive()) {
        Collection<IEntity> elements = new ArrayList<IEntity>();
        elements.addAll(rootFolder.getFolders());
        elements.addAll(rootFolder.getMarks());

        /* Return Children */
        return elements.toArray();
      }

      /* Grouping Enabled */
      List<IMark> marks = new ArrayList<IMark>();
      getAllMarks(rootFolder, marks);

      return fBookmarkGrouping.group(marks);
    }

    return new Object[0];
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  public Object[] getChildren(Object parentElement) {

    /* Return Children of Folder */
    if (parentElement instanceof IFolder) {
      IFolder parent = (IFolder) parentElement;
      Collection<IEntity> children = new ArrayList<IEntity>();
      children.addAll(parent.getFolders());
      children.addAll(parent.getMarks());

      return children.toArray();
    }

    /* Return Children of Group */
    else if (parentElement instanceof EntityGroup) {
      List<IEntity> children = ((EntityGroup) parentElement).getEntities();
      return children.toArray();
    }

    return new Object[0];
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  public Object getParent(Object element) {

    /* Handle Grouping specially */
    if (fBookmarkGrouping.isActive() && element instanceof IEntity) {
      IEntity entity = (IEntity) element;
      EntityGroup[] groups = fBookmarkGrouping.group(Collections.singletonList(entity));
      if (groups.length == 1)
        return groups[0];
    }

    /* Grouping not enabled */
    else {

      /* Parent Folder of Folder */
      if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;
        return folder.getParent();
      }

      /* Parent Folder of Mark */
      else if (element instanceof IMark) {
        IMark mark = (IMark) element;
        return mark.getFolder();
      }
    }

    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  public boolean hasChildren(Object element) {
    if (element instanceof IFolder) {
      IFolder folder = (IFolder) element;
      return folder.getMarks().size() > 0 || folder.getFolders().size() > 0;
    }

    else if (element instanceof EntityGroup)
      return ((EntityGroup) element).size() > 0;

    return false;
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    unregisterListeners();
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    Assert.isTrue(newInput instanceof IFolder || newInput == null);
    fViewer = (TreeViewer) viewer;
    fInput = (IFolder) newInput;

    /* Register Listeners if Input is available */
    if (newInput != null && oldInput == null)
      registerListeners();

    /* If new Input is NULL, unregister Listeners */
    else if (newInput == null && oldInput != null)
      unregisterListeners();
  }

  /* The ContentProvider needs to know about this Filter */
  void setBookmarkFilter(BookMarkFilter bookmarkFilter) {
    fBookmarkFilter = bookmarkFilter;
  }

  /* The ContentProvider needs to know about this Grouping */
  void setBookmarkGrouping(BookMarkGrouping bookmarkGrouping) {
    fBookmarkGrouping = bookmarkGrouping;
  }

  private void registerListeners() {

    /* Folder Listener */
    fFolderListener = new FolderListener() {

      /* Folders got updated */
      public void entitiesUpdated(final Set<FolderEvent> events) {
        JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
          public void run() {
            Set<IFolder> updatedFolders = null;
            Map<IFolder, IFolder> reparentedFolders = null;

            /* Retrieve Updated Folders */
            for (FolderEvent event : events) {
              if (event.isRoot()) {

                /* Folder got reparented */
                if (event.getOldParent() != null) {
                  if (reparentedFolders == null)
                    reparentedFolders = new HashMap<IFolder, IFolder>();
                  reparentedFolders.put(event.getEntity(), event.getOldParent());
                }

                /* Normal Update */
                else {
                  if (updatedFolders == null)
                    updatedFolders = new HashSet<IFolder>();
                  updatedFolders.add(event.getEntity());
                }
              }
            }

            /* Event not interesting for us or we are disposed */
            if (updatedFolders == null && reparentedFolders == null)
              return;

            /* Ask Filter */
            if (fBookmarkFilter.needsRefresh(IFolder.class, events))
              fViewer.refresh(false);

            /* Ask Group */
            else if (fBookmarkGrouping.needsRefresh(IFolder.class))
              fViewer.refresh(false);

            /* Handle reparented Folders */
            else if (reparentedFolders != null) {
              Set<Entry<IFolder, IFolder>> entries = reparentedFolders.entrySet();
              Set<IFolder> parentsToUpdate = new HashSet<IFolder>();
              List<Object> expandedElements = new ArrayList<Object>(Arrays.asList(fViewer.getExpandedElements()));
              try {
                fViewer.getControl().getParent().setRedraw(false);
                for (Entry<IFolder, IFolder> entry : entries) {
                  IFolder reparentedFolder = entry.getKey();
                  IFolder oldParent = entry.getValue();

                  /* Reparent while keeping the Selection / Expansion */
                  ISelection selection = fViewer.getSelection();
                  boolean expand = expandedElements.contains(reparentedFolder);
                  fViewer.remove(oldParent, new Object[] { reparentedFolder });
                  fViewer.refresh(reparentedFolder.getParent(), false);
                  fViewer.setSelection(selection);

                  if (expand)
                    fViewer.setExpandedState(reparentedFolder, expand);

                  /* Remember to update parents */
                  parentsToUpdate.add(oldParent);
                  parentsToUpdate.add(reparentedFolder.getParent());
                }
              } finally {
                fViewer.getControl().getParent().setRedraw(true);
              }

              /* Update old Parents of Reparented Bookmarks */
              for (IFolder folder : parentsToUpdate)
                updateFolderAndParents(folder);
            }

            /* Handle Updated Folders */
            if (updatedFolders != null) {
              for (IFolder folder : updatedFolders) {
                if (fInput.equals(folder))
                  fViewer.refresh(fInput);
                else
                  fViewer.refresh(folder);
              }
            }
          }
        });
      }

      /* Folders got deleted */
      public void entitiesDeleted(final Set<FolderEvent> events) {
        JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
          public void run() {

            /* Retrieve Removed Folders */
            Set<IFolder> removedFolders = null;
            for (FolderEvent event : events) {
              if (event.isRoot() && event.getEntity().getParent() != null) {
                if (removedFolders == null)
                  removedFolders = new HashSet<IFolder>();
                removedFolders.add(event.getEntity());
              }
            }

            /* Event not interesting for us or we are disposed */
            if (removedFolders == null || removedFolders.size() == 0)
              return;

            /* Ask Filter */
            if (fBookmarkFilter.needsRefresh(IFolder.class, events))
              fViewer.refresh(false);

            /* Ask Group */
            else if (fBookmarkGrouping.needsRefresh(IFolder.class))
              fViewer.refresh(false);

            /* React normally then */
            else
              fViewer.remove(removedFolders.toArray());

            /* Update Read-State counters on Parents */
            if (!fBookmarkGrouping.isActive()) {
              for (FolderEvent event : events) {
                IFolder eventParent = event.getEntity().getParent();
                if (eventParent != null && eventParent.getParent() != null)
                  updateFolderAndParents(eventParent);
              }
            }
          }
        });
      }

      /* Folders got added */
      public void entitiesAdded(final Set<FolderEvent> events) {
        JobRunner.runInUIThread(SELECTION_DELAY, fViewer.getControl(), new Runnable() {
          public void run() {

            /* Reveal and Select added Folders */
            final List<IFolder> addedFolders = new ArrayList<IFolder>();
            for (FolderEvent folderEvent : events) {
              IFolder addedFolder = folderEvent.getEntity();
              if (addedFolder.getParent() != null)
                addedFolders.add(addedFolder);
            }

            if (!addedFolders.isEmpty())
              fViewer.setSelection(new StructuredSelection(addedFolders), true);
          }
        });
      }
    };

    /* BookMark Listener */
    fBookMarkListener = new BookMarkListener() {

      /* BookMarks got Updated */
      public void entitiesUpdated(final Set<BookMarkEvent> events) {
        JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
          public void run() {
            Set<IBookMark> updatedBookMarks = null;
            Map<IBookMark, IFolder> reparentedBookMarks = null;

            /* Retrieve Updated BookMarks */
            for (BookMarkEvent event : events) {
              if (event.isRoot()) {

                /* BookMark got reparented */
                if (event.getOldParent() != null) {
                  if (reparentedBookMarks == null)
                    reparentedBookMarks = new HashMap<IBookMark, IFolder>();
                  reparentedBookMarks.put(event.getEntity(), event.getOldParent());
                }

                /* Normal Update */
                else {
                  if (updatedBookMarks == null)
                    updatedBookMarks = new HashSet<IBookMark>();
                  updatedBookMarks.add(event.getEntity());
                }
              }
            }

            /* Event not interesting for us or we are disposed */
            if (updatedBookMarks == null && reparentedBookMarks == null)
              return;

            /* Ask Filter */
            if (fBookmarkFilter.needsRefresh(IBookMark.class, events))
              fViewer.refresh(false);

            /* Ask Group */
            else if (fBookmarkGrouping.needsRefresh(IBookMark.class))
              fViewer.refresh(false);

            /* Handle reparented Folders */
            else if (reparentedBookMarks != null) {
              Set<Entry<IBookMark, IFolder>> entries = reparentedBookMarks.entrySet();
              Set<IFolder> parentsToUpdate = new HashSet<IFolder>();
              try {
                fViewer.getControl().getParent().setRedraw(false);
                for (Entry<IBookMark, IFolder> entry : entries) {
                  IBookMark reparentedBookMark = entry.getKey();
                  IFolder oldParent = entry.getValue();

                  /* Reparent while keeping the Selection */
                  ISelection selection = fViewer.getSelection();
                  fViewer.remove(oldParent, new Object[] { reparentedBookMark });
                  fViewer.refresh(reparentedBookMark.getFolder(), false);
                  fViewer.setSelection(selection);

                  /* Remember to update parents */
                  parentsToUpdate.add(oldParent);
                  parentsToUpdate.add(reparentedBookMark.getFolder());
                }
              } finally {
                fViewer.getControl().getParent().setRedraw(true);
              }

              /* Update old Parents of Reparented Bookmarks */
              for (IFolder folder : parentsToUpdate)
                updateFolderAndParents(folder);
            }

            /* Handle Updated Folders */
            if (updatedBookMarks != null)
              fViewer.update(updatedBookMarks.toArray(), null);
          }
        });
      }

      /* BookMarks got Deleted */
      public void entitiesDeleted(final Set<BookMarkEvent> events) {
        JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
          public void run() {

            /* Retrieve Removed BookMarks */
            Set<IBookMark> removedBookMarks = null;
            for (BookMarkEvent event : events) {
              if (event.isRoot()) {
                if (removedBookMarks == null)
                  removedBookMarks = new HashSet<IBookMark>();
                removedBookMarks.add(event.getEntity());
              }
            }

            /* Event not interesting for us or we are disposed */
            if (removedBookMarks == null || removedBookMarks.size() == 0)
              return;

            /* Ask Filter */
            if (fBookmarkFilter.needsRefresh(IBookMark.class, events))
              fViewer.refresh(false);

            /* Ask Group */
            else if (fBookmarkGrouping.needsRefresh(IBookMark.class))
              fViewer.refresh(false);

            /* React normally then */
            else
              fViewer.remove(removedBookMarks.toArray());

            /* Update Read-State counters on Parents */
            if (!fBookmarkGrouping.isActive()) {
              for (BookMarkEvent event : events) {
                IFolder eventParent = event.getEntity().getFolder();
                if (eventParent != null && eventParent.getParent() != null)
                  updateFolderAndParents(eventParent);
              }
            }
          }
        });
      }

      /* BookMarks got Added */
      public void entitiesAdded(Set<BookMarkEvent> events) {

        /* Reveal and Select if single Entity added */
        if (events.size() == 1) {
          final BookMarkEvent event = events.iterator().next();
          JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
            public void run() {
              expand(event.getEntity().getFolder());
            }
          });
        }
      }
    };

    {
      /* SearchMark Listener */
      fSearchMarkListener = new SearchMarkListener() {

        /* SearchMarks got Updated */
        public void entitiesUpdated(final Set<SearchMarkEvent> events) {
          JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
            public void run() {
              Set<ISearchMark> updatedSearchMarks = null;
              Map<ISearchMark, IFolder> reparentedSearchMarks = null;

              /* Retrieve Updated SearchMarks */
              for (SearchMarkEvent event : events) {
                if (event.isRoot()) {

                  /* SearchMark got reparented */
                  if (event.getOldParent() != null) {
                    if (reparentedSearchMarks == null)
                      reparentedSearchMarks = new HashMap<ISearchMark, IFolder>();
                    reparentedSearchMarks.put(event.getEntity(), event.getOldParent());
                  }

                  /* Normal Update */
                  else {
                    if (updatedSearchMarks == null)
                      updatedSearchMarks = new HashSet<ISearchMark>();
                    updatedSearchMarks.add(event.getEntity());
                  }
                }
              }

              /* Event not interesting for us or we are disposed */
              if (updatedSearchMarks == null && reparentedSearchMarks == null)
                return;

              /* Ask Filter */
              if (fBookmarkFilter.needsRefresh(ISearchMark.class, events))
                fViewer.refresh(false);

              /* Ask Group */
              else if (fBookmarkGrouping.needsRefresh(ISearchMark.class))
                fViewer.refresh(false);

              /* Handle reparented Folders */
              else if (reparentedSearchMarks != null) {
                Set<Entry<ISearchMark, IFolder>> entries = reparentedSearchMarks.entrySet();
                try {
                  fViewer.getControl().getParent().setRedraw(false);
                  for (Entry<ISearchMark, IFolder> entry : entries) {
                    ISearchMark reparentedSearchMark = entry.getKey();
                    IFolder oldParent = entry.getValue();

                    /* Reparent while keeping the Selection */
                    ISelection selection = fViewer.getSelection();
                    fViewer.remove(oldParent, new Object[] { reparentedSearchMark });
                    fViewer.refresh(reparentedSearchMark.getFolder(), false);
                    fViewer.setSelection(selection);
                  }
                } finally {
                  fViewer.getControl().getParent().setRedraw(true);
                }
              }

              /* Handle Updated Searchmarks */
              if (updatedSearchMarks != null)
                fViewer.update(updatedSearchMarks.toArray(), null);
            }
          });
        }

        /* SearchMarks got Deleted */
        public void entitiesDeleted(final Set<SearchMarkEvent> events) {
          JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
            public void run() {
              Set<ISearchMark> removedSearchMarks = null;

              /* Retrieve Removed SearchMarks */
              for (SearchMarkEvent event : events) {
                if (event.isRoot()) {
                  if (removedSearchMarks == null)
                    removedSearchMarks = new HashSet<ISearchMark>();
                  removedSearchMarks.add(event.getEntity());
                }
              }

              /* Event not interesting for us or we are disposed */
              if (removedSearchMarks == null || removedSearchMarks.size() == 0)
                return;

              /* Ask Filter */
              if (fBookmarkFilter.needsRefresh(ISearchMark.class, events))
                fViewer.refresh(false);

              /* Ask Group */
              else if (fBookmarkGrouping.needsRefresh(ISearchMark.class))
                fViewer.refresh(false);

              /* React normally then */
              else
                fViewer.remove(removedSearchMarks.toArray());
            }
          });
        }

        /* SearchMarks got Added */
        public void entitiesAdded(Set<SearchMarkEvent> events) {

          /* Reveal and Select if single Entity added */
          if (events.size() == 1) {
            final SearchMarkEvent event = events.iterator().next();
            JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
              public void run() {
                expand(event.getEntity().getFolder());
              }
            });
          }
        }
      };

      /* News Listener */
      fNewsListener = new NewsAdapter() {

        @Override
        public void entitiesAdded(final Set<NewsEvent> events) {
          JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
            public void run() {

              /* Ask Filter */
              if (fBookmarkFilter.needsRefresh(INews.class, events))
                fViewer.refresh(false);

              /* Ask Group */
              else if (fBookmarkGrouping.needsRefresh(INews.class))
                fViewer.refresh(false);

              /* Updated affected Types on read-state if required */
              if (requiresUpdate(events))
                updateParents(events);
            }
          });
        }

        @Override
        public void entitiesUpdated(final Set<NewsEvent> events) {
          JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
            public void run() {

              /* Ask Filter */
              if (fBookmarkFilter.needsRefresh(INews.class, events))
                fViewer.refresh(false);

              /* Ask Group */
              else if (fBookmarkGrouping.needsRefresh(INews.class))
                fViewer.refresh(false);

              /* Updated affected Types on read-state if required */
              if (requiresUpdate(events))
                updateParents(events);
            }
          });
        }
      };
    }

    /* Register Listeners */
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);
    DynamicDAO.addEntityListener(INews.class, fNewsListener);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
  }

  /* Update Entities that are affected by the given NewsEvents */
  private void updateParents(final Set<NewsEvent> events) {

    /* Group by Feed */
    Set<FeedLinkReference> affectedFeeds = new HashSet<FeedLinkReference>();
    for (NewsEvent event : events)
      affectedFeeds.add(event.getEntity().getFeedReference());

    /* Update related Entities */
    for (FeedLinkReference feedRef : affectedFeeds)
      updateParents(feedRef);
  }

  private void updateParents(FeedLinkReference feedRef) throws PersistenceException {

    /* Collect all affected BookMarks */
    Set<IBookMark> affectedBookMarks = Controller.getDefault().getCacheService().getBookMarks(feedRef);

    /* Update them including Parents */
    updateMarksAndParents(affectedBookMarks);
  }

  private void updateMarksAndParents(Set<IBookMark> bookmarks) {
    Set<IEntity> entitiesToUpdate = new HashSet<IEntity>();
    entitiesToUpdate.addAll(bookmarks);

    /* Collect parents */
    for (IBookMark bookmark : bookmarks) {
      List<IFolder> visibleParents = new ArrayList<IFolder>();
      collectParents(visibleParents, bookmark);

      entitiesToUpdate.addAll(visibleParents);
    }

    /* Update Entities */
    fViewer.update(entitiesToUpdate.toArray(), null);
  }

  private void collectParents(List<IFolder> parents, IEntity entity) {

    /* Determine Parent Folder */
    IFolder parent = null;
    if (entity instanceof IMark)
      parent = ((IMark) entity).getFolder();
    else if (entity instanceof IFolder)
      parent = ((IFolder) entity).getParent();

    /* Root reached */
    if (parent == null)
      return;

    /* Input reached */
    if (fInput.equals(parent))
      return;

    /* Check parent visible */
    parents.add(parent);

    /* Recursively collect visible parents */
    collectParents(parents, parent);
  }

  private void updateFolderAndParents(IFolder folder) {
    Set<IEntity> entitiesToUpdate = new HashSet<IEntity>();
    entitiesToUpdate.add(folder);

    /* Collect parents */
    List<IFolder> parents = new ArrayList<IFolder>();
    collectParents(parents, folder);
    entitiesToUpdate.addAll(parents);

    /* Update Entities */
    fViewer.update(entitiesToUpdate.toArray(), null);
  }

  private void getAllMarks(IFolder folder, List<IMark> marks) {

    /* Add all Marks */
    marks.addAll(folder.getMarks());

    /* Go through Subfolders */
    List<IFolder> folders = folder.getFolders();
    for (IFolder childFolder : folders)
      getAllMarks(childFolder, marks);
  }

  private boolean requiresUpdate(Set<NewsEvent> events) {
    for (NewsEvent newsEvent : events) {

      /* Check Change in New-State */
      boolean oldStateNew = INews.State.NEW.equals(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
      boolean currentStateNew = INews.State.NEW.equals(newsEvent.getEntity().getState());
      if (oldStateNew != currentStateNew)
        return true;

      /* Check Change in Read-State */
      boolean oldStateUnread = ModelUtils.isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
      boolean newStateUnread = ModelUtils.isUnread(newsEvent.getEntity().getState());
      if (oldStateUnread != newStateUnread)
        return true;

      /* Check Change in Sticky-State */
      boolean oldStateSticky = newsEvent.getOldNews() != null ? newsEvent.getOldNews().isFlagged() : false;
      boolean newStateSticky = newsEvent.getEntity().isFlagged();
      if (oldStateSticky != newStateSticky)
        return true;
    }

    return false;
  }

  private void expand(IFolder folder) {
    IFolder parent = folder.getParent();
    if (parent != null)
      expand(parent);

    fViewer.setExpandedState(folder, true);
  }
}