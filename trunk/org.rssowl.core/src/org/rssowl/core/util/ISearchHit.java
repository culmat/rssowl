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

package org.rssowl.core.util;

/**
 * Instances of <code>ISearchHit</code> are the result of running a query in
 * the <code>IModelSearch</code>. Every hit provides the result identified by
 * <code>T</code>, the relevance score and allows to receive additional data
 * in a generic way.
 *
 * @author ijuma
 * @author bpasero
 * @param <T> The type of Object this Hit provides.
 */
public interface ISearchHit<T> {

  /** Indicator for an unknown relevance */
  public static final float UNKNOWN_RELEVANCE = -1.0f;

  /**
   * @return Returns a Reference to the Type that is a Hit of the Search.
   */
  T getResult();

  /**
   * @return Returns the relevance of this Search Hit or
   * <code>UNKNOWN_RELEVANCE</code> in case unknown.
   */
  float getRelevance();

  /**
   * @param key The key to identify the data that is to be retrieved. Must not
   * be <code>NULL</code>.
   * @return Returns the data associated with the key or <code>NULL</code> if
   * none.
   */
  Object getData(Object key);
}