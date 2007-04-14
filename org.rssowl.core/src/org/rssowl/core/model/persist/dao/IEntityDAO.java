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

import org.rssowl.core.model.events.EntityListener;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.events.runnable.EventType;
import org.rssowl.core.model.persist.IEntity;

import java.util.Set;

/**
 * Base interface for all IEntity DAOs.
 * @param <T> 
 * @param <L> 
 * @param <E> 
 */
public interface IEntityDAO<T extends IEntity, L extends EntityListener<E>,
    E extends ModelEvent> extends IPersistableDAO<T> {

  public void addEntityListener(L listener);
  
  public void removeEntityListener(L listener);
  
  public void fireEvents(Set<E> events, EventType eventType);
}
