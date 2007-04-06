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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.rssowl.core.model.types.INews;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.editors.browser.WebBrowserView;

import java.net.MalformedURLException;
import java.util.List;

/**
 * @author bpasero
 */
public class OpenInBrowserAction extends Action implements IWorkbenchWindowActionDelegate {
  private IStructuredSelection fSelection;

  /** */
  public OpenInBrowserAction() {
    this(StructuredSelection.EMPTY);
  }

  /**
   * @param selection
   */
  public OpenInBrowserAction(IStructuredSelection selection) {
    fSelection = selection;
    setText("Open in Browser");
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {}

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
    List< ? > selection = fSelection.toList();
    for (Object object : selection) {
      if (object instanceof INews) {
        INews news = (INews) object;
        if (news.getLink() != null) {
          IWorkbenchBrowserSupport browser = PlatformUI.getWorkbench().getBrowserSupport();
          try {
            browser.createBrowser(WebBrowserView.EDITOR_ID).openURL(news.getLink().toURL());
          } catch (PartInitException e) {
            Activator.getDefault().getLog().log(e.getStatus());
          } catch (MalformedURLException e) {
            Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
      }
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }
}