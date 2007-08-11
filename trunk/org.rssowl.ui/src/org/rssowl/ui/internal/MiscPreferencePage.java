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

package org.rssowl.ui.internal;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * Container for all Preferences that have not yet been categorized.
 *
 * @author bpasero
 */
public class MiscPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private IPreferenceScope fGlobalScope;
  private Button fMinimizeToTray;
  private Button fMoveToTrayOnStart;
  private Button fMoveToTrayOnExit;
  private Button fNotificationOnlyFromTray;
  private Button fShowNotificationPopup;
  private Button fNotificationIsSticky;
  private Text fCustomBrowserInput;
  private Button fUseCustomExternalBrowser;
  private Button fUseDefaultExternalBrowser;
  private Button fUseInternalBrowser;
  private Button fCustomBrowserSearchButton;
  private Button fConfirmDeleteNews;
  private Button fLimitNotificationCheck;
  private Spinner fLimitNotificationSpinner;

  /** Leave for reflection */
  public MiscPreferencePage() {
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
  }

  /**
   * @param title
   */
  public MiscPreferencePage(String title) {
    super(title);
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {}

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    /* Browser Group */
    Group browserGroup = new Group(container, SWT.None);
    browserGroup.setText("Browser");
    browserGroup.setLayout(LayoutUtils.createGridLayout(2));
    browserGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Use internal Browser */
    fUseInternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseInternalBrowser.setText("Use internal Browser");
    fUseInternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));

    /* Use default external Browser */
    fUseDefaultExternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseDefaultExternalBrowser.setText("Use default external Browser");
    fUseDefaultExternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fUseDefaultExternalBrowser.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER));

    /* Use custom external Browser */
    fUseCustomExternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseCustomExternalBrowser.setText("Use the following external Browser:");
    fUseCustomExternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fUseCustomExternalBrowser.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER));
    fUseCustomExternalBrowser.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
        fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());
      }
    });

    fUseInternalBrowser.setSelection(!fUseDefaultExternalBrowser.getSelection() && !fUseCustomExternalBrowser.getSelection());

    fCustomBrowserInput = new Text(browserGroup, SWT.BORDER);
    fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    String customBrowserValue = fGlobalScope.getString(DefaultPreferences.CUSTOM_BROWSER_PATH);
    if (customBrowserValue != null)
      fCustomBrowserInput.setText(customBrowserValue);

    fCustomBrowserSearchButton = new Button(browserGroup, SWT.PUSH);
    fCustomBrowserSearchButton.setText("Search...");
    fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserSearchButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFileName(fCustomBrowserInput.getText());
        String path = dialog.open();
        if (path != null)
          fCustomBrowserInput.setText(path);
      }
    });

    /* System Tray Group */
    Group trayGroup = new Group(container, SWT.None);
    trayGroup.setText("System Tray");
    trayGroup.setLayout(LayoutUtils.createGridLayout(1));
    trayGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Label trayLabel = new Label(trayGroup, SWT.NONE);
    trayLabel.setText("Move to the System Tray");

    Composite trayOptionsContainer = new Composite(trayGroup, SWT.NONE);
    trayOptionsContainer.setLayout(LayoutUtils.createGridLayout(1, 10, 0));
    trayOptionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Enable / Disable Tray */
    fMinimizeToTray = new Button(trayOptionsContainer, SWT.CHECK);
    fMinimizeToTray.setText("when minimizing RSSOwl");
    fMinimizeToTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));

    /* Move to Tray on Start */
    fMoveToTrayOnStart = new Button(trayOptionsContainer, SWT.CHECK);
    fMoveToTrayOnStart.setText("when starting RSSOwl");
    fMoveToTrayOnStart.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_START));

    /* Move to Tray on Close */
    fMoveToTrayOnExit = new Button(trayOptionsContainer, SWT.CHECK);
    fMoveToTrayOnExit.setText("when closing RSSOwl");
    fMoveToTrayOnExit.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));

    /* Notification Group */
    Group notificationGroup = new Group(container, SWT.None);
    notificationGroup.setText("Notification Popup");
    notificationGroup.setLayout(LayoutUtils.createGridLayout(1));
    notificationGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Show Notification Popup */
    fShowNotificationPopup = new Button(notificationGroup, SWT.CHECK);
    fShowNotificationPopup.setText("Show notification on incoming news");
    fShowNotificationPopup.setSelection(fGlobalScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP));
    fShowNotificationPopup.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());
        fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());
        fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
        fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.isEnabled() && fLimitNotificationCheck.getSelection());
      }
    });

    /* Limit number of News showing in Notification */
    Composite limitNewsContainer = new Composite(notificationGroup, SWT.None);
    limitNewsContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));

    int notificationLimit = fGlobalScope.getInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE);

    fLimitNotificationCheck = new Button(limitNewsContainer, SWT.CHECK);
    fLimitNotificationCheck.setText("Show a maximum of ");
    fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
    fLimitNotificationCheck.setSelection(notificationLimit >= 0);
    fLimitNotificationCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.getSelection());
      }
    });

    fLimitNotificationSpinner = new Spinner(limitNewsContainer, SWT.BORDER);
    fLimitNotificationSpinner.setMinimum(1);
    fLimitNotificationSpinner.setMaximum(30);
    fLimitNotificationSpinner.setEnabled(fLimitNotificationCheck.isEnabled() && fLimitNotificationCheck.getSelection());
    if (notificationLimit > 0)
      fLimitNotificationSpinner.setSelection(notificationLimit);
    else
      fLimitNotificationSpinner.setSelection(notificationLimit * -1);

    Label label = new Label(limitNewsContainer, SWT.None);
    label.setText(" News inside the notification");

    /* Only from Tray */
    fNotificationOnlyFromTray = new Button(notificationGroup, SWT.CHECK);
    fNotificationOnlyFromTray.setText("Show notification only when window is minimized");
    fNotificationOnlyFromTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());

    /* Sticky Notification Popup */
    fNotificationIsSticky = new Button(notificationGroup, SWT.CHECK);
    fNotificationIsSticky.setText("Leave notification open until closed manually");
    fNotificationIsSticky.setSelection(fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());

    /* Confirmation Group */
    Group confirmationGroup = new Group(container, SWT.None);
    confirmationGroup.setText("Ask for confirmation");
    confirmationGroup.setLayout(LayoutUtils.createGridLayout(1));
    confirmationGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Confirm Delete News */
    fConfirmDeleteNews = new Button(confirmationGroup, SWT.CHECK);
    fConfirmDeleteNews.setText("when deleting News");
    fConfirmDeleteNews.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CONFIRM_DELETE_NEWS));

    return container;
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_MINIMIZE, fMinimizeToTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_START, fMoveToTrayOnStart.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_CLOSE, fMoveToTrayOnExit.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP, fShowNotificationPopup.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED, fNotificationOnlyFromTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP, fNotificationIsSticky.getSelection());

    if (fLimitNotificationCheck.getSelection())
      fGlobalScope.putInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE, fLimitNotificationSpinner.getSelection());
    else
      fGlobalScope.putInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE, fLimitNotificationSpinner.getSelection() * -1);

    fGlobalScope.putBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER, fUseDefaultExternalBrowser.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER, fUseCustomExternalBrowser.getSelection());
    fGlobalScope.putString(DefaultPreferences.CUSTOM_BROWSER_PATH, fCustomBrowserInput.getText());

    fGlobalScope.putBoolean(DefaultPreferences.CONFIRM_DELETE_NEWS, fConfirmDeleteNews.getSelection());

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    fMinimizeToTray.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));
    fMoveToTrayOnStart.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_START));
    fMoveToTrayOnExit.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));

    fShowNotificationPopup.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP));
    fNotificationOnlyFromTray.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());
    fNotificationIsSticky.setSelection(defaultScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());

    int limitNotificationValue = defaultScope.getInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE);
    fLimitNotificationCheck.setSelection(limitNotificationValue >= 0);
    if (limitNotificationValue >= 0)
      fLimitNotificationSpinner.setSelection(limitNotificationValue);
    fLimitNotificationCheck.setEnabled(fShowNotificationPopup.getSelection());
    fLimitNotificationSpinner.setEnabled(fShowNotificationPopup.getSelection());

    fUseDefaultExternalBrowser.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER));
    fUseCustomExternalBrowser.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER));
    fUseInternalBrowser.setSelection(!fUseDefaultExternalBrowser.getSelection() && !fUseDefaultExternalBrowser.getSelection());

    fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());

    fConfirmDeleteNews.setSelection(defaultScope.getBoolean(DefaultPreferences.CONFIRM_DELETE_NEWS));
  }
}