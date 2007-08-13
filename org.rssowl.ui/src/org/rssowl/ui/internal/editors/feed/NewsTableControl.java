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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTree;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.StatusLineUpdater;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MarkNewsReadAction;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.JobTracker;
import org.rssowl.ui.internal.util.ModelUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Part of the FeedView to display News in a TableViewer.
 *
 * @author bpasero
 */
public class NewsTableControl implements IFeedViewPart {

  /* ID to associate a Column with its ID */
  private static final String COL_ID = "org.rssowl.ui.internal.editors.feed.ColumnIdentifier";

  /* Workaround for unknown Dateo-Col Width */
  private static int DATE_COL_WIDTH = -1;

  /* TODO Developer's flag to enable / disable COD */
  static final boolean USE_CUSTOM_OWNER_DRAWN = true;

  /* Indices of Columns in the Tree-Viewer */
  static final int COL_TITLE = 0;
  static final int COL_PUBDATE = 1;
  static final int COL_AUTHOR = 2;
  static final int COL_CATEGORY = 3;
  static final int COL_STICKY = 4;

  /** Supported Columns of the Viewer */
  public enum Columns {

    /** Title of the News */
    TITLE(true, true),

    /** Date of the News */
    DATE(false, true),

    /** Author of the News */
    AUTHOR(true, true),

    /** Category of the News */
    CATEGORY(true, true),

    /** Sticky-State of the News */
    STICKY(false, false),

    /** Score of a News */
    SCORE(false, false);

    boolean fPrefersAcending;
    boolean fShowSortIndicator;

    Columns(boolean prefersAscending, boolean showSortIndicator) {
      fPrefersAcending = prefersAscending;
      fShowSortIndicator = showSortIndicator;
    }

    /**
     * @return Returns <code>TRUE</code> if this Column prefers to be sorted
     * ascending and <code>FALSE</code> otherwise.
     */
    public boolean prefersAscending() {
      return fPrefersAcending;
    }

    /**
     * @return Returns <code>TRUE</code> if this Column prefers showing a
     * sort-indicator and <code>FALSE</code> otherwise.
     */
    public boolean showSortIndicator() {
      return fShowSortIndicator;
    }
  }

  /* Tracker to Mark selected news as Read */
  private class MarkReadTracker extends JobTracker {
    MarkReadTracker(int delay, boolean showProgress) {
      super(delay, showProgress, ITask.Priority.INTERACTIVE);
    }

