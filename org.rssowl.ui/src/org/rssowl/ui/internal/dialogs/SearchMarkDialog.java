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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.PersistenceLayer;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.search.ISearchCondition;
import org.rssowl.core.model.search.ISearchField;
import org.rssowl.core.model.search.SearchSpecifier;
import org.rssowl.core.model.types.IEntity;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.core.model.types.IModelTypesFactory;
import org.rssowl.core.model.types.INews;
import org.rssowl.core.model.types.ISearchMark;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.FolderChooser;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO This is a rough-Action which is not polished or optimized and only for
 * developers purposes!
 *
 * @author bpasero
 */
public class SearchMarkDialog extends TitleAreaDialog {

  /* Section for Dialogs Settings */
  private static final String SETTINGS_SECTION = "org.rssowl.ui.internal.actions.NewSearchMarkAction";

  private Text fNameInput;
  private SearchConditionList fSearchConditionList;
  private Button fMatchAnyRadio;
  private Button fMatchAllRadio;
  private LocalResourceManager fResources;
  private IDialogSettings fDialogSettings;
  private boolean fFirstTimeOpen;
  private IFolder fParent;
  private PersistenceLayer fPersist;
  private List<ISearchCondition> fInitialSearchConditions;
  private boolean fInitialMatchAllConditions;
  private FolderChooser fFolderChooser;

  /**
   * @param shell
   * @param parent
   */
  public SearchMarkDialog(Shell shell, IFolder parent) {
    this(shell, parent, null, false);
  }

  /**
   * @param shell
   * @param parent
   * @param conditions
   * @param matchAllConditions
   */
  public SearchMarkDialog(Shell shell, IFolder parent, List<ISearchCondition> conditions, boolean matchAllConditions) {
    super(shell);
    fParent = parent;
    fInitialMatchAllConditions = matchAllConditions;
    fInitialSearchConditions = conditions;
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fDialogSettings = Activator.getDefault().getDialogSettings();
    fFirstTimeOpen = (fDialogSettings.getSection(SETTINGS_SECTION) == null);
    fPersist = NewsModel.getDefault().getPersistenceLayer();

    /* Use default Parent if required */
    if (fParent == null)
      fParent = getDefaultParent();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {

    /* Make sure Conditions are provided */
    if (fSearchConditionList.isEmpty()) {
      setErrorMessage("Please specify your search by defining some conditions below.");
      return;
    }

    /* Get selected Folder */
    fParent = fFolderChooser.getFolder();

    /* Create new Searchmark */
    ISearchMark searchMark = NewsModel.getDefault().getTypesFactory().createSearchMark(null, fParent, fNameInput.getText());
    searchMark.setMatchAllConditions(fMatchAllRadio.getSelection());

    /* Create Conditions and save in DB */
    fSearchConditionList.createConditions(searchMark);
    fPersist.getModelDAO().saveFolder(fParent);

    super.okPressed();
  }

  private IFolder getDefaultParent() {
    Long selectedRootFolderID = fPersist.getPreferencesDAO().getLong(BookMarkExplorer.PREF_SELECTED_BOOKMARK_SET);
    return new FolderReference(selectedRootFolderID).resolve();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#close()
   */
  @Override
  public boolean close() {
    boolean res = super.close();
    fResources.dispose();

    return res;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#getDialogBoundsSettings()
   */
  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    IDialogSettings section = fDialogSettings.getSection(SETTINGS_SECTION);
    if (section != null)
      return section;

    return fDialogSettings.addNewSection(SETTINGS_SECTION);
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    newShell.setText("New Saved Search");
    super.configureShell(newShell);
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    return super.getShellStyle() | SWT.RESIZE | SWT.MAX;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title Image */
    setTitleImage(RSSOwlUI.getImage(fResources, "icons/elcl16/search.gif"));

    /* Title Message */
    setMessage("You can use \'?\' for any character and \'*\' for any word in your search.", IMessageProvider.INFORMATION);

    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 5, 10, 5, 5, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Label nameLabel = new Label(container, SWT.NONE);
    nameLabel.setText("Name: ");

    fNameInput = new Text(container, SWT.SINGLE | SWT.BORDER);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fNameInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });

    Label folderLabel = new Label(container, SWT.NONE);
    folderLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    folderLabel.setText("Location: ");

    /* Folder Chooser */
    fFolderChooser = new FolderChooser(container, fParent, SWT.BORDER);
    fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
    fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    Composite radioContainer = new Composite(container, SWT.None);
    radioContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    radioContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 0));
    ((GridLayout) radioContainer.getLayout()).marginTop = 10;

    fMatchAllRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAllRadio.setText("Match all conditions");
    fMatchAllRadio.setSelection(fInitialMatchAllConditions);

    fMatchAnyRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any condition");
    fMatchAnyRadio.setSelection(!fInitialMatchAllConditions);

    Composite conditionsContainer = new Composite(container, SWT.BORDER);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(2));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);

    /* Search Conditions List */
    if (fInitialSearchConditions == null)
      fInitialSearchConditions = getDefaultConditions();
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, fInitialSearchConditions);
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fSearchConditionList.setVisibleItemCount(3);

    return container;
  }

  private List<ISearchCondition> getDefaultConditions() {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    IModelTypesFactory factory = NewsModel.getDefault().getTypesFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS, "");
    conditions.add(condition);

    field = factory.createSearchField(INews.STATE, INews.class.getName());
    condition = factory.createSearchCondition(field, SearchSpecifier.IS, INews.State.NEW);
    conditions.add(condition);

    field = factory.createSearchField(INews.STATE, INews.class.getName());
    condition = factory.createSearchCondition(field, SearchSpecifier.IS, INews.State.UNREAD);
    conditions.add(condition);

    field = factory.createSearchField(INews.STATE, INews.class.getName());
    condition = factory.createSearchCondition(field, SearchSpecifier.IS, INews.State.UPDATED);
    conditions.add(condition);

    return conditions;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    super.createButtonsForButtonBar(parent);
    getButton(IDialogConstants.OK_ID).setEnabled(false);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

    if (fFirstTimeOpen) {
      Point bestSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
      getShell().setSize(bestSize.x, bestSize.y);
      LayoutUtils.positionShell(getShell(), false);
    }
  }

  private void validateInput() {
    boolean valid = fNameInput.getText().length() > 0;
    Control button = getButton(IDialogConstants.OK_ID);
    button.setEnabled(valid);
  }
}