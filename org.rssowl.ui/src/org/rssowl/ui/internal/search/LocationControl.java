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

package org.rssowl.ui.internal.search;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;
import org.rssowl.ui.internal.views.explorer.BookMarkSorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <code>LocationConditionControl</code> is a <code>Composite</code>
 * providing the UI to define Location-Conditions for a Search.
 * <p>
 * TODO This class is currently only working on INews.
 * </p>
 *
 * @author bpasero
 */
public class LocationControl extends Composite {
  private Mode fMode = Mode.SEARCH_LOCATION;
  private Link fConditionLabel;
  private List<IFolderChild> fSelection;

  /** Supported Modes for the Control */
  public enum Mode {

    /** Select a Folder Child to Search in */
    SEARCH_LOCATION,

    /** Select a News Bin */
    SELECT_BIN
  }

  /* A Dialog to select Folders and Childs */
  private class FolderChildChooserDialog extends Dialog {
    private CheckboxTreeViewer fViewer;
    private List<IFolderChild> fCheckedElements;
    private IFolderChild fSelectedElement;
    private Set<IFolderChild> fCheckedElementsCache = new HashSet<IFolderChild>();

    FolderChildChooserDialog(Shell parentShell, IFolderChild selectedElement, List<IFolderChild> checkedElements) {
      super(parentShell);
      fSelectedElement = selectedElement;
      fCheckedElements = checkedElements;
    }

    List<IFolderChild> getCheckedElements() {
      return fCheckedElements;
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
      Object[] checkedObjects = fCheckedElementsCache.toArray();
      IStructuredSelection selection = new StructuredSelection(checkedObjects);

      List<IFolderChild> entities = ModelUtils.getFoldersBookMarksBins(selection);
      List<IEntity> entitiesTmp = new ArrayList<IEntity>(entities);

      /* Normalize */
      for (IEntity entity : entitiesTmp) {
        if (entity instanceof IFolder)
          CoreUtils.normalize((IFolder) entity, entities);
      }

      fCheckedElements = entities;

      super.okPressed();
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(LayoutUtils.createGridLayout(1, 10, 10));
      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      Label label = new Label(composite, SWT.None);
      label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      switch (fMode) {
        case SEARCH_LOCATION:
          label.setText("Please choose the locations to search in:");
          break;

        case SELECT_BIN:
          label.setText("Please choose the news bins to copy or move news into:");
          break;
      }

      /* Filter for Filtered Tree */
      final PatternFilter filter = new PatternFilter() {
        @Override
        protected boolean isLeafMatch(Viewer viewer, Object element) {
          if (fMode == Mode.SELECT_BIN && !(element instanceof INewsBin))
            return false;

          String labelText = ((IFolderChild) element).getName();
          if (labelText == null)
            return false;

          return wordMatches(labelText);
        }
      };

      /* Filtered Tree to make it easier to chose an element */
      final FilteredTree filteredTree = new FilteredTree(composite, SWT.BORDER, filter) {
        @Override
        protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
          fViewer = new CheckboxTreeViewer(parent, SWT.BORDER) {
            @Override
            public void refresh(boolean updateLabels) {
              super.refresh(updateLabels);

              /* Avoid collapsed Tree */
              expandToLevel(fMode == Mode.SELECT_BIN ? AbstractTreeViewer.ALL_LEVELS : 2);

              /* Restore Checked Elements */
              for (IFolderChild child : fCheckedElementsCache) {
                setParentsExpanded(child);
                fViewer.setChecked(child, true);
                setChildsChecked(child, true, true, false);
              }
            }
          };
          fViewer.setAutoExpandLevel(fMode == Mode.SELECT_BIN ? AbstractTreeViewer.ALL_LEVELS : 2);
          fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
          fViewer.getTree().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());
          return fViewer;
        }

        @Override
        protected void updateToolbar(boolean visible) {
          filterToolBar.getControl().setEnabled(visible);
        }

        @Override
        protected Composite createFilterControls(Composite parent) {
          Composite filterControls = super.createFilterControls(parent);
          filterToolBar.getControl().setVisible(true);
          filterToolBar.getControl().setEnabled(false);
          return filterControls;
        }
      };

      filteredTree.setInitialText("");
      if (fMode == Mode.SEARCH_LOCATION)
        filteredTree.getFilterControl().setMessage("Type here to filter locations by name");
      else
        filteredTree.getFilterControl().setMessage("Type here to filter news bins by name");
      filteredTree.getViewer().getControl().setFocus();

