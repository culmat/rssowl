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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.search.SearchSpecifier;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * The <code>StateConditionControl</code> is a <code>Composite</code>
 * providing the UI to define State-Conditions for a Search.
 * <p>
 * TODO This class is currently only working on INews.
 * </p>
 * <p>
 * TODO Enable support for *deleted* state again.
 * </p>
 *
 * @author bpasero
 */
public class StateConditionControl extends Composite {
  private Button fNewState;
  private Button fUnreadState;
  private Button fUpdatedState;
  private Button fReadState;
  private Button fDeletedState;
  private IModelFactory fFactory;
  private EnumSet<State> fSelectedStates;
  private boolean fModified;
  private final String fNewsEntity = INews.class.getName();

  /**
   * @param parent The parent Composite.
   * @param style The Style as defined by SWT constants.
   * @param selectedStates The states that are initially selected or
   * <code>NULL</code> if none.
   */
  StateConditionControl(Composite parent, int style, EnumSet<INews.State> selectedStates) {
    super(parent, style);
    fSelectedStates = selectedStates;
    fFactory = Owl.getModelFactory();

    initComponents();
  }

  /**
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see StateConditionControl#createConditions(ISearchMark)
   */
  List<ISearchCondition> createConditions() {
    return createConditions(null);
  }

  /**
   * @param searchmark The parent of the <code>ISearchCondition</code>s that
   * are being created.
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see StateConditionControl#createConditions()
   */
  List<ISearchCondition> createConditions(ISearchMark searchmark) {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(5);

    if (fNewState.getSelection())
      conditions.add(createStateCondition(searchmark, INews.State.NEW));

    if (fUnreadState.getSelection())
      conditions.add(createStateCondition(searchmark, INews.State.UNREAD));

    if (fUpdatedState.getSelection())
      conditions.add(createStateCondition(searchmark, INews.State.UPDATED));

    if (fReadState.getSelection())
      conditions.add(createStateCondition(searchmark, INews.State.READ));

    if (fDeletedState.getSelection())
      conditions.add(createStateCondition(searchmark, INews.State.HIDDEN));

    return conditions;
  }

  private ISearchCondition createStateCondition(ISearchMark searchmark, INews.State state) {
    ISearchField field = fFactory.createSearchField(INews.STATE, fNewsEntity);

    if (searchmark != null)
      return fFactory.createSearchCondition(null, searchmark, field, SearchSpecifier.IS, state);

    return fFactory.createSearchCondition(field, SearchSpecifier.IS, state);
  }

  /**
   * Selects the given States in the Control. Will deselect all states if the
   * field is <code>NULL</code>.
   *
   * @param selectedStates the news states to select in the Control or
   * <code>NULL</code> if none.
   */
  void select(EnumSet<INews.State> selectedStates) {
    fModified = true;
    fNewState.setSelection(selectedStates != null && selectedStates.contains(INews.State.NEW));
    fUnreadState.setSelection(selectedStates != null && selectedStates.contains(INews.State.UNREAD));
    fUpdatedState.setSelection(selectedStates != null && selectedStates.contains(INews.State.UPDATED));
    fReadState.setSelection(selectedStates != null && selectedStates.contains(INews.State.READ));
    fDeletedState.setSelection(selectedStates != null && selectedStates.contains(INews.State.HIDDEN));
  }

  /**
   * Optimization: In order to check weather conditions in the list have been
   * modified, this method can be used
   *
   * @return Returns <code>TRUE</code> if the list of conditions was modified
   * and <code>FALSE</code> otherwise.
   */
  boolean isModified() {
    if (fModified)
      return true;

    if (fSelectedStates == null)
      return hasSelected();

    if (fSelectedStates.contains(INews.State.NEW) != fNewState.getSelection())
      return true;

    if (fSelectedStates.contains(INews.State.UNREAD) != fUnreadState.getSelection())
      return true;

    if (fSelectedStates.contains(INews.State.UPDATED) != fUpdatedState.getSelection())
      return true;

    if (fSelectedStates.contains(INews.State.READ) != fReadState.getSelection())
      return true;

    if (fSelectedStates.contains(INews.State.HIDDEN) != fDeletedState.getSelection())
      return true;

    return false;
  }

  boolean hasSelected() {
    return fNewState.getSelection() || fUnreadState.getSelection() || fUpdatedState.getSelection() || fReadState.getSelection() || fDeletedState.getSelection();
  }

  private void initComponents() {

    /* Apply Gridlayout */
    setLayout(LayoutUtils.createGridLayout(6));

    /* Label */
    Label controlLabel = new Label(this, SWT.NONE);
    controlLabel.setText("Status of News: ");

    /* State: New */
    fNewState = new Button(this, SWT.CHECK);
    fNewState.setText("New");
    fNewState.setSelection(fSelectedStates != null && fSelectedStates.contains(INews.State.NEW));

    /* State: Unread */
    fUnreadState = new Button(this, SWT.CHECK);
    fUnreadState.setText("Unread");
    fUnreadState.setSelection(fSelectedStates != null && fSelectedStates.contains(INews.State.UNREAD));

    /* State: Updated */
    fUpdatedState = new Button(this, SWT.CHECK);
    fUpdatedState.setText("Updated");
    fUpdatedState.setSelection(fSelectedStates != null && fSelectedStates.contains(INews.State.UPDATED));

    /* State: Read */
    fReadState = new Button(this, SWT.CHECK);
    fReadState.setText("Read");
    fReadState.setSelection(fSelectedStates != null && fSelectedStates.contains(INews.State.READ));

    /* State: Deleted */
    fDeletedState = new Button(this, SWT.CHECK);
    fDeletedState.setText("Deleted");
    fDeletedState.setSelection(fSelectedStates != null && fSelectedStates.contains(INews.State.HIDDEN));
    fDeletedState.setVisible(false);
  }
}