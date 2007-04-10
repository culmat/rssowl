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
package org.rssowl.core.model.events.runnable;

import org.rssowl.core.Owl;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.ModelEvent;

import java.util.Set;

/**
 * Provides a way to fire a SourceEvent in the future.
 * 
 * @see EventRunnable
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class FolderEventRunnable extends EventRunnable<FolderEvent> {

  /**
   * Creates a new instance of this object.
   */
  public FolderEventRunnable() {
    super();
  }

  @Override
  public Class< ? extends ModelEvent> getEventClass() {
    return FolderEvent.class;
  }
  
  @Override
  protected final void firePersistEvents(Set<FolderEvent> persistEvents) {
    Owl.getListenerService().notifyFolderAdded(persistEvents);
  }

  @Override
  protected final void fireRemoveEvents(Set<FolderEvent> removeEvents) {
    Owl.getListenerService().notifyFolderDeleted(removeEvents);
  }

  @Override
  protected final void fireUpdateEvents(Set<FolderEvent> updateEvents) {
    Owl.getListenerService().notifyFolderUpdated(updateEvents);
  }
}
