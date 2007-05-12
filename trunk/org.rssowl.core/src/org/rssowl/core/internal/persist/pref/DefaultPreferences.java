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

package org.rssowl.core.internal.persist.pref;

import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.pref.IPreferencesInitializer;

/**
 * An instance of <code>IPreferencesInitializer</code> responsible for
 * defining the default preferences of RSSOwl.
 *
 * @author bpasero
 */
public class DefaultPreferences implements IPreferencesInitializer {

  /** Global: Mark all news as read on minimize */
  public static final String MARK_READ_ON_MINIMIZE = "org.rssowl.pref.MarkNewsReadOnMinimize";

  /** Global: Mark feed as read when feed changes */
  public static final String MARK_FEED_READ_ON_CHANGE = "org.rssowl.pref.MarkFeedReadOnChange";

  /** Global: Use external browser */
  public static final String USE_EXTERNAL_BROWSER = "org.rssowl.pref.UseExternalBrowser";

  /** Global: Minimize to the system tray */
  public static final String TRAY_ON_MINIMIZE = "org.rssowl.pref.UseSystemTray";

  /** Global: Minimize to the system tray on Shell Close */
  public static final String TRAY_ON_CLOSE = "org.rssowl.pref.TrayOnExit";

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

  /** BookMark News-Grouping */
  public static final String BM_NEWS_FILTERING = "org.rssowl.pref.BMNewsFiltering";

  /** BookMark News-Filtering */
  public static final String BM_NEWS_GROUPING = "org.rssowl.pref.BMNewsGrouping";

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

  /*
   * @see org.rssowl.core.model.preferences.IPreferencesInitializer#initialize(org.rssowl.core.model.preferences.IPreferencesScope)
   */
  public void initialize(IPreferenceScope defaultScope) {

    /* Default Globals */
    initGlobalDefaults(defaultScope);

    /* Default Retention Policy */
    initRetentionDefaults(defaultScope);

    /* Default Display Settings */
    initDisplayDefaults(defaultScope);

    /* Default BookMark Explorer */
    initBookMarkExplorerDefaults(defaultScope);

    /* Default Feed View */
    initFeedViewDefaults(defaultScope);

    /* Default Reload/Open Settings */
    initReloadOpenDefaults(defaultScope);
  }

  private void initGlobalDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(MARK_READ_ON_MINIMIZE, false);
    defaultScope.putBoolean(MARK_FEED_READ_ON_CHANGE, false);
    defaultScope.putBoolean(USE_EXTERNAL_BROWSER, true);
    defaultScope.putBoolean(TRAY_ON_MINIMIZE, false);
    defaultScope.putBoolean(MARK_READ_STATE, true);
    defaultScope.putInteger(MARK_READ_IN_MILLIS, 0);
    defaultScope.putBoolean(BM_OPEN_SITE_FOR_EMPTY_NEWS, false);
    defaultScope.putBoolean(BE_ENABLE_LINKING, true);
  }

  private void initRetentionDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(DEL_NEWS_BY_COUNT_STATE, true);
    defaultScope.putInteger(DEL_NEWS_BY_COUNT_VALUE, 200);
    defaultScope.putInteger(DEL_NEWS_BY_AGE_VALUE, 30);
  }

  private void initDisplayDefaults(IPreferenceScope defaultScope) {
    defaultScope.putInteger(BM_NEWS_FILTERING, -1);
    defaultScope.putInteger(BM_NEWS_GROUPING, -1);
  }

  private void initReloadOpenDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(BM_UPDATE_INTERVAL_STATE, true);
    defaultScope.putLong(BM_UPDATE_INTERVAL, 60 * 30); // 30 Minutes
    defaultScope.putBoolean(BM_OPEN_ON_STARTUP, false);
    defaultScope.putBoolean(BM_RELOAD_ON_STARTUP, false);
  }

  private void initBookMarkExplorerDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(BE_BEGIN_SEARCH_ON_TYPING, true);
    defaultScope.putBoolean(BE_SORT_BY_NAME, false);
  }

  private void initFeedViewDefaults(IPreferenceScope defaultScope) {
    defaultScope.putBoolean(FV_LAYOUT_VERTICAL, true);
    defaultScope.putIntegers(FV_SASHFORM_WEIGHTS, new int[] { 50, 50 });
    defaultScope.putBoolean(BM_OPEN_SITE_FOR_NEWS, false);
  }
}