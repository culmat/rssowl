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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.ui.internal.OwlUI;

class WelcomePage extends WizardPage {

  /**
   * @param pageName
   */
  protected WelcomePage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("This wizard will guide you through the steps of the Clean Up process.");
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new GridLayout(3, false));

    Label welcomeLabel = new Label(control, SWT.None);
    welcomeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    StringBuilder str = new StringBuilder();
    str.append("The clean up process will help you make the best out of RSSOwl. When the wizard is done it will support:");
    str.append("\n\n - Delete Read News");
    str.append("\n - Delete News from Feeds when count > N");
    str.append("\n - Delete News from Feeds when age > N days");
    str.append("\n\n - Delete Feeds & Bookmarks that haven't updated for a while");
    str.append("\n - Delete Feeds & Bookmarks that the user did not visit for a while");

    str.append("\n\n - Defragment the db4o DB");
    str.append("\n - Optimize the Lucene Index");

    str.append("\n\nPlease press next to begin the clean up process...");

    welcomeLabel.setText(str.toString());

    setControl(control);
  }
}