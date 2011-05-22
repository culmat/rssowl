/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.dialogs.fatal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.List;

/**
 * Part of the {@link FatalErrorWizard} to restore from a backup.
 *
 * @author bpasero
 */
public class RestoreBackupPage extends WizardPage {
  private List<File> fBackups;
  private ComboViewer fBackupsViewer;
  private final DateFormat fDateFormat = OwlUI.getShortDateFormat();

  RestoreBackupPage(String pageName, List<File> backups) {
    super(pageName, pageName, null);
    fBackups = backups;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {

    /* Title Image and Message */
    setImageDescriptor(OwlUI.getImageDescriptor("icons/wizban/welcome_wiz.gif")); //$NON-NLS-1$
    setMessage(Messages.RestoreBackupPage_RSSOWL_CRASH, IMessageProvider.WARNING);

    /* Container */
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 5, 0));
    ((GridLayout) container.getLayout()).marginTop = 5;

    /* Controls to Restore from a Backup */
    Label backupInfo = new Label(container, SWT.WRAP);
    if (Application.IS_WINDOWS)
      backupInfo.setText(Messages.RestoreBackupPage_BACKUP_INFO_RESTART);
    else
      backupInfo.setText(Messages.RestoreBackupPage_BACKUP_INFO_QUIT);
    backupInfo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    ((GridData) backupInfo.getLayoutData()).widthHint = 200;

    Label pickBackupLabel = new Label(container, SWT.NONE);
    pickBackupLabel.setText(Messages.RestoreBackupPage_CHOOSE_BACKUP);
    pickBackupLabel.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
    pickBackupLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    ((GridData) pickBackupLabel.getLayoutData()).verticalIndent = 10;

    fBackupsViewer = new ComboViewer(container, SWT.BORDER | SWT.READ_ONLY);
    fBackupsViewer.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
    ((GridData) fBackupsViewer.getControl().getLayoutData()).verticalIndent = 10;
    fBackupsViewer.getCombo().setVisibleItemCount(fBackups.size());
    fBackupsViewer.setContentProvider(new ArrayContentProvider());
    fBackupsViewer.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        File file = (File) element;
        return NLS.bind(Messages.RestoreBackupPage_BACKUP_LABEL, fDateFormat.format(file.lastModified()), OwlUI.getSize((int) file.length()));
      }
    });

    fBackupsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        getContainer().updateButtons();
      }
    });

    fBackupsViewer.setInput(fBackups);

    Composite adviseContainer = new Composite(container, SWT.None);
    adviseContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    adviseContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    ((GridData) adviseContainer.getLayoutData()).verticalIndent = 10;

    Label adviseLabel = new Label(adviseContainer, SWT.NONE);
    adviseLabel.setText(Messages.RestoreBackupPage_WARNING);
    adviseLabel.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
    adviseLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    Label adviseTextLabel = new Label(adviseContainer, SWT.WRAP);
    adviseTextLabel.setText(Messages.RestoreBackupPage_RESTORE_WARNING);
    adviseTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    ((GridData) adviseTextLabel.getLayoutData()).widthHint = 200;

    Dialog.applyDialogFont(container);

    setControl(container);
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return !fBackupsViewer.getSelection().isEmpty();
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if (visible)
      fBackupsViewer.getCombo().setFocus();
  };

  File getSelectedBackup() {
    IStructuredSelection selection = (IStructuredSelection) fBackupsViewer.getSelection();
    if (!selection.isEmpty())
      return (File) selection.getFirstElement();

    return null;
  }
}