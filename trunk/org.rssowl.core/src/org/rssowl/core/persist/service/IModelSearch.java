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

package org.rssowl.core.persist.service;

import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.SearchHit;

import java.util.List;

/**
 * The central interface for searching types from the persistance layer. The
 * implementation is contributable via extension-point mechanism.
 * <p>
 * TODO Add more search-methods as needed.
 * </p>
 *
 * @author bpasero
 */
public interface IModelSearch {

  /**
   * Search for the type <code>INews</code> in the persistance layer.
   *
   * @param conditions A <code>List</code> of Search-Conditions specifying the
   * search to perform.
   * @param matchAllConditions If <code>TRUE</code>, require all conditions
   * to match, and if <code>FALSE</code>, News are considered a result when
   * they match at least 1 condition.
   * @return Returns the result of the search as <code>List</code>. In case
   * no type is matching the search, an empty <code>List</code> is returned.
   * @throws PersistenceException In case of an error while searching.
   */
  List<SearchHit<NewsReference>> searchNews(List<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException;

  /**
   * Releases all resources used by the implementor of this interface. The
   * difference between this method and <code>stopIndexer</code> is that, in
   * addition to stopping the indexer, this method also releases the resources
   * required to perform a search.
   *
   * @throws PersistenceException
   */
  void shutdown() throws PersistenceException;

  /**
   * @throws PersistenceException
   */
  void startup() throws PersistenceException;

  /**
   * Deletes all the information that is stored in the search index. This must
   * be called if the information stored in the persistence layer has been
   * cleared with a method that does not issue events for the elements that are
   * removed. An example of this is
   * <code>PersistenceLayer#recreateSchema()</code>.
   *
   * @throws PersistenceException
   * @see {@link IPersistenceService}#recreateSchema()
   */
  void clearIndex() throws PersistenceException;

  /**
   * Adds a Listener to the list of Listeners that will be notified on index
   * events.
   *
   * @param listener The Listener to add to the list of Listeners that will be
   * notified on index events.
   */
  void addIndexListener(IndexListener listener);

  /**
   * Removes the Listener from the list of Listeners that will be notified on
   * index events.
   *
   * @param listener The Listener to remove from the list of Listeners that will
   * be notified on index events.
   */
  void removeIndexListener(IndexListener listener);
}