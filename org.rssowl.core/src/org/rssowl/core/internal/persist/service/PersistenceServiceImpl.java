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

package org.rssowl.core.internal.persist.service;

import org.rssowl.core.persist.service.AbstractPersistenceService;
import org.rssowl.core.persist.service.PersistenceException;

/**
 * @author bpasero
 */
public class PersistenceServiceImpl extends AbstractPersistenceService {

  /** */
  public PersistenceServiceImpl() {
  }

  /*
   * Startup the persistence layer. In case of a Database, this would be the
   * right place to create relations. Subclasses should override. @throws
   * PersistenceException In case of an error while starting up the persistence
   * layer.
   */
  @Override
  public void startup() throws PersistenceException {
    super.startup();
    /* Startup DB and Model-Search */
    DBManager.getDefault().startup();
    getModelSearch().startup();
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistService#shutdown()
   */
  public void shutdown() throws PersistenceException {
    getIDGenerator().shutdown();
    getModelSearch().shutdown();
    DBManager.getDefault().shutdown();
  }

  /*
   * @see org.rssowl.core.model.dao.IPersistService#recreateSchema()
   */
  public void recreateSchema() throws PersistenceException {
    DBManager.getDefault().dropDatabase();
    DBManager.getDefault().createDatabase();
    getModelSearch().clearIndex();
  }
}