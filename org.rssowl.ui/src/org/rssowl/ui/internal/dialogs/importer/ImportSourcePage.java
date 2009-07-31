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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;

/**
 * A {@link WizardPage} to select the source of import.
 *
 * @author bpasero
 */
public class ImportSourcePage extends WizardPage {
  private Button fImportFromFileRadio;
  private Button fImportFromDefaultRadio;
  private Text fFileInput;
  private Button fSearchFileButton;

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

    fFileInput = new Text(fileInputContainer, SWT.SINGLE | SWT.BORDER);
    fFileInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
    dialog.setText("Import From File");
    dialog.setFilterExtensions(new String[] { "*.opml", "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    String string = dialog.open();
    if (string != null)
      fFileInput.setText(string);

    updatePageComplete();
  }

  private void updatePageComplete() {
    String errorMessage = null;

    if (fImportFromDefaultRadio.getSelection())
      setPageComplete(true);
    else if (fImportFromFileRadio.getSelection()) {
      String filePath = fFileInput.getText();
      File fileToImport = new File(filePath);
      boolean fileExists = fileToImport.exists() && fileToImport.isFile();
      setPageComplete(StringUtils.isSet(filePath) && fileExists);
      if (StringUtils.isSet(filePath) && !fileExists)
        errorMessage = "Please select an existing file.";
    }

    if (errorMessage != null)
      setErrorMessage(errorMessage);
    else {
      setErrorMessage(null);
      setMessage("Please choose the source of import.");
    }
  }
}