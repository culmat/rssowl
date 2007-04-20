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

package org.rssowl.core.model.persist.dao;

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.persist.IPersistable;

import java.util.Collection;

/**
 * The base interface that provides methods for saving, loading and deleting
 * IPersistables.
 * 
 * @param <T> The type of the IPersistable that the implementation of this interface
 * can handle.
 */
public interface IPersistableDAO<T extends IPersistable> {

  /**
   * Loads the persistable with <code>id</code> from the persistence system
   * and returns it. If no persistable with the provided id exists,
   * <code>null</code> is returned.
   * 
   * @param id The id of the persistable to load from the persistence system.
   * @return the persistable with <code>id</code> or <code>null</code> in
   * case none exists.
   */
  T load(long id);

  Collection<T> loadAll();

  /**
   * Saves <code>persistable</code> to the persistence system. This method
   * handles new and existing perstistables. In other words, it will add or
   * update the persistable as appropriate.
   * 
   * @param persistable The persistable to update.
   * @return The persistable saved.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   */
  T save(T persistable);

  void saveAll(Collection<T> persistables);

  /**
   * Deletes <code>persistable</code> from the persistence system.
   * 
   * @param persistable The persistable to delete.
   */
  void delete(T persistable);

  void deleteAll(Collection<T> objects);

  long countAll();
  
  Class<? extends T> getEntityClass();
}