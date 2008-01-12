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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class LabelAction extends Action {
  private final ILabel fLabel;
  private IStructuredSelection fSelection;
  private final boolean fAddLabel;

  /**
   * @param label
   * @param selection
   * @param addLabel
   */
  public LabelAction(ILabel label, IStructuredSelection selection, boolean addLabel) {
    fLabel = label;
    fSelection = selection;
    fAddLabel = addLabel;
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    List<INews> newsList = ModelUtils.getEntities(fSelection, INews.class);
    if (newsList.isEmpty())
      return;

    /* For each News */
    for (INews newsItem : newsList) {
      Set<ILabel> newsLabels = newsItem.getLabels();

      /* Add Label */
      if (fAddLabel) {
        newsItem.addLabel(fLabel);
      }

      /* Remove single Label */
      else if (fLabel != null)
        newsItem.removeLabel(fLabel);

      /* Remove all Labels */
      else {
        List<ILabel> newsLabelsCopy = new ArrayList<ILabel>(newsLabels.size());
        newsLabelsCopy.addAll(newsLabels);

        for (ILabel newsLabel : newsLabelsCopy) {
          newsItem.removeLabel(newsLabel);
        }
      }
    }

    /* Mark Saved Search Service as in need for a quick Update */
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();

    /* Save */
    DynamicDAO.saveAll(newsList);
  }
}