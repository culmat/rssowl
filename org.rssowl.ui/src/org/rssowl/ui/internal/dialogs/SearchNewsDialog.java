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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
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
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.SearchHit;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CTable;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.AssignLabelsAction;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.editors.feed.NewsBrowserLabelProvider;
import org.rssowl.ui.internal.editors.feed.NewsBrowserViewer;
import org.rssowl.ui.internal.editors.feed.NewsColumn;
import org.rssowl.ui.internal.editors.feed.NewsColumnViewModel;
import org.rssowl.ui.internal.editors.feed.NewsComparator;
import org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider;
import org.rssowl.ui.internal.search.LocationControl;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <code>SearchNewsDialog</code> allows to define a number of
 * <code>ISearchCondition</code>s to search in all News. The result is given out
 * in a Table-Control below.
 *
 * @author bpasero
 */
public class SearchNewsDialog extends TitleAreaDialog {

  /* Min width of the dialog in DLUs */
  private static final int DIALOG_MIN_WIDTH = 500;

  /* Sash Weights when Preview is invisible */
  private static final int[] TWO_SASH_WEIGHTS = new int[] { 40, 60, 0 };

  /* Sash Weights when Preview is visible */
  private static final int[] THREE_SASH_WEIGHTS = new int[] { 33, 33, 33 };

  /* Section for Dialogs Settings */
  private static final String SETTINGS_SECTION = "org.rssowl.ui.internal.dialogs.SearchNewsDialog";

  /* Preference: Sash Weights */
  private static final String PREF_SASH_WEIGHTS = "org.rssowl.ui.internal.dialogs.search.SashWeights";

  /* Number of News to preload before showing as result */
  private static final int NUM_PRELOADED = 20;

  /* Count number of open Dialogs */
  private static int fgOpenDialogCount;

  /* Button IDs */
  private static final int BUTTON_SEARCH = 1000;
  private static final int BUTTON_CLEAR = 1001;

  /* Flag to enable / disable COD */
  private static final boolean USE_CUSTOM_OWNER_DRAWN = true;

  /* Viewer and Controls */
  private Button fMatchAllRadio;
  private Button fMatchAnyRadio;
  private LocationControl fLocationControl;
  private SearchConditionList fSearchConditionList;
  private CTable fCustomTable;
  private TableViewer fResultViewer;
  private NewsColumnViewModel fColumnModel;
  private ScoredNewsComparator fNewsSorter;
  private Link fStatusLabel;
  private NewsBrowserViewer fBrowserViewer;
  private NewsTableLabelProvider fNewsTableLabelProvider;
  private int[] fCachedWeights;

  /* Misc. */
  private LocalResourceManager fResources;
  private IDialogSettings fDialogSettings;
  private IModelSearch fModelSearch;
  private NewsListener fNewsListener;
  private boolean fFirstTimeOpen;
  private boolean fShowsHandCursor;
  private Cursor fHandCursor;
  private ISearchCondition fInitialScope;
  private List<ISearchCondition> fInitialConditions;
  private boolean fRunSearch;
  private boolean fMatchAllConditions;
  private INewsDAO fNewsDao;
  private IPreferenceScope fPreferences;
  private LabelAdapter fLabelListener;
  private boolean fIsPreviewVisible;
  private SashForm fSashForm;
  private Composite fBottomSash;
  private List<ISearchCondition> fCurrentSearchConditions;

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
      if (fNewsComparator.getSortBy() != NewsColumn.RELEVANCE)
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

    void setSortBy(NewsColumn sortColumn) {
      fNewsComparator.setSortBy(sortColumn);
    }

