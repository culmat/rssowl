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
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.dialogs.importer.ImportSourcePage.Source;
import org.rssowl.ui.internal.dialogs.welcome.WelcomeWizard;
import org.rssowl.ui.internal.editors.feed.NewsGrouping;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A {@link Wizard} to import bookmarks, saved searches and bins with the option
 * to also import settings (Labels, Filters, Properties) from OPML. Supports
 * importing from local resources as well as remote.
 *
 * @author bpasero
 */
public class ImportWizard extends Wizard {
  private ImportSourcePage fImportSourcePage;
  private ImportElementsPage fImportElementsPage;
  private ImportTargetPage fImportTargetPage;
  private ImportOptionsPage fImportOptionsPage;
  private String fWebsite;
  private boolean fIsKewordSearch;
  private boolean fIsWelcome;

  /** Default Constructor */
  public ImportWizard() {
    this(null, false);
  }

  /**
   * @param isWelcome if <code>true</code>, this wizard is used from the
   * {@link WelcomeWizard}.
   */
  public ImportWizard(boolean isWelcome) {
    this(null, false);
    fIsWelcome = isWelcome;
  }

  /**
   * @param website a link to a website to discover feeds on.
   * @param isKeywordSearch defines if the keyword search should be selected or
   * not.
   */
  public ImportWizard(String website, boolean isKeywordSearch) {
    fWebsite = website;
    fIsKewordSearch = isKeywordSearch;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle("Import");

    /* Page 1: Source to Import */
    fImportSourcePage = new ImportSourcePage(fWebsite, fIsKewordSearch);
    addPage(fImportSourcePage);

    /* Page 2: Elements to Import */
    fImportElementsPage = new ImportElementsPage();
    addPage(fImportElementsPage);

    /* Page 3: Target to Import */
    if (!fIsWelcome) {
      fImportTargetPage = new ImportTargetPage();
      addPage(fImportTargetPage);
    }

    /* Page 4: Import Options */
    fImportOptionsPage = new ImportOptionsPage();
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
    else if (page instanceof ImportElementsPage && !fImportElementsPage.showOptionsPage() && fIsWelcome)
      return null;

    return super.getNextPage(page);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {

    /* Directly Return if nothing to import */
    if (fImportSourcePage.getSource() == Source.NONE)
      return true;

    /* Perform Import */
    return doImport();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {

    /* Prohibit direct Finish from Sources that require a remote connection or include recommended feeds */
    if (getContainer().getCurrentPage() == fImportSourcePage) {
      if (fImportSourcePage.isRemoteSource() || (fImportSourcePage.getSource() == Source.RECOMMENDED && !fIsWelcome))
        return false;
    }

    /* Allow to Finish if nothing should be imported */
    if (fImportSourcePage.getSource() == Source.NONE)
      return true;

    /* Other Pages decide on their own */
    return super.canFinish();
  }

  private boolean doImport() {

    /* Collect Elements to Import */
    List<IFolderChild> folderChilds = fImportElementsPage.getFolderChildsToImport();
    boolean isRSSOwlOPML = isRSSOwlOPMLImport(folderChilds);
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
    IFolder target = fIsWelcome ? null : fImportTargetPage.getTargetLocation();

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
    ImportUtils.doImport(target, folderChilds, labels, filters, preferences, !fIsWelcome);

    /* Add Default Saved Searches if this is from Welcome Wizard */
    if (fIsWelcome && !isRSSOwlOPML && !importLabels && !importFilters && !importPreferences)
      addDefaultSearches();

    /* Save Settings of Pages */
    fImportSourcePage.saveSettings();

    /* Ask for a restart if preferences have been imported */
    if (importPreferences && preferences != null && !preferences.isEmpty()) {
      boolean restart = MessageDialog.openQuestion(getShell(), "Restart RSSOwl", "It is necessary to restart RSSOwl after preferences have been imported.\n\nDo you want to restart now?");
      if (restart) {
        BookMarkExplorer explorer = OwlUI.getOpenedBookMarkExplorer();
        if (explorer != null)
          explorer.saveStateOnDispose(false);

        PlatformUI.getWorkbench().restart();
        return true;
      }
    }

    /* Reveal and Select Target Folder */
    if (target != null && target.getParent() != null) {
      BookMarkExplorer explorer = OwlUI.getOpenedBookMarkExplorer();
      if (explorer != null)
        explorer.reveal(target, true);
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

  private boolean isRSSOwlOPMLImport(List<IFolderChild> folderChilds) {
    for (IFolderChild child : folderChilds) {
      if (child instanceof IFolder && child.getParent() == null) {
        IFolder set = (IFolder) child;
        if (set.getProperty(ITypeImporter.TEMPORARY_FOLDER) == null)
          return true;
      }
    }

    return false;
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

  private void addDefaultSearches() {
    Set<IFolder> roots = CoreUtils.loadRootFolders();
    if (roots.isEmpty())
      return;

    IFolder root = roots.iterator().next();

    IModelFactory factory = Owl.getModelFactory();
    String newsEntityName = INews.class.getName();

    /* SearchCondition: New and Updated News */
    {
      ISearchMark mark = factory.createSearchMark(null, root, "New and Updated News");
      mark.setMatchAllConditions(true);

      ISearchField field1 = factory.createSearchField(INews.STATE, newsEntityName);
      factory.createSearchCondition(null, mark, field1, SearchSpecifier.IS, EnumSet.of(INews.State.NEW, INews.State.UPDATED));
    }

    /* SearchCondition: Recent News */
    {
      ISearchMark mark = factory.createSearchMark(null, root, "Recent News");
      mark.setMatchAllConditions(true);

      ISearchField field1 = factory.createSearchField(INews.AGE_IN_DAYS, newsEntityName);
      factory.createSearchCondition(null, mark, field1, SearchSpecifier.IS_LESS_THAN, 2);
    }

    /* SearchCondition: News with Attachments */
    {
      ISearchMark mark = factory.createSearchMark(null, root, "News with Attachments");
      mark.setMatchAllConditions(true);

      ISearchField field = factory.createSearchField(INews.HAS_ATTACHMENTS, newsEntityName);
      factory.createSearchCondition(null, mark, field, SearchSpecifier.IS, true);
    }

    /* SearchCondition: Sticky News */
    {
      ISearchMark mark = factory.createSearchMark(null, root, "Sticky News");
      mark.setMatchAllConditions(true);

      ISearchField field = factory.createSearchField(INews.IS_FLAGGED, newsEntityName);
      factory.createSearchCondition(null, mark, field, SearchSpecifier.IS, true);
    }

    /* SearchCondition: News is Labeld */
    {
      ISearchMark mark = factory.createSearchMark(null, root, "Labeled News");
      IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(mark);
      preferences.putInteger(DefaultPreferences.BM_NEWS_GROUPING, NewsGrouping.Type.GROUP_BY_LABEL.ordinal());

      ISearchField field = factory.createSearchField(INews.LABEL, newsEntityName);
      factory.createSearchCondition(null, mark, field, SearchSpecifier.IS, "*");
    }

    DynamicDAO.save(root);
  }
}