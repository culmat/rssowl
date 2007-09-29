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
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;
import org.rssowl.ui.internal.views.explorer.BookMarkSorter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class FeedSelectionPage extends WizardPage {
  private CheckboxTreeViewer fViewer;
  private Button fSelectAll;
  private Button fDeselectAll;

  /**
   * @param pageName
   */
  protected FeedSelectionPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("Please choose the Bookmarks you want to clean up.");
  }

  /* Returns all selected Bookmarks */
  Set<IBookMark> getSelection() {
    Set<IBookMark> selection = new HashSet<IBookMark>();

    Object[] checkedElements = fViewer.getCheckedElements();
    for (Object checkedElement : checkedElements) {

      /* Folder */
      if (checkedElement instanceof IFolder)
        addAll(selection, (IFolder) checkedElement);

      /* Bookmark */
      else if (checkedElement instanceof IBookMark)
        selection.add((IBookMark) checkedElement);
    }

    return selection;
  }

  private void addAll(Set<IBookMark> bookmarks, IFolder folder) {

    /* Child Folders */
    List<IFolder> childFolders = folder.getFolders();
    for (IFolder childFolder : childFolders) {
      addAll(bookmarks, childFolder);
    }

    /* Bookmarks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark)
        bookmarks.add((IBookMark) mark);
    }
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
        Set<IFolder> rootFolders = Controller.getDefault().getCacheService().getRootFolders();
        return rootFolders.toArray();
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
          return !folder.getChildren().isEmpty();
        }

        return false;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* LabelProvider */
    fViewer.setLabelProvider(new BookMarkLabelProvider(false));

    /* Filter out any Search Marks */
    fViewer.addFilter(new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        return !(element instanceof ISearchMark);
      }
    });

    /* Listen on Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        IFolder folder = selection.getFirstElement() instanceof IFolder ? (IFolder) selection.getFirstElement() : null;

        /* Expand / Collapse Folder */
        if (folder != null && !folder.getChildren().isEmpty()) {
          boolean expandedState = !fViewer.getExpandedState(folder);
          fViewer.setExpandedState(folder, expandedState);

          if (expandedState && fViewer.getChecked(folder))
            setChildsChecked(folder, true, true);
        }
      }
    });

    /* Dummy Input */
    fViewer.setInput(new Object());
    fViewer.setAllChecked(true);

    /* Set Checked Elements */
    Set<IFolder> rootFolders = Controller.getDefault().getCacheService().getRootFolders();
    for (IFolder folder : rootFolders) {
      setCheckedElements(folder, false);
    }

    /* Update Checks on Selection */
    fViewer.getTree().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          TreeItem item = (TreeItem) e.item;
          setChildsChecked((IFolderChild) item.getData(), item.getChecked(), true);

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
          setChildsChecked((IFolderChild) event.getElement(), isChecked, false);
      }

      public void treeCollapsed(TreeExpansionEvent event) {}
    });

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fViewer.setAllChecked(true);
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fViewer.setAllChecked(false);
      }
    });

    setControl(container);
  }

  private void setCheckedElements(IFolderChild entity, boolean parentChecked) {

    /* Check for Preference */
    IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(entity);
    if (prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER)) {
      if (!parentChecked) {
        setParentsExpanded(entity);
        parentChecked = true;
      }
      fViewer.setChecked(entity, true);
      setChildsChecked(entity, true, true);
    }

    /* Check for Childs */
    if (entity instanceof IFolder) {
      List<IFolderChild> children = ((IFolder) entity).getChildren();
      for (IFolderChild child : children) {
        setCheckedElements(child, parentChecked);
      }
    }
  }

  private void setParentsExpanded(IFolderChild folderChild) {
    IFolder parent = folderChild.getParent();
    if (parent != null) {
      fViewer.setExpandedState(parent, true);
      setParentsExpanded(parent);
    }
  }

  private void setChildsChecked(IFolderChild folderChild, boolean checked, boolean onlyExpanded) {
    if (folderChild instanceof IFolder && (!onlyExpanded || fViewer.getExpandedState(folderChild))) {
      List<IFolderChild> children = ((IFolder) folderChild).getChildren();
      for (IFolderChild child : children) {
        fViewer.setChecked(child, checked);
        setChildsChecked(child, checked, onlyExpanded);
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
}