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
package org.rssowl.core.internal.persist.service;

import org.rssowl.core.persist.events.ModelEvent;
import org.rssowl.core.persist.events.runnable.EventRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventsMap {
  
  private static final EventsMap INSTANCE = new EventsMap();
  
  private static class InternalMap extends HashMap<Class<? extends ModelEvent>, EventRunnable<? extends ModelEvent>>{
    InternalMap(){
      super();
    }
  }
  
  private ThreadLocal<InternalMap> fEvents = new ThreadLocal<InternalMap>();

  private ThreadLocal<Map<Integer, ModelEvent>> fEventTemplatesMap = 
    new ThreadLocal<Map<Integer, ModelEvent>>();

  private EventsMap() {
    // Enforce singleton pattern
  }
  
  public final static EventsMap getInstance() {
    return INSTANCE;
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
  
  private EventRunnable< ? extends ModelEvent> getEventRunnable(ModelEvent event) {
    InternalMap map = fEvents.get();
    if (map == null) {
      map = new InternalMap();
      fEvents.set(map);
    }
    Class<? extends ModelEvent> eventClass = event.getClass();
    EventRunnable< ? extends ModelEvent> eventRunnable = map.get(eventClass);
    if (eventRunnable == null) {
      eventRunnable = event.createEventRunnable();
      map.put(eventClass, eventRunnable);
    }
    return eventRunnable;
  }
  
  public EventRunnable<? extends ModelEvent> removeEventRunnable(Class<? extends ModelEvent> klass) {
    InternalMap map = fEvents.get();
    if (map == null)
      return null;
    
    EventRunnable<? extends ModelEvent> runnable = map.remove(klass);
    return runnable;
  }
  
  public List<EventRunnable> removeEventRunnables()    {
    InternalMap map = fEvents.get();
    if (map == null)
      return new ArrayList<EventRunnable>(0);
    
    List<EventRunnable> eventRunnables = new ArrayList<EventRunnable>(map.size());
    for (Map.Entry<Class<? extends ModelEvent>, EventRunnable<? extends ModelEvent>> entry : map.entrySet()) {
      eventRunnables.add(entry.getValue());
    }
    map.clear();
    return eventRunnables;
  }
  
  public void putEventTemplate(int id, ModelEvent event) {
    Map<Integer, ModelEvent> map = fEventTemplatesMap.get();
    if (map == null) {
      map = new HashMap<Integer, ModelEvent>();
      fEventTemplatesMap.set(map);
    }
    map.put(id, event);
  }

  public final Map<Integer, ModelEvent> getEventTemplatesMap() {
    Map<Integer, ModelEvent> map = fEventTemplatesMap.get();
    if (map == null)
      return Collections.emptyMap();
    
    return Collections.unmodifiableMap(fEventTemplatesMap.get());
  }

  public Map<Integer, ModelEvent> removeEventTemplatesMap() {
    Map<Integer, ModelEvent> map = fEventTemplatesMap.get();
    fEventTemplatesMap.remove();
    return map;
  }
}
