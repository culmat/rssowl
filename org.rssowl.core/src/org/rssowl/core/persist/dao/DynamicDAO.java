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
package org.rssowl.core.persist.dao;

import org.rssowl.core.Owl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.events.EntityListener;
import org.rssowl.core.persist.events.ModelEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamicDAO {

  private static DAOService DAO_SERVICE;
  
  private synchronized static final DAOService getDAOService() {
    if (DAO_SERVICE == null)
      DAO_SERVICE = Owl.getPersistenceService().getDAOService();
    
    return DAO_SERVICE;
  }
  
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
  
  public static <T extends IPersistable> void saveAll(Collection<T> persistables) {
    if (persistables.size() == 0)
      return;
    
    Map<Class< ? extends IPersistable>, List<T>> persistablesMap = getPersistablesMap(persistables);
    for (Map.Entry<Class<? extends IPersistable>, List<T>> entry : persistablesMap.entrySet()) {
      IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(entry.getKey());
      dao.saveAll(entry.getValue());
    }
  }

  private static <T extends IPersistable> Map<Class<? extends IPersistable>, List<T>> getPersistablesMap(Collection<T> persistables) {
    Map<Class<? extends IPersistable>, List<T>> persistablesMap = new LinkedHashMap<Class<? extends IPersistable>, List<T>>(3);
    for (T persistable : persistables) {
      Class<? extends IPersistable> persistableClass = persistable.getClass();
      List<T> persistableList = persistablesMap.get(persistableClass);
      if (persistableList == null) {
        persistableList = new ArrayList<T>(persistables.size());
        persistablesMap.put(persistableClass, persistableList);
      }
      persistableList.add(persistable);
    }
    return persistablesMap;
  }
  
  public static <T extends IPersistable> void delete(T persistable) {
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(persistable.getClass());
    dao.delete(persistable);
  }
  
  public static <T extends IPersistable> void deleteAll(Collection<T> persistables) {
    if (persistables.size() == 0)
      return;
    
    Map<Class< ? extends IPersistable>, List<T>> persistablesMap = getPersistablesMap(persistables);
    for (Map.Entry<Class<? extends IPersistable>, List<T>> entry : persistablesMap.entrySet()) {
      IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromEntity(entry.getKey());
      dao.deleteAll(entry.getValue());
    }
  }

  public static <T extends IPersistable> void countAll(Class<T> persistableClass) {
    IPersistableDAO<T> dao = getDAOFromEntity(persistableClass);
    dao.countAll();
  }
  
  public static <T extends IEntity, L extends EntityListener<E, T>, E extends ModelEvent>
      void addEntityListener(Class<T> entityClass, L listener) {
    
    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromEntity(entityClass);
    dao.addEntityListener(listener);
  }
  
  public static <T extends IEntity, L extends EntityListener<E, T>, E extends ModelEvent>
      void removeEntityListener(Class<T> entityClass, L listener) {

    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromEntity(entityClass);
    dao.removeEntityListener(listener);
  }
  
  public static <D extends IPersistableDAO<T>, T extends IPersistable> D getDAO(Class<D> daoInterface)    {
    return getDAOService().getDAO(daoInterface);
  }
  
  public static <T extends IPersistable> IPersistableDAO<T> getDAOFromEntity(Class<? extends T> persistableClass)  {
    return getDAOService().getDAOFromEntity(persistableClass);
  }
}
