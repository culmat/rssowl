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

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class LabelAction extends Action {
  private final Collection<ILabel> fLabels;
  private final IStructuredSelection fSelection;
  private final boolean fAddLabels;

  /**
   * @param labels
   * @param selection
   * @param addLabels
   */
  public LabelAction(Collection<ILabel> labels, IStructuredSelection selection, boolean addLabels) {
    fLabels = labels;
    fSelection = selection;
    fAddLabels = addLabels;
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

      /* For each Label */
      for (ILabel label : fLabels) {

        /* Add Label */
        if (fAddLabels && !newsLabels.contains(label))
          newsItem.addLabel(label);

        /* Remove Label */
        else if (!fAddLabels && newsLabels.contains(label))
          newsItem.removeLabel(label);
      }
    }

    /* Mark Saved Search Service as in need for a quick Update */
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();

    /* Save */
    DynamicDAO.saveAll(newsList);
  }
}