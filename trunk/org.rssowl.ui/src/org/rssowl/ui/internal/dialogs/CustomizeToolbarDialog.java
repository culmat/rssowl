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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.CoolBarAdvisor.Item;
import org.rssowl.ui.internal.util.CColumnLayoutData;
import org.rssowl.ui.internal.util.CTable;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.CColumnLayoutData.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link CustomizeToolbarDialog} allows to manage the Items appearing in
 * the application Toolbar.
 *
 * @author bpasero
 */
//TODO Implement DND (consider adapting to other dialogs supporting Move Up / Move Down)
//TODO Separators are unhappy due to multiple occurance
//TODO Implement Modes
public class CustomizeToolbarDialog extends Dialog {
  private static final String DIALOG_SETTINGS_KEY = "org.rssowl.ui.internal.dialogs.CustomizeToolbarDialog";

  private LocalResourceManager fResources;
  private boolean fFirstTimeOpen;
  private TableViewer fViewer;
  private IPreferenceScope fPreferences;
  private Map<String, Item> fMapIdToItem;
  private Button fAddButton;
  private Button fRemoveButton;
  private Button fMoveUpButton;
  private Button fMoveDownButton;
  private Button fRestoreDefaults;

  /**
   * @param parentShell
   */
  public CustomizeToolbarDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fFirstTimeOpen = (Activator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY) == null);
    fPreferences = Owl.getPreferenceService().getGlobalScope();

    fMapIdToItem = new HashMap<String, Item>();

    Item[] items = Item.values();
    for (Item item : items) {
      fMapIdToItem.put(item.getId(), item);
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#close()
   */
  @Override
  public boolean close() {
    fResources.dispose();
    return super.close();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Customize Toolbar");
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite container = createContainer(parent);

    /* Table showing Tool Items */
    Composite tableContainer = new Composite(container, SWT.NONE);
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    CTable cTable = new CTable(tableContainer, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    fViewer = new TableViewer(cTable.getControl());
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTable().setHeaderVisible(true);
    ((GridData) fViewer.getTable().getLayoutData()).heightHint = fViewer.getTable().getItemHeight() * 20;
    fViewer.getTable().setFocus();

    TableColumn nameCol = new TableColumn(fViewer.getTable(), SWT.NONE);

    CColumnLayoutData data = new CColumnLayoutData(Size.FILL, 100);
    cTable.manageColumn(nameCol, data, "Currently Visible Items", null, null, false, false);

    /* ContentProvider returns all selected Items */
    fViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS);
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fViewer.setLabelProvider(new CellLabelProvider() {
      @Override
      public void update(ViewerCell cell) {
        String itemId = (String) cell.getElement();
        Item item = fMapIdToItem.get(itemId);

        cell.setText(item.getName());
        if (item.getImg() != null)
          cell.setImage(fResources.createImage(item.getImg()));
      }
    });

    /* Selection */
    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateMoveEnablement();
      }
    });

    /* Set Dummy Input */
    fViewer.setInput(this);

    /* Container for the Buttons to Manage Providers */
    Composite buttonContainer = new Composite(container, SWT.None);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

    /* Add */
    final Menu menu = new Menu(getShell(), SWT.POP_UP);
    menu.addMenuListener(new MenuListener() {
      public void menuShown(MenuEvent e) {
        MenuItem[] items = menu.getItems();
        for (MenuItem item : items) {
          item.dispose();
        }

        /* Fill not yet visible Items */
        List<String> visibleItems = Arrays.asList(fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS));
        Item[] toolItems = Item.values();
        for (final Item toolItem : toolItems) {
          if (!visibleItems.contains(toolItem.getId()) || toolItem == Item.SEPARATOR) {
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            item.setText(toolItem.getName());
            if (toolItem.getImg() != null)
              item.setImage(fResources.createImage(toolItem.getImg()));
            item.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                onAdd(toolItem);
              }
            });
          }
        }
      }

      public void menuHidden(MenuEvent e) {}
    });

    fAddButton = new Button(buttonContainer, SWT.DOWN);
    fAddButton.setText("&Add");
    setButtonLayoutData(fAddButton);
    fAddButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Rectangle rect = fAddButton.getBounds();
        Point pt = new Point(rect.x, rect.y + rect.height);
        pt = fAddButton.toDisplay(pt);
        menu.setLocation(pt.x, pt.y);
        menu.setVisible(true);
      }
    });

    /* Remove */
    fRemoveButton = new Button(buttonContainer, SWT.PUSH);
    fRemoveButton.setText("&Remove");
    fRemoveButton.setEnabled(false);
    setButtonLayoutData(fRemoveButton);
    fRemoveButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onRemove();
      }
    });

    /* Separator */
    Label sep = new Label(buttonContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
    sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

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

    /* Restore Defaults */
    fRestoreDefaults = new Button(buttonContainer, SWT.PUSH);
    fRestoreDefaults.setText("Restore &Defaults");
    setButtonLayoutData(fRestoreDefaults);
    ((GridData) fRestoreDefaults.getLayoutData()).grabExcessVerticalSpace = true;
    ((GridData) fRestoreDefaults.getLayoutData()).verticalAlignment = SWT.END;
    fRestoreDefaults.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onRestoreDefaults();
      }
    });

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    return container;
  }

  private void onAdd(Item newItem) {
    String[] items = fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS);
    List<String> newItems = new ArrayList<String>();
    for (String item : items) {
      newItems.add(item);
    }

    int selectionIndex = fViewer.getTable().getSelectionIndex();
    if (selectionIndex >= 0)
      newItems.add(selectionIndex + 1, newItem.getId());
    else
      newItems.add(newItem.getId());

    fPreferences.putStrings(DefaultPreferences.TOOLBAR_ITEMS, newItems.toArray(new String[newItems.size()]));
    fViewer.refresh();
    updateMoveEnablement();

    fViewer.setSelection(StructuredSelection.EMPTY);
  }

  private void onRemove() {
    String[] items = fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS);
    List<String> newItems = new ArrayList<String>();
    for (String item : items) {
      newItems.add(item);
    }

    int[] selectionIndices = fViewer.getTable().getSelectionIndices();
    for (int i = 0; i < selectionIndices.length; i++) {
      int index = selectionIndices[i] - i;
      newItems.remove(index);
    }

    fPreferences.putStrings(DefaultPreferences.TOOLBAR_ITEMS, newItems.toArray(new String[newItems.size()]));
    fViewer.refresh();
    updateMoveEnablement();

    fViewer.setSelection(StructuredSelection.EMPTY);
  }

  private Composite createContainer(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    composite.setFont(parent.getFont());
    return composite;
  }

  private void onRestoreDefaults() {
    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();
    String[] defaultState = defaultScope.getStrings(DefaultPreferences.TOOLBAR_ITEMS);

    fPreferences.putStrings(DefaultPreferences.TOOLBAR_ITEMS, defaultState);
    fViewer.refresh();
    updateMoveEnablement();
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
    fRemoveButton.setEnabled(!fViewer.getSelection().isEmpty());
  }

  private void onMove(boolean up) {
    TableItem[] items = fViewer.getTable().getItems();
    List<String> sortedItemIds = new ArrayList<String>(items.length);
    for (TableItem item : items) {
      sortedItemIds.add((String) item.getData());
    }

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    String selectedItem = (String) selection.getFirstElement();

    String[] order = fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS);
    int selectedIndex = sortedItemIds.indexOf(selectedItem);

    /* Move Up */
    if (up && selectedIndex > 0) {
      String order1 = order[selectedIndex];
      String order2 = order[selectedIndex - 1];
      order[selectedIndex] = order2;
      order[selectedIndex - 1] = order1;
    }

    /* Move Down */
    else if (!up && selectedIndex < sortedItemIds.size() - 1) {
      String order1 = order[selectedIndex];
      String order2 = order[selectedIndex + 1];
      order[selectedIndex] = order2;
      order[selectedIndex + 1] = order1;
    }

    fPreferences.putStrings(DefaultPreferences.TOOLBAR_ITEMS, order);
    fViewer.refresh();
    updateMoveEnablement();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
   */
  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings settings = Activator.getDefault().getDialogSettings();
    IDialogSettings section = settings.getSection(DIALOG_SETTINGS_KEY);
    if (section != null)
      return section;

    return settings.addNewSection(DIALOG_SETTINGS_KEY);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsStrategy()
   */
  @Override
  protected int getDialogBoundsStrategy() {
    return Dialog.DIALOG_PERSISTLOCATION | Dialog.DIALOG_PERSISTSIZE;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#isResizable()
   */
  @Override
  protected boolean isResizable() {
    return true;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

    /* Dialog was not opened before */
    if (fFirstTimeOpen) {
      Shell shell = getShell();

      /* Minimum Size */
      int minWidth = convertHorizontalDLUsToPixels(OwlUI.MIN_DIALOG_WIDTH_DLU);
      int minHeight = shell.computeSize(minWidth, SWT.DEFAULT).y;

      /* Required Size */
      Point requiredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      shell.setSize(Math.max(minWidth, requiredSize.x), Math.max(minHeight, requiredSize.y));
      LayoutUtils.positionShell(shell, false);
    }
  }
}