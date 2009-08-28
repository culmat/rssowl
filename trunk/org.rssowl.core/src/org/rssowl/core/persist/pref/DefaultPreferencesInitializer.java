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

package org.rssowl.core.persist.pref;

import java.util.ArrayList;
import java.util.List;

/**
 * An instance of <code>IPreferencesInitializer</code> responsible for defining
 * the default preferences of RSSOwl.
 * <p>
 * Subclasses may override to provide custom settings.
 * </p>
 * <p>
 * TODO Consider moving to org.rssowl.ui giving implicit dependencies.
 * </p>
 *
 * @author bpasero
 */
public class DefaultPreferencesInitializer implements IPreferencesInitializer {

  /*
   * @see
   * org.rssowl.core.model.preferences.IPreferencesInitializer#initialize(org
   * .rssowl.core.model.preferences.IPreferencesScope)
   */
  public void initialize(IPreferenceScope defaultScope) {

    /* Default Globals */
    initGlobalDefaults(defaultScope);

    /* Default Eclipse Globals */
    initGlobalEclipseDefaults(defaultScope);

    /* Default News Column Settings */
    initNewsColumnsDefaults(defaultScope);

    /* Default Retention Policy */
    initRetentionDefaults(defaultScope);

    /* Default Clean Up */
    initCleanUpDefaults(defaultScope);

    /* Default Display Settings */
    initDisplayDefaults(defaultScope);

    /* Default BookMark Explorer */
    initBookMarkExplorerDefaults(defaultScope);

    /* Default Feed View */
    initFeedViewDefaults(defaultScope);

    /* Default Reload/Open Settings */
    initReloadOpenDefaults(defaultScope);

    /* Toolbar Item Settings */
    initToolbarDefaults(defaultScope);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initGlobalDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.USE_OS_PASSWORD.id(), true);
    defaultScope.putBoolean(Preferences.MARK_READ_ON_MINIMIZE.id(), false);
    defaultScope.putBoolean(Preferences.MARK_READ_ON_CHANGE.id(), false);
    defaultScope.putBoolean(Preferences.MARK_READ_ON_TAB_CLOSE.id(), false);
    defaultScope.putBoolean(Preferences.DISABLE_JAVASCRIPT.id(), true);
    defaultScope.putBoolean(Preferences.USE_DEFAULT_EXTERNAL_BROWSER.id(), true);
    defaultScope.putBoolean(Preferences.TRAY_ON_MINIMIZE.id(), false);
    defaultScope.putBoolean(Preferences.MARK_READ_STATE.id(), true);
    defaultScope.putInteger(Preferences.MARK_READ_IN_MILLIS.id(), 0);
    defaultScope.putBoolean(Preferences.BM_OPEN_SITE_FOR_EMPTY_NEWS.id(), false);
    defaultScope.putBoolean(Preferences.FADE_NOTIFIER.id(), true);
    defaultScope.putBoolean(Preferences.CLOSE_NOTIFIER_ON_OPEN.id(), true);
    defaultScope.putInteger(Preferences.LIMIT_NOTIFICATION_SIZE.id(), 5);
    defaultScope.putBoolean(Preferences.SHOW_NOTIFICATION_POPUP.id(), true);
    defaultScope.putBoolean(Preferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED.id(), true);
    defaultScope.putBoolean(Preferences.SEARCH_DIALOG_PREVIEW_VISIBLE.id(), true);
    defaultScope.putInteger(Preferences.AUTOCLOSE_NOTIFICATION_VALUE.id(), 8);
    defaultScope.putBoolean(Preferences.SHOW_TOOLBAR.id(), true);
    defaultScope.putBoolean(Preferences.SHOW_STATUS.id(), true);
    defaultScope.putBoolean(Preferences.BM_LOAD_TITLE_FROM_FEED.id(), true);
    defaultScope.putIntegers(Preferences.SEARCH_DIALOG_NEWS_COLUMNS.id(), new int[] { 9, 0, 8, 1, 2, 3, 6 });
    defaultScope.putInteger(Preferences.SEARCH_DIALOG_NEWS_SORT_COLUMN.id(), 9);
    defaultScope.putBoolean(Preferences.SEARCH_DIALOG_NEWS_SORT_ASCENDING.id(), false);
    defaultScope.putIntegers(Preferences.SHARE_PROVIDER_STATE.id(), new int[] { 1, 2, 3, 4, 5, 6, 7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18, -19, -20, -21, -22 });
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initGlobalEclipseDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.ECLIPSE_SINGLE_CLICK_OPEN.id(), true);
    defaultScope.putBoolean(Preferences.ECLIPSE_RESTORE_TABS.id(), true);
    defaultScope.putBoolean(Preferences.ECLIPSE_MULTIPLE_TABS.id(), true);
    defaultScope.putInteger(Preferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD.id(), 5);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initRetentionDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.DEL_NEWS_BY_COUNT_STATE.id(), true);
    defaultScope.putInteger(Preferences.DEL_NEWS_BY_COUNT_VALUE.id(), 200);
    defaultScope.putInteger(Preferences.DEL_NEWS_BY_AGE_VALUE.id(), 30);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initCleanUpDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.CLEAN_UP_BM_BY_LAST_UPDATE_STATE.id(), true);
    defaultScope.putInteger(Preferences.CLEAN_UP_BM_BY_LAST_UPDATE_VALUE.id(), 30);

    defaultScope.putBoolean(Preferences.CLEAN_UP_BM_BY_LAST_VISIT_STATE.id(), true);
    defaultScope.putInteger(Preferences.CLEAN_UP_BM_BY_LAST_VISIT_VALUE.id(), 30);

    defaultScope.putInteger(Preferences.CLEAN_UP_NEWS_BY_COUNT_VALUE.id(), 200);
    defaultScope.putInteger(Preferences.CLEAN_UP_NEWS_BY_AGE_VALUE.id(), 30);

    defaultScope.putBoolean(Preferences.CLEAN_UP_REMINDER_STATE.id(), true);
    defaultScope.putInteger(Preferences.CLEAN_UP_REMINDER_DAYS_VALUE.id(), 30);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initDisplayDefaults(IPreferenceScope defaultScope) {
    defaultScope.putInteger(Preferences.BM_NEWS_FILTERING.id(), -1);
    defaultScope.putInteger(Preferences.BM_NEWS_GROUPING.id(), -1);
    defaultScope.putBoolean(Preferences.BM_LOAD_IMAGES.id(), true);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initReloadOpenDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.BM_UPDATE_INTERVAL_STATE.id(), true);
    defaultScope.putLong(Preferences.BM_UPDATE_INTERVAL.id(), 60 * 30); // 30 Minutes
    defaultScope.putBoolean(Preferences.BM_OPEN_ON_STARTUP.id(), false);
    defaultScope.putBoolean(Preferences.BM_RELOAD_ON_STARTUP.id(), false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initNewsColumnsDefaults(IPreferenceScope defaultScope) {
    defaultScope.putIntegers(Preferences.BM_NEWS_COLUMNS.id(), new int[] { 0, 1, 2, 3, 6 });
    defaultScope.putInteger(Preferences.BM_NEWS_SORT_COLUMN.id(), 1);
    defaultScope.putBoolean(Preferences.BM_NEWS_SORT_ASCENDING.id(), false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initBookMarkExplorerDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.BE_BEGIN_SEARCH_ON_TYPING.id(), true);
    defaultScope.putBoolean(Preferences.BE_SORT_BY_NAME.id(), false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initFeedViewDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(Preferences.FV_LAYOUT_CLASSIC.id(), true);
    defaultScope.putIntegers(Preferences.FV_SASHFORM_WEIGHTS.id(), new int[] { 50, 50 });
    defaultScope.putBoolean(Preferences.BM_OPEN_SITE_FOR_NEWS.id(), false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  private void initToolbarDefaults(IPreferenceScope defaultScope) {
    List<String> items = new ArrayList<String>();

    /* New | Import | Export */
    items.add("org.rssowl.ui.NewDropDown");
    items.add("org.rssowl.ui.actions.ImportFeeds");
    items.add("org.rssowl.ui.actions.ExportFeeds");

    /* Undo | Redo */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.UndoAction");
    items.add("org.rssowl.ui.RedoAction");

    /* Update | Update All */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.actions.Reload");
    items.add("org.rssowl.ui.actions.ReloadAll");

    /* Search */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.SearchNewsAction");

    /* Mark Read | Mark All Read */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.ToggleReadState");
    items.add("org.rssowl.ui.MarkAllRead");

    /* Label | Sticky */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.Label");
    items.add("org.rssowl.ui.actions.MarkSticky");

    /* Next | Previous */
    items.add("org.rssowl.ui.CoolBarSeparator");
    items.add("org.rssowl.ui.Next");
    items.add("org.rssowl.ui.Previous");

    defaultScope.putStrings(Preferences.TOOLBAR_ITEMS.id(), items.toArray(new String[items.size()]));
    defaultScope.putInteger(Preferences.TOOLBAR_MODE.id(), 0);
    defaultScope.putInteger(Preferences.DEFAULT_NEXT_ACTION.id(), 1);
    defaultScope.putInteger(Preferences.DEFAULT_PREVIOUS_ACTION.id(), 5);
  }
}