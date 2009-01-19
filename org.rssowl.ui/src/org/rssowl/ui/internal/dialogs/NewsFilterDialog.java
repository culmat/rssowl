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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.filter.NewsActionDescriptor;
import org.rssowl.ui.internal.filter.NewsActionList;
import org.rssowl.ui.internal.filter.NewsActionPresentationManager;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A dialog to add and update a news filter with search conditions and actions.
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class NewsFilterDialog extends TitleAreaDialog {
  private LocalResourceManager fResources;
  private final ISearchFilter fEditedFilter;
  private final List<Integer> fExcludedConditions = getExcludedConditions();
  private SearchConditionList fSearchConditionList;
  private NewsActionPresentationManager fNewsActionPresentationManager = NewsActionPresentationManager.getInstance();
  private Button fMatchAllRadio;
  private Button fMatchAnyRadio;
  private Button fMatchAllNewsRadio;
  private NewsActionList fFilterActionList;
  private Text fNameInput;
  private int fFilterPosition;
  private ISearch fPresetSearch;
  private ISearchFilter fAddedFilter;

  /**
   * @param parentShell the Shell to create this Dialog on.
   */
  public NewsFilterDialog(Shell parentShell) {
    this(parentShell, (ISearchFilter) null);
  }

  /**
   * @param parentShell the Shell to create this Dialog on.
   * @param filter the {@link ISearchFilter} to edit or <code>null</code> if
   * none.
   */
  public NewsFilterDialog(Shell parentShell, ISearchFilter filter) {
    super(parentShell);

    fEditedFilter = filter;
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /**
   * @param parentShell the Shell to create this Dialog on.
   * @param presetSearch a search that is preset in the condition area.
   */
  public NewsFilterDialog(Shell parentShell, ISearch presetSearch) {
    super(parentShell);

    fPresetSearch = presetSearch;
    fEditedFilter = null;
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /**
   * @param filterPosition the sort order for the resulting news filter.
   */
  void setFilterPosition(int filterPosition) {
    fFilterPosition = filterPosition;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {

    /* Ensure that Filter Name is set */
    if (!StringUtils.isSet(fNameInput.getText())) {
      setErrorMessage("Please enter a name for the news filter.");
      fNameInput.setFocus();
      return;
    }

    /* Ensure that a Search Condition is specified if required */
    if (fSearchConditionList.isEmpty() && !fMatchAllNewsRadio.getSelection()) {
      setErrorMessage("Please enter at least one search condition for the news filter.");
      fSearchConditionList.focusInput();
      return;
    }

    /* Ensure that an Action is specified */
    if (fFilterActionList.isEmpty()) {
      setErrorMessage("Please choose at least one action for the news filter.");
      fFilterActionList.focusInput();
      return;
    }

    /* Ensure that Actions are not conflicting */
    List<IFilterAction> actions = fFilterActionList.createActions();
    if (isConflicting(actions))
      return;

    /* Create new Filter and save */
    if (fEditedFilter == null) {
      fAddedFilter = createFilter(actions);
      DynamicDAO.save(fAddedFilter);
    }

    /* Update existing Filter */
    else {
      updateFilter(actions);
      DynamicDAO.save(fEditedFilter);
    }

    super.okPressed();
  }

  /**
   * @return the {@link ISearchFilter} that was added or updated.
   */
  public ISearchFilter getFilter() {
    return fEditedFilter != null ? fEditedFilter : fAddedFilter;
  }

  private ISearchFilter createFilter(List<IFilterAction> actions) {
    IModelFactory factory = Owl.getModelFactory();
    ISearch search = null;

    /* Create Conditions unless filter should match all News */
    if (!fMatchAllNewsRadio.getSelection()) {
      List<ISearchCondition> conditions = fSearchConditionList.createConditions();
      search = factory.createSearch(null);
      search.setMatchAllConditions(fMatchAllRadio.getSelection());
      for (ISearchCondition condition : conditions) {
        search.addSearchCondition(condition);
      }
    }

    /* Create Actions */
    ISearchFilter filter = factory.createSearchFilter(null, search, fNameInput.getText());
    filter.setEnabled(true);
    filter.setMatchAllNews(fMatchAllNewsRadio.getSelection());
    filter.setOrder(fFilterPosition);
    for (IFilterAction action : actions) {
      filter.addAction(action);
    }

    return filter;
  }

  private void updateFilter(List<IFilterAction> actions) {

    /* Name */
    fEditedFilter.setName(fNameInput.getText());

    /* Actions */
    if (fFilterActionList.isModified()) {

      /* Remove Old Actions */
      List<IFilterAction> oldActions = fEditedFilter.getActions();
      for (IFilterAction oldAction : oldActions) {
        fEditedFilter.removeAction(oldAction);
      }

      /* Add New Actions */
      for (IFilterAction action : actions) {
        fEditedFilter.addAction(action);
      }
    }

    /* Search got Added */
    if (fEditedFilter.matchAllNews() && !fMatchAllNewsRadio.getSelection()) {
      fEditedFilter.setMatchAllNews(false);

      IModelFactory factory = Owl.getModelFactory();
      List<ISearchCondition> conditions = fSearchConditionList.createConditions();
      ISearch search = factory.createSearch(null);
      search.setMatchAllConditions(fMatchAllRadio.getSelection());
      for (ISearchCondition condition : conditions) {
        search.addSearchCondition(condition);
      }

      fEditedFilter.setSearch(search);
    }

    /* Search got Deleted */
    if (!fEditedFilter.matchAllNews() && fMatchAllNewsRadio.getSelection()) {
      fEditedFilter.setMatchAllNews(true);
      fEditedFilter.setSearch(null);
    }

    /* Search got Updated */
    if (!fEditedFilter.matchAllNews() && !fMatchAllNewsRadio.getSelection()) {
      ISearch editedSearch = fEditedFilter.getSearch();
      editedSearch.setMatchAllConditions(fMatchAllRadio.getSelection());

      /* Update Search Conditions */
      if (fSearchConditionList.isModified()) {

        /* Remove Old Conditions */
        List<ISearchCondition> oldConditions = editedSearch.getSearchConditions();
        for (ISearchCondition oldCondition : oldConditions) {
          editedSearch.removeSearchCondition(oldCondition);
        }

        /* Add New Conditions */
        List<ISearchCondition> conditions = fSearchConditionList.createConditions();
        for (ISearchCondition condition : conditions) {
          editedSearch.addSearchCondition(condition);
        }
      }
    }
  }

  private boolean isConflicting(List<IFilterAction> actions) {
    for (IFilterAction action : actions) {
      NewsActionDescriptor newsAction = fNewsActionPresentationManager.getNewsActionDescriptor(action.getActionId());
      for (IFilterAction otherAction : actions) {
        if (action == otherAction)
          continue;

        NewsActionDescriptor otherNewsAction = fNewsActionPresentationManager.getNewsActionDescriptor(otherAction.getActionId());
        if (otherNewsAction.getNewsAction().conflictsWith(newsAction.getNewsAction())) {
          StringBuilder str = new StringBuilder();
          str.append("Please remove the action '").append(otherNewsAction.getName()).append("'. It can not be used together with the action '").append(newsAction.getName()).append("'.");

          setErrorMessage(str.toString());
          return true;
        }
      }
    }

    return false;
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
    boolean res = super.close();
    fResources.dispose();
    return res;
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/filter_wiz.gif"));

    /* Title Message */
    setMessage("Please define the search conditions and actions to perform on matching news.", IMessageProvider.INFORMATION);

    /* Name Input Filed */
    Composite container = new Composite(parent, SWT.None);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 5, 0, 0, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Label nameLabel = new Label(container, SWT.NONE);
    nameLabel.setText("Name: ");

    Composite nameContainer = new Composite(container, SWT.BORDER);
    nameContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    nameContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    nameContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    fNameInput = new Text(nameContainer, SWT.SINGLE);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    if (fEditedFilter != null) {
      fNameInput.setText(fEditedFilter.getName());
      fNameInput.selectAll();
    }

    fNameInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        setErrorMessage(null);
      }
    });

    ToolBar generateTitleBar = new ToolBar(nameContainer, SWT.FLAT);
    generateTitleBar.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    ToolItem generateTitleItem = new ToolItem(generateTitleBar, SWT.PUSH);
    generateTitleItem.setImage(OwlUI.getImage(fResources, "icons/etool16/info.gif"));
    generateTitleItem.setToolTipText("Create name from conditions");
    generateTitleItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onGenerateName();
      }
    });

    /* Create Condition Controls */
    Composite conditionContainer = new Composite(parent, SWT.NONE);
    conditionContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    conditionContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    createConditionControls(conditionContainer);

    /* Label in between */
    Composite labelContainer = new Composite(parent, SWT.NONE);
    labelContainer.setLayout(LayoutUtils.createGridLayout(1, 10, 5, 0, 0, false));
    labelContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    Label explanationLabel = new Label(labelContainer, SWT.NONE);
    explanationLabel.setText("For news matching the above conditions perform these actions:");

    /* Create Action Controls */
    Composite actionContainer = new Composite(parent, SWT.NONE);
    actionContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 0, false));
    actionContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    createActionControls(actionContainer);

    /* Separator */
    new Label(actionContainer, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.END, true, false));

    return parent;
  }

  void onGenerateName() {
    String name;
    if (fMatchAllNewsRadio.getSelection())
      name = "All News";
    else {
      List<ISearchCondition> conditions = fSearchConditionList.createConditions();
      name = ModelUtils.getName(conditions, fMatchAllRadio.getSelection());
    }

    if (name.length() > 0) {
      fNameInput.setText(name);
      fNameInput.selectAll();
    }
  }

  private void createConditionControls(Composite container) {
    Composite topControlsContainer = new Composite(container, SWT.None);
    topControlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    topControlsContainer.setLayout(LayoutUtils.createGridLayout(4, 10, 0));

    boolean matchAllNews = (fEditedFilter != null) ? fEditedFilter.matchAllNews() : false;
    boolean matchAllConditions = !matchAllNews && (fEditedFilter != null) ? fEditedFilter.getSearch().matchAllConditions() : true;

    if (fPresetSearch != null)
      matchAllConditions = fPresetSearch.matchAllConditions();

    /* Radio to select Condition Matching */
    fMatchAllRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAllRadio.setText("&Match all conditions");
    fMatchAllRadio.setSelection(matchAllConditions && !matchAllNews);

    fMatchAnyRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any c&ondition");
    fMatchAnyRadio.setSelection(!matchAllConditions && !matchAllNews);

    fMatchAllNewsRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAllNewsRadio.setText("Match all &News");
    fMatchAllNewsRadio.setSelection(matchAllNews);
    fMatchAllNewsRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        setControlEnabled(fSearchConditionList, !fMatchAllNewsRadio.getSelection());
      }
    });

    /* ToolBar to add and select existing saved searches */
    final ToolBarManager dialogToolBar = new ToolBarManager(SWT.RIGHT | SWT.FLAT);

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
        Collection<ISearchMark> searchMarks = DynamicDAO.loadAll(ISearchMark.class);
        Menu menu = new Menu(parent);

        /* Show Existing Saved Searches */
        for (final ISearchMark searchMark : searchMarks) {
          if (isSupported(searchMark)) {
            MenuItem item = new MenuItem(menu, SWT.None);
            item.setText(searchMark.getName());
            item.setImage(OwlUI.getImage(fResources, OwlUI.SEARCHMARK));
            item.addSelectionListener(new SelectionAdapter() {

              @Override
              public void widgetSelected(SelectionEvent e) {

                /* Match Conditions */
                fMatchAllRadio.setSelection(searchMark.matchAllConditions());
                fMatchAnyRadio.setSelection(!searchMark.matchAllConditions());
                fMatchAllNewsRadio.setSelection(false);
                setControlEnabled(fSearchConditionList, !fMatchAllNewsRadio.getSelection());

                /* Show Conditions */
                fSearchConditionList.showConditions(searchMark.getSearchConditions());
              }
            });
          }
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
        gc.drawLine(clArea.x, clArea.y + clArea.height - 1, clArea.x + clArea.width, clArea.y + clArea.height - 1);
      }
    });

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, getDefaultConditions(), fExcludedConditions);
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fSearchConditionList.setVisibleItemCount(3);

    /* Show Initial Conditions if present */
    if (fEditedFilter != null && fEditedFilter.getSearch() != null)
      fSearchConditionList.showConditions(fEditedFilter.getSearch().getSearchConditions());
    else if (fPresetSearch != null)
      fSearchConditionList.showConditions(fPresetSearch.getSearchConditions());

    /* Update Enable-State of Search Condition List */
    setControlEnabled(fSearchConditionList, !fMatchAllNewsRadio.getSelection());
  }

  private void setControlEnabled(Control control, boolean enabled) {
    control.setEnabled(enabled);
    if (control instanceof Composite) {
      Composite composite = (Composite) control;
      Control[] children = composite.getChildren();
      for (Control child : children) {
        setControlEnabled(child, enabled);
      }
    }
  }

  private boolean isSupported(ISearchMark searchmark) {
    List<ISearchCondition> conditions = searchmark.getSearchConditions();
    for (ISearchCondition condition : conditions) {
      if (fExcludedConditions.contains(condition.getField().getId()))
        return false;
    }

    return true;
  }

  /* We allow all conditions because a filter could also be run on existing news! */
  private List<Integer> getExcludedConditions() {
    return Collections.emptyList();
  }

  private List<ISearchCondition> getDefaultConditions() {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    IModelFactory factory = Owl.getModelFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "");

    conditions.add(condition);

    return conditions;
  }

  private void createActionControls(Composite container) {

    /* Container for Actions */
    final Composite actionsContainer = new Composite(container, SWT.NONE);
    actionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    actionsContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 10));
    actionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    actionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);
    actionsContainer.addPaintListener(new PaintListener() {
      public void paintControl(PaintEvent e) {
        GC gc = e.gc;
        Rectangle clArea = actionsContainer.getClientArea();
        gc.setForeground(actionsContainer.getDisplay().getSystemColor(SWT.COLOR_GRAY));
        gc.drawLine(clArea.x, clArea.y, clArea.x + clArea.width, clArea.y);
      }
    });

    /* Action List */
    fFilterActionList = new NewsActionList(actionsContainer, SWT.NONE, getDefaultActions());
    fFilterActionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fFilterActionList.setVisibleItemCount(3);

    /* Show initial Actions if present */
    if (fEditedFilter != null)
      fFilterActionList.showActions(fEditedFilter.getActions());
  }

  private List<IFilterAction> getDefaultActions() {
    List<IFilterAction> defaultActions = new ArrayList<IFilterAction>(1);

    IModelFactory factory = Owl.getModelFactory();
    defaultActions.add(factory.createFilterAction(MoveNewsAction.ID));

    return defaultActions;
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    if (fEditedFilter == null)
      shell.setText("New News Filter");
    else
      shell.setText("Edit News Filter '" + fEditedFilter.getName() + "'");
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#isResizable()
   */
  @Override
  protected boolean isResizable() {
    return true;
  }
}