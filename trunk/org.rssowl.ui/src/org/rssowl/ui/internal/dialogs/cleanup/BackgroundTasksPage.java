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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.rssowl.ui.internal.OwlUI;

/**
 * @author bpasero
 */
public class BackgroundTasksPage extends WizardPage {
  private Button fDefragCheck;
  private Button fOptimizeLucene;

  /**
   * @param pageName
   */
  protected BackgroundTasksPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("This wizard will guide you through the steps of the Clean Up process.");
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite control = new Composite(parent, SWT.NONE);
    control.setLayout(new GridLayout(1, false));

    /* Defragment Database */
    fDefragCheck = new Button(control, SWT.CHECK);
    fDefragCheck.setText("Defragment the database to make RSSOwl faster");
    fDefragCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fDefragCheck.getSelection())
          setMessage("Defragment will only take place after the next restart of RSSOwl", WARNING);
        else
          setMessage("This wizard will guide you through the steps of the Clean Up process.");
      }
    });

    /* Optimize Lucene */
    fOptimizeLucene = new Button(control, SWT.CHECK);
    fOptimizeLucene.setText("Optimize the search index to make searches faster");

    setControl(control);
  }

  boolean defragDatabase() {
    return fDefragCheck.getSelection();
  }

  boolean optimizeLucene() {
    return fOptimizeLucene.getSelection();
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return isCurrentPage();
  }
}