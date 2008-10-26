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

import java.util.ArrayList;
import java.util.Collection;
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
  private NewsActionList fFilterActionList;
  private Text fNameInput;
  private int fFilterPosition;

  /**
   * @param parentShell the Shell to create this Dialog on.
   */
  public NewsFilterDialog(Shell parentShell) {
    this(parentShell, null);
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

    /* Ensure that a Search Condition is specified */
    if (fSearchConditionList.isEmpty()) {
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

    /* Create new Filter */
    if (fEditedFilter == null) {
      IModelFactory factory = Owl.getModelFactory();

      /* Create Conditions */
      List<ISearchCondition> conditions = fSearchConditionList.createConditions();
      ISearch search = factory.createSearch(null, fNameInput.getText());
      search.setMatchAllConditions(fMatchAllRadio.getSelection());
      for (ISearchCondition condition : conditions) {
        search.addSearchCondition(condition);
      }

      /* Create Actions */
      ISearchFilter filter = factory.createSearchFilter(null, search, fNameInput.getText());
      filter.setEnabled(true);
      filter.setOrder(fFilterPosition);
      for (IFilterAction action : actions) {
        filter.addAction(action);
      }

      /* Save search Filter */
      DynamicDAO.save(filter);
    }

    /* Update existing Filter */
    else {
      fEditedFilter.setName(fNameInput.getText());
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

      /* Update Search Actions */
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

      /* Update search Filter */
      DynamicDAO.save(fEditedFilter);
    }

    super.okPressed();
  }

  private boolean isConflicting(List<IFilterAction> actions) {
    for (IFilterAction action : actions) {
      NewsActionDescriptor newsAction = fNewsActionPresentationManager.getNewsActionDescriptor(action.getActionId());
      for (IFilterAction otherAction : actions) {
        if (action == otherAction)
          continue;

        NewsActionDescriptor otherNewsAction = fNewsActionPresentationManager.getNewsActionDescriptor(otherAction.getActionId());
        if (otherNewsAction.getNewsAction().isConflicting(newsAction.getNewsAction())) {
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
    Composite nameContainer = new Composite(parent, SWT.None);
    nameContainer.setLayout(LayoutUtils.createGridLayout(2, 10, 5, 0, 0, false));
    nameContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Label nameLabel = new Label(nameContainer, SWT.NONE);
    nameLabel.setText("Name: ");

    fNameInput = new Text(nameContainer, SWT.SINGLE | SWT.BORDER);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    if (fEditedFilter != null) {
      fNameInput.setText(fEditedFilter.getName());
      fNameInput.selectAll();
    }

    fNameInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        setErrorMessage(null);
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

  private void createConditionControls(Composite container) {
    Composite topControlsContainer = new Composite(container, SWT.None);
    topControlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    topControlsContainer.setLayout(LayoutUtils.createGridLayout(3, 10, 0));

    boolean matchAllConditions = (fEditedFilter != null) ? fEditedFilter.getSearch().matchAllConditions() : true;

    /* Radio to select Condition Matching */
    fMatchAllRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAllRadio.setText("&Match all conditions");
    fMatchAllRadio.setSelection(matchAllConditions);

    fMatchAnyRadio = new Button(topControlsContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any c&ondition");
    fMatchAnyRadio.setSelection(!matchAllConditions);

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
    if (fEditedFilter != null)
      fSearchConditionList.showConditions(fEditedFilter.getSearch().getSearchConditions());
  }

  private boolean isSupported(ISearchMark searchmark) {
    List<ISearchCondition> conditions = searchmark.getSearchConditions();
    for (ISearchCondition condition : conditions) {
      if (fExcludedConditions.contains(condition.getField().getId()))
        return false;
    }

    return true;
  }

  private List<Integer> getExcludedConditions() {
    List<Integer> conditionsToExclude = new ArrayList<Integer>(3);

    conditionsToExclude.add(INews.STATE);
    conditionsToExclude.add(INews.IS_FLAGGED);
    conditionsToExclude.add(INews.LABEL);

    return conditionsToExclude;
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
    fFilterActionList.setVisibleItemCount(2);

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
}