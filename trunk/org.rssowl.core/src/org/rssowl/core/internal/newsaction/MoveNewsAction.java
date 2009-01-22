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

package org.rssowl.core.internal.newsaction;

import org.rssowl.core.INewsAction;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.DynamicDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An instance of {@link INewsAction} to move a list of news to a bin.
 *
 * @author bpasero
 */
public class MoveNewsAction implements INewsAction {

  /** ID of this Action */
  public static final String ID = "org.rssowl.core.MoveNewsAction";

  /*
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(List<INews> news, Object data) {
    List<IEntity> entitiesToSave = new ArrayList<IEntity>(news.size());

    Long[] binIds = (Long[]) data;
    List<INewsBin> bins = new ArrayList<INewsBin>(binIds.length);
    for (Long id : binIds) {
      INewsBin bin = DynamicDAO.load(INewsBin.class, id);
      if (bin != null)
        bins.add(bin);
    }

    if (bins.isEmpty())
      return Collections.emptyList();

    /* For each target Bin */
    for (INewsBin bin : bins) {

      /* For each News: Copy */
      List<INews> copiedNews = new ArrayList<INews>(news.size());
      for (INews newsitem : news) {
        if (newsitem.getParentId() != bin.getId()) { // News could be already inside the bin
          INews newsCopy = Owl.getModelFactory().createNews(newsitem, bin);
          copiedNews.add(newsCopy);

          if (!entitiesToSave.contains(newsitem))
            entitiesToSave.add(newsitem);
        }
      }

      /* Save */
      if (!copiedNews.isEmpty()) {
        DynamicDAO.saveAll(copiedNews);
        DynamicDAO.save(bin);
      }
    }

    /* Set news as deleted */
    for (IEntity entity : entitiesToSave) {
      ((INews) entity).setState(INews.State.HIDDEN);
    }

    return entitiesToSave;
  }

  /*
   * @see org.rssowl.core.INewsAction#isConflicting(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return otherAction instanceof DeleteNewsAction || otherAction instanceof MoveNewsAction || otherAction instanceof CopyNewsAction;
  }
}