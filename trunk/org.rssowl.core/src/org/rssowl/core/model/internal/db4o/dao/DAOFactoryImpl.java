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

import org.rssowl.core.model.persist.dao.DAOFactory;
import org.rssowl.core.model.persist.dao.IAttachmentDAO;
import org.rssowl.core.model.persist.dao.IBookMarkDAO;
import org.rssowl.core.model.persist.dao.ICategoryDAO;
import org.rssowl.core.model.persist.dao.IFeedDAO;
import org.rssowl.core.model.persist.dao.IFolderDAO;
import org.rssowl.core.model.persist.dao.ILabelDAO;
import org.rssowl.core.model.persist.dao.INewsCounterDAO;
import org.rssowl.core.model.persist.dao.INewsDAO;
import org.rssowl.core.model.persist.dao.IPersonDAO;
import org.rssowl.core.model.persist.dao.IPreferencesDAO;
import org.rssowl.core.model.persist.dao.ISearchConditionDAO;
import org.rssowl.core.model.persist.dao.ISearchMarkDAO;

public final class DAOFactoryImpl extends DAOFactory  {

  @Override
  public final IAttachmentDAO createAttachmentDAO() {
    return new AttachmentDAOImpl();
  }

  @Override
  public IBookMarkDAO createBookMarkDAO() {
    return new BookMarkDAOImpl();
  }

  @Override
  public ICategoryDAO createCategoryDAO() {
    return new CategoryDAOImpl();
  }

  @Override
  public IFeedDAO createFeedDAO() {
    return new FeedDAOImpl();
  }

  @Override
  public IFolderDAO createFolderDAO() {
    return new FolderDAOImpl();
  }

  @Override
  public INewsCounterDAO createNewsCounterDAO() {
    return new NewsCounterDAOImpl();
  }

  @Override
  public INewsDAO createNewsDAO() {
    return new NewsDAOImpl();
  }

  @Override
  public IPersonDAO createPersonDAO() {
    return new PersonDAOImpl();
  }

  @Override
  public IPreferencesDAO createPreferencesDAO() {
    return new PreferencesDAOImpl();
  }

  @Override
  public ISearchConditionDAO createSearchConditionDAO() {
    return new SearchConditionDAOImpl();
  }

  @Override
  public ISearchMarkDAO createSearchMarkDAO() {
    return new SearchMarkDAOImpl();
  }

  @Override
  public ILabelDAO createLabelDAO() {
    return new LabelDAOImpl();
  }
}
