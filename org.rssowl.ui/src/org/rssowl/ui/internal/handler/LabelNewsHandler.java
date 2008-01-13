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

package org.rssowl.ui.internal.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.rssowl.core.persist.ILabel;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.editors.feed.FeedView;

/**
 * This {@link IHandler} is required to support key-bindings for dynamic
 * programmatic added actions like labelling news.
 *
 * @author bpasero
 */
public class LabelNewsHandler extends AbstractHandler {
  private final ILabel fLabel;

  /**
   * @param label The {@link ILabel} to assign to the selected news.
   */
  public LabelNewsHandler(ILabel label) {
    fLabel = label;
  }

  /*
   * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  @Override
  public Object execute(ExecutionEvent event) {
    FeedView feedview = OwlUI.getActiveFeedView();
    if (feedview == null)
      return null;

    ISelectionProvider selectionProvider = feedview.getSite().getSelectionProvider();
    if (selectionProvider == null)
      return null;

    /* Perform Action */
    new LabelAction(fLabel, (IStructuredSelection) selectionProvider.getSelection(), true).run();

    return null; //As per JavaDoc
  }
}