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

package org.rssowl.core.internal.persist.pref;

import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.pref.Preferences;
import org.rssowl.core.persist.pref.IPreferencesInitializer;

/**
 * An instance of <code>IPreferencesInitializer</code> responsible for defining
 * the default preferences of RSSOwl.
 * <p>
 * Subclasses may override to provide custom settings.
 * </p>
 *
 * @author bpasero
 */
public class DefaultPreferences implements IPreferencesInitializer {

  /** Global: Use Master Password to encrypt passwords to feeds */
  public static final String USE_MASTER_PASSWORD = Preferences.USE_MASTER_PASSWORD.id();

  /** Global: Use OS Password to encrypt passwords to feeds */
  public static final String USE_OS_PASSWORD = Preferences.USE_OS_PASSWORD.id();

  /** Global: Mark all news as read on minimize */
  public static final String MARK_READ_ON_MINIMIZE = Preferences.MARK_READ_ON_MINIMIZE.id();

  /** Global: Mark feed as read when feed changes */
  public static final String MARK_READ_ON_CHANGE = Preferences.MARK_READ_ON_CHANGE.id();

  /** Global: Mark all news as read on tab close */
  public static final String MARK_READ_ON_TAB_CLOSE = Preferences.MARK_READ_ON_TAB_CLOSE.id();

  /** Global: Disable JavaScript */
  public static final String DISABLE_JAVASCRIPT = Preferences.DISABLE_JAVASCRIPT.id();

  /** Global: Disable JavaScript Exceptions */
  public static final String DISABLE_JAVASCRIPT_EXCEPTIONS = Preferences.DISABLE_JAVASCRIPT_EXCEPTIONS.id();

  /** Global: Use default external browser */
  public static final String USE_DEFAULT_EXTERNAL_BROWSER = Preferences.USE_DEFAULT_EXTERNAL_BROWSER.id();

  /** Global: Use custom external browser */
  public static final String USE_CUSTOM_EXTERNAL_BROWSER = Preferences.USE_CUSTOM_EXTERNAL_BROWSER.id();

  /** Global: Path to the custom Browser */
  public static final String CUSTOM_BROWSER_PATH = Preferences.CUSTOM_BROWSER_PATH.id();

  /** Global: Re-Open last opened Browser on Startup */
  public static final String REOPEN_BROWSER_TABS = Preferences.REOPEN_BROWSER_TABS.id();

  /** Global: Minimize to the system tray */
  public static final String TRAY_ON_MINIMIZE = Preferences.TRAY_ON_MINIMIZE.id();

  /** Global: Minimize to the system tray on Shell Close */
  public static final String TRAY_ON_CLOSE = Preferences.TRAY_ON_CLOSE.id();

  /** Global: Minimize to the system tray on Application Start */
  public static final String TRAY_ON_START = Preferences.TRAY_ON_START.id();

  /** Global: Mark Read state */
  public static final String MARK_READ_STATE = Preferences.MARK_READ_STATE.id();

  /** Global: Mark Read after X seconds */
  public static final String MARK_READ_IN_MILLIS = Preferences.MARK_READ_IN_MILLIS.id();

  /** Retention Policy: Delete News > N (boolean) */
  public static final String DEL_NEWS_BY_COUNT_STATE = Preferences.DEL_NEWS_BY_COUNT_STATE.id();

  /** Retention Policy: Delete News > N (int) */
  public static final String DEL_NEWS_BY_COUNT_VALUE = Preferences.DEL_NEWS_BY_COUNT_VALUE.id();

  /** Retention Policy: Delete News > N Days (boolean) */
  public static final String DEL_NEWS_BY_AGE_STATE = Preferences.DEL_NEWS_BY_AGE_STATE.id();

  /** Retention Policy: Delete News > N Days (int) */
  public static final String DEL_NEWS_BY_AGE_VALUE = Preferences.DEL_NEWS_BY_AGE_VALUE.id();

  /** Retention Policy: Delete read News (boolean) */
  public static final String DEL_READ_NEWS_STATE = Preferences.DEL_READ_NEWS_STATE.id();

  /** Retention Policy: Never Delete Unread News (boolean) */
  public static final String NEVER_DEL_UNREAD_NEWS_STATE = Preferences.NEVER_DEL_UNREAD_NEWS_STATE.id();

  /** BookMarks: Visible Columns */
  public static final String BM_NEWS_COLUMNS = Preferences.BM_NEWS_COLUMNS.id();

  /** BookMarks: Sorted Column */
  public static final String BM_NEWS_SORT_COLUMN = Preferences.BM_NEWS_SORT_COLUMN.id();

