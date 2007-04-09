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

import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.NewsCounter;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.reference.AttachmentReference;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.CategoryReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.reference.LabelReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.model.reference.PersonReference;
import org.rssowl.core.model.reference.SearchConditionReference;
import org.rssowl.core.model.reference.SearchMarkReference;

import java.net.URI;

/**
 * The central interface for saving, deleting and retrieving types from the
 * persistance layer. The implementation is contributable via extension-point
 * mechanism.
 * 
 * @author bpasero
 */
public interface IModelDAO {

  /**
   * Saves the given Type.
   * 
   * @param bookmark The BookMark to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IBookMark saveBookMark(IBookMark bookmark) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The BookMark to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteBookMark(BookMarkReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param Attachment The Attachment to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IAttachment saveAttachment(IAttachment Attachment) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Attachment to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteAttachment(AttachmentReference reference) throws PersistenceException;

  /**
   * Saves the given Type using <code>IModelDAO.DEEP</code> as kind of
   * operation.
   * 
   * @param category The Category to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ICategory saveCategory(ICategory category) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Category to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteCategory(CategoryReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param feed The Feed to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IFeed saveFeed(IFeed feed) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Feed to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteFeed(FeedReference reference) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Feed to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteFeed(FeedLinkReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param folder The Folder to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IFolder saveFolder(IFolder folder) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Folder to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteFolder(FolderReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param label The Label to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ILabel saveLabel(ILabel label) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Label to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteLabel(LabelReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param Person The Person to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IPerson savePerson(IPerson Person) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The Person to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deletePerson(PersonReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param news The News to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  INews saveNews(INews news) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The News to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteNews(NewsReference reference) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param searchmark The SearchMark to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ISearchMark saveSearchMark(ISearchMark searchmark) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The SearchMark to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteSearchMark(SearchMarkReference reference) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ISearchMark loadSearchMark(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IPerson loadPerson(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  INews loadNews(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ILabel loadLabel(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IFolder loadFolder(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IFeed loadFeed(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ICategory loadCategory(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IBookMark loadBookMark(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IAttachment loadAttachment(long id) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param id The ID of the type to load from the persistance layer.
   * @return The Type with the given ID loaded from the persistance layer or
   * <code>NULL</code> in case there is no Type available for the given ID.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ISearchCondition loadSearchCondition(long id) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param condition The SearchCondition to update.
   * @return A Reference of the Type that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  ISearchCondition saveSearchCondition(ISearchCondition condition) throws PersistenceException;

  /**
   * Delete the given Type.
   * 
   * @param reference The SearchCondition to delete as reference.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteSearchCondition(SearchConditionReference reference) throws PersistenceException;

  /**
   * Loads the given type with the given ID from the persistance layer.
   * 
   * @param link The link of the IConditionalGet to load from the persistance
   * layer.
   * @return The Type with the given <code>link</code> loaded from the
   * persistance layer or <code>NULL</code> in case there is no Type available
   * for the given <code>link</code>.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   * @throws IllegalArgumentException in case <code>link</code> is
   * <code>null</code>.
   */
  IConditionalGet loadConditionalGet(URI link) throws PersistenceException;

  /**
   * Saves the given Type.
   * 
   * @param conditionalGet The {@link IConditionalGet} to update.
   * @return The object that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  IConditionalGet saveConditionalGet(IConditionalGet conditionalGet) throws PersistenceException;

  /**
   * Deletes the given Type.
   * 
   * @param conditionalGet The IConditionalGet to delete.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteConditionalGet(IConditionalGet conditionalGet) throws PersistenceException;
  
  /**
   * Loads the NewsCounter from the persistance layer or returns <code>null</code>
   * if no counter has been saved yet.
   * 
   * @return The NewsCounter from the persistance layer or returns <code>null</code>
   * if no counter has been saved yet.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   * @throws IllegalArgumentException in case <code>link</code> is
   * <code>null</code>.
   */
  NewsCounter loadNewsCounter() throws PersistenceException;

  /**
   * Saves the NewsCounter.
   * 
   * @param newsCounter The {@link NewsCounter} to update.
   * @return The object that was saved.
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  NewsCounter saveNewsCounter(NewsCounter newsCounter) throws PersistenceException;

  /**
   * Deletes the news counter.
   * 
   * @throws PersistenceException In case of an error while accessing the
   * persistance layer implementation.
   */
  void deleteNewsCounter() throws PersistenceException;
}