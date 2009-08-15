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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.rssowl.core.Owl;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
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
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.importer.ImportSourcePage.Source;
import org.rssowl.ui.internal.util.FolderChildCheckboxTree;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 *A {@link WizardPage} to select the elements to import.
 *
 * @author bpasero
 */
public class ImportElementsPage extends WizardPage {
  private CheckboxTreeViewer fViewer;
  private FolderChildCheckboxTree fFolderChildTree;
  private Button fDeselectAll;
  private Button fSelectAll;
  private Button fFlattenCheck;
  private Button fHideExistingCheck;
  private ExistingBookmarkFilter fExistingFilter = new ExistingBookmarkFilter();
  private Source fLastSourceKind;
  private File fLastSourceFile;
  private long fLastSourceFileModified;
  private List<ILabel> fLabels = new ArrayList<ILabel>();
  private List<ISearchFilter> fFilters = new ArrayList<ISearchFilter>();
  private List<IPreference> fPreferences = new ArrayList<IPreference>();

  /* Filter to Exclude Existing Bookmarks (empty folders are excluded as well) */
  private static class ExistingBookmarkFilter extends ViewerFilter {
    private IBookMarkDAO dao = DynamicDAO.getDAO(IBookMarkDAO.class);
    private Map<IFolderChild, Boolean> cache = new IdentityHashMap<IFolderChild, Boolean>();

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (element instanceof IFolderChild)
        return select((IFolderChild) element);

      return true;
    }

    void clear() {
      cache.clear();
    }

    private boolean select(IFolderChild element) {

      /* Bookmark (exclude if another Bookmark with same Link exists) */
      if (element instanceof IBookMark) {
        IBookMark bm = (IBookMark) element;
        Boolean select = cache.get(bm);
        if (select == null) {
          select = !dao.exists(bm.getFeedLinkReference());
          cache.put(bm, select);
        }

        return select;
      }

      /* Bin (exclude if another Bin with same name Exists at same Location) */
      else if (element instanceof INewsBin) {
        INewsBin bin = (INewsBin) element;
        Boolean select = cache.get(bin);
        if (select == null) {
          select = !CoreUtils.existsNewsBin(bin);
          cache.put(bin, select);
        }

        return select;
      }

      /* Search (exclude if another Search with same name Exists at same Location and same Conditions) */
      else if (element instanceof ISearchMark) {
        ISearchMark searchmark = (ISearchMark) element;
        Boolean select = cache.get(searchmark);
        if (select == null) {
          select = !CoreUtils.existsSearchMark(searchmark);
          cache.put(searchmark, select);
        }

        return select;
      }

      /* Folder */
      else if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;
        Boolean select = cache.get(folder);
        if (select == null) {
          List<IFolderChild> children = folder.getChildren();
          for (IFolderChild child : children) {
            select = select(child);
            if (select)
              break;
          }

          cache.put(folder, select);
        }

        return select != null ? select : false;
      }

