/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.ui.internal.editors.feed.NewsBrowserViewModel;

import java.net.URI;
import java.util.Date;

/**
 * Testing the NewsBrowserViewModel.
 *
 * @author bpasero
 */
public class NewsBrowserViewModelTests {
  private IModelFactory fFactory = Owl.getModelFactory();

  /**
   * @throws Exception
   */
  @Test
  public void testNullModel() throws Exception {
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(null);

    assertEquals(-1L, model.findGroup(5L).longValue());
    assertEquals(-1L, model.getExpandedNews().longValue());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(-1L, model.nextNews(false, -1L).longValue());
    assertEquals(-1L, model.previousNews(false, -1L).longValue());
    assertEquals(-1L, model.nextNews(true, -1L).longValue());
    assertEquals(-1L, model.previousNews(true, -1L).longValue());
    assertEquals(-1L, model.nextNews(false, 5L).longValue());
    assertEquals(-1L, model.previousNews(false, 5L).longValue());
    assertEquals(-1L, model.nextNews(true, 5L).longValue());
    assertEquals(-1L, model.previousNews(true, 5L).longValue());
    assertEquals(-1L, model.removeNews(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())).longValue());

    model.setExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()), false);
    model.setGroupExpanded(5L, false);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testEmptyModel() throws Exception {
    Object[] elements = new Object[0];
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements);

    assertEquals(-1L, model.findGroup(5L).longValue());
    assertEquals(-1L, model.getExpandedNews().longValue());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(-1L, model.nextNews(false, -1L).longValue());
    assertEquals(-1L, model.previousNews(false, -1L).longValue());
    assertEquals(-1L, model.nextNews(true, -1L).longValue());
    assertEquals(-1L, model.previousNews(true, -1L).longValue());
    assertEquals(-1L, model.nextNews(false, 5L).longValue());
    assertEquals(-1L, model.previousNews(false, 5L).longValue());
    assertEquals(-1L, model.nextNews(true, 5L).longValue());
    assertEquals(-1L, model.previousNews(true, 5L).longValue());
    assertEquals(-1L, model.removeNews(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())).longValue());

    model.setExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()), false);
    model.setGroupExpanded(5L, false);
  }
}