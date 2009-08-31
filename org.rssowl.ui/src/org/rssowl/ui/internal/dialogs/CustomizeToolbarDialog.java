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
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.CoolBarAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.CoolBarAdvisor.CoolBarItem;
import org.rssowl.ui.internal.CoolBarAdvisor.CoolBarMode;
import org.rssowl.ui.internal.util.CColumnLayoutData;
import org.rssowl.ui.internal.util.CTable;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.CColumnLayoutData.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link CustomizeToolbarDialog} allows to manage the Items appearing in
 * the application Toolbar.
 *
 * @author bpasero
 */
public class CustomizeToolbarDialog extends Dialog {

  /* Size and Location Settings */
  private static final String DIALOG_SETTINGS_KEY = "org.rssowl.ui.internal.dialogs.CustomizeToolbarDialog";

  private LocalResourceManager fResources;
  private boolean fFirstTimeOpen;
  private TableViewer fItemViewer;
  private ComboViewer fModeViewer;
  private IPreferenceScope fPreferences;
  private Button fAddButton;
  private Button fRemoveButton;
  private Button fMoveUpButton;
  private Button fMoveDownButton;
  private Button fRestoreDefaults;

  /* Used in the Toolbar Item Viewer to avoid equal conflict with Separator / Spacer */
  private static class ToolBarItem {
    CoolBarItem item;

    ToolBarItem(CoolBarItem theItem) {
      item = theItem;
    }
  }

