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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * Container for all Preferences that have not yet been categorized.
 *
 * @author bpasero
 */
public class MiscPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of this Preferences Page */
  public static final String ID = "org.rssowl.ui.MiscPreferences";

  private IPreferenceScope fGlobalScope;
  private IPreferenceScope fEclipseScope;
  private Button fMinimizeToTray;
  private Button fMoveToTrayOnStart;
  private Button fMoveToTrayOnExit;
  private Spinner fAutoCloseTabsSpinner;
  private Button fAutoCloseTabsCheck;
  private Button fUseMultipleTabsCheck;
  private Button fReopenFeedsOnStartupCheck;
  private Button fAlwaysReuseFeedView;

  /** Leave for reflection */
  public MiscPreferencePage() {
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
    fEclipseScope = Owl.getPreferenceService().getEclipseScope();
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

    /* View Options */
    createViewOptions(container);

    /* System Tray Options */
    createTrayOptions(container);

    return container;
  }

  private void createViewOptions(Composite container) {

    /* View Group */
    Composite viewGroup = new Composite(container, SWT.None);
    viewGroup.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    viewGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fReopenFeedsOnStartupCheck = new Button(viewGroup, SWT.CHECK);
    fReopenFeedsOnStartupCheck.setText("Re-Open last opened feeds on startup");
    fReopenFeedsOnStartupCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS));

    fAlwaysReuseFeedView = new Button(viewGroup, SWT.CHECK);
    fAlwaysReuseFeedView.setText("Always open feeds in the same tab");
    fAlwaysReuseFeedView.setSelection(fGlobalScope.getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW));

    fUseMultipleTabsCheck = new Button(viewGroup, SWT.CHECK);
    fUseMultipleTabsCheck.setText("Show multiple tabs side by side");
    fUseMultipleTabsCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS));

    Composite autoCloseTabsContainer = new Composite(viewGroup, SWT.None);
    autoCloseTabsContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0, 0, 2, false));

    fAutoCloseTabsCheck = new Button(autoCloseTabsContainer, SWT.CHECK);
    fAutoCloseTabsCheck.setText("Never show more than  ");
    fAutoCloseTabsCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS));
    fAutoCloseTabsCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());
        fAlwaysReuseFeedView.setEnabled(!fAutoCloseTabsCheck.getSelection() || fAutoCloseTabsSpinner.getSelection() > 1);
      }
    });

    fAutoCloseTabsSpinner = new Spinner(autoCloseTabsContainer, SWT.BORDER);
    fAutoCloseTabsSpinner.setMinimum(1);
    fAutoCloseTabsSpinner.setMaximum(100);
    fAutoCloseTabsSpinner.setSelection(fEclipseScope.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD));
    fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());
    fAutoCloseTabsSpinner.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fAlwaysReuseFeedView.setEnabled(!fAutoCloseTabsCheck.getSelection() || fAutoCloseTabsSpinner.getSelection() > 1);
      }
    });

    Label label = new Label(autoCloseTabsContainer, SWT.None);
    label.setText(" tabs");

    fAlwaysReuseFeedView.setEnabled(!fAutoCloseTabsCheck.getSelection() || fAutoCloseTabsSpinner.getSelection() > 1);
  }

  private void createTrayOptions(Composite container) {

    /* System Tray Group */
    Composite trayGroup = new Composite(container, SWT.None);
    trayGroup.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    ((GridLayout) trayGroup.getLayout()).marginTop = 5;
    trayGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    Label label = new Label(trayGroup, SWT.NONE);
    label.setText("Move to the System Tray: ");

    /* Enable / Disable Tray */
    fMinimizeToTray = new Button(trayGroup, SWT.CHECK);
    fMinimizeToTray.setText("when minimizing RSSOwl");
    fMinimizeToTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));

    /* Move to Tray on Start */
    fMoveToTrayOnStart = new Button(trayGroup, SWT.CHECK);
    fMoveToTrayOnStart.setText("when starting RSSOwl");
    fMoveToTrayOnStart.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_START));

    /* Move to Tray on Close */
    fMoveToTrayOnExit = new Button(trayGroup, SWT.CHECK);
    fMoveToTrayOnExit.setText("when closing RSSOwl");
    fMoveToTrayOnExit.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));
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
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS, fReopenFeedsOnStartupCheck.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW, fAlwaysReuseFeedView.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS, fUseMultipleTabsCheck.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS, fAutoCloseTabsCheck.getSelection());
    fEclipseScope.putInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, fAutoCloseTabsSpinner.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_MINIMIZE, fMinimizeToTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_START, fMoveToTrayOnStart.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_CLOSE, fMoveToTrayOnExit.getSelection());

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    fReopenFeedsOnStartupCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS));
    fAlwaysReuseFeedView.setSelection(defaultScope.getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW));
    fUseMultipleTabsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS));
    fAutoCloseTabsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS));
    fAutoCloseTabsSpinner.setSelection(defaultScope.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD));
    fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());

    fMinimizeToTray.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));
    fMoveToTrayOnStart.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_START));
    fMoveToTrayOnExit.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));

    fAlwaysReuseFeedView.setEnabled(!fAutoCloseTabsCheck.getSelection() || fAutoCloseTabsSpinner.getSelection() > 1);
  }
}