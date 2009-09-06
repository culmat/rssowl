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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.rssowl.ui.internal.editors.browser.messages"; //$NON-NLS-1$
  public static String WebBrowserInput_LOADING;

  public static String WebBrowserView_BACK;
  public static String WebBrowserView_BLANK_PAGE;
  public static String WebBrowserView_CONFIGURE;
  public static String WebBrowserView_ENTER_WEBSITE_PHRASE;
  public static String WebBrowserView_FIND_FEEDS;
  public static String WebBrowserView_FORWARD;
  public static String WebBrowserView_HOME;
  public static String WebBrowserView_NEW_TAB;
  public static String WebBrowserView_RELOAD;
  public static String WebBrowserView_SHARE_LINK;
  public static String WebBrowserView_STOP;

  private Messages() {}

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}
