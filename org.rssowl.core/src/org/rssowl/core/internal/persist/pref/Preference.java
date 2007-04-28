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
package org.rssowl.core.internal.persist.pref;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.persist.IPreference;

import java.util.Arrays;

public final class Preference extends AbstractEntity implements IPreference {
  private String fKey;
  
  private Type fType;
  private String[] fValues;
  private transient Object fCachedValues;
  
  /**
   * Provided for deserialization purposes.
   */
  protected Preference() {
  }
  
  public Preference(String key) {
    Assert.isNotNull(key, "key cannot be null"); //$NON-NLS-1$
    this.fKey = key;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getKey()
   */
  public synchronized final String getKey() {
    return fKey;
  }
  
  public synchronized final Type getType() {
    return fType;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getBoolean()
   */
  public synchronized final Boolean getBoolean() {
    boolean[] values = getBooleans();
    if (values != null && values.length > 0)
      return values[0];
    
    return null;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getBooleans()
   */
  public synchronized final boolean[] getBooleans() {
    if (fValues == null)
      return null;
    checkType(Type.BOOLEAN);
    
    boolean[] cachedValues = (boolean[]) fCachedValues;
    if (fCachedValues != null)
      return Arrays.copyOf(cachedValues, cachedValues.length);
    
    cachedValues = new boolean[fValues.length];
    int index = 0;
    for (String value : fValues) {
      cachedValues[index++] = Boolean.valueOf(value);
    }
    fCachedValues = cachedValues;
    return Arrays.copyOf(cachedValues, cachedValues.length);
  }
  
  private void checkType(Type type) {
    Assert.isLegal(fType == type, "The type of the Preference is not of the expected " +
        "type. It should be: " + fType + ", but it is: " + type);
  }

  /*
   * @see org.rssowl.core.internal.persist.pref.T#getInteger()
   */
  public synchronized final Integer getInteger() {
    int[] values = getIntegers();
    if (values != null && values.length > 0)
      return values[0];
    
    return null;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getIntegers()
   */
  public synchronized final int[] getIntegers() {
    if (fValues == null)
      return null;
    checkType(Type.INTEGER);
    
    int[] cachedValues = (int[]) fCachedValues;
    if (fCachedValues != null)
      return Arrays.copyOf(cachedValues, cachedValues.length);
    
    cachedValues = new int[fValues.length];
    int index = 0;
    for (String value : fValues) {
      cachedValues[index++] = Integer.valueOf(value);
    }
    fCachedValues = cachedValues;
    return Arrays.copyOf(cachedValues, cachedValues.length);
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getLong()
   */
  public synchronized final Long getLong() {
    long[] values = getLongs();
    if (values != null && values.length > 0)
      return values[0];
    
    return null;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getLongs()
   */
  public synchronized final long[] getLongs()   { 
    if (fValues == null)
      return null;
    checkType(Type.LONG);
    
    long[] cachedValues = (long[]) fCachedValues;
    if (fCachedValues != null)
      return Arrays.copyOf(cachedValues, cachedValues.length);
    
    cachedValues = new long[fValues.length];
    int index = 0;
    for (String value : fValues) {
      cachedValues[index++] = Long.valueOf(value);
    }
    fCachedValues = cachedValues;
    return Arrays.copyOf(cachedValues, cachedValues.length);
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getString()
   */
  public synchronized final String getString() {
    String[] values = getStrings();
    if (values != null && values.length > 0)
      return values[0];
    
    return null;
  }
  
  /*
   * @see org.rssowl.core.internal.persist.pref.T#getStrings()
   */
  public synchronized final String[] getStrings() {
    if (fValues == null)
      return null;
    checkType(Type.STRING);
    
    return Arrays.copyOf(fValues, fValues.length);
  }
  
  public synchronized final void putStrings(String ... strings) {
    if (strings == null) {
      clear();
      return;
    }
    fType = Type.STRING;
    String[] cachedValues = Arrays.copyOf(strings, strings.length);
    fCachedValues = cachedValues;
    fValues = cachedValues;
  }

  public synchronized final void putLongs(long ... longs) {
    if (longs == null) {
      clear();
      return;
    }
    fType = Type.LONG;
    long[] cachedValues = Arrays.copyOf(longs, longs.length);
    fCachedValues = cachedValues;
    fValues = new String[cachedValues.length];
    int index = 0;
    for (long cachedValue : cachedValues) {
      fValues[index++] = String.valueOf(cachedValue);
    }
  }
  
  public synchronized final void putIntegers(int ... integers) {
    if (integers == null) {
      clear();
      return;
    }
    fType = Type.INTEGER;
    int[] cachedValues = Arrays.copyOf(integers, integers.length);
    fCachedValues = cachedValues;
    fValues = new String[cachedValues.length];
    int index = 0;
    for (int cachedValue : cachedValues) {
      fValues[index++] = String.valueOf(cachedValue);
    }
  }
  
  public synchronized final void putBooleans(boolean ... booleans) {
    if (booleans == null) {
      clear();
      return;
    }
    fType = Type.BOOLEAN;
    boolean[] cachedValues = Arrays.copyOf(booleans, booleans.length);
    fCachedValues = cachedValues;
    fValues = new String[cachedValues.length];
    int index = 0;
    for (boolean cachedValue : cachedValues) {
      fValues[index++] = String.valueOf(cachedValue);
    }
  }

  public synchronized final void clear() {
    fValues = null;
    fType = null;
    fCachedValues = null;
  }
}