  /** BookMarks: Ascended / Descended Sorting */
  public static final String BM_NEWS_SORT_ASCENDING = Preferences.BM_NEWS_SORT_ASCENDING.id();

  /** BookMarks: Auto-Update Interval (integer) */
  public static final String BM_UPDATE_INTERVAL = Preferences.BM_UPDATE_INTERVAL.id();

  /** BookMarks: Auto-Update Interval State (boolean) */
  public static final String BM_UPDATE_INTERVAL_STATE = Preferences.BM_UPDATE_INTERVAL_STATE.id();

  /** BookMarks: Open on Startup */
  public static final String BM_OPEN_ON_STARTUP = Preferences.BM_OPEN_ON_STARTUP.id();

  /** BookMarks: Reload on Startup */
  public static final String BM_RELOAD_ON_STARTUP = Preferences.BM_RELOAD_ON_STARTUP.id();

  /** Feed View: Search Target */
  public static final String FV_SEARCH_TARGET = Preferences.FV_SEARCH_TARGET.id();

  /** Feed View: Selected Grouping */
  public static final String FV_GROUP_TYPE = Preferences.FV_GROUP_TYPE.id();

  /** Feed View: Selected Filter */
  public static final String FV_FILTER_TYPE = Preferences.FV_FILTER_TYPE.id();

  /** Feed View: SashForm Weights */
  public static final String FV_SASHFORM_WEIGHTS = Preferences.FV_SASHFORM_WEIGHTS.id();

  /** Feed View: Layout */
  public static final String FV_LAYOUT_CLASSIC = Preferences.FV_LAYOUT_CLASSIC.id();

  /** Feed View: Browser Maximized */
  public static final String FV_BROWSER_MAXIMIZED = Preferences.FV_BROWSER_MAXIMIZED.id();

  /** Feed View: Highlight Search Results */
  public static final String FV_HIGHLIGHT_SEARCH_RESULTS = Preferences.FV_HIGHLIGHT_SEARCH_RESULTS.id();

  /** BookMark Explorer */
  public static final String BE_BEGIN_SEARCH_ON_TYPING = Preferences.BE_BEGIN_SEARCH_ON_TYPING.id();

  /** BookMark Explorer */
  public static final String BE_ALWAYS_SHOW_SEARCH = Preferences.BE_ALWAYS_SHOW_SEARCH.id();

  /** BookMark Explorer */
  public static final String BE_SORT_BY_NAME = Preferences.BE_SORT_BY_NAME.id();

  /** BookMark Explorer */
  public static final String BE_FILTER_TYPE = Preferences.BE_FILTER_TYPE.id();

  /** BookMark Explorer */
  public static final String BE_GROUP_TYPE = Preferences.BE_GROUP_TYPE.id();

  /** BookMark Explorer */
  public static final String BE_ENABLE_LINKING = Preferences.BE_ENABLE_LINKING.id();

  /** BookMark Explorer */
  public static final String BE_DISABLE_FAVICONS = Preferences.BE_DISABLE_FAVICONS.id();

  /** BookMark News-Grouping */
  public static final String BM_NEWS_FILTERING = Preferences.BM_NEWS_FILTERING.id();

  /** BookMark News-Filtering */
  public static final String BM_NEWS_GROUPING = Preferences.BM_NEWS_GROUPING.id();

  /** BookMark Load Images */
  public static final String BM_LOAD_IMAGES = Preferences.BM_LOAD_IMAGES.id();

  /** NewsMark Selected News */
  public static final String NM_SELECTED_NEWS = Preferences.NM_SELECTED_NEWS.id();

  /** Mark: Open Website instead of showing News */
  public static final String BM_OPEN_SITE_FOR_NEWS = Preferences.BM_OPEN_SITE_FOR_NEWS.id();

  /** Global: Open Website instead of showing News when description is empty */
  public static final String BM_OPEN_SITE_FOR_EMPTY_NEWS = Preferences.BM_OPEN_SITE_FOR_EMPTY_NEWS.id();

  /** Global: Show Notification Popup */
  public static final String SHOW_NOTIFICATION_POPUP = Preferences.SHOW_NOTIFICATION_POPUP.id();

  /** Global: Show Notification Popup only from Tray */
  public static final String SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED = Preferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED.id();

  /** Global: Leave Notification Popup open until closed */
  public static final String STICKY_NOTIFICATION_POPUP = Preferences.STICKY_NOTIFICATION_POPUP.id();

