/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.core.internal.newsaction;

import org.rssowl.core.INewsAction;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;

import java.util.ArrayList;
import java.util.List;

/**
 * An instance of {@link INewsAction} to mark a list of news as sticky.
 *
 * @author bpasero
 */
public class MarkStickyNewsAction implements INewsAction {

  /*
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(List<INews> news, Object data) {
    List<IEntity> entitiesToSave = new ArrayList<IEntity>(news.size());

    for (INews newsitem : news) {
      if (!newsitem.isFlagged()) {
        newsitem.setFlagged(true);
        entitiesToSave.add(newsitem);
      }
    }

    return entitiesToSave;
  }

  /*
   * @see org.rssowl.core.INewsAction#isConflicting(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return otherAction instanceof DeleteNewsAction;
  }

  /*
   * @see org.rssowl.core.INewsAction#getLabel(java.lang.Object)
   */
  public String getLabel(Object data) {
    return null;
  }
}