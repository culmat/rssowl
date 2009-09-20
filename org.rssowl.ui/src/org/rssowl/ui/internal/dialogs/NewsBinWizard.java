/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.rssowl.ui.internal.OwlUI;

/**
 * The {@link NewsBinWizard} is only used in the Eclipse Integration to create
 * new Bins.
 *
 * @author bpasero
 */
public class NewsBinWizard extends Wizard implements INewWizard {

  /* Page for Wizard */
  private class NewNewsBinWizardPage extends WizardPage {

    NewNewsBinWizardPage(String pageName) {
      super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/newsbin_wiz.gif")); //$NON-NLS-1$
      setMessage(Messages.NewsBinWizard_BIN_WIZ_TITLE);
    }

    public void createControl(Composite parent) {
      Composite control = new Composite(parent, SWT.NONE);
      control.setLayout(new GridLayout(2, false));

      setControl(control);
    }
  }

  private NewNewsBinWizardPage fPage;

  /** Leave for Reflection */
  public NewsBinWizard() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) {}

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    fPage = new NewNewsBinWizardPage(Messages.NewsBinWizard_NEW_NEWS_BIN);
    setWindowTitle(Messages.NewsBinWizard_NEWS_BIN);
    setHelpAvailable(false);
    addPage(fPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {
    return false;
  }
}