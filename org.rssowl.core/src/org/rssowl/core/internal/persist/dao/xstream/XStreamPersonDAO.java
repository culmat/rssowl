/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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
package org.rssowl.core.internal.persist.dao.xstream;

import org.rssowl.core.internal.persist.Person;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.dao.IPersonDAO;
import org.rssowl.core.persist.event.PersonEvent;
import org.rssowl.core.persist.event.PersonListener;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class XStreamPersonDAO extends XStreamEntityDAO<IPerson, PersonListener, PersonEvent> implements IPersonDAO {

  public XStreamPersonDAO(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    super(Person.class, baseDir, xStream, idGenerator, eventManager);
  }

  public Set<String> loadAllNames() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public boolean exists(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public IPerson load(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public long countAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void delete(IPerson persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void deleteAll(Collection<IPerson> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public Collection<IPerson> loadAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public IPerson save(IPerson persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void saveAll(Collection<IPerson> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

}
