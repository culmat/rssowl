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

package org.rssowl.core.persist.events;

import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.events.runnable.BookMarkEventRunnable;

/**
 * <p>
 * An Event-Object being used to notify Listeners, whenever the type
 * <code>IBookMark</code> was added, updated or deleted in the persistance
 * layer.
 * </p>
 * In order to retrieve the Model-Object that is affected on the Event, use the
 * <code>resolve()</code> Method of the <code>ModelReference</code> stored
 * in this Event.
 * 
 * @author bpasero
 */
public final class BookMarkEvent extends ModelEvent {

  /* In case of Reparenting, remember the old parent */
  private final IFolder fOldParent;

  /**
   * Stores an instance of <code>ModelReference</code> and the Parent
   * Reference for the affected Type in this Event.
   * 
   * @param mark An instance of <code>ModelReference</code> for the
   * affected Type.
   * @param oldParent If this Event informs about a Reparenting the old parent
   * is used to do updates in the UI, <code>NULL</code> otherwise.
   * @param isRoot <code>TRUE</code> if this Event is a Root-Event,
   * <code>FALSE</code> otherwise.
   */
  public BookMarkEvent(IBookMark mark, IFolder oldParent, boolean isRoot) {
    super(mark, isRoot);
    fOldParent = oldParent;
  }

  /*
   * @see org.rssowl.core.model.events.ModelEvent#getReference()
   */
  @Override
  public IBookMark getEntity() {
    return (IBookMark) super.getEntity();
  }

  /**
   * Get the previous Parent of this Type in case this Event informs about a
   * Reparenting.
   * <p>
   * Note that this Method <em>will</em> return <code>NULL</code> in any
   * case where the Event is not informing about reparenting!
   * </p>
   * 
   * @return Returns the previous Parent of this Type in case this Event informs
   * about Reparenting. Otherwise this Method will return <code>NULL</code>.
   */
  public IFolder getOldParent() {
    return fOldParent;
  }

  @Override
  public BookMarkEventRunnable createEventRunnable() {
    return new BookMarkEventRunnable();
  }
}