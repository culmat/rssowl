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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link WizardPage} to select the source of import.
 *
 * @author bpasero
 */
public class ImportSourcePage extends WizardPage {

  /* Max number of Sources to remember */
  private static final int MAX_REMEMBER_SOURCES = 5;

  private Button fImportFromFileRadio;
  private Button fImportFromDefaultRadio;
  private Combo fFileInput;
  private Button fSearchFileButton;
  private IPreferenceScope fPreferences;

  /* Sources for Import */
  enum Source {
    NONE, FILE, DEFAULT
  }

  /**
   * @param pageName
   */
  protected ImportSourcePage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please choose the source of import.");
    fPreferences = Owl.getPreferenceService().getGlobalScope();
  }

  /* Save Import Sources */
  void saveSettings() {

    /* First fill current as new one */
    List<String> newImportSources = new ArrayList<String>();
    if (fImportFromFileRadio.getSelection())
      newImportSources.add(fFileInput.getText());

    /* Then add up to 4 more old ones to remember */
    String[] oldImportSources = fPreferences.getStrings(DefaultPreferences.IMPORT_SOURCES);
    if (oldImportSources != null) {
      for (int i = 0; i < oldImportSources.length && newImportSources.size() < MAX_REMEMBER_SOURCES; i++) {
        if (!newImportSources.contains(oldImportSources[i]))
          newImportSources.add(oldImportSources[i]);
      }
    }

    /* Save List */
    if (!newImportSources.isEmpty())
      fPreferences.putStrings(DefaultPreferences.IMPORT_SOURCES, newImportSources.toArray(new String[newImportSources.size()]));
  }

  /* Returns the type of Source to use for the Import */
  Source getSource() {
    if (fImportFromFileRadio.getSelection())
      return Source.FILE;

    return Source.DEFAULT;
  }

  /* Returns the File to Import from or null if none */
  File getImportFile() {
    return fImportFromFileRadio.getSelection() ? new File(fFileInput.getText()) : null;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Import from File */
    fImportFromFileRadio = new Button(container, SWT.RADIO);
    fImportFromFileRadio.setSelection(true);
    fImportFromFileRadio.setText("Import From a File");
    fImportFromFileRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
        fFileInput.setEnabled(fImportFromFileRadio.getSelection());
        fSearchFileButton.setEnabled(fImportFromFileRadio.getSelection());
        if (fImportFromFileRadio.getSelection())
          fFileInput.setFocus();
      }
    });

    Composite fileInputContainer = new Composite(container, SWT.None);
    fileInputContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) fileInputContainer.getLayout()).marginLeft = 15;
    ((GridLayout) fileInputContainer.getLayout()).marginBottom = 10;
    fileInputContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fFileInput = new Combo(fileInputContainer, SWT.DROP_DOWN | SWT.BORDER);
    fFileInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    String[] previousSources = fPreferences.getStrings(DefaultPreferences.IMPORT_SOURCES);
    if (previousSources != null) {
      for (String source : previousSources) {
        fFileInput.add(source);
      }
    }

    fFileInput.setFocus();
    fFileInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePageComplete();
      }
    });

    fSearchFileButton = new Button(fileInputContainer, SWT.PUSH);
    fSearchFileButton.setText("Browse...");
    setButtonLayoutData(fSearchFileButton);
    fSearchFileButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onBrowse();
      }
    });

    /* Import from Recommended Feeds */
    fImportFromDefaultRadio = new Button(container, SWT.RADIO);
    fImportFromDefaultRadio.setText("Import Recommended Feeds");
    fImportFromDefaultRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
      }
    });

    setControl(container);
    updatePageComplete();
  }

  private void onBrowse() {
    FileDialog dialog = new FileDialog(getShell());
    dialog.setText("Choose Import File");

    /* Set Export Formats also for Import (we assume this is supported) */
    List<String> filterExtensions = new ArrayList<String>();
    filterExtensions.add("*.opml");
    filterExtensions.add("*.xml");

    Collection<String> exportFormats = Owl.getInterpreter().getExportFormats();
    for (String exportFormat : exportFormats) {
      String format = "*." + exportFormat.toLowerCase();
      if (!filterExtensions.contains(format))
        filterExtensions.add(format);
    }

    if (!filterExtensions.contains("*.*"))
      filterExtensions.add("*.*");

    dialog.setFilterExtensions(filterExtensions.toArray(new String[filterExtensions.size()]));
    if (StringUtils.isSet(fFileInput.getText()))
      dialog.setFileName(fFileInput.getText());

    String string = dialog.open();
    if (string != null)
      fFileInput.setText(string);

    updatePageComplete();
  }

  private void updatePageComplete() {
    String errorMessage = null;

    /* Import Default */
    if (fImportFromDefaultRadio.getSelection())
      setPageComplete(true);

    /* Import from File */
    else if (fImportFromFileRadio.getSelection()) {
      String filePath = fFileInput.getText();
      File fileToImport = new File(filePath);
      boolean fileExists = fileToImport.exists() && fileToImport.isFile();
      setPageComplete(StringUtils.isSet(filePath) && fileExists);
      if (StringUtils.isSet(filePath) && !fileExists)
        errorMessage = "Please select an existing file.";
    }

    /* Set Error Message */
    if (errorMessage != null)
      setErrorMessage(errorMessage);

    /* Restore Normal Message */
    else {
      setErrorMessage(null);
      setMessage("Please choose the source of import.");
    }
  }
}