  /** Global: Auto Close Time */
  public static final String AUTOCLOSE_NOTIFICATION_VALUE = Preferences.AUTOCLOSE_NOTIFICATION_VALUE.id();

  /** Global: Limit number of News in notification */
  public static final String LIMIT_NOTIFICATION_SIZE = Preferences.LIMIT_NOTIFICATION_SIZE.id();

  /** Global: Limit Notifier to Selected Elements */
  public static final String LIMIT_NOTIFIER_TO_SELECTION = Preferences.LIMIT_NOTIFIER_TO_SELECTION.id();

  /** Global: Close Notifier after clicking on Item */
  public static final String CLOSE_NOTIFIER_ON_OPEN = Preferences.CLOSE_NOTIFIER_ON_OPEN.id();

  /** Global: Enable Notifier for Element */
  public static final String ENABLE_NOTIFIER = Preferences.ENABLE_NOTIFIER.id();

  /** Global: Use transparency fade in / fade out */
  public static final String FADE_NOTIFIER = Preferences.FADE_NOTIFIER.id();

  /** Global: Show Description Excerpt in Notifier */
  public static final String SHOW_EXCERPT_IN_NOTIFIER = Preferences.SHOW_EXCERPT_IN_NOTIFIER.id();

  /** Global: Always reuse feed view */
  public static final String ALWAYS_REUSE_FEEDVIEW = Preferences.ALWAYS_REUSE_FEEDVIEW.id();

  /** Global: Always reuse Browser */
  public static final String ALWAYS_REUSE_BROWSER = Preferences.ALWAYS_REUSE_BROWSER.id();

  /** Global: Clean Up: Delete BMs by last visit (state) */
  public static final String CLEAN_UP_BM_BY_LAST_VISIT_STATE = Preferences.CLEAN_UP_BM_BY_LAST_VISIT_STATE.id();

  /** Global: Clean Up: Delete BMs by last visit (value) */
  public static final String CLEAN_UP_BM_BY_LAST_VISIT_VALUE = Preferences.CLEAN_UP_BM_BY_LAST_VISIT_VALUE.id();

  /** Global: Clean Up: Delete BMs by last update (state) */
  public static final String CLEAN_UP_BM_BY_LAST_UPDATE_STATE = Preferences.CLEAN_UP_BM_BY_LAST_UPDATE_STATE.id();

  /** Global: Clean Up: Delete BMs by last update (value) */
  public static final String CLEAN_UP_BM_BY_LAST_UPDATE_VALUE = Preferences.CLEAN_UP_BM_BY_LAST_UPDATE_VALUE.id();

  /** Global: Clean Up: Delete BMs with a connection error */
  public static final String CLEAN_UP_BM_BY_CON_ERROR = Preferences.CLEAN_UP_BM_BY_CON_ERROR.id();

  /** Global: Clean Up: Delete duplicate BMs */
  public static final String CLEAN_UP_BM_BY_DUPLICATES = Preferences.CLEAN_UP_BM_BY_DUPLICATES.id();

  /** Global: Clean Up: Delete News > N (boolean) */
  public static final String CLEAN_UP_NEWS_BY_COUNT_STATE = Preferences.CLEAN_UP_NEWS_BY_COUNT_STATE.id();

  /** Global: Clean Up: Delete News > N (int) */
  public static final String CLEAN_UP_NEWS_BY_COUNT_VALUE = Preferences.CLEAN_UP_NEWS_BY_COUNT_VALUE.id();

  /** Global: Clean Up: Delete News > N Days (boolean) */
  public static final String CLEAN_UP_NEWS_BY_AGE_STATE = Preferences.CLEAN_UP_NEWS_BY_AGE_STATE.id();

  /** Global: Clean Up: Delete News > N Days (int) */
  public static final String CLEAN_UP_NEWS_BY_AGE_VALUE = Preferences.CLEAN_UP_NEWS_BY_AGE_VALUE.id();

  /** Global: Clean Up: Delete read News (boolean) */
  public static final String CLEAN_UP_READ_NEWS_STATE = Preferences.CLEAN_UP_READ_NEWS_STATE.id();

  /** Global: Clean Up: Never Delete Unread News (boolean) */
  public static final String CLEAN_UP_NEVER_DEL_UNREAD_NEWS_STATE = Preferences.CLEAN_UP_NEVER_DEL_UNREAD_NEWS_STATE.id();

  /** Global: Clean Up: The Date of the next reminder for Clean-Up as Long */
  public static final String CLEAN_UP_REMINDER_DATE_MILLIES = Preferences.CLEAN_UP_REMINDER_DATE_MILLIES.id();

