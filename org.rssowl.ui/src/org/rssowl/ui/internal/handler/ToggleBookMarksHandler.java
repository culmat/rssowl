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

package org.rssowl.ui.internal.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

/**
 * This {@link IHandler} is required to support key-bindings for programmatic
 * added actions.
 *
 * @author bpasero
 */
public class ToggleBookMarksHandler extends AbstractHandler implements IHandler {

  /*
   * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
   */
  public Object execute(ExecutionEvent event) {
    IWorkbenchPage page = OwlUI.getPage();
    if (page != null) {
      IViewPart explorerView = page.findView(BookMarkExplorer.VIEW_ID);

      /* Hide Bookmarks */
      if (explorerView != null)
        page.hideView(explorerView);

      /* Show Bookmarks */
      else {
        try {
          page.showView(BookMarkExplorer.VIEW_ID);
        } catch (PartInitException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    }

    return null; //As per JavaDoc
  }
}