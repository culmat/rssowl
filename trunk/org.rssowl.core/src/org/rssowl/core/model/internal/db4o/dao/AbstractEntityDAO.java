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
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.dao.IEntityDAO;
import org.rssowl.core.persist.events.EntityListener;
import org.rssowl.core.persist.events.ModelEvent;
import org.rssowl.core.persist.events.runnable.EventType;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A db4o abstract implementation of EntityDAO.
 * 
 * @param <T> Type of concrete implementation of IEntity.
 * @param <L> Type of EntityListener.
 * @param <E> Type of ModelEvent.
 */
public abstract class AbstractEntityDAO<T extends IEntity,
    L extends EntityListener<E, T>, E extends ModelEvent>
    extends AbstractPersistableDAO<T> implements IEntityDAO<T, L, E>{

  private final List<L> entityListeners = new CopyOnWriteArrayList<L>();
  
  /**
   * Creates an instance of this class.
   * 
   * @param entityClass
   * @param saveFully
   */
  public AbstractEntityDAO(Class<? extends T> entityClass, boolean saveFully) {
    super(entityClass, saveFully);
  }
  
  protected abstract E createSaveEventTemplate(T entity);

  protected abstract E createDeleteEventTemplate(T entity);
  
  @Override
  protected void preSave(T entity) {
    E event = createSaveEventTemplate(entity);
    DBHelper.putEventTemplate(event);
  }
  
  @Override
  protected void preDelete(T entity) {
    E event = createDeleteEventTemplate(entity);
    DBHelper.putEventTemplate(event);
  }
  
  public final void fireEvents(final Set<E> events, final EventType eventType) {
    Assert.isNotNull(eventType, "eventType");
    for (final L listener : entityListeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          switch (eventType) {
            case PERSIST:
              listener.entitiesAdded(events);
              break;
            case UPDATE:
              listener.entitiesUpdated(events);
              break;
            case REMOVE:
              listener.entitiesDeleted(events);
              break;
            default:
              throw new IllegalArgumentException("eventType unknown: " + eventType);
          }
        }
      });
    }
  }
  
  public void addEntityListener(L listener) {
    entityListeners.add(listener);
  }
  
  public void removeEntityListener(L listener) {
    entityListeners.remove(listener);
  }
}
