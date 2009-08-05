/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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

package org.rssowl.core.tests.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests methods in CoreUtils.
 */
public class CoreUtilsTest {

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testNormalize() throws Exception {
    CoreUtils.normalize(null);
    CoreUtils.normalize(Collections.EMPTY_LIST);

    IModelFactory factory = Owl.getModelFactory();
    IFolder root = factory.createFolder(null, null, "Root");
    IFolder folder1 = factory.createFolder(null, root, "Folder 1");
    IFolder folder2 = factory.createFolder(null, root, "Folder 2");
    IFolder folder3 = factory.createFolder(null, folder2, "Folder 3");
    IBookMark mark1 = factory.createBookMark(null, root, new FeedLinkReference(new URI("#")), "Mark 1");
    IBookMark mark2 = factory.createBookMark(null, folder1, new FeedLinkReference(new URI("#")), "Mark 2");
    IBookMark mark3 = factory.createBookMark(null, folder2, new FeedLinkReference(new URI("#")), "Mark 3");
    IBookMark mark4 = factory.createBookMark(null, folder3, new FeedLinkReference(new URI("#")), "Mark 4");

    List<IEntity> entities = new ArrayList<IEntity>();
    entities.add(root);
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, folder1, folder2, folder3 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, folder1, folder2, folder3, mark1, mark2, mark3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { mark1, mark2, mark3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(4, entities.size());
    assertEquals(mark1, entities.get(0));
    assertEquals(mark2, entities.get(1));
    assertEquals(mark3, entities.get(2));
    assertEquals(mark4, entities.get(3));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { folder3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(folder3, entities.get(0));
  }
}