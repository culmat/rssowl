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

package org.rssowl.ui.internal.undo;

import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * An instance of {@link IUndoOperation} allowing to undo/redo copying of News
 * into a Bin.
 *
 * @author bpasero
 */
public class CopyOperation implements IUndoOperation {
  private final Map<State, List<NewsReference>> fCopiedNews;
  final NewsBinReference fBinRef;
  final int fNewsCount;
  final INewsDAO fNewsDao = DynamicDAO.getDAO(INewsDAO.class);

  /**
   * @param copiednews
   * @param bin
   */
  public CopyOperation(List<INews> copiednews, INewsBin bin) {
    fCopiedNews = ModelUtils.toStateMap(copiednews);
    fBinRef = bin.toReference();
    fNewsCount = copiednews.size();
  }

  /*
   * @see org.rssowl.ui.internal.undo.IUndoOperation#getName()
   */
  public String getName() {
    return "Copy " + fNewsCount + " News";
  }

  /*
   * @see org.rssowl.ui.internal.undo.IUndoOperation#undo()
   */
  public void undo() {

    /* Remove copied News from Bin */
    INewsBin bin = fBinRef.resolve();
    if (bin == null)
      return;

    List<INews> news = ModelUtils.resolveAll(fCopiedNews);
    for (INews newsitem : news) {
      bin.removeNews(newsitem);
    }

    DynamicDAO.save(bin);

    /* Force quick update of saved searches */
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();

    /* Set Copied News to Hidden */
    fNewsDao.setState(news, INews.State.HIDDEN, false, false);
  }

  /*
   * @see org.rssowl.ui.internal.undo.IUndoOperation#redo()
   */
  public void redo() {

    /* Add copied News to Bin again and restore State */
    INewsBin bin = fBinRef.resolve();
    if (bin == null)
      return;

    Set<Entry<State, List<NewsReference>>> entries = fCopiedNews.entrySet();
    for (Entry<State, List<NewsReference>> entry : entries) {
      INews.State oldState = entry.getKey();
      List<NewsReference> newsRefs = entry.getValue();

      List<INews> resolvedNews = new ArrayList<INews>(newsRefs.size());
      for (NewsReference newsRef : newsRefs) {
        INews newsitem = newsRef.resolve();
        if (newsitem != null) {
          resolvedNews.add(newsitem);

          bin.addNews(newsitem);
        }
      }

      /* Force quick update of saved searches */
      Controller.getDefault().getSavedSearchService().forceQuickUpdate();

      /* Set old state back to all news */
      fNewsDao.setState(resolvedNews, oldState, false, false);
    }

    DynamicDAO.save(bin);
  }
}