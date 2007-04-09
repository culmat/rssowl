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

package org.rssowl.core.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.internal.persist.BookMark;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.internal.persist.Folder;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.RSSOwlUI;

import java.net.URI;

/**
 * Testing the <code>RSSOwlUI</code> facade.
 *
 * @author bpasero
 */
public class MiscUITests {

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    NewsModel.getDefault().getPersistenceLayer().recreateSchema();
    NewsModel.getDefault().getPersistenceLayer().getModelSearch().shutdown();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFavicon() throws Exception {
    IModelDAO dao = NewsModel.getDefault().getPersistenceLayer().getModelDAO();

    /* Delete previously stored favicons */
    for (int i = 0; i < 5; i++)
      RSSOwlUI.deleteImage(i);

    IFeed feed = new Feed(new URI("http://www.rssowl.org/node/feed"));
    IFolder root = new Folder(null, null, "Root");
    IBookMark bookmark = new BookMark(null, root, new FeedLinkReference(feed.getLink()), "Bookmark");
    root.addMark(bookmark);

    feed = dao.saveFeed(feed);
    root = dao.saveFolder(root);

    assertEquals(null, RSSOwlUI.getFavicon(bookmark));

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    assertNotNull(RSSOwlUI.getFavicon(bookmark));

    dao.deleteBookMark(new BookMarkReference(bookmark.getId()));

    // Not yet implemented
    // assertEquals(null, RSSOwlUI.getFavicon(bookmark));
  }
}