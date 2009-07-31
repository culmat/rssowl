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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.FolderChooser;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.Set;

/**
 * A {@link WizardPage} to select the Target location for the import.
 *
 * @author bpasero
 */
public class ImportTargetPage extends WizardPage {
  private FolderChooser fFolderChooser;
  private Button fChooseExistingRadio;
  private Button fCreateNewSetRadio;
  private Text fSetNameInput;

  /**
   * @param pageName
   */
  protected ImportTargetPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please choose the target location to import into.");
  }

  /**
   * @return the target {@link IFolder} for the import.
   */
  IFolder getTargetLocation() {
    if (fChooseExistingRadio.getSelection())
      return fFolderChooser.getFolder();

    return Owl.getModelFactory().createFolder(null, null, fSetNameInput.getText());
  }

  private void updatePageComplete() {
    if (fChooseExistingRadio.getSelection())
      setPageComplete(true);
    else
      setPageComplete(StringUtils.isSet(fSetNameInput.getText()));
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Choose Existing Folder */
    fChooseExistingRadio = new Button(container, SWT.RADIO);
    fChooseExistingRadio.setText("Import into an existing Folder");
    fChooseExistingRadio.setSelection(true);
    fChooseExistingRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fSetNameInput.setEnabled(!fChooseExistingRadio.getSelection());
        if (!fChooseExistingRadio.getSelection())
          fSetNameInput.setFocus();
        updatePageComplete();
      }
    });

    Composite folderContainer = new Composite(container, SWT.None);
    folderContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) folderContainer.getLayout()).marginLeft = 15;
    ((GridLayout) folderContainer.getLayout()).marginBottom = 5;
    folderContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Set<IFolder> rootFolders = CoreUtils.loadRootFolders();
    fFolderChooser = new FolderChooser(folderContainer, rootFolders.iterator().next(), null, SWT.BORDER, false, 7);
    fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
    fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    fFolderChooser.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        if (fCreateNewSetRadio.getSelection()) {
          fChooseExistingRadio.setSelection(true);
          fCreateNewSetRadio.setSelection(false);
          fSetNameInput.setEnabled(false);
          updatePageComplete();
        }
      }
    });

    /* Create new Bookmark Set */
    fCreateNewSetRadio = new Button(container, SWT.RADIO);
    fCreateNewSetRadio.setText("Import into a new Bookmark Set");

    Composite newBookmarkSetContainer = new Composite(container, SWT.None);
    newBookmarkSetContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) newBookmarkSetContainer.getLayout()).marginLeft = 15;
    newBookmarkSetContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Label nameLabel = new Label(newBookmarkSetContainer, SWT.None);
    nameLabel.setText("Name: ");

    fSetNameInput = new Text(newBookmarkSetContainer, SWT.SINGLE | SWT.BORDER);
    fSetNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fSetNameInput.setEnabled(false);
    fSetNameInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePageComplete();
      }
    });

    setControl(container);
  }
}