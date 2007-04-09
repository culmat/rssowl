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

package org.rssowl.ui.internal.search;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.INews.State;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.persist.search.ISearchField;
import org.rssowl.core.model.persist.search.SearchSpecifier;
import org.rssowl.core.util.Pair;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The <code>SearchConditionList</code> is a scrolled composite that allows to
 * define a set of <code>ISearchCondition</code>s. The UI allows to add,
 * remove and edit single Conditions.
 *
 * @author bpasero
 */
public class SearchConditionList extends ScrolledComposite {
  private List<SearchConditionItem> fItems;
  private Composite fContainer;
  private LocalResourceManager fResources;
  private Image fAddIcon;
  private Image fDeleteIcon;
  private boolean fModified;
  private int fVisibleItemCount = 3;
  private StateConditionControl fStateConditionControl;

  /**
   * @param parent The parent Composite.
   * @param style The Style as defined by SWT constants.
   * @param conditions The initial conditions this List is showing.
   */
  public SearchConditionList(Composite parent, int style, List<ISearchCondition> conditions) {
    super(parent, style | SWT.V_SCROLL);
    fItems = new ArrayList<SearchConditionItem>();
    fResources = new LocalResourceManager(JFaceResources.getResources(), this);

    initResources();
    initComponents(conditions);
  }

  /**
   * Sets the number of <code>SearchConditionItem</code>s that should be
   * visible in the List. If the number of items is higher, scrollbars will be
   * shown automatically.
   *
   * @param count the number of <code>SearchConditionItem</code>s that should
   * be visible in the List.
   */
  public void setVisibleItemCount(int count) {
    Assert.isLegal(count >= 0);
    fVisibleItemCount = count;
  }

  /**
   * Returns <code>TRUE</code> when this List has no items with a specific
   * value, and <code>FALSE</code> otherwise.
   *
   * @return <code>TRUE</code> when this List has no items with a specific
   * value, and <code>FALSE</code> otherwise.
   */
  public boolean isEmpty() {
    if (fStateConditionControl.hasSelected())
      return false;

    for (SearchConditionItem item : fItems) {
      if (item.hasValue())
        return false;
    }

    return true;
  }

  /*
   * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
   */
  @Override
  public Point computeSize(int wHint, int hHint, boolean changed) {
    Point point = super.computeSize(wHint, hHint, changed);

    /* Compute from Condition Item */
    if (fVisibleItemCount > 0 && fItems.size() > 0) {
      int itemHeight = fItems.get(0).computeSize(wHint, hHint).y + 2;
      point.y = fVisibleItemCount * itemHeight;
    }

    /* Compute from State Control */
    else if (fVisibleItemCount > 0) {
      int itemHeight = fStateConditionControl.computeSize(wHint, hHint).y + 8;
      point.y = fVisibleItemCount * itemHeight;
    }

    return point;
  }

  /**
   * Removes all but the first Items from the List.
   */
  public void reset() {
    setRedraw(false);
    try {

      /* Remove all Items */
      if (fItems.size() > 0) {
        List<SearchConditionItem> itemsToRemove = new ArrayList<SearchConditionItem>(fItems);
        for (SearchConditionItem itemToRemove : itemsToRemove) {
          itemToRemove.getParent().dispose();
          removeItem(itemToRemove);
        }
      }

      /* Clear State Control */
      fStateConditionControl.select(null);

      /* Add default */
      addItem(getDefaultCondition());
    } finally {
      setRedraw(true);
    }

    focusInput();
    fModified = true;
  }

  /**
   * Passes focus to the first item of this List.
   */
  public void focusInput() {
    if (!fItems.isEmpty())
      fItems.get(0).focusInput();
  }

  /**
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see SearchConditionList#createConditions(ISearchMark)
   */
  public List<ISearchCondition> createConditions() {
    return createConditions(null);
  }

