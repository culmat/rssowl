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

package org.rssowl.ui.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.events.FolderAdapter;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.ui.internal.actions.NewFolderAction;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;

import java.util.Set;

/**
 * The <code>FolderChooser</code> allows to select a <code>IFolder</code>.
 * It provides an expandable Tree-Viewer to display all folders which is
 * initially collapsed.
 *
 * @author bpasero
 */
public class FolderChooser extends Composite implements DisposeListener {
  private Composite fParent;
  private IFolder fSelectedFolder;
  private ResourceManager fResources;
  private Composite fFolderViewerContainer;
  private ToolItem fToggleItem;
  private TreeViewer fFolderViewer;
  private Label fFolderIcon;
  private Label fFolderName;
  private int fViewerHeight;
  private FolderAdapter fFolderListener;

  /**
   * @param parent
   * @param initial
   * @param style
   */
  public FolderChooser(Composite parent, IFolder initial, int style) {
    super(parent, style);
    fParent = parent;
    fSelectedFolder = initial;
    fResources = new LocalResourceManager(JFaceResources.getResources(), parent);

    initComponents();
    addDisposeListener(this);
  }

  /**
   * @return Returns the <code>IFolder</code> that has been selected.
   */
  public IFolder getFolder() {
    return fSelectedFolder;
  }

  /*
   * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
   */
  public void widgetDisposed(DisposeEvent e) {
    unregisterListeners();
  }

  private void registerListeners() {
    fFolderListener = new FolderAdapter() {
      @Override
      public void folderUpdated(final Set<FolderEvent> events) {
        if (events.isEmpty())
          return;

        /* Refresh and show added Folder */
        JobRunner.runInUIThread(fFolderViewer.getControl(), new Runnable() {
          public void run() {
            fFolderViewer.refresh();
            FolderEvent event = events.iterator().next();
            expand(event.getEntity());
          }
        });
      }

      @Override
      public void folderAdded(final Set<FolderEvent> events) {
        if (events.isEmpty())
          return;

        /* Select added Folder */
        JobRunner.runInUIThread(fFolderViewer.getControl(), new Runnable() {
          public void run() {
            FolderEvent event = events.iterator().next();
            fFolderViewer.setSelection(new StructuredSelection(event.getEntity()));
          }
        });
      }
    };

    NewsModel.getDefault().addFolderListener(fFolderListener);
  }

  private void expand(IFolder folder) {
    IFolder parent = folder.getParent();
    if (parent != null)
      expand(parent);

    fFolderViewer.setExpandedState(folder, true);
  }

  private void unregisterListeners() {
    NewsModel.getDefault().removeFolderListener(fFolderListener);
  }