      /* Filter when Typing into Tree */
      filteredTree.getViewer().getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.character > 0x20) {
            String character = String.valueOf(e.character);
            Text text = filteredTree.getFilterControl();
            text.setFocus();
            text.setText(character);
            text.setSelection(1);
            filter.setPattern(character);

            /* Consume the Event */
            e.doit = false;
          }
        }
      });

      int viewerHeight = fViewer.getTree().getItemHeight() * 20 + 12;
      ((GridData) composite.getLayoutData()).heightHint = viewerHeight;

      /* Sort by Name if set so */
      if (Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.BE_SORT_BY_NAME)) {
        BookMarkSorter sorter = new BookMarkSorter();
        sorter.setType(BookMarkSorter.Type.SORT_BY_NAME);
        fViewer.setComparator(sorter);
      }

      fViewer.setContentProvider(new ITreeContentProvider() {
        public Object[] getElements(Object inputElement) {
          Collection<IFolder> rootFolders = CoreUtils.loadRootFolders();
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
            return !folder.isEmpty();
          }

          return false;
        }

        public void dispose() {}

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
      });

      fViewer.setLabelProvider(new BookMarkLabelProvider(false) {
        @Override
        public void update(ViewerCell cell) {
          super.update(cell);

          if (fMode == Mode.SELECT_BIN) {
            Object element = cell.getElement();
            if (element instanceof IFolder)
              cell.setForeground(fViewer.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY));
          }
        }
      });

      /* Filter out any Search Marks */
      fViewer.addFilter(new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
          switch (fMode) {
            case SEARCH_LOCATION:
              return !(element instanceof ISearchMark);

            case SELECT_BIN:
              if (element instanceof IFolder)
                return containsBin(((IFolder) element).getChildren());
              return !(element instanceof ISearchMark || element instanceof IBookMark);
          }

          return true;
        }

        private boolean containsBin(List<IFolderChild> children) {
          for (IFolderChild child : children) {
            if (child instanceof INewsBin)
              return true;
            else if (child instanceof IFolder && containsBin((((IFolder) child).getChildren())))
              return true;
          }

          return false;
        }
      });

      fViewer.addDoubleClickListener(new IDoubleClickListener() {
        public void doubleClick(DoubleClickEvent event) {
          IStructuredSelection selection = (IStructuredSelection) event.getSelection();
          IFolder folder = selection.getFirstElement() instanceof IFolder ? (IFolder) selection.getFirstElement() : null;

          /* Expand / Collapse Folder */
          if (folder != null && !folder.isEmpty()) {
            boolean expandedState = !fViewer.getExpandedState(folder);
            fViewer.setExpandedState(folder, expandedState);

            if (expandedState && fViewer.getChecked(folder))
              setChildsChecked(folder, true, true, false);
          }
        }
      });

      fViewer.setInput(new Object());

      /* Apply checked elements */
      if (fCheckedElements != null) {
        for (IFolderChild child : fCheckedElements) {
          setParentsExpanded(child);
          cache(child, true);
          fViewer.setChecked(child, true);
          setChildsChecked(child, true, true, true);
        }
      }

      /* Update Checks on Selection */
      fViewer.getTree().addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (e.detail == SWT.CHECK) {
            TreeItem item = (TreeItem) e.item;

            /* Disable selection for Folders if SELECT_BIN */
            if (fMode == Mode.SELECT_BIN && item.getData() instanceof IFolder) {
              e.detail = SWT.NONE;
              e.doit = false;
              item.setChecked(false);
            }

            /* Normal selection behavior otherwise */
            else {
              IFolderChild folderChild = (IFolderChild) item.getData();
              setChildsChecked(folderChild, item.getChecked(), false, true);
              cache(folderChild, item.getChecked());

              if (!item.getChecked())
                setParentsChecked(folderChild, false, true);
            }
          }
        }
      });

      /* Update Checks on Expand */
      fViewer.addTreeListener(new ITreeViewerListener() {
        public void treeExpanded(TreeExpansionEvent event) {
          boolean isChecked = fViewer.getChecked(event.getElement());
          if (isChecked)
            setChildsChecked((IFolderChild) event.getElement(), isChecked, false, false);
        }

        public void treeCollapsed(TreeExpansionEvent event) {}
      });

      /* Select and Show Selection */
      if (fSelectedElement != null) {
        fViewer.setSelection(new StructuredSelection(fSelectedElement));
        fViewer.getTree().showSelection();
      }

      /* Select All / Deselect All */
      Composite buttonContainer = new Composite(composite, SWT.NONE);
      buttonContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
      buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      if (fMode == Mode.SEARCH_LOCATION) {
        Button selectAll = new Button(buttonContainer, SWT.PUSH);
        selectAll.setText("&Select All");
        setButtonLayoutData(selectAll);
        selectAll.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            OwlUI.setAllChecked(fViewer.getTree(), true);
            cacheAll(true);
          }
        });
      }

      Button deselectAll = new Button(buttonContainer, SWT.PUSH);
      deselectAll.setText("&Deselect All");
      setButtonLayoutData(deselectAll);
      deselectAll.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          OwlUI.setAllChecked(fViewer.getTree(), false);
          cacheAll(false);
        }
      });

      return composite;
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {

      /* Separator */
      Label sep = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

      return super.createButtonBar(parent);
    }

    private void cache(IFolderChild child, boolean checked) {
      if (checked)
        fCheckedElementsCache.add(child);
      else
        fCheckedElementsCache.remove(child);
    }

    private void cacheAll(boolean checked) {
      Tree tree = fViewer.getTree();
      cacheAll(tree.getItems(), checked);
    }

    private void cacheAll(TreeItem[] items, boolean checked) {
      for (TreeItem item : items) {
        if (item.getData() != null) { //Could not yet be resolved!
          cache((IFolderChild) item.getData(), checked);
          cacheAll(item.getItems(), checked);
        }
      }
    }

    private void setChildsChecked(IFolderChild folderChild, boolean checked, boolean onlyExpanded, boolean cache) {
      if (folderChild instanceof IFolder && (!onlyExpanded || fViewer.getExpandedState(folderChild))) {
        List<IFolderChild> children = ((IFolder) folderChild).getChildren();
        for (IFolderChild child : children) {
          if (cache)
            cache(child, checked);
          fViewer.setChecked(child, checked);
          setChildsChecked(child, checked, onlyExpanded, cache);
        }
      }
    }

    private void setParentsChecked(IFolderChild folderChild, boolean checked, boolean cache) {
      IFolder parent = folderChild.getParent();
      if (parent != null) {
        if (cache)
          cache(parent, checked);
        fViewer.setChecked(parent, checked);
        setParentsChecked(parent, checked, cache);
      }
    }

    private void setParentsExpanded(IFolderChild folderChild) {
      IFolder parent = folderChild.getParent();
      if (parent != null) {
        fViewer.setExpandedState(parent, true);
        setParentsExpanded(parent);
      }
    }

    /*
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      switch (fMode) {
        case SEARCH_LOCATION:
          newShell.setText("Choose Location");
          break;
        case SELECT_BIN:
          newShell.setText("Choose News Bins");
          break;
      }
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
     */
    @Override
    protected void initializeBounds() {
      super.initializeBounds();
      Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT);
      getShell().setSize(bestSize);
      LayoutUtils.positionShell(getShell(), false);
    }
  }

  /**
   * @param parent
   * @param style
   */
  public LocationControl(Composite parent, int style) {
    this(parent, style, Mode.SEARCH_LOCATION);
  }

  /**
   * @param parent
   * @param style
   * @param mode
   */
  public LocationControl(Composite parent, int style, Mode mode) {
    super(parent, style);

    fMode = mode;
    initComponents();
  }

  /**
   * @return the selected locations
   */
  public Long[][] getSelection() {
    return fSelection != null ? ModelUtils.toPrimitive(fSelection) : null;
  }

  /**
   * @param selection
   */
  public void select(Long[][] selection) {
    fSelection = CoreUtils.toEntities(selection);
    fConditionLabel.setText(getLabel(fSelection));
  }

  private void initComponents() {

    /* Apply Gridlayout */
    setLayout(LayoutUtils.createGridLayout(1, 5, 1));

    fConditionLabel = new Link(this, SWT.NONE);
    fConditionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fConditionLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IFolderChild selectedChild = null;
        if (e.text != null && e.text.length() > 0)
          selectedChild = fSelection.get(Integer.valueOf(e.text));

        onChangeCondition(selectedChild);
      }
    });

    fConditionLabel.setText(getLabel(fSelection));
  }

  private void onChangeCondition(IFolderChild selectedChild) {
    FolderChildChooserDialog dialog = new FolderChildChooserDialog(getShell(), selectedChild, fSelection);
    if (dialog.open() == IDialogConstants.OK_ID) {
      List<IFolderChild> checkedElements = dialog.getCheckedElements();
      fSelection = checkedElements;
      fConditionLabel.setText(getLabel(fSelection));
      notifyListeners(SWT.Modify, new Event());

      /* Link might require more space now */
      getShell().layout(true, true);
    }
  }

  @SuppressWarnings("null")
  private String getLabel(List<IFolderChild> entities) {
    if (entities == null || entities.size() == 0) {
      switch (fMode) {
        case SEARCH_LOCATION:
          return "<a href=\"\">Choose Location...</a>";
        case SELECT_BIN:
          return "<a href=\"\">Choose News Bins...</a>";
      }
    }

    StringBuilder strB = new StringBuilder();
    for (int i = 0; i < entities.size(); i++) {
      strB.append("<a href=\"" + i + "\">").append(entities.get(i).getName()).append("</a>").append(", ");
    }

    if (strB.length() > 0)
      strB.delete(strB.length() - 2, strB.length());

    return strB.toString();
  }
}