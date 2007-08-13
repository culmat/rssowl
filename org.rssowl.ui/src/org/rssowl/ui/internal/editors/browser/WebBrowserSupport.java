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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.AbstractWorkbenchBrowserSupport;
import org.eclipse.ui.browser.IWebBrowser;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.BrowserUtils;

import java.net.URL;

/**
 * RSSOwl's own support for an embedded Browser. Will respect the "Use external
 * Browser" setting to open a Link externally if set.
 *
 * @author bpasero
 */
public class WebBrowserSupport extends AbstractWorkbenchBrowserSupport {

  /** Leave Default Constructor for Reflection */
  public WebBrowserSupport() {}

  /*
   * @see org.eclipse.ui.browser.IWorkbenchBrowserSupport#createBrowser(java.lang.String)
   */
  public IWebBrowser createBrowser(final String browserId) {
    Assert.isNotNull(browserId);

    /* Create WebBrowser and return */
    return new IWebBrowser() {
      private IEditorPart fBrowserView;

      /*
       * @see org.eclipse.ui.browser.IWebBrowser#openURL(java.net.URL)
       */
      public void openURL(URL url) throws PartInitException {
        Assert.isNotNull(url);

        /* Open externally */
        if (useExternalBrowser())
          openExternal(url);

        /* Open internally */
        else
          openInternal(url);
      }

      private boolean useExternalBrowser() {
        IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();
        return globalScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || globalScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);
      }

      private void openInternal(URL url) throws PartInitException {
        WebBrowserInput input = new WebBrowserInput(url.toExternalForm());
        IWorkbenchPage page = OwlUI.getPage();
        if (page != null)
          fBrowserView = page.openEditor(input, WebBrowserView.EDITOR_ID);
      }

      private void openExternal(URL url) {
        BrowserUtils.openLink(url.toExternalForm());
      }

      /*
       * @see org.eclipse.ui.browser.IWebBrowser#close()
       */
      public boolean close() {
        IWorkbenchPage page = OwlUI.getPage();
        if (page != null && fBrowserView != null)
          page.closeEditor(fBrowserView, false);

        return true;
      }

      /*
       * @see org.eclipse.ui.browser.IWebBrowser#getId()
       */
      public String getId() {
        return browserId;
      }
    };
  }

  /*
   * @see org.eclipse.ui.browser.IWorkbenchBrowserSupport#createBrowser(int,
   * java.lang.String, java.lang.String, java.lang.String)
   */
  public IWebBrowser createBrowser(int style, String browserId, String name, String tooltip) {
    return createBrowser(browserId);
  }

  /*
   * @see org.eclipse.ui.browser.AbstractWorkbenchBrowserSupport#isInternalWebBrowserAvailable()
   */
  @Override
  public boolean isInternalWebBrowserAvailable() {
    return true;
  }
}