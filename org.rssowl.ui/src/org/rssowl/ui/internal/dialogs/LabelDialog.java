
package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ColorPicker;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.Collection;
import java.util.Set;

/**
 * A Dialog to add or edit Labels.
 *
 * @author bpasero
 */
public class LabelDialog extends Dialog {
  private final DialogMode fMode;
  private final ILabel fExistingLabel;
  private Text fNameInput;
  private String fName;
  private Collection<ILabel> fAllLabels;
  private ColorPicker fColorPicker;
  private Label fInfoImg;
  private Label fInfoText;
  private ResourceManager fResources = new LocalResourceManager(JFaceResources.getResources());

  /** Supported Modes of the Label Dialog */
  public enum DialogMode {

    /** Add Label */
    ADD,

    /** Edit Label */
    EDIT,
  };

  /**
   * @param parentShell
   * @param mode
   * @param label
   */
  public LabelDialog(Shell parentShell, DialogMode mode, ILabel label) {
    super(parentShell);
    fMode = mode;
    fExistingLabel = label;
    fAllLabels = CoreUtils.loadSortedLabels();
  }

  /**
   * @return the name of the label.
   */
  public String getName() {
    return fName != null ? fName.trim() : null;
  }

  /**
   * @return the color of the label as {@link RGB}.
   */
  public RGB getColor() {
    return fColorPicker.getColor();
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
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 10, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

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
            values= StringUtils.replaceAll(values, ",", " "); // Comma not allowed for Labels

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

    /* Info Container */
    Composite infoContainer = new Composite(composite, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) infoContainer.getLayout()).marginTop = 15;

    fInfoImg = new Label(infoContainer, SWT.NONE);
    fInfoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    fInfoText = new Label(infoContainer, SWT.WRAP);
    fInfoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    showInfo();

    return composite;
  }

  private void showInfo() {
    switch (fMode) {
      case ADD:
        fInfoText.setText("Please enter the name and color for the new Label.");
        break;
      case EDIT:
        fInfoText.setText("Please update the name and color for this Label.");
        break;
    }

    fInfoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif"));
    fInfoImg.getParent().layout();
  }

  private void showError(String msg) {
    fInfoText.setText(msg);
    fInfoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/error.gif"));
    fInfoImg.getParent().layout();
  }

  private void onModifyName() {
    boolean labelExists = false;
    String inputValue = fNameInput.getText().trim().toLowerCase();

    /* Disallow comma in the Name for a Label */
    if (inputValue.contains(",")) {
      getButton(IDialogConstants.OK_ID).setEnabled(false);
      showError("A comma is not allowed in the name for a Label.");
      return;
    }

    /* Check if Label exists already */
    for (ILabel label : fAllLabels) {
      if (label.getName().toLowerCase().equals(inputValue) && label != fExistingLabel) {
        labelExists = true;
        break;
      }
    }

    /* Disable OK Button if Label exists */
    getButton(IDialogConstants.OK_ID).setEnabled(!labelExists && fNameInput.getText().length() > 0);
    if (labelExists)
      showError("A Label with this name already exists.");
    else
      showInfo();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {

    /* Spacer */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

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