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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.rssowl.ui.internal.dialogs.fatal.messages"; //$NON-NLS-1$
  public static String ErrorInfoPage_COPY;
  public static String ErrorInfoPage_ERROR_DETAILS;
  public static String ErrorInfoPage_GENERAL_ERROR_ADVISE;
  public static String ErrorInfoPage_NEXT_PAGE_ADVISE;
  public static String ErrorInfoPage_RSSOWL_CRASH;
  public static String ErrorInfoPage_SEND_LOGS_ADVISE;
  public static String FatalErrorWizard_CRASH_REPORTER;
  public static String FatalErrorWizard_RESTORE_BACKUP;
  public static String FatalErrorWizard_WE_ARE_SORRY;

  public static String RestoreBackupPage_BACKUP_INFO_QUIT;
  public static String RestoreBackupPage_BACKUP_INFO_RESTART;
  public static String RestoreBackupPage_BACKUP_LABEL;
  public static String RestoreBackupPage_CHOOSE_BACKUP;
  public static String RestoreBackupPage_RESTORE_WARNING;
  public static String RestoreBackupPage_RSSOWL_CRASH;
  public static String RestoreBackupPage_WARNING;

  private Messages() {}

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}