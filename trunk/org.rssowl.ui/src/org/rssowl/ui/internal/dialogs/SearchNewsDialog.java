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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.SearchHit;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTable;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkNewsReadAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.editors.feed.NewsComparator;
import org.rssowl.ui.internal.editors.feed.NewsTableControl;
import org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider;
import org.rssowl.ui.internal.editors.feed.NewsTableControl.Columns;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <code>SearchNewsDialog</code> allows to define a number of
 * <code>ISearchCondition</code>s to search in all News. The result is given
 * out in a Table-Control below.
 * <p>
 * TODO Unfortunately this Dialog copies a lot of existing code from
 * NewsTableControl
 * </p>
 *
 * @author bpasero
 */
public class SearchNewsDialog extends TitleAreaDialog {

  /* Min width of the dialog in DLUs */
  private static final int DIALOG_MIN_WIDTH = 500;

  /* Section for Dialogs Settings */
  private static final String SETTINGS_SECTION = "org.rssowl.ui.internal.dialogs.SearchNewsDialog";

  /* ID to associate a Column with its ID */
  private static final String COL_ID = "org.rssowl.ui.internal.editors.feed.ColumnIdentifier";

  /* Workaround for unknown Dateo-Col Width */
  private static int DATE_COL_WIDTH = -1;

  /* Number of News to preload before showing as result */
  private static final int NUM_PRELOADED = 20;

  /* Count number of open Dialogs */
  private static int fgOpenDialogCount;

  /* Button IDs */
  private static final int BUTTON_SEARCH = 1000;
  private static final int BUTTON_CLEAR = 1001;

  /* TODO Developer's flag to enable / disable COD */
  private static final boolean USE_CUSTOM_OWNER_DRAWN = true;

  /* Indices of Columns in the Table-Viewer */
  private static final int COL_RELEVANCE = 0;
  private static final int COL_TITLE = 1;
  private static final int COL_CATEGORY = 4;
  private static final int COL_STICKY = 5;

  /* Viewer and Controls */
  private Button fMatchAllRadio;
  private Button fMatchAnyRadio;
  private SearchConditionList fSearchConditionList;
  private TableViewer fViewer;
  private ScoredNewsComparator fNewsSorter;
  private Link fStatusLabel;

  /* Misc. */
  private NewsTableControl.Columns fInitialSortColumn = NewsTableControl.Columns.SCORE;
  private boolean fInitialAscending;
  private LocalResourceManager fResources;
  private IDialogSettings fDialogSettings;
  private IModelSearch fModelSearch;
  private NewsListener fNewsListener;
  private boolean fFirstTimeOpen;
  private boolean fShowsHandCursor;
  private Cursor fHandCursor;
  private List<ISearchCondition> fInitialConditions;
  private boolean fRunSearch;
  private boolean fMatchAllConditions;
  private INewsDAO fNewsDao;
  private IPreferenceScope fPreferences;
  private LabelAdapter fLabelListener;

  /* Container for a search result */
  private static class ScoredNews {
    private NewsReference fNewsRef;
    private INews fResolvedNews;
    private Float fScore;
    private Relevance fRelevance;
    private final State fState;

    ScoredNews(NewsReference newsRef, INews.State state, Float score, Relevance relevance) {
      fNewsRef = newsRef;
      fState = state;
      fScore = score;
      fRelevance = relevance;
    }

    INews getNews() {
      if (fResolvedNews == null)
        fResolvedNews = fNewsRef.resolve();

      return fResolvedNews;
    }

    INews.State getState() {
      return fState;
    }

    NewsReference getNewsReference() {
      return fNewsRef;
    }

    Float getScore() {
      return fScore;
    }

    Relevance getRelevance() {
      return fRelevance;
    }
  }

  /* ScoredNews Relevance */
  private enum Relevance {

    /** Indicates Low Relevance */
    LOW,

    /** Indicates Medium Relevance */
    MEDIUM,

    /** Indicates High Relevance */
    HIGH;
  }

  /* Comparator for Scored News */
  private static class ScoredNewsComparator extends ViewerComparator implements Comparator<ScoredNews> {
    private NewsComparator fNewsComparator = new NewsComparator();

    /*
     * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer,
     * java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {

      /* Unlikely to happen */
      if (!(e1 instanceof ScoredNews) || !(e2 instanceof ScoredNews))
        return 0;

      /* Proceed comparing Scored News */
      return compare((ScoredNews) e1, (ScoredNews) e2);
    }

