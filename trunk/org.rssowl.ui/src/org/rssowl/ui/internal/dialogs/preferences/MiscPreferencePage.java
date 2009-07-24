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

package org.rssowl.ui.internal.dialogs.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
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
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.List;

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
  private Button fBrowserMaximizedLayoutRadio;
  private Button fVerticalLayoutRadio;
  private Button fClassicLayoutRadio;
  private Button fMinimizeToTray;
  private Button fMoveToTrayOnStart;
  private Button fMoveToTrayOnExit;
  private Spinner fAutoCloseTabsSpinner;
  private Button fAutoCloseTabsCheck;
  private Button fUseMultipleTabsCheck;
  private Button fReopenFeedsOnStartupCheck;
  private Button fAlwaysReuseFeedView;
  private Button fOpenOnSingleClick;

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

    /* Layout Options */
    createLayoutOptions(container);

    /* Tab Options */
    createTabOptions(container);

    /* System Tray Options */
    createTrayOptions(container);

    /* Misc Options */
    createMiscOptions(container);

    return container;
  }

  private void createLayoutOptions(Composite container) {
    boolean browserMaximized = fGlobalScope.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);

    Label label = new Label(container, SWT.NONE);
    label.setText("Layout");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Group */
    Composite group = new Composite(container, SWT.None);
    group.setLayout(LayoutUtils.createGridLayout(3, 7, 3));
    ((GridLayout) group.getLayout()).marginBottom = 5;
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fClassicLayoutRadio = new Button(group, SWT.RADIO);
    fClassicLayoutRadio.setText("Classic View");
    fClassicLayoutRadio.setSelection(!browserMaximized && fGlobalScope.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC));

    fVerticalLayoutRadio = new Button(group, SWT.RADIO);
    fVerticalLayoutRadio.setText("Vertical View");
    fVerticalLayoutRadio.setSelection(!browserMaximized && !fGlobalScope.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC));

    fBrowserMaximizedLayoutRadio = new Button(group, SWT.RADIO);
    fBrowserMaximizedLayoutRadio.setText("Newspaper View");
    fBrowserMaximizedLayoutRadio.setSelection(browserMaximized);
  }

  private void createTabOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("Tabbed Browsing");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Group */
    Composite group = new Composite(container, SWT.None);
    group.setLayout(LayoutUtils.createGridLayout(1, 7, 3));
    ((GridLayout) group.getLayout()).marginBottom = 5;
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fAlwaysReuseFeedView = new Button(group, SWT.CHECK);
    fAlwaysReuseFeedView.setText("Always open feeds in the same tab");
    fAlwaysReuseFeedView.setSelection(fGlobalScope.getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW));

    fUseMultipleTabsCheck = new Button(group, SWT.CHECK);
    fUseMultipleTabsCheck.setText("Show multiple tabs side by side");
    fUseMultipleTabsCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS));

    Composite autoCloseTabsContainer = new Composite(group, SWT.None);
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

    label = new Label(autoCloseTabsContainer, SWT.None);
    label.setText(" tabs");

    fAlwaysReuseFeedView.setEnabled(!fAutoCloseTabsCheck.getSelection() || fAutoCloseTabsSpinner.getSelection() > 1);
  }

  private void createMiscOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("Misc.");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Group */
    Composite miscGroup = new Composite(container, SWT.None);
    miscGroup.setLayout(LayoutUtils.createGridLayout(1, 7, 3));
    miscGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fReopenFeedsOnStartupCheck = new Button(miscGroup, SWT.CHECK);
    fReopenFeedsOnStartupCheck.setText("Reopen last opened feeds on startup");
    fReopenFeedsOnStartupCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS));

    fOpenOnSingleClick = new Button(miscGroup, SWT.CHECK);
    fOpenOnSingleClick.setText("Open feeds with a single mouse-click");
    fOpenOnSingleClick.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_SINGLE_CLICK_OPEN));
  }

  private void createTrayOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("System Tray");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Group */
    Composite group = new Composite(container, SWT.None);
    group.setLayout(LayoutUtils.createGridLayout(1, 7, 3));
    ((GridLayout) group.getLayout()).marginBottom = 5;
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    label = new Label(group, SWT.NONE);
    label.setText("Move to the System Tray: ");

    /* Enable / Disable Tray */
    fMinimizeToTray = new Button(group, SWT.CHECK);
    fMinimizeToTray.setText("when minimizing RSSOwl");
    fMinimizeToTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));

    /* Move to Tray on Start */
    fMoveToTrayOnStart = new Button(group, SWT.CHECK);
    fMoveToTrayOnStart.setText("when starting RSSOwl");
    fMoveToTrayOnStart.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_START));

    /* Move to Tray on Close */
    fMoveToTrayOnExit = new Button(group, SWT.CHECK);
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
    boolean layoutChanged = false;

    if (!fBrowserMaximizedLayoutRadio.getSelection() && fGlobalScope.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC) != fClassicLayoutRadio.getSelection()) {
      fGlobalScope.putBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC, fClassicLayoutRadio.getSelection());
      layoutChanged = true;
    }

    if (fGlobalScope.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED) != fBrowserMaximizedLayoutRadio.getSelection()) {
      fGlobalScope.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, fBrowserMaximizedLayoutRadio.getSelection());
      layoutChanged = true;
    }

    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_SINGLE_CLICK_OPEN, fOpenOnSingleClick.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS, fReopenFeedsOnStartupCheck.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW, fAlwaysReuseFeedView.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS, fUseMultipleTabsCheck.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS, fAutoCloseTabsCheck.getSelection());
    fEclipseScope.putInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, fAutoCloseTabsSpinner.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_MINIMIZE, fMinimizeToTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_START, fMoveToTrayOnStart.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_CLOSE, fMoveToTrayOnExit.getSelection());

    /* Update Visible Feedviews */
    if (layoutChanged) {
      List<FeedView> feedViews = OwlUI.getFeedViews();
      for (FeedView feedView : feedViews) {
        feedView.updateLayout();
      }
    }

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    boolean browserMaximized = defaultScope.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
    fClassicLayoutRadio.setSelection(!browserMaximized && defaultScope.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC));
    fVerticalLayoutRadio.setSelection(!browserMaximized && !defaultScope.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC));
    fBrowserMaximizedLayoutRadio.setSelection(browserMaximized);

    fOpenOnSingleClick.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_SINGLE_CLICK_OPEN));
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