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

package org.rssowl.ui.internal.dialogs.importer;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.FileDialog;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * A {@link Wizard} to import bookmarks, saved searches and bins with the option
 * to also import settings (Labels, Filters, Properties) from OPML.
 *
 * @author bpasero
 */
public class ImportWizard extends Wizard {
  private ImportSourcePage fImportSourcePage;
  private ImportElementsPage fImportElementsPage;
  private ImportTargetPage fImportTargetPage;
  private ImportOptionsPage fImportOptionsPage;

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle("Import");

    /* Page 1: Source to Import */
    fImportSourcePage = new ImportSourcePage("Choose Source");
    addPage(fImportSourcePage);

    /* Page 2: Elements to Import */
    fImportElementsPage = new ImportElementsPage("Choose Elements");
    addPage(fImportElementsPage);

    /* Page 3: Target to Import */
    fImportTargetPage = new ImportTargetPage("Choose Target Location");
    addPage(fImportTargetPage);

    /* Page 4: Import Options */
    fImportOptionsPage = new ImportOptionsPage("Import Options");
    addPage(fImportOptionsPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
   */
  @Override
  public IWizardPage getNextPage(IWizardPage page) {

    /* Check if the ImportOptionsPage needs to be shown */
    if (page instanceof ImportTargetPage && !fImportElementsPage.showOptionsPage())
      return null;

    return super.getNextPage(page);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {
    //TODO Properly handle this method if called from first page
    //TODO Save Entities, Save Global/Eclipse Scope, Flush Eclipse Scope
    //TODO Properly save feeds for bookmarks if necessary
    //    IFeedDAO feedDao = Owl.getPersistenceService().getDAOService().getFeedDAO();
    //    FeedReference feedRef = feedDao.loadReference(uri);
    //
    //    /* Create a new Feed then */
    //    if (feedRef == null) {
    //      IFeed feed = Owl.getModelFactory().createFeed(null, uri);
    //      feed.setHomepage(homepage != null ? URIUtils.createURI(homepage) : null);
    //      feed.setDescription(description);
    //      feed = feedDao.save(feed);
    //    }

    FileDialog dialog = new FileDialog(getShell());
    dialog.setText("Import Feeds from OPML");
    dialog.setFilterExtensions(new String[] { "*.opml", "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    String string = dialog.open();
    if (string != null) {
      try {
        ImportUtils.importFeeds(string);
      } catch (Exception e) {
        Activator.getDefault().logError(e.getMessage(), e);
        ErrorDialog.openError(getShell(), "Error importing Feeds", "RSSOwl was unable to import '" + string + "'", Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }

      /* Force to rerun saved searches */
      JobRunner.runDelayedInBackgroundThread(new Runnable() {
        public void run() {
          Controller.getDefault().getSavedSearchService().updateSavedSearches(true);
        }
      });
    }

    return true;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return false;
  }
}