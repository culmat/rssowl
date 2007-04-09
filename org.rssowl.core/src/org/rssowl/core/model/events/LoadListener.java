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

import org.rssowl.core.model.persist.IEntity;

/**
 * The listener interface for receiving entity loaded events. This is
 * mainly intended for DAO objects that need to perform some processing
 * after the entity is loaded but before it is used.
 * 
 * @author Ismael Juma (ismael@juma.me.uk)
 * @param <T> Type of the entity that this listener is interested in receiving
 * events for.
 */
public interface LoadListener<T extends IEntity> {
  /**
   * Invoked when an entity is loaded.
   * @param entity IEntity that was loaded.
   */
  void entityLoaded(T entity);
}
