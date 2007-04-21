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
package org.rssowl.core.model.persist.dao;

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.util.ReparentInfo;

import java.util.Collection;
import java.util.List;

public interface IFolderDAO extends IEntityDAO<IFolder, FolderListener, FolderEvent>   {

  /**
   * Loads all Folders from the persistance layer that do not have any parent
   * Folder (in other words root folders).
   *
   * @return A Collection containing all Folders that do not have any
   * parent Folder.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  Collection<IFolder> loadRoots();

  /**
   * <p>
   * If <code>foldersInfos</code> is not null, performs the reparenting of the
   * folders as described by them. The <code>FolderEvent</code>s issued will
   * have a filled <code>oldParent</code> property to indicate that
   * reparenting took place.
   * </p>
   * <p>
   * If <code>markInfos</code> is not null, performs the reparenting of the
   * marks as described by them. Depending on the type of the mark,
   * BookMarkEvents or SearchMarkEvents will be issed. They will contain a
   * non-null <code>oldParent</code> property to indicate that reparenting
   * took place.
   * </p>
   *
   * @param folderInfos <code>null</code> or list of ReparentInfo objects
   * describing the reparenting details for a list of folders.
   * @param markInfos <code>null</code> or list of ReparentInfo objects
   * describing the reparenting details for a list of marks.
   * @throws PersistenceException In case of an error while loading the Types.
   */
  void reparent(List<ReparentInfo<IFolder, IFolder>> folderInfos,
      List<ReparentInfo<IMark, IFolder>> markInfos) throws PersistenceException;

}
