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
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.rssowl.core.Owl;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IModelFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.search.IModelSearch;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.persist.search.ISearchField;
import org.rssowl.core.model.persist.search.ISearchHit;
import org.rssowl.core.model.persist.search.SearchSpecifier;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTable;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeTypesStickyAction;
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
import java.util.Collections;
import java.util.Comparator;
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

  /* Button IDs */
  private static final int BUTTON_SEARCH = 1000;
  private static final int BUTTON_CLEAR = 1001;

  /* TODO Developer's flag to enable / disable COD */
  private static final boolean USE_CUSTOM_OWNER_DRAWN = true;

  /* Indices of Columns in the Table-Viewer */
  private static final int COL_RELEVANCE = 0;
  private static final int COL_TITLE = 1;
  private static final int COL_STICKY = 5;

  /* Viewer and Controls */
  private Button fMatchAllRadio;
  private Button fMatchAnyRadio;
  private SearchConditionList fSearchConditionList;
  private TableViewer fViewer;
  private ScoredNewsComparator fNewsSorter;
  private Label fStatusLabel;

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

  /* Container for a search result */
  private static class ScoredNews {
    private INews fNews;
    private Float fScore;
    private Relevance fRelevance;

    ScoredNews(INews news, Float score, Relevance relevance) {
      fNews = news;
      fScore = score;
      fRelevance = relevance;
    }

    INews getNews() {
      return fNews;
    }

    Float getScore() {
      return fScore;
    }

    Relevance getRelevance() {
      return fRelevance;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return fNews.hashCode();
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      return fNews.equals(obj);
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
      int result = news1.getScore().compareTo(news2.getScore());
      return fNewsComparator.isAscending() ? result : result * -1;
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

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#getColumnImage(java.lang.Object,
     * int)
     */
    @Override
    protected Image getColumnImage(Object element, int columnIndex) {

      /* Relevance Column */
      if (columnIndex == COL_RELEVANCE) {
        ScoredNews scoredNews = (ScoredNews) element;
        if (scoredNews.getRelevance() == Relevance.HIGH)
          return fHighRelevanceIcon;
        else if (scoredNews.getRelevance() == Relevance.MEDIUM)
          return fMediumRelevanceIcon;

        return fLowRelevanceIcon;
      }

      /* Any other Column */
      return super.getColumnImage(((ScoredNews) element).getNews(), columnIndex - 1);
    }

    /*
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#erase(org.eclipse.swt.widgets.Event,
     * java.lang.Object)
     */
    @Override
    protected void erase(Event event, Object element) {
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
    this(parentShell, null, false, false);
  }

  /**
   * @param parentShell
   * @param conditions A List of Conditions that should show initially.
   * @param matchAllConditions If <code>TRUE</code>, require all conditions
   * to match, <code>FALSE</code> otherwise.
   * @param runSearch If <code>TRUE</code>, run the search after the dialog
   * opened.
   */
  public SearchNewsDialog(Shell parentShell, List<ISearchCondition> conditions, boolean matchAllConditions, boolean runSearch) {
    super(parentShell);

    fResources = new LocalResourceManager(JFaceResources.getResources());
    fDialogSettings = Activator.getDefault().getDialogSettings();
    fFirstTimeOpen = (fDialogSettings.getSection(SETTINGS_SECTION) == null);
    fModelSearch = Owl.getPersistenceService().getModelSearch();
    fHandCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    fInitialConditions = conditions;
    fMatchAllConditions = matchAllConditions;
    fRunSearch = runSearch;
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
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
    setTitleImage(OwlUI.getImage(fResources, "icons/elcl16/search.gif"));

    /* Title Message */
    setMessage("You can use \'?\' for any character and \'*\' for any word in your search.", IMessageProvider.INFORMATION);

    /* Sashform dividing search definition from results */
    SashForm sashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

    /* Top Area */
    Composite topSash = new Composite(sashForm, SWT.NONE);
    topSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));

    Composite topSashContent = new Composite(topSash, SWT.None);
    topSashContent.setLayout(LayoutUtils.createGridLayout(2, 5, 0, 0, 0, false));
    topSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((GridLayout) topSashContent.getLayout()).marginBottom = 5;

    /* Create Condition Controls */
    createConditionControls(topSashContent);

    /* Separator */
    new Label(topSash, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

    /* Create Sash */
    Composite bottomSash = new Composite(sashForm, SWT.NONE);
    bottomSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    sashForm.setWeights(new int[] { 50, 50 });

    Composite bottomSashContent = new Composite(bottomSash, SWT.None);
    bottomSashContent.setLayout(LayoutUtils.createGridLayout(1, 5, 2));
    bottomSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Viewer for Results */
    createResultViewer(bottomSashContent);

    return sashForm;
  }

  private void createConditionControls(Composite container) {
    Composite topControlsContainer = new Composite(container, SWT.None);
    topControlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    topControlsContainer.setLayout(LayoutUtils.createGridLayout(3, 5, 0));

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
    Composite conditionsContainer = new Composite(container, SWT.BORDER);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(2));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, getDefaultConditions());
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fSearchConditionList.setVisibleItemCount(3);
    fSearchConditionList.focusInput();

    /* Show Initial Conditions if present */
    if (fInitialConditions != null)
      fSearchConditionList.showConditions(fInitialConditions);
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

    Composite buttonBar = new Composite(parent, SWT.None);
    buttonBar.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    buttonBar.setLayout(layout);

    /* Status Label */
    fStatusLabel = new Label(buttonBar, SWT.NONE);
    fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    /* Search */
    Button searchButton = createButton(buttonBar, BUTTON_SEARCH, "Search", true);
    ((GridData) searchButton.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) searchButton.getLayoutData()).grabExcessHorizontalSpace = true;

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
        List<ISearchHit<NewsReference>> searchHits = fModelSearch.searchNews(conditions, matchAllConditions);
        fResult = new ArrayList<ScoredNews>(searchHits.size());

        /* Retrieve maximum raw relevance */
        Float maxRelevanceScore = 0f;
        for (ISearchHit<NewsReference> searchHit : searchHits) {
          Float relevanceRaw = searchHit.getRelevance();
          maxRelevanceScore = Math.max(maxRelevanceScore, relevanceRaw);
        }

        /* Calculate Thresholds */
        Float mediumRelThreshold = maxRelevanceScore / 3f * 1f;
        Float highRelThreshold = maxRelevanceScore / 3f * 2f;

        /* Fill Results with Relevance */
        for (ISearchHit<NewsReference> searchHit : searchHits) {
          INews news = searchHit.getResult().resolve();
          if (news != null) {//TODO Remove once Bug 173 is fixed
            Float relevanceRaw = searchHit.getRelevance();
            Relevance relevance = Relevance.LOW;
            if (relevanceRaw > highRelThreshold)
              relevance = Relevance.HIGH;
            else if (relevanceRaw > mediumRelThreshold)
              relevance = Relevance.MEDIUM;

            fResult.add(new ScoredNews(news, relevanceRaw, relevance));
          }
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {

        /* Set Input (sorted) to Viewer */
        Collections.sort(fResult, fNewsSorter);
        fViewer.setInput(fResult);

        /* Update Status Label */
        int size = fResult.size();
        if (size == 0)
          fStatusLabel.setText("The search returned no results.");
        else if (size == 1)
          fStatusLabel.setText("The search returned " + fResult.size() + " result.");
        else
          fStatusLabel.setText("The search returned " + fResult.size() + " results.");

        /* Enable Buttons and update Cursor */
        getButton(BUTTON_SEARCH).setEnabled(true);
        getShell().setCursor(null);
        getShell().setDefaultButton(getButton(BUTTON_SEARCH));
        getButton(BUTTON_SEARCH).setFocus();
      }
    });
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

    SearchMarkDialog dialog = new SearchMarkDialog((Shell) getShell().getParent(), null, conditions, fMatchAllRadio.getSelection());
    dialog.open();
  }

  private void createResultViewer(Composite bottomSashContent) {

    /* Container for Table */
    Composite tableContainer = new Composite(bottomSashContent, SWT.NONE);
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

    /* Custom Table */
    int style = SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL;
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

    /* Create the Columns */
    createColumns(customTable);

    /* Apply ContentProvider */
    fViewer.setContentProvider(getContentProvider());

    /* Create LabelProvider (Custom Owner Drawn enabled!) */
    if (USE_CUSTOM_OWNER_DRAWN)
      OwnerDrawLabelProvider.setUpOwnerDraw(fViewer);
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
    List< ? > selectedElements = selection.toList();
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
        onNewsEvent(events);
      }
    };
    Owl.getListenerService().addNewsListener(fNewsListener);
  }

  private void onNewsEvent(Set<NewsEvent> events) {

    /* No Result set yet */
    if (fViewer.getInput() == null)
      return;

    /* Search if news is part of result */
    List< ? > input = (List< ? >) fViewer.getInput();
    for (NewsEvent event : events) {
      for (Object object : input) {
        INews news = ((ScoredNews) object).getNews();

        /* News is part of the list, refresh and return */
        if (news.equals(event.getEntity())) {
          JobRunner.runInUIThread(getShell(), new Runnable() {
            public void run() {
              fViewer.refresh();
            }
          });
          return;
        }
      }
    }
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
        new MakeTypesStickyAction(new StructuredSelection(((ScoredNews) data).getNews())).run();
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
    Owl.getListenerService().removeNewsListener(fNewsListener);
  }

  private void onMouseDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    if (selection.isEmpty())
      return;

    /* Convert Selection to INews */
    List< ? > selectedElements = selection.toList();
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
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new Separator("internalopen"));
        manager.add(new GroupMarker("open"));
        manager.add(new Separator("mark"));
        manager.add(new Separator("edit"));
        manager.add(new Separator("copy"));
        manager.add(new Separator("label"));

        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

        /* Need a Selection here */
        if (selection.isEmpty())
          return;

        /* Open in FeedView */
        manager.appendToGroup("internalopen", new OpenNewsAction(selection, getShell()));

        /* Sticky */
        manager.appendToGroup("label", new MakeTypesStickyAction(selection));

        /* Label */
        MenuManager labelMenu = new MenuManager("Label");
        manager.appendToGroup("label", labelMenu);

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

        List<ILabel> labels = Owl.getPersistenceService().getApplicationLayer().loadLabels();
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
        if (inputElement instanceof List< ? >)
          return getVisibleNews((List< ? >) inputElement);

        return new Object[0];
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    };
  }

  private Object[] getVisibleNews(List< ? > elements) {
    List<ScoredNews> news = new ArrayList<ScoredNews>();

    for (Object element : elements) {
      if (element instanceof ScoredNews && ((ScoredNews) element).getNews().isVisible())
        news.add((ScoredNews) element);
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
    cal.set(2006, 12, 12, 12, 12, 12);
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
      getShell().setSize(Math.max(bestSize.x, minWidth), bestSize.y);
      LayoutUtils.positionShell(getShell(), false);
    }
  }

  private void setNewsState(List<INews> news, INews.State state) {
    Owl.getPersistenceService().getApplicationLayer().setNewsState(news, state, true, false);
  }
}