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

package org.rssowl.core.model.dao;

import org.rssowl.core.model.persist.dao.DAOService;
import org.rssowl.core.model.persist.dao.IPreferencesDAO;
import org.rssowl.core.model.persist.search.IModelSearch;

/**
 * <p>
 * The <code>IPersistenceService</code> is a contributable class that handles
 * addition, update, deletion and search of the various Model Types in the
 * application.
 * </p>
 *
 * @see AbstractPersistenceService
 * @author bpasero
 */
public interface IPersistenceService {

  void startup();
  
  DAOService getDAOService();
  
  /**
   * Gets the implementation of <code>IDGenerator</code> that generates IDs
   * that have not yet been used by the persistence layer. The implementation is
   * looked up using the "org.rssowl.core.model.IDGenerator" extension point.
   *
   * @return An implementation of IDGenerator.
   * @see IDGenerator
   */
  IDGenerator getIDGenerator();

  /**
   * <p>
   * Get the Implementation of <code>IApplicationLayer</code> that contains
   * special Methods which are used through the Application and access the
   * persistence layer. The implementation is looked up using the
   * "org.rssowl.core.model.ApplicationLayer" Extension Point.
   * </p>
   * Subclasses may override to provide their own implementation.
   *
   * @return Returns the Implementation of <code>IApplicationLayer</code> that
   * contains special Methods which are used through the Application and access
   * the persistence layer.
   */
  IApplicationLayer getApplicationLayer();

  /**
   * <p>
   * Get the Implementation of <code>IPreferencesDAO</code> that allows to
   * add, update and delete preferences. The implementation is looked up using
   * the "org.rssowl.core.model.PreferencesDAO" Extension Point.
   * </p>
   * Subclasses may override to provide their own implementation.
   *
   * @return Returns the Implementation of <code>IPreferencesDAO</code> that
   * allows to add, update and delete preferences.
   */
  IPreferencesDAO getPreferencesDAO();

  /**
   * <p>
   * Get the Implementation of <code>IModelSearch</code> that allows to search
   * model types. The implementation is looked up using the
   * "org.rssowl.core.model.ModelSearch" Extension Point.
   * </p>
   * Subclasses may override to provide their own implementation.
   *
   * @return Returns the Implementation of <code>IModelSearch</code> that
   * allows to search model types.
   */
  IModelSearch getModelSearch();

  /**
   * Shutdown the persistence layer. In case of a Database, this would be the
   * right place to save the relations.
   *
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  void shutdown() throws PersistenceException;

  /**
   * Recreate the Schema of the persistence layer. In case of a Database, this
   * would drop relations and create them again.
   *
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  void recreateSchema() throws PersistenceException;
}