    NewsColumn getSortBy() {
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

    ScoredNewsLabelProvider(NewsColumnViewModel model) {
      super(model);
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
      NewsColumn column = fColumnModel.getColumn(cell.getColumnIndex());

      /* Text */
      if (column == NewsColumn.CATEGORY)
        cell.setText(getCategories(scoredNews.getNews()));
      else
        cell.setText(getColumnText(scoredNews.getNews(), column, cell.getColumnIndex()));

      /* Image */
      cell.setImage(getColumnImage(scoredNews, column, cell.getColumnIndex()));

      /* Font */
      cell.setFont(getFont(scoredNews.getNews(), cell.getColumnIndex()));

      /* Foreground */
      Color foreground = getForeground(scoredNews.getNews(), cell.getColumnIndex());

      /* TODO This is required to invalidate + redraw the entire TableItem! */
      if (USE_CUSTOM_OWNER_DRAWN) {
        Item item = (Item) cell.getItem();
        if (item instanceof TableItem)
          ((TableItem) cell.getItem()).setForeground(foreground);
      } else
        cell.setForeground(foreground);

      /* Background */
      cell.setBackground(getBackground(scoredNews.getNews(), cell.getColumnIndex()));
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
     * @see org.rssowl.ui.internal.editors.feed.NewsTableLabelProvider#getColumnImage(java.lang.Object, org.rssowl.ui.internal.editors.feed.NewsColumn, int)
     */
    @Override
    protected Image getColumnImage(Object element, NewsColumn column, int colIndex) {

      /* Score Column */
      if (column == NewsColumn.RELEVANCE) {
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
      return super.getColumnImage(((ScoredNews) element).getNews(), column, colIndex);
    }

    /*
     * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
     */
    @Override
    public String getToolTipText(Object element) {
      ScoredNews scoredNews = (ScoredNews) element;
      INews news = scoredNews.getNews();
      FeedLinkReference feedRef = news.getFeedReference();
      IBookMark bookMark = CoreUtils.getBookMark(feedRef);

      String name = null;
      if (bookMark != null)
        name = bookMark.getName();
      else
        name = feedRef.getLinkAsText();

      if (news.getParentId() != 0) {
        INewsBin bin = DynamicDAO.load(INewsBin.class, news.getParentId());
        if (bin != null) {
          name = bin.getName() + ": " + name;
        }
      }

      return StringUtils.replaceAll(name, "&", "&&");
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
      Table table = (Table) event.widget;
      Point point = new Point(event.x, event.y);
      TableItem item = table.getItem(point);

      /* Only valid for Feed Column */
      if (item != null) {
        int feedIndex = indexOf(table, NewsColumn.FEED);
        if (feedIndex >= 0 && item.getBounds(feedIndex).contains(point))
          return super.getToolTipArea(event);
      }

      return null;
    }

    private static int indexOf(Table table, NewsColumn column) {
      if (table.isDisposed())
        return -1;

      TableColumn[] columns = table.getColumns();
      for (int i = 0; i < columns.length; i++)
        if (column == columns[i].getData(NewsColumnViewModel.COL_ID))
          return i;

      return -1;
    }

    public static void enableFor(ColumnViewer viewer) {
      new FeedColumnToolTipSupport(viewer, ToolTip.NO_RECREATE);
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
      conditions.add(factory.createSearchCondition(field, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(searchScope)));
    }

    /* Add default condition as well */
    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, ""));

    return conditions;
  }

