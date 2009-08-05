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

package org.rssowl.ui.internal.dialogs.importer;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.Collection;
import java.util.List;

/**
 * A {@link WizardPage} to select additionsl options for the import.
 *
 * @author bpasero
 */
//TODO Clarify what happens with existing Labels, Filters, Preferences
public class ImportOptionsPage extends WizardPage {
  private Button fImportLabelsCheck;
  private Button fImportFiltersCheck;
  private Button fImportPreferencesCheck;
  private boolean fFiltersUseLabels;

  /**
   * @param pageName
   */
  protected ImportOptionsPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please select additional options for the import.");
  }

  boolean importLabels() {
    return fImportLabelsCheck.getSelection();
  }

  boolean importFilters() {
    return fImportFiltersCheck.getSelection();
  }

  boolean importPreferences() {
    return fImportPreferencesCheck.getSelection();
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    StyledText infoText = new StyledText(container, SWT.WRAP | SWT.READ_ONLY);
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    ((GridData) infoText.getLayoutData()).widthHint = 200;
    infoText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    infoText.setText("The following options allow to import more than just feeds. You can choose to import Labels, News Filters and Preferences if available from the source.");

    /* Labels */
    fImportLabelsCheck = new Button(container, SWT.CHECK);
    fImportLabelsCheck.setImage(OwlUI.getImage(fImportLabelsCheck, "icons/elcl16/labels.gif"));
    fImportLabelsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    ((GridData) fImportLabelsCheck.getLayoutData()).verticalIndent = 10;

    /* Filters */
    fImportFiltersCheck = new Button(container, SWT.CHECK);
    fImportFiltersCheck.setImage(OwlUI.getImage(fImportFiltersCheck, "icons/etool16/filter.gif"));
    fImportFiltersCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fImportFiltersCheck.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fImportFiltersCheck.getSelection() && !fImportLabelsCheck.getSelection() && fFiltersUseLabels) {
          fImportLabelsCheck.setSelection(true);
          setMessage("Labels will also be imported because some Filters make use of them as part of their actions.", IMessageProvider.INFORMATION);
        } else if (!fImportFiltersCheck.getSelection()) {
          setMessage("Please select additional options for the import.");
        }
      }
    });

    /* Preferences */
    fImportPreferencesCheck = new Button(container, SWT.CHECK);
    fImportPreferencesCheck.setImage(OwlUI.getImage(fImportPreferencesCheck, "icons/elcl16/preferences.gif"));
    fImportPreferencesCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Info Container */
    Composite infoContainer = new Composite(container, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.END, false, true));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(infoImg, "icons/obj16/info.gif"));
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    infoText = new StyledText(infoContainer, SWT.WRAP | SWT.READ_ONLY);
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    ((GridData) infoText.getLayoutData()).widthHint = 200;
    infoText.setText("Note: Use 'Import Preferences' with care. All of your existing preferences will be replaced by the imported ones if selected.");
    infoText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

    setControl(container);
  }

  /*
  * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
  */
  @Override
  public void setVisible(boolean visible) {
    ImportElementsPage elementsPage = (ImportElementsPage) getPreviousPage().getPreviousPage();
    update(elementsPage.getLabelsToImport().size(), elementsPage.getFiltersToImport().size(), !elementsPage.getPreferencesToImport().isEmpty());
    fFiltersUseLabels = filtersUseLabels(elementsPage.getFiltersToImport());

    super.setVisible(visible);
  }

  private void update(int labelCount, int filterCount, boolean hasPreferences) {

    /* Labels */
    if (labelCount != 0)
      fImportLabelsCheck.setText("Import Labels (" + labelCount + " in total)");
    else
      fImportLabelsCheck.setText("Import Labels (No Labels Available)");
    fImportLabelsCheck.setEnabled(labelCount != 0);

    /* Filters */
    if (filterCount != 0)
      fImportFiltersCheck.setText("Import News Filters (" + filterCount + " in total)");
    else
      fImportFiltersCheck.setText("Import News Filters (No Filters Available)");
    fImportFiltersCheck.setEnabled(filterCount != 0);

    /* Preferences */
    if (hasPreferences)
      fImportPreferencesCheck.setText("Import Preferences");
    else
      fImportPreferencesCheck.setText("Import Preferences (No Preferences Available)");
    fImportPreferencesCheck.setEnabled(hasPreferences);
  }

  private boolean filtersUseLabels(Collection<ISearchFilter> filters) {
    for (ISearchFilter filter : filters) {
      List<IFilterAction> actions = filter.getActions();
      for (IFilterAction action : actions) {
        if (LabelNewsAction.ID.equals(action.getActionId()))
          return true;
      }
    }

    return false;
  }
}