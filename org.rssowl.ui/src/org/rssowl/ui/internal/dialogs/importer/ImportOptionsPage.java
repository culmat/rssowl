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
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.OwlUI;

import java.util.Collection;

/**
 * A {@link WizardPage} to select additionsl options for the import.
 *
 * @author bpasero
 */
public class ImportOptionsPage extends WizardPage {
  private Button fImportLabelsCheck;
  private Button fImportFiltersCheck;
  private Button fImportSettingsCheck;

  /**
   * @param pageName
   */
  protected ImportOptionsPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please select additional options for the import.");
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
    infoText.setText("The following options allow to import more than just feeds. You can choose to import Labels, News Filters and Preferences.");

    /* Labels */
    Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
    fImportLabelsCheck = new Button(container, SWT.CHECK);
    fImportLabelsCheck.setImage(OwlUI.getImage(fImportLabelsCheck, "icons/elcl16/labels.gif"));
    if (!labels.isEmpty())
      fImportLabelsCheck.setText("Import Labels (" + labels.size() + " in total)");
    else
      fImportLabelsCheck.setText("Import Labels (No Labels Found)");
    fImportLabelsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    ((GridData) fImportLabelsCheck.getLayoutData()).verticalIndent = 10;
    fImportLabelsCheck.setEnabled(!labels.isEmpty());

    /* Filters */
    Collection<ISearchFilter> filters = DynamicDAO.loadAll(ISearchFilter.class);
    final boolean filtersUseLabels = false; //filtersUseLabels(filters); TODO
    fImportFiltersCheck = new Button(container, SWT.CHECK);
    fImportFiltersCheck.setImage(OwlUI.getImage(fImportFiltersCheck, "icons/etool16/filter.gif"));
    if (!filters.isEmpty())
      fImportFiltersCheck.setText("Import News Filters (" + filters.size() + " in total)");
    else
      fImportFiltersCheck.setText("Import News Filters (No Filters Found)");
    fImportFiltersCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fImportFiltersCheck.setEnabled(!filters.isEmpty());
    fImportFiltersCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fImportFiltersCheck.getSelection() && !fImportLabelsCheck.getSelection() && filtersUseLabels) {
          fImportLabelsCheck.setSelection(true);
          setMessage("Labels will also be imported because some Filters make use of them as part of their actions.", IMessageProvider.INFORMATION);
        } else if (!fImportFiltersCheck.getSelection()) {
          setMessage("Please select additional options for the import.");
        }
      }
    });

    /* Properties */
    fImportSettingsCheck = new Button(container, SWT.CHECK);
    fImportSettingsCheck.setImage(OwlUI.getImage(fImportSettingsCheck, "icons/elcl16/preferences.gif"));
    fImportSettingsCheck.setText("Import Preferences");
    fImportSettingsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    setControl(container);
  }

  /*
  * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
  */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
  }
}