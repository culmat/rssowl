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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.util.Iterator;
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

    /* Check if the ImportTargetPage needs to be shown */
    if (page instanceof ImportElementsPage && !fImportSourcePage.isRemoteSource() && fImportElementsPage.getFolderChildsToImport().isEmpty() && fImportElementsPage.showOptionsPage())
      return fImportOptionsPage;

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
    return doImport();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {

    /* Prohibit direct Finish from Sources that require a remote connection */
    if (getContainer().getCurrentPage() == fImportSourcePage && fImportSourcePage.isRemoteSource())
      return false;

    /* Other Pages decide on their own */
    return super.canFinish();
  }

  private boolean doImport() {

    /* Collect Elements to Import */
    List<IFolderChild> folderChilds = fImportElementsPage.getFolderChildsToImport();
    if (fImportElementsPage.excludeExisting())
      folderChilds = excludeExisting(folderChilds);

    /* Check for Errors First */
    if (StringUtils.isSet(fImportElementsPage.getErrorMessage())) {
      getContainer().showPage(fImportElementsPage);
      return false;
    }

    List<ILabel> labels = fImportElementsPage.getLabelsToImport();
    List<ISearchFilter> filters = fImportElementsPage.getFiltersToImport();
    List<IPreference> preferences = fImportElementsPage.getPreferencesToImport();

    /* Normalize Elements to Import */
    CoreUtils.normalize(folderChilds);

    /* Get Target Location (may be null) */
    IFolder target = fImportTargetPage.getTargetLocation();

    /* Get Options */
    boolean importLabels = fImportOptionsPage.importLabels();
    if (!importLabels)
      labels = null;

    boolean importFilters = fImportOptionsPage.importFilters();
    if (!importFilters)
      filters = null;

    boolean importPreferences = fImportOptionsPage.importPreferences();
    if (!importPreferences)
      preferences = null;

    /* Show warning and ask for confirmation if preferences should be imported */
    if (importPreferences && preferences != null && !preferences.isEmpty()) {
      MessageDialog dialog = new MessageDialog(getShell(), "Attention", null, "All of your existing preferences will be replaced with the imported ones.\n\nDo you want to continue?", MessageDialog.WARNING, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
      if (dialog.open() != 0)
        return false;
    }

    /* Run Import */
    ImportUtils.doImport(target, folderChilds, labels, filters, preferences);

    /* Save Settings of Pages */
    fImportSourcePage.saveSettings();

    /* Ask for a restart if preferences have been imported */
    if (importPreferences && preferences != null && !preferences.isEmpty()) {
      boolean restart = MessageDialog.openQuestion(getShell(), "Restart RSSOwl", "It is necessary to restart RSSOwl after preferences have been imported.\n\nDo you want to restart now?");
      if (restart) {
        BookMarkExplorer explorer = OwlUI.getOpenBookMarkExplorer();
        if (explorer != null)
          explorer.saveStateOnDispose(false);

        PlatformUI.getWorkbench().restart();
        return true;
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

    return true;
  }

  /* Remove existing Bookmarks and Empty Folders */
  private List<IFolderChild> excludeExisting(List<IFolderChild> folderChilds) {
    IBookMarkDAO dao = DynamicDAO.getDAO(IBookMarkDAO.class);

    for (Iterator<IFolderChild> iterator = folderChilds.iterator(); iterator.hasNext();) {
      IFolderChild child = iterator.next();

      /* Bookmark (exclude if another Bookmark with same Link exists) */
      if (child instanceof IBookMark) {
        IBookMark bm = (IBookMark) child;
        if (dao.exists(bm.getFeedLinkReference()))
          iterator.remove();
      }

      /* Bin (exclude if another Bin with same name Exists at same Location) */
      else if (child instanceof INewsBin) {
        INewsBin bin = (INewsBin) child;
        if (CoreUtils.existsNewsBin(bin))
          iterator.remove();
      }

      /* Search (exclude if another Search with same name Exists at same Location and same Conditions) */
      else if (child instanceof ISearchMark) {
        ISearchMark search = (ISearchMark) child;
        if (CoreUtils.existsSearchMark(search))
          iterator.remove();
      }

      /* Folder */
      else if (child instanceof IFolder) {
        excludeExisting((IFolder) child);
      }
    }

    /* Exclude Empty Folders */
    for (Iterator<IFolderChild> iterator = folderChilds.iterator(); iterator.hasNext();) {
      IFolderChild child = iterator.next();
      if (child instanceof IFolder && ((IFolder) child).getChildren().isEmpty())
        iterator.remove();
    }

    return folderChilds;
  }

  /* Remove existing Bookmarks and Empty Folders */
  private void excludeExisting(IFolder folder) {
    IBookMarkDAO dao = DynamicDAO.getDAO(IBookMarkDAO.class);
    List<IFolderChild> children = folder.getChildren();

    for (IFolderChild child : children) {

      /* Bookmark (exclude if another Bookmark with same Link exists) */
      if (child instanceof IBookMark) {
        IBookMark bm = (IBookMark) child;
        if (dao.exists(bm.getFeedLinkReference()))
          folder.removeChild(bm);
      }

      /* Bin (exclude if another Bin with same name Exists at same Location) */
      else if (child instanceof INewsBin) {
        INewsBin bin = (INewsBin) child;
        if (CoreUtils.existsNewsBin(bin))
          folder.removeChild(bin);
      }

      /* Search (exclude if another Search with same name Exists at same Location and same Conditions) */
      else if (child instanceof ISearchMark) {
        ISearchMark search = (ISearchMark) child;
        if (CoreUtils.existsSearchMark(search))
          folder.removeChild(search);
      }

      /* Folder */
      else if (child instanceof IFolder) {
        excludeExisting((IFolder) child);
      }
    }

    /* Remove from Parent if Empty Now */
    if (folder.getChildren().isEmpty() && folder.getParent() != null)
      folder.getParent().removeChild(folder);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return true;
  }
}