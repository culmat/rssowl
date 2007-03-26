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

package org.rssowl.core.util;


/**
 * Abstract Adapter for the <code>ITask</code>. Implementors need to
 * implement the run()-Method.
 * 
 * @author bpasero
 */
public abstract class TaskAdapter implements ITask {

  /*
   * @see org.rssowl.ui.internal.ITask#getName()
   */
  public String getName() {
    return null;
  }

  /*
   * @see org.rssowl.ui.internal.ITask#getPriority()
   */
  public Priority getPriority() {
    return Priority.DEFAULT;
  }
}