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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderChooser;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class SearchMarkPropertyPage implements IEntityPropertyPage {
  private IPropertyDialogSite fSite;
  private Text fNameInput;
  private ISearchMark fSearchMark;
  private Button fMatchAllRadio;
  private SearchConditionList fSearchConditionList;
  private Button fMatchAnyRadio;
  private FolderChooser fFolderChooser;
  private boolean fSearchChanged;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite,
   * java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(entities.size() == 1 && entities.get(0) instanceof ISearchMark);
    fSite = site;
    fSearchMark = (ISearchMark) entities.get(0);
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Name */
    Label nameLabel = new Label(container, SWT.None);
    nameLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    nameLabel.setText("Name: ");

    Composite nameContainer = new Composite(container, SWT.BORDER);
    nameContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    nameContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    nameContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    fNameInput = new Text(nameContainer, SWT.NONE);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fNameInput.setText(fSearchMark.getName());

    ToolBar generateTitleBar = new ToolBar(nameContainer, SWT.FLAT);
    generateTitleBar.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    ToolItem generateTitleItem = new ToolItem(generateTitleBar, SWT.PUSH);
    generateTitleItem.setImage(OwlUI.getImage(fSite.getResourceManager(), "icons/etool16/info.gif"));
    generateTitleItem.setToolTipText("Create name from conditions");
    generateTitleItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onGenerateName();
      }
    });

    /* Location */
    Label locationLabel = new Label(container, SWT.None);
    locationLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    locationLabel.setText("Location: ");

    fFolderChooser = new FolderChooser(container, fSearchMark.getParent(), SWT.BORDER);
    fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
    fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    Composite radioContainer = new Composite(container, SWT.None);
    radioContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    radioContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 0));
    ((GridLayout) radioContainer.getLayout()).marginTop = 10;

    fMatchAllRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAllRadio.setText("Match all conditions");
    fMatchAllRadio.setSelection(fSearchMark.matchAllConditions());

    fMatchAnyRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any condition");
    fMatchAnyRadio.setSelection(!fSearchMark.matchAllConditions());

    Composite conditionsContainer = new Composite(container, SWT.BORDER);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(1));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, fSearchMark.getSearchConditions());
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fSearchConditionList.setVisibleItemCount(3);

    return container;
  }

  void onGenerateName() {
    List<ISearchCondition> conditions = fSearchConditionList.createConditions();
    String name = ModelUtils.getName(conditions, fMatchAllRadio.getSelection());

    if (name.length() > 0) {
      fNameInput.setText(name);
      fNameInput.selectAll();
    }
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {
    if (fNameInput != null) {
      fNameInput.setFocus();
      fNameInput.selectAll();
    }
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {

    /* Require a Name */
    if (fNameInput.getText().length() == 0) {
      fSite.select(this);
      fNameInput.setFocus();
      fSite.setMessage("Please enter a name for the saved search.", IPropertyDialogSite.MessageType.ERROR);

      return false;
    }

    /* Require a Condition */
    if (fSearchConditionList.isEmpty()) {
      fSite.select(this);
      fNameInput.setFocus();
      fSite.setMessage("Please specify your search by defining some conditions.", IPropertyDialogSite.MessageType.ERROR);

      return false;
    }

    /* Check for changed Name */
    if (!fSearchMark.getName().equals(fNameInput.getText())) {
      fSearchMark.setName(fNameInput.getText());
      entitiesToSave.add(fSearchMark);
    }

    /* Update match-all-condition */
    if (fSearchMark.matchAllConditions() != fMatchAllRadio.getSelection()) {
      fSearchMark.setMatchAllConditions(fMatchAllRadio.getSelection());
      entitiesToSave.add(fSearchMark);
      fSearchChanged = true;
    }

    /* Update Conditions (TODO Could be optimized to not replace all conditions) */
    if (fSearchConditionList.isModified()) {
      entitiesToSave.add(fSearchMark);
      fSearchChanged = true;

      /* Remove Old Conditions */
      List<ISearchCondition> oldConditions = new ArrayList<ISearchCondition>(fSearchMark.getSearchConditions());
      for (ISearchCondition oldCondition : oldConditions) {
        fSearchMark.removeSearchCondition(oldCondition);
      }

      /* Add New Conditions */
      fSearchConditionList.createConditions(fSearchMark);
    }

    /* Re-Run search if conditions changed */
    if (fSearchChanged)
      Controller.getDefault().getSavedSearchService().updateSavedSearches(Collections.singleton(fSearchMark), true);

    return true;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {

    /* Reparent if necessary */
    if (fSearchMark.getParent() != fFolderChooser.getFolder()) {
      ReparentInfo<IFolderChild, IFolder> reparent = new ReparentInfo<IFolderChild, IFolder>(fSearchMark, fFolderChooser.getFolder(), null, null);
      Owl.getPersistenceService().getDAOService().getFolderDAO().reparent(Collections.singletonList(reparent));
    }
  }
}