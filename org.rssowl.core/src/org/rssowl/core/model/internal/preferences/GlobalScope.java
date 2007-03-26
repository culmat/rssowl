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

package org.rssowl.core.model.internal.preferences;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.preferences.IPreferencesDAO;
import org.rssowl.core.model.preferences.IPreferencesScope;
import org.rssowl.core.model.preferences.PreferencesEvent;
import org.rssowl.core.model.preferences.PreferencesListener;

import java.util.Arrays;
import java.util.Properties;

/**
 * Implementation of <code>IPreferencesScope</code> that asks the
 * <code>IPreferenesDAO</code> of the persistence layer for its Preferences.
 * 
 * @author bpasero
 */
public class GlobalScope implements IPreferencesScope {
  private Properties fCache;
  private IPreferencesScope fParent;
  private IPreferencesDAO fPrefDao;

  /**
   * @param parent
   */
  public GlobalScope(IPreferencesScope parent) {
    fParent = parent;
    fCache = new Properties();
    fPrefDao = NewsModel.getDefault().getPersistenceLayer().getPreferencesDAO();

    registerListeners();
  }

  private void registerListeners() {
    NewsModel.getDefault().addPreferencesListener(new PreferencesListener() {
      public void preferenceAdded(PreferencesEvent event) {
        fCache.put(event.getKey(), event.getValue());
      }

      public void preferenceDeleted(PreferencesEvent event) {
        fCache.remove(event.getKey());
      }

      public void preferenceUpdated(PreferencesEvent event) {
        fCache.put(event.getKey(), event.getValue());
      }
    });
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesScope#getParent()
   */
  public IPreferencesScope getParent() {
    return fParent;
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesScope#flush()
   */
  public void flush() {
  // TODO Consider later for performance improvements
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#delete(java.lang.String)
   */
  public void delete(String key) {
    fPrefDao.delete(key);
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getBoolean(java.lang.String)
   */
  public boolean getBoolean(String key) {
    Boolean res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (Boolean) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getBoolean(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getBoolean(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getInteger(java.lang.String)
   */
  public int getInteger(String key) {
    Integer res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (Integer) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getInteger(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getInteger(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getIntegers(java.lang.String)
   */
  public int[] getIntegers(String key) {
    int[] res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (int[]) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getIntegers(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getIntegers(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getLong(java.lang.String)
   */
  public long getLong(String key) {
    Long res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (Long) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getLong(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getLong(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getLongs(java.lang.String)
   */
  public long[] getLongs(String key) {
    long[] res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (long[]) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getLongs(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getLongs(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getString(java.lang.String)
   */
  public String getString(String key) {
    String res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (String) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getString(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getString(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesNode#getStrings(java.lang.String)
   */
  public String[] getStrings(String key) {
    String[] res = null;

    /* Consult Cache */
    Object cachedRes = fCache.get(key);
    if (cachedRes != null)
      return (String[]) cachedRes;

    /* Consult the Persistence Layer */
    res = fPrefDao.getStrings(key);

    /* Ask Parent */
    if (res == null)
      res = fParent.getStrings(key);

    /* Cache Value */
    if (res != null)
      fCache.put(key, res);

    return res;
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putBoolean(java.lang.String,
   * boolean)
   */
  public void putBoolean(String key, boolean value) {

    /* Check if value is already up-to-date */
    if (cached(key, value))
      return;

    /* Delete if matches parent scope */
    if (value == fParent.getBoolean(key)) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putBoolean(key, value);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putInteger(java.lang.String,
   * int)
   */
  public void putInteger(String key, int value) {

    /* Check if value is already up-to-date */
    if (cached(key, value))
      return;

    /* Delete if matches parent scope */
    if (value == fParent.getInteger(key)) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putInteger(key, value);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putIntegers(java.lang.String,
   * int[])
   */
  public void putIntegers(String key, int[] values) {
    Assert.isNotNull(values);

    /* Check if value is already up-to-date */
    if (cached(key, values))
      return;

    /* Delete if matches parent scope */
    if (Arrays.equals(values, fParent.getIntegers(key))) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putIntegers(key, values);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putLong(java.lang.String,
   * long)
   */
  public void putLong(String key, long value) {

    /* Check if value is already up-to-date */
    if (cached(key, value))
      return;

    /* Delete if matches parent scope */
    if (value == fParent.getLong(key)) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putLong(key, value);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putLongs(java.lang.String,
   * long[])
   */
  public void putLongs(String key, long[] values) {
    Assert.isNotNull(values);

    /* Check if value is already up-to-date */
    if (cached(key, values))
      return;

    /* Delete if matches parent scope */
    if (Arrays.equals(values, fParent.getLongs(key))) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putLongs(key, values);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putString(java.lang.String,
   * java.lang.String)
   */
  public void putString(String key, String value) {
    Assert.isNotNull(value);

    /* Check if value is already up-to-date */
    if (cached(key, value))
      return;

    /* Delete if matches parent scope */
    if (value.equals(fParent.getString(key))) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putString(key, value);
  }

  /*
   * @see org.rssowl.ui.internal.preferences.IPreferencesNode#putStrings(java.lang.String,
   * java.lang.String[])
   */
  public void putStrings(String key, String[] values) {
    Assert.isNotNull(values);

    /* Check if value is already up-to-date */
    if (cached(key, values))
      return;

    /* Delete if matches parent scope */
    if (Arrays.equals(values, fParent.getStrings(key))) {
      delete(key);
      return;
    }

    /* Save to DB */
    fPrefDao.putStrings(key, values);
  }

  private boolean cached(String key, Object value) {
    Object cachedRes = fCache.get(key);
    if (cachedRes == null)
      return false;

    if (value instanceof Object[])
      return Arrays.equals((Object[]) cachedRes, (Object[]) value);

    return cachedRes.equals(value);
  }
}