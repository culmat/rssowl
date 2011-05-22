/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.wizard.Wizard;
import org.rssowl.core.Owl;
import org.rssowl.ui.internal.Application;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * The {@link FatalErrorWizard} shows up when RSSOwl crashed during startup in a
 * fatal, unrecoverable way.
 *
 * @author bpasero
 */
public class FatalErrorWizard extends Wizard {
  private ErrorInfoPage fErrorInfoPage;
  private RestoreBackupPage fRestoreBackupPage;
  private final IStatus fErrorStatus;
  private int fReturnCode = IApplication.EXIT_OK;
  private final List<File> fBackups;

  public FatalErrorWizard(IStatus errorStatus) {
    fErrorStatus = errorStatus;

    boolean allowToRestoreBackups = !(errorStatus.getException() instanceof OutOfMemoryError);
    fBackups = allowToRestoreBackups ? Owl.getBackups() : Collections.<File> emptyList();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle(Messages.FatalErrorWizard_CRASH_REPORTER);
    setHelpAvailable(false);

    /* Error Info */
    fErrorInfoPage = new ErrorInfoPage(Messages.FatalErrorWizard_WE_ARE_SORRY, fErrorStatus, !fBackups.isEmpty());
    addPage(fErrorInfoPage);

    /* Restore Backup (if backups are present) */
    if (!fBackups.isEmpty()) {
      fRestoreBackupPage = new RestoreBackupPage(Messages.FatalErrorWizard_RESTORE_BACKUP, fBackups);
      addPage(fRestoreBackupPage);
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {

    /* Handle selected backup if present */
    if (fRestoreBackupPage != null) {
      File backup = fRestoreBackupPage.getSelectedBackup();
      if (backup != null) {

        /* Trigger Backup Restore */
        Owl.restore(backup);
      }
    }

    /* Windows: Support to restart from dialog */
    if (Application.IS_WINDOWS)
      fReturnCode = IApplication.EXIT_RESTART;

    return true;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {

    /* Make sure user is on last page to Finish */
    if (fRestoreBackupPage != null && getContainer().getCurrentPage() != fRestoreBackupPage)
      return false;

    /* Other Pages decide on their own */
    return super.canFinish();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return false;
  }

  /**
   * @return one of the {@link IApplication} return codes.
   */
  public int getReturnCode() {
    return fReturnCode;
  }
}