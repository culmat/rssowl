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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.types.IEntity;
import org.rssowl.core.model.types.INews;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bpasero
 */
public class MarkUnreadAction extends Action implements IWorkbenchWindowActionDelegate {
  private IStructuredSelection fSelection;

  /**
   * 
   */
  public MarkUnreadAction() {}

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
      public void run() {
        MarkUnreadAction.this.run();
      }
    });
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {

    /* Only consider Entities */
    List<IEntity> entities = ModelUtils.getEntities(fSelection);
    List<INews> news = new ArrayList<INews>();

    /* Separate News */
    for (IEntity element : entities) {
      if (element instanceof INews)
        news.add((INews) element);
    }

    /* Mark Unread */
    if (news.size() > 0)
      NewsModel.getDefault().getPersistenceLayer().getApplicationLayer().setNewsState(news, INews.State.UNREAD, true, false);
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {}
}