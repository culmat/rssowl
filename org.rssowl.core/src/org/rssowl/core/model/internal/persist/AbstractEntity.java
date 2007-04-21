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

package org.rssowl.core.model.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IEntity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bpasero
 */
public abstract class AbstractEntity extends Persistable implements IEntity {
  private Long fId;
  private HashMap<String, Serializable> fProperties;

  /**
   * Default constructor provided for deserialization purposes.
   */
  protected AbstractEntity() {}

  /**
   * @param id
   */
  protected AbstractEntity(Long id) {
    fId = id;
  }

  /*
   * @see org.rssowl.core.model.types.IEntity#getId()
   */
  public Long getId() {
    return fId;
  }

  /*
   * @see org.rssowl.core.model.types.IEntity#setId(java.lang.Long)
   */
  public void setId(Long id) {
    Assert.isNotNull(id, "id cannot be null"); //$NON-NLS-1$
    if (id.equals(fId))
      return;

    if (fId != null)
      throw new IllegalStateException("Cannot change id after it's been set."); //$NON-NLS-1$

    fId = id;
  }

  /**
   * Set a Property identified by a unique Key to this Model. Because the value
   * is persisted into the DataBase, it is required that the value is
   * implementing <code>java.io.Serializable</code>
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
   * @see org.rssowl.core.persist.IEntity#setProperty(java.lang.String,
   * java.lang.Object)
   */
  public void setProperty(String key, Object value) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      fProperties = new HashMap<String, Serializable>();
  
    /* Ignore any value not being a subtype of Serializable */
    if (value instanceof Serializable)
      fProperties.put(key, (Serializable) value);
  }

  public Object getProperty(String key) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      return null;
  
    return fProperties.get(key);
  }

  public Object removeProperty(String key) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      return null;
  
    return fProperties.remove(key);
  }

  public Map<String, ? > getProperties() {
    if (fProperties == null)
      return Collections.emptyMap();
  
    return Collections.unmodifiableMap(fProperties);
  }
  
  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    IEntity type = (IEntity) obj;
    if (fId == null || type.getId() == null)
      return false;

    return fId.equals(type.getId());
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    if (fId == null)
      return super.hashCode();

    return fId.hashCode();
  }

  /*
   * @see java.lang.Object#toString()
   */
  @Override
  @SuppressWarnings("nls")
  public String toString() {
    String name = super.toString();
    int index = name.lastIndexOf('.');
    if (index != -1)
      name = name.substring(index + 1, name.length());

    return name + " (Properties = " + getProperties() + ", ";
  }
}