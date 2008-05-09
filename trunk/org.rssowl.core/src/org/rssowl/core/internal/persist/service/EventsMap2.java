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
package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.runnable.EventRunnable;
import org.rssowl.core.persist.event.runnable.EventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventsMap2 {

  private static class InternalMap extends HashMap<Class<? extends ModelEvent>, EventRunnable<? extends ModelEvent>>{
    InternalMap(int capacity){
      super(capacity);
    }
  }

  private final InternalMap fEvents = new InternalMap(8);

  public EventsMap2() {
  }

  public final void putPersistEvent(ModelEvent event) {
    EventRunnable< ? extends ModelEvent> eventRunnable = getEventRunnable(event);
    eventRunnable.addCheckedPersistEvent(event);
  }

  public final void putUpdateEvent(ModelEvent event) {
    EventRunnable< ? extends ModelEvent> eventRunnable = getEventRunnable(event);
    eventRunnable.addCheckedUpdateEvent(event);
  }

  public final void putRemoveEvent(ModelEvent event) {
    EventRunnable< ? extends ModelEvent> eventRunnable = getEventRunnable(event);
    eventRunnable.addCheckedRemoveEvent(event);
  }

  public final boolean containsPersistEvent(Class<? extends ModelEvent> eventClass, IEntity entity) {
    EventRunnable<? extends ModelEvent> eventRunnable = getEventRunnable(eventClass);
    return eventRunnable.getPersistEvents().contains(entity);
  }

  public final boolean containsUpdateEvent(Class<? extends ModelEvent> eventClass, IEntity entity) {
    EventRunnable<? extends ModelEvent> eventRunnable = getEventRunnable(eventClass);
    return eventRunnable.getUpdateEvents().contains(entity);
  }

  public final boolean containsRemoveEvent(Class<? extends ModelEvent> eventClass, IEntity entity) {
    EventRunnable<? extends ModelEvent> eventRunnable = getEventRunnable(eventClass);
    return eventRunnable.getRemoveEvents().contains(entity);
  }

  private EventRunnable<? extends ModelEvent> getEventRunnable(Class<? extends ModelEvent> eventClass)   {
    EventRunnable< ? extends ModelEvent> eventRunnable = fEvents.get(eventClass);
    return eventRunnable;
  }

  private EventRunnable< ? extends ModelEvent> getEventRunnable(ModelEvent event) {
    Class<? extends ModelEvent> eventClass = event.getClass();
    EventRunnable<? extends ModelEvent> eventRunnable = getEventRunnable(eventClass);
    if (eventRunnable == null) {
      eventRunnable = event.createEventRunnable();
      fEvents.put(eventClass, eventRunnable);
    }
    return eventRunnable;
  }

  public EventRunnable<? extends ModelEvent> removeEventRunnable(Class<? extends ModelEvent> klass) {
    EventRunnable<? extends ModelEvent> runnable = fEvents.remove(klass);
    return runnable;
  }


  public List<EventRunnable<?>> getEventRunnables() {
    List<EventRunnable<?>> eventRunnables = new ArrayList<EventRunnable<?>>(fEvents.size());
    for (Map.Entry<Class<? extends ModelEvent>, EventRunnable<? extends ModelEvent>> entry : fEvents.entrySet()) {
      eventRunnables.add(entry.getValue());
    }
    return eventRunnables;
  }

  public List<EventRunnable<?>> removeEventRunnables()    {
    List<EventRunnable<?>> eventRunnables = getEventRunnables();
    fEvents.clear();
    return eventRunnables;
  }

  public void putEvent(ModelEvent modelEvent, EventType eventType) {
    Assert.isNotNull(eventType, "eventType");
    switch (eventType) {
      case PERSIST: putPersistEvent(modelEvent);
      break;
      case UPDATE: putUpdateEvent(modelEvent);
      break;
      case REMOVE: putRemoveEvent(modelEvent);
      break;
      default: throw new IllegalArgumentException("Unknown eventType: " + eventType);
    }
  }
}
