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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.update.core.ICategory;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * The <code>UpdateDialog</code> shows when updates are available and will show
 * a description of the update.
 *
 * @author bpasero
 */
public class UpdateDialog extends TitleAreaDialog {
  private LocalResourceManager fResources;
  private final IInstallFeatureOperation[] fUpdates;
  private StyledText fUpdateInfoTextRight;
  private StyledText fUpdateInfoTextLeft;
  private StyledText fUpdateInfoTextBottom;
  private Button fUpdateOnStartupCheck;
  private IPreferenceScope fPreferences;

  /**
   * @param parentShell
   * @param updates
   */
  public UpdateDialog(Shell parentShell, IInstallFeatureOperation[] updates) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fUpdates = updates;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    fPreferences.putBoolean(DefaultPreferences.UPDATE_ON_STARTUP, fUpdateOnStartupCheck.getSelection());
    super.okPressed();
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#close()
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
    shell.setText(Messages.UpdateDialog_UPDATE_AVAILABLE);
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title */
    setTitle(Messages.UpdateDialog_UPDATE_AVAILABLE);

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/update_wiz.png")); //$NON-NLS-1$

    /* Title Message */
    setMessage(Messages.UpdateDialog_NEW_VERSION_MSG);

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(1, 5, 5));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    Composite updateInfoContainer = new Composite(composite, SWT.BORDER);
    updateInfoContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 5, 0, 5, false));
    updateInfoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    updateInfoContainer.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    fUpdateInfoTextLeft = new StyledText(updateInfoContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    fUpdateInfoTextLeft.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    fUpdateInfoTextLeft.setLineSpacing(5);
    fUpdateInfoTextLeft.setEnabled(false);
    fUpdateInfoTextLeft.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    fUpdateInfoTextRight = new StyledText(updateInfoContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    fUpdateInfoTextRight.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    fUpdateInfoTextRight.setLineSpacing(5);
    fUpdateInfoTextRight.setEnabled(false);
    fUpdateInfoTextRight.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    fUpdateInfoTextBottom = new StyledText(updateInfoContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    fUpdateInfoTextBottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fUpdateInfoTextBottom.setLineSpacing(5);
    fUpdateInfoTextBottom.setEnabled(false);
    fUpdateInfoTextBottom.setBackground(composite.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

    /* Fill in Update Details */
    showUpdateDescription();

    /* Add Option to Control Checking for Updates Automatically */
    fUpdateOnStartupCheck = new Button(composite, SWT.CHECK);
    fUpdateOnStartupCheck.setText(Messages.UpdateDialog_UPDATE_ON_STARTUP);
    fUpdateOnStartupCheck.setSelection(fPreferences.getBoolean(DefaultPreferences.UPDATE_ON_STARTUP));

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    applyDialogFont(composite);

    return composite;
  }

  @SuppressWarnings("deprecation")
  private void showUpdateDescription() {
    String oldVer = null;
    String newVer = null;
    long dlSize = 0;
    String provider = null;
    String description = null;

    if (fUpdates != null && fUpdates.length > 0) {
      IFeature oldFeature = fUpdates[0].getOldFeature();
      IFeature newFeature = fUpdates[0].getFeature();

      /* Versions */
      org.eclipse.core.runtime.PluginVersionIdentifier oldVersion = (oldFeature != null) ? oldFeature.getVersionedIdentifier().getVersion() : null;
      org.eclipse.core.runtime.PluginVersionIdentifier newVersion = newFeature.getVersionedIdentifier().getVersion();
      if (oldVersion != null)
        oldVer = oldVersion.getMajorComponent() + "." + oldVersion.getMinorComponent() + "." + oldVersion.getServiceComponent(); //$NON-NLS-1$ //$NON-NLS-2$
      newVer = newVersion.getMajorComponent() + "." + newVersion.getMinorComponent() + "." + newVersion.getServiceComponent(); //$NON-NLS-1$ //$NON-NLS-2$

      if (newVer.equals(oldVer) && oldVersion != null) {
        newVer += "." + newVersion.getQualifierComponent(); //$NON-NLS-1$
        oldVer += "." + oldVersion.getQualifierComponent(); //$NON-NLS-1$
      }

      /* Other */
      dlSize = newFeature.getDownloadSize() * 1000;
      provider = newFeature.getProvider();

      /* Description */
      ICategory[] categories = newFeature.getSite().getCategories();
      if (categories.length > 0) {
        IURLEntry descriptionEntry = categories[0].getDescription();
        if (descriptionEntry != null)
          description = descriptionEntry.getAnnotation();
      }

      showUpdateDescription(oldVer, newVer, dlSize, provider, description);
    }
  }

  private void showUpdateDescription(String oldVer, String newVer, long dlSize, String provider, String description) {

    /* Old Version */
    if (StringUtils.isSet(oldVer)) {
      int offset = fUpdateInfoTextLeft.getText().length();
      fUpdateInfoTextLeft.append(Messages.UpdateDialog_CURRENT_VERSION);
      fUpdateInfoTextLeft.setStyleRange(new StyleRange(offset, fUpdateInfoTextLeft.getText().length() - offset, null, null, SWT.BOLD));
      fUpdateInfoTextRight.append(oldVer);
      fUpdateInfoTextLeft.append("\n"); //$NON-NLS-1$
      fUpdateInfoTextRight.append("\n"); //$NON-NLS-1$
    }

    /* New Version */
    if (StringUtils.isSet(newVer)) {
      int offset = fUpdateInfoTextLeft.getText().length();
      fUpdateInfoTextLeft.append(Messages.UpdateDialog_NEW_VERSION);
      fUpdateInfoTextLeft.setStyleRange(new StyleRange(offset, fUpdateInfoTextLeft.getText().length() - offset, null, null, SWT.BOLD));
      fUpdateInfoTextRight.append(newVer);
      fUpdateInfoTextLeft.append("\n"); //$NON-NLS-1$
      fUpdateInfoTextRight.append("\n"); //$NON-NLS-1$
    }

    /* Provider */
    if (StringUtils.isSet(provider)) {
      int offset = fUpdateInfoTextLeft.getText().length();
      fUpdateInfoTextLeft.append(Messages.UpdateDialog_UPDATE_PROVIDER);
      fUpdateInfoTextLeft.setStyleRange(new StyleRange(offset, fUpdateInfoTextLeft.getText().length() - offset, null, null, SWT.BOLD));
      fUpdateInfoTextRight.append(provider);
      fUpdateInfoTextLeft.append("\n"); //$NON-NLS-1$
      fUpdateInfoTextRight.append("\n"); //$NON-NLS-1$
    }

    /* Size */
    if (dlSize > 0) {
      int offset = fUpdateInfoTextLeft.getText().length();
      fUpdateInfoTextLeft.append(Messages.UpdateDialog_DL_SIZE);
      fUpdateInfoTextLeft.setStyleRange(new StyleRange(offset, fUpdateInfoTextLeft.getText().length() - offset, null, null, SWT.BOLD));
      fUpdateInfoTextRight.append(OwlUI.getSize((int) dlSize));
      fUpdateInfoTextLeft.append("\n"); //$NON-NLS-1$
      fUpdateInfoTextRight.append("\n"); //$NON-NLS-1$
    }

    /* Description */
    if (StringUtils.isSet(description)) {
      int offset = fUpdateInfoTextBottom.getText().length();
      fUpdateInfoTextBottom.append(Messages.UpdateDialog_DESCRIPTION);
      fUpdateInfoTextBottom.append("\n"); //$NON-NLS-1$
      fUpdateInfoTextBottom.setStyleRange(new StyleRange(offset, fUpdateInfoTextBottom.getText().length() - offset, null, null, SWT.BOLD));
      fUpdateInfoTextBottom.append(description);
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, Messages.UpdateDialog_DOWNLOAD_INSTALL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE | getDefaultOrientation();

    /* Follow Apple's Human Interface Guidelines for Application Modal Dialogs */
    if (!Application.IS_MAC)
      style |= SWT.CLOSE;

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

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