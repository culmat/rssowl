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
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.List;

/**
 * @author bpasero
 */
public class LabelAction extends Action {
  private ILabel fLabel;
  private IStructuredSelection fSelection;

  /**
   * @param label
   * @param selection
   */
  public LabelAction(ILabel label, IStructuredSelection selection) {
    fLabel = label;
    fSelection = selection;
  }

  @Override
  public void run() {
    List<INews> newsList = ModelUtils.getEntities(fSelection, INews.class);
    for (INews newsItem : newsList) {
      /* Apply Label */
      ILabel label = newsItem.getLabel();
      if (label == null && fLabel != null)
        newsItem.setLabel(fLabel);
      else if (label != null && fLabel == null)
        newsItem.setLabel(null);
      else if (label != null && fLabel != null && !fLabel.equals(label))
        newsItem.setLabel(fLabel);
    }

    /* Mark Saved Search Service as in need for a quick Update */
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();

    /* Save */
    DynamicDAO.saveAll(newsList);
  }
}