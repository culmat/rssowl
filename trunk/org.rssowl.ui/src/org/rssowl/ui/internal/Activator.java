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
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class Activator extends AbstractUIPlugin {

  /** ID of this Plugin */
  public static final String PLUGIN_ID = "org.rssowl.ui"; //$NON-NLS-1$

  private static Activator plugin;

  /**
   * The constructor.
   */
  public Activator() {
    plugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return The shared instance.
   */
  public static Activator getDefault() {
    return plugin;
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