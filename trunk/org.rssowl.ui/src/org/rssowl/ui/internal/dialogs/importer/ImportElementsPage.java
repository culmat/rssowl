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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
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
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.importer.ImportSourcePage.Source;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;
import org.rssowl.ui.internal.views.explorer.BookMarkSorter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
  private Button fDeselectAll;
  private Button fSelectAll;
  private Button fHideExisting;
  private ExistingBookmarkFilter fExistingFilter = new ExistingBookmarkFilter();
  private Source fLastSourceKind;
  private File fLastSourceFile;

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

    private boolean select(IFolderChild element) {

      /* Bookmark */
      if (element instanceof IBookMark) {
        IBookMark bm = (IBookMark) element;
        Boolean select = cache.get(bm.getId());
        if (select == null) {
          select = !dao.exists(bm.getFeedLinkReference());
          cache.put(bm, select);
        }

        return select;
      }

      /* Folder */
      else if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;
        Boolean select = cache.get(folder.getId());
        if (select == null) {
          List<IFolderChild> children = folder.getChildren();
          for (IFolderChild child : children) {
            select = select(child);
            if (select)
              break;
          }

          cache.put(folder, select);
        }

        return select;
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

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Viewer to select particular Folders/Marks */
    fViewer = new CheckboxTreeViewer(container, SWT.BORDER);
    fViewer.setAutoExpandLevel(2);
    fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((GridData) fViewer.getTree().getLayoutData()).heightHint = 190;
    fViewer.getTree().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

    /* Sort by Name if set so */
    if (Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.BE_SORT_BY_NAME)) {
      BookMarkSorter sorter = new BookMarkSorter();
      sorter.setType(BookMarkSorter.Type.SORT_BY_NAME);
      fViewer.setComparator(sorter);
    }

    /* ContentProvider */
    fViewer.setContentProvider(new ITreeContentProvider() {
      public Object[] getElements(Object inputElement) {
        return ((Collection<?>) inputElement).toArray();
      }

      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IFolder) {
          IFolder folder = (IFolder) parentElement;
          return folder.getChildren().toArray();
        }

        return new Object[0];
      }

      public Object getParent(Object element) {
        if (element instanceof IFolder) {
          IFolder folder = (IFolder) element;
          return folder.getParent();
        }

        return null;
      }

      public boolean hasChildren(Object element) {
        if (element instanceof IFolder) {
          IFolder folder = (IFolder) element;
          return !folder.isEmpty();
        }

        return false;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* LabelProvider */
    fViewer.setLabelProvider(new BookMarkLabelProvider(false));

    fViewer.addFilter(fExistingFilter);

    /* Listen on Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        IFolder folder = selection.getFirstElement() instanceof IFolder ? (IFolder) selection.getFirstElement() : null;

        /* Expand / Collapse Folder */
        if (folder != null && !folder.isEmpty()) {
          boolean expandedState = !fViewer.getExpandedState(folder);
          fViewer.setExpandedState(folder, expandedState);

          if (expandedState && fViewer.getChecked(folder))
            setChildsChecked(folder, true);
        }
      }
    });

    /* Update Checks on Selection */
    fViewer.getTree().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          TreeItem item = (TreeItem) e.item;
          setChildsChecked((IFolderChild) item.getData(), item.getChecked());

          if (!item.getChecked())
            setParentsChecked((IFolderChild) item.getData(), false);
        }
      }
    });

    /* Update Checks on Expand */
    fViewer.addTreeListener(new ITreeViewerListener() {
      public void treeExpanded(TreeExpansionEvent event) {
        boolean isChecked = fViewer.getChecked(event.getElement());
        if (isChecked)
          setChildsChecked((IFolderChild) event.getElement(), isChecked);
      }

      public void treeCollapsed(TreeExpansionEvent event) {}
    });

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        OwlUI.setAllChecked(fViewer.getTree(), true);
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        OwlUI.setAllChecked(fViewer.getTree(), false);
      }
    });

    fHideExisting = new Button(buttonContainer, SWT.CHECK);
    fHideExisting.setText("Hide Existing Bookmarks");
    fHideExisting.setSelection(true);
    setButtonLayoutData(fHideExisting);
    ((GridData) fHideExisting.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) fHideExisting.getLayoutData()).grabExcessHorizontalSpace = true;
    fHideExisting.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fHideExisting.getSelection())
          fViewer.addFilter(fExistingFilter);
        else
          fViewer.removeFilter(fExistingFilter);

        fViewer.expandToLevel(2);
        updateMessage();
      }
    });

    setControl(container);
  }

  private void setChildsChecked(IFolderChild folderChild, boolean checked) {
    if (folderChild instanceof IFolder) {
      List<IFolderChild> children = ((IFolder) folderChild).getChildren();
      for (IFolderChild child : children) {
        fViewer.setChecked(child, checked);
        setChildsChecked(child, checked);
      }
    }
  }

  private void setParentsChecked(IFolderChild folderChild, boolean checked) {
    IFolder parent = folderChild.getParent();
    if (parent != null) {
      fViewer.setChecked(parent, checked);
      setParentsChecked(parent, checked);
    }
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
    else if (source == Source.FILE && importSourcePage.getImportFile().equals(fLastSourceFile))
      return;

    fLastSourceKind = source;
    fLastSourceFile = importSourcePage.getImportFile();

    InputStream in = null;

    /* Import from Supplied File */
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
    }

    /* Re-Add Filter if necessary */
    if (!fHideExisting.getSelection()) {
      fHideExisting.setSelection(true);
      fViewer.addFilter(fExistingFilter);
    }

    /* Apply as Input */
    fViewer.setInput(folderChilds);
    OwlUI.setAllChecked(fViewer.getTree(), true);
    updateMessage();
  }

  private void updateMessage() {
    List<?> input = (List<?>) fViewer.getInput();
    if (!input.isEmpty() && fViewer.getTree().getItemCount() == 0)
      setMessage("Some elemens are hidden because they already exist.", IMessageProvider.WARNING);
    else
      setMessage("Please choose the elements to import.");
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return true;
  }
}