  private void initComponents() {
    Composite headerContainer = new Composite(this, SWT.None);
    headerContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));
    ((GridLayout) headerContainer.getLayout()).marginLeft = 3;
    headerContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    headerContainer.setBackground(fParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    headerContainer.setCursor(fParent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    headerContainer.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        onToggle();
      }
    });

    fFolderIcon = new Label(headerContainer, SWT.None);
    fFolderIcon.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    fFolderIcon.setBackground(fParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    fFolderIcon.setCursor(fParent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    fFolderIcon.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        onToggle();
      }
    });

    fFolderName = new Label(headerContainer, SWT.None);
    fFolderName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fFolderName.setBackground(fParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    fFolderName.setCursor(fParent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    fFolderName.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        onToggle();
      }
    });

    ToolBar toggleBar = new ToolBar(headerContainer, SWT.FLAT);
    toggleBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, true));
    toggleBar.setBackground(fParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    toggleBar.setCursor(headerContainer.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

    ToolItem addFolderItem = new ToolItem(toggleBar, SWT.PUSH);
    addFolderItem.setImage(RSSOwlUI.getImage(fResources, "icons/etool16/add_crop.gif"));
    addFolderItem.setToolTipText("New Folder...");
    addFolderItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onNewFolder();
      }
    });

    fToggleItem = new ToolItem(toggleBar, SWT.PUSH);
    fToggleItem.setImage(RSSOwlUI.getImage(fResources, "icons/ovr16/arrow_down.gif"));
    fToggleItem.setToolTipText("Show Folders");
    fToggleItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onToggle();
      }
    });

    fFolderViewerContainer = new Composite(this, SWT.None);
    fFolderViewerContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 0, false));
    fFolderViewerContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fFolderViewerContainer.setBackground(fParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    Label separator = new Label(fFolderViewerContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fFolderViewer = new TreeViewer(fFolderViewerContainer, SWT.None);
    fFolderViewer.setAutoExpandLevel(2);
    fFolderViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fFolderViewer.getTree().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

    fViewerHeight = fFolderViewer.getTree().getItemHeight() * 10 + 12;
    ((GridData) fFolderViewerContainer.getLayoutData()).heightHint = fViewerHeight;
    ((GridData) fFolderViewerContainer.getLayoutData()).exclude = true;

    fFolderViewer.setContentProvider(new ITreeContentProvider() {
      public Object[] getElements(Object inputElement) {
        Set<IFolder> rootFolders = Controller.getDefault().getCacheService().getRootFolders();
        return rootFolders.toArray();
      }

      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IFolder) {
          IFolder folder = (IFolder) parentElement;
          return folder.getFolders().toArray();
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
          return !folder.getFolders().isEmpty();
        }

        return false;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    fFolderViewer.setLabelProvider(new BookMarkLabelProvider());
    fFolderViewer.setInput(new Object());

    fFolderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        onFolderSelected((IFolder) selection.getFirstElement());
      }
    });

    fFolderViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        IFolder folder = (IFolder) selection.getFirstElement();

        /* Expand / Collapse Folder */
        if (!folder.getFolders().isEmpty()) {
          boolean expandedState = !fFolderViewer.getExpandedState(folder);
          fFolderViewer.setExpandedState(folder, expandedState);
        }

        /* Select Folder and toggle */
        else {
          onToggle();
        }
      }
    });

    /* Select the input Folder */
    fFolderViewer.setSelection(new StructuredSelection(fSelectedFolder));

    /* Add Menu: "New Folder" */
    MenuManager menuManager = new MenuManager();
    menuManager.add(new Action("New Folder...") {
      @Override
      public void run() {
        onNewFolder();
      }
    });

    Menu menu = menuManager.createContextMenu(fFolderViewer.getTree());
    fFolderViewer.getTree().setMenu(menu);

    /* Register Model Listeners */
    registerListeners();
  }

  private void onNewFolder() {

    /* Make sure Folder-List is visible */
    if (((GridData) fFolderViewerContainer.getLayoutData()).exclude)
      onToggle();

    /* Create new Folder */
    IStructuredSelection selection = (IStructuredSelection) fFolderViewer.getSelection();
    NewFolderAction action = new NewFolderAction(fFolderViewer.getTree().getShell(), (IFolder) selection.getFirstElement());
    action.run(null);
  }

  private void onFolderSelected(IFolder folder) {
    fSelectedFolder = folder;
    fFolderIcon.setImage(RSSOwlUI.getImage(fResources, folder));
    fFolderName.setText(folder.getName());
  }

  private void onToggle() {
    boolean excluded = ((GridData) fFolderViewerContainer.getLayoutData()).exclude;

    fToggleItem.setImage(RSSOwlUI.getImage(fResources, excluded ? "icons/ovr16/arrow_up.gif" : "icons/ovr16/arrow_down.gif"));
    fToggleItem.setToolTipText(excluded ? "Hide Folders" : "Show Folders");

    ((GridData) fFolderViewerContainer.getLayoutData()).exclude = !excluded;
    fFolderViewerContainer.getShell().layout();

    Point size = fFolderViewerContainer.getShell().getSize();
    fFolderViewerContainer.getShell().setSize(size.x, size.y + (excluded ? fViewerHeight : -fViewerHeight));

    if (excluded)
      fFolderViewer.getTree().setFocus();
    else
      fFolderViewer.getTree().getShell().setFocus();
  }
}