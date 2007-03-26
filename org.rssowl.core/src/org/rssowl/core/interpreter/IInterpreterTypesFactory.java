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

package org.rssowl.core.interpreter;

import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.types.IAttachment;
import org.rssowl.core.model.types.IBookMark;
import org.rssowl.core.model.types.ICategory;
import org.rssowl.core.model.types.ICloud;
import org.rssowl.core.model.types.IEntity;
import org.rssowl.core.model.types.IExtendableType;
import org.rssowl.core.model.types.IFeed;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.core.model.types.IGuid;
import org.rssowl.core.model.types.IImage;
import org.rssowl.core.model.types.INews;
import org.rssowl.core.model.types.IPerson;
import org.rssowl.core.model.types.ISource;
import org.rssowl.core.model.types.ITextInput;

import java.net.URI;

/**
 * The Factory used by the Interpreter to create the Types while interpreting.
 * The Implementation is contributable via Extension-Point in case you want to
 * provide your own Model.
 * 
 * @author bpasero
 */
public interface IInterpreterTypesFactory {

  /**
   * @param link The Link of this Feed.
   * @return The new instance of this type.
   */
  IFeed createFeed(URI link);

  /**
   * @param feed The Parent type.
   * @return The new instance of this type.
   */
  INews createNews(IFeed feed);

  /**
   * @param type The Parent type.
   * @return The new instance of this type.
   */
  IPerson createPerson(IExtendableType type);

  /**
   * @param feed The Parent type.
   * @return The new instance of this type.
   */
  IImage createImage(IFeed feed);

  /**
   * @param news The Parent type.
   * @return The new instance of this type.
   */
  IAttachment createAttachment(INews news);

  /**
   * @param type The Parent type.
   * @return The new instance of this type.
   */
  ICategory createCategory(IEntity type);

  /**
   * @param news The Parent type.
   * @return The new instance of this type.
   */
  ISource createSource(INews news);

  /**
   * @param news The Parent type.
   * @return The new instance of this type.
   */
  IGuid createGuid(INews news);

  /**
   * @param feed The Parent type.
   * @return The new instance of this type.
   */
  ICloud createCloud(IFeed feed);

  /**
   * @param feed The Parent type.
   * @return The new instance of this type.
   */
  ITextInput createTextInput(IFeed feed);

  /**
   * @param parent The Parent Folder or <code>NULL</code> if none.
   * @param name The Name of the Folder.
   * @return The new instance of this type.
   */
  IFolder createFolder(IFolder parent, String name);

  /**
   * @param folder The Parent Folder of this BookMark.
   * @param feedRef The reference to the feed this BookMark is related to.
   * @param name The Name of the BookMark.
   * @return The new instance of this type.
   */
  IBookMark createBookMark(IFolder folder, FeedLinkReference feedRef, String name);
}