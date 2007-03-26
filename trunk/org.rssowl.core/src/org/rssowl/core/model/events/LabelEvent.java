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

package org.rssowl.core.model.events;

import org.rssowl.core.model.events.runnable.LabelEventRunnable;
import org.rssowl.core.model.types.ILabel;

/**
 * <p>
 * An Event-Object being used to notify Listeners, whenever the type
 * <code>ILabel</code> was added, updated or deleted in the persistance layer.
 * </p>
 * In order to retrieve the Model-Object that is affected on the Event, use the
 * <code>resolve()</code> Method of the <code>ModelReference</code> stored
 * in this Event.
 * 
 * @author bpasero
 */
public final class LabelEvent extends ModelEvent {

  /**
   * Stores an instance of <code>ModelReference</code> for the affected Type
   * in this Event.
   * 
   * @param label An instance of <code>ModelReference</code> for the
   * affected Type.
   * @param isRoot <code>TRUE</code> if this Event is a Root-Event,
   * <code>FALSE</code> otherwise.
   */
  public LabelEvent(ILabel label, boolean isRoot) {
    super(label, isRoot);
  }

  /*
   * @see org.rssowl.core.model.events.ModelEvent#getReference()
   */
  @Override
  public ILabel getEntity() {
    return (ILabel) super.getEntity();
  }

  @Override
  public LabelEventRunnable createEventRunnable() {
    return new LabelEventRunnable();
  }
}