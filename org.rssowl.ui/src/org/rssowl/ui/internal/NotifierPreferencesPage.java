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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;
import org.rssowl.ui.internal.views.explorer.BookMarkSorter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class NotifierPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
  private IPreferenceScope fGlobalScope;
  private Button fNotificationOnlyFromTray;
  private Button fShowNotificationPopup;
  private Button fNotificationIsSticky;
  private Button fLimitNotificationCheck;
  private Spinner fLimitNotificationSpinner;
  private CheckboxTreeViewer fViewer;
  private Button fDeselectAll;
  private Button fSelectAll;
  private Button fLimitNotifierToSelectionCheck;

  /** Leave for reflection */
  public NotifierPreferencesPage() {
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
  }

  /**
   * @param title
   */
  public NotifierPreferencesPage(String title) {
    super(title);
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    /* Misc. Options */
    createNotificationOptions(container);

    /* Spacer */
    new Label(container, SWT.None);

    /* Viewer to select Folders/Marks for the Notifier */
    createNotifierViewer(container);

    return container;
  }

  private void createNotifierViewer(Composite container) {

    /* Check Button to enable Limitation */
    fLimitNotifierToSelectionCheck = new Button(container, SWT.CHECK);
    fLimitNotifierToSelectionCheck.setText("Only show notification for selected bookmarks: ");
    fLimitNotifierToSelectionCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION));
    fLimitNotifierToSelectionCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        setLimitNotificationEnabled(fLimitNotifierToSelectionCheck.getSelection());
      }
    });

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
        }
      }
    });

    /* Dummy Input */
    fViewer.setInput(new Object());

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

    setLimitNotificationEnabled(fShowNotificationPopup.getSelection() && fLimitNotifierToSelectionCheck.getSelection());
    fLimitNotifierToSelectionCheck.setEnabled(fShowNotificationPopup.getSelection());
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

  private void setLimitNotificationEnabled(boolean selection) {
    fViewer.getTree().setEnabled(selection);
    fSelectAll.setEnabled(selection);
    fDeselectAll.setEnabled(selection);
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

  private void createNotificationOptions(Composite container) {
    Composite notificationGroup = new Composite(container, SWT.None);
    notificationGroup.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    notificationGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Show Notification Popup */
    fShowNotificationPopup = new Button(notificationGroup, SWT.CHECK);
    fShowNotificationPopup.setText("Show notification on incoming news");
    fShowNotificationPopup.setSelection(fGlobalScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP));
    fShowNotificationPopup.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());
        fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());
        fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
        fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.isEnabled() && fLimitNotificationCheck.getSelection());
        setLimitNotificationEnabled(fShowNotificationPopup.getSelection() && fLimitNotifierToSelectionCheck.getSelection());
        fLimitNotifierToSelectionCheck.setEnabled(fShowNotificationPopup.getSelection());
      }
    });

    /* Limit number of News showing in Notification */
    Composite limitNewsContainer = new Composite(notificationGroup, SWT.None);
    limitNewsContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));

    int notificationLimit = fGlobalScope.getInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE);

    fLimitNotificationCheck = new Button(limitNewsContainer, SWT.CHECK);
    fLimitNotificationCheck.setText("Show a maximum of ");
    fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
    fLimitNotificationCheck.setSelection(notificationLimit >= 0);
    fLimitNotificationCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.getSelection());
      }
    });

    fLimitNotificationSpinner = new Spinner(limitNewsContainer, SWT.BORDER);
    fLimitNotificationSpinner.setMinimum(1);
    fLimitNotificationSpinner.setMaximum(30);
    fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.isEnabled() && fLimitNotificationCheck.getSelection());
    if (notificationLimit > 0)
      fLimitNotificationSpinner.setSelection(notificationLimit);
    else
      fLimitNotificationSpinner.setSelection(notificationLimit * -1);

    Label label = new Label(limitNewsContainer, SWT.None);
    label.setText(" News inside the notification");

    /* Only from Tray */
    fNotificationOnlyFromTray = new Button(notificationGroup, SWT.CHECK);
    fNotificationOnlyFromTray.setText("Show notification only when window is minimized");
    fNotificationOnlyFromTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());

    /* Sticky Notification Popup */
    fNotificationIsSticky = new Button(notificationGroup, SWT.CHECK);
    fNotificationIsSticky.setText("Leave notification open until closed manually");
    fNotificationIsSticky.setSelection(fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP, fShowNotificationPopup.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED, fNotificationOnlyFromTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP, fNotificationIsSticky.getSelection());

    if (fLimitNotificationCheck.getSelection())
      fGlobalScope.putInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE, fLimitNotificationSpinner.getSelection());
    else
      fGlobalScope.putInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE, fLimitNotificationSpinner.getSelection() * -1);

    fGlobalScope.putBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION, fLimitNotifierToSelectionCheck.getSelection());

    /* Entity Scopes from Selected Elements */
    if (fLimitNotifierToSelectionCheck.getSelection()) {
      Set<IFolder> rootFolders = Controller.getDefault().getCacheService().getRootFolders();
      List<?> checkedElements = Arrays.asList(fViewer.getCheckedElements());
      final Set<IFolderChild> entitiesToSave = new HashSet<IFolderChild>();

      for (IFolder folder : rootFolders) {
        boolean checked = checkedElements.contains(folder);
        performOk(folder, checkedElements, entitiesToSave, checked);
      }

      /* Save */
      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
        public void run() {
          DynamicDAO.saveAll(entitiesToSave);
        }
      });
    }

    return super.performOk();
  }

  private void performOk(IFolderChild entity, List<?> checkedElements, Set<IFolderChild> entitiesToSave, boolean parentChecked) {
    IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(entity);
    boolean save = false;

    /* Folder */
    boolean checked = checkedElements.contains(entity) || parentChecked;

    /* Now Checked and previously not */
    if (checked && !prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER)) {
      prefs.putBoolean(DefaultPreferences.ENABLE_NOTIFIER, true);
      save = true;
    }

    /* Now unchecked but previously checked */
    else if (!checked && prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER)) {
      prefs.delete(DefaultPreferences.ENABLE_NOTIFIER);
      save = true;
    }

    /* Remember to save if required */
    if (save)
      entitiesToSave.add(entity);

    /* Childs */
    if (entity instanceof IFolder) {
      List<IFolderChild> children = ((IFolder) entity).getChildren();
      for (IFolderChild child : children) {
        performOk(child, checkedElements, entitiesToSave, checked);
      }
    }
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    fShowNotificationPopup.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP));
    fNotificationOnlyFromTray.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());
    fNotificationIsSticky.setSelection(defaultScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());

    int limitNotificationValue = defaultScope.getInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE);
    fLimitNotificationCheck.setSelection(limitNotificationValue >= 0);
    if (limitNotificationValue >= 0)
      fLimitNotificationSpinner.setSelection(limitNotificationValue);
    fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
    fLimitNotificationSpinner.setEnabled(fShowNotificationPopup.getSelection());

    fLimitNotifierToSelectionCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION));

    setLimitNotificationEnabled(fShowNotificationPopup.getSelection() && fLimitNotificationCheck.getSelection());
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {}
}