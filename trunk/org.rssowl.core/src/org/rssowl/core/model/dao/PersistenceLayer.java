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

import org.rssowl.core.model.preferences.IPreferencesDAO;
import org.rssowl.core.model.search.IModelSearch;
import org.rssowl.core.util.ExtensionUtils;

/**
 * <p>
 * The <code>PersistenceLayer</code> is a contributable class that handles
 * addition, update, deletion and search of the various Model Types in the
 * application.
 * </p>
 * <p>
 * Contributors have to implement the Methods startup() and shutdown(), but may
 * chose to leave the others as provided in this abstract class. This default
 * implementation uses extension-points to lookup ModelDAO, ModelSearch and
 * PreferencesDAO.
 * </p>
 * 
 * @author bpasero
 */
public abstract class PersistenceLayer {

  /* ID for Application Layer Contribution */
  private static final String MODEL_APPLICATION_LAYER_EXTENSION_POINT = "org.rssowl.core.ApplicationLayer"; //$NON-NLS-1$

  /* ID for Model DAO Contribution */
  private static final String MODEL_DAO_EXTENSION_POINT = "org.rssowl.core.ModelDAO"; //$NON-NLS-1$

  /* ID for Model Search Contribution */
  private static final String MODEL_SEARCH_EXTENSION_POINT = "org.rssowl.core.ModelSearch"; //$NON-NLS-1$

  /* ID for Preferences DAO Contribution */
  private static final String MODEL_PREFERENCES_EXTENSION_POINT = "org.rssowl.core.PreferencesDAO"; //$NON-NLS-1$

  /* ID for ID Generator Contribution */
  private static final String MODEL_ID_GENERATOR_EXTENSION_POINT = "org.rssowl.core.IDGenerator"; //$NON-NLS-1$

  private IModelDAO fModelDAO;
  private IModelSearch fModelSearch;
  private IPreferencesDAO fPreferencesDAO;
  private IApplicationLayer fApplicationLayer;
  private IDGenerator fIDGenerator;

  /**
   * <p>
   * Get the Implementation of <code>IModelDAO</code> that allows to add,
   * update and delete model types. The implementation is looked up using the
   * "org.rssowl.core.model.ModelDAO" Extension Point.
   * </p>
   * Subclasses may override to provide their own implementation.
   * 
   * @return Returns the Implementation of <code>IModelDAO</code> that allows
   * to add, update and delete model types.
   */
  public IModelDAO getModelDAO() {
    if (fModelDAO == null)
      fModelDAO = (IModelDAO) ExtensionUtils.loadSingletonExecutableExtension(MODEL_DAO_EXTENSION_POINT);

    return fModelDAO;
  }

  /**
   * Gets the implementation of <code>IDGenerator</code> that generates IDs
   * that have not yet been used by the persistence layer. The implementation if
   * looked up using the "org.rssowl.core.model.IDGenerator" extension point.
   * 
   * @return An implementation of IDGenerator.
   * @see IDGenerator
   */
  public IDGenerator getIDGenerator() {
    if (fIDGenerator == null)
      fIDGenerator = (IDGenerator) ExtensionUtils.loadSingletonExecutableExtension(MODEL_ID_GENERATOR_EXTENSION_POINT);

    return fIDGenerator;
  }

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
  public IApplicationLayer getApplicationLayer() {
    if (fApplicationLayer == null)
      fApplicationLayer = (IApplicationLayer) ExtensionUtils.loadSingletonExecutableExtension(MODEL_APPLICATION_LAYER_EXTENSION_POINT);

    return fApplicationLayer;
  }

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
  public IPreferencesDAO getPreferencesDAO() {
    if (fPreferencesDAO == null)
      fPreferencesDAO = (IPreferencesDAO) ExtensionUtils.loadSingletonExecutableExtension(MODEL_PREFERENCES_EXTENSION_POINT);

    return fPreferencesDAO;
  }

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
  public IModelSearch getModelSearch() {
    if (fModelSearch == null)
      fModelSearch = (IModelSearch) ExtensionUtils.loadSingletonExecutableExtension(MODEL_SEARCH_EXTENSION_POINT);

    return fModelSearch;
  }

  /**
   * Startup the persistence layer. In case of a Database, this would be the
   * right place to create relations. Subclasses should override.
   * 
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  @SuppressWarnings("unused")
  public void startup() throws PersistenceException {

    /* Initialize these Singletons */
    getModelDAO();
    getModelSearch();
    getPreferencesDAO();
    getApplicationLayer();
    getIDGenerator();
  }

  /**
   * Shutdown the persistence layer. In case of a Database, this would be the
   * right place to save the relations.
   * 
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  public abstract void shutdown() throws PersistenceException;

  /**
   * Recreate the Schema of the persistence layer. In case of a Database, this
   * would drop relations and create them again.
   * 
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  public abstract void recreateSchema() throws PersistenceException;
}