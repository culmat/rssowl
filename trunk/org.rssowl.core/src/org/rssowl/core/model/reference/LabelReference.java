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

package org.rssowl.core.model.reference;

import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.types.ILabel;

/**
 * Implementation of the <code>ModelReference</code> for the Type
 * <code>ILabel</code>.
 * 
 * @author bpasero
 */
public final class LabelReference extends ModelReference {

  /**
   * Instantiates a new leightweight reference. Any resolve()-call will be
   * passed to the <code>IModelDAO</code> to load the heavyweight type from
   * the persistance layer.
   * 
   * @param id The ID of the type to use for loading the type from the
   * persistance layer.
   */
  public LabelReference(long id) {
    super(id);
  }

  @Override
  public ILabel resolve() throws PersistenceException {
    return NewsModel.getDefault().getPersistenceLayer().getModelDAO().loadLabel(getId());
  }
}