  /**
   * @param searchmark The parent of the <code>ISearchCondition</code>s that
   * are being created.
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see SearchConditionList#createConditions()
   */
  public List<ISearchCondition> createConditions(ISearchMark searchmark) {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    /* State Conditions */
    conditions.addAll(fStateConditionControl.createConditions(searchmark));

    /* Non State Conditions */
    for (SearchConditionItem item : fItems) {
      ISearchCondition condition = item.createCondition(searchmark, true);
      if (condition != null)
        conditions.add(condition);
    }

    return conditions;
  }

  /**
   * Shows the List of <code>ISearchCondition</code> in this List.
   *
   * @param conditions the List of <code>ISearchCondition</code> to show in
   * this List.
   */
  public void showConditions(List<ISearchCondition> conditions) {

    /* Split Conditions (state / non state) */
    Pair<EnumSet<State>, List<ISearchCondition>> pair = splitConditions(conditions);

    fModified = true;
    setRedraw(false);
    try {

      /* Update State Control */
      fStateConditionControl.select(pair.getFirst());

      /* Remove all */
      List<SearchConditionItem> itemsToRemove = new ArrayList<SearchConditionItem>(fItems);
      for (SearchConditionItem itemToRemove : itemsToRemove) {
        itemToRemove.getParent().dispose();
        removeItem(itemToRemove);
      }

      /* Add non-state Conditions */
      if (pair.getSecond() != null) {
        for (ISearchCondition condition : pair.getSecond())
          addItem(condition);
      }
    } finally {
      setRedraw(true);
    }
  }

  /**
   * Optimization: In order to check weather conditions in the list have been
   * modified, this method can be used. Note that this method will also return
   * <code>TRUE</code> if a condition was modified and then reset to its
   * initial value.
   *
   * @return Returns <code>TRUE</code> if the list of conditions was
   * potentially modified (conditions added, removed or updated) and
   * <code>FALSE</code> otherwise.
   */
  public boolean isModified() {
    if (fModified)
      return true;

    if (fStateConditionControl.isModified())
      return true;

    for (SearchConditionItem item : fItems) {
      if (item.isModified())
        return true;
    }

    return false;
  }

  private void initResources() {
    fAddIcon = RSSOwlUI.getImage(fResources, "icons/etool16/add.gif");
    fDeleteIcon = RSSOwlUI.getImage(fResources, "icons/etool16/cancel.gif");
  }

