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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTable;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.ShareProvider;
import org.rssowl.ui.internal.CColumnLayoutData.Size;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog to configure the share providers.
 *
 * @author bpasero
 */
public class ShareProvidersListDialog extends TitleAreaDialog {

  /* Section for Dialogs Settings */
  private static final String SETTINGS_SECTION = "org.rssowl.ui.internal.dialogs.ShareProvidersListDialog";

  private LocalResourceManager fResources;
  private CheckboxTableViewer fViewer;
  private IPreferenceScope fPreferences;
  private Button fMoveUpButton;
  private Button fMoveDownButton;

  /**
   * @param parentShell
   */
  public ShareProvidersListDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fPreferences = Owl.getPreferenceService().getGlobalScope();
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
    boolean res = super.close();
    fResources.dispose();
    return res;
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title */
    setTitle("Configure Communities");

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/share_wiz.gif"));

    setMessage("Configure the sort order and enablement of communities for sharing.");

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Composite tableContainer = new Composite(composite, SWT.NONE);
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    CTable cTable = new CTable(tableContainer, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    fViewer = new CheckboxTableViewer(cTable.getControl());
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTable().setHeaderVisible(true);
    ((GridData) fViewer.getTable().getLayoutData()).heightHint = fViewer.getTable().getItemHeight() * 15;

    TableColumn nameCol = new TableColumn(fViewer.getTable(), SWT.NONE);

    CColumnLayoutData data = new CColumnLayoutData(Size.FILL, 100);
    cTable.manageColumn(nameCol, data, "Available Providers", null, null, false, false);

    /* ContentProvider returns all providers */
    fViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return Controller.getDefault().getShareProviders().toArray();
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fViewer.setLabelProvider(new CellLabelProvider() {
      @Override
      public void update(ViewerCell cell) {
        ShareProvider provider = (ShareProvider) cell.getElement();
        Display display = fViewer.getControl().getDisplay();
        cell.setText(provider.getName());
        if (StringUtils.isSet(provider.getIconPath()))
          cell.setImage(fResources.createImage(OwlUI.getImageDescriptor(provider.getPluginId(), provider.getIconPath())));
        cell.setForeground(provider.isEnabled() ? display.getSystemColor(SWT.COLOR_BLACK) : display.getSystemColor(SWT.COLOR_DARK_GRAY));
      }
    });

    /* Selection */
    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateMoveEnablement();
      }
    });

    /* Set input (ignored by ContentProvider anyways) */
    fViewer.setInput(this);
    updateCheckedState();

    /* Listen on Check State Changes */
    fViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        ShareProvider provider = (ShareProvider) event.getElement();
        provider.setEnabled(event.getChecked());
        save();
        fViewer.update(provider, null);
      }
    });

    /* Container for the Buttons to Manage Providers */
    Composite buttonContainer = new Composite(composite, SWT.None);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

    /* Move Provider Up */
    fMoveUpButton = new Button(buttonContainer, SWT.PUSH);
    fMoveUpButton.setText("Move &Up");
    fMoveUpButton.setEnabled(false);
    setButtonLayoutData(fMoveUpButton);
    fMoveUpButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onMove(true);
      }
    });

    /* Move Provider Down */
    fMoveDownButton = new Button(buttonContainer, SWT.PUSH);
    fMoveDownButton.setText("Move &Down");
    fMoveDownButton.setEnabled(false);
    setButtonLayoutData(fMoveDownButton);
    fMoveDownButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onMove(false);
      }
    });

    /* Separator */
    Label sep = new Label(buttonContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
    sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Select All */
    Button selectAllButton = new Button(buttonContainer, SWT.PUSH);
    selectAllButton.setText("Select &All");
    setButtonLayoutData(selectAllButton);
    selectAllButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelectAll(false);
      }
    });

    /* De-Select All */
    Button deSelectAllButton = new Button(buttonContainer, SWT.PUSH);
    deSelectAllButton.setText("&Deselect All");
    setButtonLayoutData(deSelectAllButton);
    deSelectAllButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelectAll(true);
      }
    });

    /* Close */
    Button closeButton = new Button(buttonContainer, SWT.PUSH);
    closeButton.getShell().setDefaultButton(closeButton);
    closeButton.setText("&Close");
    setButtonLayoutData(closeButton);
    ((GridData) closeButton.getLayoutData()).verticalAlignment = SWT.END;
    ((GridData) closeButton.getLayoutData()).grabExcessVerticalSpace = true;
    closeButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        close();
      }
    });

    return composite;
  }

  private void onSelectAll(boolean deselect) {
    TableItem[] items = fViewer.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      TableItem tableItem = items[i];
      ShareProvider provider = (ShareProvider) tableItem.getData();
      provider.setEnabled(!deselect);
      fViewer.setChecked(provider, !deselect);
    }

    save();
    fViewer.refresh();
  }

  private void save() {
    TableItem[] items = fViewer.getTable().getItems();
    int[] newState = new int[items.length];

    for (int i = 0; i < items.length; i++) {
      TableItem tableItem = items[i];
      ShareProvider provider = (ShareProvider) tableItem.getData();

      int index = provider.getIndex();
      index++; //Adjust to non-zero indexing
      if (!provider.isEnabled())
        index = index * -1;

      newState[i] = index;
    }

    fPreferences.putIntegers(DefaultPreferences.SHARE_PROVIDER_STATE, newState);
  }

  private void updateMoveEnablement() {
    boolean enableMoveUp = true;
    boolean enableMoveDown = true;
    int[] selectionIndices = fViewer.getTable().getSelectionIndices();
    if (selectionIndices.length == 1) {
      enableMoveUp = selectionIndices[0] != 0;
      enableMoveDown = selectionIndices[0] != fViewer.getTable().getItemCount() - 1;
    } else {
      enableMoveUp = false;
      enableMoveDown = false;
    }

    fMoveUpButton.setEnabled(enableMoveUp);
    fMoveDownButton.setEnabled(enableMoveDown);
  }

  private void onMove(boolean up) {
    TableItem[] items = fViewer.getTable().getItems();
    List<ShareProvider> sortedProviders = new ArrayList<ShareProvider>(items.length);
    for (TableItem item : items) {
      sortedProviders.add((ShareProvider) item.getData());
    }

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    ShareProvider selectedProvider = (ShareProvider) selection.getFirstElement();

    int[] order = fPreferences.getIntegers(DefaultPreferences.SHARE_PROVIDER_STATE);
    int selectedIndex = sortedProviders.indexOf(selectedProvider);

    /* Move Up */
    if (up && selectedIndex > 0) {
      int order1 = order[selectedIndex];
      int order2 = order[selectedIndex - 1];
      order[selectedIndex] = order2;
      order[selectedIndex - 1] = order1;
    }

    /* Move Down */
    else if (!up && selectedIndex < sortedProviders.size() - 1) {
      int order1 = order[selectedIndex];
      int order2 = order[selectedIndex + 1];
      order[selectedIndex] = order2;
      order[selectedIndex + 1] = order1;
    }

    fPreferences.putIntegers(DefaultPreferences.SHARE_PROVIDER_STATE, order);
    fViewer.refresh();
    fViewer.getTable().showSelection();
    updateCheckedState();
    updateMoveEnablement();
  }

  private void updateCheckedState() {
    TableItem[] items = fViewer.getTable().getItems();
    for (TableItem item : items) {
      ShareProvider provider = (ShareProvider) item.getData();
      fViewer.setChecked(provider, provider.isEnabled());
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {
    return null;
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Configure Communities");
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#isResizable()
   */
  @Override
  protected boolean isResizable() {
    return true;
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.APPLICATION_MODAL | SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.CLOSE | getDefaultOrientation();

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
   */
  @Override
  protected int getDialogBoundsStrategy() {
    return DIALOG_PERSISTSIZE;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
   */
  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings settings = Activator.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(SETTINGS_SECTION);
    if (section != null)
      return section;

    return settings.addNewSection(SETTINGS_SECTION);
  }
}