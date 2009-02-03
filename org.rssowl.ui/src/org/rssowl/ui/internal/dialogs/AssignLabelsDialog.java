
package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
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
import org.rssowl.core.Owl;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Dialog to assign Labels to a selection of News.
 *
 * @author bpasero
 */
public class AssignLabelsDialog extends Dialog {
  private Text fLabelsInput;
  private final Set<INews> fNews;
  private Set<ILabel> fExistingLabels;
  private ResourceManager fResources = new LocalResourceManager(JFaceResources.getResources());
  private HashSet<String> fExistingLabelNames;
  private Label fInfoImg;
  private Label fInfoText;

  /**
   * @param parentShell
   * @param news
   */
  public AssignLabelsDialog(Shell parentShell, Set<INews> news) {
    super(parentShell);
    fNews = news;
    fExistingLabels = CoreUtils.loadSortedLabels();
    fExistingLabelNames = new HashSet<String>(fExistingLabels.size());
    for (ILabel label : fExistingLabels) {
      fExistingLabelNames.add(label.getName().toLowerCase());
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    String labelsValue = fLabelsInput.getText();
    String[] labelsValueSplit = labelsValue.split(",");

    /* Remove All Labels first */
    for (INews news : fNews) {
      Set<ILabel> newsLabels = news.getLabels();
      for (ILabel newsLabel : newsLabels) {
        news.removeLabel(newsLabel);
      }
    }

    /* Assign New Labels */
    if (labelsValueSplit.length > 0) {

      /* For each typed Label */
      for (String labelValue : labelsValueSplit) {
        ILabel label = null;
        labelValue = labelValue.trim();
        if (labelValue.length() == 0)
          continue;

        /* Check if Label exists */
        for (ILabel existingLabel : fExistingLabels) {
          if (existingLabel.getName().toLowerCase().equals(labelValue.toLowerCase())) {
            label = existingLabel;
            break;
          }
        }

        /* Create new Label if necessary */
        if (label == null) {
          ILabel newLabel = Owl.getModelFactory().createLabel(null, labelValue);
          newLabel.setColor(OwlUI.toString(new RGB(0, 0, 0)));
          newLabel.setOrder(fExistingLabels.size());
          DynamicDAO.save(newLabel);
          fExistingLabels.add(newLabel);
          label = newLabel;
        }

        /* Add Label to all News */
        for (INews news : fNews) {
          news.addLabel(label);
        }
      }
    }

    /* Save News */
    DynamicDAO.saveAll(fNews);

    super.okPressed();
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
    Label nameLabel = new Label(composite, SWT.NONE);
    nameLabel.setText("Labels: ");

    fLabelsInput = new Text(composite, SWT.BORDER | SWT.SINGLE);
    fLabelsInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fLabelsInput.setText(getLabelsValue());
    fLabelsInput.setSelection(fLabelsInput.getText().length());
    fLabelsInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        onModifyName();
      }
    });

    /* Add auto-complete for Labels taken from existing Categories */
    TextContentAdapter adapter = new TextContentAdapter() {
      @Override
      public String getControlContents(Control control) {
        String text = fLabelsInput.getText();
        int selectionOffset = fLabelsInput.getSelection().x;
        if (selectionOffset == 0)
          return "";

        int previousCommaIndex = getPreviousCommaIndex(text, selectionOffset);

        /* No Previous Comma Found - Return from Beginning */
        if (previousCommaIndex == -1)
          return text.substring(0, selectionOffset).trim();

        /* Previous Comma Found - Return from Comma */
        return text.substring(previousCommaIndex + 1, selectionOffset).trim();
      }

      private int getPreviousCommaIndex(String text, int selectionOffset) {
        int previousCommaIndex = -1;
        for (int i = 0; i < text.length(); i++) {
          if (i == selectionOffset)
            break;

          if (text.charAt(i) == ',')
            previousCommaIndex = i;
        }
        return previousCommaIndex;
      }

      private int getNextCommaIndex(String text, int selectionOffset) {
        int nextCommaIndex = -1;
        for (int i = selectionOffset + 1; i < text.length(); i++) {
          if (text.charAt(i) == ',')
            return i;
        }
        return nextCommaIndex;
      }

      @Override
      public void insertControlContents(Control control, String textToInsert, int cursorPosition) {
        String text = fLabelsInput.getText();

        int selectionOffset = fLabelsInput.getSelection().x;
        int previousCommaIndex = getPreviousCommaIndex(text, selectionOffset);
        int nextCommaIndex = getNextCommaIndex(text, selectionOffset);

        /* Replace All: No Comma Found */
        if (previousCommaIndex == -1 && nextCommaIndex == -1) {
          text = textToInsert + ", ";
        }

        /* Replace All beginning with Previous Comma  */
        else if (previousCommaIndex != -1 && nextCommaIndex == -1) {
          text = text.substring(0, previousCommaIndex);
          text = text + ", " + textToInsert + ", ";
        }

        /* Replace all from beginning till Next Comma */
        else if (previousCommaIndex == -1 && nextCommaIndex != -1) {
          text = textToInsert + text.substring(nextCommaIndex);
        }

        /* Replace all from previous Comma till next Comma */
        else {
          String leftHand = text.substring(0, previousCommaIndex);
          String rightHand = text.substring(nextCommaIndex);

          text = leftHand + ", " + textToInsert + rightHand;
        }

        fLabelsInput.setText(text);
        fLabelsInput.setSelection(fLabelsInput.getText().length());
      }
    };

    final Pair<SimpleContentProposalProvider, ContentProposalAdapter> pair = OwlUI.hookAutoComplete(fLabelsInput, adapter, null, true);
    pair.getSecond().setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);

    /* Load proposals in the Background */
    JobRunner.runDelayedInBackgroundThread(new Runnable() {
      public void run() {
        if (!fLabelsInput.isDisposed()) {
          Set<String> values = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
              return o1.compareToIgnoreCase(o2);
            }
          });

          Set<String> categoryNames = DynamicDAO.getDAO(ICategoryDAO.class).loadAllNames();
          categoryNames = StringUtils.replaceAll(categoryNames, ",", " "); // Comma not allowed for Labels

          values.addAll(categoryNames);

          /* Apply Proposals */
          if (!fLabelsInput.isDisposed())
            OwlUI.applyAutoCompleteProposals(values, pair.getFirst(), pair.getSecond());
        }
      }
    });

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

  private String getLabelsValue() {

    /* Sort by Sort Key to respect order */
    Set<ILabel> labels = new TreeSet<ILabel>(new Comparator<ILabel>() {
      public int compare(ILabel l1, ILabel l2) {
        if (l1.equals(l2))
          return 0;

        return l1.getOrder() < l2.getOrder() ? -1 : 1;
      }
    });

    for (INews news : fNews) {
      Set<ILabel> newsLabels = news.getLabels();
      labels.addAll(newsLabels);
    }

    StringBuilder str = new StringBuilder();
    for (ILabel label : labels) {
      str.append(label.getName()).append(", ");
    }

    if (str.length() > 0)
      str = str.delete(str.length() - 2, str.length());
    return str.toString();
  }

  private void showWarning(String msg) {
    fInfoText.setText(msg);
    fInfoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/warning.gif"));
    fInfoImg.getParent().layout();
  }

  private void showInfo() {
    fInfoText.setText("Separate Labels with commas. New Labels will be created if not yet existing.");
    fInfoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif"));
    fInfoImg.getParent().layout();
  }

  private void onModifyName() {
    int newLabelCounter = 0;
    String labelsValue = fLabelsInput.getText();
    String[] labelsValueSplit = labelsValue.split(",");
    Set<String> handledNewLabels = new HashSet<String>(1);
    for (String labelValue : labelsValueSplit) {
      labelValue = labelValue.trim().toLowerCase();
      if (labelValue.length() > 0 && !handledNewLabels.contains(labelValue) && !fExistingLabelNames.contains(labelValue)) {
        newLabelCounter++;
        handledNewLabels.add(labelValue.toLowerCase());
      }
    }

    if (newLabelCounter == 0)
      showInfo();
    else if (newLabelCounter == 1)
      showWarning("One new Label will be created.");
    else
      showWarning(newLabelCounter + " new Labels will be created.");
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createButtonBar(Composite parent) {

    /* Spacer */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

    return super.createButtonBar(parent);
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Assign Labels");
  }
}