  /** Global: Clean Up: Enabled state for the reminder for Clean-Up */
  public static final String CLEAN_UP_REMINDER_STATE = Preferences.CLEAN_UP_REMINDER_STATE.id();

  /** Global: Clean Up: Number of days before showing the reminder for Clean-Up */
  public static final String CLEAN_UP_REMINDER_DAYS_VALUE = Preferences.CLEAN_UP_REMINDER_DAYS_VALUE.id();

  /** Global: Search Dialog: State of showing Preview */
  public static final String SEARCH_DIALOG_PREVIEW_VISIBLE = Preferences.SEARCH_DIALOG_PREVIEW_VISIBLE.id();

  /** Global: Visible Columns in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_COLUMNS = Preferences.SEARCH_DIALOG_NEWS_COLUMNS.id();

  /** Global: Sorted Column in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_SORT_COLUMN = Preferences.SEARCH_DIALOG_NEWS_SORT_COLUMN.id();

  /** Global: Ascended / Descended Sorting in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_SORT_ASCENDING = Preferences.SEARCH_DIALOG_NEWS_SORT_ASCENDING.id();

  /** Global: Show Toolbar */
  public static final String SHOW_TOOLBAR = Preferences.SHOW_TOOLBAR.id();

  /** Global: Show Statusbar */
  public static final String SHOW_STATUS = Preferences.SHOW_STATUS.id();

  /** Global: Load Title from Feed in Bookmark Wizard */
  public static final String BM_LOAD_TITLE_FROM_FEED = Preferences.BM_LOAD_TITLE_FROM_FEED.id();

  /** Global: Last used Keyword Feed */
  public static final String LAST_KEYWORD_FEED = Preferences.LAST_KEYWORD_FEED.id();

  /** Global: Open Browser Tabs in the Background */
  public static final String OPEN_BROWSER_IN_BACKGROUND = Preferences.OPEN_BROWSER_IN_BACKGROUND.id();

  /** Global: Share Provider Order and Enablement */
  public static final String SHARE_PROVIDER_STATE = Preferences.SHARE_PROVIDER_STATE.id();

  /**
   * Eclipse Preferences Follow
   */

  /** Global Eclipse: Open on Single Click */
  public static final String ECLIPSE_SINGLE_CLICK_OPEN = Preferences.ECLIPSE_SINGLE_CLICK_OPEN.id();

  /** Global Eclipse: Restore Tabs on startup */
  public static final String ECLIPSE_RESTORE_TABS = Preferences.ECLIPSE_RESTORE_TABS.id();

  /** Global Eclipse: Use multiple Tabs */
  public static final String ECLIPSE_MULTIPLE_TABS = Preferences.ECLIPSE_MULTIPLE_TABS.id();

  /** Global Eclipse: Autoclose Tabs */
  public static final String ECLIPSE_AUTOCLOSE_TABS = Preferences.ECLIPSE_AUTOCLOSE_TABS.id();

  /** Global Eclipse: Autoclose Tabs Threshold */
  public static final String ECLIPSE_AUTOCLOSE_TABS_THRESHOLD = Preferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD.id();

  /** Global Eclipse: Use Proxy */
  public static final String ECLIPSE_USE_PROXY = Preferences.ECLIPSE_USE_PROXY.id();

  /** Global Eclipse: Use System Proxy */
  public static final String ECLIPSE_USE_SYSTEM_PROXY = Preferences.ECLIPSE_USE_SYSTEM_PROXY.id();

  /** Global Eclipse: Proxy Host */
  public static final String ECLIPSE_PROXY_HOST = Preferences.ECLIPSE_PROXY_HOST.id();

