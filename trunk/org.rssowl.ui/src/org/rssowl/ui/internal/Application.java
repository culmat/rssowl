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

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.actions.NewBookMarkAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.util.JobRunner;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
  private ApplicationWorkbenchAdvisor fWorkbenchAdvisor;

  /** Constant for the application being run on Windows or not */
  public static final boolean IS_WINDOWS = "win32".equals(SWT.getPlatform());

  /** Constant for the application being run on Linux or not */
  public static final boolean IS_LINUX = "gtk".equals(SWT.getPlatform());

  /** Constant for the application being run on Mac or not */
  public static final boolean IS_MAC = "carbon".equals(SWT.getPlatform());

  /*
   * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
   */
  public Object start(IApplicationContext context) throws Exception {

    /* Start the Application Server */
    ApplicationServer server = ApplicationServer.getDefault();
    try {
      server.startup();
    }

    /* Server alredady bound - perform hand-shake */
    catch (BindException e) {
      String link = parseLink(Platform.getCommandLineArgs());
      doHandshake(link);
    }

    /* Server not yet bound - register hand-shake-handler */
    server.setHandshakeHandler(new ApplicationServer.HandshakeHandler() {
      public void handle(String token) {
        if (StringUtils.isSet(token)) {
          restoreApplication();

          if (URIUtils.looksLikeLink(token))
            handleLinkSupplied(token);
        }
      }
    });

    /* Proceed normally */
    Display display = PlatformUI.createDisplay();
    try {

      /* Handle possible Link supplied after startup */
      Runnable runAfterUIStartup = new Runnable() {
        public void run() {
          String link = parseLink(Platform.getCommandLineArgs());
          if (StringUtils.isSet(link))
            handleLinkSupplied(link);
        }
      };

      /* Create the Workbench */
      fWorkbenchAdvisor = new ApplicationWorkbenchAdvisor(runAfterUIStartup);
      int returnCode = PlatformUI.createAndRunWorkbench(display, fWorkbenchAdvisor);
      if (returnCode == PlatformUI.RETURN_RESTART)
        return IApplication.EXIT_RESTART;

      return IApplication.EXIT_OK;
    } finally {
      display.dispose();
    }
  }

  /*
   * @see org.eclipse.equinox.app.IApplication#stop()
   */
  public void stop() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null)
      return;

    final Display display = workbench.getDisplay();
    display.syncExec(new Runnable() {
      public void run() {
        if (!display.isDisposed())
          workbench.close();
      }
    });
  }

  /* Return the first Link in this Array or NULL otherwise */
  private String parseLink(String[] commandLineArgs) {
    for (String arg : commandLineArgs) {
      if (URIUtils.looksLikeLink(arg))
        return arg;
    }

    return null;
  }

  /* Server already running. Pass a message to the running Server and exit. */
  private void doHandshake(String message) {
    try {
      Socket socket = new Socket(InetAddress.getByName(ApplicationServer.LOCALHOST), ApplicationServer.DEFAULT_SOCKET_PORT);
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
      writer.println(StringUtils.isSet(message) ? message : ApplicationServer.STARTUP_HANDSHAKE);
      writer.flush();

      /*
       * Send a message to the other running instance of RSSOwl and wait some
       * time, so that is has a chance to read the message. After that, the
       * other running instance will restore from taskbar or tray to show the
       * user. Then exit this instance consequently.
       */
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        System.exit(0);
      } finally {
        System.exit(0);
      }
    } catch (UnknownHostException e) {
      Activator.getDefault().logError("handleSocketBound()", e);
    } catch (IOException e) {
      Activator.getDefault().logError("handleSocketBound()", e);
    }
  }

  /* Focus the Application */
  private void restoreApplication() {
    final Shell shell = OwlUI.getPrimaryShell();
    if (shell != null) {
      JobRunner.runInUIThread(shell, new Runnable() {
        public void run() {

          /* Restore from Tray */
          if (fWorkbenchAdvisor.getPrimaryWorkbenchWindowAdvisor().isMinimizedToTray()) {
            fWorkbenchAdvisor.getPrimaryWorkbenchWindowAdvisor().restoreFromTray(shell);
          }

          /* Force Active and De-Iconify */
          else {
            shell.forceActive();
            shell.setMinimized(false);
          }
        }
      });
    }
  }

  /* Handle the supplied Link */
  private void handleLinkSupplied(final String link) {

    /* Need a Shell */
    final Shell shell = OwlUI.getPrimaryShell();
    if (shell == null)
      return;

    /* Check for existing BookMark */
    final IBookMark existingBookMark = getBookMark(link);
    JobRunner.runInUIThread(shell, new Runnable() {
      public void run() {

        /* Open Dialog to add this new BookMark */
        if (existingBookMark == null) {
          new NewBookMarkAction(shell, null, link).run(null);
        }

        /* Display selected Feed since its existing already */
        else {
          IWorkbenchPage page = OwlUI.getPage();
          if (page != null) {
            try {
              page.openEditor(new FeedViewInput(existingBookMark), FeedView.ID, OpenStrategy.activateOnOpen());
            } catch (PartInitException e) {
              Activator.getDefault().getLog().log(e.getStatus());
            }
          }
        }
      }
    });
  }

  private IBookMark getBookMark(String link) {

    /* Need a URI */
    URI linkAsURI;
    try {
      linkAsURI = new URI(link);
    } catch (URISyntaxException e) {
      return null;
    }

    /* Check if a BookMark exists for the Link */
    IApplicationLayer appLayer = Owl.getPersistenceService().getApplicationLayer();
    IFeed feed = appLayer.loadFeed(linkAsURI);
    if (feed != null) {
      FeedLinkReference feedRef = new FeedLinkReference(feed.getLink());
      final List<IBookMark> bookMarks = appLayer.loadBookMarks(feedRef);
      if (bookMarks.size() > 0)
        return bookMarks.get(0);
    }

    return null;
  }
}