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
package org.rssowl.core.model.internal.db4o.dao;

import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.NewsCounter;
import org.rssowl.core.model.persist.dao.DAOService;
import org.rssowl.core.model.persist.dao.IAttachmentDAO;
import org.rssowl.core.model.persist.dao.IBookMarkDAO;
import org.rssowl.core.model.persist.dao.ICategoryDAO;
import org.rssowl.core.model.persist.dao.IConditionalGetDAO;
import org.rssowl.core.model.persist.dao.IFeedDAO;
import org.rssowl.core.model.persist.dao.IFolderDAO;
import org.rssowl.core.model.persist.dao.ILabelDAO;
import org.rssowl.core.model.persist.dao.INewsCounterDAO;
import org.rssowl.core.model.persist.dao.INewsDAO;
import org.rssowl.core.model.persist.dao.IPersistableDAO;
import org.rssowl.core.model.persist.dao.IPersonDAO;
import org.rssowl.core.model.persist.dao.IPreferencesDAO;
import org.rssowl.core.model.persist.dao.ISearchConditionDAO;
import org.rssowl.core.model.persist.dao.ISearchMarkDAO;
import org.rssowl.core.model.persist.search.ISearchCondition;

import java.util.HashMap;
import java.util.Map;

public final class DAOServiceImpl extends DAOService  {

  private final IPreferencesDAO fPreferencesDAO = new PreferencesDAOImpl();
  private final IAttachmentDAO fAttachmentDAO = new AttachmentDAOImpl();
  private final IBookMarkDAO fBookMarkDAO = new BookMarkDAOImpl();
  private final ICategoryDAO fCategoryDAO = new CategoryDAOImpl();
  private final IConditionalGetDAO fConditionalGetDAO = new ConditionalGetDAOImpl();
  private final IFeedDAO fFeedDAO = new FeedDAOImpl();
  private final IFolderDAO fFolderDAO = new FolderDAOImpl();
  private final INewsCounterDAO fNewsCounterDAO = new NewsCounterDAOImpl();
  private final INewsDAO fNewsDAO = new NewsDAOImpl();
  private final IPersonDAO fPersonDAO = new PersonDAOImpl();
  private final ISearchConditionDAO fSearchConditionDAO = new SearchConditionDAOImpl();
  private final ISearchMarkDAO fSearchMarkDAO = new SearchMarkDAOImpl();
  private final ILabelDAO fLabelDAO = new LabelDAOImpl();
  
  private final Map<Class<?>, Object> fEntityInterfacesToDaosMap = new HashMap<Class<?>, Object>();
  private final Map<Class<?>, Object> fEntityDaoClassesToDaosMap = new HashMap<Class<?>, Object>();
  private final Map<Class<?>, Object> fEntityClassesToDaosMap = new HashMap<Class<?>, Object>();
  
  public DAOServiceImpl() {
    super();
    fEntityDaoClassesToDaosMap.put(IAttachmentDAO.class, fAttachmentDAO);
    fEntityDaoClassesToDaosMap.put(IBookMarkDAO.class, fBookMarkDAO);
    fEntityDaoClassesToDaosMap.put(ICategoryDAO.class, fCategoryDAO);
    fEntityDaoClassesToDaosMap.put(IConditionalGetDAO.class, fConditionalGetDAO);
    fEntityDaoClassesToDaosMap.put(IFeedDAO.class, fFeedDAO);
    fEntityDaoClassesToDaosMap.put(IFolderDAO.class, fFolderDAO);
    fEntityDaoClassesToDaosMap.put(ILabelDAO.class, fLabelDAO);
    fEntityDaoClassesToDaosMap.put(INewsCounterDAO.class, fNewsCounterDAO);
    fEntityDaoClassesToDaosMap.put(INewsDAO.class, fNewsDAO);
    fEntityDaoClassesToDaosMap.put(IPersonDAO.class, fPersonDAO);
    fEntityDaoClassesToDaosMap.put(ISearchConditionDAO.class, fSearchConditionDAO);
    fEntityDaoClassesToDaosMap.put(ISearchMarkDAO.class, fSearchMarkDAO);

    for (Object value : fEntityDaoClassesToDaosMap.values()) {
      IPersistableDAO<?> dao = (IPersistableDAO<?>) value;
      putInEntityClassesToDaosMap(dao);
    }
    fEntityDaoClassesToDaosMap.put(IPreferencesDAO.class, fPreferencesDAO);
    
    fEntityInterfacesToDaosMap.put(IAttachment.class, fAttachmentDAO);
    fEntityInterfacesToDaosMap.put(IBookMark.class, fBookMarkDAO);
    fEntityInterfacesToDaosMap.put(ICategory.class, fCategoryDAO);
    fEntityInterfacesToDaosMap.put(IConditionalGet.class, fConditionalGetDAO);
    fEntityInterfacesToDaosMap.put(IFeed.class, fFeedDAO);
    fEntityInterfacesToDaosMap.put(IFolder.class, fFolderDAO);
    fEntityInterfacesToDaosMap.put(ILabel.class, fLabelDAO);
    fEntityInterfacesToDaosMap.put(NewsCounter.class, fNewsCounterDAO);
    fEntityInterfacesToDaosMap.put(INews.class, fNewsDAO);
    fEntityInterfacesToDaosMap.put(IPerson.class, fPersonDAO);
    fEntityInterfacesToDaosMap.put(ISearchCondition.class, fSearchConditionDAO);
    fEntityInterfacesToDaosMap.put(ISearchMark.class, fSearchMarkDAO);
    
  }

  private void putInEntityClassesToDaosMap(IPersistableDAO<?> dao) {
    fEntityClassesToDaosMap.put(dao.getEntityClass(), dao);
  }

  @Override
  public final IPreferencesDAO getPreferencesDAO() {
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
  
  @SuppressWarnings("unchecked")
  @Override
  public final <T extends IPersistableDAO<?>> T getDAO(Class<T> daoInterface) {
    return (T) fEntityDaoClassesToDaosMap.get(daoInterface);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public final <T extends IPersistableDAO<? super P>, P extends IPersistable> T getDAOFromEntity(Class<P> persistableClass) {
    if (persistableClass.isInterface()) {
      Object value = fEntityInterfacesToDaosMap.get(persistableClass);
      return (T) value;
    }
    Object value = fEntityClassesToDaosMap.get(persistableClass);
    return (T) value;
  }
}
