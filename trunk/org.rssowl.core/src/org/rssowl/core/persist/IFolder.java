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

package org.rssowl.core.persist;

import java.net.URI;
import java.util.List;

/**
 * Folders store a number of Marks in an hierachical order. The hierachical
 * order is achieved by allowing to store Folders inside Folders.
 * <p>
 * In case a Blogroll URL is set for the Folder, it is to be interpreted as
 * root-folder of a "Synchronized Blogroll". This special kind of Folder allows
 * to synchronize its contents from a remote OPML file that contains a number of
 * Feeds.
 * </p>
 *
 * @author bpasero
 */
public interface IFolder extends IFolderChild, Reparentable<IFolder> {

  /** One of the fields in this type described as constant */
  public static final int NAME = 0;

  /** One of the fields in this type described as constant */
  public static final int BLOGROLL_LINK = 1;

  /** One of the fields in this type described as constant */
  public static final int MARKS = 2;

  /** One of the fields in this type described as constant */
  public static final int FOLDERS = 3;

  /**
   * Get a list of the children contained in this folder. Typically, these
   * children will be of type IMark or IFolder.
   *
   * @return a list of children contained in this folder.
   * <p>
   * Note: The returned List should not be modified. The default implementation
   * returns an unmodifiable List using
   * <code>Collections.unmodifiableList()</code>. Trying to modify the List
   * will result in <code>UnsupportedOperationException</code>.
   * </p>
   * @see #getMarks()
   * @see #getFolders()
   */
  List<IFolderChild> getChildren();

  /**
   * Adds an instance of <code>IMark</code> as Child to this Folder.
   *
   * @param mark An instance of <code>IMark</code> to be added as Child to
   * this Folder.
   */
  void addMark(IMark mark);

  /**
   * Moves a List of <code>IFolderChild</code> contained in this Folder to a
   * new position.
   *
   * @param children The List of <code>IFolderChild</code> being moved to a
   * new position.
   * @param position The new Position identified by a <code>IFolder</code>
   * contained in this folder.
   * @param after If <code>true</code>, move the folders to a one index after
   * the given position.
   */
  void reorderChildren(List<? extends IFolderChild> children, IFolderChild position, boolean after);

  /**
   * Removes an instance of <code>IMark</code> from this Folder.
   *
   * @param mark An instance of <code>IMark</code> to be removed from this
   * Folder.
   */
  void removeMark(IMark mark);

  /**
   * Get a list of marks contained in this folder. Typically, these marks may be
   * of type ISearchMark and/or IBookMark.
   *
   * @return a list of marks contained in this folder. Typically, these marks
   * may be of type ISearchMark and/or IBookMark.
   * <p>
   * Note: The returned List should not be modified. The default Implementation
   * returns an unmodifiable List using
   * <code>Collections.unmodifiableList()</code>. Trying to modify the List
   * will result in <code>UnsupportedOperationException</code>.
   * </p>
   */
  List<IMark> getMarks();

  /**
   * Adds an instance of <code>IFolder</code> as Child to this Folder.
   *
   * @param folder An instance of <code>IFolder</code> to be added to this
   * Folder.
   */
  void addFolder(IFolder folder);

  /**
   * Get a list of the sub-folders contained in this folder.
   *
   * @return a list of sub-folders of this folder.
   * <p>
   * Note: The returned List should not be modified. The default Implementation
   * returns an unmodifiable List using
   * <code>Collections.unmodifiableList()</code>. Trying to modify the List
   * will result in <code>UnsupportedOperationException</code>.
   * </p>
   */
  List<IFolder> getFolders();

  /**
   * Get the parent folder or null if no parent folder exists.
   *
   * @return the parent folder or null if no parent folder exists.
   */
  IFolder getParent();

  /**
   * Get the Name of this Folder.
   *
   * @return the name of the folder.
   */
  String getName();

  /**
   * Set the Name of this Folder.
   *
   * @param name the name of the folder to set.
   */
  void setName(String name);

  /**
   * Get the Link to the Blogroll this Folder is pointing to.
   *
   * @return Returns the Link to the Blogroll this Folder is pointing to.
   */
  URI getBlogrollLink();

  /**
   * Set the Link to the Blogroll this Folder is pointing to.
   *
   * @param blogrollLink the Link to the Blogroll this Folder is pointing to.
   */
  void setBlogrollLink(URI blogrollLink);

  /**
   * Removes an instance of <code>IFolder</code> that is equal to
   * <code>folder</code> from the list of child folders.
   *
   * @param folder An instance of <code>IFolder</code> to be removed.
   */
  void removeFolder(IFolder folder);
}