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

package org.rssowl.core.tests.persist.service;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.service.DBManager;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.Controller;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.query.Query;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests defragmentation of db.
 */
public class DefragmentTest {

  private URI fPluginLocation;

  /**
   *
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    Owl.getPersistenceService().getModelSearch().shutdown();
    Controller.getDefault().getNewsService().testDirtyShutdown();
    fPluginLocation = FileLocator.toFileURL(Platform.getBundle("org.rssowl.core.tests").getEntry("/")).toURI();
    saveFeeds();
  }

  /**
   * Tests defragment.
   */
  @Test
  public void testDefragment() {
    String dbPath = DBManager.getDBFilePath();
    File originDbFile = new File(dbPath + ".origin");
    DBManager.copyFile(new File(dbPath), originDbFile);
    File defragmentedDbFile = new File(dbPath + ".dest");
    DBManager.copyDatabase(originDbFile, defragmentedDbFile,
        new NullProgressMonitor());

    System.gc();
    ObjectContainer db = Db4o.openFile(DBManager.createConfiguration(), originDbFile.getAbsolutePath());
    ObjectContainer defragmentedDb = Db4o.openFile(DBManager.createConfiguration(),
        defragmentedDbFile.getAbsolutePath());

    List<IEntity> entities = db.query(IEntity.class);
    assertEquals(entities.size(), defragmentedDb.query(IEntity.class).size());
    for (IEntity entity : entities) {
      Query query = defragmentedDb.query();
      query.constrain(entity.getClass());
      query.descend("fId").constrain(Long.valueOf(entity.getId())); //$NON-NLS-1$
      List<?> result = query.execute();
      assertEquals(1, result.size());
      assertEquals(entity, result.get(0));
    }
  }

  private void saveFeeds() throws Exception {
    List<IFeed> feeds = new ArrayList<IFeed>();
    for (int i = 1; i < 6; i++) {
      URI feedLink = fPluginLocation.resolve("data/performance/" + i + ".xml").toURL().toURI();
      IFeed feed = new Feed(feedLink);

      InputStream inS = new BufferedInputStream(new FileInputStream(new File(feed.getLink())));
      Owl.getInterpreter().interpret(inS, feed);

      feeds.add(feed);
    }
    DynamicDAO.saveAll(feeds);
  }
}
