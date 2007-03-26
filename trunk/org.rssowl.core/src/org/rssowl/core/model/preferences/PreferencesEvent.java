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

import org.eclipse.core.runtime.Assert;

/**
 * An Event-Object being used to notify Listeners, whenever a Preference was
 * added, updated or deleted in the persistance layer.
 * 
 * @author bpasero
 */
public class PreferencesEvent {
  private Object fValue;
  private String fKey;

  /**
   * @param key The Key of the Preferences as identifier
   * @param value The Value of the Preferences
   */
  public PreferencesEvent(String key, Object value) {
    Assert.isNotNull(key, "The Key must not be NULL"); //$NON-NLS-1$
    Assert.isNotNull(value, "The Value must not be NULL"); //$NON-NLS-1$
    fKey = key;
    fValue = value;
  }

  /**
   * Get the value of the Preference.
   * 
   * @return The Value of the Preference.
   */
  public Object getValue() {
    return fValue;
  }

  /**
   * Get the value of the Preference as <code>Boolean</code>.
   * 
   * @return The Value of the Preference as <code>Boolean</code>.
   */
  public Boolean getBoolean() {
    Assert.isTrue(fValue instanceof Boolean, "The Value is not of the type Boolean"); //$NON-NLS-1$
    return ((Boolean) fValue);
  }

  /**
   * Get the value of the Preference as <code>Integer</code>.
   * 
   * @return The Value of the Preference as <code>Integer</code>.
   */
  public Integer getInteger() {
    Assert.isTrue(fValue instanceof Integer, "The Value is not of the type Integer"); //$NON-NLS-1$
    return ((Integer) fValue);
  }

  /**
   * Get the value of the Preference as <code>Long</code>.
   * 
   * @return The Value of the Preference as <code>Long</code>.
   */
  public Long getLong() {
    Assert.isTrue(fValue instanceof Long, "The Value is not of the type Long"); //$NON-NLS-1$
    return ((Long) fValue);
  }

  /**
   * Get the value of the Preference as <code>String</code>.
   * 
   * @return The Value of the Preference as <code>String</code>.
   */
  public String getString() {
    Assert.isTrue(fValue instanceof String, "The Value is not of the type String"); //$NON-NLS-1$
    return (String) fValue;
  }

  /**
   * Get the value of the Preference as <code>String[]</code>.
   * 
   * @return The Value of the Preference as <code>String[]</code>.
   */
  public String[] getStrings() {
    Assert.isTrue(fValue instanceof String[], "The Value is not of the type String[]"); //$NON-NLS-1$
    return (String[]) fValue;
  }

  /**
   * Get the Key of the Preference.
   * 
   * @return The Key of the Preference.
   */
  public String getKey() {
    return fKey;
  }
}