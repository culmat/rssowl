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
import org.rssowl.core.persist.event.EntityListener;
import org.rssowl.core.persist.event.ModelEvent;

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

  public static boolean exists(Class<? extends IEntity> entityClass, long id) {
    IEntityDAO<?, ?, ?> dao = getDAOFromEntity(entityClass);
    return dao.exists(id);
  }

  public static <T extends IEntity> T load(Class<T> entityClass, long id) {
    IEntityDAO<T, ?, ?> dao = getDAOFromEntity(entityClass);
    return dao.load(id);
  }

  public static <T extends IPersistable> Collection<T> loadAll(Class<T> persistableClass) {
    IPersistableDAO<T> dao = getDAOFromPersistable(persistableClass);
    return dao.loadAll();
  }

  @SuppressWarnings("unchecked")
  public static <T extends IPersistable> T save(T persistable) {
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromPersistable(persistable.getClass());
    return dao.save(persistable);
  }

  @SuppressWarnings("unchecked")
  public static <T extends IPersistable> void saveAll(Collection<T> persistables) {
    if (persistables.size() == 0)
      return;

    Map<Class< ? extends IPersistable>, List<T>> persistablesMap = getPersistablesMap(persistables);
    for (Map.Entry<Class< ? extends IPersistable>, List<T>> entry : persistablesMap.entrySet()) {
      IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromPersistable(entry.getKey());
      dao.saveAll(entry.getValue());
    }
  }

  private static <T extends IPersistable> Map<Class< ? extends IPersistable>, List<T>> getPersistablesMap(Collection<T> persistables) {
    Map<Class< ? extends IPersistable>, List<T>> persistablesMap = new LinkedHashMap<Class< ? extends IPersistable>, List<T>>(3);
    for (T persistable : persistables) {
      Class< ? extends IPersistable> persistableClass = persistable.getClass();
      List<T> persistableList = persistablesMap.get(persistableClass);
      if (persistableList == null) {
        persistableList = new ArrayList<T>(persistables.size());
        persistablesMap.put(persistableClass, persistableList);
      }
      persistableList.add(persistable);
    }
    return persistablesMap;
  }

  @SuppressWarnings("unchecked")
  public static <T extends IPersistable> void delete(T persistable) {
    IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromPersistable(persistable.getClass());
    dao.delete(persistable);
  }

  @SuppressWarnings("unchecked")
  public static <T extends IPersistable> void deleteAll(Collection<T> persistables) {
    if (persistables.size() == 0)
      return;

    Map<Class< ? extends IPersistable>, List<T>> persistablesMap = getPersistablesMap(persistables);
    for (Map.Entry<Class< ? extends IPersistable>, List<T>> entry : persistablesMap.entrySet()) {
      IPersistableDAO<T> dao = (IPersistableDAO<T>) getDAOFromPersistable(entry.getKey());
      dao.deleteAll(entry.getValue());
    }
  }

  public static <T extends IPersistable> void countAll(Class<T> persistableClass) {
    IPersistableDAO<T> dao = getDAOFromPersistable(persistableClass);
    dao.countAll();
  }

  public static <T extends IEntity, L extends EntityListener<E, T>, E extends ModelEvent> void addEntityListener(Class<T> entityClass, L listener) {
    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromPersistable(entityClass);
    dao.addEntityListener(listener);
  }

  public static <T extends IEntity, L extends EntityListener<E, T>, E extends ModelEvent> void removeEntityListener(Class<T> entityClass, L listener) {

    IEntityDAO<T, L, E> dao = (IEntityDAO<T, L, E>) getDAOFromPersistable(entityClass);
    dao.removeEntityListener(listener);
  }

  public static <D extends IPersistableDAO<T>, T extends IPersistable> D getDAO(Class<D> daoInterface) {
    return getDAOService().getDAO(daoInterface);
  }

  public static <T extends IPersistable> IPersistableDAO<T> getDAOFromPersistable(Class< ? extends T> persistableClass) {
    return getDAOService().getDAOFromPersistable(persistableClass);
  }

  public static <T extends IEntity> IEntityDAO<T, ?, ?> getDAOFromEntity(Class< ? extends T> entityClass)  {
    return getDAOService().getDAOFromPersistable(entityClass);
  }
}