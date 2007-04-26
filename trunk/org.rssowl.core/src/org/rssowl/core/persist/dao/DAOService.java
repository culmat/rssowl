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

/**
 * The <code>DAOService</code> is an abstract class that provides getter to
 * the data access objects of all <code>IPersistable</code> model types in
 * RSSOwl. This service can be contributed by using the DAOService extension
 * point provided in this bundle.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public abstract class DAOService {

  public abstract <T extends IPersistableDAO< ? >> T getDAO(Class<T> daoInterface);

  public abstract <T extends IPersistableDAO< ? super P>, P extends IPersistable> T getDAOFromEntity(Class<P> persistableClass);

  public abstract IAttachmentDAO getAttachmentDAO();

  public abstract IBookMarkDAO getBookMarkDAO();

  public abstract ICategoryDAO getCategoryDAO();

  public abstract IFeedDAO getFeedDAO();

  public abstract IFolderDAO getFolderDAO();

  public abstract INewsCounterDAO getNewsCounterDAO();

  public abstract INewsDAO getNewsDAO();

  public abstract IPersonDAO getPersonDAO();

  public abstract IPreferencesDAO getPreferencesDAO();

  public abstract ISearchConditionDAO getSearchConditionDAO();

  public abstract ISearchMarkDAO getSearchMarkDAO();

  public abstract ILabelDAO getLabelDAO();

  public abstract IConditionalGetDAO getConditionalGetDAO();
}