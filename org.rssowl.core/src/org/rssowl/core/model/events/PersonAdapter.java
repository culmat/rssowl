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

package org.rssowl.core.model.events;

import java.util.Set;

/**
 * Provides an empty implementation of <code>PersonListener</code>. Useful if
 * the client only needs to implement a subset of the interface.
 * 
 * @author bpasero
 */
public class PersonAdapter implements PersonListener {

  /*
   * @see org.rssowl.core.model.events.PersonListener#personAdded(java.util.List)
   */
  public void entitiesAdded(Set<PersonEvent> events) { }

  /*
   * @see org.rssowl.core.model.events.PersonListener#personDeleted(java.util.List)
   */
  public void entitiesDeleted(Set<PersonEvent> events) { }

  /*
   * @see org.rssowl.core.model.events.PersonListener#personUpdated(java.util.List)
   */
  public void entitiesUpdated(Set<PersonEvent> events) { }
}