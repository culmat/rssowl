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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.ShareProvider;
import org.rssowl.ui.internal.StatusLineUpdater;
import org.rssowl.ui.internal.actions.AssignLabelsAction;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.actions.SendLinkAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.dialogs.preferences.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.dialogs.preferences.SharingPreferencesPage;
import org.rssowl.ui.internal.editors.browser.WebBrowserContext;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.CTree;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.JobTracker;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Part of the FeedView to display News in a TableViewer.
 *
 * @author bpasero
 */
public class NewsTableControl implements IFeedViewPart {

  /* Flag to enable / disable COD */
  static final boolean USE_CUSTOM_OWNER_DRAWN = true;

  /* Tracker to Mark selected news as Read */
  private class MarkReadTracker extends JobTracker {
    private boolean fUpdateDelayDynamically;

    MarkReadTracker(int delay, boolean showProgress) {
      super(delay, showProgress, ITask.Priority.INTERACTIVE);
    }

    @Override
    public int getDelay() {
      if (fUpdateDelayDynamically)
        return fInputPreferences.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS);

      return super.getDelay();
    }

    public void setUpdateDelayDynamically(boolean updateDelayDynamically) {
      fUpdateDelayDynamically = updateDelayDynamically;
    }
  }

  /* Custom Tooltip Support for Feed Column */
  private static class FeedColumnToolTipSupport extends ColumnViewerToolTipSupport {
    FeedColumnToolTipSupport(ColumnViewer viewer, int style) {
      super(viewer, style, false);
    }

    /*
     * @see org.eclipse.jface.viewers.ColumnViewerToolTipSupport#getToolTipArea(org.eclipse.swt.widgets.Event)
     */
    @Override
    protected Object getToolTipArea(Event event) {
      Tree tree = (Tree) event.widget;
      Point point = new Point(event.x, event.y);
      TreeItem item = tree.getItem(point);

      /* Only valid for Feed Column */
      if (item != null) {
        int feedIndex = indexOf(tree, NewsColumn.FEED);
        if (feedIndex >= 0 && item.getBounds(feedIndex).contains(point))
          return super.getToolTipArea(event);
      }

      return null;
    }

    private static int indexOf(Tree tree, NewsColumn column) {
      if (tree.isDisposed())
        return -1;

      TreeColumn[] columns = tree.getColumns();
      for (int i = 0; i < columns.length; i++) {
        if (column == columns[i].getData(NewsColumnViewModel.COL_ID))
          return i;
      }

      return -1;
    }

    public static void enableFor(ColumnViewer viewer) {
      new FeedColumnToolTipSupport(viewer, ToolTip.NO_RECREATE);
    }
  }

  private IEditorSite fEditorSite;
  private MarkReadTracker fNewsStateTracker;
  private MarkReadTracker fInstantMarkUnreadTracker;
  private NewsTableViewer fViewer;
  private NewsTableLabelProvider fNewsTableLabelProvider;
  private ISelectionChangedListener fSelectionChangeListener;
  private IPropertyChangeListener fPropertyChangeListener;
  private CTree fCustomTree;
  private int[] fOldColumnOrder;
  private LocalResourceManager fResources;
  private NewsComparator fNewsSorter;
  private Cursor fHandCursor;
  private boolean fShowsHandCursor;
  private final AtomicBoolean fBlockNewsStateTracker = new AtomicBoolean(false);
  private LabelAdapter fLabelListener;
  private IPreferenceScope fInputPreferences;
  private final INewsDAO fNewsDao = Owl.getPersistenceService().getDAOService().getNewsDAO();
  private NewsColumnViewModel fColumnModel;
  private FeedViewInput fEditorInput;
  private boolean fBlockColumMoveEvent;
  private IStructuredSelection fLastSelection = StructuredSelection.EMPTY;

  /* Settings */
  private IPreferenceScope fGlobalPreferences;

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#init(org.eclipse.ui.IEditorSite)
   */
  public void init(IEditorSite editorSite) {
    fEditorSite = editorSite;
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fInstantMarkUnreadTracker = new MarkReadTracker(0, false);
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#onInputChanged(org.rssowl.ui.internal.editors.feed.FeedViewInput)
   */
  public void onInputChanged(FeedViewInput input) {
    fEditorInput = input;
    fInputPreferences = Owl.getPreferenceService().getEntityScope(input.getMark());

    if (fNewsStateTracker != null)
      fNewsStateTracker.cancel();

    fInstantMarkUnreadTracker.cancel();

    fNewsStateTracker = new MarkReadTracker(fInputPreferences.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS), false);
    fNewsStateTracker.setUpdateDelayDynamically(true);
  }

  IPreferenceScope getInputPreferences() {
    return fInputPreferences;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#createViewer(org.eclipse.swt.widgets.Composite)
   */
  public NewsTableViewer createViewer(Composite parent) {
    int style = SWT.MULTI | SWT.FULL_SELECTION;

    fCustomTree = new CTree(parent, style);
    fCustomTree.getControl().setHeaderVisible(true);

    fViewer = new NewsTableViewer(fCustomTree.getControl());
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.setUseHashlookup(true);
    fViewer.getControl().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());
    fViewer.getControl().setFont(OwlUI.getThemeFont(OwlUI.HEADLINES_FONT_ID, SWT.NORMAL));

    /* Custom Tooltip for Feed Column */
    FeedColumnToolTipSupport.enableFor(fViewer);

    /* This is a Workaround until we remember expanded Groups */
    fViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

    fHandCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

    /* Drag and Drop */
    initDragAndDrop();

    return fViewer;
  }

  /**
   * return the last selection in the table viewer.
   */
  IStructuredSelection getLastSelection() {
    return fLastSelection;
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE;
    Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance(), URLTransfer.getInstance() };

    /* Drag Support */
    fViewer.addDragSupport(ops, transfers, new DragSourceListener() {
      public void dragStart(final DragSourceEvent event) {
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            LocalSelectionTransfer.getTransfer().setSelection(fViewer.getSelection());
            LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
            event.doit = true;
          }
        });
      }

      public void dragSetData(final DragSourceEvent event) {
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {

            /* Set Selection using LocalSelectionTransfer */
            if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType))
              event.data = LocalSelectionTransfer.getTransfer().getSelection();

            /* Set Text using Text- or URLTransfer */
            else if (TextTransfer.getInstance().isSupportedType(event.dataType) || URLTransfer.getInstance().isSupportedType(event.dataType))
              setTextData(event);
          }
        });
      }

      public void dragFinished(DragSourceEvent event) {
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            LocalSelectionTransfer.getTransfer().setSelection(null);
            LocalSelectionTransfer.getTransfer().setSelectionSetTime(0);
          }
        });
      }
    });
  }

  private void setTextData(DragSourceEvent event) {
    IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
    Set<INews> news = ModelUtils.normalize(selection.toList());

    if (!news.isEmpty()) {
      String linkAsText = CoreUtils.getLink(news.iterator().next());
      if (StringUtils.isSet(linkAsText))
        event.data = linkAsText;
    }
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#getViewer()
   */
  public NewsTableViewer getViewer() {
    return fViewer;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#initViewer(org.eclipse.jface.viewers.IStructuredContentProvider,
   * org.eclipse.jface.viewers.ViewerFilter)
   */
  public void initViewer(IStructuredContentProvider contentProvider, ViewerFilter filter) {

    /* Apply ContentProvider */
    fViewer.setContentProvider(contentProvider);

    /* Create LabelProvider (Custom Owner Drawn enabled!) */
    NewsColumnViewModel columnModel = createColumnModel(fEditorInput.getMark());
    fNewsTableLabelProvider = new NewsTableLabelProvider(columnModel);
    if (USE_CUSTOM_OWNER_DRAWN) {
      fViewer.getControl().addListener(SWT.EraseItem, new Listener() {
        public void handleEvent(Event event) {
          Object element = event.item.getData();
          fNewsTableLabelProvider.erase(event, element);
        }
      });
    }

    /* Create Sorter */
    fNewsSorter = new NewsComparator();
    fViewer.setComparator(fNewsSorter);

    /* Set Comparer */
    fViewer.setComparer(getComparer());

    /* Create and Show Columns */
    showColumns(columnModel, false, false);

    /* Add Filter */
    fViewer.addFilter(filter);

    /* Hook Contextual Menu */
    hookContextualMenu();

    /* Register Listeners */
    registerListeners();

    /* Propagate Selection Events */
    fEditorSite.setSelectionProvider(fViewer);
  }

  /* Called when column settings have been updated */
  void updateColumns(Object input) {
    showColumns(createColumnModel(input), true, true);
  }

  private NewsColumnViewModel createColumnModel(Object input) {
    NewsColumnViewModel model;
    if (input instanceof IFolderChild)
      model = NewsColumnViewModel.loadFrom(Owl.getPreferenceService().getEntityScope((IEntity) input));
    else
      model = NewsColumnViewModel.createGlobal();

    /* Synthetically add the "Feed" column if both "Feed" and "Location" not present */
    if ((input instanceof ISearchMark) || (input instanceof INewsBin) || (input instanceof FolderNewsMark)) {
      if (!model.getColumns().contains(NewsColumn.FEED) && !model.getColumns().contains(NewsColumn.LOCATION)) {
        model.getColumns().add(1, NewsColumn.FEED);
      }
    }

    return model;
  }

  private void showColumns(NewsColumnViewModel newModel, boolean update, boolean refresh) {
    if (fCustomTree.getControl().isDisposed())
      return;

    /* Return early if no change is required */
    if (newModel.equals(fColumnModel))
      return;

    /* Dispose Old */
    fBlockColumMoveEvent = true;
    try {
      fCustomTree.clear();
    } finally {
      fBlockColumMoveEvent = false;
    }

    /* Keep as current */
    fColumnModel = newModel;

    /* Create Columns */
    List<NewsColumn> cols = newModel.getColumns();
    for (int i = 0; i < cols.size(); i++) {
      NewsColumn col = cols.get(i);
      TreeViewerColumn viewerColumn = new TreeViewerColumn(fViewer, SWT.LEFT);
      fCustomTree.manageColumn(viewerColumn.getColumn(), newModel.getLayoutData(col), col.showName() ? col.getName() : null, col.showTooltip() ? col.getName() : null, null, col.isMoveable(), col.isResizable());
      if (i == 0)
        viewerColumn.getColumn().setResizable(true); //Need to override this due to bug on windows
      viewerColumn.getColumn().setData(NewsColumnViewModel.COL_ID, col);

      if (newModel.getSortColumn() == col && col.showSortIndicator()) {
        fCustomTree.getControl().setSortColumn(viewerColumn.getColumn());
        fCustomTree.getControl().setSortDirection(newModel.isAscending() ? SWT.UP : SWT.DOWN);
      }
    }

    /* Remember Column Order */
    fOldColumnOrder = fCustomTree.getControl().getColumnOrder();

    /* Update Tree */
    if (update)
      fCustomTree.update();

    /* Update Sorter */
    fNewsSorter.setAscending(newModel.isAscending());
    fNewsSorter.setSortBy(newModel.getSortColumn());

    /* Set Label Provider */
    fNewsTableLabelProvider.init(newModel);
    fViewer.setLabelProvider(fNewsTableLabelProvider);

    /* Refresh if necessary */
    if (refresh)
      fViewer.refresh(true);

    /* Enable Sorting adding listeners to Columns */
    TreeColumn[] columns = fCustomTree.getControl().getColumns();
    for (final TreeColumn column : columns) {
      column.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          NewsColumn oldSortBy = fNewsSorter.getSortBy();
          NewsColumn newSortBy = (NewsColumn) column.getData(NewsColumnViewModel.COL_ID);
          boolean defaultAscending = newSortBy.prefersAscending();
          boolean ascending = (oldSortBy != newSortBy) ? defaultAscending : !fNewsSorter.isAscending();

          /* Update Model */
          fColumnModel.setSortColumn(newSortBy);
          fColumnModel.setAscending(ascending);

          /* Update Sorter */
          fNewsSorter.setSortBy(newSortBy);
          fNewsSorter.setAscending(ascending);

          /* Indicate Sort-Column in UI for Columns that have a certain width */
          if (newSortBy.showSortIndicator()) {
            fCustomTree.getControl().setSortColumn(column);
            fCustomTree.getControl().setSortDirection(ascending ? SWT.UP : SWT.DOWN);
          } else {
            fCustomTree.getControl().setSortColumn(null);
          }

          /* Refresh UI */
          fViewer.refresh(false);

          /* Save Column Model in Background */
          saveColumnModelInBackground();
        }
      });

      /* Listen to moved columns */
      column.addListener(SWT.Move, new Listener() {
        public void handleEvent(Event event) {
          if (fCustomTree.getControl().isDisposed() || fBlockColumMoveEvent)
            return;

          int[] columnOrder = fCustomTree.getControl().getColumnOrder();
          if (!Arrays.equals(fOldColumnOrder, columnOrder)) {

            /* Remember Old */
            fOldColumnOrder = columnOrder;

            /* Create Column Model from Control */
            NewsColumnViewModel currentModel = NewsColumnViewModel.initializeFrom(fCustomTree.getControl());
            currentModel.setSortColumn(fNewsSorter.getSortBy());
            currentModel.setAscending(fNewsSorter.isAscending());

            /* Save in case the model changed */
            if (!currentModel.equals(fColumnModel)) {
              fColumnModel = currentModel;
              saveColumnModelInBackground();
            }
          }
        }
      });
    }
  }

  private void registerListeners() {

    /* Open selected News Links in Browser on doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onMouseDoubleClick(event);
      }
    });

    /* Hook into Statusline */
    fViewer.addSelectionChangedListener(new StatusLineUpdater(fEditorSite.getActionBars().getStatusLineManager()));

    /* Track Selections in the Viewer */
    fSelectionChangeListener = new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        onSelectionChanged(event);
      }
    };
    fViewer.addSelectionChangedListener(fSelectionChangeListener);

    /* Perform Action on Mouse-Down */
    fCustomTree.getControl().addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        onMouseDown(event);
      }
    });

    /* Perform Action on Mouse-Up */
    fCustomTree.getControl().addListener(SWT.MouseUp, new Listener() {
      public void handleEvent(Event event) {
        onMouseUp(event);
      }
    });

    /* Update Cursor on Mouse-Move */
    fCustomTree.getControl().addListener(SWT.MouseMove, new Listener() {
      public void handleEvent(Event event) {
        onMouseMove(event);
      }
    });

    /* Redraw on Label update */
    fLabelListener = new LabelAdapter() {
      @Override
      public void entitiesUpdated(Set<LabelEvent> events) {
        JobRunner.runInUIThread(fViewer.getTree(), new Runnable() {
          public void run() {
            fViewer.refresh(true);
          }
        });
      }
    };
    DynamicDAO.addEntityListener(ILabel.class, fLabelListener);

    /* Refresh Viewer when Sticky Color Changes */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (fViewer.getControl().isDisposed())
          return;

        if (OwlUI.STICKY_BG_COLOR_ID.equals(event.getProperty())) {
          ((NewsTableLabelProvider) fViewer.getLabelProvider()).updateResources();
          fViewer.refresh(true);
        }
      }
    };
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  private void onMouseDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (selection.isEmpty())
      return;

    Object firstElem = selection.getFirstElement();

    /* Open News */
    if (firstElem instanceof INews)
      new OpenInBrowserAction(selection, WebBrowserContext.createFrom((INews) firstElem, fEditorInput.getMark())).run();

    /* Toggle expanded State of Group */
    else if (firstElem instanceof EntityGroup)
      fViewer.setExpandedState(firstElem, !fViewer.getExpandedState(firstElem));
  }

  private void onSelectionChanged(SelectionChangedEvent event) {

    /* Only consider Structured Selections */
    if (!(event.getSelection() instanceof IStructuredSelection))
      return;

    /* Remember */
    fLastSelection = (IStructuredSelection) event.getSelection();

    /* Check Flag */
    if (fBlockNewsStateTracker.get())
      return;

    /* Retrieve all NewsReferences of the Selection */
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();

    /* Only responsible for single Selection of a News */
    if (selection.size() != 1 || !(selection.getFirstElement() instanceof INews)) {
      fNewsStateTracker.cancel();
      fInstantMarkUnreadTracker.cancel();
      return;
    }

    /* Trigger the Tracker if news is not read already */
    final INews selectedNews = (INews) selection.getFirstElement();
    if (selectedNews.getState() != INews.State.READ && selectedNews.isVisible()) {
      final boolean markRead = fInputPreferences.getBoolean(DefaultPreferences.MARK_READ_STATE);
      final int delay = fNewsStateTracker.getDelay();

      /* Instantly mark as *unread* if required */
      if ((!markRead || delay > 0) && selectedNews.getState() != INews.State.UNREAD) {
        fInstantMarkUnreadTracker.run(new TaskAdapter() {
          public IStatus run(IProgressMonitor monitor) {
            setNewsState(selectedNews, INews.State.UNREAD, true);
            return Status.OK_STATUS;
          }
        });
      }

      /* Instantly Mark Read (see Bug 1023) */
      if (markRead && delay == 0)
        setNewsState(selectedNews, INews.State.READ, false);

      /* Mark Read after Delay */
      else if (markRead) {
        fNewsStateTracker.run(new TaskAdapter() {
          public IStatus run(IProgressMonitor monitor) {
            setNewsState(selectedNews, INews.State.READ, true);
            return Status.OK_STATUS;
          }
        });
      }
    }

    /* Cancel any possible running JobTracker */
    else if (selectedNews.getState() == INews.State.READ) {
      fNewsStateTracker.cancel();
      fInstantMarkUnreadTracker.cancel();
    }
  }

  private void onMouseUp(Event event) {

    /* Middle Mouse Button pressed */
    if (event.button == 2) {
      Point p = new Point(event.x, event.y);
      TreeItem item = fCustomTree.getControl().getItem(p);

      /* Problem - return */
      if (item == null || item.isDisposed())
        return;

      /* Open News */
      Object element = item.getData();
      if (element instanceof INews)
        new OpenInBrowserAction(new StructuredSelection(element), WebBrowserContext.createFrom((INews) element, fEditorInput.getMark())).run();
    }
  }

  private void onMouseDown(Event event) {
    boolean disableTrackerTemporary = false;
    Point p = new Point(event.x, event.y);
    TreeItem item = fCustomTree.getControl().getItem(p);

    /* Problem - return */
    if (item == null || item.isDisposed())
      return;

    /* Mouse-Up over Read-State-Column */
    if (event.button == 1 && isInImageBounds(item, NewsColumn.TITLE, p)) {
      Object data = item.getData();

      /* Toggle State between Read / Unread */
      if (data instanceof INews) {
        INews news = (INews) data;
        disableTrackerTemporary = (news.getState() == INews.State.READ);
        INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD : INews.State.READ;
        setNewsState(news, newState, false);
      }
    }

    /* Mouse-Up over Sticky-State-Column */
    else if (event.button == 1 && isInImageBounds(item, NewsColumn.STICKY, p)) {
      Object data = item.getData();

      /* Toggle State between Sticky / Not Sticky */
      if (data instanceof INews) {
        disableTrackerTemporary = false;
        new MakeNewsStickyAction(new StructuredSelection(data)).run();
      }
    }

    /*
     * This is a workaround: Immediately after the mouse-down-event has been
     * issued, a selection-event is triggered. This event is resulting in the
     * news-state-tracker to run and mark the selected news as read again. To
     * avoid this, we disable the tracker for a short while and set it back to
     * enabled again.
     */
    if (disableTrackerTemporary)
      JobRunner.runDelayedFlagInversion(200, fBlockNewsStateTracker);
  }

  private void onMouseMove(Event event) {
    Point p = new Point(event.x, event.y);
    TreeItem item = fCustomTree.getControl().getItem(p);

    /* Problem / Group hovered - reset */
    if (item == null || item.isDisposed() || item.getData() instanceof EntityGroup) {
      if (fShowsHandCursor && !fCustomTree.getControl().isDisposed()) {
        fCustomTree.getControl().setCursor(null);
        fShowsHandCursor = false;
      }
      return;
    }

    /* Show Hand-Cursor if action can be performed */
    boolean changeToHandCursor = isInImageBounds(item, NewsColumn.TITLE, p) || isInImageBounds(item, NewsColumn.STICKY, p);
    if (!fShowsHandCursor && changeToHandCursor) {
      fCustomTree.getControl().setCursor(fHandCursor);
      fShowsHandCursor = true;
    } else if (fShowsHandCursor && !changeToHandCursor) {
      fCustomTree.getControl().setCursor(null);
      fShowsHandCursor = false;
    }
  }

  /*
   * This Comparer is used to optimize some operations on the Viewer being used.
   * When deleting Entities, the Delete-Event is providing a reference to the
   * deleted Entity, which can not be resolved anymore. This Comparer will
   * return <code>TRUE</code> for a reference compared with an Entity that has
   * the same ID and is belonging to the same Entity. At any time, it _must_ be
   * avoided to call add, update or refresh with passing in a Reference!
   */
  private IElementComparer getComparer() {
    return new IElementComparer() {
      public boolean equals(Object a, Object b) {

        /* Quickyly check this common case */
        if (a == b)
          return true;

        if (a instanceof ModelReference && b instanceof IEntity)
          return ((ModelReference) a).references((IEntity) b);

        if (b instanceof ModelReference && a instanceof IEntity)
          return ((ModelReference) b).references((IEntity) a);

        return a.equals(b);
      }

      public int hashCode(Object element) {
        return element.hashCode();
      }
    };
  }

  private void hookContextualMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        final IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

        /* Open */
        {
          manager.add(new Separator("open"));

          /* Show only when internal browser is used */
          if (!selection.isEmpty() && !fGlobalPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER) && !fGlobalPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
            manager.add(new OpenInExternalBrowserAction(selection));
        }

        /* Attachments */
        {
          OwlUI.fillAttachmentsMenu(manager, selection, new SameShellProvider(fViewer.getTree().getShell()), false);
        }

        /* Mark / Label */
        {
          manager.add(new Separator("mark"));

          /* Mark */
          MenuManager markMenu = new MenuManager("Mark", "mark");
          manager.add(markMenu);

          /* Mark as Read */
          IAction action = new ToggleReadStateAction(selection);
          action.setEnabled(!selection.isEmpty());
          markMenu.add(action);

          /* Mark All Read */
          action = new MarkAllNewsReadAction();
          markMenu.add(action);

          /* Sticky */
          markMenu.add(new Separator());
          action = new MakeNewsStickyAction(selection);
          action.setEnabled(!selection.isEmpty());
          markMenu.add(action);

          /* Label */
          if (!selection.isEmpty()) {
            Collection<ILabel> labels = CoreUtils.loadSortedLabels();

            /* Label */
            MenuManager labelMenu = new MenuManager("Label");
            manager.appendToGroup("mark", labelMenu);

            /* Assign / Organize Labels */
            labelMenu.add(new AssignLabelsAction(fViewer.getTree().getShell(), selection));
            labelMenu.add(new Action("Organize Labels...") {
              @Override
              public void run() {
                PreferencesUtil.createPreferenceDialogOn(fViewer.getTree().getShell(), ManageLabelsPreferencePage.ID, null, null).open();
              }
            });
            labelMenu.add(new Separator());

            /* Retrieve Labels that all selected News contain */
            Set<ILabel> selectedLabels = ModelUtils.getLabelsForAll(selection);
            for (final ILabel label : labels) {
              LabelAction labelAction = new LabelAction(label, selection);
              labelAction.setChecked(selectedLabels.contains(label));
              labelMenu.add(labelAction);
            }

            /* Remove All Labels */
            labelMenu.add(new Separator());
            LabelAction removeAllLabels = new LabelAction(null, selection);
            removeAllLabels.setEnabled(!labels.isEmpty());
            labelMenu.add(removeAllLabels);
          }
        }

        /* Move To / Copy To */
        if (!selection.isEmpty()) {
          manager.add(new Separator("movecopy"));

          /* Load all news bins and sort by name */
          List<INewsBin> newsbins = new ArrayList<INewsBin>(DynamicDAO.loadAll(INewsBin.class));

          Comparator<INewsBin> comparator = new Comparator<INewsBin>() {
            public int compare(INewsBin o1, INewsBin o2) {
              return o1.getName().compareTo(o2.getName());
            };
          };

          Collections.sort(newsbins, comparator);

          /* Move To */
          MenuManager moveMenu = new MenuManager("Move To", "moveto");
          manager.add(moveMenu);

          for (INewsBin bin : newsbins) {
            if (fViewer.getInput() instanceof NewsBinReference && bin.getId().equals(((NewsBinReference) fViewer.getInput()).getId()))
              continue;

            moveMenu.add(new MoveCopyNewsToBinAction(selection, bin, true));
          }

          moveMenu.add(new Separator("movetonewbin"));
          moveMenu.add(new MoveCopyNewsToBinAction(selection, null, true));

          /* Copy To */
          MenuManager copyMenu = new MenuManager("Copy To", "copyto");
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            if (fViewer.getInput() instanceof NewsBinReference && bin.getId().equals(((NewsBinReference) fViewer.getInput()).getId()))
              continue;

            copyMenu.add(new MoveCopyNewsToBinAction(selection, bin, false));
          }

          copyMenu.add(new Separator("copytonewbin"));
          copyMenu.add(new MoveCopyNewsToBinAction(selection, null, false));
        }

        /* Share */
        {
          manager.add(new Separator("share"));
          MenuManager shareMenu = new MenuManager("Share News", OwlUI.SHARE, "sharenews");
          manager.add(shareMenu);

          List<ShareProvider> providers = Controller.getDefault().getShareProviders();
          for (final ShareProvider provider : providers) {
            if (provider.isEnabled()) {
              shareMenu.add(new Action(provider.getName()) {
                @Override
                public void run() {
                  if (SendLinkAction.ID.equals(provider.getId())) {
                    IActionDelegate action = new SendLinkAction();
                    action.selectionChanged(null, selection);
                    action.run(null);
                  } else {
                    Object obj = selection.getFirstElement();
                    if (obj != null && obj instanceof INews) {
                      String shareLink = provider.toShareUrl((INews) obj);
                      new OpenInBrowserAction(new StructuredSelection(shareLink)).run();
                    }
                  }
                };

                @Override
                public ImageDescriptor getImageDescriptor() {
                  if (StringUtils.isSet(provider.getIconPath()))
                    return OwlUI.getImageDescriptor(provider.getPluginId(), provider.getIconPath());

                  return super.getImageDescriptor();
                };

                @Override
                public boolean isEnabled() {
                  return !selection.isEmpty();
                }

                @Override
                public String getActionDefinitionId() {
                  return SendLinkAction.ID.equals(provider.getId()) ? SendLinkAction.ID : super.getActionDefinitionId();
                }

                @Override
                public String getId() {
                  return SendLinkAction.ID.equals(provider.getId()) ? SendLinkAction.ID : super.getId();
                }
              });
            }
          }

          /* Configure Providers */
          shareMenu.add(new Separator());
          shareMenu.add(new Action("&Configure...") {
            @Override
            public void run() {
              PreferencesUtil.createPreferenceDialogOn(fViewer.getTree().getShell(), SharingPreferencesPage.ID, null, null).open();
            };
          });
        }

        manager.add(new Separator("filter"));
        manager.add(new Separator("edit"));
        manager.add(new Separator("copy"));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        /* Need a good Selection here */
        if (selection.isEmpty() || (selection.size() == 1 && selection.getFirstElement() instanceof EntityGroup))
          return;

        /* Show in Feed (only for searchmarks) */
        if (fViewer.getInput() instanceof SearchMarkReference) {
          OpenNewsAction showInFeedAction = new OpenNewsAction(selection);
          showInFeedAction.setText("Show in Feed");
          manager.appendToGroup("open", showInFeedAction);
        }
      }
    });

    /* Create and Register with Workbench */
    Menu menu = manager.createContextMenu(fViewer.getControl());
    fViewer.getControl().setMenu(menu);
    fEditorSite.registerContextMenu(manager, fViewer);
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setInput(java.lang.Object)
   */
  public void setPartInput(Object input) {

    /* Update Columns for Input */
    showColumns(createColumnModel(input), true, false);

    /* Set Input to Viewer */
    if (input instanceof IEntity)
      fViewer.setInput(((IEntity) input).toReference());
    else
      fViewer.setInput(input);
  }

  /**
   * Adjusts the scroll position to reflect the sorting.
   */
  public void adjustScrollPosition() {
    Tree tree = fViewer.getTree();
    int itemCount = tree.getItemCount();
    if (itemCount > 0) {
      if (fNewsSorter.getSortBy() == NewsColumn.DATE && fNewsSorter.isAscending()) {
        TreeItem item = tree.getItem(itemCount - 1);
        int childCount = item.getItemCount();
        if (childCount != 0)
          item = item.getItem(childCount - 1);
        tree.showItem(item);
      } else
        tree.setTopItem(tree.getItem(0));
    }
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setFocus()
   */
  public void setFocus() {
    fViewer.getControl().setFocus();
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#dispose()
   */
  public void dispose() {
    fNewsStateTracker.cancel();
    fInstantMarkUnreadTracker.cancel();
    fResources.dispose();
    unregisterListeners();
    fEditorInput = null;
  }

  void setBlockNewsStateTracker(boolean block) {
    fBlockNewsStateTracker.set(block);
  }

  private void unregisterListeners() {
    fViewer.removeSelectionChangedListener(fSelectionChangeListener);
    DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  private void setNewsState(final INews news, final INews.State state, boolean async) {
    Runnable runnable = new Runnable() {
      public void run() {

        /* The news might have been marked as hidden/deleted meanwhile, so return */
        if (!news.isVisible())
          return;

        Set<INews> singleNewsSet = Collections.singleton(news);

        /* Add to UndoStack */
        UndoStack.getInstance().addOperation(new NewsStateOperation(singleNewsSet, state, true));

        /* Perform Operation */
        fNewsDao.setState(singleNewsSet, state, true, false);
      }
    };

    if (async)
      JobRunner.runInUIThread(fViewer.getControl(), runnable);
    else
      runnable.run();
  }

  private int indexOf(NewsColumn column) {
    Tree tree = fCustomTree.getControl();
    if (tree.isDisposed())
      return -1;

    TreeColumn[] columns = tree.getColumns();
    for (int i = 0; i < columns.length; i++) {
      if (column == columns[i].getData(NewsColumnViewModel.COL_ID))
        return i;
    }

    return -1;
  }

  private boolean isInImageBounds(TreeItem item, NewsColumn column, Point p) {
    int index = indexOf(column);
    if (index == -1)
      return false;

    return item.getImageBounds(index).contains(p);
  }

  private void saveColumnModelInBackground() {
    final IPreferenceScope[] scope = new IPreferenceScope[1];
    final boolean[] saveMark = new boolean[] { false };
    final INewsMark mark = fEditorInput.getMark();

    IPreferenceScope entityPrefs = Owl.getPreferenceService().getEntityScope(mark);
    if (entityPrefs.hasKey(DefaultPreferences.BM_NEWS_COLUMNS) || entityPrefs.hasKey(DefaultPreferences.BM_NEWS_SORT_COLUMN) || entityPrefs.hasKey(DefaultPreferences.BM_NEWS_SORT_ASCENDING)) {
      scope[0] = entityPrefs; //Save to Entity
      saveMark[0] = true;
    } else
      scope[0] = fGlobalPreferences; //Save Globally

    final NewsColumnViewModel modelCopy = new NewsColumnViewModel(fColumnModel);
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        modelCopy.saveTo(scope[0]);
        if (saveMark[0]) {
          if (mark instanceof FolderNewsMark)
            DynamicDAO.save(((FolderNewsMark) mark).getFolder());
          else
            DynamicDAO.save(mark);
        }
      }
    });
  }
}