    /*
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(ScoredNews news1, ScoredNews news2) {

      /* Not sorting by Score */
      if (fNewsComparator.getSortBy() != NewsTableControl.Columns.SCORE)
        return fNewsComparator.compare(news1.getNews(), news2.getNews());

      /* Sort by Score */
      if (!news1.getScore().equals(news2.getScore())) {
        int result = news1.getScore().compareTo(news2.getScore());
        return fNewsComparator.isAscending() ? result : result * -1;
      }

      /* Default: Sort by Date */
      Date date1 = DateUtils.getRecentDate(news1.getNews());
      Date date2 = DateUtils.getRecentDate(news2.getNews());

      return date2.compareTo(date1);
    }

    void setAscending(boolean ascending) {
      fNewsComparator.setAscending(ascending);
    }

    void setSortBy(Columns sortColumn) {
      fNewsComparator.setSortBy(sortColumn);
    }

    Columns getSortBy() {
      return fNewsComparator.getSortBy();
    }

    boolean isAscending() {
      return fNewsComparator.isAscending();
    }
  }

  /* LabelProvider for Scored News */
  private static class ScoredNewsLabelProvider extends NewsTableLabelProvider {
    private Image fHighRelevanceIcon;
    private Image fMediumRelevanceIcon;
    private Image fLowRelevanceIcon;

    ScoredNewsLabelProvider() {
      createResources();
    }

    private void createResources() {
      fHighRelevanceIcon = OwlUI.getImage(fResources, "icons/obj16/high.gif");
      fMediumRelevanceIcon = OwlUI.getImage(fResources, "icons/obj16/medium.gif");
      fLowRelevanceIcon = OwlUI.getImage(fResources, "icons/obj16/low.gif");
    }

    /*
     * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
     */
    @Override
    public void update(ViewerCell cell) {
      ScoredNews scoredNews = (ScoredNews) cell.getElement();

      /* Text */
      if (cell.getColumnIndex() == COL_CATEGORY)
        cell.setText(getCategories(scoredNews.getNews()));
      else
        cell.setText(getColumnText(scoredNews.getNews(), cell.getColumnIndex() - 1));

      /* Image */
      cell.setImage(getColumnImage(scoredNews, cell.getColumnIndex()));

      /* Font */
      cell.setFont(getFont(scoredNews.getNews(), cell.getColumnIndex() - 1));

      /* Foreground */
      Color foreground = getForeground(scoredNews.getNews(), cell.getColumnIndex() - 1);

      /* TODO This is required to invalidate + redraw the entire TableItem! */
      if (USE_CUSTOM_OWNER_DRAWN) {
        Item item = (Item) cell.getItem();
        if (item instanceof TableItem)
          ((TableItem) cell.getItem()).setForeground(foreground);
      } else
        cell.setForeground(foreground);

      /* Background */
      cell.setBackground(getBackground(scoredNews.getNews(), cell.getColumnIndex() - 1));
    }

    private String getCategories(INews news) {
      StringBuilder builder = new StringBuilder();
      List<ICategory> categories = news.getCategories();
      for (ICategory category : categories) {
        if (category.getName() != null)
          builder.append(category.getName()).append(", ");
        else if (category.getDomain() != null)
          builder.append(category.getDomain()).append(", ");
      }

      if (builder.length() > 0)
        return builder.substring(0, builder.length() - 2);

      return builder.toString();
    }

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#getColumnImage(java.lang.Object,
     * int)
     */
    @Override
    protected Image getColumnImage(Object element, int columnIndex) {

      /* Relevance Column */
      if (columnIndex == COL_RELEVANCE) {
        ScoredNews scoredNews = (ScoredNews) element;
        switch (scoredNews.getRelevance()) {
          case HIGH:
            return fHighRelevanceIcon;
          case MEDIUM:
            return fMediumRelevanceIcon;
          case LOW:
            return fLowRelevanceIcon;
        }
      }

      /* Any other Column */
      return super.getColumnImage(((ScoredNews) element).getNews(), columnIndex - 1);
    }

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#erase(org.eclipse.swt.widgets.Event,
     * java.lang.Object)
     */
    @Override
    public void erase(Event event, Object element) {
      super.erase(event, ((ScoredNews) element).getNews());
    }

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#paint(org.eclipse.swt.widgets.Event,
     * java.lang.Object)
     */
    @Override
    protected void paint(Event event, Object element) {
      super.paint(event, ((ScoredNews) element).getNews());
    }

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#measure(org.eclipse.swt.widgets.Event,
     * java.lang.Object)
     */
    @Override
    protected void measure(Event event, Object element) {
      super.measure(event, ((ScoredNews) element).getNews());
    }
  }

  /**
   * @param parentShell
   */
  public SearchNewsDialog(Shell parentShell) {
    this(parentShell, null, true, false);
  }

  /**
   * @param parentShell
   * @param searchScope
   */
  public SearchNewsDialog(Shell parentShell, List<IFolderChild> searchScope) {
    this(parentShell, toSearchConditions(searchScope), true, false);
  }

  private static List<ISearchCondition> toSearchConditions(List<IFolderChild> searchScope) {
    IModelFactory factory = Owl.getModelFactory();
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(2);

    /* Add scope as condition if provided */
    if (!searchScope.isEmpty()) {
      ISearchField field = factory.createSearchField(INews.LOCATION, INews.class.getName());
      conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(searchScope)));
    }

    /* Add default condition as well */
    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS, ""));

    return conditions;
  }

  /**
   * @param parentShell
   * @param initialConditions A List of Conditions that should show initially.
   * @param matchAllConditions If <code>TRUE</code>, require all conditions
   * to match, <code>FALSE</code> otherwise.
   * @param runSearch If <code>TRUE</code>, run the search after the dialog
   * opened.
   */
  public SearchNewsDialog(Shell parentShell, List<ISearchCondition> initialConditions, boolean matchAllConditions, boolean runSearch) {
    super(parentShell);

    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fDialogSettings = Activator.getDefault().getDialogSettings();
    fFirstTimeOpen = (fDialogSettings.getSection(SETTINGS_SECTION) == null);
    fModelSearch = Owl.getPersistenceService().getModelSearch();
    fHandCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    fInitialConditions = initialConditions;
    fMatchAllConditions = matchAllConditions;
    fRunSearch = runSearch;
    fNewsDao = DynamicDAO.getDAO(INewsDAO.class);
  }

  /*
   * @see org.eclipse.jface.window.Window#open()
   */
  @Override
  public int open() {
    fgOpenDialogCount++;
    return super.open();
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
    fgOpenDialogCount--;

    /*
     * Workaround for Eclipse Bug 186025: The Virtual Manager is not cleared
     * when the TableViewer is disposed. Due to the hookListener() call, a
     * reference to the TableViewer is held in Memory, so we need to explicitly
     * clear the virtual manager.
     */
    fViewer.setItemCount(0);

    boolean res = super.close();
    fResources.dispose();
    unregisterListeners();
    return res;
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Search News");
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#create()
   */
  @Override
  public void create() {
    super.create();

    /* Perform the search slightly delayed if requested */
    if (fRunSearch) {
      JobRunner.runInUIThread(200, getShell(), new Runnable() {
        public void run() {
          onSearch();
        }
      });
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/search.gif"));

    /* Title Message */
    setMessage("You can use \'?\' for any character and \'*\' for any number of characters in your search.", IMessageProvider.INFORMATION);

    /* Sashform dividing search definition from results */
    SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    sashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* Top Area */
    Composite topSash = new Composite(sashForm, SWT.NONE);
    topSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));

    Composite topSashContent = new Composite(topSash, SWT.None);
    topSashContent.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    topSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Condition Controls */
    createConditionControls(topSashContent);

    /* Create Sash */
    Composite bottomSash = new Composite(sashForm, SWT.NONE);
    bottomSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    sashForm.setWeights(new int[] { 50, 50 });

    /* Separator */
    new Label(bottomSash, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

    Composite bottomSashContent = new Composite(bottomSash, SWT.None);
    bottomSashContent.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    bottomSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Viewer for Results */
    createResultViewer(bottomSashContent);

    return sashForm;
  }

  private void createConditionControls(Composite container) {
    Composite topControlsContainer = new Composite(container, SWT.None);
    topControlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    topControlsContainer.setLayout(LayoutUtils.createGridLayout(3, 10, 0));

    /* Radio to select Condition Matching */
    fMatchAllRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAllRadio.setText("Match all conditions");
    fMatchAllRadio.setSelection(fMatchAllConditions);

    fMatchAnyRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any condition");
    fMatchAnyRadio.setSelection(!fMatchAllConditions);

    /* ToolBar to add and select existing saved searches */
    final ToolBarManager dialogToolBar = new ToolBarManager(SWT.RIGHT | SWT.FLAT);

    IAction savedSearches = new Action("Saved Searches", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {
        getMenuCreator().getMenu(dialogToolBar.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.SEARCHMARK;
      }
    };

    savedSearches.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Set<ISearchMark> searchMarks = Controller.getDefault().getCacheService().getSearchMarks();
        Menu menu = new Menu(parent);

        /* Create new Saved Search */
        MenuItem newSavedSearch = new MenuItem(menu, SWT.NONE);
        newSavedSearch.setText("New Saved Search...");
        newSavedSearch.setImage(OwlUI.getImage(fResources, "icons/etool16/add.gif"));
        newSavedSearch.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            onSave();
          }
        });

        /* Separator */
        if (searchMarks.size() > 0)
          new MenuItem(menu, SWT.SEPARATOR);

        /* Show Existing Saved Searches */
        for (final ISearchMark searchMark : searchMarks) {
          MenuItem item = new MenuItem(menu, SWT.None);
          item.setText(searchMark.getName());
          item.setImage(OwlUI.getImage(fResources, OwlUI.SEARCHMARK));
          item.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
              show(searchMark);
            }
          });
        }

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });

    dialogToolBar.add(savedSearches);
    dialogToolBar.createControl(topControlsContainer);
    dialogToolBar.getControl().setLayoutData(new GridData(SWT.END, SWT.BEGINNING, true, false));
    dialogToolBar.getControl().getItem(0).setText(savedSearches.getText());

    /* Container for Conditions */
    final Composite conditionsContainer = new Composite(container, SWT.NONE);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 10));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);
    conditionsContainer.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        Rectangle clArea = conditionsContainer.getClientArea();
        gc.setForeground(conditionsContainer.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        gc.drawLine(clArea.x, clArea.y, clArea.x + clArea.width, clArea.y);
      }
    });

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, getDefaultConditions());
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fSearchConditionList.setVisibleItemCount(3);

    /* Show Initial Conditions if present */
    if (fInitialConditions != null)
      fSearchConditionList.showConditions(fInitialConditions);

    /* Focus Input */
    int index = 0;
    if (fInitialConditions != null && fInitialConditions.size() == 2)
      index = 1;

    fSearchConditionList.focusInput(index);
  }

  /* Show conditions of the given searchmark */
  private void show(ISearchMark sm) {

    /* Match Conditions */
    fMatchAllRadio.setSelection(sm.matchAllConditions());
    fMatchAnyRadio.setSelection(!sm.matchAllConditions());

    /* Show Conditions */
    fSearchConditionList.showConditions(sm.getSearchConditions());

    /* Unset Error Message */
    setErrorMessage(null);
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
    layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
    layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

    Composite buttonBar = new Composite(parent, SWT.NONE);
    buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    buttonBar.setLayout(layout);

    /* Status Label */
    fStatusLabel = new Link(buttonBar, SWT.NONE);
    fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    ((GridData) fStatusLabel.getLayoutData()).heightHint = 20;
    fStatusLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSave();
      }
    });

    /* Search */
    Button searchButton = createButton(buttonBar, BUTTON_SEARCH, "Search", true);
    ((GridData) searchButton.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) searchButton.getLayoutData()).grabExcessHorizontalSpace = false;

    /* Clear */
    createButton(buttonBar, BUTTON_CLEAR, "Clear", false);

    return buttonBar;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
   */
  @Override
  protected void buttonPressed(int buttonId) {
    switch (buttonId) {
      case BUTTON_SEARCH:
        onSearch();
        break;

      case BUTTON_CLEAR:
        onClear();
        break;
    }
  }

  private void onSearch() {

    /* Make sure Conditions are provided */
    if (fSearchConditionList.isEmpty()) {
      setErrorMessage("Please specify your search by defining some conditions below.");
      fSearchConditionList.focusInput();
      return;
    }

    /* Unset Error Message */
    setErrorMessage(null);

    /* Create Conditions */
    final List<ISearchCondition> conditions = fSearchConditionList.createConditions();
    final boolean matchAllConditions = fMatchAllRadio.getSelection();

    /* Disable Buttons and update Cursor */
    getButton(BUTTON_SEARCH).setEnabled(false);
    getShell().setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_APPSTARTING));

    JobRunner.runUIUpdater(new UIBackgroundJob(getShell()) {
      private List<ScoredNews> fResult = null;

      @Override
      protected void runInBackground(IProgressMonitor monitor) {

        /* Perform Search in the Background */
        List<SearchHit<NewsReference>> searchHits = fModelSearch.searchNews(conditions, matchAllConditions);
        fResult = new ArrayList<ScoredNews>(searchHits.size());

        /* Retrieve maximum raw relevance */
        Float maxRelevanceScore = 0f;
        for (SearchHit<NewsReference> searchHit : searchHits) {
          Float relevanceRaw = searchHit.getRelevance();
          maxRelevanceScore = Math.max(maxRelevanceScore, relevanceRaw);
        }

        /* Calculate Thresholds */
        Float mediumRelThreshold = maxRelevanceScore / 3f * 1f;
        Float highRelThreshold = maxRelevanceScore / 3f * 2f;

        Set<State> visibleStates = State.getVisible();

        /* Fill Results with Relevance */
        for (SearchHit<NewsReference> searchHit : searchHits) {

          /* Only add visible News for now */
          INews.State state = (State) searchHit.getData(INews.STATE);
          if (!visibleStates.contains(state))
            continue;

          /* TODO Have to test if Entity really exists (bug 337) */
          if (!fNewsDao.exists(searchHit.getResult().getId()))
            continue;

          Float relevanceRaw = searchHit.getRelevance();
          Relevance relevance = Relevance.LOW;
          if (relevanceRaw > highRelThreshold)
            relevance = Relevance.HIGH;
          else if (relevanceRaw > mediumRelThreshold)
            relevance = Relevance.MEDIUM;

          /* Add to result */
          fResult.add(new ScoredNews(searchHit.getResult(), state, relevanceRaw, relevance));
        }

        /* Preload some results that are known to be shown initially */
        preload(fResult);
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {

        /* Set Input (sorted) to Viewer */
        fViewer.setInput(fResult);

        /* Update Status Label */
        String text;
        int size = fResult.size();
        if (size == 0)
          text = "The search returned no results. ";
        else if (size == 1)
          text = "The search returned " + fResult.size() + " result. ";
        else
          text = "The search returned " + fResult.size() + " results. ";

        text += "Click <a>here</a> to save this search.";
        fStatusLabel.setText(text);

        /* Enable Buttons and update Cursor */
        getButton(BUTTON_SEARCH).setEnabled(true);
        getShell().setCursor(null);
        getShell().setDefaultButton(getButton(BUTTON_SEARCH));
        getButton(BUTTON_SEARCH).setFocus();

        /* Move Focus back to last Search Condition Element */
        fSearchConditionList.focusInput();
      }
    });
  }

  private void preload(List<ScoredNews> list) {
    for (int i = 0; i < list.size() && i < NUM_PRELOADED; i++) {
      list.get(i).getNews();
    }
  }

  private void onClear() {

    /* Reset Conditions */
    fSearchConditionList.reset();
    fMatchAllRadio.setSelection(false);
    fMatchAnyRadio.setSelection(true);
    fViewer.setInput(Collections.emptyList());

    /* Unset Error Message */
    setErrorMessage(null);

    /* Unset Status Message */
    fStatusLabel.setText("");
  }

  private void onSave() {
    List<ISearchCondition> conditions = fSearchConditionList.createConditions();

    /* Add default if empty */
    if (conditions.isEmpty())
      conditions.addAll(getDefaultConditions());

    SearchMarkDialog dialog = new SearchMarkDialog((Shell) getShell().getParent(), null, null, conditions, fMatchAllRadio.getSelection());
    dialog.open();
  }

  private void createResultViewer(Composite bottomSashContent) {

    /* Container for Table */
    Composite tableContainer = new Composite(bottomSashContent, SWT.NONE);
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

    /* Custom Table */
    int style = SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL;
    CTable customTable = new CTable(tableContainer, style);

    /* Viewer */
    fViewer = new TableViewer(customTable.getControl()) {
      @Override
      public ISelection getSelection() {
        StructuredSelection selection = (StructuredSelection) super.getSelection();
        return convertToNews(selection);
      }
    };
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.setUseHashlookup(true);
    fViewer.getControl().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());
    fViewer.getTable().setHeaderVisible(true);

    /* Separator */
    new Label(bottomSashContent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Create the Columns */
    createColumns(customTable);

    /* Apply ContentProvider */
    fViewer.setContentProvider(getContentProvider());

    /* Create LabelProvider (Custom Owner Drawn enabled!) */
    final NewsTableLabelProvider newsTableLabelProvider = new NewsTableLabelProvider();
    if (USE_CUSTOM_OWNER_DRAWN) {
      fViewer.getControl().addListener(SWT.EraseItem, new Listener() {
        public void handleEvent(Event event) {
          Object element = event.item.getData();
          newsTableLabelProvider.erase(event, element);
        }
      });
    }

    //OwnerDrawLabelProvider.setUpOwnerDraw(fViewer); Not being used due to performance reasons
    fViewer.setLabelProvider(new ScoredNewsLabelProvider());

    /* Create Sorter */
    fNewsSorter = new ScoredNewsComparator();
    fNewsSorter.setAscending(fInitialAscending);
    fNewsSorter.setSortBy(fInitialSortColumn);
    fViewer.setComparator(fNewsSorter);

    /* Hook Contextual Menu */
    hookContextualMenu();

    /* Register Listeners */
    registerListeners();
  }

  /* Convert Selection to INews */
  private ISelection convertToNews(StructuredSelection selection) {
    List<?> selectedElements = selection.toList();
    List<INews> selectedNews = new ArrayList<INews>();
    for (Object selectedElement : selectedElements) {
      ScoredNews scoredNews = (ScoredNews) selectedElement;
      selectedNews.add(scoredNews.getNews());
    }

    return new StructuredSelection(selectedNews);
  }

  private void registerListeners() {

    /* Open selected News Links in Browser on doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onMouseDoubleClick(event);
      }
    });

    /* Perform Action on Mouse-Down */
    fViewer.getControl().addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        onMouseDown(event);
      }
    });

    /* Update Cursor on Mouse-Move */
    fViewer.getControl().addListener(SWT.MouseMove, new Listener() {
      public void handleEvent(Event event) {
        onMouseMove(event);
      }
    });

    /* Enable Sorting adding listeners to Columns */
    TableColumn[] columns = fViewer.getTable().getColumns();
    for (final TableColumn column : columns) {
      column.addSelectionListener(new SelectionAdapter() {
        @SuppressWarnings("unchecked")
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
            fViewer.getTable().setSortColumn(column);
            fViewer.getTable().setSortDirection(ascending ? SWT.UP : SWT.DOWN);
          } else {
            fViewer.getTable().setSortColumn(null);
          }

          /* Since Virtual Style is set, we have to sort the model manually */
          Collections.sort(((List<ScoredNews>) fViewer.getInput()), fNewsSorter);
          fViewer.refresh(false);
        }
      });
    }

    /* Listen to News-Events */
    fNewsListener = new NewsListener() {
      public void entitiesAdded(Set<NewsEvent> events) {
      /* Ignore */
      }

      public void entitiesUpdated(Set<NewsEvent> events) {
        onNewsEvent(events);
      }

      public void entitiesDeleted(Set<NewsEvent> events) {
      /* Ignore */
      }
    };
    DynamicDAO.addEntityListener(INews.class, fNewsListener);

    /* Redraw on Label update */
    fLabelListener = new LabelAdapter() {
      @Override
      public void entitiesUpdated(Set<LabelEvent> events) {
        JobRunner.runInUIThread(fViewer.getTable(), new Runnable() {
          public void run() {
            fViewer.refresh(true);
          }
        });
      }
    };
    DynamicDAO.addEntityListener(ILabel.class, fLabelListener);
  }

  private void onNewsEvent(final Set<NewsEvent> events) {

    /* No Result set yet */
    if (fViewer.getInput() == null)
      return;

    /* Check for Update / Deleted News */
    JobRunner.runUIUpdater(new UIBackgroundJob(getShell()) {
      private List<ScoredNews> fDeletedScoredNews;
      private List<ScoredNews> fUpdatedScoredNews;

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        List<?> input = (List<?>) fViewer.getInput();
        for (NewsEvent event : events) {
          for (Object object : input) {
            ScoredNews scoredNews = ((ScoredNews) object);
            NewsReference newsRef = scoredNews.getNewsReference();

            /* News is part of the list */
            if (newsRef.references(event.getEntity())) {
              INews news = event.getEntity();

              /* News got Deleted */
              if (!news.isVisible()) {
                if (fDeletedScoredNews == null)
                  fDeletedScoredNews = new ArrayList<ScoredNews>();
                fDeletedScoredNews.add(scoredNews);
              }

              /* News got Updated */
              else {
                if (fUpdatedScoredNews == null)
                  fUpdatedScoredNews = new ArrayList<ScoredNews>();
                fUpdatedScoredNews.add(scoredNews);
              }
            }
          }
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {

        /* News got Deleted */
        if (fDeletedScoredNews != null)
          fViewer.remove(fDeletedScoredNews.toArray());

        /* News got Updated */
        if (fUpdatedScoredNews != null)
          fViewer.update(fUpdatedScoredNews.toArray(), null);
      }
    });
  }

  private void onMouseDown(Event event) {
    Point p = new Point(event.x, event.y);
    TableItem item = fViewer.getTable().getItem(p);

    /* Problem - return */
    if (item == null || item.isDisposed())
      return;

    /* Mouse-Up over Read-State-Column */
    if (event.button == 1 && item.getImageBounds(COL_TITLE).contains(p)) {
      Object data = item.getData();

      /* Toggle State between Read / Unread */
      if (data instanceof ScoredNews) {
        INews news = ((ScoredNews) data).getNews();
        INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD : INews.State.READ;
        setNewsState(new ArrayList<INews>(Arrays.asList(new INews[] { news })), newState);
      }
    }

    /* Mouse-Up over Sticky-State-Column */
    else if (event.button == 1 && item.getImageBounds(COL_STICKY).contains(p)) {
      Object data = item.getData();

      /* Toggle State between Sticky / Not Sticky */
      if (data instanceof ScoredNews) {
        new MakeNewsStickyAction(new StructuredSelection(((ScoredNews) data).getNews())).run();
      }
    }
  }

  private void onMouseMove(Event event) {
    Point p = new Point(event.x, event.y);
    TableItem item = fViewer.getTable().getItem(p);

    /* Problem / Group hovered - reset */
    if (item == null || item.isDisposed() || item.getData() instanceof EntityGroup) {
      if (fShowsHandCursor && !fViewer.getControl().isDisposed()) {
        fViewer.getControl().setCursor(null);
        fShowsHandCursor = false;
      }
      return;
    }

    /* Show Hand-Cursor if action can be performed */
    boolean changeToHandCursor = item.getImageBounds(COL_TITLE).contains(p) || item.getImageBounds(COL_STICKY).contains(p);
    if (!fShowsHandCursor && changeToHandCursor) {
      fViewer.getControl().setCursor(fHandCursor);
      fShowsHandCursor = true;
    } else if (fShowsHandCursor && !changeToHandCursor) {
      fViewer.getControl().setCursor(null);
      fShowsHandCursor = false;
    }
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
    DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
  }

  private void onMouseDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (selection.isEmpty())
      return;

    /* Convert Selection to INews */
    List<?> selectedElements = selection.toList();
    List<INews> selectedNews = new ArrayList<INews>();
    for (Object selectedElement : selectedElements) {
      ScoredNews scoredNews = (ScoredNews) selectedElement;
      selectedNews.add(scoredNews.getNews());
    }

    /* Open News */
    new OpenNewsAction(new StructuredSelection(selectedNews), getShell()).run();
  }

  private void hookContextualMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

        /* Open */
        {

          /* Open in FeedView */
          manager.add(new Separator("internalopen"));
          if (!selection.isEmpty())
            manager.appendToGroup("internalopen", new OpenNewsAction(selection, getShell()));

          manager.add(new GroupMarker("open"));

          /* Show only when internal browser is used */
          if (!selection.isEmpty() && !fPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER) && !fPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
            manager.add(new OpenInExternalBrowserAction(selection));
        }

        /* Mark / Label */
        if (!selection.isEmpty()) {
          manager.add(new Separator("mark"));

          /* Mark */
          MenuManager markMenu = new MenuManager("Mark", "mark");
          manager.add(markMenu);

          /* Mark as Read */
          IAction action = new MarkNewsReadAction(selection);
          action.setEnabled(!selection.isEmpty());
          markMenu.add(action);

          /* Sticky */
          markMenu.add(new Separator());
          action = new MakeNewsStickyAction(selection);
          action.setEnabled(!selection.isEmpty());
          markMenu.add(action);

          /* Label */
          if (!selection.isEmpty()) {
            Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);

            /* Label */
            MenuManager labelMenu = new MenuManager("Label");
            manager.appendToGroup("mark", labelMenu);

            /* Retrieve Labels that all selected News contain */
            Set<ILabel> selectedLabels = ModelUtils.getLabelsForAll(selection);

            IAction removeAllLabels = new Action("Remove All Labels") {
              @Override
              public void run() {
                Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
                new LabelAction(labels, (IStructuredSelection) fViewer.getSelection(), false).run();
              }
            };

            removeAllLabels.setEnabled(!labels.isEmpty());
            labelMenu.add(removeAllLabels);
            labelMenu.add(new Separator());

            for (final ILabel label : labels) {
              IAction labelAction = new Action(label.getName(), IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                  Set<ILabel> labels = new HashSet<ILabel>(1);
                  labels.add(label);
                  new LabelAction(labels, (IStructuredSelection) fViewer.getSelection(), isChecked()).run();
                }
              };

              labelAction.setChecked(selectedLabels.contains(label));
              labelMenu.add(labelAction);
            }

            labelMenu.add(new Separator());
            labelMenu.add(new Action("Organize...") {
              @Override
              public void run() {
                PreferencesUtil.createPreferenceDialogOn(fViewer.getTable().getShell(), ManageLabelsPreferencePage.ID, null, null).open();
              }
            });
          }
        }

        manager.add(new Separator("edit"));
        manager.add(new Separator("copy"));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });

    /* Create and Register with Workbench */
    Menu menu = manager.createContextMenu(fViewer.getControl());
    fViewer.getControl().setMenu(menu);

    /* Register with Part Site */
    IWorkbenchWindow window = OwlUI.getWindow();
    if (window != null) {
      IWorkbenchPart activePart = window.getPartService().getActivePart();
      if (activePart != null && activePart.getSite() != null)
        activePart.getSite().registerContextMenu(manager, fViewer);
    }
  }

  private void createColumns(CTable customTable) {

    /* Score Column */
    TableViewerColumn col = new TableViewerColumn(fViewer, SWT.CENTER);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FIXED, 24), null, null, true, true);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.SCORE);
    col.getColumn().setToolTipText("Relevance");
    if (fInitialSortColumn == NewsTableControl.Columns.SCORE) {
      customTable.getControl().setSortColumn(col.getColumn());
    }

    /* Headline Column */
    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 60), "Title", null, true, true);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.TITLE);
    if (fInitialSortColumn == NewsTableControl.Columns.TITLE) {
      customTable.getControl().setSortColumn(col.getColumn());
      customTable.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Date Column */
    int width = getInitialDateColumnWidth();
    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FIXED, width), "Date", null, true, true);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.DATE);
    if (fInitialSortColumn == NewsTableControl.Columns.DATE) {
      customTable.getControl().setSortColumn(col.getColumn());
      customTable.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Author Column */
    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 20), "Author", null, true, true);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.AUTHOR);
    if (fInitialSortColumn == NewsTableControl.Columns.AUTHOR) {
      customTable.getControl().setSortColumn(col.getColumn());
      customTable.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Category Column */
    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 20), "Category", null, true, true);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.CATEGORY);
    if (fInitialSortColumn == NewsTableControl.Columns.CATEGORY) {
      customTable.getControl().setSortColumn(col.getColumn());
      customTable.getControl().setSortDirection(fInitialAscending ? SWT.UP : SWT.DOWN);
    }

    /* Sticky Column */
    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FIXED, 18), null, null, true, false);
    col.getColumn().setData(COL_ID, NewsTableControl.Columns.STICKY);
    col.getColumn().setToolTipText("Sticky State");
  }

  private IStructuredContentProvider getContentProvider() {
    return new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        if (inputElement instanceof List<?>)
          return getVisibleNews((List<?>) inputElement);

        return new Object[0];
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    };
  }

  private Object[] getVisibleNews(List<?> elements) {
    List<ScoredNews> news = new ArrayList<ScoredNews>();
    Set<INews.State> visibleStates = INews.State.getVisible();
    for (Object element : elements) {
      if (element instanceof ScoredNews) {
        ScoredNews scoredNews = (ScoredNews) element;
        if (visibleStates.contains(scoredNews.getState()))
          news.add((ScoredNews) element);
      }
    }

    return news.toArray();
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

    DATE_COL_WIDTH = OwlUI.getTextSize(fViewer.getTable(), OwlUI.getBold(JFaceResources.DEFAULT_FONT), sampleDate).x;
    DATE_COL_WIDTH += 30; // Bounds of TableColumn requires more space

    return DATE_COL_WIDTH;
  }

  private List<ISearchCondition> getDefaultConditions() {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    IModelFactory factory = Owl.getModelFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS, "");

    conditions.add(condition);

    return conditions;
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.TITLE | SWT.BORDER | SWT.MIN | SWT.MAX | SWT.RESIZE | SWT.CLOSE | getDefaultOrientation();

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
   */
  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings section = fDialogSettings.getSection(SETTINGS_SECTION);
    if (section != null)
      return section;

    return fDialogSettings.addNewSection(SETTINGS_SECTION);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

    /* No dialog settings stored */
    if (fFirstTimeOpen) {

      /* Minimum Size */
      int minWidth = convertHorizontalDLUsToPixels(DIALOG_MIN_WIDTH);

      Point bestSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
      getShell().setSize(minWidth, bestSize.y);
      LayoutUtils.positionShell(getShell(), false);
    }

    /* Move a bit to bottom right if multiple dialogs are open at the same time */
    if (fgOpenDialogCount > 1) {
      Point location = getShell().getLocation();
      location.x += 20 * (fgOpenDialogCount - 1);
      location.y += 20 * (fgOpenDialogCount - 1);
      getShell().setLocation(location);
    }
  }

  private void setNewsState(List<INews> news, INews.State state) {
    Owl.getPersistenceService().getDAOService().getNewsDAO().setState(news, state, true, false);
  }
}