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

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.events.NewsEvent;
import org.rssowl.core.persist.events.NewsListener;

import java.util.Collection;

public interface INewsDAO extends IEntityDAO<INews, NewsListener, NewsEvent>    {

  /**
   * Sets the state of all the news items contained in <code>news</code> to
   * <code>state</code>. In addition, if <code>affectEquivalentNews</code>
   * is <code>true</code>, the state of equivalent news in other feeds will
   * also be changed to <code>state</code>. Note that news items whose state
   * is equal to <code>state</code> will not be changed or updated in the
   * persistence layer.
   *
   * @param news A Collection of <code>INews</code> whose state should be changed.
   * @param state The state to set the news items to.
   * @param affectEquivalentNews If set to <code>TRUE</code> the state of
   * equivalent news in other feeds will also be changed to <code>state</code>
   * @param force If set to <code>TRUE</code>, the method will update even
   * those News that match the given state.
   * @throws PersistenceException
   */
  void setState(Collection<INews> news, INews.State state, boolean affectEquivalentNews, boolean force) throws PersistenceException; 
}
