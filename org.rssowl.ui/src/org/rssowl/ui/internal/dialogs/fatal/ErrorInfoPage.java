/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.dialogs.fatal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.CustomWizardDialog;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * Part of the {@link FatalErrorWizard} to give information on the fatal error.
 *
 * @author bpasero
 */
public class ErrorInfoPage extends WizardPage {
  private final IStatus fErrorStatus;
  private Menu fCopyMenu;
  private final boolean fHasBackups;

  ErrorInfoPage(String pageName, IStatus errorStatus, boolean hasBackups) {
    super(pageName, pageName, null);
    fErrorStatus = errorStatus;
    fHasBackups = hasBackups;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {

    /* Title Image and Message */
    setImageDescriptor(OwlUI.getImageDescriptor("icons/wizban/welcome_wiz.gif")); //$NON-NLS-1$
    setMessage(Messages.ErrorInfoPage_RSSOWL_CRASH, IMessageProvider.WARNING);

    /* Container */
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 10, 5, false));

    /* Crash Report Label */
    Link dialogMessageLabel = new Link(container, SWT.WRAP);
    dialogMessageLabel.setText(Messages.ErrorInfoPage_SEND_LOGS_ADVISE);
    dialogMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    dialogMessageLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if ("save".equals(e.text)) //$NON-NLS-1$
          OwlUI.saveCrashReport(getShell());
        else
          BrowserUtils.sendErrorLog();
      }
    });

    /* Error Details Label */
    if (fErrorStatus != null && StringUtils.isSet(fErrorStatus.getMessage())) {
      Label reasonLabel = new Label(container, SWT.NONE);
      reasonLabel.setText(Messages.ErrorInfoPage_ERROR_DETAILS);
      reasonLabel.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
      reasonLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
      ((GridData) reasonLabel.getLayoutData()).verticalIndent = 10;

      Label errorDetailsLabel = new Label(container, SWT.WRAP);
      errorDetailsLabel.setText(fErrorStatus.getMessage());
      errorDetailsLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      ((GridData) errorDetailsLabel.getLayoutData()).widthHint = 200;
      ((GridData) errorDetailsLabel.getLayoutData()).verticalIndent = 10;

      fCopyMenu = new Menu(errorDetailsLabel.getShell(), SWT.POP_UP);
      MenuItem copyItem = new MenuItem(fCopyMenu, SWT.PUSH);
      copyItem.setText(Messages.ErrorInfoPage_COPY);
      copyItem.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          OwlUI.getClipboard(fCopyMenu.getDisplay()).setContents(new Object[] { fErrorStatus.getMessage() }, new Transfer[] { TextTransfer.getInstance() });
        }
      });
      errorDetailsLabel.setMenu(fCopyMenu);
    }

    /* Recovery Label */
    Link moreInfoLabel = new Link(container, SWT.WRAP);
    if (fHasBackups)
      moreInfoLabel.setText(Messages.ErrorInfoPage_NEXT_PAGE_ADVISE);
    else
      moreInfoLabel.setText(Messages.ErrorInfoPage_GENERAL_ERROR_ADVISE);
    moreInfoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    ((GridData) moreInfoLabel.getLayoutData()).widthHint = 200;
    if (fErrorStatus != null && StringUtils.isSet(fErrorStatus.getMessage()))
      ((GridData) moreInfoLabel.getLayoutData()).verticalIndent = 10;

    moreInfoLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if ("faq".equals(e.text)) //$NON-NLS-1$
          BrowserUtils.openFAQ(fErrorStatus);
        else if ("forum".equals(e.text)) //$NON-NLS-1$
          BrowserUtils.openHelpForum(fErrorStatus);
      }
    });

    Dialog.applyDialogFont(container);

    setControl(container);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    /* Transfer Focus to Buttons, otherwise a link is focussed which looks weird */
    if (visible) {
      Button focusButton = ((CustomWizardDialog) getContainer()).getButton(IDialogConstants.NEXT_ID);
      if (focusButton == null)
        focusButton = ((CustomWizardDialog) getContainer()).getButton(IDialogConstants.FINISH_ID);

      if (focusButton != null)
        focusButton.setFocus();
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    if (fCopyMenu != null && !fCopyMenu.isDisposed())
      fCopyMenu.dispose();
  }
}