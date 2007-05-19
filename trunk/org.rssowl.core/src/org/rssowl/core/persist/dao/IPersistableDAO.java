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

package org.rssowl.core.persist.dao;

import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.service.PersistenceException;

import java.util.Collection;

/**
 * The base interface that provides methods for saving, loading and deleting
 * IPersistables.
 *
 * @param <T> The type of the IPersistable that the implementation of this
 * interface can handle.
 */
public interface IPersistableDAO<T extends IPersistable> {

  /**
   * Loads a <code>Collection</code> of all <code>IPersistable</code> this
   * DAO is responsible for from the persistence service.
   *
   * @return Returns a <code>Collection</code> of all
   * <code>IPersistable</code> this DAO is responsible for from the
   * persistence service.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   */
  Collection<T> loadAll() throws PersistenceException;

  /**
   * Saves <code>persistable</code> to the persistence system. This method
   * handles new and existing perstistables. In other words, it will add or
   * update the persistable as appropriate.
   *
   * @param persistable The persistable to update.
   * @return The persistable saved.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   * @see IPersistableDAO#saveAll(Collection)
   */
  T save(T persistable) throws PersistenceException;

  /**
   * Saves the given Collection of <code>persistable</code>s to the
   * persistence system in a single operation. This method handles new and
   * existing perstistables. In other words, it will add or update the
   * persistable as appropriate.
   *
   * @param persistables The persistables to save.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   * @see IPersistableDAO#save(IPersistable)
   */
  void saveAll(Collection<T> persistables) throws PersistenceException;

  /**
   * Deletes <code>persistable</code> from the persistence system.
   *
   * @param persistable The persistable to delete.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   * @see IPersistableDAO#deleteAll(Collection)
   */
  void delete(T persistable) throws PersistenceException;

  /**
   * Deletes the given Collection of <code>persistable</code>s from the
   * persistence system in a single operation.
   *
   * @param persistables The persistables to delete.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   * @see IPersistableDAO#delete(IPersistable)
   */
  void deleteAll(Collection<T> persistables) throws PersistenceException;

  /**
   * Counts the number of all <code>IPersistable</code> this DAO is
   * responsible for from the persistence layer.
   *
   * @return Returns the number of all <code>IPersistable</code> this DAO is
   * responsible for from the persistence layer.
   * @throws PersistenceException In case of an error while trying to perform
   * the operation.
   */
  long countAll() throws PersistenceException;

  /**
   * @return Returns the <code>Class</code> this DAO is responsible for.
   */
  Class<? extends T> getEntityClass();
}