  /**
   * @param parentShell
   */
  public CustomizeToolbarDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fFirstTimeOpen = (Activator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_KEY) == null);
    fPreferences = Owl.getPreferenceService().getGlobalScope();
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

    fItemViewer = new TableViewer(cTable.getControl());
    fItemViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fItemViewer.getTable().setHeaderVisible(true);
    ((GridData) fItemViewer.getTable().getLayoutData()).heightHint = fItemViewer.getTable().getItemHeight() * 22;
    fItemViewer.getTable().setFocus();

    TableColumn nameCol = new TableColumn(fItemViewer.getTable(), SWT.NONE);

    CColumnLayoutData data = new CColumnLayoutData(Size.FILL, 100);
    cTable.manageColumn(nameCol, data, "Currently Visible Items", null, null, false, false);

    /* ContentProvider returns all selected Items */
    fItemViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        int[] itemIds = fPreferences.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
        ToolBarItem[] items = new ToolBarItem[itemIds.length];
        for (int i = 0; i < itemIds.length; i++) {
          items[i] = new ToolBarItem(CoolBarItem.values()[itemIds[i]]);
        }
        return items;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fItemViewer.setLabelProvider(new CellLabelProvider() {
      @Override
      public void update(ViewerCell cell) {
        CoolBarItem item = ((ToolBarItem) cell.getElement()).item;
        cell.setText(item.getName());
        if (item.getImg() != null)
          cell.setImage(fResources.createImage(item.getImg()));
      }
    });

    /* Selection */
    fItemViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        updateButtonEnablement();
      }
    });

    /* Support Keyboard Remove */
    fItemViewer.getTable().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.DEL || (Application.IS_MAC && e.keyCode == SWT.BS))
          onRemove();
      }
    });

    /* Drag Support */
    fItemViewer.addDragSupport(DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, new DragSourceAdapter() {
      @Override
      public void dragStart(final DragSourceEvent event) {
        SafeRunnable.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            IStructuredSelection selection = (IStructuredSelection) fItemViewer.getSelection();
            event.doit = (selection.size() == 1);

            if (event.doit) {
              LocalSelectionTransfer.getTransfer().setSelection(selection);
              LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
            };
          }
        });
      }

      @Override
      public void dragSetData(final DragSourceEvent event) {
        SafeRunnable.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType))
              event.data = LocalSelectionTransfer.getTransfer().getSelection();
          }
        });
      }

      @Override
      public void dragFinished(DragSourceEvent event) {
        SafeRunnable.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            LocalSelectionTransfer.getTransfer().setSelection(null);
            LocalSelectionTransfer.getTransfer().setSelectionSetTime(0);
          }
        });
      }
    });

    /* Drop Support */
    ViewerDropAdapter dropSupport = new ViewerDropAdapter(fItemViewer) {

      @Override
      public boolean validateDrop(Object target, int operation, TransferData transferType) {
        return true;
      }

      @Override
      public boolean performDrop(Object data) {
        ToolBarItem target = (ToolBarItem) getCurrentTarget();
        if (target != null) {
          onMove((ToolBarItem) ((StructuredSelection) data).getFirstElement(), target, getCurrentLocation());
          return true;
        }

        return false;
      }
    };
    dropSupport.setFeedbackEnabled(true);
    dropSupport.setScrollEnabled(true);
    dropSupport.setSelectionFeedbackEnabled(true);
    fItemViewer.addDropSupport(DND.DROP_MOVE, new Transfer[] { LocalSelectionTransfer.getTransfer() }, dropSupport);

    /* Set Dummy Input */
    fItemViewer.setInput(this);

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
        int[] toolbarItemIds = fPreferences.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
        List<CoolBarItem> visibleItems = new ArrayList<CoolBarItem>();
        for (int toolbarItemId : toolbarItemIds) {
          visibleItems.add(CoolBarItem.values()[toolbarItemId]);
        }

        CoolBarItem[] toolItems = CoolBarItem.values();
        int currentGroup = -1;
        for (final CoolBarItem toolItem : toolItems) {
          if (!visibleItems.contains(toolItem) || toolItem == CoolBarItem.SEPARATOR || toolItem == CoolBarItem.SPACER) {

            /* Divide Groups by Separators */
            if (currentGroup >= 0 && currentGroup != toolItem.getGroup())
              new MenuItem(menu, SWT.SEPARATOR);

            /* Create Menu Item */
            MenuItem item = new MenuItem(menu, SWT.PUSH);
            if (StringUtils.isSet(toolItem.getTooltip()))
              item.setText(toolItem.getTooltip());
            else
              item.setText(toolItem.getName());
            if (toolItem.getImg() != null)
              item.setImage(fResources.createImage(toolItem.getImg()));

            item.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {

                /* Add Item */
                onAdd(toolItem);

                /* Re-Open Menu for More */
                JobRunner.runInUIThread(menu, new Runnable() {
                  public void run() {
                    menu.setVisible(true);
                  };
                });
              }
            });

            currentGroup = toolItem.getGroup();
          }
        }
      }

      public void menuHidden(MenuEvent e) {}
    });

    fAddButton = new Button(buttonContainer, SWT.DOWN);
    fAddButton.setText("&Add");
    applyDialogFont(fAddButton);
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
    applyDialogFont(fRemoveButton);
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
    applyDialogFont(fMoveUpButton);
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
    applyDialogFont(fMoveDownButton);
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
    applyDialogFont(fRestoreDefaults);
    setButtonLayoutData(fRestoreDefaults);
    ((GridData) fRestoreDefaults.getLayoutData()).grabExcessVerticalSpace = true;
    ((GridData) fRestoreDefaults.getLayoutData()).verticalAlignment = SWT.END;
    fRestoreDefaults.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onRestoreDefaults();
      }
    });

    /* Toolbar Mode */
    Composite modeContainer = new Composite(container, SWT.None);
    modeContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 0));
    modeContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false, 2, 1));

    Label showLabel = new Label(modeContainer, SWT.NONE);
    showLabel.setText("Show: ");

    fModeViewer = new ComboViewer(modeContainer, SWT.READ_ONLY | SWT.BORDER);
    fModeViewer.setContentProvider(new ArrayContentProvider());
    fModeViewer.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        if (element instanceof CoolBarMode) {
          switch ((CoolBarMode) element) {
            case IMAGE:
              return "Icons";
            case TEXT:
              return "Text";
            case IMAGE_TEXT:
              return "Icons and Text";
          }
        }

        return super.getText(element);
      }
    });

    fModeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
        CoolBarMode mode = (CoolBarMode) selection;
        fPreferences.putInteger(DefaultPreferences.TOOLBAR_MODE, mode.ordinal());
      }
    });

    fModeViewer.setInput(CoolBarAdvisor.CoolBarMode.values());
    fModeViewer.setSelection(new StructuredSelection(CoolBarMode.values()[fPreferences.getInteger(DefaultPreferences.TOOLBAR_MODE)]));

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    applyDialogFont(container);

    return container;
  }

  private void onAdd(CoolBarItem newItem) {
    int[] toolbarItemIds = fPreferences.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
    List<CoolBarItem> newItems = new ArrayList<CoolBarItem>();
    for (int toolbarItemId : toolbarItemIds) {
      newItems.add(CoolBarItem.values()[toolbarItemId]);
    }

    int selectionIndex = fItemViewer.getTable().getSelectionIndex();
    if (selectionIndex >= 0)
      newItems.add(selectionIndex + 1, newItem);
    else
      newItems.add(newItem);

    /* Save & Refresh */
    int[] newItemsRaw = new int[newItems.size()];
    for (int i = 0; i < newItems.size(); i++)
      newItemsRaw[i] = newItems.get(i).ordinal();

    fPreferences.putIntegers(DefaultPreferences.TOOLBAR_ITEMS, newItemsRaw);
    fItemViewer.refresh();

    /* Update Selection */
    if (selectionIndex >= 0)
      fItemViewer.getTable().setSelection(selectionIndex + 1);
    else
      fItemViewer.getTable().setSelection(fItemViewer.getTable().getItemCount() - 1);

    /* Update Buttons */
    updateButtonEnablement();
  }

  private void onRemove() {
    int[] toolbarItemIds = fPreferences.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
    List<CoolBarItem> newItems = new ArrayList<CoolBarItem>();
    for (int toolbarItemId : toolbarItemIds) {
      newItems.add(CoolBarItem.values()[toolbarItemId]);
    }

    int[] selectionIndices = fItemViewer.getTable().getSelectionIndices();
    for (int i = 0; i < selectionIndices.length; i++) {
      int index = selectionIndices[i] - i;
      newItems.remove(index);
    }

    /* Save & Refresh */
    int[] newItemsRaw = new int[newItems.size()];
    for (int i = 0; i < newItems.size(); i++)
      newItemsRaw[i] = newItems.get(i).ordinal();

    fPreferences.putIntegers(DefaultPreferences.TOOLBAR_ITEMS, newItemsRaw);
    fItemViewer.refresh();

    /* Update Selection */
    int tableItemCount = fItemViewer.getTable().getItemCount();
    if (selectionIndices.length > 0 && selectionIndices[0] < tableItemCount)
      fItemViewer.getTable().setSelection(selectionIndices[0]);
    else if (tableItemCount > 0)
      fItemViewer.getTable().setSelection(tableItemCount - 1);

    /* Update Buttons */
    updateButtonEnablement();
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
    int[] defaultItemsState = defaultScope.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
    int defaultMode = defaultScope.getInteger(DefaultPreferences.TOOLBAR_MODE);

    fPreferences.putIntegers(DefaultPreferences.TOOLBAR_ITEMS, defaultItemsState);
    fPreferences.putInteger(DefaultPreferences.TOOLBAR_MODE, defaultMode);
    fItemViewer.refresh();
    fModeViewer.setSelection(new StructuredSelection(CoolBarMode.values()[defaultMode]));
    updateButtonEnablement();
  }

  private void updateButtonEnablement() {
    boolean enableMoveUp = true;
    boolean enableMoveDown = true;
    int[] selectionIndices = fItemViewer.getTable().getSelectionIndices();
    if (selectionIndices.length == 1) {
      enableMoveUp = selectionIndices[0] != 0;
      enableMoveDown = selectionIndices[0] != fItemViewer.getTable().getItemCount() - 1;
    } else {
      enableMoveUp = false;
      enableMoveDown = false;
    }

    fMoveUpButton.setEnabled(enableMoveUp);
    fMoveDownButton.setEnabled(enableMoveDown);
    fRemoveButton.setEnabled(!fItemViewer.getSelection().isEmpty());
  }

  private void onMove(ToolBarItem source, ToolBarItem destination, int location) {

    /* Find Source and Destination Index */
    int sourceIndex = -1;
    int destinationIndex = -1;
    TableItem[] tableItems = fItemViewer.getTable().getItems();
    for (int i = 0; i < tableItems.length; i++) {
      if (source.equals(tableItems[i].getData()))
        sourceIndex = i;
      else if (destination.equals(tableItems[i].getData()))
        destinationIndex = i;
    }

    /* Return if Invalid */
    if (sourceIndex == -1 || destinationIndex == -1)
      return;

    /* Remember Selection */
    ISelection selection = fItemViewer.getSelection();

    /* Determine Location */
    int newLocation = (sourceIndex < destinationIndex) ? destinationIndex : destinationIndex + 1;
    if (location == ViewerDropAdapter.LOCATION_BEFORE)
      newLocation--;

    /* Move using Viewer Api */
    fItemViewer.remove(source);
    fItemViewer.insert(source, newLocation);

    /* Save */
    tableItems = fItemViewer.getTable().getItems();
    int[] items = new int[tableItems.length];
    for (int i = 0; i < tableItems.length; i++) {
      items[i] = ((ToolBarItem) tableItems[i].getData()).item.ordinal();
    }

    fPreferences.putIntegers(DefaultPreferences.TOOLBAR_ITEMS, items);

    /* Restore Selection */
    fItemViewer.setSelection(selection);

    /* Update Buttons */
    updateButtonEnablement();
  }

  private void onMove(boolean up) {
    TableItem[] items = fItemViewer.getTable().getItems();

    int[] toolbarItemIds = fPreferences.getIntegers(DefaultPreferences.TOOLBAR_ITEMS);
    int selectedIndex = fItemViewer.getTable().getSelectionIndex();

    /* Move Up */
    if (up && selectedIndex > 0) {
      int order1 = toolbarItemIds[selectedIndex];
      int order2 = toolbarItemIds[selectedIndex - 1];
      toolbarItemIds[selectedIndex] = order2;
      toolbarItemIds[selectedIndex - 1] = order1;
    }

    /* Move Down */
    else if (!up && selectedIndex < items.length - 1) {
      int order1 = toolbarItemIds[selectedIndex];
      int order2 = toolbarItemIds[selectedIndex + 1];
      toolbarItemIds[selectedIndex] = order2;
      toolbarItemIds[selectedIndex + 1] = order1;
    }

    /* Save & Refresh */
    fPreferences.putIntegers(DefaultPreferences.TOOLBAR_ITEMS, toolbarItemIds);
    fItemViewer.refresh();

    /* Update Selection */
    fItemViewer.getTable().setSelection(up ? selectedIndex - 1 : selectedIndex + 1);

    /* Update Buttons */
    updateButtonEnablement();
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