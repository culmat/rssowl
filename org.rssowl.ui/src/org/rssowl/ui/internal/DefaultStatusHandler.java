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

package org.rssowl.ui.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.WorkbenchErrorHandler;
import org.rssowl.core.persist.service.PersistenceException;

import java.io.IOException;

/**
 * Default status handler in RSSOwl that performs certain special actions when
 * certain Exceptions are thrown.
 *
 * @author bpasero@rssowl.org
 */
public class DefaultStatusHandler extends WorkbenchErrorHandler {

  /*
   * @see org.eclipse.ui.statushandlers.AbstractStatusHandler#handle(org.eclipse.ui.statushandlers.StatusAdapter, int)
   */
  @Override
  public void handle(StatusAdapter statusAdapter, int style) {
    IStatus status = statusAdapter.getStatus();

    /* Specially treat IOExceptions from PersistenceException */
    if (status != null) {
      Throwable ex = status.getException();
      if (ex instanceof PersistenceException)
        handlePersistenceException((PersistenceException) ex);
      else if (ex != null) {
        Throwable cause = ex.getCause();
        if (cause instanceof PersistenceException)
          handlePersistenceException((PersistenceException) cause);
      }
    }

    /* Handle in WorkbenchErrorHandle in any case */
    super.handle(statusAdapter, style);
  }

  private void handlePersistenceException(PersistenceException ex) {
    Throwable cause = ex.getCause();
    if (cause != null && cause instanceof IOException) {
      ErrorDialog.openError(OwlUI.getPrimaryShell(), "Error", "RSSOwl was unable to write to your disk. This is typically caused by a disk that is full or if the permissions are not set correctly. We recommend that you solve the problem and restart the application.", Activator.getDefault().createErrorStatus(cause.getMessage(), (Exception) cause));
    }
  }
}