  /** Global Eclipse: Proxy Port */
  public static final String ECLIPSE_PROXY_PORT = Preferences.ECLIPSE_PROXY_PORT.id();

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
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initGlobalDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(USE_OS_PASSWORD, true);
    defaultScope.putBoolean(MARK_READ_ON_MINIMIZE, false);
    defaultScope.putBoolean(MARK_READ_ON_CHANGE, false);
    defaultScope.putBoolean(MARK_READ_ON_TAB_CLOSE, false);
    defaultScope.putBoolean(DISABLE_JAVASCRIPT, true);
    defaultScope.putBoolean(USE_DEFAULT_EXTERNAL_BROWSER, true);
    defaultScope.putBoolean(TRAY_ON_MINIMIZE, false);
    defaultScope.putBoolean(MARK_READ_STATE, true);
    defaultScope.putInteger(MARK_READ_IN_MILLIS, 0);
    defaultScope.putBoolean(BM_OPEN_SITE_FOR_EMPTY_NEWS, false);
    defaultScope.putBoolean(FADE_NOTIFIER, true);
    defaultScope.putBoolean(CLOSE_NOTIFIER_ON_OPEN, true);
    defaultScope.putInteger(LIMIT_NOTIFICATION_SIZE, 5);
    defaultScope.putBoolean(SHOW_NOTIFICATION_POPUP, true);
    defaultScope.putBoolean(SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED, true);
    defaultScope.putBoolean(SEARCH_DIALOG_PREVIEW_VISIBLE, true);
    defaultScope.putInteger(AUTOCLOSE_NOTIFICATION_VALUE, 8);
    defaultScope.putBoolean(SHOW_TOOLBAR, true);
    defaultScope.putBoolean(SHOW_STATUS, true);
    defaultScope.putBoolean(BM_LOAD_TITLE_FROM_FEED, true);
    defaultScope.putIntegers(SEARCH_DIALOG_NEWS_COLUMNS, new int[] { 9, 0, 8, 1, 2, 3, 6 }); //TODO Must be in sync with NewsColumn enum
    defaultScope.putInteger(SEARCH_DIALOG_NEWS_SORT_COLUMN, 9); //TODO Must be in sync with NewsColumn enum
    defaultScope.putBoolean(SEARCH_DIALOG_NEWS_SORT_ASCENDING, false);
    defaultScope.putIntegers(SHARE_PROVIDER_STATE, new int[] { 1, 2, 3, 4, 5, 6, 7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18 }); //TODO Must be in sync with Share Provider contributions
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initGlobalEclipseDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(ECLIPSE_SINGLE_CLICK_OPEN, true);
    defaultScope.putBoolean(ECLIPSE_RESTORE_TABS, true);
    defaultScope.putBoolean(ECLIPSE_MULTIPLE_TABS, true);
    defaultScope.putInteger(ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, 5);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initRetentionDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(DEL_NEWS_BY_COUNT_STATE, true);
    defaultScope.putInteger(DEL_NEWS_BY_COUNT_VALUE, 200);
    defaultScope.putInteger(DEL_NEWS_BY_AGE_VALUE, 30);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initCleanUpDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(CLEAN_UP_BM_BY_LAST_UPDATE_STATE, true);
    defaultScope.putInteger(CLEAN_UP_BM_BY_LAST_UPDATE_VALUE, 30);

    defaultScope.putBoolean(CLEAN_UP_BM_BY_LAST_VISIT_STATE, true);
    defaultScope.putInteger(CLEAN_UP_BM_BY_LAST_VISIT_VALUE, 30);

    defaultScope.putInteger(CLEAN_UP_NEWS_BY_COUNT_VALUE, 200);
    defaultScope.putInteger(CLEAN_UP_NEWS_BY_AGE_VALUE, 30);

    defaultScope.putBoolean(CLEAN_UP_REMINDER_STATE, true);
    defaultScope.putInteger(CLEAN_UP_REMINDER_DAYS_VALUE, 30);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initDisplayDefaults(IPreferenceScope defaultScope) {
    defaultScope.putInteger(BM_NEWS_FILTERING, -1);
    defaultScope.putInteger(BM_NEWS_GROUPING, -1);
    defaultScope.putBoolean(BM_LOAD_IMAGES, true);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initReloadOpenDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(BM_UPDATE_INTERVAL_STATE, true);
    defaultScope.putLong(BM_UPDATE_INTERVAL, 60 * 30); // 30 Minutes
    defaultScope.putBoolean(BM_OPEN_ON_STARTUP, false);
    defaultScope.putBoolean(BM_RELOAD_ON_STARTUP, false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initNewsColumnsDefaults(IPreferenceScope defaultScope) {
    defaultScope.putIntegers(BM_NEWS_COLUMNS, new int[] { 0, 1, 2, 3, 6 }); //TODO Must be in sync with NewsColumn enum
    defaultScope.putInteger(BM_NEWS_SORT_COLUMN, 1); //TODO Must be in sync with NewsColumn enum
    defaultScope.putBoolean(BM_NEWS_SORT_ASCENDING, false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initBookMarkExplorerDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(BE_BEGIN_SEARCH_ON_TYPING, true);
    defaultScope.putBoolean(BE_SORT_BY_NAME, false);
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initFeedViewDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(FV_LAYOUT_CLASSIC, true);
    defaultScope.putIntegers(FV_SASHFORM_WEIGHTS, new int[] { 50, 50 });
    defaultScope.putBoolean(BM_OPEN_SITE_FOR_NEWS, false);
  }
}