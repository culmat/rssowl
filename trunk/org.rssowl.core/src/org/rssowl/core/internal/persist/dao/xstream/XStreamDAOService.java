/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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

package org.rssowl.core.internal.persist.dao.xstream;

import org.rssowl.core.internal.persist.dao.CachingBookMarkDAO;
import org.rssowl.core.internal.persist.dao.CachingFolderDAO;
import org.rssowl.core.internal.persist.dao.CachingNewsBinDAO;
import org.rssowl.core.internal.persist.dao.CachingSearchDAO;
import org.rssowl.core.internal.persist.dao.CachingSearchMarkDAO;
import org.rssowl.core.internal.persist.dao.ConditionalGetDAOImpl;
import org.rssowl.core.internal.persist.dao.EntitiesToBeIndexedDAOImpl;
import org.rssowl.core.internal.persist.dao.IDescriptionDAO;
import org.rssowl.core.internal.persist.dao.LabelDAOImpl;
import org.rssowl.core.internal.persist.dao.PreferencesDAOImpl;
import org.rssowl.core.internal.persist.dao.SearchConditionDAOImpl;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.dao.DAOService;
import org.rssowl.core.persist.dao.IAttachmentDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.persist.dao.IConditionalGetDAO;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.dao.IPersistableDAO;
import org.rssowl.core.persist.dao.IPersonDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.dao.ISearchConditionDAO;
import org.rssowl.core.persist.dao.ISearchDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class XStreamDAOService extends DAOService {

  private final ISearchConditionDAO fSearchConditionDAO = new SearchConditionDAOImpl();
  private final IPreferenceDAO fPreferencesDAO = new PreferencesDAOImpl();
  private final IConditionalGetDAO fConditionalGetDAO = new ConditionalGetDAOImpl();
  private final ILabelDAO fLabelDAO = new LabelDAOImpl();

  /* Internal */
  //FIXME Decide how to handle this
  private final EntitiesToBeIndexedDAOImpl fEntitiesToBeIndexedDAO = new EntitiesToBeIndexedDAOImpl();

  /* Caching DAOs */
  private final IFolderDAO fFolderDAO = new CachingFolderDAO();
  private final IBookMarkDAO fBookMarkDAO = new CachingBookMarkDAO();
  private final ISearchMarkDAO fSearchMarkDAO = new CachingSearchMarkDAO();
  private final INewsBinDAO fNewsBinDAO = new CachingNewsBinDAO();
  private final ISearchDAO fSearchDAO = new CachingSearchDAO();

  private volatile IAttachmentDAO fAttachmentDAO;
  private volatile ICategoryDAO fCategoryDAO;
  private volatile XStreamFeedDAO fFeedDAO;
  private volatile INewsDAO fNewsDAO;
  private volatile IPersonDAO fPersonDAO;

  private volatile INewsCounterDAO fNewsCounterDAO;

  /* Internal */
  private volatile IDescriptionDAO fDescriptionDAO;

  private volatile Map<Class<?>, Object> fEntityInterfacesToDaosMap;
  private volatile Map<Class<?>, Object> fEntityDaoClassesToDaosMap;
  private volatile Map<Class<?>, Object> fEntityClassesToDaosMap;

  public XStreamDAOService() {
    super();
  }

  public void startup(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) throws PersistenceException {
    createDaos(baseDir, xStream, idGenerator, eventManager);
    fEntityDaoClassesToDaosMap = createEntityDaoClassesToDaosMap();
    fEntityClassesToDaosMap = createEntityClassesToDaosMap();
    fEntityInterfacesToDaosMap = createEntityInterfacesToDaosMap();
  }

  private void createDaos(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    fAttachmentDAO = new XStreamAttachmentDAO(baseDir, xStream, idGenerator, eventManager);
    fCategoryDAO = new XStreamCategoryDAO(baseDir, xStream, idGenerator, eventManager);
    fFeedDAO = new XStreamFeedDAO(baseDir, xStream, idGenerator, eventManager);
    fNewsDAO = new XStreamNewsDAO(baseDir, xStream, idGenerator, eventManager, fFeedDAO, fNewsBinDAO);
    fPersonDAO = new XStreamPersonDAO(baseDir, xStream, idGenerator, eventManager);
    fNewsCounterDAO = new XStreamNewsCounterDAO(baseDir, xStream);
    fDescriptionDAO = new XStreamDescriptionDAO(baseDir, xStream);
  }

  private Map<Class<?>, Object> createEntityInterfacesToDaosMap() {
    Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
    map.put(IAttachment.class, fAttachmentDAO);
    map.put(IBookMark.class, fBookMarkDAO);
    map.put(ICategory.class, fCategoryDAO);
    map.put(IConditionalGet.class, fConditionalGetDAO);
    map.put(IFeed.class, fFeedDAO);
    map.put(IFolder.class, fFolderDAO);
    map.put(ILabel.class, fLabelDAO);
    map.put(INews.class, fNewsDAO);
    map.put(IPerson.class, fPersonDAO);
    map.put(ISearchCondition.class, fSearchConditionDAO);
    map.put(ISearchMark.class, fSearchMarkDAO);
    map.put(IPreference.class, fPreferencesDAO);
    map.put(INewsBin.class, fNewsBinDAO);
    map.put(ISearch.class, fSearchDAO);
    map.put(NewsCounter.class, fNewsCounterDAO);
    return map;
  }

  private Map<Class<?>, Object> createEntityClassesToDaosMap() {
    Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
    for (Object value : fEntityDaoClassesToDaosMap.values()) {
      IPersistableDAO<?> dao = (IPersistableDAO<?>) value;
      map.put(dao.getEntityClass(), dao);
    }
    return map;
  }

  private Map<Class<?>, Object> createEntityDaoClassesToDaosMap() {
    Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
    map.put(IAttachmentDAO.class, fAttachmentDAO);
    map.put(IBookMarkDAO.class, fBookMarkDAO);
    map.put(ICategoryDAO.class, fCategoryDAO);
    map.put(IConditionalGetDAO.class, fConditionalGetDAO);
    map.put(IFeedDAO.class, fFeedDAO);
    map.put(IFolderDAO.class, fFolderDAO);
    map.put(ILabelDAO.class, fLabelDAO);
    map.put(INewsCounterDAO.class, fNewsCounterDAO);
    map.put(INewsDAO.class, fNewsDAO);
    map.put(IPersonDAO.class, fPersonDAO);
    map.put(ISearchConditionDAO.class, fSearchConditionDAO);
    map.put(ISearchMarkDAO.class, fSearchMarkDAO);
    map.put(IPreferenceDAO.class, fPreferencesDAO);
    map.put(INewsBinDAO.class, fNewsBinDAO);
    map.put(ISearchDAO.class, fSearchDAO);
    return map;
  }

  @Override
  public final IPreferenceDAO getPreferencesDAO() {
    return fPreferencesDAO;
  }

  @Override
  public final IAttachmentDAO getAttachmentDAO() {
    return fAttachmentDAO;
  }

  @Override
  public final IBookMarkDAO getBookMarkDAO() {
    return fBookMarkDAO;
  }

  @Override
  public final ICategoryDAO getCategoryDAO() {
    return fCategoryDAO;
  }

  @Override
  public IConditionalGetDAO getConditionalGetDAO() {
    return fConditionalGetDAO;
  }

  @Override
  public final IFeedDAO getFeedDAO() {
    return fFeedDAO;
  }

  @Override
  public final IFolderDAO getFolderDAO() {
    return fFolderDAO;
  }

  @Override
  public final INewsCounterDAO getNewsCounterDAO() {
    return fNewsCounterDAO;
  }

  @Override
  public final INewsDAO getNewsDAO() {
    return fNewsDAO;
  }

  @Override
  public final IPersonDAO getPersonDAO() {
    return fPersonDAO;
  }

  @Override
  public final ISearchConditionDAO getSearchConditionDAO() {
    return fSearchConditionDAO;
  }

  @Override
  public final ISearchMarkDAO getSearchMarkDAO() {
    return fSearchMarkDAO;
  }

  @Override
  public final ILabelDAO getLabelDAO() {
    return fLabelDAO;
  }

  @Override
  public INewsBinDAO getNewsBinDao() {
    return fNewsBinDAO;
  }

  public EntitiesToBeIndexedDAOImpl getEntitiesToBeIndexedDAO() {
    return fEntitiesToBeIndexedDAO;
  }

  public IDescriptionDAO getDescriptionDAO() {
    return fDescriptionDAO;
  }

  @Override
  public ISearchDAO getSearchDAO() {
    return fSearchDAO;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends IPersistableDAO<?>> T getDAO(Class<T> daoInterface) {
    return (T) fEntityDaoClassesToDaosMap.get(daoInterface);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T extends IPersistableDAO<? super P>, P extends IPersistable> T getDAOFromPersistable(Class<P> persistableClass) {
    if (persistableClass.isInterface()) {
      Object value = fEntityInterfacesToDaosMap.get(persistableClass);
      return (T) value;
    }
    Object value = fEntityClassesToDaosMap.get(persistableClass);
    return (T) value;
  }
}
