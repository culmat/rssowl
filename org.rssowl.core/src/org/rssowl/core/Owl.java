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

package org.rssowl.core;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.connection.IConnectionService;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.interpreter.IInterpreterService;
import org.rssowl.core.model.IListenerService;
import org.rssowl.core.model.dao.IApplicationService;
import org.rssowl.core.model.dao.IPersistenceService;
import org.rssowl.core.model.persist.IModelFactory;
import org.rssowl.core.model.persist.pref.IPreferenceService;

/**
 * The <code>Owl</code> class is the main facade to all API in RSSOwl.
 *
 * @author bpasero
 */
public class Owl {

  /** Flag indicating wether the Controller is accessed from a Test */
  public static boolean TESTING = false;

  /**
   * <p>
   * Get the Implementation of <code>IApplicationService</code> that contains
   * special Methods which are used through the Application and access the
   * persistence layer. The implementation is looked up using the
   * "org.rssowl.core.model.ApplicationService" Extension Point.
   * </p>
   * Subclasses may override to provide their own implementation.
   *
   * @return Returns the Implementation of <code>I</code> that contains
   * special Methods which are used through the Application and access the
   * persistence layer.
   */
  public static IApplicationService getApplicationService() {
    return InternalOwl.getDefault().getApplicationService();
  }

  /**
   * @return
   */
  public static IListenerService getListenerService() {
    return InternalOwl.getDefault().getListenerService();
  }

  /**
   * @return
   */
  public static IPreferenceService getPreferenceService() {
    Assert.isTrue(InternalOwl.getDefault().isStarted(), "The Owl facade  has not yet finished initialization");
    return InternalOwl.getDefault().getPreferenceService();
  }

  /**
   * @return
   */
  public static IPersistenceService getPersistenceService() {
    Assert.isTrue(InternalOwl.getDefault().isStarted(), "The Owl facade  has not yet finished initialization");
    return InternalOwl.getDefault().getPersistenceService();
  }

  /**
   * @return
   */
  public static IConnectionService getConnectionService() {
    Assert.isTrue(InternalOwl.getDefault().isStarted(), "The Owl facade  has not yet finished initialization");
    return InternalOwl.getDefault().getConnectionService();
  }

  /**
   * @return
   */
  public static IInterpreterService getInterpreter() {
    Assert.isTrue(InternalOwl.getDefault().isStarted(), "The Owl facade  has not yet finished initialization");
    return InternalOwl.getDefault().getInterpreter();
  }

  /**
   * @return
   */
  public static IModelFactory getModelFactory() {
    Assert.isTrue(InternalOwl.getDefault().isStarted(), "The Owl facade  has not yet finished initialization");
    return InternalOwl.getDefault().getModelFactory();
  }
}