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
import org.rssowl.core.internal.InternalOwl;
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
  public void recreateSchema() throws PersistenceException {
    DBManager.getDefault().dropDatabase();
    DBManager.getDefault().createDatabase(new LongOperationMonitor(new NullProgressMonitor()) {
      @Override
      public void beginLongOperation(boolean isCancelable) {
        //Do nothing
      }
    }, true);

    getModelSearch().clearIndex();
  }

  /**
   * Instructs the persistence service to schedule an optimization run during
   * the next time the application is started. The actual optimization type is
   * dependent on the persistence system being used and implementors are free to
   * leave this as a no-op in case the the persistence system tunes itself
   * automatically during runtime.
   *
   * @throws PersistenceException in case a problem occurs while trying to
   * schedule this operation.
   */
  public void defragmentOnNextStartup() throws PersistenceException {
    try {
      DBManager.getDefault().getDefragmentFile().createNewFile();
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  /**
   * Returns the profile {@link File} that contains all data and the
   * {@link Long} timestamp when it was last successfully used.
   *
   * @return the profile {@link File} and the {@link Long} timestamp when it was
   * last successfully used.
   */
  public Pair<File, Long> getProfile() {
    return DBManager.getDefault().getProfile();
  }

  /**
   * Provides a list of available backups for the user to restore from in case
   * of an unrecoverable error.
   *
   * @return a list of available backups for the user to restore from in case of
   * an unrecoverable error.
   */
  public List<File> getProfileBackups() {
    return DBManager.getDefault().getProfileBackups();
  }

  /**
   * Will rename the provided backup file to the operational RSSOwl profile
   * database.
   *
   * @param backup the backup {@link File} to restore from.
   * @throws PersistenceException in case a problem occurs while trying to
   * execute this operation.
   */
  public void restoreProfile(File backup) throws PersistenceException {
    DBManager.getDefault().restoreProfile(backup);
  }

  /**
   * Recreate the Profile of the persistence layer. In case of a Database, this
   * would drop relations and create them again.
   *
   * @throws PersistenceException In case of an error while starting up the
   * persistence layer.
   */
  public void recreateProfile() throws PersistenceException {

    /* Backup DB */
    DBManager.getDefault().backupProfile();

    /* Drop DB Schema */
    DBManager.getDefault().dropDatabase();
    
    /* Create Empty Database */
    DBManager.getDefault().createDatabase(new LongOperationMonitor(new NullProgressMonitor()) {
      @Override
      public void beginLongOperation(boolean isCancelable) {
        //Do nothing
      }
    }, true);

    /* Make sure to be started */
    if (!InternalOwl.getDefault().isStarted())
      InternalOwl.getDefault().startup(new LongOperationMonitor(new NullProgressMonitor()) {}, true);

    /* Reindex Search on next startup */
   getModelSearch().reIndexOnNextStartup();
  }
}