  private void initComponents(List<ISearchCondition> conditions) {

    /* Split Conditions (state / non state) */
    Pair<EnumSet<State>, List<ISearchCondition>> pair = splitConditions(conditions);

    /* Adjust Scrolled Composite */
    setLayout(new GridLayout(1, false));
    setExpandHorizontal(true);
    setExpandVertical(true);
    getVerticalBar().setIncrement(10);

    /* Create the Container */
    fContainer = new Composite(this, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    setContent(fContainer);

    /* Add the State Condition Control */
    addStateConditionControl(pair.getFirst());

    /* Add other Conditions */
    if (pair.getSecond() != null) {
      for (ISearchCondition condition : pair.getSecond())
        addItem(condition);
    }

    /* Update Size */
    updateSize();
  }

  private void addStateConditionControl(EnumSet<State> selectedStates) {

    /* Add the State Condition Control */
    Composite stateControlContainer = new Composite(fContainer, SWT.NONE);
    stateControlContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    stateControlContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fStateConditionControl = new StateConditionControl(stateControlContainer, SWT.NONE, selectedStates);

    ToolBar buttonBar = new ToolBar(stateControlContainer, SWT.FLAT);
    buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

    /* Button to add Condition */
    ToolItem addButton = new ToolItem(buttonBar, SWT.PUSH);
    addButton.setImage(fAddIcon);
    addButton.setToolTipText("Add Condition");
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addItem(getDefaultCondition(), 0).focusInput();
      }
    });

    /* Button to reset State Control */
    ToolItem deleteButton = new ToolItem(buttonBar, SWT.PUSH);
    deleteButton.setImage(fDeleteIcon);
    deleteButton.setToolTipText("Reset Condition");
    deleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fStateConditionControl.select(null);
      }
    });
  }

  private ISearchCondition getDefaultCondition() {
    IModelTypesFactory factory = NewsModel.getDefault().getTypesFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS, "");

    return condition;
  }

  SearchConditionItem addItem(ISearchCondition condition) {
    return addItem(condition, fItems.size());
  }

  SearchConditionItem addItem(ISearchCondition condition, int index) {

    /* Container for Item */
    final Composite itemContainer = new Composite(fContainer, SWT.NONE);
    itemContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    itemContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Create Item */
    final SearchConditionItem item = new SearchConditionItem(itemContainer, SWT.NONE, condition);
    item.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Create Button Box */
    ToolBar buttonBar = new ToolBar(itemContainer, SWT.FLAT);
    buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

    /* Button to add Condition */
    ToolItem addButton = new ToolItem(buttonBar, SWT.PUSH);
    addButton.setImage(fAddIcon);
    addButton.setToolTipText("Add Condition");
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd(item);
      }
    });

    /* Button to delete Condition */
    ToolItem deleteButton = new ToolItem(buttonBar, SWT.PUSH);
    deleteButton.setImage(fDeleteIcon);
    deleteButton.setToolTipText("Delete Condition");
    deleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onDelete(item, itemContainer);
      }
    });

    /* Add to the End */
    if (index == fItems.size())
      fItems.add(item);

    /* Add to specific Index */
    else {
      SearchConditionItem oldItem = fItems.get(index);
      fItems.add(index, item);
      item.getParent().moveAbove(oldItem.getParent());
    }

    /* Force Layout */
    layout(true, true);
    update();

    /* Update Size */
    updateSize();

    return item;
  }

  int indexOf(SearchConditionItem item) {
    return fItems.indexOf(item);
  }

  void onAdd(SearchConditionItem selectedItem) {
    ISearchCondition condition = createCondition(selectedItem.createCondition(null, false));
    SearchConditionItem addedItem = addItem(condition, indexOf(selectedItem) + 1);
    addedItem.focusInput();

    fModified = true;
  }

  void onDelete(final SearchConditionItem item, final Composite itemContainer) {
    getDisplay().asyncExec(new Runnable() {
      public void run() {

        /* Delete */
        itemContainer.dispose();
        removeItem(item);
        fModified = true;
      }
    });
  }

  private ISearchCondition createCondition(ISearchCondition current) {
    IModelTypesFactory factory = NewsModel.getDefault().getTypesFactory();
    ISearchField field = factory.createSearchField(current.getField().getId(), current.getField().getEntityName());
    return factory.createSearchCondition(field, current.getSpecifier(), "");
  }

  void removeItem(SearchConditionItem item) {

    /* Dispose and Remove */
    item.dispose();
    fItems.remove(item);

    /* Force Layout */
    layout(true, true);
    update();

    /* Update Size */
    updateSize();
  }

  private void updateSize() {
    setMinSize(fContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }

  private Pair<EnumSet<INews.State>, List<ISearchCondition>> splitConditions(List<ISearchCondition> conditions) {
    String newsEntity = INews.class.getName();
    List<INews.State> stateConditions = null;
    List<ISearchCondition> otherConditons = null;

    /* For each Condition */
    for (ISearchCondition condition : conditions) {
      ISearchField field = condition.getField();

      /* State Condition */
      if (newsEntity.equals(field.getEntityName()) && field.getId() == INews.STATE) {
        if (stateConditions == null)
          stateConditions = new ArrayList<INews.State>();

        stateConditions.add((State) condition.getValue());
      }

      /* Other Condition */
      else {
        if (otherConditons == null)
          otherConditons = new ArrayList<ISearchCondition>();

        otherConditons.add(condition);
      }
    }

    /* Build States EnumSet */
    EnumSet<INews.State> states = null;
    if (stateConditions != null)
      states = EnumSet.copyOf(stateConditions);

    return Pair.create(states, otherConditons);
  }
}