  /**
   * @param parentShell
   * @param initialConditions A List of Conditions that should show initially.
   * @param matchAllConditions If <code>TRUE</code>, require all conditions to
   * match, <code>FALSE</code> otherwise.
   * @param runSearch If <code>TRUE</code>, run the search after the dialog
   * opened.
   */
  public SearchNewsDialog(Shell parentShell, List<ISearchCondition> initialConditions, boolean matchAllConditions, boolean runSearch) {
    super(parentShell);

    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fDialogSettings = Activator.getDefault().getDialogSettings();
    fFirstTimeOpen = (fDialogSettings.getSection(SETTINGS_SECTION) == null);
    fIsPreviewVisible = fPreferences.getBoolean(DefaultPreferences.SEARCH_DIALOG_PREVIEW_VISIBLE);
    fCachedWeights = fPreferences.getIntegers(PREF_SASH_WEIGHTS);
    fModelSearch = Owl.getPersistenceService().getModelSearch();
    fHandCursor = parentShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
    fMatchAllConditions = matchAllConditions;
    fRunSearch = runSearch;
    fNewsDao = DynamicDAO.getDAO(INewsDAO.class);

    /* Look for initial conditions and scope */
    if (initialConditions != null) {
      Pair<ISearchCondition, List<ISearchCondition>> conditions = CoreUtils.splitScope(initialConditions);
      fInitialScope = conditions.getFirst();
      fInitialConditions = conditions.getSecond();
    }
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

    /* Store Column Model */
    if (!fResultViewer.getTable().isDisposed()) {
      NewsColumnViewModel model = NewsColumnViewModel.initializeFrom(fResultViewer.getTable());
      model.setSortColumn(fNewsSorter.getSortBy());
      model.setAscending(fNewsSorter.isAscending());
      model.saveTo(fPreferences, true);
    }

    /* Store Preferences */
    fPreferences.putBoolean(DefaultPreferences.SEARCH_DIALOG_PREVIEW_VISIBLE, fIsPreviewVisible);
    if (fCachedWeights != null)
      fPreferences.putIntegers(PREF_SASH_WEIGHTS, fCachedWeights);

    /*
     * Workaround for Eclipse Bug 186025: The Virtual Manager is not cleared
     * when the TableViewer is disposed. Due to the hookListener() call, a
     * reference to the TableViewer is held in Memory, so we need to explicitly
     * clear the virtual manager.
     */
    fResultViewer.setItemCount(0);

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

    /* Title */
    setTitle("Search News");

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/search.gif"));

    /* Title Message */
    restoreInfoMessage(false);

    /* Sashform dividing search definition from results */
    fSashForm = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);
    fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fSashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* Top Area */
    Composite topSash = new Composite(fSashForm, SWT.NONE);
    topSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));

    Composite topSashContent = new Composite(topSash, SWT.None);
    topSashContent.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    topSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Condition Controls */
    createConditionControls(topSashContent);

    /* Create Center Sash */
    Composite centerSash = new Composite(fSashForm, SWT.NONE);
    centerSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    centerSash.addControlListener(new ControlAdapter() {

      @Override
      public void controlResized(ControlEvent e) {
        fCachedWeights = fSashForm.getWeights();
      }
    });

    /* Separator */
    new Label(centerSash, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

    Composite centerSashContent = new Composite(centerSash, SWT.None);
    centerSashContent.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    centerSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Viewer for Results */
    createResultViewer(centerSashContent);

    /* Create Bottom Sash */
    fBottomSash = new Composite(fSashForm, SWT.NONE);
    fBottomSash.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    fBottomSash.setVisible(fIsPreviewVisible);

    Composite bottomSashContent = new Composite(fBottomSash, SWT.None);
    bottomSashContent.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    bottomSashContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    bottomSashContent.setBackground(bottomSashContent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* Create Viewer for News Item */
    createBrowserViewer(bottomSashContent);

    /* Set weight to SashForm */
    if (fCachedWeights != null)
      fSashForm.setWeights(fCachedWeights);
    else
      fSashForm.setWeights(fIsPreviewVisible ? THREE_SASH_WEIGHTS : TWO_SASH_WEIGHTS);

    /* Separator */
    new Label(fBottomSash, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

    return fSashForm;
  }

  private void createBrowserViewer(Composite bottomSashContent) {
    fBrowserViewer = new NewsBrowserViewer(bottomSashContent, SWT.NONE) {
      @Override
      protected Collection<String> getHighlightedWords() {
        return CoreUtils.extractWords(fCurrentSearchConditions, false, true);
      }
    };
    fBrowserViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Create Content Provider */
    fBrowserViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Object[] && ((Object[]) inputElement).length > 0)
          inputElement = ((Object[]) inputElement)[0];

        if (inputElement instanceof NewsReference)
          return new Object[] { ((NewsReference) inputElement).resolve() };

        return new Object[] { inputElement };
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Create LabelProvider */
    fBrowserViewer.setLabelProvider(new NewsBrowserLabelProvider(fBrowserViewer));

    /* Set input when selection in result viewer changes */
    fResultViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        if (!selection.isEmpty() && fIsPreviewVisible) {
          fBrowserViewer.setInput(selection.getFirstElement());
          hideBrowser(false);
        }
      }
    });
  }

  private void createConditionControls(Composite container) {
    Composite topControlsContainer = new Composite(container, SWT.None);
    topControlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    topControlsContainer.setLayout(LayoutUtils.createGridLayout(5, 10, 3));

    /* Radio to select Condition Matching */
    fMatchAllRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAllRadio.setText("&Match all conditions");
    fMatchAllRadio.setSelection(fMatchAllConditions);

    fMatchAnyRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any c&ondition");
    fMatchAnyRadio.setSelection(!fMatchAllConditions);

    /* Separator */
    Label sep = new Label(topControlsContainer, SWT.SEPARATOR | SWT.VERTICAL);
    sep.setLayoutData(new GridData(SWT.DEFAULT, 20));

    /* Scope */
    Composite scopeContainer = new Composite(topControlsContainer, SWT.None);
    scopeContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    scopeContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));

    Label locationLabel = new Label(scopeContainer, SWT.NONE);
    locationLabel.setText("Search in: ");

    fLocationControl = new LocationControl(scopeContainer, SWT.WRAP) {
      @Override
      protected String getDefaultLabel() {
        return "All News";
      }
    };
    fLocationControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    ((GridData) fLocationControl.getLayoutData()).widthHint = 100;
    fLocationControl.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));

    /* ToolBar to add and select existing saved searches */
    final ToolBarManager dialogToolBar = new ToolBarManager(SWT.RIGHT | SWT.FLAT);

    /* Columns */
    IAction columnDropdown = new Action("Visible Columns", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {
        getMenuCreator().getMenu(dialogToolBar.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.COLUMNS;
      }
    };

    columnDropdown.setMenuCreator(new IMenuCreator() {
      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        MenuItem restoreDefaults = new MenuItem(menu, SWT.None);
        restoreDefaults.setText("&Restore Default Columns");
        restoreDefaults.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            NewsColumnViewModel defaultModel = NewsColumnViewModel.createDefault(true);
            if (!defaultModel.equals(fColumnModel))
              showColumns(defaultModel, true);
          }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        NewsColumn[] columns = NewsColumn.values();
        for (final NewsColumn column : columns) {
          if (column.isSelectable()) {
            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(column.getName());
            if (fColumnModel.contains(column))
              item.setSelection(true);

            item.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                if (fColumnModel.contains(column))
                  fColumnModel.removeColumn(column);
                else
                  fColumnModel.addColumn(column);

                showColumns(fColumnModel, true);
              }
            });
          }
        }

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }

      public void dispose() {}
    });

    dialogToolBar.add(columnDropdown);

    /* Separator */
    dialogToolBar.add(new Separator());

    /* Toggle Preview */
    final String previewActionId = "org.rssowl.ui.internal.dialogs.search.PreviewAction";
    IAction previewAction = new Action("&Preview Results", IAction.AS_CHECK_BOX) {
      @Override
      public void run() {
        fIsPreviewVisible = !fIsPreviewVisible;
        fSashForm.setWeights(fIsPreviewVisible ? THREE_SASH_WEIGHTS : TWO_SASH_WEIGHTS);
        fBottomSash.setVisible(fIsPreviewVisible);
        fSashForm.layout();
        dialogToolBar.find(previewActionId).update(IAction.TOOL_TIP_TEXT);

        /* Select and Show News if required */
        if (fIsPreviewVisible && fResultViewer.getTable().getItemCount() > 0) {

          /* Select first News if required */
          if (fResultViewer.getSelection().isEmpty())
            fResultViewer.getTable().select(0);

          /* Set input and Focus */
          fBrowserViewer.setInput(((IStructuredSelection) fResultViewer.getSelection()).getFirstElement());
          hideBrowser(false);
          fResultViewer.getTable().setFocus();

          /* Make sure to show the selection */
          fResultViewer.getTable().showSelection();
        }
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/etool16/highlight.gif");
      }

      @Override
      public String getToolTipText() {
        if (fIsPreviewVisible)
          return "Hide Preview";

        return "Show Preview";
      }
    };
    previewAction.setId(previewActionId);
    previewAction.setChecked(fIsPreviewVisible);
    dialogToolBar.add(previewAction);

    /* Separator */
    dialogToolBar.add(new Separator());

    /* Existing Saved Searches */
    IAction savedSearches = new Action("S&aved Searches", IAction.AS_DROP_DOWN_MENU) {
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
        Collection<ISearchMark> searchMarks = CoreUtils.loadSortedSearchMarks();
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
    dialogToolBar.getControl().setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true));

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

    /* Show Initial Scope if present */
    if (fInitialScope != null && fInitialScope.getValue() instanceof Long[][])
      fLocationControl.select((Long[][]) fInitialScope.getValue());

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

    Pair<ISearchCondition, List<ISearchCondition>> conditions = CoreUtils.splitScope(sm.getSearchConditions());

    /* Show Scope */
    Long[][] scope = null;
    if (conditions.getFirst() != null && conditions.getFirst().getValue() instanceof Long[][])
      scope = (Long[][]) conditions.getFirst().getValue();
    fLocationControl.select(scope);

    /* Show Conditions */
    fSearchConditionList.showConditions(conditions.getSecond());

    /* Unset Warning/Error Message */
    restoreInfoMessage(true);

    /* Layout */
    fLocationControl.getParent().getParent().getParent().layout(true, true);
  }

  private void restoreInfoMessage(boolean clearError) {
    if (clearError)
      setErrorMessage(null);
    setMessage("Use \'?\' for any character and \'*\' for any character sequence in your search. Surround words with quotes to search for phrases.", IMessageProvider.INFORMATION);
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
    fStatusLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSave();
      }
    });

    /* Search */
    Button searchButton = createButton(buttonBar, BUTTON_SEARCH, "&Search", true);
    ((GridData) searchButton.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) searchButton.getLayoutData()).grabExcessHorizontalSpace = false;

    /* Clear */
    createButton(buttonBar, BUTTON_CLEAR, "&Clear", false);

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

    /* Create Conditions */
    fCurrentSearchConditions = fSearchConditionList.createConditions();
    ISearchCondition locationCondition = fLocationControl.toScopeCondition();
    if (locationCondition != null)
      fCurrentSearchConditions.add(locationCondition);

    /* Make sure there is no Location Conflict */
    if (CoreUtils.isLocationConflict(fCurrentSearchConditions)) {
      setErrorMessage(null);
      setMessage("You are using a location condition together with a value for \"Search in\".", IMessageProvider.WARNING);
    }

    /* Unset Warning/Error Message */
    else
      restoreInfoMessage(true);

    final boolean matchAllConditions = fMatchAllRadio.getSelection();

    /* Disable Buttons and update Cursor */
    getButton(BUTTON_SEARCH).setEnabled(false);
    getShell().setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_APPSTARTING));

    JobRunner.runUIUpdater(new UIBackgroundJob(getShell()) {
      private List<ScoredNews> fResult = null;
      private Exception fException = null;

      @Override
      protected void runInBackground(IProgressMonitor monitor) {

        /* Perform Search in the Background */
        try {
          List<SearchHit<NewsReference>> searchHits = fModelSearch.searchNews(fCurrentSearchConditions, matchAllConditions);
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
        } catch (PersistenceException e) {
          fException = e;
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {

        /* Check for error first */
        if (fException != null) {
          setErrorMessage(fException.getMessage());
          fResult = Collections.emptyList();
        }

        /* Set Input (sorted) to Viewer */
        fResultViewer.setInput(fResult);

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

        /* Select First Result if Preview is visible */
        if (fIsPreviewVisible && size > 0) {
          fResultViewer.getTable().select(0);
          fResultViewer.getTable().showSelection();

          /* Set input and Focus */
          Object selection = ((IStructuredSelection) fResultViewer.getSelection()).getFirstElement();
          boolean refresh = selection.equals(fBrowserViewer.getInput());

          fBrowserViewer.setInput(selection);
          hideBrowser(false);
          fResultViewer.getTable().setFocus();

          if (refresh)
            fBrowserViewer.refresh();
        }

        /* Clear Browser Viewer otherwise */
        else if (fIsPreviewVisible)
          hideBrowser(true);
      }
    });
  }

  private void preload(List<ScoredNews> list) {
    for (int i = 0; i < list.size() && i < NUM_PRELOADED; i++) {
      list.get(i).getNews();
    }
  }

  private void hideBrowser(boolean hide) {
    if (hide) {
      fBrowserViewer.setInput(URIUtils.ABOUT_BLANK);
      fBrowserViewer.getControl().setVisible(false);
    } else
      fBrowserViewer.getControl().setVisible(true);
  }

  private void onClear() {

    /* Reset Conditions */
    fSearchConditionList.reset();
    fMatchAllRadio.setSelection(false);
    fMatchAnyRadio.setSelection(true);
    fResultViewer.setInput(Collections.emptyList());
    hideBrowser(true);

    /* Unset Warning/Error Message */
    restoreInfoMessage(true);

    /* Unset Status Message */
    fStatusLabel.setText("");
  }

  private void onSave() {
    List<ISearchCondition> conditions = fSearchConditionList.createConditions();

    /* Add default if empty */
    if (conditions.isEmpty())
      conditions.addAll(getDefaultConditions());

    ISearchCondition locationCondition = fLocationControl.toScopeCondition();
    if (locationCondition != null)
      conditions.add(locationCondition);

    SearchMarkDialog dialog = new SearchMarkDialog((Shell) getShell().getParent(), OwlUI.getBookMarkExplorerSelection(), null, conditions, fMatchAllRadio.getSelection());
    dialog.open();
  }

  private void createResultViewer(Composite centerSashContent) {

    /* Container for Table */
    Composite tableContainer = new Composite(centerSashContent, SWT.NONE);
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

    /* Custom Table */
    int style = SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL;
    fCustomTable = new CTable(tableContainer, style);

    /* Viewer */
    fResultViewer = new TableViewer(fCustomTable.getControl()) {
      @Override
      public ISelection getSelection() {
        StructuredSelection selection = (StructuredSelection) super.getSelection();
        return convertToNews(selection);
      }
    };
    fResultViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fResultViewer.setUseHashlookup(true);
    fResultViewer.getControl().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());
    fResultViewer.getTable().setHeaderVisible(true);

    /* Custom Tooltips for Feed Column */
    FeedColumnToolTipSupport.enableFor(fResultViewer);

    /* Separator */
    new Label(centerSashContent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Apply ContentProvider */
    fResultViewer.setContentProvider(getContentProvider());

    /* Create LabelProvider */
    NewsColumnViewModel model = NewsColumnViewModel.loadFrom(fPreferences, true);
    fNewsTableLabelProvider = new ScoredNewsLabelProvider(model);
    if (USE_CUSTOM_OWNER_DRAWN) {
      fResultViewer.getControl().addListener(SWT.EraseItem, new Listener() {
        public void handleEvent(Event event) {
          Object element = event.item.getData();
          fNewsTableLabelProvider.erase(event, element);
        }
      });
    }

    /* Create Sorter */
    fNewsSorter = new ScoredNewsComparator();
    fResultViewer.setComparator(fNewsSorter);

    /* Create the Columns */
    showColumns(model, false);

    /* Hook Contextual Menu */
    hookContextualMenu();

    /* Drag and Drop */
    initDragAndDrop();

    /* Register Listeners */
    registerListeners();
  }

  private void showColumns(NewsColumnViewModel model, boolean update) {
    fResultViewer.getTable().setRedraw(false);
    try {

      /* Dispose Old */
      fCustomTable.clear();

      /* Keep as current */
      fColumnModel = model;

      /* Create New */
      List<NewsColumn> cols = model.getColumns();
      for (NewsColumn col : cols) {
        TableViewerColumn viewerColumn = new TableViewerColumn(fResultViewer, SWT.LEFT);
        fCustomTable.manageColumn(viewerColumn.getColumn(), model.getLayoutData(col), col.showName() ? col.getName() : null, col.showTooltip() ? col.getName() : null, null, col.isMoveable(), col.isResizable());
        viewerColumn.getColumn().setData(NewsColumnViewModel.COL_ID, col);

        if (model.getSortColumn() == col && col.showSortIndicator()) {
          fCustomTable.getControl().setSortColumn(viewerColumn.getColumn());
          fCustomTable.getControl().setSortDirection(model.isAscending() ? SWT.UP : SWT.DOWN);
        }
      }

      /* Enable Sorting adding listeners to Columns */
      TableColumn[] columns = fResultViewer.getTable().getColumns();
      for (final TableColumn column : columns) {
        column.addSelectionListener(new SelectionAdapter() {
          @SuppressWarnings("unchecked")
          @Override
          public void widgetSelected(SelectionEvent e) {
            NewsColumn oldSortBy = fNewsSorter.getSortBy();
            NewsColumn newSortBy = (NewsColumn) column.getData(NewsColumnViewModel.COL_ID);
            boolean defaultAscending = newSortBy.prefersAscending();
            boolean ascending = (oldSortBy != newSortBy) ? defaultAscending : !fNewsSorter.isAscending();

            fNewsSorter.setSortBy(newSortBy);
            fNewsSorter.setAscending(ascending);

            /* Indicate Sort-Column in UI for Columns that have a certain width */
            if (newSortBy.showSortIndicator()) {
              fResultViewer.getTable().setSortColumn(column);
              fResultViewer.getTable().setSortDirection(ascending ? SWT.UP : SWT.DOWN);
            } else {
              fResultViewer.getTable().setSortColumn(null);
            }

            /* Since Virtual Style is set, we have to sort the model manually */
            Collections.sort(((List<ScoredNews>) fResultViewer.getInput()), fNewsSorter);
            fResultViewer.refresh(false);
          }
        });
      }

      /* Update Table */
      if (update)
        fCustomTable.update();

      /* Update Sorter */
      fNewsSorter.setAscending(model.isAscending());
      fNewsSorter.setSortBy(model.getSortColumn());

      /* Set Label Provider */
      fNewsTableLabelProvider.init(model);
      fResultViewer.setLabelProvider(fNewsTableLabelProvider);

      /* Refresh if necessary */
      if (update)
        fResultViewer.refresh(true);
    } finally {
      fResultViewer.getTable().setRedraw(true);
    }
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE;
    Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance(), URLTransfer.getInstance() };

    /* Drag Support */
    fResultViewer.addDragSupport(ops, transfers, new DragSourceListener() {
      public void dragStart(final DragSourceEvent event) {
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            LocalSelectionTransfer.getTransfer().setSelection(fResultViewer.getSelection());
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
      String linkAsText = news.iterator().next().getLinkAsText();
      if (StringUtils.isSet(linkAsText))
        event.data = linkAsText;
    }
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
    fResultViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onMouseDoubleClick(event);
      }
    });

    /* Perform Action on Mouse-Down */
    fResultViewer.getControl().addListener(SWT.MouseDown, new Listener() {
      public void handleEvent(Event event) {
        onMouseDown(event);
      }
    });

    /* Update Cursor on Mouse-Move */
    fResultViewer.getControl().addListener(SWT.MouseMove, new Listener() {
      public void handleEvent(Event event) {
        onMouseMove(event);
      }
    });

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
        JobRunner.runInUIThread(fResultViewer.getTable(), new Runnable() {
          public void run() {
            fResultViewer.refresh(true);
          }
        });
      }
    };
    DynamicDAO.addEntityListener(ILabel.class, fLabelListener);
  }

  private void onNewsEvent(final Set<NewsEvent> events) {

    /* No Result set yet */
    if (fResultViewer.getInput() == null)
      return;

    /* Check for Update / Deleted News */
    JobRunner.runUIUpdater(new UIBackgroundJob(getShell()) {
      private List<ScoredNews> fDeletedScoredNews;
      private List<ScoredNews> fUpdatedScoredNews;
      private Set<NewsEvent> fUpdatedNewsEvents;

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        List<?> input = (List<?>) fResultViewer.getInput();
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

                if (fUpdatedNewsEvents == null)
                  fUpdatedNewsEvents = new HashSet<NewsEvent>();
                fUpdatedNewsEvents.add(event);
              }
            }
          }
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {

        /* News got Deleted */
        if (fDeletedScoredNews != null)
          fResultViewer.remove(fDeletedScoredNews.toArray());

        /* News got Updated */
        if (fUpdatedScoredNews != null)
          fResultViewer.update(fUpdatedScoredNews.toArray(), null);

        /* Update Browser Viewer if visible */
        if (fBrowserViewer.getControl().isVisible()) {
          Object input = fBrowserViewer.getInput();

          if (fUpdatedNewsEvents != null) {
            for (NewsEvent event : fUpdatedNewsEvents) {
              if (event.getEntity().equals(input)) {
                fBrowserViewer.update(Collections.singleton(event));
                break; // Viewer only shows 1 News at maximum
              }
            }
          }

          if (fDeletedScoredNews != null) {
            for (ScoredNews news : fDeletedScoredNews) {
              if (news.getNews().equals(input)) {
                fBrowserViewer.remove(news.getNews());
                break; // Viewer only shows 1 News at maximum
              }
            }
          }
        }
      }
    });
  }

  private void onMouseDown(Event event) {
    Point p = new Point(event.x, event.y);
    TableItem item = fResultViewer.getTable().getItem(p);

    /* Problem - return */
    if (item == null || item.isDisposed())
      return;

    /* Mouse-Up over Read-State-Column */
    if (event.button == 1 && isInImageBounds(item, NewsColumn.TITLE, p)) {
      Object data = item.getData();

      /* Toggle State between Read / Unread */
      if (data instanceof ScoredNews) {
        INews news = ((ScoredNews) data).getNews();
        INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD : INews.State.READ;
        setNewsState(new ArrayList<INews>(Arrays.asList(new INews[] { news })), newState);
      }
    }

    /* Mouse-Up over Sticky-State-Column */
    else if (event.button == 1 && isInImageBounds(item, NewsColumn.STICKY, p)) {
      Object data = item.getData();

      /* Toggle State between Sticky / Not Sticky */
      if (data instanceof ScoredNews) {
        new MakeNewsStickyAction(new StructuredSelection(((ScoredNews) data).getNews())).run();
      }
    }
  }

  private void onMouseMove(Event event) {
    Point p = new Point(event.x, event.y);
    TableItem item = fResultViewer.getTable().getItem(p);

    /* Problem / Group hovered - reset */
    if (item == null || item.isDisposed() || item.getData() instanceof EntityGroup) {
      if (fShowsHandCursor && !fResultViewer.getControl().isDisposed()) {
        fResultViewer.getControl().setCursor(null);
        fShowsHandCursor = false;
      }
      return;
    }

    /* Show Hand-Cursor if action can be performed */
    boolean changeToHandCursor = isInImageBounds(item, NewsColumn.TITLE, p) || isInImageBounds(item, NewsColumn.STICKY, p);
    if (!fShowsHandCursor && changeToHandCursor) {
      fResultViewer.getControl().setCursor(fHandCursor);
      fShowsHandCursor = true;
    } else if (fShowsHandCursor && !changeToHandCursor) {
      fResultViewer.getControl().setCursor(null);
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
        IStructuredSelection selection = (IStructuredSelection) fResultViewer.getSelection();

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
            moveMenu.add(new MoveCopyNewsToBinAction(selection, bin, true));
          }

          moveMenu.add(new Separator("movetonewbin"));
          moveMenu.add(new MoveCopyNewsToBinAction(selection, null, true));

          /* Copy To */
          MenuManager copyMenu = new MenuManager("Copy To", "copyto");
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            copyMenu.add(new MoveCopyNewsToBinAction(selection, bin, false));
          }

          copyMenu.add(new Separator("copytonewbin"));
          copyMenu.add(new MoveCopyNewsToBinAction(selection, null, false));
        }

        /* Mark / Label */
        if (!selection.isEmpty()) {
          manager.add(new Separator("mark"));

          /* Mark */
          MenuManager markMenu = new MenuManager("Mark", "mark");
          manager.add(markMenu);

          /* Mark as Read */
          IAction action = new ToggleReadStateAction(selection);
          action.setEnabled(!selection.isEmpty());
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
            labelMenu.add(new AssignLabelsAction(getShell(), selection));
            labelMenu.add(new Action("Organize Labels...") {
              @Override
              public void run() {
                PreferencesUtil.createPreferenceDialogOn(getShell(), ManageLabelsPreferencePage.ID, null, null).open();
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

        manager.add(new Separator("filter"));
        manager.add(new Separator("edit"));
        manager.add(new Separator("copy"));
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });

    /* Create and Register with Workbench */
    Menu menu = manager.createContextMenu(fResultViewer.getControl());
    fResultViewer.getControl().setMenu(menu);

    /* Register with Part Site */
    IWorkbenchWindow window = OwlUI.getWindow();
    if (window != null) {
      IWorkbenchPart activePart = window.getPartService().getActivePart();
      if (activePart != null && activePart.getSite() != null)
        activePart.getSite().registerContextMenu(manager, fResultViewer);
    }
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

  private List<ISearchCondition> getDefaultConditions() {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    IModelFactory factory = Owl.getModelFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "");

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

    /* Add to UndoStack */
    UndoStack.getInstance().addOperation(new NewsStateOperation(news, state, true));

    /* Perform Operation */
    Owl.getPersistenceService().getDAOService().getNewsDAO().setState(news, state, true, false);
  }

  private int indexOf(NewsColumn column) {
    Table table = fCustomTable.getControl();
    if (table.isDisposed())
      return -1;

    TableColumn[] columns = table.getColumns();
    for (int i = 0; i < columns.length; i++) {
      if (column == columns[i].getData(NewsColumnViewModel.COL_ID))
        return i;
    }

    return -1;
  }

  private boolean isInImageBounds(TableItem item, NewsColumn column, Point p) {
    int index = indexOf(column);
    if (index == -1)
      return false;

    return item.getImageBounds(index).contains(p);
  }
}