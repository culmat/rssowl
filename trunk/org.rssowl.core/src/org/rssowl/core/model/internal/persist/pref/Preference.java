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
package org.rssowl.core.model.internal.persist.pref;

import org.eclipse.core.runtime.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Preference {
  public enum Type { 
    BOOLEAN, INTEGER, LONG, STRING, LONG_ARRAY, STRING_ARRAY, INTEGER_ARRAY;
  }
  
  private String fKey;
  
  private List<String> fValues;
  
  private Type fType;
  
  /**
   * Provided for deserialization purposes.
   */
  protected Preference() {
  }
  
  public Preference(String key, Type type) {
    Assert.isNotNull(key, "key cannot be null"); //$NON-NLS-1$
    Assert.isNotNull(type, "Type cannot be null"); //$NON-NLS-1$
    this.fKey = key;
    this.fType = type;
  }
  
  public final String getKey() {
    return fKey;
  }
  
  public final void addValue(String value) {
    Assert.isNotNull(value, "value cannot be null"); //$NON-NLS-1$
    if (fValues == null) {
      fValues = new ArrayList<String>();
    }
    fValues.add(value);
  }
  
  public final List<String> getValues() {
    if (fValues == null)
      return Collections.emptyList();
    
    return Collections.unmodifiableList(fValues);
  }
  
  public final void removeValue(int index) {
    fValues.remove(index);
  }

  public Type getType() {
    return fType;
  }
}
