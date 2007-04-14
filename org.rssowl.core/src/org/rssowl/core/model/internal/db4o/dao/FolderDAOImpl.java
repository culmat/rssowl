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
package org.rssowl.core.model.internal.db4o.dao;

import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.internal.persist.Folder;
import org.rssowl.core.model.persist.dao.IFolderDAO;

public final class FolderDAOImpl extends AbstractEntityDAO<Folder, FolderListener,
    FolderEvent> implements IFolderDAO<Folder>   {

  public FolderDAOImpl() {
    super(Folder.class);
  }

  @Override
  protected final FolderEvent createDeleteEventTemplate(Folder entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final FolderEvent createSaveEventTemplate(Folder entity) {
    return new FolderEvent(entity, null, true);
  }

  @Override
  protected final boolean isSaveFully() {
    return false;
  }

}
