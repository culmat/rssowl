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

package org.rssowl.core;

import org.rssowl.core.persist.INews;

import java.util.List;

/**
 * <p>
 * Instances of {@link INewsAction} can be contributed from the
 * org.rssowl.core.NewsAction extension point. Their purpose is to perfrom
 * certain operations on a List of {@link INews}, e.g. marking them read.
 * </p>
 * The news filter facility in RSSOwl makes use of {@link INewsAction} to
 * perform certain operations based on search conditions.
 *
 * @author bpasero
 */
public interface INewsAction {

  /**
   * Runs the operation on the list of news.
   *
   * @param news the list of news to perform the operation on.
   * @param data arbitrary data associated with the action.
   */
  void run(List<INews> news, Object data);

  /**
   * Checks whether the two operations can be used together or not. E.g. an
   * operation to delete a list of news is likely not compatible with another
   * operation to mark the news as read.
   *
   * @param otherAction another news action to test for conflicting operations.
   * @return <code>true</code> in case the two operations can not be used
   * together and <code>false</code> otherwise.
   */
  boolean conflictsWith(INewsAction otherAction);
}