      return true;
    }
  }

  /**
   * @param pageName
   */
  protected ImportElementsPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please choose the elements to import.");
  }

  /* Get Elements to Import */
  List<IFolderChild> getFolderChildsToImport() {
    importSource(); //Ensure to be in sync with Source

    return fFolderChildTree.getCheckedElements();
  }

  /* Returns whether existing bookmarks should be ignored for the Import */
  boolean excludeExisting() {
    return fHideExistingCheck.getSelection();
  }

  /* Returns Labels available for Import */
  List<ILabel> getLabelsToImport() {
    importSource(); //Ensure to be in sync with Source
    return fLabels;
  }

  /* Returns Filters available for Import */
  List<ISearchFilter> getFiltersToImport() {
    importSource(); //Ensure to be in sync with Source
    return fFilters;
  }

  /* Returns the Preferences available for Import */
  List<IPreference> getPreferencesToImport() {
    importSource(); //Ensure to be in sync with Source
    return fPreferences;
  }

  /* Check if the Options Page should be shown from the Wizard */
  boolean showOptionsPage() {
    return !fLabels.isEmpty() || !fFilters.isEmpty() || !fPreferences.isEmpty();
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Viewer for Folder Child Selection */
    fFolderChildTree = new FolderChildCheckboxTree(container);
    fViewer = fFolderChildTree.getViewer();

    /* Filter (exclude existing) */
    fViewer.addFilter(fExistingFilter);

    /* Update Page Complete on Selection */
    fViewer.getTree().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
      }
    });

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(4, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setAllChecked(true);
        updatePageComplete();
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setAllChecked(false);
        updatePageComplete();
      }
    });

    /* Show as Flat List of News Marks */
    fFlattenCheck = new Button(buttonContainer, SWT.CHECK);
    fFlattenCheck.setText("Flatten Hierarchy");
    setButtonLayoutData(fFlattenCheck);
    ((GridData) fFlattenCheck.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) fFlattenCheck.getLayoutData()).grabExcessHorizontalSpace = true;
    fFlattenCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setFlat(fFlattenCheck.getSelection());
        fViewer.expandToLevel(2);
      }
    });

    /* Hide Existing News Marks */
    fHideExistingCheck = new Button(buttonContainer, SWT.CHECK);
    fHideExistingCheck.setText("Hide Existing");
    fHideExistingCheck.setSelection(true);
    setButtonLayoutData(fHideExistingCheck);
    fHideExistingCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fHideExistingCheck.getSelection())
          fViewer.addFilter(fExistingFilter);
        else
          fViewer.removeFilter(fExistingFilter);

        fViewer.expandToLevel(2);
        updateMessage(false);
      }
    });

    setControl(container);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {

    /* Load Elements to Import from Source on first time */
    if (visible)
      importSource();

    super.setVisible(visible);
    fViewer.getControl().setFocus();
  }

  private void importSource() {
    try {
      doImportSource();
    } catch (InterpreterException e) {
      Activator.getDefault().logError(e.getMessage(), e);
      setErrorMessage(e.getMessage());
    } catch (ParserException e) {
      Activator.getDefault().logError(e.getMessage(), e);
      setErrorMessage(e.getMessage());
    } catch (FileNotFoundException e) {
      Activator.getDefault().logError(e.getMessage(), e);
      setErrorMessage(e.getMessage());
    }
  }

  private void doImportSource() throws InterpreterException, ParserException, FileNotFoundException {
    ImportSourcePage importSourcePage = (ImportSourcePage) getPreviousPage();
    Source source = importSourcePage.getSource();

    /* Return if the Source did not Change */
    if (source == Source.DEFAULT && fLastSourceKind == Source.DEFAULT)
      return;
    else if (source == Source.FILE && importSourcePage.getImportFile().equals(fLastSourceFile) && importSourcePage.getImportFile().lastModified() == fLastSourceFileModified)
      return;

    /* Remember Source */
    fLastSourceKind = source;
    fLastSourceFile = importSourcePage.getImportFile();
    fLastSourceFileModified = fLastSourceFile != null ? fLastSourceFile.lastModified() : 0;

    /* Reset Fields */
    fLabels.clear();
    fFilters.clear();
    fPreferences.clear();

    /* Import from Supplied File */
    InputStream in = null;
    if (source == Source.FILE) {
      File fileToImport = importSourcePage.getImportFile();
      in = new FileInputStream(fileToImport);
    }

    /* Import from Default OPML File */
    else if (source == Source.DEFAULT) {
      in = getClass().getResourceAsStream("/default_feeds.xml"); //$NON-NLS-1$;
    }

    /* Show Folder Childs in Viewer */
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(in);
    List<IFolderChild> folderChilds = new ArrayList<IFolderChild>();
    for (IEntity type : types) {
      if (type instanceof IFolderChild)
        folderChilds.add((IFolderChild) type);
      else if (type instanceof ILabel)
        fLabels.add((ILabel) type);
      else if (type instanceof ISearchFilter)
        fFilters.add((ISearchFilter) type);
      else if (type instanceof IPreference)
        fPreferences.add((IPreference) type);
    }

    /* Re-Add Filter if necessary */
    if (!fHideExistingCheck.getSelection()) {
      fHideExistingCheck.setSelection(true);
      fViewer.addFilter(fExistingFilter);
    }

    /* Apply as Input */
    fViewer.setInput(folderChilds);
    OwlUI.setAllChecked(fViewer.getTree(), true);
    fExistingFilter.clear();
    updateMessage(true);
  }

  private void updateMessage(boolean clearErrors) {
    List<?> input = (List<?>) fViewer.getInput();
    if (!input.isEmpty() && fViewer.getTree().getItemCount() == 0)
      setMessage("Some elemens are hidden because they already exist.", IMessageProvider.WARNING);
    else
      setMessage("Please choose the elements to import.");

    if (clearErrors)
      setErrorMessage(null);

    updatePageComplete();
  }

  private void updatePageComplete() {
    boolean complete = (showOptionsPage() || fViewer.getCheckedElements().length > 0);
    setPageComplete(complete);
  }
}