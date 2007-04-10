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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.editors.browser.WebBrowserInput;
import org.rssowl.ui.internal.editors.browser.WebBrowserView;
import org.rssowl.ui.internal.util.BrowserUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of <code>CBrowser</code> wrap around the Browser-Widget and
 * enhance it by some means.
 *
 * @author bpasero
 */
public class CBrowser {

  /* JavaScript: print() Method */
  private static final String JAVA_SCRIPT_PRINT = "window.print();";

  /* Flag to check if Mozilla is available on Windows */
  private static boolean fgMozillaAvailable = true;

  private Browser fBrowser;
  private boolean fBlockNavigation;
  private IPreferenceScope fPreferences;
  private Map<String, ILinkHandler> fLinkHandler;

  /**
   * @param parent The Parent Composite of this Browser.
   * @param style The Style to use for the Browser-
   */
  public CBrowser(Composite parent, int style) {
    fBrowser = createBrowser(parent, style);
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fLinkHandler = new HashMap<String, ILinkHandler>();
    hookListeners();

    /* Add custom Context Menu on OS where this is not supported */
    if (Application.IS_LINUX || fgMozillaAvailable)
      hookMenu();
  }

  /**
   * Adds the given Handler to this instance responsible for the given Command.
   *
   * @param commandId The ID of the Command the provided Handler is responsible
   * for.
   * @param handler The Handler responsible for the fiven ID.
   */
  public void addLinkHandler(String commandId, ILinkHandler handler) {
    fLinkHandler.put(commandId, handler);
  }

  private Browser createBrowser(Composite parent, int style) {
    Browser browser = null;

    /* Try Mozilla over IE on Windows */
    if (Application.IS_WINDOWS && fgMozillaAvailable) {
      try {
        browser = new Browser(parent, style | SWT.MOZILLA);
      } catch (SWTError e) {
        fgMozillaAvailable = false;
        Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), null));
      }
    }

    /* Any other OS, or Mozilla unavailable, use default */
    if (browser == null)
      browser = new Browser(parent, style);

    /* Add Focusless Scroll Hook on Windows */
    if (Application.IS_WINDOWS)
      browser.setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, true);

    /* Clear all Link Handlers upon disposal */
    browser.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        fLinkHandler.clear();
      }
    });

    return browser;
  }

  private void hookMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {

        /* Back */
        manager.add(new Action("Back") {
          @Override
          public void run() {
            fBrowser.back();
          }

          @Override
          public boolean isEnabled() {
            return fBrowser.isBackEnabled();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/backward.gif");
          }
        });

        /* Forward */
        manager.add(new Action("Forward") {
          @Override
          public void run() {
            fBrowser.forward();
          }

          @Override
          public boolean isEnabled() {
            return fBrowser.isForwardEnabled();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/forward.gif");
          }
        });

        /* Reload */
        manager.add(new Separator());
        manager.add(new Action("Reload") {
          @Override
          public void run() {
            fBrowser.refresh();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/elcl16/reload.gif");
          }
        });

        /* Stop */
        manager.add(new Action("Stop") {
          @Override
          public void run() {
            fBrowser.stop();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/cancel.gif");
          }
        });
      }
    });

    Menu menu = manager.createContextMenu(fBrowser);
    fBrowser.setMenu(menu);
  }

  /**
   * Returns the Browser-Widget this class is wrapping.
   *
   * @return The Browser-Widget this class is wrapping.
   */
  public Browser getControl() {
    return fBrowser;
  }

  /**
   * Browse to the given URL.
   *
   * @param url The URL to browse to.
   */
  public void setUrl(String url) {
    fBlockNavigation = false;
    fBrowser.setUrl(url);
  }

  /**
   * Navigate to the previous session history item.
   *
   * @return <code>true</code> if the operation was successful and
   * <code>false</code> otherwise
   */
  public boolean back() {
    fBlockNavigation = false;
    return fBrowser.back();
  }

  /**
   * Navigate to the next session history item.
   *
   * @return <code>true</code> if the operation was successful and
   * <code>false</code> otherwise
   */
  public boolean forward() {
    fBlockNavigation = false;
    return fBrowser.forward();
  }

  /**
   * Print the Browser using the JavaScript print() method
   *
   * @return <code>TRUE</code> in case of success, <code>FALSE</code>
   * otherwise
   */
  public boolean print() {
    return fBrowser.execute(JAVA_SCRIPT_PRINT);
  }

  private void hookListeners() {

    /* Listen to Open-Window-Changes */
    fBrowser.addOpenWindowListener(new OpenWindowListener() {
      public void open(WindowEvent event) {

        /* Do not handle when external Browser is being used */
        if (useExternalBrowser())
          return;

        /* Open Browser in new Tab */
        WebBrowserInput input = new WebBrowserInput(URIUtils.ABOUT_BLANK);
        IWorkbenchPage page = OwlUI.getPage();
        if (page != null) {
          try {
            WebBrowserView browserView = (WebBrowserView) page.openEditor(input, WebBrowserView.EDITOR_ID, OpenStrategy.activateOnOpen());
            event.browser = browserView.getBrowser().getControl();
          } catch (PartInitException e) {
            Activator.getDefault().getLog().log(e.getStatus());
          }
        }
      }
    });

    /* Listen to Location-Changes */
    fBrowser.addLocationListener(new LocationListener() {
      public void changed(LocationEvent event) {
        if (event.top && useExternalBrowser())
          fBlockNavigation = true;
      }

      public void changing(LocationEvent event) {

        /* Handle Application Protocol */
        if (event.location != null && event.location.contains(ILinkHandler.HANDLER_PROTOCOL)) {
          try {
            URI link = new URI(event.location);
            String host = link.getHost();
            if (StringUtils.isSet(host) && fLinkHandler.containsKey(host)) {
              fLinkHandler.get(host).handle(host, link);
              event.doit = false;
              return;
            }
          } catch (URISyntaxException e) {
            Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }

        /* Feature not enabled */
        if (!useExternalBrowser())
          return;

        /*
         * Bug on Mac: Safari puts out links from images as event.location
         * resulting in RSSOwl to open a browser although its not necessary
         * Workaround is to disable this feature on Mac until its fixed. (see
         * Bug #1068304).
         */
        if (Application.IS_MAC)
          return;

        /* Only proceed if navigation should not be blocked */
        if (!fBlockNavigation)
          return;

        /* Let local ApplicationServer URLs open */
        if (ApplicationServer.getDefault().isNewsServerUrl(event.location))
          return;

        /* The URL must not be empty or about:blank (Problem on Linux) */
        if (!StringUtils.isSet(event.location) || URIUtils.ABOUT_BLANK.equals(event.location))
          return;

        /* Finally, cancel event and open URL external */
        event.doit = false;
        BrowserUtils.openLink(event.location);
      }
    });
  }

  private boolean useExternalBrowser() {
    return fPreferences.getBoolean(DefaultPreferences.USE_EXTERNAL_BROWSER);
  }
}