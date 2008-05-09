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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.dao.IEntityDAO;
import org.rssowl.core.persist.event.EntityListener;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.util.LoggingSafeRunnable;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class XStreamEntityDAO<T extends IEntity,
  L extends EntityListener<E, T>, E extends ModelEvent>
  extends XStreamPersistableDAO<T> implements IEntityDAO<T, L, E> {

  /** The List of Listeners for this DAO */
  protected final List<L> fEntityListeners = new CopyOnWriteArrayList<L>();
  protected final ManualEventManager fEventManager;
  protected final IDGenerator fIdGenerator;

  /**
   * Creates an instance of this class.
   *
   * @param entityClass
   * @param eventManager
   * @param idGenerator
   */
  public XStreamEntityDAO(Class<? extends T> entityClass, File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    super(entityClass, baseDir, xStream);
    Assert.isNotNull(idGenerator, "idGenerator");
    Assert.isNotNull(eventManager, "eventManager");
    fEventManager = eventManager;
    fIdGenerator = idGenerator;
  }

  public void addEntityListener(L listener) {
    fEntityListeners.add(listener);
  }

  public void removeEntityListener(L listener) {
    fEntityListeners.remove(listener);
  }

  public final void fireEvents(final Set<E> events, final EventType eventType) {
    Assert.isNotNull(eventType, "eventType");
    for (final L listener : fEntityListeners) {
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
}
