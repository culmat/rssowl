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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
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
  private Button fMoveToTrayOnExit;
  private Button fUseExternalBrowser;
  private Button fNotificationOnlyFromTray;
  private Button fShowNotificationPopup;
  private Button fNotificationIsSticky;

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

    /* System Tray Group */
    Group trayGroup = new Group(container, SWT.None);
    trayGroup.setText("System Tray");
    trayGroup.setLayout(LayoutUtils.createGridLayout(1));
    trayGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Enable / Disable Tray */
    fMinimizeToTray = new Button(trayGroup, SWT.CHECK);
    fMinimizeToTray.setText("Minimize to the system tray");
    fMinimizeToTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_SYSTEM_TRAY));
    fMinimizeToTray.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fMoveToTrayOnExit.setEnabled(fMinimizeToTray.getSelection());
      }
    });

    /* Move to Tray on Close */
    fMoveToTrayOnExit = new Button(trayGroup, SWT.CHECK);
    fMoveToTrayOnExit.setText("Move to the system tray when closing the window");
    fMoveToTrayOnExit.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_EXIT));
    fMoveToTrayOnExit.setEnabled(fMinimizeToTray.getSelection());

    /* Separator */
    new Label(container, SWT.NONE);

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
      }
    });

    /* Only from Tray */
    fNotificationOnlyFromTray = new Button(notificationGroup, SWT.CHECK);
    fNotificationOnlyFromTray.setText("Show notification only when minimized to the system tray");
    fNotificationOnlyFromTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_FROM_TRAY));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());

    /* Sticky Notification Popup */
    fNotificationIsSticky = new Button(notificationGroup, SWT.CHECK);
    fNotificationIsSticky.setText("Leave notification open until closed manually");
    fNotificationIsSticky.setSelection(fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());

    /* Separator */
    new Label(container, SWT.NONE);

    /* Browser Group */
    Group browserGroup = new Group(container, SWT.None);
    browserGroup.setText("Browser");
    browserGroup.setLayout(LayoutUtils.createGridLayout(1));
    browserGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Open Links Internal / External */
    fUseExternalBrowser = new Button(browserGroup, SWT.CHECK);
    fUseExternalBrowser.setText("Use external browser");
    fUseExternalBrowser.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_EXTERNAL_BROWSER));

    /* Separator */
    new Label(container, SWT.NONE);

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
    fGlobalScope.putBoolean(DefaultPreferences.USE_EXTERNAL_BROWSER, fUseExternalBrowser.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.USE_SYSTEM_TRAY, fMinimizeToTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_EXIT, fMoveToTrayOnExit.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP, fShowNotificationPopup.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_FROM_TRAY, fNotificationOnlyFromTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP, fNotificationIsSticky.getSelection());

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    fUseExternalBrowser.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_EXTERNAL_BROWSER));
    fMinimizeToTray.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_SYSTEM_TRAY));
    fMoveToTrayOnExit.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_EXIT));
    fMoveToTrayOnExit.setEnabled(fMinimizeToTray.getSelection());
    fShowNotificationPopup.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP));
    fNotificationOnlyFromTray.setSelection(defaultScope.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_FROM_TRAY));
    fNotificationOnlyFromTray.setEnabled(fShowNotificationPopup.getSelection());
    fNotificationIsSticky.setSelection(defaultScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP));
    fNotificationIsSticky.setEnabled(fShowNotificationPopup.getSelection());
  }
}