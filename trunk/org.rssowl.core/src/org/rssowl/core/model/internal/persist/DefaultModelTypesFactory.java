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

package org.rssowl.core.model.internal.persist;

import org.rssowl.core.model.internal.persist.search.SearchCondition;
import org.rssowl.core.model.internal.persist.search.SearchField;
import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.ICloud;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IGuid;
import org.rssowl.core.model.persist.IImage;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.ISource;
import org.rssowl.core.model.persist.ITextInput;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.persist.search.ISearchField;
import org.rssowl.core.model.persist.search.SearchSpecifier;
import org.rssowl.core.model.reference.FeedLinkReference;

import java.net.URI;
import java.util.Date;

/**
 * Default implementation of IModelTypesFactory. It instantiates the concrete
 * classes provided in the {@link org.rssowl.core.model.internal.persist} package.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class DefaultModelTypesFactory implements IModelTypesFactory {

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createNews(java.lang.Long,
   * org.rssowl.core.model.types.IFeed, java.util.Date)
   */
  public INews createNews(Long id, IFeed feed, Date receiveDate) {
    News news = new News(id, feed, receiveDate);

    /* Automatically add to the Feed */
    feed.addNews(news);
    return news;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createPerson(long,
   * org.rssowl.core.model.reference.ModelReference)
   */
  public IPerson createPerson(Long id, IPersistable parentRef) {
    Person person = new Person(id);

    /* Automatically add to the Feed or News */
    if (parentRef instanceof IFeed)
      ((IFeed) parentRef).setAuthor(person);
    else if (parentRef instanceof INews)
      ((INews) parentRef).setAuthor(person);

    return person;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createImage(org.rssowl.core.model.types.IFeed)
   */
  public IImage createImage(IFeed feed) {
    Image image = new Image();

    /* Automatically add to the Feed */
    feed.setImage(image);

    return image;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createAttachment(long,
   * java.net.URI, org.rssowl.core.model.reference.NewsReference)
   */
  public IAttachment createAttachment(Long id, INews news) {
    Attachment attachment = new Attachment(id, news);
    news.addAttachment(attachment);

    return attachment;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createCategory(java.lang.Long,
   * org.rssowl.core.model.types.IEntity)
   */
  public ICategory createCategory(Long id, IEntity parent) {
    Category category = new Category(id);

    /* Automatically add to the Feed or News */
    if (parent instanceof IFeed)
      ((IFeed) parent).addCategory(category);
    else if (parent instanceof INews)
      ((INews) parent).addCategory(category);

    return category;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createSource(org.rssowl.core.model.types.INews)
   */
  public ISource createSource(final INews news) {
    Source source = new Source();

    /* Automatically set to the News */
    news.setSource(source);

    return source;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createGuid(org.rssowl.core.model.types.INews,
   * java.lang.String)
   */
  public IGuid createGuid(final INews news, String value) {
    Guid guid = new Guid(value);

    /* Automatically set to the News */
    news.setGuid(guid);

    return guid;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createCloud(org.rssowl.core.model.types.IFeed)
   */
  public ICloud createCloud(IFeed feed) {
    CloudAdapter cloud = new CloudAdapter();

    /* Automatically set to the Feed */
    feed.setCloud(cloud);

    return cloud;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createTextInput(org.rssowl.core.model.types.IFeed)
   */
  public ITextInput createTextInput(IFeed feed) {
    TextInputAdapter textInput = new TextInputAdapter();

    /* Automatically set to the Feed */
    feed.setTextInput(textInput);

    return textInput;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createFeed(java.lang.Long,
   * java.net.URI)
   */
  public IFeed createFeed(Long id, URI link) {
    return new Feed(id, link);
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createFolder(long,
   * java.lang.String, org.rssowl.core.model.reference.FolderReference)
   */
  public IFolder createFolder(Long id, IFolder parent, String name) {
    Folder folder = new Folder(id, parent, name);

    /* Automatically add to the Folder */
    if (parent != null)
      parent.addFolder(folder);

    return folder;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createLabel(long,
   * java.lang.String)
   */
  public ILabel createLabel(Long id, String name) {
    return new Label(id, name);
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createSearchMark(long,
   * java.lang.String, org.rssowl.core.model.reference.FolderReference)
   */
  public ISearchMark createSearchMark(Long id, IFolder folder, String name) {
    SearchMark searchMark = new SearchMark(id, folder, name);

    /* Automatically add to the Folder */
    folder.addMark(searchMark);

    return searchMark;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createBookMark(java.lang.Long,
   * org.rssowl.core.model.types.IFolder,
   * org.rssowl.core.model.reference.FeedLinkReference, java.lang.String)
   */
  public IBookMark createBookMark(Long id, IFolder folder, FeedLinkReference feedRef, String name) {
    BookMark bookMark = new BookMark(id, folder, feedRef, name);

    /* Automatically add to the Folder */
    folder.addMark(bookMark);

    return bookMark;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createSearchCondition(java.lang.Long,
   * org.rssowl.core.model.types.ISearchMark,
   * org.rssowl.core.model.search.ISearchField,
   * org.rssowl.core.model.search.SearchSpecifier, java.lang.Object)
   */
  public ISearchCondition createSearchCondition(Long id, ISearchMark searchMark, ISearchField field, SearchSpecifier specifier, Object value) {
    SearchCondition condition = new SearchCondition(id, field, specifier, value);

    /* Automatically add to the SearchMark */
    searchMark.addSearchCondition(condition);
    return condition;
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createSearchCondition(org.rssowl.core.model.search.ISearchField,
   * org.rssowl.core.model.search.SearchSpecifier, java.lang.Object)
   */
  public ISearchCondition createSearchCondition(ISearchField field, SearchSpecifier specifier, Object value) {
    return new SearchCondition(field, specifier, value);
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createSearchField(int,
   * java.lang.String)
   */
  public ISearchField createSearchField(int id, String entityName) {
    return new SearchField(id, entityName);
  }

  /*
   * @see org.rssowl.core.model.types.IModelTypesFactory#createConditionalGet(java.lang.String,
   * java.net.URI, java.lang.String)
   */
  public IConditionalGet createConditionalGet(String ifModifiedSince, URI link, String ifNoneMatch) {
    return new ConditionalGet(ifModifiedSince, link, ifNoneMatch);
  }
}