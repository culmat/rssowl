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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.ui.internal.Activator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author bpasero
 */
public class CleanUpWizard extends Wizard {
  private BackgroundTasksPage fBackgroundPage;
  private IWizardPage fWelcomePage;

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle("Clean Up");
    setHelpAvailable(false);

    /* Welcome */
    fWelcomePage = new WelcomePage("Welcome to the Clean Up Wizard");
    addPage(fWelcomePage);

    /* Background Tasks */
    fBackgroundPage = new BackgroundTasksPage("Background Tasks");
    addPage(fBackgroundPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @SuppressWarnings("restriction")
  @Override
  public boolean performFinish() {

    /* Optimize Lucene */
    if (fBackgroundPage.optimizeLucene()) {
      try {
        getContainer().run(true, false, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor) {
            try {
              monitor.beginTask("Optimizing the search index...", IProgressMonitor.UNKNOWN);
              Owl.getPersistenceService().getModelSearch().optimize();
            } catch (PersistenceException e) {
              Activator.getDefault().logError(e.getMessage(), e);
            }
          }
        });
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    /* Defragment Database */
    if (fBackgroundPage.defragDatabase()) {
      String stateLocation = org.rssowl.core.internal.Activator.getDefault().getStateLocation().toOSString();
      try {
        new File(stateLocation, "defragment").createNewFile();
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return true;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return true;
  }
}