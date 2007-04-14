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

public abstract class DAOFactory {

  public abstract IAttachmentDAO createAttachmentDAO();
  
  public abstract IBookMarkDAO createBookMarkDAO();
  
  public abstract ICategoryDAO createCategoryDAO();
  
  public abstract IFeedDAO createFeedDAO();
  
  public abstract IFolderDAO createFolderDAO();
  
  public abstract INewsCounterDAO createNewsCounterDAO();
  
  public abstract INewsDAO createNewsDAO();
  
  public abstract IPersonDAO createPersonDAO();
  
  public abstract IPreferencesDAO createPreferencesDAO();
  
  public abstract ISearchConditionDAO createSearchConditionDAO();
  
  public abstract ISearchMarkDAO createSearchMarkDAO();

  public abstract ILabelDAO createLabelDAO();
}
