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

import org.rssowl.core.internal.persist.Category;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.persist.event.CategoryEvent;
import org.rssowl.core.persist.event.CategoryListener;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class XStreamCategoryDAO extends XStreamEntityDAO<ICategory, CategoryListener, CategoryEvent> implements ICategoryDAO {

  public XStreamCategoryDAO(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    super(Category.class, baseDir, xStream, idGenerator, eventManager);
  }

  public Set<String> loadAllNames() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public boolean exists(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public ICategory load(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public long countAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void delete(ICategory persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void deleteAll(Collection<ICategory> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public Collection<ICategory> loadAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public ICategory save(ICategory persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void saveAll(Collection<ICategory> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }
}
