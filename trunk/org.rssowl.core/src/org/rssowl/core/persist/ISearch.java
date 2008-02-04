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
package org.rssowl.core.persist;

import org.rssowl.core.persist.service.IModelSearch;

import java.util.List;

/**
 * An entity that represents a set of search conditions that can be used to find
 * matching news.
 *
 * @see IModelSearch
 */
public interface ISearch extends IEntity    {

  /**
   * Gets the name of this search.
   *
   * @return the name of this search.
   */
  String getName();

  /**
   * Sets the name of this search,
   *
   * @param name The name of this search.
   */
  void setName(String name);

  /**
   * Adds a <code>ISearchCondition</code> to the list of conditions used to
   * search for <code>INews</code>.
   *
   * @param searchCondition The condition to add.
   */
  void addSearchCondition(ISearchCondition searchCondition);

  /**
   * Removes a <code>ISearchCondition</code> from the list of conditions used
   * to search for <code>INews</code>.
   *
   * @param searchCondition The condition to remove.
   * @return <code>true</code> if the item was removed.
   */
  boolean removeSearchCondition(ISearchCondition searchCondition);

  /**
   * @return A List of search conditions specifying the search that is to be
   * performed to match News.
   * <p>
   * Note: The returned List should not be modified. The default Implementation
   * returns an unmodifiable List using
   * <code>Collections.unmodifiableList()</code>. Trying to modify the List
   * will result in <code>UnsupportedOperationException</code>.
   * </p>
   */
  List<ISearchCondition> getSearchConditions();

  /**
   * Describes how the search conditions are connected to other conditions from
   * the same search:
   * <ul>
   * <li>If TRUE, News have to match <em>all</em> Conditions</li>
   * <li>If FALSE, News have to match at least <em>one</em> of the Conditions</li>
   * </ul>
   *
   * @return Returns <code>TRUE</code> if this condition is to be connected to
   * other conditions of the same search with a logical AND, <code>FALSE</code>
   * if this condition is to be connected with a logical OR.
   */
  boolean matchAllConditions();

  /**
   * @param matchAllConditions <code>TRUE</code> if this condition is to be
   * connected to other conditions of the same search with a logical AND,
   * <code>FALSE</code> if this condition is to be connected with a logical
   * OR.
   */
  void setMatchAllConditions(boolean matchAllConditions);

}