    @Override
    protected int getDelay() {
      return fPreferences.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS);
    }
  }

  private IEditorSite fEditorSite;
  private JobTracker fNewsStateTracker;
  private NewsTableViewer fViewer;
  private ISelectionChangedListener fSelectionChangeListener;
  private CTree fCustomTree;
  private LocalResourceManager fResources;
  private NewsComparator fNewsSorter;
  private Cursor fHandCursor;
  private boolean fShowsHandCursor;
  private AtomicBoolean fBlockNewsStateTracker = new AtomicBoolean(false);
  private LabelAdapter fLabelListener;

  /* Settings */
  private IPreferenceScope fPreferences;
  private Columns fInitialSortColumn = Columns.DATE;
  private boolean fInitialAscending = false;

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#init(org.eclipse.ui.IEditorSite)
   */
  public void init(IEditorSite editorSite) {
    fEditorSite = editorSite;
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fNewsStateTracker = new MarkReadTracker(fPreferences.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS), false);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#onInputChanged(org.rssowl.ui.internal.editors.feed.FeedViewInput)
   */
  public void onInputChanged(FeedViewInput input) {
  /* Ignore */
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

    /* TODO This is a Workaround until we remember expanded Groups */
    fViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

    fHandCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

    return fViewer;
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

    /* Headline Column */
    TreeViewerColumn col = new TreeViewerColumn(fViewer, SWT.LEFT);
    fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 60), "Title", null, true, true);
    col.getColumn().setData(COL_ID, Columns.TITLE);
    col.getColumn().setMoveable(false);
    if (fInitialSortColumn == Columns.TITLE) {
      fCustomTree.getControl().setSortColumn(col.getColumn());
      fCustomTree.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Date Column */
    int width = getInitialDateColumnWidth();
    col = new TreeViewerColumn(fViewer, SWT.LEFT);
    fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FIXED, width), "Date", null, true, true);
    col.getColumn().setData(COL_ID, Columns.DATE);
    col.getColumn().setMoveable(false);
    if (fInitialSortColumn == Columns.DATE) {
      fCustomTree.getControl().setSortColumn(col.getColumn());
      fCustomTree.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Author Column */
    col = new TreeViewerColumn(fViewer, SWT.LEFT);
    fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 20), "Author", null, true, true);
    col.getColumn().setData(COL_ID, Columns.AUTHOR);
    col.getColumn().setMoveable(false);
    if (fInitialSortColumn == Columns.AUTHOR) {
      fCustomTree.getControl().setSortColumn(col.getColumn());
      fCustomTree.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Category Column */
    col = new TreeViewerColumn(fViewer, SWT.LEFT);
    fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 20), "Category", null, true, true);
    col.getColumn().setData(COL_ID, Columns.CATEGORY);
    col.getColumn().setMoveable(false);
    if (fInitialSortColumn == Columns.CATEGORY) {
      fCustomTree.getControl().setSortColumn(col.getColumn());
      fCustomTree.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Sticky Column */
    col = new TreeViewerColumn(fViewer, SWT.LEFT);
    fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FIXED, 18), null, null, true, false);
    col.getColumn().setData(COL_ID, Columns.STICKY);
    col.getColumn().setMoveable(false);
    col.getColumn().setToolTipText("Sticky State");

    /* Apply ContentProvider */
    fViewer.setContentProvider(contentProvider);

    /* Create LabelProvider (Custom Owner Drawn enabled!) */
    if (USE_CUSTOM_OWNER_DRAWN)
      OwnerDrawLabelProvider.setUpOwnerDraw(fViewer);
    fViewer.setLabelProvider(new NewsTableLabelProvider());

    /* Create Sorter */
    fNewsSorter = new NewsComparator();
    fNewsSorter.setAscending(fInitialAscending);
    fNewsSorter.setSortBy(fInitialSortColumn);
    fViewer.setComparator(fNewsSorter);

    /* Set Comparer */
    fViewer.setComparer(getComparer());

    /* Add Filter */
    fViewer.addFilter(filter);

    /* Hook Contextual Menu */
    hookContextualMenu();

    /* Register Listeners */
    registerListeners();

    /* Propagate Selection Events */
    fEditorSite.setSelectionProvider(fViewer);
  }

  private int getInitialDateColumnWidth() {

    /* Check if Cached already */
    if (DATE_COL_WIDTH > 0)
      return DATE_COL_WIDTH;

    /* Calculate and Cache */
    DateFormat dF = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    Calendar cal = Calendar.getInstance();
    cal.set(2006, Calendar.DECEMBER, 12, 12, 12, 12);
    String sampleDate = dF.format(cal.getTime());

    DATE_COL_WIDTH = OwlUI.getTextSize(fCustomTree.getControl(), OwlUI.getBold(JFaceResources.DEFAULT_FONT), sampleDate).x;
    DATE_COL_WIDTH += 30; // Bounds of TableColumn requires more space

    return DATE_COL_WIDTH;
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
    fViewer.addPostSelectionChangedListener(fSelectionChangeListener);

    /* Perform Action on Mouse-Down */
    fCustomTree.getControl().addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        onMouseDown(event);
      }
    });

    /* Update Cursor on Mouse-Move */
    fCustomTree.getControl().addListener(SWT.MouseMove, new Listener() {
      public void handleEvent(Event event) {
        onMouseMove(event);
      }
    });

    /* Enable Sorting adding listeners to Columns */
    TreeColumn[] columns = fCustomTree.getControl().getColumns();
    for (final TreeColumn column : columns) {
      column.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          Columns oldSortBy = fNewsSorter.getSortBy();
          Columns newSortBy = (Columns) column.getData(COL_ID);
          boolean defaultAscending = newSortBy.prefersAscending();
          boolean ascending = (oldSortBy != newSortBy) ? defaultAscending : !fNewsSorter.isAscending();

          fNewsSorter.setSortBy(newSortBy);
          fNewsSorter.setAscending(ascending);

          /* Indicate Sort-Column in UI for Columns that have a certain width */
          if (newSortBy.showSortIndicator()) {
            fCustomTree.getControl().setSortColumn(column);
            fCustomTree.getControl().setSortDirection(ascending ? SWT.UP : SWT.DOWN);
          } else {
            fCustomTree.getControl().setSortColumn(null);
          }

          fViewer.refresh(false);
        }
      });
    }

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
  }

  private void onMouseDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (selection.isEmpty())
      return;

    Object firstElem = selection.getFirstElement();

    /* Open News */
    if (firstElem instanceof INews)
      new OpenInBrowserAction(selection).run();

    /* Toggle expanded State of Group */
    else if (firstElem instanceof EntityGroup)
      fViewer.setExpandedState(firstElem, !fViewer.getExpandedState(firstElem));
  }

  private void onSelectionChanged(SelectionChangedEvent event) {

    /* Check Flag and only consider Structured Selections */
    if (fBlockNewsStateTracker.get() || !(event.getSelection() instanceof IStructuredSelection))
      return;

    /* Check if settings disable the tracker */
    if (!fPreferences.getBoolean(DefaultPreferences.MARK_READ_STATE))
      return;

    /* Retrieve all NewsReferences of the Selection */
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();

    /* Only responsible for single Selection of a News */
    if (selection.size() != 1 || !(selection.getFirstElement() instanceof INews)) {
      fNewsStateTracker.cancel();
      return;
    }

    /* Trigger the Tracker if news is not read already */
    final INews selectedNews = (INews) selection.getFirstElement();
    if (selectedNews.getState() != INews.State.READ && selectedNews.isVisible()) {
      fNewsStateTracker.run(new TaskAdapter() {
        public IStatus run(IProgressMonitor monitor) {
          setNewsState(Arrays.asList(new INews[] { selectedNews }), INews.State.READ);
          return Status.OK_STATUS;
        }
      });
    }

    /* Cancel any possible running JobTracker */
    else if (selectedNews.getState() == INews.State.READ) {
      fNewsStateTracker.cancel();
    }
  }

  private void onMouseDown(Event event) {
    boolean disableTrackerTemporary = false;
    Point p = new Point(event.x, event.y);
    TreeItem item = fCustomTree.getControl().getItem(p);

    /* Problem - return */
    if (item == null || item.isDisposed())
      return;

    /* Don't run Tracker if other Mouse Button is used */
    if (event.button != 1)
      disableTrackerTemporary = true;

    /* Mouse-Up over Read-State-Column */
    if (event.button == 1 && item.getImageBounds(COL_TITLE).contains(p)) {
      Object data = item.getData();

      /* Toggle State between Read / Unread */
      if (data instanceof INews) {
        INews news = (INews) data;
        disableTrackerTemporary = (news.getState() == INews.State.READ);
        INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD : INews.State.READ;
        setNewsState(new ArrayList<INews>(Arrays.asList(new INews[] { news })), newState);
      }
    }

    /* Mouse-Up over Sticky-State-Column */
    else if (event.button == 1 && item.getImageBounds(COL_STICKY).contains(p)) {
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
    boolean changeToHandCursor = item.getImageBounds(COL_TITLE).contains(p) || item.getImageBounds(COL_STICKY).contains(p);
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
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

        /* Open */
        {
          manager.add(new Separator("open"));

          /* Show only when internal browser is used */
          if (!selection.isEmpty() && !fPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER) && !fPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
            manager.add(new OpenInExternalBrowserAction(selection));
        }

        /* Mark / Label */
        {
          manager.add(new Separator("mark"));

          /* Mark */
          MenuManager markMenu = new MenuManager("Mark", "mark");
          manager.add(markMenu);

          /* Mark as Read */
          IAction action = new MarkNewsReadAction(selection);
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

            /* Label */
            MenuManager labelMenu = new MenuManager("Label");
            manager.appendToGroup("mark", labelMenu);

            /* Retrieve selected Labels from Selection (including NULL!) */
            Set<ILabel> selectedLabels = ModelUtils.getLabels(selection);
            ILabel commonLabel = null;
            if (selectedLabels.size() == 1)
              commonLabel = selectedLabels.iterator().next();

            IAction labelNone = new Action("None", IAction.AS_RADIO_BUTTON) {
              @Override
              public void run() {
                new LabelAction(null, (IStructuredSelection) fViewer.getSelection()).run();
              }
            };
            labelNone.setChecked(selectedLabels.size() == 0 || (selectedLabels.size() == 1 && commonLabel == null));

            labelMenu.add(labelNone);
            labelMenu.add(new Separator());

            Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
            for (final ILabel label : labels) {
              IAction labelAction = new Action(label.getName(), IAction.AS_RADIO_BUTTON) {
                @Override
                public void run() {
                  new LabelAction(label, (IStructuredSelection) fViewer.getSelection()).run();
                }
              };

              labelAction.setChecked(label.equals(commonLabel));
              labelMenu.add(labelAction);
            }

            labelMenu.add(new Separator());
            labelMenu.add(new Action("Organize...") {
              @Override
              public void run() {
                PreferencesUtil.createPreferenceDialogOn(fViewer.getTree().getShell(), ManageLabelsPreferencePage.ID, null, null).open();
              }
            });
          }
        }

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
    if (input instanceof IBookMark)
      fViewer.setInput(((IBookMark) input).getFeedLinkReference());
    else if (input instanceof ISearchMark)
      fViewer.setInput(new SearchMarkReference(((ISearchMark) input).getId()));
    else
      fViewer.setInput(input);
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
    fResources.dispose();
    unregisterListeners();
  }

  void setBlockNewsStateTracker(boolean block) {
    fBlockNewsStateTracker.set(block);
  }

  private void unregisterListeners() {
    fViewer.removePostSelectionChangedListener(fSelectionChangeListener);
    DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
  }

  private void setNewsState(List<INews> news, INews.State state) {
    Owl.getPersistenceService().getDAOService().getNewsDAO().setState(news, state, true, false);
  }
}