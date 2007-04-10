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
package org.rssowl.core.model.internal.db4o;

import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.internal.persist.pref.Preference;
import org.rssowl.core.model.internal.persist.pref.Preference.Type;
import org.rssowl.core.model.persist.pref.IPreferencesDAO;
import org.rssowl.core.model.persist.pref.PreferencesEvent;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Query;

import java.util.List;

/**
 * Default implementation of {@link IPreferencesDAO}.
 *
 * {@inheritDoc}
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class PreferencesDAOImpl implements IPreferencesDAO  {

  private ObjectContainer fDb = DBManager.getDefault().getObjectContainer();

  /**
   * Creates an instance of this class.
   */
  public PreferencesDAOImpl() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
      }
      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }

  public void putInteger(String key, int value) throws PersistenceException {
    Integer valueInteger = Integer.valueOf(value);
    Preference pref = new Preference(key, Type.INTEGER);
    pref.addValue(valueInteger.toString());
    savePreference(pref, valueInteger);
  }

  public void putIntegers(String key, int[] values) throws PersistenceException {
    Preference pref = new Preference(key, Type.INTEGER_ARRAY);
    for (int value : values) {
      pref.addValue(String.valueOf(value));
    }
    savePreference(pref, values);
  }

  public void putLong(String key, long value) throws PersistenceException {
    Long valueLong = Long.valueOf(value);
    Preference pref = new Preference(key, Type.LONG);
    pref.addValue(valueLong.toString());
    savePreference(pref, valueLong);
  }
  public void putLongs(String key, long[] values) throws PersistenceException {
    Preference pref = new Preference(key, Type.LONG_ARRAY);
    for (long value : values) {
      pref.addValue(String.valueOf(value));
    }
    savePreference(pref, values);
  }
  public void putString(String key, String value) throws PersistenceException {
    Preference pref = new Preference(key, Type.STRING);
    pref.addValue(value);
    savePreference(pref, value);
  }

  public void putStrings(String key, String[] values) throws PersistenceException {
    Preference pref = new Preference(key, Type.STRING_ARRAY);
    for (String value : values) {
      pref.addValue(value);
    }
    savePreference(pref, values);
  }

  public void putBoolean(String key, boolean value)
      throws PersistenceException {
    Boolean valueBoolean = Boolean.valueOf(value);
    Preference pref = new Preference(key, Type.BOOLEAN);
    pref.addValue(valueBoolean.toString());
    savePreference(pref, valueBoolean);
  }

  @SuppressWarnings("unchecked")
  private Preference findPreference(String key)   {
    Query query = fDb.ext().query();
    query.constrain(Preference.class);
    query.descend("fKey").constrain(key); //$NON-NLS-1$
    ObjectSet<Preference> prefs = query.execute();
    if (prefs.hasNext()) {
      Preference pref = prefs.next();
      fDb.activate(pref, Integer.MAX_VALUE);
      return pref;
    }
    return null;
  }

  private synchronized void savePreference(Preference preference, Object originalObject) throws PersistenceException {
    Preference savedPreference = findPreference(preference.getKey());
    boolean update = false;
    if (savedPreference == null)
      fDb.ext().set(preference, Integer.MAX_VALUE);
    else {
      update = true;

      if (savedPreference.getType() != preference.getType()) {
        throw new PersistenceException("Trying to replace an existing " + //$NON-NLS-1$
            "preference with a preference of a different type"); //$NON-NLS-1$
      }
      fDb.delete(savedPreference);
      fDb.ext().set(preference, Integer.MAX_VALUE);
    }
    fDb.commit();

    PreferencesEvent event = new PreferencesEvent(preference.getKey(),
        originalObject);
    if (update) {
      Owl.getListenerService().notifyPreferencesUpdated(event);
    }
    else {
      Owl.getListenerService().notifyPreferenceAdded(event);
    }
  }

  private Object getValues(String key)
      throws PersistenceException {
    Preference pref = findPreference(key);
    return getValues(pref);
  }

  private Object getValues(Preference pref) {
    if (pref == null || pref.getValues().size() == 0) {
      return null;
    }
    List<String> values = pref.getValues();
    switch (pref.getType()) {
      case BOOLEAN:
        return Boolean.valueOf(values.get(0));
      case INTEGER:
        return Integer.valueOf(values.get(0));
      case LONG:
        return Long.valueOf(values.get(0));
      case STRING:
        return values.get(0);
      case LONG_ARRAY:
        return getLongArray(values);
      case INTEGER_ARRAY:
        return getIntegerArray(values);
      case STRING_ARRAY:
        return values.toArray(new String[values.size()]);
    }
    throw new IllegalStateException("unknown preference type found: " + pref.getType()); //$NON-NLS-1$
  }

  private int[] getIntegerArray(List<String> values) {
    int[] intArray = new int[values.size()];
    for (int i = 0, c = values.size(); i < c; ++i) {
      intArray[i] = Integer.valueOf(values.get(i));
    }
    return intArray;
  }

  private long[] getLongArray(List<String> values) {
    long[] longArray = new long[values.size()];
    for (int i = 0, c = values.size(); i < c; ++i) {
      longArray[i] = Long.valueOf(values.get(i));
    }
    return longArray;
  }

  public Boolean getBoolean(String key) throws PersistenceException {
    return (Boolean) getValues(key);
  }

  public String getString(String key) throws PersistenceException {
    return (String) getValues(key);
  }

  public String[] getStrings(String key) throws PersistenceException {
    return (String[]) getValues(key);
  }

  public Integer getInteger(String key) throws PersistenceException {
    return (Integer) getValues(key);
  }

  public int[] getIntegers(String key) throws PersistenceException {
    return (int[]) getValues(key);
  }

  public long[] getLongs(String key) throws PersistenceException {
    return (long[]) getValues(key);
  }

  public Long getLong(String key) throws PersistenceException {
    return (Long) getValues(key);
  }

  public boolean delete(String key) throws PersistenceException {
    Preference pref = findPreference(key);
    if (pref == null)
      return false;

    Object value = getValues(pref);
    fDb.delete(pref);
    fDb.commit();
    Owl.getListenerService().notifyPreferencesDeleted(new PreferencesEvent(key,
        value));
    return true;
  }

}