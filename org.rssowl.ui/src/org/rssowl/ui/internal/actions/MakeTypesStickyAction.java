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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets the Sticky-State of selected Items.
 *
 * @author bpasero
 */
public class MakeTypesStickyAction extends Action implements IWorkbenchWindowActionDelegate {
  private IStructuredSelection fSelection;
  private boolean fFlag;

  /**
   * Leave for Reflection.
   */
  public MakeTypesStickyAction() {
    this(StructuredSelection.EMPTY);
  }

  /**
   * @param selection
   */
  public MakeTypesStickyAction(IStructuredSelection selection) {
    fSelection = selection;
    init();
  }

  @Override
  public String getText() {
    return fFlag ? "Set Sticky" : "Unset Sticky";
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return fFlag ? OwlUI.NEWS_PIN : OwlUI.NEWS_PINNED;
  }

  private void init() {
    List<IEntity> entities = ModelUtils.getEntities(fSelection);
    for (IEntity entity : entities) {

      /* News which is not sticky */
      if (entity instanceof INews && !((INews) entity).isFlagged()) {
        fFlag = true;
        break;
      }
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    run();
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    List<IEntity> entities = ModelUtils.getEntities(fSelection);
    List<INews> news = new ArrayList<INews>();

    /* Retrieve INews */
    for (IEntity entity : entities) {
      if (entity instanceof INews)
        news.add((INews) entity);
    }

    /* Set Sticky State */
    for (INews newsItem : news) {
      newsItem.setFlagged(fFlag);
    }

    /* Save List of INews */
    DynamicDAO.saveAll(news);

    /* Update in case this action is rerun on the same selection */
    fFlag = !fFlag;
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