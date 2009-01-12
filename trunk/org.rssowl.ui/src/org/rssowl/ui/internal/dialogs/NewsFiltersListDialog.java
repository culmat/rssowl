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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ISearchFilterDAO;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTable;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.CColumnLayoutData.Size;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A dialog to manage news filters in RSSOwl. The dialog allows to add, edit and
 * delete filters as well as moving them up or down to define an order of
 * filters to apply.
 *
 * @author bpasero
 */
public class NewsFiltersListDialog extends TitleAreaDialog {
  private LocalResourceManager fResources;
  private CheckboxTableViewer fViewer;
  private Button fEditButton;
  private Button fDeleteButton;
  private Button fMoveDownButton;
  private Button fMoveUpButton;
  private Image fFilterIcon;
  private ISearchFilterDAO fSearchFilterDao;

  /**
   * @param parentShell
   */
  public NewsFiltersListDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fFilterIcon = OwlUI.getImage(fResources, "icons/obj16/news_filter.gif");
    fSearchFilterDao = DynamicDAO.getDAO(ISearchFilterDAO.class);
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

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/filter_wiz.gif"));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Composite tableContainer = new Composite(composite, SWT.NONE);
    tableContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    CTable cTable = new CTable(tableContainer, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    fViewer = new CheckboxTableViewer(cTable.getControl());
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    fViewer.getTable().setHeaderVisible(true);
    ((GridData) fViewer.getTable().getLayoutData()).heightHint = fViewer.getTable().getItemHeight() * 15;

    TableColumn nameCol = new TableColumn(fViewer.getTable(), SWT.NONE);

    CColumnLayoutData data = new CColumnLayoutData(Size.FILL, 100);
    cTable.manageColumn(nameCol, data, "Name", null, false, false);

    /* ContentProvider returns all filters */
    fViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return fSearchFilterDao.loadAll().toArray();
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    fViewer.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ISearchFilter) element).getName();
      }

      @Override
      public Image getImage(Object element) {
        return fFilterIcon;
      }
    });

    /* Sort */
    fViewer.setComparator(new ViewerComparator() {
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        ISearchFilter filter1 = (ISearchFilter) e1;
        ISearchFilter filter2 = (ISearchFilter) e2;

        return (filter1.getOrder() < filter2.getOrder() ? -1 : (filter1.getOrder() == filter2.getOrder() ? 0 : 1));
      }
    });

    /* Selection */
    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        fEditButton.setEnabled(!event.getSelection().isEmpty());
        fDeleteButton.setEnabled(!event.getSelection().isEmpty());

        updateMoveEnablement();
      }
    });

    /* Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onEdit();
      }
    });

    /* Set input (ignored by ContentProvider anyways) */
    fViewer.setInput(this);
    updateCheckedState();

    /* Listen on Check State Changes */
    fViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        ISearchFilter filter = (ISearchFilter) event.getElement();
        filter.setEnabled(event.getChecked());
        fSearchFilterDao.save(filter);
      }
    });

    /* Container for the Buttons to Manage Sets */
    Composite buttonContainer = new Composite(composite, SWT.None);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

    /* Adds a new Filter */
    Button addButton = new Button(buttonContainer, SWT.PUSH);
    addButton.setText("&New...");
    addButton.setFocus();
    setButtonLayoutData(addButton);
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd();
      }
    });

    /* Edits a selected Filter */
    fEditButton = new Button(buttonContainer, SWT.PUSH);
    fEditButton.setText("&Edit...");
    setButtonLayoutData(fEditButton);
    fEditButton.setEnabled(false);
    fEditButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onEdit();
      }
    });

    /* Deletes the selected Filter */
    fDeleteButton = new Button(buttonContainer, SWT.PUSH);
    fDeleteButton.setText("&Delete...");
    setButtonLayoutData(fDeleteButton);
    fDeleteButton.setEnabled(false);
    fDeleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onDelete();
      }
    });

    /* Move Filter Up */
    fMoveUpButton = new Button(buttonContainer, SWT.PUSH);
    fMoveUpButton.setText("Move &Up");
    fMoveUpButton.setEnabled(false);
    setButtonLayoutData(fMoveUpButton);
    ((GridData) fMoveUpButton.getLayoutData()).verticalAlignment = SWT.END;
    ((GridData) fMoveUpButton.getLayoutData()).grabExcessVerticalSpace = true;
    fMoveUpButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onMove(true);
      }
    });

    /* Move Filter Down */
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
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Update Title Message */
    updateTitle();

    return composite;
  }

  private void updateTitle() {
    ISearchFilter problematicFilter= null;

    Table table = fViewer.getTable();
    TableItem[] items = table.getItems();
    for (TableItem item : items) {
      ISearchFilter filter = (ISearchFilter) item.getData();
      if (filter.matchAllNews()) {
        int index = table.indexOf(item);
        if (index < table.getItemCount() - 1) {
          problematicFilter = filter;
          break;
        }
      }
    }

    if (problematicFilter != null)
      setMessage("The filter '" + problematicFilter.getName() + "' matches on all News. Move it to the bottom or\n disable it to support the other filters below.", IMessageProvider.WARNING);
    else
      setMessage("Enabled filters will run automatically in the order shown below.\n If a news matches more than one filter, only the first will be applied.", IMessageProvider.INFORMATION);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
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
    List<ISearchFilter> sortedFilters = new ArrayList<ISearchFilter>(items.length);
    for (TableItem item : items) {
      sortedFilters.add((ISearchFilter) item.getData());
    }

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    ISearchFilter selectedFilter = (ISearchFilter) selection.getFirstElement();
    int selectedFilterOrder = selectedFilter.getOrder();
    ISearchFilter otherFilter = null;
    int index = sortedFilters.indexOf(selectedFilter);

    /* Move Up */
    if (up && index > 0) {
      otherFilter = sortedFilters.get(index - 1);
      selectedFilter.setOrder(otherFilter.getOrder());
      otherFilter.setOrder(selectedFilterOrder);
    }

    /* Move Down */
    else if (!up && index < sortedFilters.size() - 1) {
      otherFilter = sortedFilters.get(index + 1);
      selectedFilter.setOrder(otherFilter.getOrder());
      otherFilter.setOrder(selectedFilterOrder);
    }

    fSearchFilterDao.saveAll(Arrays.asList(new ISearchFilter[] { selectedFilter, otherFilter }));
    fViewer.refresh();
    updateCheckedState();
    updateMoveEnablement();
    updateTitle();
  }

  private void updateCheckedState() {
    TableItem[] items = fViewer.getTable().getItems();
    for (TableItem item : items) {
      ISearchFilter filter = (ISearchFilter) item.getData();
      fViewer.setChecked(filter, filter.isEnabled());
    }
  }

  private void onAdd() {
    NewsFilterDialog dialog = new NewsFilterDialog(getShell());
    Table table = fViewer.getTable();
    dialog.setFilterPosition(table.getItemCount());
    if (dialog.open() == IDialogConstants.OK_ID) {
      fViewer.refresh();
      updateCheckedState();
      fViewer.setSelection(new StructuredSelection(table.getItem(table.getItemCount() - 1).getData()));
      fViewer.getTable().setFocus();
      updateTitle();
    }
  }

  private void onEdit() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    ISearchFilter filter = (ISearchFilter) selection.getFirstElement();

    NewsFilterDialog dialog = new NewsFilterDialog(getShell(), filter);
    if (dialog.open() == IDialogConstants.OK_ID) {
      fViewer.refresh(true);
      fViewer.getTable().setFocus();
      updateTitle();
    }
  }

  private void onDelete() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();

    List<?> selectedFilters = selection.toList();
    ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(getShell(), "Confirm Delete", "This action can not be undone", getMessage(selectedFilters), null);
    if (dialog.open() == IDialogConstants.OK_ID) {
      List<ISearchFilter> filtersToDelete = new ArrayList<ISearchFilter>(selectedFilters.size());
      for (Iterator<?> iterator = selectedFilters.iterator(); iterator.hasNext();) {
        ISearchFilter filter = (ISearchFilter) iterator.next();
        filtersToDelete.add(filter);
      }

      fSearchFilterDao.deleteAll(filtersToDelete);
      fViewer.remove(selection.toArray());
      updateTitle();
    }
  }

  private String getMessage(List<?> elements) {
    StringBuilder message = new StringBuilder("Are you sure you want to delete ");

    /* One Element */
    if (elements.size() == 1) {
      ISearchFilter filter = (ISearchFilter) elements.get(0);
      message.append("the filter '").append(filter.getName()).append("'?");
    }

    /* N Elements */
    else {
      message.append("the selected elements?");
    }

    return message.toString();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("News Filters");
  }
}