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

package org.rssowl.core.internal;

import org.rssowl.core.connection.IConnectionService;
import org.rssowl.core.connection.internal.ConnectionServiceImpl;
import org.rssowl.core.interpreter.IInterpreterService;
import org.rssowl.core.interpreter.internal.InterpreterServiceImpl;
import org.rssowl.core.model.IListenerService;
import org.rssowl.core.model.dao.IPersistenceService;
import org.rssowl.core.model.internal.ListenerServiceImpl;
import org.rssowl.core.model.internal.persist.DefaultModelTypesFactory;
import org.rssowl.core.model.internal.persist.pref.PreferenceServiceImpl;
import org.rssowl.core.model.persist.IModelFactory;
import org.rssowl.core.model.persist.pref.IPreferenceService;
import org.rssowl.core.util.ExtensionUtils;

/**
 * The <code>InternalOwl</code> is being used from the public <code>Owl</code>
 * facade.
 *
 * @author bpasero
 */
public final class InternalOwl {

  /* The Singleton Instance */
  private static final InternalOwl INSTANCE = new InternalOwl();

  /* Extension Point: Factory for Model Types */
  private static final String MODEL_TYPESFACTORY_EXTENSION_POINT = "org.rssowl.core.ModelTypesFactory"; //$NON-NLS-1$

  /* Extension Point: Persistence Layer */
  private static final String PERSISTANCE_LAYER_EXTENSION_POINT = "org.rssowl.core.PersistanceLayer"; //$NON-NLS-1$

  private IListenerService fListenerService = new ListenerServiceImpl();
  private IPreferenceService fPreferencesService;
  private IConnectionService fConnectionService;
  private IInterpreterService fInterpreterService;
  private IPersistenceService fPersistenceService;
  private IModelFactory fModelFactory;
  private boolean fStarted;

  private InternalOwl() {}

  /** <em>Never</em> change the ordering of this method's calls! */
  public void startup() {
    fModelFactory = loadTypesFactory();
    fPersistenceService = loadPersistenceService();
    fPersistenceService.startup();
    fConnectionService = new ConnectionServiceImpl();
    fInterpreterService = new InterpreterServiceImpl();
    fPreferencesService = new PreferenceServiceImpl();

    /* Flag as started */
    fStarted = true;
  }

  /**
   * @return
   */
  public static InternalOwl getDefault() {
    return INSTANCE;
  }

  /**
   * @return
   */
  public boolean isStarted() {
    return fStarted;
  }

  /** */
  public void shutdown() {
    fConnectionService.shutdown();
    fPersistenceService.shutdown();
  }

  /**
   * @return
   */
  public IListenerService getListenerService() {
    return fListenerService;
  }

  /**
   * @return
   */
  public IPreferenceService getPreferenceService() {
    return fPreferencesService;
  }

  /**
   * @return
   */
  public IPersistenceService getPersistenceService() {
    return fPersistenceService;
  }

  /* Load the contributed persistence service */
  private IPersistenceService loadPersistenceService() {
    return (IPersistenceService) ExtensionUtils.loadSingletonExecutableExtension(PERSISTANCE_LAYER_EXTENSION_POINT);
  }

  /**
   * @return
   */
  public IConnectionService getConnectionService() {
    return fConnectionService;
  }

  /**
   * @return
   */
  public IInterpreterService getInterpreter() {
    return fInterpreterService;
  }

  /**
   * @return
   */
  public IModelFactory getModelFactory() {
    return fModelFactory;
  }

  /* Load Model Types Factory contribution */
  private IModelFactory loadTypesFactory() {
    IModelFactory defaultFactory = new DefaultModelTypesFactory();
    return (IModelFactory) ExtensionUtils.loadSingletonExecutableExtension(MODEL_TYPESFACTORY_EXTENSION_POINT, defaultFactory);
  }
}