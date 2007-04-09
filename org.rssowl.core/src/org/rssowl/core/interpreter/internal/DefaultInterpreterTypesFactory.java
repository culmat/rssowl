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

package org.rssowl.core.interpreter.internal;

import org.rssowl.core.interpreter.IInterpreterTypesFactory;
import org.rssowl.core.model.internal.types.Attachment;
import org.rssowl.core.model.internal.types.BookMark;
import org.rssowl.core.model.internal.types.Category;
import org.rssowl.core.model.internal.types.CloudAdapter;
import org.rssowl.core.model.internal.types.Feed;
import org.rssowl.core.model.internal.types.Folder;
import org.rssowl.core.model.internal.types.Guid;
import org.rssowl.core.model.internal.types.Image;
import org.rssowl.core.model.internal.types.News;
import org.rssowl.core.model.internal.types.Person;
import org.rssowl.core.model.internal.types.Source;
import org.rssowl.core.model.internal.types.TextInputAdapter;
import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.ICloud;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IGuid;
import org.rssowl.core.model.persist.IImage;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISource;
import org.rssowl.core.model.persist.ITextInput;
import org.rssowl.core.model.reference.FeedLinkReference;

import java.net.URI;

/**
 * Factory for the Interpreter Plugin to work with the applications own Model.
 * Clients should never call any Method out of here.
 * 
 * @author bpasero
 */
public class DefaultInterpreterTypesFactory implements IInterpreterTypesFactory {

  /*
   * @see org.rssowl.core.interpreter.IInterpreterTypesFactory#createFeed(java.net.URI)
   */
  public IFeed createFeed(URI link) {
    return new Feed(link);
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createNews(org.rssowl.core.model.types.IFeed)
   */
  public INews createNews(IFeed feed) {
    return new News(feed);
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createAttachment(org.rssowl.core.model.types.INews)
   */
  public IAttachment createAttachment(INews news) {
    return new Attachment(news);
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createCategory(org.rssowl.core.model.types.IExtendableType)
   */
  public ICategory createCategory(final IEntity type) {
    return new Category();
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterTypesFactory#createImage(org.rssowl.core.model.types.IFeed,
   * java.net.URI)
   */
  public IImage createImage(final IFeed feed) {
    return new Image();
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createSource(org.rssowl.core.model.types.INews)
   */
  public ISource createSource(final INews news) {
    return new Source();
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createGuid(org.rssowl.core.model.types.INews)
   */
  public IGuid createGuid(final INews news) {
    return new Guid();
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createCloud(org.rssowl.core.model.types.IFeed)
   */
  public ICloud createCloud(final IFeed feed) {
    return new CloudAdapter();
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createTextInput(org.rssowl.core.model.types.IFeed)
   */
  public ITextInput createTextInput(final IFeed feed) {
    return new TextInputAdapter();
  }

  /*
   * @see org.rssowl.core.model.types.ITypesFactory#createPerson(org.rssowl.core.model.types.IExtendableType)
   */
  public IPerson createPerson(final IPersistable type) {
    return new Person(null);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterTypesFactory#createFolder(org.rssowl.core.model.types.IFolder,
   * java.lang.String)
   */
  public IFolder createFolder(IFolder parent, String name) {
    Folder folder = new Folder(null, parent, name);

    /* Automatically add to the Folder if given */
    if (parent != null)
      parent.addFolder(folder);

    return folder;
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterTypesFactory#createBookMark(org.rssowl.core.model.types.IFolder,
   * org.rssowl.core.model.reference.FeedLinkReference, java.lang.String)
   */
  public IBookMark createBookMark(IFolder folder, FeedLinkReference feedRef, String name) {
    BookMark bookMark = new BookMark(null, folder, feedRef, name);

    /* Automatically add to the Folder */
    folder.addMark(bookMark);

    return bookMark;
  }
}