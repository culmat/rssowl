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

package org.rssowl.core.internal;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.IListenerService;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.pref.PreferencesEvent;
import org.rssowl.core.persist.pref.PreferencesListener;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.util.Set;

/**
 * TODO Remove me
 */
public class ListenerServiceImpl implements IListenerService {

  //TODO Need a better solution here
  private static final boolean DEBUG = false;

  /* Listener Lists for listening on Model Events */
  private ListenerList fPreferencesListeners = new ListenerList();

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferenceAdded(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferenceAdded(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceAdded(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferencesDeleted(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferencesDeleted(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceDeleted(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferencesUpdated(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferencesUpdated(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceUpdated(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addPreferencesListener(org.rssowl.core.model.persist.pref.PreferencesListener)
   */
  public void addPreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removePreferencesListener(org.rssowl.core.model.persist.pref.PreferencesListener)
   */
  public void removePreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.remove(listener);
  }

  @SuppressWarnings("nls")
  private void logEvents(Set< ? extends ModelEvent> events, EventType eventType) {
    if (DEBUG) {
      String eventTypeString = null;
      switch (eventType) {
        case PERSIST:
          eventTypeString = " Added ("; //$NON-NLS-1$
          break;
        case UPDATE:
          eventTypeString = " Updated ("; //$NON-NLS-1$
          break;
        case REMOVE:
          eventTypeString = " Removed ("; //$NON-NLS-1$
          break;
      }
      IPersistable type = null;
      ModelEvent event = events.iterator().next();
      if (eventType != EventType.REMOVE)
        type = event.getEntity();

      String typeName = type == null ? "" : type.getClass().getSimpleName();
      String typeString = type == null ? "" : type.toString();

      if (events.size() > 0 && typeName == "")
        typeName = events.iterator().next().getClass().getSimpleName();

      System.out.println(typeName + eventTypeString + typeString + ", events = " + events.size() + ", isRoot = " + event.isRoot() + ")");
    }
  }
}