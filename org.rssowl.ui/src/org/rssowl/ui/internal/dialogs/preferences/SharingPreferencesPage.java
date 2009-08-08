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

package org.rssowl.ui.internal.dialogs.preferences;

import org.eclipse.jface.preference.PreferencePage;
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.ShareProvider;
import org.rssowl.ui.internal.util.CColumnLayoutData;
import org.rssowl.ui.internal.util.CTable;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.CColumnLayoutData.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreferencePage} to configure {@link ShareProvider}.
 *
 * @author bpasero
 */
public class SharingPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of the Page */
  public static final String ID = "org.rssowl.ui.SharingPreferencesPage";

  private LocalResourceManager fResources;
  private IPreferenceScope fPreferences;
  private Button fMoveDownButton;
  private Button fMoveUpButton;
  private CheckboxTableViewer fViewer;
  private int[] fInitialShareProviderState;

  /** Leave for reflection */
  public SharingPreferencesPage() {
    setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/share.gif"));
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fPreferences = Owl.getPreferenceService().getGlobalScope();
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
    fInitialShareProviderState = fPreferences.getIntegers(DefaultPreferences.SHARE_PROVIDER_STATE);
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createContainer(parent);

    StyledText infoText = new StyledText(container, SWT.WRAP | SWT.READ_ONLY);
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    ((GridData) infoText.getLayoutData()).widthHint = 200;
    infoText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    infoText.setText("Select from the following list of available communities to share your Bookmarks and News with others:");

    Composite tableContainer = new Composite(container, SWT.NONE);
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    CTable cTable = new CTable(tableContainer, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    fViewer = new CheckboxTableViewer(cTable.getControl());
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTable().setHeaderVisible(true);
    ((GridData) fViewer.getTable().getLayoutData()).heightHint = fViewer.getTable().getItemHeight() * 15;
    fViewer.getTable().setFocus();

    TableColumn nameCol = new TableColumn(fViewer.getTable(), SWT.NONE);

    CColumnLayoutData data = new CColumnLayoutData(Size.FILL, 100);
    cTable.manageColumn(nameCol, data, "Available Communities", null, null, false, false);

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
        cell.setText(provider.getName());
        if (StringUtils.isSet(provider.getIconPath()))
          cell.setImage(fResources.createImage(OwlUI.getImageDescriptor(provider.getPluginId(), provider.getIconPath())));
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

    /* Ensure that the first checked element is visible */
    TableItem[] items = fViewer.getTable().getItems();
    for (TableItem item : items) {
      if (item.getChecked()) {
        fViewer.getTable().showItem(item);
        break;
      }
    }

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
    Composite buttonContainer = new Composite(container, SWT.None);
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

    /* Info Container */
    Composite infoContainer = new Composite(container, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif"));
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    Label infoTextLabel = new Label(infoContainer, SWT.WRAP);
    infoTextLabel.setText("Enabled communities will appar in the menu for selected Bookmarks and News.");
    infoTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    return container;
  }

  private Composite createContainer(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
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
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    save();
    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();
    int[] defaultState = defaultScope.getIntegers(DefaultPreferences.SHARE_PROVIDER_STATE);

    Owl.getPreferenceService().getGlobalScope().putIntegers(DefaultPreferences.SHARE_PROVIDER_STATE, defaultState);
    fViewer.refresh();
    updateCheckedState();
    updateMoveEnablement();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  @Override
  protected void performApply() {
    super.performApply();
    fInitialShareProviderState = fPreferences.getIntegers(DefaultPreferences.SHARE_PROVIDER_STATE);
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performCancel()
   */
  @Override
  public boolean performCancel() {
    fPreferences.putIntegers(DefaultPreferences.SHARE_PROVIDER_STATE, fInitialShareProviderState);
    return super.performCancel();
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }
}