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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.INews;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bpasero
 */
public class LabelAction extends Action {
  private ILabel fLabel;
  private IStructuredSelection fSelection;
  private IApplicationLayer fAppLayer;

  /**
   * @param label
   * @param selection
   */
  public LabelAction(ILabel label, IStructuredSelection selection) {
    fLabel = label;
    fSelection = selection;
    fAppLayer = NewsModel.getDefault().getPersistenceLayer().getApplicationLayer();
  }

  @Override
  public void run() {
    if (!fSelection.isEmpty()) {
      List<IEntity> entities = ModelUtils.getEntities(fSelection);
      List<INews> news = new ArrayList<INews>();
      for (IEntity entity : entities) {
        if (entity instanceof INews) {
          INews newsItem = (INews) entity;

          /* Apply Label */
          ILabel label = newsItem.getLabel();
          if (label == null && fLabel != null)
            newsItem.setLabel(fLabel);
          else if (label != null && fLabel == null)
            newsItem.setLabel(null);
          else if (label != null && fLabel != null && !fLabel.equals(label))
            newsItem.setLabel(fLabel);

          /* Add to List */
          news.add((INews) entity);
        }
      }

      /* Save */
      if (news.size() > 0)
        fAppLayer.saveNews(news);
    }
  }
}