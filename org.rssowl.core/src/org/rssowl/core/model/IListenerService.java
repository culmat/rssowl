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

package org.rssowl.core.model;

import org.rssowl.core.persist.pref.PreferencesEvent;
import org.rssowl.core.persist.pref.PreferencesListener;

/**
 * @author bpasero
 */
public interface IListenerService {

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferenceAdded(final PreferencesEvent event);

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferencesDeleted(final PreferencesEvent event);

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferencesUpdated(final PreferencesEvent event);

  /**
   * @param listener the PreferencesListener to add to the list of listeners.
   */
  void addPreferencesListener(PreferencesListener listener);

  /**
   * @param listener the PreferencesListener to remove to the list of listeners.
   */
  void removePreferencesListener(PreferencesListener listener);
}