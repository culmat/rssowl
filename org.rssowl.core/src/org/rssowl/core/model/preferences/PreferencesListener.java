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

package org.rssowl.core.model.preferences;

/**
 * A Listener being notified whenever a Preference was added, updated or deleted
 * in the persistance layer.
 * 
 * @author bpasero
 */
public interface PreferencesListener {

  /**
   * @param event Event object containing additional information like the Key
   * and Value of the preference that was added.
   */
  void preferenceAdded(PreferencesEvent event);

  /**
   * @param event Event object containing additional information like the Key
   * and Value of the preference that was updated.
   */
  void preferenceUpdated(PreferencesEvent event);

  /**
   * @param event Event object containing additional information like the Key
   * and Value of the preference that was deleted.
   */
  void preferenceDeleted(PreferencesEvent event);
}