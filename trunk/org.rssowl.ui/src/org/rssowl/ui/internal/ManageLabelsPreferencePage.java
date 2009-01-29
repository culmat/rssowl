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

package org.rssowl.ui.internal;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.SearchHit;
import org.rssowl.ui.internal.dialogs.ConfirmDialog;
import org.rssowl.ui.internal.dialogs.NewsFiltersListDialog;
import org.rssowl.ui.internal.util.ColorPicker;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class ManageLabelsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of the Page */
  public static final String ID = "org.rssowl.ui.ManageLabels";

  private LocalResourceManager fResources;
  private TreeViewer fViewer;
  private Button fMoveDownButton;
  private Button fMoveUpButton;
  private IModelSearch fModelSearch = Owl.getPersistenceService().getModelSearch();
  private ISearchField fLabelField = Owl.getModelFactory().createSearchField(INews.LABEL, INews.class.getName());

  /* Supported Modes of the Label Dialog */
  private enum DialogMode {

    /** Add Label */
    ADD,

    /** Edit Label */
    EDIT,
  };

  /* A dialog to add or edit Labels */
  class LabelDialog extends Dialog {
    private final DialogMode fMode;
    private final ILabel fExistingLabel;
    private Text fNameInput;
    private String fName;
    private Collection<ILabel> fAllLabels;
    private ColorPicker fColorPicker;

    LabelDialog(Shell parentShell, DialogMode mode, ILabel label) {
      super(parentShell);
      fMode = mode;
      fExistingLabel = label;
      fAllLabels = CoreUtils.loadSortedLabels();
    }

    String getName() {
      return fName;
    }

    RGB getColor() {
      return fColorPicker.getColor();
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(LayoutUtils.createGridLayout(2, 10, 10));
      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      /* Label */
      Label label = new Label(composite, SWT.None);
      label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

      switch (fMode) {
        case ADD:
          label.setText("Please enter the name and color for the new Label:");
          break;
        case EDIT:
          label.setText("Please update the name and color for this Label:");
          break;
      }

      /* Name */
      if (fMode == DialogMode.ADD || fMode == DialogMode.EDIT) {
        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setText("Name: ");

        fNameInput = new Text(composite, SWT.BORDER | SWT.SINGLE);
        fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        if (fExistingLabel != null) {
          fNameInput.setText(fExistingLabel.getName());
          fNameInput.selectAll();
          fNameInput.setFocus();
        }

        fNameInput.addModifyListener(new ModifyListener() {
          public void modifyText(ModifyEvent e) {
            onModifyName();
          }
        });

        /* Add auto-complete for Labels taken from existing Categories */
        final Pair<SimpleContentProposalProvider, ContentProposalAdapter> pair = OwlUI.hookAutoComplete(fNameInput, null, true);

        /* Load proposals in the Background */
        JobRunner.runDelayedInBackgroundThread(new Runnable() {
          public void run() {
            if (!fNameInput.isDisposed()) {
              Set<String> values = DynamicDAO.getDAO(ICategoryDAO.class).loadAllNames();

              /* Apply Proposals */
              if (!fNameInput.isDisposed())
                OwlUI.applyAutoCompleteProposals(values, pair.getFirst(), pair.getSecond());
            }
          }
        });
      }

      /* Color */
      if (fMode == DialogMode.ADD || fMode == DialogMode.EDIT) {
        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setText("Color: ");

        fColorPicker = new ColorPicker(composite, SWT.FLAT);
        if (fExistingLabel != null)
          fColorPicker.setColor(OwlUI.getRGB(fExistingLabel));
      }

      return composite;
    }

    private void onModifyName() {
      boolean labelExists = false;

      for (ILabel label : fAllLabels) {
        if (label.getName().equals(fNameInput.getText()) && label != fExistingLabel) {
          labelExists = true;
          break;
        }
      }

      getButton(IDialogConstants.OK_ID).setEnabled(!labelExists && fNameInput.getText().length() > 0);
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {

      /* Spacer */
      new Label(parent, SWT.None).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

      Control control = super.createButtonBar(parent);

      /* Udate enablement */
      getButton(IDialogConstants.OK_ID).setEnabled(fNameInput.getText().length() > 0);

      return control;
    }

    /*
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);

      switch (fMode) {
        case ADD:
          newShell.setText("New Label");
          break;
        case EDIT:
          newShell.setText("Edit Label");
          break;
      }
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
      if (fNameInput != null)
        fName = fNameInput.getText();

      super.okPressed();
    }
  }

  /** Leave for reflection */
  public ManageLabelsPreferencePage() {
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createContainer(parent);

    /* Label */
    Label infoLabel = new Label(container, SWT.None);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    infoLabel.setText("Labels can be used to categorize and prioritize your news.");

    /* Label Viewer */
    createViewer(container);

    /* Button Box */
    createButtons(container);

    /* Info Container */
    Composite infoContainer = new Composite(container, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif"));
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    Link infoText = new Link(infoContainer, SWT.WRAP);
    infoText.setText("You can automatically assign labels to news by adding <a>News Filters</a>.");
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    infoText.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        new NewsFiltersListDialog(getShell()).open();
      }
    });

    return container;
  }

  private void createButtons(Composite container) {
    Composite buttonBox = new Composite(container, SWT.None);
    buttonBox.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

    Button addButton = new Button(buttonBox, SWT.PUSH);
    addButton.setText("&New...");
    setButtonLayoutData(addButton);
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd();
      }
    });

    final Button editButton = new Button(buttonBox, SWT.PUSH);
    editButton.setText("&Edit...");
    editButton.setEnabled(!fViewer.getSelection().isEmpty());
    setButtonLayoutData(editButton);
    editButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onEdit();
      }
    });

    final Button deleteButton = new Button(buttonBox, SWT.PUSH);
    deleteButton.setText("&Delete");
    deleteButton.setEnabled(!fViewer.getSelection().isEmpty());
    setButtonLayoutData(deleteButton);
    deleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onDelete();
      }
    });

    Label sep = new Label(buttonBox, SWT.SEPARATOR | SWT.HORIZONTAL);
    sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Move Label Up */
    fMoveUpButton = new Button(buttonBox, SWT.PUSH);
    fMoveUpButton.setText("Move &Up");
    fMoveUpButton.setEnabled(false);
    setButtonLayoutData(fMoveUpButton);
    fMoveUpButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onMove(true);
      }
    });

    /* Move Label Down */
    fMoveDownButton = new Button(buttonBox, SWT.PUSH);
    fMoveDownButton.setText("Move &Down");
    fMoveDownButton.setEnabled(false);
    setButtonLayoutData(fMoveDownButton);
    fMoveDownButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onMove(false);
      }
    });

    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        editButton.setEnabled(!event.getSelection().isEmpty());
        deleteButton.setEnabled(!event.getSelection().isEmpty());
        updateMoveEnablement();
      }
    });
  }

  private void onMove(boolean up) {
    TreeItem[] items = fViewer.getTree().getItems();
    List<ILabel> sortedLabels = new ArrayList<ILabel>(items.length);
    for (TreeItem item : items) {
      sortedLabels.add((ILabel) item.getData());
    }

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    ILabel selectedLabel = (ILabel) selection.getFirstElement();
    int selectedLabelOrder = selectedLabel.getOrder();
    ILabel otherLabel = null;
    int index = sortedLabels.indexOf(selectedLabel);

    /* Move Up */
    if (up && index > 0) {
      otherLabel = sortedLabels.get(index - 1);
      selectedLabel.setOrder(otherLabel.getOrder());
      otherLabel.setOrder(selectedLabelOrder);
    }

    /* Move Down */
    else if (!up && index < sortedLabels.size() - 1) {
      otherLabel = sortedLabels.get(index + 1);
      selectedLabel.setOrder(otherLabel.getOrder());
      otherLabel.setOrder(selectedLabelOrder);
    }

    DynamicDAO.getDAO(ILabelDAO.class).saveAll(Arrays.asList(new ILabel[] { selectedLabel, otherLabel }));
    fViewer.refresh();
    updateMoveEnablement();
  }

  private void updateMoveEnablement() {
    boolean enableMoveUp = true;
    boolean enableMoveDown = true;

    TreeItem[] selection = fViewer.getTree().getSelection();
    int[] selectionIndices = new int[selection.length];
    for (int i = 0; i < selection.length; i++)
      selectionIndices[i] = fViewer.getTree().indexOf(selection[i]);

    if (selectionIndices.length == 1) {
      enableMoveUp = selectionIndices[0] != 0;
      enableMoveDown = selectionIndices[0] != fViewer.getTree().getItemCount() - 1;
    } else {
      enableMoveUp = false;
      enableMoveDown = false;
    }

    fMoveUpButton.setEnabled(enableMoveUp);
    fMoveDownButton.setEnabled(enableMoveDown);
  }

  private void onAdd() {
    LabelDialog dialog = new LabelDialog(getShell(), DialogMode.ADD, null);
    if (dialog.open() == IDialogConstants.OK_ID) {
      String name = dialog.getName();
      RGB color = dialog.getColor();

      ILabel newLabel = Owl.getModelFactory().createLabel(null, name);
      newLabel.setColor(color.red + "," + color.green + "," + color.blue);
      newLabel.setOrder(fViewer.getTree().getItemCount());
      DynamicDAO.save(newLabel);

      fViewer.refresh();
      fViewer.setSelection(new StructuredSelection(newLabel));
    }
    fViewer.getTree().setFocus();
  }

  private void onEdit() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      ILabel label = (ILabel) selection.getFirstElement();
      LabelDialog dialog = new LabelDialog(getShell(), DialogMode.EDIT, label);
      if (dialog.open() == IDialogConstants.OK_ID) {
        boolean changed = false;
        String name = dialog.getName();
        RGB color = dialog.getColor();

        if (!label.getName().equals(name)) {
          label.setName(name);
          changed = true;
        }

        String colorStr = color.red + "," + color.green + "," + color.blue;
        if (!label.getColor().equals(colorStr)) {
          label.setColor(colorStr);
          changed = true;
        }

        /* Save Label */
        if (changed) {
          Controller.getDefault().getSavedSearchService().forceQuickUpdate();
          DynamicDAO.save(label);
          fViewer.update(label, null);
        }
      }
    }
    fViewer.getTree().setFocus();
  }

  private void onDelete() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      ILabel label = (ILabel) selection.getFirstElement();

      String msg = "Are you sure you want to delete the Label '" + label.getName() + "'?";
      ConfirmDialog dialog = new ConfirmDialog(getShell(), "Confirm Delete", "This action can not be undone", msg, null);
      if (dialog.open() == IDialogConstants.OK_ID) {

        /* Remove Label from any News containing it */
        Collection<INews> affectedNews = findNewsWithLabel(label);
        for (INews news : affectedNews) {
          news.removeLabel(label);
        }

        Controller.getDefault().getSavedSearchService().forceQuickUpdate();
        DynamicDAO.saveAll(affectedNews);

        /* Delete Label from DB */
        DynamicDAO.delete(label);
        fViewer.refresh();
      }
    }
    fViewer.getTree().setFocus();
  }

  private Collection<INews> findNewsWithLabel(ILabel label) {
    List<INews> news = new ArrayList<INews>();

    ISearchCondition condition = Owl.getModelFactory().createSearchCondition(fLabelField, SearchSpecifier.IS, label.getName());
    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(Collections.singleton(condition), false);

    for (SearchHit<NewsReference> hit : result) {
      INews newsitem = hit.getResult().resolve();
      if (newsitem != null) {
        Set<ILabel> newsLabels = newsitem.getLabels();
        if (newsLabels != null && newsLabels.contains(label))
          news.add(newsitem);
      }
    }

    return news;
  }

  private void createViewer(Composite container) {
    fViewer = new TreeViewer(container, SWT.FULL_SELECTION | SWT.BORDER | SWT.SINGLE);
    fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTree().setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));

    /* Content Provider */
    fViewer.setContentProvider(new ITreeContentProvider() {
      public Object[] getElements(Object inputElement) {
        return CoreUtils.loadSortedLabels().toArray();
      }

      public Object[] getChildren(Object parentElement) {
        return null;
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object element) {
        return false;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fViewer.setLabelProvider(new CellLabelProvider() {
      @Override
      public void update(ViewerCell cell) {
        ILabel label = (ILabel) cell.getElement();

        /* Text */
        cell.setText(label.getName());

        /* Color */
        cell.setForeground(OwlUI.getColor(fResources, label));
      }
    });

    /* Set dummy Input */
    fViewer.setInput(new Object());

    /* Edit on Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onEdit();
      }
    });
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

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }
}