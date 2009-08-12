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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * A Dialog to show activity, such as the progress of updating feeds or
 * downloading attachments.
 *
 * @author bpasero
 */
public class ActivityDialog extends TitleAreaDialog {

  /* Keep the visible instance saved */
  private static ActivityDialog fgVisibleInstance;

  /* Minimum Height in DLUs */
  private static final int MIN_DIALOG_HEIGHT_DLU = 160;

  private LocalResourceManager fResources;

  /**
   * @param parentShell
   */
  public ActivityDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /**
   * @return Returns an instance of <code>ActivityDialog</code> or
   * <code>NULL</code> in case no instance is currently open.
   */
  public static ActivityDialog getVisibleInstance() {
    return fgVisibleInstance;
  }

  /*
   * @see org.eclipse.jface.window.Window#open()
   */
  @Override
  public int open() {
    fgVisibleInstance = this;
    return super.open();
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#close()
   */
  @Override
  public boolean close() {
    fgVisibleInstance = null;
    fResources.dispose();
    return super.close();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Activity");
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Control c = super.createContents(parent);

    getButton(IDialogConstants.OK_ID).setFocus();

    return c;
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @SuppressWarnings("restriction")
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title */
    setTitle("Activity");

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/activity_wiz.png"));

    /* Title Message */
    setMessage("Currently running feed updates and downloads.");

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Progress Viewer (TODO This is Internal Eclipse) */
    org.eclipse.ui.internal.progress.DetailedProgressViewer viewer = new org.eclipse.ui.internal.progress.DetailedProgressViewer(composite, SWT.NONE);
    viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    org.eclipse.ui.internal.progress.ProgressViewerContentProvider provider = new org.eclipse.ui.internal.progress.ProgressViewerContentProvider(viewer, false, false);
    viewer.setContentProvider(provider);
    viewer.setInput(org.eclipse.ui.internal.progress.ProgressManager.getInstance());

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    return composite;
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.MIN | SWT.TITLE | SWT.BORDER | getDefaultOrientation();

    /* Follow Apple's Human Interface Guidelines for Application Modal Dialogs */
    if (!Application.IS_MAC)
      style |= SWT.CLOSE;

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
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
    int minHeight = convertVerticalDLUsToPixels(MIN_DIALOG_HEIGHT_DLU);

    /* Required Size */
    Point requiredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    shell.setSize(Math.max(minWidth, requiredSize.x), Math.max(minHeight, requiredSize.y));
    LayoutUtils.positionShell(shell, false);
  }
}