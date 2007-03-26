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

package org.rssowl.core.model.reference;

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.types.IEntity;

/**
 * A <code>ModelReference</code> is a potential lightweight representation of
 * a Model-Type. The actual heavyweight Model-Type can be retrieved calling the
 * resolve()-Method.
 * 
 * @author bpasero
 */
public abstract class ModelReference {
  private final long fId;

  /**
   * Instantiates a new leightweight reference. Any resolve()-call will be
   * passed to the <code>IModelDAO</code> to load the heavyweight type from
   * the persistance layer.
   * 
   * @param id The ID of the type to use for loading the type from the
   * persistance layer.
   */
  protected ModelReference(long id) {
    fId = id;
  }

  /**
   * Get the ID of the Type this reference is related to.
   * 
   * @return The ID of the Type this reference is related to.
   */
  public final long getId() {
    return fId;
  }

  /**
   * Returns the heavyweight Model-Type that this reference is pointing to.
   * 
   * @return The heavyweight <code>IEntity</code> that this reference is
   * pointing to.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  public abstract IEntity resolve() throws PersistenceException;
  
  /**
   * Returns <code>true</code> if calling {@link #resolve()} on this reference 
   * will return an entity equal to <code>entity</code>. Note that subclasses 
   * are free to use something besides {@link IEntity#getId()} to assert this.
   * 
   * @param entity The IEntity to compare to.
   * @return <code>true</code> if this object references <code>entity</code> or
   * <code>false</code> otherwise.
   */
  public boolean references(IEntity entity) {
    Long entityId = entity.getId();
    return entityId == null ? false : fId == entityId.longValue();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    ModelReference other = (ModelReference) obj;
    return fId == other.fId;
  }

  @Override
  public int hashCode() {
    return (int) (fId ^ (fId >>> 32));
  }

  @Override
  @SuppressWarnings("nls")
  public String toString() {
    String name = super.toString();
    int index = name.lastIndexOf('.');
    if (index != -1)
      name = name.substring(index + 1, name.length());

    return name + " (ID = " + getId() + ")";
  }
}