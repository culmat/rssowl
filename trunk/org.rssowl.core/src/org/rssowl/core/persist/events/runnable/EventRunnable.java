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
package org.rssowl.core.persist.events.runnable;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.dao.DAOService;
import org.rssowl.core.persist.dao.IEntityDAO;
import org.rssowl.core.persist.events.ModelEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for runnables that fire one event. These runnables are useful
 * when one wants to specify an event to be called in the future. Another
 * possible use is to fire the event in an event loop.
 * 
 * @param <T> A subclass of ModelReference that establishes the type of the
 * first parameter accepted by the constructor.
 * 
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public abstract class EventRunnable<T extends ModelEvent> implements Runnable   {
  
  private Set<T> fPersistEvents;
  
  private Set<T> fRemoveEvents;
  
  private Set<T> fUpdateEvents;
  
  private final Class<T> fEventClass;
  private final IEntityDAO<?, ?, T> fEntityDAO;

  /**
   * Creates an instance of this class.
   * @param eventClass 
   */
  protected EventRunnable(Class<T> eventClass, IEntityDAO<?, ?, T> entityDAO) {
    Assert.isNotNull(eventClass, "eventClass");
    Assert.isNotNull(entityDAO, "entityDAO");
    fEventClass = eventClass;
    fEntityDAO = entityDAO;
  }
  
  protected static final DAOService getDAOService() {
    return Owl.getPersistenceService().getDAOService();
  }

  /**
   * Fires the event type defined by the eventType property appropriate
   * to T.
   */
  public final void run() {
    if (shouldFirePersistEvents())
      fireEvents(fPersistEvents, EventType.PERSIST);

    if (shouldFireRemoveEvents())
      fireEvents(fRemoveEvents, EventType.REMOVE);
    
    if (shouldFireUpdateEvents())
      fireEvents(fUpdateEvents, EventType.UPDATE);
  }
  
  private void fireEvents(Set<T> persistEvents, EventType eventType) {
    fEntityDAO.fireEvents(Collections.unmodifiableSet(persistEvents), eventType);
  }
  
  @SuppressWarnings("unchecked")
  public final void addCheckedPersistEvent(ModelEvent event) {
    checkEventType(getEventClass(), event);
    addPersistEvent((T) event);
  }
  
  private Class<? extends ModelEvent> getEventClass()   {
    return fEventClass;
  }
  
  private void checkEventType(Class<?> expectedClass, ModelEvent eventReceived)    {
    if (!expectedClass.isInstance(eventReceived))
      throw new IllegalArgumentException("event must be of type: " +  //$NON-NLS-1$
          expectedClass + ", but it is of type: " + eventReceived.getClass()); //$NON-NLS-1$
  }

  @SuppressWarnings("unchecked")
  public final void addCheckedRemoveEvent(ModelEvent event) {
    checkEventType(getEventClass(), event);
    addRemoveEvent((T) event);
  }
  
  @SuppressWarnings("unchecked")
  public final void addCheckedUpdateEvent(ModelEvent event) {
    checkEventType(getEventClass(), event);
    addUpdateEvent((T) event);
  }
  
  public final void addPersistEvent(T event) {
    if (fPersistEvents == null)
      fPersistEvents = new HashSet<T>(3);
    
    if (removeEventsContains(event))
      return;
    
    fPersistEvents.add(event);
  }
  
  private boolean removeEventsContains(ModelEvent event) {
    return fRemoveEvents != null && fRemoveEvents.contains(event);
  }
  
  private boolean persistEventsContains(ModelEvent event) {
    return fPersistEvents != null && fPersistEvents.contains(event);
  }
  
  public final void addRemoveEvent(T event) {
    if (fRemoveEvents == null)
      fRemoveEvents = new HashSet<T>(3);
    
    if (fUpdateEvents != null)
      fUpdateEvents.remove(event);
    if (fPersistEvents != null)
      fPersistEvents.remove(event);
    
    fRemoveEvents.add(event);
  }
  
  public final void addUpdateEvent(T event) {
    if (fUpdateEvents == null)
      fUpdateEvents = new HashSet<T>(3);

    if (removeEventsContains(event) || persistEventsContains(event))
      return;
    
    fUpdateEvents.add(event);
  }
  
  public final List<T> getAllEvents() {
    List<T> allEvents = new ArrayList<T>(fPersistEvents.size() + 
        fRemoveEvents.size() + fUpdateEvents.size());
    allEvents.addAll(fPersistEvents);
    allEvents.addAll(fRemoveEvents);
    allEvents.addAll(fUpdateEvents);
    return allEvents;
  }
  
  private boolean shouldFirePersistEvents() {
    return (fPersistEvents != null) && (fPersistEvents.size() > 0);
  }
  
  private boolean shouldFireUpdateEvents() {
    return (fUpdateEvents != null) && (fUpdateEvents.size() > 0);
  }
  
  private boolean shouldFireRemoveEvents() {
    return (fRemoveEvents != null) && (fRemoveEvents.size() > 0);
  }

  public final Set<T> getPersistEvents() {
    if (fPersistEvents == null)
      return Collections.emptySet();
    
    return Collections.unmodifiableSet(fPersistEvents);
  }

  public final Set<T> getRemoveEvents() {
    if (fRemoveEvents == null)
      return Collections.emptySet();
    
    return Collections.unmodifiableSet(fRemoveEvents);
  }

  public final Set<T> getUpdateEvents() {
    if (fUpdateEvents == null)
      return Collections.emptySet();
      
    return Collections.unmodifiableSet(fUpdateEvents);
  }
}
