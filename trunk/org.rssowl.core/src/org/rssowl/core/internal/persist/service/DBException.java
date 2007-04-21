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

package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * A checked exception representing a problem that takes place while updating or
 * querying the database. Contains a status object describing the cause of the
 * exception.
 * 
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class DBException extends CoreException {

  /**
   * Creates a new exception with the given status object. The message of the
   * given status is used as the exception message.
   * 
   * @param status the status object to be associated with this exception
   */
  public DBException(IStatus status) {
    super(status);
  }
}