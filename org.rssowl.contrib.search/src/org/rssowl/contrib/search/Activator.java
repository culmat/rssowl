
package org.rssowl.contrib.search;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

  /** The plug-in ID */
  public static final String PLUGIN_ID = "org.rssowl.contrib.search"; //$NON-NLS-1$

  // The shared instance
  private static Activator fPlugin;

  /**
   * The constructor
   */
  public Activator() {
    fPlugin = this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    fPlugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return fPlugin;
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
