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
import org.rssowl.core.util.ExtensionUtils;

/**
 * <p>
 * The <code>AbstractPersistenceService</code> is a contributable class that
 * handles addition, update, deletion and search of the various Model Types in
 * the application.
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
public abstract class AbstractPersistenceService implements IPersistenceService {

  /* ID for Application Layer Contribution */
  private static final String MODEL_APPLICATION_LAYER_EXTENSION_POINT = "org.rssowl.core.ApplicationLayer"; //$NON-NLS-1$

  /* ID for Model DAO Contribution */
  private static final String MODEL_DAO_EXTENSION_POINT = "org.rssowl.core.ModelDAO"; //$NON-NLS-1$

  /* ID for Model Search Contribution */
  private static final String MODEL_SEARCH_EXTENSION_POINT = "org.rssowl.core.ModelSearch"; //$NON-NLS-1$

  /* ID for ID Generator Contribution */
  private static final String MODEL_ID_GENERATOR_EXTENSION_POINT = "org.rssowl.core.IDGenerator"; //$NON-NLS-1$

  /* ID for DAO Factory Contribution */
  private static final String MODEL_DAO_FACTORY_EXTENSION_POINT = "org.rssowl.core.DAOFactory";
  
  private IModelDAO fModelDAO;
  private IModelSearch fModelSearch;
  private IApplicationLayer fApplicationLayer;
  private IDGenerator fIDGenerator;
  private DAOService fDAOService;
  
  /** */
  protected AbstractPersistenceService() {
    startup();
  }

  private void startup() throws PersistenceException {

    /* Initialize these Singletons */
    getModelDAO();
    getModelSearch();
    getApplicationLayer();
    getIDGenerator();
    getDAOService();
  }
  
  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getDAOFactory()
   */
  public DAOService getDAOService() {
    if (fDAOService == null)
      fDAOService = (DAOService) ExtensionUtils.loadSingletonExecutableExtension(MODEL_DAO_FACTORY_EXTENSION_POINT);
    
    return fDAOService;
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getApplicationLayer()
   */
  public IApplicationLayer getApplicationLayer() {
    if (fApplicationLayer == null)
      fApplicationLayer = (IApplicationLayer) ExtensionUtils.loadSingletonExecutableExtension(MODEL_APPLICATION_LAYER_EXTENSION_POINT);

    return fApplicationLayer;
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getIDGenerator()
   */
  public IDGenerator getIDGenerator() {
    if (fIDGenerator == null)
      fIDGenerator = (IDGenerator) ExtensionUtils.loadSingletonExecutableExtension(MODEL_ID_GENERATOR_EXTENSION_POINT);

    return fIDGenerator;
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getModelDAO()
   */
  public IModelDAO getModelDAO() {
    if (fModelDAO == null)
      fModelDAO = (IModelDAO) ExtensionUtils.loadSingletonExecutableExtension(MODEL_DAO_EXTENSION_POINT);

    return fModelDAO;
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getModelSearch()
   */
  public IModelSearch getModelSearch() {
    if (fModelSearch == null)
      fModelSearch = (IModelSearch) ExtensionUtils.loadSingletonExecutableExtension(MODEL_SEARCH_EXTENSION_POINT);

    return fModelSearch;
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getPreferencesDAO()
   */
  public IPreferencesDAO getPreferencesDAO() {
    return fDAOService.getPreferencesDAO();
  }
}