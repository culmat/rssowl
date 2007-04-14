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
package org.rssowl.core.model.internal.db4o.dao;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.EntityListener;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.model.internal.db4o.DBManager;
import org.rssowl.core.model.internal.db4o.DatabaseEvent;
import org.rssowl.core.model.internal.db4o.DatabaseListener;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.util.LoggingSafeRunnable;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public abstract class AbstractEntityDAO<T extends IPersistable,
    L extends EntityListener<E>, E extends ModelEvent> {

  private final List<L> entityListeners = new CopyOnWriteArrayList<L>();
  private final Class<T> fEntityClass;

  private ReadWriteLock fLock;
  private Lock fWriteLock;
  private ObjectContainer fDb;
  
  /**
   * Creates an instance of this class.
   */
  public AbstractEntityDAO(Class<T> entityClass) {
    Assert.isNotNull(entityClass, "entityClass");
    fEntityClass = entityClass;
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
        fLock = event.getLock();
        fWriteLock = fLock.writeLock();
      }
      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }
  
  protected abstract boolean isSaveFully();
  
  protected abstract E createSaveEventTemplate(T entity);
  
  protected abstract E createDeleteEventTemplate(T entity);
  
  public T load(long id) {
    try {
      Query query = fDb.query();
      query.constrain(fEntityClass);
      query.descend("fId").constrain(Long.valueOf(id)); //$NON-NLS-1$

      @SuppressWarnings("unchecked")
      ObjectSet<T> set = query.execute();
      for (T entity : set) {
        // TODO Activate completely by default for now. Must decide how to deal
        // with this.
        fDb.activate(entity, Integer.MAX_VALUE);
        return entity;
      }
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
    return null;
  }
  
  public Collection<T> loadAll()  {
    try {
      ObjectSet<T> entities = fDb.query(fEntityClass);
      activateAll(entities);

      return new ArrayList<T>(entities);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }
  
  protected final <O> Collection<O> activateAll(Collection<O> list) {
    for (O o : list)
      fDb.ext().activate(o, Integer.MAX_VALUE);

    return list;
  }
  
  public T save(T object) {
    saveAll(Collections.singletonList(object));
    return object;
  }
  
  public <C extends Collection<T>> C saveAll(C objects) {
    fWriteLock.lock();
    try {
      for (T object : objects) {
        E event = createSaveEventTemplate(object);
        DBHelper.putEventTemplate(event);
        doSave(object);
      }
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
    return objects;
  }
  
  protected void doSave(T entity) {
    if (isSaveFully())
      fDb.ext().set(entity, Integer.MAX_VALUE);
    else
      fDb.set(entity);
  }

  public void delete(T object) {
    deleteAll(Collections.singletonList(object));
  }
  
  protected void doDelete(T entity) {
    fDb.delete(entity);
  }
  
  public void deleteAll(Collection<T> objects) {
    fWriteLock.lock();
    try {
      for (T object : objects) {
        E event = createDeleteEventTemplate(object);
        DBHelper.putEventTemplate(event);
        doDelete(object);
      }
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }
  
  public long countAll() {
    try {
      ObjectSet<T> entities = fDb.query(fEntityClass);
      return entities.size();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }
  
  protected void fireAddEvents(final Set<E> events) {
    for (final L listener : entityListeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }
  
  protected void fireUpdateEvents(final Set<E> events) {
    for (final L listener : entityListeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }
  
  protected void fireDeleteEvents(final Set<E> events) {
    for (final L listener : entityListeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }
  
  public void addLifeCycleListener(L listener) {
    entityListeners.add(listener);
  }
  
  public void removeLifeCycleListener(L listener) {
    entityListeners.remove(listener);
  }
}
