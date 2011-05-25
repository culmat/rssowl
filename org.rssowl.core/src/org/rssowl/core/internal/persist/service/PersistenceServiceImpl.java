/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.service.AbstractPersistenceService;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.LongOperationMonitor;
import org.rssowl.core.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The persistence service controls the lifecycle of the underlying database.
 *
 * @author bpasero
 */
public class PersistenceServiceImpl extends AbstractPersistenceService {

  /** Default Constructor */
  public PersistenceServiceImpl() {}

  /*
   * @see
   * org.rssowl.core.persist.service.IPersistenceService#startup(org.rssowl.
   * core.util.LongOperationMonitor, boolean)
   */
  @Override
  public void startup(LongOperationMonitor monitor, boolean emergency) {
    super.startup(monitor, emergency);

    /* Startup DB and Model-Search */
    DBManager.getDefault().startup(monitor, emergency);
    getModelSearch().startup();
  }

  /*
   * @see org.rssowl.core.persist.service.IPersistenceService#shutdown(boolean)
   */
  public void shutdown(boolean emergency) throws PersistenceException {

    /* Shutdown ID Generator, Search and DB */
    if (!emergency) {

      /* ID Generator (safely) */
      try {
        getIDGenerator().shutdown();
      } catch (Exception e) {
        Activator.safeLogError(e.getMessage(), e);
      }

      /* Search (safely) */
      try {
        getModelSearch().shutdown(emergency);
      } catch (Exception e) {
        Activator.safeLogError(e.getMessage(), e);
      }

      /* DB */
      DBManager.getDefault().shutdown();
    }

    /* Emergent Exit: Shutdown DB and Search */
    else {

      /* DB (safely) */
      try {
        DBManager.getDefault().shutdown();
      } catch (Exception e) {
        Activator.safeLogError(e.getMessage(), e);
      }

      /* Search */
      getModelSearch().shutdown(emergency);
    }
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistService#recreateSchema()
   */
  public void recreateSchema(boolean clearSearchIndex) throws PersistenceException {
    DBManager.getDefault().dropDatabase();
    DBManager.getDefault().createDatabase(new LongOperationMonitor(new NullProgressMonitor()) {
      @Override
      public void beginLongOperation(boolean isCancelable) {
        //Do nothing
      }
    }, true);

    if (clearSearchIndex)
      getModelSearch().clearIndex();
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IPersistenceService#optimizeOnNextStartup()
   */
  public void optimizeOnNextStartup() throws PersistenceException {
    try {
      DBManager.getDefault().getDefragmentFile().createNewFile();
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  /*
   * @see org.rssowl.core.persist.service.IPersistenceService#getProfile()
   */
  public Pair<File, Long> getProfile() {
    return DBManager.getDefault().getProfile();
  }

  /*
   * @see org.rssowl.core.persist.service.IPersistenceService#getBackups()
   */
  public List<File> getBackups() {
    return DBManager.getDefault().getBackups();
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IPersistenceService#restore(java.io.File)
   */
  public void restore(File backup) throws PersistenceException {
    DBManager.getDefault().restore(backup);
  }
}