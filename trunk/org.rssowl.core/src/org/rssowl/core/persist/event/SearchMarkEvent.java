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

package org.rssowl.core.persist.event;

import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.event.runnable.SearchMarkEventRunnable;

/**
 * <p>
 * An Event-Object being used to notify Listeners, whenever the type
 * <code>ISearchMark</code> was added, updated or deleted in the persistance
 * layer.
 * </p>
 * In order to retrieve the Model-Object that is affected on the Event, use the
 * <code>resolve()</code> Method of the <code>ModelReference</code> stored
 * in this Event.
 *
 * @author bpasero
 */
public final class SearchMarkEvent extends ModelEvent {

  /* In case of Reparenting, remember the old parent */
  private final IFolder fOldParent;

  /* Only for resultsChanged(): TRUE if *new* news where added */
  private boolean fAddedNewNews;

  /**
   * Stores an instance of <code>ModelReference</code> and the Parent
   * Reference for the affected Type in this Event.
   *
   * @param mark An instance of <code>ModelReference</code> for the affected
   * Type.
   * @param oldParent If this Event informs about a Reparenting the old parent
   * is used to do updates in the UI, <code>NULL</code> otherwise.
   * @param isRoot <code>TRUE</code> if this Event is a Root-Event,
   * <code>FALSE</code> otherwise.
   */
  public SearchMarkEvent(ISearchMark mark, IFolder oldParent, boolean isRoot) {
    this(mark, oldParent, isRoot, false);
  }

  /**
   * Stores an instance of <code>ModelReference</code> and the Parent
   * Reference for the affected Type in this Event.
   *
   * @param mark An instance of <code>ModelReference</code> for the affected
   * Type.
   * @param oldParent If this Event informs about a Reparenting the old parent
   * is used to do updates in the UI, <code>NULL</code> otherwise.
   * @param isRoot <code>TRUE</code> if this Event is a Root-Event,
   * <code>FALSE</code> otherwise.
   * @param addedNewNews TRUE if *new* news where added.
   */
  public SearchMarkEvent(ISearchMark mark, IFolder oldParent, boolean isRoot, boolean addedNewNews) {
    super(mark, isRoot);
    fOldParent = oldParent;
    fAddedNewNews = addedNewNews;
  }

  /*
   * @see org.rssowl.core.model.events.ModelEvent#getReference()
   */
  @Override
  public ISearchMark getEntity() {
    return (ISearchMark) super.getEntity();
  }

  /**
   * @return TRUE if *new* news where added from a resultsChanged() event.
   */
  public boolean isAddedNewNews() {
    return fAddedNewNews;
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
  public SearchMarkEventRunnable createEventRunnable() {
    return new SearchMarkEventRunnable();
  }
}