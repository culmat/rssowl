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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * The main plugin class to be used in the desktop.
 */
public class Activator extends AbstractUIPlugin {

  /** ID of this Plugin */
  public static final String PLUGIN_ID = "org.rssowl.ui"; //$NON-NLS-1$

  /* The reg. expression for an URL */
  private static final String URL_REGEX = "(www([\\wv\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?)|(http|ftp|https):\\/\\/[\\w]+(.[\\w]+)([\\wv\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"; //$NON-NLS-1$

  /* The compiled pattern to match an URL */
  private static final Pattern URL_REGEX_PATTERN = Pattern.compile(URL_REGEX);

  private static Activator fgPlugin;

  /**
   * The constructor.
   */
  public Activator() {
    fgPlugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    /* Start internal Server (chance that System.exit() gets called!) */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        startServer();
      }
    });

    /* Propagate startup to Controller */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        Controller.getDefault().startup();
      }
    });

    /* Propagate post-ui startup to Controller */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            Controller.getDefault().postUIStartup();
          }
        });
      }
    });
  }

  /* Start the Application Server */
  private void startServer() {
    ApplicationServer server = ApplicationServer.getDefault();
    try {
      server.startup();
    }

    /* Server alredady bound - perform hand-shake */
    catch (BindException e) {
      String link = parseLink(Platform.getCommandLineArgs());
      doHandshake(link);
    }

    /* Log any IOException */
    catch (IOException e) {
      logError(e.getMessage(), e);
    }
  }

  /* Return the first Link in this Array or NULL otherwise */
  private String parseLink(String[] commandLineArgs) {
    for (String arg : commandLineArgs) {
      if (looksLikeLink(arg))
        return arg;
    }

    return null;
  }

  /*
   * Return TRUE in case the given String looks like a Link.
   */
  private boolean looksLikeLink(String str) {

    /* Is empty or null? */
    if (!isSet(str))
      return false;

    /* Contains whitespaces ? */
    if (str.indexOf(' ') >= 0)
      return false;

    /* RegEx Link check */
    if (URL_REGEX_PATTERN.matcher(str).matches())
      return true;

    /* Try creating an URL object */
    try {
      new URL(str);
    } catch (MalformedURLException e) {
      return false;
    }

    /* String is an URL */
    return true;
  }

  /*
   * Returns TRUE in case the given String has a value that is not "" or <code>NULL</code>.
   */
  private boolean isSet(String str) {
    return (str != null && str.length() > 0);
  }

  /* Server already running. Pass a message to the running Server and exit. */
  private void doHandshake(String message) {
    try {
      Socket socket = new Socket(InetAddress.getByName(ApplicationServer.LOCALHOST), ApplicationServer.DEFAULT_SOCKET_PORT);
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
      writer.println(isSet(message) ? message : ApplicationServer.STARTUP_HANDSHAKE);
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

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {

    /* Propagate shutdown to Controller */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        Controller.getDefault().shutdown();
      }
    });

    /* Proceed */
    super.stop(context);
    fgPlugin = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return The shared instance.
   */
  public static Activator getDefault() {
    return fgPlugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path.
   *
   * @param path the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    return AbstractUIPlugin.imageDescriptorFromPlugin("org.rssowl.ui", path); //$NON-NLS-1$
  }

  /**
   * Log an Info Message.
   *
   * @param msg The message to log as Info.
   */
  public void logInfo(@SuppressWarnings("unused")
  String msg) {
  // TODO Need a better logging facility here
  // getLog().log(new Status(IStatus.INFO, getBundle().getSymbolicName(),
  // IStatus.OK, msg, null));
  }

  /**
   * Log an Error Message.
   *
   * @param msg The message to log as Error.
   * @param e The occuring Exception to log.
   */
  public void logError(String msg, Exception e) {
    if (msg == null)
      msg = ""; //$NON-NLS-1$

    getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), IStatus.ERROR, msg, e));
  }

  /**
   * Create a IStatus out of the given message and exception.
   *
   * @param msg The message describing the error.
   * @param e The Exception that occured.
   * @return An IStatus out of the given message and exception.
   */
  public IStatus createErrorStatus(String msg, Exception e) {
    return new Status(IStatus.ERROR, Activator.getDefault().getBundle().getSymbolicName(), IStatus.ERROR, msg, e);
  }
}