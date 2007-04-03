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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.events.NewsAdapter;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.runnable.EventType;
import org.rssowl.core.model.preferences.IPreferencesScope;
import org.rssowl.core.model.preferences.PreferencesEvent;
import org.rssowl.core.model.preferences.PreferencesListener;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.Set;

/**
 * @author bpasero
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

  /* WebSite class being used for the Browser on Windows only */
  private static final String SWT_BROWSER_WIN = "org.eclipse.swt.browser.WebSite";

  /** Key for Data-Slot in Controls that support this Hook */
  public static final String FOCUSLESS_SCROLL_HOOK = "org.rssowl.ui.internal.FocuslessScrollHook";

  private TrayItem fTrayItem;
  private boolean fTrayTeasing;
  private boolean fTrayEnabled;
  private boolean fMinimizedToTray;
  private ApplicationActionBarAdvisor fActionBarAdvisor;
  private LocalResourceManager fResources;
  private IPreferencesScope fPreferences;

  /* Listeners */
  private NewsAdapter fNewsListener;
  private ShellListener fTrayShellListener;
  private PreferencesListener fPrefListener;

  /**
   * @param configurer
   */
  public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
    super(configurer);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
   */
  @Override
  public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
    fActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
    return fActionBarAdvisor;
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
   */
  @Override
  public void preWindowOpen() {
    IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

    /* Set Window State and define visibility of UI elements */
    configurer.setShowCoolBar(true);
    configurer.setShowPerspectiveBar(false);
    configurer.setShowStatusLine(true);
    configurer.setShowMenuBar(true);
    configurer.setShowFastViewBars(true);
    configurer.setShowProgressIndicator(true);
    configurer.setTitle("RSSOwl - Next Generation"); //$NON-NLS-1$

    /* Apply DND Support for Editor Area */
    configurer.addEditorAreaTransfer(LocalSelectionTransfer.getTransfer());
    configurer.configureEditorAreaDropListener(new EditorDNDImpl());
  }

  /**
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowOpen()
   */
  @Override
  public void postWindowOpen() {

    /* Retrieve Preferences */
    fPreferences = NewsModel.getDefault().getGlobalScope();

    /* Hook TrayItem if supported on OS and 1st Window */
    if (fPreferences.getBoolean(DefaultPreferences.USE_SYSTEM_TRAY))
      enableTray();

    /* Win only: Allow Scroll over Cursor-Control */
    if (Application.IS_WINDOWS)
      hookFocuslessScrolling(getWindowConfigurer().getWindow().getShell().getDisplay());

    /* Register Listeners */
    registerListeners();
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowShellClose()
   */
  @Override
  public boolean preWindowShellClose() {

    /* Check if Prefs tell to move to tray */
    if (fPreferences.getBoolean(DefaultPreferences.TRAY_ON_EXIT)) {
      getWindowConfigurer().getWindow().getShell().notifyListeners(SWT.Iconify, new Event());
      return false;
    }

    return super.preWindowShellClose();
  }

  boolean isMinimizedToTray() {
    return fMinimizedToTray;
  }

  private void registerListeners() {

    /* Add Shell sListener */
    getWindowConfigurer().getWindow().getShell().addShellListener(new ShellAdapter() {
      @Override
      public void shellIconified(ShellEvent e) {
        onMinimize();
      }
    });

    /* Listen on Preferences Changes */
    fPrefListener = new PreferencesListener() {
      public void preferenceAdded(PreferencesEvent event) {
        onPreferencesChange(event, EventType.PERSIST);
      }

      public void preferenceDeleted(PreferencesEvent event) {
        onPreferencesChange(event, EventType.REMOVE);
      }

      public void preferenceUpdated(PreferencesEvent event) {
        onPreferencesChange(event, EventType.UPDATE);
      }
    };
    NewsModel.getDefault().addPreferencesListener(fPrefListener);
  }

  private void unregisterListeners() {
    NewsModel.getDefault().removePreferencesListener(fPrefListener);
  }

  private void onPreferencesChange(PreferencesEvent event, EventType type) {

    /* Tray Preference Change */
    if (DefaultPreferences.USE_SYSTEM_TRAY.equals(event.getKey())) {
      boolean useTray;
      if (type == EventType.REMOVE)
        useTray = NewsModel.getDefault().getDefaultScope().getBoolean(DefaultPreferences.USE_SYSTEM_TRAY);
      else
        useTray = event.getBoolean();

      if (useTray && !fTrayEnabled)
        enableTray();
      else if (fTrayEnabled)
        disableTray();
    }
  }

  @Override
  public void dispose() {
    unregisterListeners();

    if (fTrayItem != null)
      fTrayItem.dispose();

    if (fNewsListener != null)
      NewsModel.getDefault().removeNewsListener(fNewsListener);

    fResources.dispose();
  }

  private void onMinimize() {

    /* Mark displayed News as Read on Minimize if set in Preferences */
    IEditorPart activeEditor = RSSOwlUI.getActiveEditor();
    if (activeEditor != null && activeEditor instanceof FeedView) {
      FeedView feedView = (FeedView) activeEditor;
      feedView.notifyUIEvent(FeedView.UIEvent.MINIMIZE);
    }
  }

  /* Enable System-Tray Support */
  private void enableTray() {

    /* Only enable for Primary Window */
    IWorkbenchWindow primaryWindow = RSSOwlUI.getPrimaryWindow();
    if (primaryWindow == null || !primaryWindow.equals(getWindowConfigurer().getWindow()))
      return;

    final Shell shell = primaryWindow.getShell();
    final Tray tray = shell.getDisplay().getSystemTray();

    /* Tray not support on the OS */
    if (tray == null)
      return;

    /* Create Item in Tray */
    fTrayItem = new TrayItem(tray, SWT.NONE);
    fTrayItem.setToolTipText("RSSOwl");
    fTrayEnabled = true;

    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(false);

    /* Apply Image */
    fTrayItem.setImage(RSSOwlUI.getImage(fResources, RSSOwlUI.TRAY_OWL));

    /* Minimize to Tray on Shell Iconify */
    fTrayShellListener = new ShellAdapter() {

      @Override
      public void shellIconified(ShellEvent e) {
        moveToTray(shell);
      }
    };
    shell.addShellListener(fTrayShellListener);

    /* Show Menu on Selection */
    fTrayItem.addListener(SWT.MenuDetect, new Listener() {
      public void handleEvent(Event event) {
        MenuManager trayMenu = new MenuManager();
        Menu menu = trayMenu.createContextMenu(shell);
        fActionBarAdvisor.fillTrayItem(trayMenu);
        menu.setVisible(true);
      }
    });

    /* Handle DefaultSelection */
    fTrayItem.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event event) {

        /* Restore from Tray */
        if (!shell.isVisible())
          restoreFromTray(shell);

        /* Move to Tray */
        else if (!Application.IS_WINDOWS)
          moveToTray(shell);
      }
    });

    /* Indicate new News in Tray */
    fNewsListener = new NewsAdapter() {

      @Override
      public void newsAdded(Set<NewsEvent> events) {
        JobRunner.runInUIThread(fTrayItem, new Runnable() {
          public void run() {

            /* Update Icon only when Tray is visible and not yet teasing */
            if (!fTrayItem.getVisible() || fTrayTeasing || shell.getVisible())
              return;

            fTrayTeasing = true;
            fTrayItem.setImage(RSSOwlUI.getImage(fResources, RSSOwlUI.TRAY_OWL_TEASING));
          }
        });
      }
    };
    NewsModel.getDefault().addNewsListener(fNewsListener);
  }

  /* Move to System Tray */
  private void moveToTray(Shell shell) {
    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(true);
    shell.setVisible(false);
    fMinimizedToTray = true;
  }

  /* Restore from System Tray */
  void restoreFromTray(Shell shell) {
    shell.setVisible(true);
    shell.setActive();

    /* Un-Minimize if minimized */
    if (shell.getMinimized())
      shell.setMinimized(false);

    /*
     * Bug in Eclipse: For some reason the workbench-layout is broken, when
     * restoring from the Tray after it has been moved to tray with Shell-Close.
     * Force a layout() to avoid this issue.
     */
//    else {
//      shell.setRedraw(false);
//      try {
//        shell.layout();
//      } finally {
//        shell.setRedraw(true);
//      }
//    }

    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(false);

    if (fTrayTeasing)
      fTrayItem.setImage(RSSOwlUI.getImage(fResources, RSSOwlUI.TRAY_OWL));

    fTrayTeasing = false;
    fMinimizedToTray = false;
  }

  /* Disable System-Tray Support */
  private void disableTray() {

    /* First make sure to have the Window restored */
    restoreFromTray(getWindowConfigurer().getWindow().getShell());

    fTrayEnabled = false;
    fMinimizedToTray = false;

    if (fTrayItem != null)
      fTrayItem.dispose();

    if (fNewsListener != null)
      NewsModel.getDefault().removeNewsListener(fNewsListener);

    if (fTrayShellListener != null)
      getWindowConfigurer().getWindow().getShell().removeShellListener(fTrayShellListener);
  }

  /* Support for focusless scrolling */
  private void hookFocuslessScrolling(final Display display) {
    display.addFilter(SWT.MouseWheel, new Listener() {
      public void handleEvent(Event event) {
        Control control = display.getCursorControl();

        /* Control must be non-focus undisposed */
        if (control == null || control.isDisposed() || control.isFocusControl())
          return;

        /* Pass focus to control and disable event if allowed */
        boolean isBrowser = SWT_BROWSER_WIN.equals(control.getClass().getName());
        if (isBrowser || control.getData(FOCUSLESS_SCROLL_HOOK) != null) {

          /* Break Condition */
          control.setFocus();

          /* Re-Post Event to Cursor Control */
          event.doit = false;
          event.widget = control;
          display.post(event);
        }
      }
    });
  }
}