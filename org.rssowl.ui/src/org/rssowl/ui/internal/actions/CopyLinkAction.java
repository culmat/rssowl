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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.OwlUI;

import java.util.List;

/**
 * Copy the Link of the given Elements into the Clipboard.. E.g. the Link of a
 * BookMark's Feed.
 *
 * @author bpasero
 */
public class CopyLinkAction extends Action {

  /** ID of this Action */
  public static final String ID = "org.rssowl.ui.CopyLinkAction";

  /**
   * Set ID and Action Definition ID.
   */
  public CopyLinkAction() {
    setId(ID);
    setActionDefinitionId(ID);
    setText("Copy Link");
    setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/copy_link.gif"));
    setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dlcl16/copy_link.gif"));
  }

  /*
   * @see org.eclipse.jface.action.Action#isEnabled()
   */
  @Override
  public boolean isEnabled() {
    ISelection selection = OwlUI.getActiveSelection();
    if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
      List<?> list = ((IStructuredSelection) selection).toList();
      for (Object entry : list) {
        if (entry instanceof IBookMark || entry instanceof INews)
          return true;
      }
    }

    return false;
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    ISelection selection = OwlUI.getActiveSelection();
    if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      StringBuilder str = new StringBuilder();

      /* Build Contents */
      if (!structuredSelection.isEmpty()) {
        List<?> list = structuredSelection.toList();
        int i = 0;
        for (Object element : list) {
          if (element instanceof IBookMark) {
            str.append(i > 0 ? "\n" : "").append(((IBookMark) element).getFeedLinkReference().getLinkAsText());
            i++;
          } else if (element instanceof INews) {
            INews news = (INews) element;
            String link = CoreUtils.getLink(news);
            if (link != null) {
              str.append(i > 0 ? "\n" : "").append(link);
              i++;
            }
          }
        }
      }

      /* Set Contents to Clipboard */
      if (str.length() > 0)
        OwlUI.getClipboard().setContents(new Object[] { str.toString() }, new Transfer[] { TextTransfer.getInstance() });
    }
  }
}