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
  public static final String USE_MASTER_PASSWORD = "org.rssowl.pref.UseMasterPassword";

  /** Global: Use OS Password to encrypt passwords to feeds */
  public static final String USE_OS_PASSWORD = "org.rssowl.pref.UseOSPassword";

  /** Global: Mark all news as read on minimize */
  public static final String MARK_READ_ON_MINIMIZE = "org.rssowl.pref.MarkNewsReadOnMinimize";

  /** Global: Mark feed as read when feed changes */
  public static final String MARK_READ_ON_CHANGE = "org.rssowl.pref.MarkFeedReadOnChange";

  /** Global: Mark all news as read on tab close */
  public static final String MARK_READ_ON_TAB_CLOSE = "org.rssowl.pref.MarkNewsReadOnTabClose";

  /** Global: Disable JavaScript */
  public static final String DISABLE_JAVASCRIPT = "org.rssowl.pref.DisableJavaScript";

  /** Global: Disable JavaScript Exceptions */
  public static final String DISABLE_JAVASCRIPT_EXCEPTIONS = "org.rssowl.pref.DisableJavaScriptExceptions";

  /** Global: Use default external browser */
  public static final String USE_DEFAULT_EXTERNAL_BROWSER = "org.rssowl.pref.UseExternalBrowser";

  /** Global: Use custom external browser */
  public static final String USE_CUSTOM_EXTERNAL_BROWSER = "org.rssowl.pref.UseCustomExternalBrowser";

  /** Global: Path to the custom Browser */
  public static final String CUSTOM_BROWSER_PATH = "org.rssowl.pref.CustomBrowserPath";

  /** Global: Minimize to the system tray */
  public static final String TRAY_ON_MINIMIZE = "org.rssowl.pref.UseSystemTray";

  /** Global: Minimize to the system tray on Shell Close */
  public static final String TRAY_ON_CLOSE = "org.rssowl.pref.TrayOnExit";

  /** Global: Minimize to the system tray on Application Start */
  public static final String TRAY_ON_START = "org.rssowl.pref.TrayOnStart";

  /** Global: Mark Read state */
  public static final String MARK_READ_STATE = "org.rssowl.pref.MarkReadState";

  /** Global: Mark Read after X seconds */
  public static final String MARK_READ_IN_MILLIS = "org.rssowl.pref.MarkReadInMillis";

  /** Retention Policy: Delete News > N (boolean) */
  public static final String DEL_NEWS_BY_COUNT_STATE = "org.rssowl.pref.DelNewsByCountState";

  /** Retention Policy: Delete News > N (int) */
  public static final String DEL_NEWS_BY_COUNT_VALUE = "org.rssowl.pref.DelNewsByCountValue";

  /** Retention Policy: Delete News > N Days (boolean) */
  public static final String DEL_NEWS_BY_AGE_STATE = "org.rssowl.pref.DelNewsByAgeState";

  /** Retention Policy: Delete News > N Days (int) */
  public static final String DEL_NEWS_BY_AGE_VALUE = "org.rssowl.pref.DelNewsByAgeValue";

  /** Retention Policy: Delete read News (boolean) */
  public static final String DEL_READ_NEWS_STATE = "org.rssowl.pref.DelReadNewsState";

  /** Retention Policy: Never Delete Unread News (boolean) */
  public static final String NEVER_DEL_UNREAD_NEWS_STATE = "org.rssowl.pref.NeverDelUnreadNewsState";

  /** BookMarks: Visible Columns */
  public static final String BM_NEWS_COLUMNS = "org.rssowl.pref.BMNewsColumns";

  /** BookMarks: Sorted Column */
  public static final String BM_NEWS_SORT_COLUMN = "org.rssowl.pref.BMNewsSortColumn";

  /** BookMarks: Ascended / Descended Sorting */
  public static final String BM_NEWS_SORT_ASCENDING = "org.rssowl.pref.BMNewsSortAscending";

  /** BookMarks: Auto-Update Interval (integer) */
  public static final String BM_UPDATE_INTERVAL = "org.rssowl.pref.BMUpdateInterval";

  /** BookMarks: Auto-Update Interval State (boolean) */
  public static final String BM_UPDATE_INTERVAL_STATE = "org.rssowl.pref.BMUpdateIntervalState";

  /** BookMarks: Open on Startup */
  public static final String BM_OPEN_ON_STARTUP = "org.rssowl.pref.BMOpenOnStartup";

  /** BookMarks: Reload on Startup */
  public static final String BM_RELOAD_ON_STARTUP = "org.rssowl.pref.BMReloadOnStartup";

  /** Feed View: Search Target */
  public static final String FV_SEARCH_TARGET = "org.rssowl.ui.internal.editors.feed.SearchTarget";

  /** Feed View: Selected Grouping */
  public static final String FV_GROUP_TYPE = "org.rssowl.ui.internal.editors.feed.GroupType";

  /** Feed View: Selected Filter */
  public static final String FV_FILTER_TYPE = "org.rssowl.ui.internal.editors.feed.FilterType";

  /** Feed View: SashForm Weights */
  public static final String FV_SASHFORM_WEIGHTS = "org.rssowl.ui.internal.editors.feed.SashFormWeights";

  /** Feed View: Layout */
  public static final String FV_LAYOUT_VERTICAL = "org.rssowl.ui.internal.editors.feed.LayoutVertical";

  /** Feed View: Browser Maximized */
  public static final String FV_BROWSER_MAXIMIZED = "org.rssowl.ui.internal.editors.feed.BrowserMaximized";

  /** Feed View: Highlight Search Results */
  public static final String FV_HIGHLIGHT_SEARCH_RESULTS = "org.rssowl.ui.internal.editors.feed.HighlightSearchResults";

  /** BookMark Explorer */
  public static final String BE_BEGIN_SEARCH_ON_TYPING = "org.rssowl.ui.internal.views.explorer.BeginSearchOnTyping"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_ALWAYS_SHOW_SEARCH = "org.rssowl.ui.internal.views.explorer.AlwaysShowSearch"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_SORT_BY_NAME = "org.rssowl.ui.internal.views.explorer.SortByName"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_FILTER_TYPE = "org.rssowl.ui.internal.views.explorer.FilterType"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_GROUP_TYPE = "org.rssowl.ui.internal.views.explorer.GroupingType"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_ENABLE_LINKING = "org.rssowl.ui.internal.views.explorer.EnableLinking"; //$NON-NLS-1$

  /** BookMark Explorer */
  public static final String BE_DISABLE_FAVICONS = "org.rssowl.ui.internal.views.explorer.DisableFavicons"; //$NON-NLS-1$

  /** BookMark News-Grouping */
  public static final String BM_NEWS_FILTERING = "org.rssowl.pref.BMNewsFiltering";

  /** BookMark News-Filtering */
  public static final String BM_NEWS_GROUPING = "org.rssowl.pref.BMNewsGrouping";

  /** BookMark Load Images */
  public static final String BM_LOAD_IMAGES = "org.rssowl.pref.BMLoadImages";

  /** NewsMark Selected News */
  public static final String NM_SELECTED_NEWS = "org.rssowl.pref.NMSelectedNews";

  /** Mark: Open Website instead of showing News */
  public static final String BM_OPEN_SITE_FOR_NEWS = "org.rssowl.pref.BMOpenSiteForNews";

  /** Global: Open Website instead of showing News when description is empty */
  public static final String BM_OPEN_SITE_FOR_EMPTY_NEWS = "org.rssowl.pref.OpenSiteForEmptyNews";

  /** Global: Show Notification Popup */
  public static final String SHOW_NOTIFICATION_POPUP = "org.rssowl.pref.ShowNotificationPoup";

  /** Global: Show Notification Popup only from Tray */
  public static final String SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED = "org.rssowl.pref.ShowNotificationPoupOnlyWhenMinimized";

  /** Global: Leave Notification Popup open until closed */
  public static final String STICKY_NOTIFICATION_POPUP = "org.rssowl.pref.StickyNotificationPoup";

  /** Global: Auto Close Time */
  public static final String AUTOCLOSE_NOTIFICATION_VALUE = "org.rssowl.pref.AutoCloseNotificationPoupValue";

  /** Global: Limit number of News in notification */
  public static final String LIMIT_NOTIFICATION_SIZE = "org.rssowl.pref.LimitNotificationPoup";

  /** Global: Limit Notifier to Selected Elements */
  public static final String LIMIT_NOTIFIER_TO_SELECTION = "org.rssowl.pref.LimitNotifierToSelection";

  /** Global: Close Notifier after clicking on Item */
  public static final String CLOSE_NOTIFIER_ON_OPEN = "org.rssowl.pref.CloseNotifierOnOpen";

  /** Global: Enable Notifier for Element */
  public static final String ENABLE_NOTIFIER = "org.rssowl.pref.EnableNotifier";

  /** Global: Use transparency fade in / fade out */
  public static final String FADE_NOTIFIER = "org.rssowl.pref.FadeNotifier";

  /** Global: Show Description Excerpt in Notifier */
  public static final String SHOW_EXCERPT_IN_NOTIFIER = "org.rssowl.pref.ShowExcerptInNotifier";

  /** Global: Always reuse feed view */
  public static final String ALWAYS_REUSE_FEEDVIEW = "org.rssowl.pref.AlwaysReuseFeedView";

  /** Global: Always reuse Browser */
  public static final String ALWAYS_REUSE_BROWSER = "org.rssowl.pref.AlwaysReuseBrowser";

  /** Global: Clean Up: Delete BMs by last visit (state) */
  public static final String CLEAN_UP_BM_BY_LAST_VISIT_STATE = "org.rssowl.pref.CleanUpBMByLastVisitState";

  /** Global: Clean Up: Delete BMs by last visit (value) */
  public static final String CLEAN_UP_BM_BY_LAST_VISIT_VALUE = "org.rssowl.pref.CleanUpBMByLastVisitValue";

  /** Global: Clean Up: Delete BMs by last update (state) */
  public static final String CLEAN_UP_BM_BY_LAST_UPDATE_STATE = "org.rssowl.pref.CleanUpBMByLastUpdateState";

  /** Global: Clean Up: Delete BMs by last update (value) */
  public static final String CLEAN_UP_BM_BY_LAST_UPDATE_VALUE = "org.rssowl.pref.CleanUpBMByLastUpdateValue";

  /** Global: Clean Up: Delete BMs with a connection error */
  public static final String CLEAN_UP_BM_BY_CON_ERROR = "org.rssowl.pref.CleanUpBMByConError";

  /** Global: Clean Up: Delete duplicate BMs */
  public static final String CLEAN_UP_BM_BY_DUPLICATES = "org.rssowl.pref.CleanUpBMByDuplicates";

  /** Global: Clean Up: Delete News > N (boolean) */
  public static final String CLEAN_UP_NEWS_BY_COUNT_STATE = "org.rssowl.pref.CleanUpNewsByCountState";

  /** Global: Clean Up: Delete News > N (int) */
  public static final String CLEAN_UP_NEWS_BY_COUNT_VALUE = "org.rssowl.pref.CleanUpNewsByCountValue";

  /** Global: Clean Up: Delete News > N Days (boolean) */
  public static final String CLEAN_UP_NEWS_BY_AGE_STATE = "org.rssowl.pref.CleanUpNewsByAgeState";

  /** Global: Clean Up: Delete News > N Days (int) */
  public static final String CLEAN_UP_NEWS_BY_AGE_VALUE = "org.rssowl.pref.CleanUpNewsByAgeValue";

  /** Global: Clean Up: Delete read News (boolean) */
  public static final String CLEAN_UP_READ_NEWS_STATE = "org.rssowl.pref.CleanUpReadNewsState";

  /** Global: Clean Up: Never Delete Unread News (boolean) */
  public static final String CLEAN_UP_NEVER_DEL_UNREAD_NEWS_STATE = "org.rssowl.pref.CleanUpNeverDelUnreadNewsState";

  /** Global: Clean Up: The Date of the next reminder for Clean-Up as Long */
  public static final String CLEAN_UP_REMINDER_DATE_MILLIES = "org.rssowl.pref.CleanUpReminderDateMillies";

  /** Global: Clean Up: Enabled state for the reminder for Clean-Up */
  public static final String CLEAN_UP_REMINDER_STATE = "org.rssowl.pref.CleanUpReminderState";

  /** Global: Clean Up: Number of days before showing the reminder for Clean-Up */
  public static final String CLEAN_UP_REMINDER_DAYS_VALUE = "org.rssowl.pref.CleanUpReminderDaysValue";

  /** Global: Search Dialog: State of showing Preview */
  public static final String SEARCH_DIALOG_PREVIEW_VISIBLE = "org.rssowl.pref.SearchDialogPreviewVisible";

  /** Global: Visible Columns in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_COLUMNS = "org.rssowl.pref.SearchDialogNewsColumns";

  /** Global: Sorted Column in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_SORT_COLUMN = "org.rssowl.pref.SearchDialogNewsSortColumn";

  /** Global: Ascended / Descended Sorting in Search Dialog */
  public static final String SEARCH_DIALOG_NEWS_SORT_ASCENDING = "org.rssowl.pref.SearchDialogNewsSortAscending";

  /** Global: Show Toolbar */
  public static final String SHOW_TOOLBAR = "org.rssowl.pref.ShowToolbar";

  /** Global: Show Statusbar */
  public static final String SHOW_STATUS = "org.rssowl.pref.ShowStatus";

  /** Global: Load Title from Feed in Bookmark Wizard */
  public static final String BM_LOAD_TITLE_FROM_FEED = "org.rssowl.pref.BMLoadTitleFromFeed";

  /** Global: Last used Keyword Feed */
  public static final String LAST_KEYWORD_FEED = "org.rssowl.pref.LastKeywordFeed";

  /** Global: Open Browser Tabs in the Background */
  public static final String OPEN_BROWSER_IN_BACKGROUND = "org.rssowl.pref.OpenBrowserInBackground";

  /** Global: Share Provider Order and Enablement */
  public static final String SHARE_PROVIDER_STATE = "org.rssowl.pref.ShareProviderState";

  /**
   * Eclipse Preferences Follow
   */

  /** Global Eclipse: Restore Tabs on startup */
  public static final String ECLIPSE_RESTORE_TABS = "instance/org.eclipse.ui.workbench/USE_IPERSISTABLE_EDITORS";

  /** Global Eclipse: Use multiple Tabs */
  public static final String ECLIPSE_MULTIPLE_TABS = "instance/org.eclipse.ui/SHOW_MULTIPLE_EDITOR_TABS";

  /** Global Eclipse: Autoclose Tabs */
  public static final String ECLIPSE_AUTOCLOSE_TABS = "instance/org.eclipse.ui.workbench/REUSE_OPEN_EDITORS_BOOLEAN";

  /** Global Eclipse: Autoclose Tabs Threshold */
  public static final String ECLIPSE_AUTOCLOSE_TABS_THRESHOLD = "instance/org.eclipse.ui.workbench/REUSE_OPEN_EDITORS";

  /** Global Eclipse: Use Proxy */
  public static final String ECLIPSE_USE_PROXY = "/configuration/org.eclipse.core.net/proxiesEnabled";

  /** Global Eclipse: Use System Proxy */
  public static final String ECLIPSE_USE_SYSTEM_PROXY = "/configuration/org.eclipse.core.net/systemProxiesEnabled";

  /** Global Eclipse: Proxy Host */
  public static final String ECLIPSE_PROXY_HOST = "/configuration/org.eclipse.core.net/proxyData/HTTP/host";

  /** Global Eclipse: Proxy Port */
  public static final String ECLIPSE_PROXY_PORT = "/configuration/org.eclipse.core.net/proxyData/HTTP/port";

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
    defaultScope.putIntegers(SHARE_PROVIDER_STATE, new int[] { 1, 2, 3, 4, 5, 6, 7, -8, -9, -10, -11, -12, -13, -14, -15, -16, -17, -18  }); //TODO Must be in sync with Share Provider contributions
  }

  /**
   * @param defaultScope the container for preferences to fill.
   */
  protected void initGlobalEclipseDefaults(IPreferenceScope defaultScope) {
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
    defaultScope.putBoolean(FV_LAYOUT_VERTICAL, true);
    defaultScope.putIntegers(FV_SASHFORM_WEIGHTS, new int[] { 50, 50 });
    defaultScope.putBoolean(BM_OPEN_SITE_FOR_NEWS, false);
  }
}