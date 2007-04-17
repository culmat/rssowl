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
package org.rssowl.core.model.persist.dao;

import org.rssowl.core.Owl;
import org.rssowl.core.model.events.EntityListener;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IPersistable;

import java.util.Collection;

public final class DynamicDAO {

  private static final DAOService DAO_SERVICE = Owl.getPersistenceService().getDAOService();

  public static <T extends IPersistable> T load(Class<T> persistableClass, long id) {
    IPersistableDAO<T> dao = getDAOFromEntity(persistableClass);
    return dao.load(id);
  }
  
  public static <T extends IPersistable> Collection<T> loadAll(Class<T> persistableClass)   {
    IPersistableDAO<T> dao = getDAOFromEntity(persistableClass);
    return dao.loadAll();
  }
  
  public static <T extends IPersistable> T save(T persistable) {
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(persistable.getClass());
    return dao.save(persistable);
  }
  
  public static <C extends Collection<T>, T extends IPersistable> C saveAll(C persistables) {
    if (persistables.size() == 0)
      return persistables;
    
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(persistables.iterator().next().getClass());
    return dao.saveAll(persistables);
  }
  
  public static <T extends IPersistable> void delete(T persistable) {
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(persistable.getClass());
    dao.delete(persistable);
  }
  
  public static <T extends IPersistable> void deleteAll(Collection<T> objects) {
    if (objects.size() == 0)
      return;
    
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(objects.iterator().next().getClass());
    dao.deleteAll(objects);
  }

  public static <T extends IPersistable> void countAll(Class<T> persistableClass) {
    IPersistableDAO<T> dao = getDAOFromEntity(persistableClass);
    dao.countAll();
  }
  
  public static <T extends IEntity, L extends EntityListener<E>, E extends ModelEvent>
      void addEntityListener(Class<T> entityClass, L listener) {
    
    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromEntity(entityClass);
    dao.addEntityListener(listener);
  }
  
  public static <T extends IEntity, L extends EntityListener<E>, E extends ModelEvent>
      void removeEntityListener(Class<T> entityClass, L listener) {

    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromEntity(entityClass);
    dao.removeEntityListener(listener);
  }
  
  public static <D extends IPersistableDAO<T>, T extends IPersistable> D getDAO(Class<D> daoInterface)    {
    return DAO_SERVICE.getDAO(daoInterface);
  }
  
  public static <T extends IPersistable> IPersistableDAO<T> getDAOFromEntity(Class<? extends T> persistableClass)  {
    return DAO_SERVICE.getDAOFromEntity(persistableClass);
  }
}
