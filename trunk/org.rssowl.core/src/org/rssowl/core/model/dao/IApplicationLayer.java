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

package org.rssowl.core.model.dao;

import org.rssowl.core.model.ReparentInfo;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * The <code>IApplicationLayer</code> interface is providing methods that
 * access the Persistance Layer as required by the Application. It is very
 * important that implementors optimize the performance of all methods.
 * <p>
 * TODO There is some methods in here which are no longer used or only
 * referenced from Test-Cases. Before releasing, make sure to remove them!
 * </p>
 *
 * @author bpasero
 */
public interface IApplicationLayer {

  /**
   * Loads and returns all the feeds available in the persistence layer.
   *
   * @return a list of all feeds in the persistence layer.
   * @throws PersistenceException In case of an error while loading the Types.
   */
  List<IFeed> loadAllFeeds();

  /**
   * Loads and returns all the bookmarks available in the system.
   * <p>
   * TODO No longer being used!
   * </p>
   *
   * @param activateFully if <code>true</code>, the marks are fully
   * activated. Otherwise, the default is used.
   * @return a list of all bookmarks in the system.
   * @throws PersistenceException In case of an error while loading the Types.
   */
  List<IBookMark> loadAllBookMarks(boolean activateFully) throws PersistenceException;

  /**
   * Get all bookmarks that reference the given Feed.
   *
   * @param feedRef A reference to the Feed of interest.
   * @return Returns a List of <code>BookMarkReference</code> that point to
   * the given Feed.
   * @throws PersistenceException In case of an error while loading the Types.
   */
  List<IBookMark> loadBookMarks(FeedLinkReference feedRef) throws PersistenceException;

  /**
   * Loads all Folders from the persistance layer, that do not have any parent
   * Folder, making them the root-leveld Folders.
   *
   * @return A List containing references to all Folders that do not have any
   * parent Folder or <code>NULL</code> in case no root-leveld Folders are
   * available.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  List<IFolder> loadRootFolders() throws PersistenceException;

  /**
   * Deletes all folder items in <code>folders</code> and their respective
   * children from the persistence layer.
   *
   * @param folders list containing items to be deleted.
   * @throws PersistenceException In case of an error while loading the Types.
   */
  void deleteFolders(List<IFolder> folders) throws PersistenceException;

  /**
   * Loads all Labels from the persistence layer.
   *
   * @return A List containing all Labels.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  List<ILabel> loadLabels() throws PersistenceException;

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
  void reparent(List<ReparentInfo<IFolder, IFolder>> folderInfos, List<ReparentInfo<IMark, IFolder>> markInfos) throws PersistenceException;

  /**
   * Returns a <code>FeedReference</code> that points to a IFeed with the
   * given <code>link</code> in the persistance layer.
   *
   * @param link The link of the feed whose reference should be returned.
   * @return A FeedReference pointing to a IFeed with the given link or
   * <code>NULL</code> in case there is no IFeed with the given link.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  FeedReference loadFeedReference(URI link) throws PersistenceException;

  /**
   * Loads a <code>IFeed</code> with the given <code>link</code> from the
   * persistance layer.
   *
   * @param link The link of the IFeed to load from the persistance layer.
   * @return The IFeed with the given link loaded from the persistance layer or
   * <code>NULL</code> in case there is no IFeed available for the given link.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IFeed loadFeed(URI link) throws PersistenceException;

  /**
   * Sets the state of all the news items contained in <code>news</code> to
   * <code>state</code>. In addition, if <code>affectEquivalentNews</code>
   * is <code>true</code>, the state of equivalent news in other feeds will
   * also be changed to <code>state</code>. Note that news items whose state
   * is equal to <code>state</code> will not be changed or updated in the
   * persistence layer.
   *
   * @param news A List of <code>INews</code> whose state should be changed.
   * @param state The state to set the news items to.
   * @param afffectEquivalentNews If set to <code>TRUE</code> the state of
   * equivalent news in other feeds will also be changed to <code>state</code>
   * @param force If set to <code>TRUE</code>, the method will update even
   * those News that match the given state.
   * @throws PersistenceException
   */
  void setNewsState(List<INews> news, INews.State state, boolean afffectEquivalentNews, boolean force) throws PersistenceException;

  /**
   * Saves all the news items in <code>newsList</code>. This method should be
   * used instead of {@link IModelDAO#saveNews(INews)} when the caller has a set
   * of INews that should be saved as part of the same transaction and whose
   * events should be batched together.
   *
   * @param newsList List containing <code>INews</code> to be saved. Should
   * not be <code>null</code>.
   * @return A List containing the saved INews in no particular order.
   * @throws PersistenceException if there is a problem persisting the item.
   * @throws IllegalArgumentException if <code>newsList</code> is
   * <code>null</code>.
   * @see IModelDAO#saveNews(INews)
   */
  List<INews> saveNews(List<INews> newsList) throws PersistenceException;

  /**
   * Handles all the persistence-related operations for a feed that has been
   * provided by the interpreter. This includes:
   * <li>Merging the contents of the interpreted feed with the currently saved
   * one.</li>
   * <li>Running the retention policy.</li>
   * <li>Updating the ConditionalGet object associated with the feed.</li>
   *
   * @param bookMark The BookMark that contains the feed that has been reloaded.
   * @param interpretedFeed The IFeed object that has been supplied by the
   * interpreter based on its online contents.
   * @param conditionalGet IConditionalObject associated with the IFeed or
   * <code>null</code> if there isn't one.
   * @param deleteConditionalGet if <code>true</code> an existing
   * IConditionalGet object associated with the IFeed will be deleted as part of
   * this operation.
   */
  void handleFeedReload(IBookMark bookMark, IFeed interpretedFeed, IConditionalGet conditionalGet, boolean deleteConditionalGet);

  /**
   * Loads a sorted <code>Set</code> of Strings containing all categories that
   * are persisted in the persistence layer.
   *
   * @return a sorted <code>Set</code> of Strings containing all categories
   * that are persisted in the persistence layer.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  Set<String> loadCategories();

  /**
   * Loads a sorted <code>Set</code> of Strings containing all authors that
   * are persisted in the persistence layer.
   *
   * @return a sorted <code>Set</code> of Strings containing all authors that
   * are persisted in the persistence layer.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  Set<String> loadAuthors();
}