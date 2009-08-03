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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.ArrayList;
import java.util.List;

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
    doImport();
    return true;
  }

  private void doImport() {

    /* Collect Elements to Import */
    List<IFolderChild> folderChilds = fImportElementsPage.getFolderChildsToImport();
    List<ILabel> labels = fImportElementsPage.getLabelsToImport();
    List<ISearchFilter> filters = fImportElementsPage.getFiltersToImport();
    List<IPreference> preferences = fImportElementsPage.getPreferencesToImport();

    /* Normalize Elements to Import */
    List<IFolder> folders = new ArrayList<IFolder>();
    for (IFolderChild child : folderChilds) {
      if (child instanceof IFolder)
        folders.add((IFolder) child);
    }

    for (IFolder folder : folders) {
      CoreUtils.normalize(folder, folderChilds);
    }

    /* Get Target Location (may be null) */
    IFolder target = fImportTargetPage.getTargetLocation();

    /* Get Options */
    boolean importLabels = fImportOptionsPage.importLabels();
    boolean importFilters = fImportOptionsPage.importFilters();
    boolean importPreferences = fImportOptionsPage.importPreferences();

    if (!importLabels)
      labels = null;

    if (!importFilters)
      filters = null;

    if (!importPreferences)
      preferences = null;

    /* Run Import */
    ImportUtils.doImport(target, folderChilds, labels, filters, preferences);

    /* Ask for a restart if preferences have been imported */
    if (importPreferences && preferences != null && !preferences.isEmpty()) {
      boolean restart = MessageDialog.openQuestion(getShell(), "Restart RSSOwl", "It is recommended to restart RSSOwl after preferences have been imported.\n\nDo you want to restart now?");
      if (restart) {
        PlatformUI.getWorkbench().restart();
        return;
      }
    }

    /* Reload Imported Elements */
    new ReloadTypesAction(new StructuredSelection(folderChilds), OwlUI.getPrimaryShell()).run();

    /* Force to rerun saved searches */
    JobRunner.runDelayedInBackgroundThread(new Runnable() {
      public void run() {
        Controller.getDefault().getSavedSearchService().updateSavedSearches(true);
      }
    });
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return false;
  }
}