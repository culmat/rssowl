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

import org.rssowl.core.persist.reference.NewsReference;

import java.util.EnumSet;
import java.util.List;

/**
 * The SearchMark is acting like virtual folders in Thunderbird. The user is
 * defining some criterias, e.g. "mozilla" as part of a news-title, and all News
 * that matches this criteria will be related to this SearchMark.
 *
 * @author bpasero
 */
public interface ISearchMark extends IMark {

  /** One of the fields in this type described as constant */
  public static final int MATCHING_NEWS = 4;

  /** One of the fields in this type described as constant */
  public static final int SEARCH_CONDITIONS = 5;

  /** One of the fields in this type described as constant */
  public static final int MATCH_ALL_CONDITIONS = 6;

  /**
   * Sets the result of this search mark. The result is a List of
   * <code>NewsReference</code> that may be <code>NULL</code> if not results
   * are matching the search. In addition, a <code>INews.State</code> has to
   * be provided in order to quickly resolve the state information out of the
   * matching news.
   *
   * @param news A List of <code>NewsReference</code> that may be
   * <code>NULL</code> if not results are matching the search.
   * @param state The <code>INews.State</code> of the news that match the
   * search.
   * @return Returns <code>TRUE</code> if the new result differs from the
   * existing one and <code>FALSE</code> otherwise.
   */
  boolean setResult(List<NewsReference> news, INews.State state);

  /**
   * Returns a List of all news that match this search mark's conditions. To
   * reduce the memory impact of this method, the news are returned as
   * <code>NewsReference</code>.
   *
   * @return Returns a List of all news that match this search mark's
   * conditions. To reduce the memory impact of this method, the news are
   * returned as <code>NewsReference</code>.
   */
  List<NewsReference> getResult();

  /**
   * Returns a List of all news that match this search mark's conditions and the
   * provided <code>INews.State</code>. To reduce the memory impact of this
   * method, the news are returned as <code>NewsReference</code>.
   *
   * @param states A Set of <code>INews.State</code> that the resulting news
   * must have.
   * @return Returns a List of all news that match this search mark's conditions
   * and the provided <code>INews.State</code>. To reduce the memory impact
   * of this method, the news are returned as <code>NewsReference</code>.
   */
  List<NewsReference> getResult(EnumSet<INews.State> states);

  /**
   * Returns the number of news that match this search mark's conditions and the
   * provided <code>INews.State</code>.
   *
   * @param states A Set of <code>INews.State</code> that the resulting news
   * must have.
   * @return Returns the number of news that match this search mark's conditions
   * and the provided <code>INews.State</code>.
   */
  int getResultCount(EnumSet<INews.State> states);

  /**
   * Adds a <code>ISearchCondition</code> to the list of conditions this
   * searchmark uses to search for <code>INews</code>.
   *
   * @param searchCondition The condition to add.
   */
  void addSearchCondition(ISearchCondition searchCondition);

  /**
   * Removes a <code>ISearchCondition</code> from the list of conditions this
   * searchmark uses to search for <code>INews</code>.
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