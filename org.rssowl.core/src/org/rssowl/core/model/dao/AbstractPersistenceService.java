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

import org.rssowl.core.model.persist.dao.DAOFactory;
import org.rssowl.core.model.persist.dao.IAttachmentDAO;
import org.rssowl.core.model.persist.dao.IBookMarkDAO;
import org.rssowl.core.model.persist.dao.ICategoryDAO;
import org.rssowl.core.model.persist.dao.IFeedDAO;
import org.rssowl.core.model.persist.dao.IFolderDAO;
import org.rssowl.core.model.persist.dao.INewsCounterDAO;
import org.rssowl.core.model.persist.dao.INewsDAO;
import org.rssowl.core.model.persist.dao.IPersonDAO;
import org.rssowl.core.model.persist.dao.IPreferencesDAO;
import org.rssowl.core.model.persist.dao.ISearchConditionDAO;
import org.rssowl.core.model.persist.dao.ISearchMarkDAO;
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
  private DAOFactory fDAOFactory;
  
  private IPreferencesDAO fPreferencesDAO;
  private IAttachmentDAO fAttachmentDAO;
  private IBookMarkDAO fBookMarkDAO;
  private ICategoryDAO fCategoryDAO;
  private IFeedDAO fFeedDAO;
  private IFolderDAO fFolderDAO;
  private INewsCounterDAO fNewsCounterDAO;
  private INewsDAO fNewsDAO;
  private IPersonDAO fPersonDAO;
  private ISearchConditionDAO fSearchConditionDAO;
  private ISearchMarkDAO fSearchMarkDAO;

  /** */
  protected AbstractPersistenceService() {
    startup();
  }

  private void startup() throws PersistenceException {

    /* Initialize these Singletons */
    getModelDAO();
    getModelSearch();
    getPreferencesDAO();
    getApplicationLayer();
    getIDGenerator();
    getAttachmentDAO();
    getBookMarkDAO();
    getCategoryDAO();
    getFeedDAO();
    getFolderDAO();
    getNewsCounterDAO();
    getNewsDAO();
    getPersonDAO();
    getSearchConditionDAO();
    getSearchMarkDAO();
  }
  
  /*
   * @see org.rssowl.core.model.dao.IPersistenceService#getDAOFactory()
   */
  public DAOFactory getDAOFactory() {
    if (fDAOFactory == null)
      fDAOFactory = (DAOFactory) ExtensionUtils.loadSingletonExecutableExtension(MODEL_DAO_FACTORY_EXTENSION_POINT);
    
    return fDAOFactory;
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
    if (fPreferencesDAO == null)
      fPreferencesDAO = getDAOFactory().createPreferencesDAO();

    return fPreferencesDAO;
  }

  public IAttachmentDAO getAttachmentDAO() {
    if (fAttachmentDAO == null)
      fAttachmentDAO = getDAOFactory().createAttachmentDAO();
    
    return fAttachmentDAO;
  }

  public IBookMarkDAO getBookMarkDAO() {
    if (fBookMarkDAO == null)
      fBookMarkDAO = getDAOFactory().createBookMarkDAO();
    
    return fBookMarkDAO;
  }

  public ICategoryDAO getCategoryDAO() {
    if (fCategoryDAO == null)
      fCategoryDAO = getDAOFactory().createCategoryDAO();
    
    return fCategoryDAO;
  }

  public IFeedDAO getFeedDAO() {
    if (fFeedDAO == null)
      fFeedDAO = getDAOFactory().createFeedDAO();
    
    return fFeedDAO;
  }

  public IFolderDAO getFolderDAO() {
    if (fFolderDAO == null)
      fFolderDAO = getDAOFactory().createFolderDAO();
    
    return fFolderDAO;
  }

  public INewsCounterDAO getNewsCounterDAO() {
    if (fNewsCounterDAO == null)
      fNewsCounterDAO = getDAOFactory().createNewsCounterDAO();
    
    return fNewsCounterDAO;
  }

  public INewsDAO getNewsDAO() {
    if (fNewsDAO == null)
      fNewsDAO = getDAOFactory().createNewsDAO();
    
    return fNewsDAO;
  }

  public IPersonDAO getPersonDAO() {
    if (fPersonDAO == null)
      fPersonDAO = getDAOFactory().createPersonDAO();
    
    return fPersonDAO;
  }

  public ISearchConditionDAO getSearchConditionDAO() {
    if (fSearchConditionDAO == null)
      fSearchConditionDAO = getDAOFactory().createSearchConditionDAO();
    
    return fSearchConditionDAO;
  }

  public ISearchMarkDAO getSearchMarkDAO() {
    if (fSearchMarkDAO == null)
      fSearchMarkDAO = getDAOFactory().createSearchMarkDAO();
    
    return fSearchMarkDAO;
  }
}