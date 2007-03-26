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

package org.rssowl.core.model.types;

import org.eclipse.core.runtime.IAdaptable;

import java.util.Map;

/**
 * The super-type of all Elements.
 * 
 * @author bpasero
 */
public interface IExtendableType extends IAdaptable {

  /**
   * Set a Property identified by a unique Key to this Model. This Method can be
   * used to extend the Model with values, for example in case the interpreted
   * Feed makes use of non-Feed-standard Elements.
   * <p>
   * It is <em>not</em> recommended to store complex types as Properties, but
   * Strings and other basic Types.
   * </p>
   * <p>
   * Chose a key with <em>caution</em>. The key should be qualified like
   * classes, for instance "org.yourproject.yourpackage.YourProperty" in order
   * to avoid overriding another key that was set by a different person.
   * </p>
   * 
   * @param key The unique identifier of the Property.
   * @param value The value of the Property.
   */
  void setProperty(String key, Object value);

  /**
   * Get a Property from this Map or NULL if not existing for the given Key.
   * 
   * @param key The unique identifier of the Property.
   * @return The value of the Property or NULL if no value is stored for the
   * given key.
   */
  Object getProperty(String key);

  /**
   * Removes a Property from this Map.
   * 
   * @param key The unique identifier of the Property.
   * @return The value of the Property or NULL if no value is stored for the
   * given key.
   */
  Object removeProperty(String key);

  /**
   * Get the Map containing all Properties of this Type.
   * 
   * @return The Map containing all Properties of this Type.
   */
  Map<String, ? > getProperties();
}