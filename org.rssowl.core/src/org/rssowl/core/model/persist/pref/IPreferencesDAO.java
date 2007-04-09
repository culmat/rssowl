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

package org.rssowl.core.model.persist.pref;

import org.rssowl.core.model.dao.PersistenceException;

/**
 * The <code>IPreferencesDAO</code> offers methods to store and retrieve
 * Preferences, simply by providing a Key-Value-Pair. The underlying persistance
 * layer is responsible for how the Values are stored.
 * 
 * @author bpasero
 */
public interface IPreferencesDAO {

  /**
   * Stores a <code>boolean</code> value under the given key into the
   * persistance layer or updates it, if it is already present.
   * 
   * @param key The key under which the value is stored.
   * @param value The <code>boolean</code> value that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putBoolean(String key, boolean value) throws PersistenceException;

  /**
   * Retrieves a <code>Boolean</code> value from the persistance layer, or
   * <code>NULL</code> if not present.
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>Boolean</code> value or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  Boolean getBoolean(String key) throws PersistenceException;

  /**
   * Stores a <code>String</code> value under the given key into the
   * persistance layer or updates it, if it is already present.
   * 
   * @param key The key under which the value is stored.
   * @param value The <code>String</code> value that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putString(String key, String value) throws PersistenceException;

  /**
   * Retrieves a <code>String</code> value from the persistance layer, or
   * <code>NULL</code> if not present.
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>String</code> value or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  String getString(String key) throws PersistenceException;

  /**
   * Stores a <code>String</code> array under the given key into the
   * persistance layer or updates it, if it is already present.
   * <p>
   * Note: The underlying persistence solution is making sure to keep the order
   * of Items inside the Array when saving and loading.
   * </p>
   * 
   * @param key The key under which the value is stored.
   * @param values The <code>String</code> array that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putStrings(String key, String values[]) throws PersistenceException;

  /**
   * Retrieves a <code>String</code> array from the persistance layer, or
   * <code>NULL</code> if not present.
   * <p>
   * Note: The underlying persistence solution is making sure to keep the order
   * of Items inside the Array when saving and loading.
   * </p>
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>String</code> array or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  String[] getStrings(String key) throws PersistenceException;

  /**
   * Stores a <code>int</code> value under the given key into the persistance
   * layer or updates it, if it is already present.
   * 
   * @param key The key under which the value is stored.
   * @param value The <code>int</code> value that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putInteger(String key, int value) throws PersistenceException;

  /**
   * Retrieves a <code>Integer</code> value from the persistance layer, or
   * <code>NULL</code> if not present.
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>Integer</code> value or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  Integer getInteger(String key) throws PersistenceException;

  /**
   * Stores a <code>long</code> value under the given key into the persistance
   * layer or updates it, if it is already present.
   * 
   * @param key The key under which the value is stored.
   * @param value The <code>long</code> value that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putLong(String key, long value) throws PersistenceException;

  /**
   * Retrieves a <code>Long</code> value from the persistance layer, or
   * <code>NULL</code> if not present.
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>Long</code> value or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  Long getLong(String key) throws PersistenceException;

  /**
   * Stores a <code>long</code> array under the given key into the persistance
   * layer or updates it, if it is already present.
   * <p>
   * Note: The underlying persistence solution is making sure to keep the order
   * of Items inside the Array when saving and loading.
   * </p>
   * 
   * @param key The key under which the value is stored.
   * @param values The <code>long</code> array that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putLongs(String key, long values[]) throws PersistenceException;
  
  /**
   * Stores a <code>int</code> array under the given key into the persistance
   * layer or updates it, if it is already present.
   * 
   * @param key The key under which the value is stored.
   * @param values The <code>int</code> array that is to be stored.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  void putIntegers(String key, int values[]) throws PersistenceException;

  /**
   * Retrieves a <code>long</code> array from the persistance layer, or
   * <code>NULL</code> if not present.
   * <p>
   * Note: The underlying persistence solution is making sure to keep the order
   * of Items inside the Array when saving and loading.
   * </p>
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>long</code> array or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  long[] getLongs(String key) throws PersistenceException;
  
  /**
   * Retrieves a <code>int</code> array from the persistance layer, or
   * <code>NULL</code> if not present.
   * 
   * @param key The Key under which the value is stored.
   * @return The <code>int</code> array or <code>NULL</code> if not
   * existant for the given key.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  int[] getIntegers(String key) throws PersistenceException;

  /**
   * If the persistence layer contains a preference with a key that matches
   * <code>key</code>, the preference is deleted and <code>true</code> is
   * returned. Otherwise, no action is taken and <code>false</code> is returned.
   * 
   * @param key The key under which the value is stored.
   * @return <code>true</code> if a preference exists with key matching
   * <code>key</code>. <code>false</code> otherwise.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer.
   */
  boolean delete(String key) throws PersistenceException;
}