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

package org.rssowl.core.model.types;

import org.rssowl.core.model.dao.IDGenerator;

/**
 * Implementors of <code>IEntity</code> add a certain model-type to the
 * application. Any entity is <em>uniquely</em> identified by its ID.
 * Implementors have to make sure, that no entity of any kind will ever have the
 * same ID as another entity.
 * 
 * @see IDGenerator
 * @author bpasero
 */
public interface IEntity extends IExtendableType {

  /**
   * Can be used in a
   * <code>ISearchField<code> to represent a search over all fields of the given Type.
   */
  public static final int ALL_FIELDS = -1;

  /**
   * Get the unique id for this object. Implementors have to make sure, that no
   * entity of any kind will ever have the same ID as another entity.
   * 
   * @return Unique id for the object.
   */
  Long getId();

  /**
   * Sets the unique id for this object. Implementors have to make sure, that no
   * entity of any kind will ever have the same ID as another entity.
   * 
   * @param id Unique id for the object.
   */
  void setId(Long id);
}