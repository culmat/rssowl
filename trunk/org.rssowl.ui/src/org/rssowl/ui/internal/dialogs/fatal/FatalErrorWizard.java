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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.util.ImportUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
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
  private CleanProfilePage fCleanProfilePage;
  private final IStatus fErrorStatus;
  private int fReturnCode = IApplication.EXIT_OK;
  private final List<File> fProfileBackups = new ArrayList<File>();
  private final List<File> fOPMLBackups = new ArrayList<File>();
  private final boolean fIsOOMError;

  public FatalErrorWizard(IStatus errorStatus) {
    fErrorStatus = errorStatus;
    fIsOOMError = (fErrorStatus.getException() instanceof OutOfMemoryError);
    if (!fIsOOMError)
      findBackups();
  }

  private void findBackups() {

    /* Collect Profile Backups */
    fProfileBackups.addAll(Owl.getBackups());

    /* Collect OPML Backups if no profile backups can be found */
    if (fProfileBackups.isEmpty()) {
      IPath backPath = Platform.getLocation();
      File backupDir = backPath.toFile();
      if (backupDir.exists()) {

        /* Daily OPML Backup */
        File dailyBackupFile = backPath.append(Controller.DAILY_BACKUP).toFile();
        if (dailyBackupFile.exists())
          fOPMLBackups.add(dailyBackupFile);

        /* Weekly OPML Backup */
        File weeklyBackupFile = backPath.append(Controller.WEEKLY_BACKUP).toFile();
        if (weeklyBackupFile.exists())
          fOPMLBackups.add(weeklyBackupFile);
      }
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle(Messages.FatalErrorWizard_CRASH_REPORTER);
    setHelpAvailable(false);

    /* Error Info */
    fErrorInfoPage = new ErrorInfoPage(Messages.FatalErrorWizard_WE_ARE_SORRY, fErrorStatus, !fIsOOMError);
    addPage(fErrorInfoPage);

    /* Add Restore Pages if this is not an OOM Error */
    if (!fIsOOMError) {

      /* Restore Profile Backup (if profile backups are present) */
      if (!fProfileBackups.isEmpty()) {
        fRestoreBackupPage = new RestoreBackupPage(Messages.FatalErrorWizard_RESTORE_BACKUP, fProfileBackups);
        addPage(fRestoreBackupPage);
      }

      /* Otherwise allow to restore from OPML Backup or clean start */
      else {
        fCleanProfilePage = new CleanProfilePage(fOPMLBackups.isEmpty() ? Messages.FatalErrorWizard_START_OVER : Messages.FatalErrorWizard_RESTORE_SUBSCRIPTIONS_SETTINGS, !fOPMLBackups.isEmpty());
        addPage(fCleanProfilePage);
      }
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {

    /* Finish */
    try {
      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
        public void run() {
          internalPerformFinish();
        }
      });
    } catch (PersistenceException e) {
      Activator.getDefault().logError(e.getMessage(), e);

      /* Show Error to the User */
      String msg;
      if (StringUtils.isSet(e.getMessage()))
        msg = NLS.bind(Messages.FatalErrorWizard_RESTORE_ERROR_N, e.getMessage());
      else
        msg = Messages.FatalErrorWizard_RESTORE_ERROR;

      ((WizardPage) getContainer().getCurrentPage()).setMessage(msg, IMessageProvider.ERROR);

      return false;
    }

    /* Windows: Support to restart from dialog */
    if (Application.IS_WINDOWS)
      fReturnCode = IApplication.EXIT_RESTART;

    return true;
  }

  private void internalPerformFinish() throws PersistenceException {

    /* Handle selected backup if present */
    if (fRestoreBackupPage != null) {
      File backup = fRestoreBackupPage.getSelectedBackup();
      if (backup != null)
        Owl.restore(backup);
    }

    /* Handle Clean Profile if selected */
    else if (fCleanProfilePage != null && fCleanProfilePage.doCleanProfile()) {

      /* Recreate the Profile */
      Owl.recreateProfile();

      /* Try to Import from OPML backups if present */
      if (!fOPMLBackups.isEmpty()) {
        List<? extends IEntity> types = null;

        /* First Try Daily Backup */
        File recentBackup = fOPMLBackups.get(0);
        try {
          types = InternalOwl.getDefault().getInterpreter().importFrom(new FileInputStream(recentBackup));
        } catch (Exception e) {
          if (fOPMLBackups.size() == 1)
            throw new PersistenceException(e.getMessage(), e);
        }

        /* Second Try Weekly Backup */
        if (types == null && fOPMLBackups.size() == 2) {
          File weeklyBackup = fOPMLBackups.get(1);
          try {
            types = Owl.getInterpreter().importFrom(new FileInputStream(weeklyBackup));
          } catch (Exception e) {
            throw new PersistenceException(e.getMessage(), e);
          }
        }

        /* Do Import */
        if (types != null)
          ImportUtils.doImport(null, types, false);
      }
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {

    /* Make sure user is on last page to Finish */
    if (fRestoreBackupPage != null && getContainer().getCurrentPage() != fRestoreBackupPage)
      return false;
    else if (fCleanProfilePage != null && getContainer().getCurrentPage() != fCleanProfilePage)
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