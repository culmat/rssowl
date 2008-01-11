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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Action to move or copy a selection of News to a Newsbin.
 *
 * @author bpasero
 */
public class MoveCopyNewsToBinAction extends Action {
  private final IStructuredSelection fSelection;
  private final boolean fIsMove;
  private INewsBin fBin;

  /**
   * @param selection
   * @param bin
   * @param isMove
   */
  public MoveCopyNewsToBinAction(IStructuredSelection selection, INewsBin bin, boolean isMove) {
    fSelection = selection;
    fBin = bin;
    fIsMove = isMove;
  }

  /*
   * @see org.eclipse.jface.action.Action#getImageDescriptor()
   */
  @Override
  public ImageDescriptor getImageDescriptor() {
    return OwlUI.NEWSBIN;
  }

  /*
   * @see org.eclipse.jface.action.Action#getText()
   */
  @Override
  public String getText() {
    return fBin != null ? fBin.getName() : "New News Bin...";
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {

    /* Open Dialog to create a new Bin first */
    if (fBin == null) {
      NewNewsBinAction action = new NewNewsBinAction();
      action.run(null);
      fBin = action.getNewsbin();
    }

    /* Move / Copy */
    if (fBin != null)
      moveCopyToBin();
  }

  private void moveCopyToBin() {
    List<?> objects = fSelection.toList();
    Set<INews> news = ModelUtils.normalize(objects);
    boolean requiresSave = false;

    /* For each  News */
    List<INews> skippedNews = new ArrayList<INews>(0);
    for (INews newsitem : news) {

      /* Don't allow adding same news again */
      if (fBin.containsNews(newsitem)) {
        skippedNews.add(newsitem);
        continue;
      }

      INews newsCopy = Owl.getModelFactory().createNews(newsitem);

      /* Ensure the state is *unread* since it has been seen */
      if (newsCopy.getState() == INews.State.NEW)
        newsCopy.setState(INews.State.UNREAD);

      DynamicDAO.save(newsCopy);
      fBin.addNews(newsCopy);

      requiresSave = true;
    }

    /* Save */
    if (requiresSave)
      DynamicDAO.save(fBin);

    /* Delete News from Source if required */
    if (fIsMove) {
      news.removeAll(skippedNews);

      if (!news.isEmpty()) {

        /* Mark Saved Search Service as in need for a quick Update */
        Controller.getDefault().getSavedSearchService().forceQuickUpdate();

        /* Delete News in single Transaction */
        DynamicDAO.getDAO(INewsDAO.class).setState(news, INews.State.DELETED, false, false);
      }
    }
  }
}