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

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.Triple;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.editors.feed.NewsBrowserViewModel;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Testing the NewsBrowserViewModel.
 *
 * @author bpasero
 */
public class NewsBrowserViewModelTests {
  private IModelFactory fFactory;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNullModel() throws Exception {
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(null);

    assertEquals(-1L, model.findGroup(5L));
    assertFalse(model.isFirstItemUnread());
    assertEquals(-1L, model.getExpandedNews());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(-1L, model.nextNews(false, -1L));
    assertEquals(-1L, model.previousNews(false, -1L));
    assertEquals(-1L, model.nextNews(true, -1L));
    assertEquals(-1L, model.previousNews(true, -1L));
    assertEquals(-1L, model.nextNews(false, 5L));
    assertEquals(-1L, model.previousNews(false, 5L));
    assertEquals(-1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.previousNews(true, 5L));
    assertEquals(-1L, model.removeNews(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertEquals(0, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()), false);
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

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(-1L, model.getExpandedNews());
    assertFalse(model.isFirstItemUnread());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(-1L, model.nextNews(false, -1L));
    assertEquals(-1L, model.previousNews(false, -1L));
    assertEquals(-1L, model.nextNews(true, -1L));
    assertEquals(-1L, model.previousNews(true, -1L));
    assertEquals(-1L, model.nextNews(false, 5L));
    assertEquals(-1L, model.previousNews(false, 5L));
    assertEquals(-1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.previousNews(true, 5L));
    assertEquals(-1L, model.removeNews(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));

    assertEquals(0, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()), false);
    model.setGroupExpanded(5L, false);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testEmptyModelWithPageSize() throws Exception {
    Object[] elements = new Object[0];
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements, 5);

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(-1L, model.getExpandedNews());
    assertFalse(model.isFirstItemUnread());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(-1L, model.nextNews(false, -1L));
    assertEquals(-1L, model.previousNews(false, -1L));
    assertEquals(-1L, model.nextNews(true, -1L));
    assertEquals(-1L, model.previousNews(true, -1L));
    assertEquals(-1L, model.nextNews(false, 5L));
    assertEquals(-1L, model.previousNews(false, 5L));
    assertEquals(-1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.previousNews(true, 5L));
    assertEquals(-1L, model.removeNews(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date())));

    assertEquals(0, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsExpanded(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()), false);
    model.setGroupExpanded(5L, false);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFlatModel() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    Object[] elements = new Object[] { news1, news2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements);

    assertTrue(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(3, model.getNewsCount());
    assertEquals(3, model.getVisibleNewsCount());

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    assertEquals(-1L, model.removeNews(news2));
    assertEquals(3L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));

    assertEquals(2, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    model.setNewsVisible(news1, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(1, model.getVisibleNewsCount());

    model.setNewsVisible(news3, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    model.setNewsVisible(news1, true);
    model.setNewsVisible(news3, true);

    assertEquals(2, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, true);

    revealed = model.reveal(news1);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(1, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    revealed = model.reveal(news3);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, false);

    revealed = model.reveal(news3);
    assertEquals(2, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));
    assertEquals(news3.getId(), revealed.getSecond().get(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFlatModelWithSmallPage() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    Object[] elements = new Object[] { news1, news2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements, 2);

    assertTrue(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(3, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(2);
    assertTrue(nextPage.getFirst().isEmpty());
    assertEquals(1, nextPage.getSecond().size());
    assertEquals(news3.getId(), nextPage.getSecond().get(0));
    assertFalse(nextPage.getThird());

    assertEquals(-1L, model.removeNews(news2));
    assertEquals(3L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));

    assertEquals(2, model.getNewsCount());
    assertEquals(1, model.getVisibleNewsCount());

    model.setNewsVisible(news1, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    model.setNewsVisible(news3, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    model.setNewsVisible(news1, true);
    model.setNewsVisible(news3, true);

    assertEquals(2, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, true);

    revealed = model.reveal(news1);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(1, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    revealed = model.reveal(news3);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, false);

    revealed = model.reveal(news3);
    assertEquals(2, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));
    assertEquals(news3.getId(), revealed.getSecond().get(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFlatModelWithLargePage() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    Object[] elements = new Object[] { news1, news2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements, 5);

    assertTrue(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());
    assertTrue(model.getGroups().isEmpty());
    assertEquals(0, model.getGroupSize(5L));
    assertTrue(model.getNewsIds(5L).isEmpty());
    assertFalse(model.hasGroup(5L));
    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));
    assertTrue(model.isGroupExpanded(5L));
    assertEquals(3, model.getNewsCount());
    assertEquals(3, model.getVisibleNewsCount());

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    assertEquals(-1L, model.removeNews(news2));
    assertEquals(3L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));

    assertEquals(2, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    model.setNewsVisible(news1, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(1, model.getVisibleNewsCount());

    model.setNewsVisible(news3, false);
    assertEquals(2, model.getNewsCount());
    assertEquals(0, model.getVisibleNewsCount());

    model.setNewsVisible(news1, true);
    model.setNewsVisible(news3, true);

    assertEquals(2, model.getNewsCount());
    assertEquals(2, model.getVisibleNewsCount());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, true);

    revealed = model.reveal(news1);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(1, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    revealed = model.reveal(news3);
    assertTrue(revealed.getFirst().isEmpty());
    assertEquals(news1.getId(), revealed.getSecond().get(0));

    model.setNewsVisible(news1, false);
    model.setNewsVisible(news3, false);

    revealed = model.reveal(news3);
    assertEquals(2, revealed.getSecond().size());
    assertEquals(news1.getId(), revealed.getSecond().get(0));
    assertEquals(news3.getId(), revealed.getSecond().get(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGroupedModel() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    EntityGroup group1 = new EntityGroup(100, "foo");
    EntityGroup group2 = new EntityGroup(101, "foo");

    Object[] elements = new Object[] { group1, news1, news2, group2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements);

    assertFalse(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(100L, model.findGroup(1L));
    assertEquals(100L, model.findGroup(2L));
    assertEquals(101L, model.findGroup(3L));

    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());

    assertEquals(2, model.getGroups().size());
    assertEquals(2, model.getGroupSize(100L));
    assertEquals(1, model.getGroupSize(101L));

    Map<Long, List<Long>> groups = model.getGroups();
    List<Long> group_1 = groups.get(100L);
    List<Long> group_2 = groups.get(101L);
    assertEquals(1, group_1.get(0).longValue());
    assertEquals(2, group_1.get(1).longValue());
    assertEquals(3, group_2.get(0).longValue());

    List<Long> newsIds = model.getNewsIds(100L);
    assertEquals(1, newsIds.get(0).longValue());
    assertEquals(2, newsIds.get(1).longValue());

    assertFalse(model.hasGroup(5L));
    assertTrue(model.hasGroup(100L));
    assertTrue(model.hasGroup(101L));

    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));

    assertTrue(model.isGroupExpanded(5L));
    model.setGroupExpanded(100L, false);
    assertFalse(model.isGroupExpanded(100L));

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    assertEquals(100L, model.removeNews(news1));
    assertTrue(model.hasGroup(100L));
    assertEquals(100L, model.removeNews(news2));
    assertFalse(model.hasGroup(100L));
    assertEquals(101L, model.removeNews(news3));
    assertFalse(model.hasGroup(101L));

    assertFalse(model.isNewsExpanded(news1));
    assertFalse(model.isNewsExpanded(news2));
    assertFalse(model.isNewsExpanded(news3));

    assertEquals(0, model.getGroups().size());
    assertEquals(0, model.getGroupSize(100L));
    assertEquals(0, model.getGroupSize(101L));

    assertTrue(model.getNewsIds(100L).isEmpty());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGroupedModelWithLargePage() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    EntityGroup group1 = new EntityGroup(100, "foo");
    EntityGroup group2 = new EntityGroup(101, "foo");

    Object[] elements = new Object[] { group1, news1, news2, group2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements, 5);

    assertFalse(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(100L, model.findGroup(1L));
    assertEquals(100L, model.findGroup(2L));
    assertEquals(101L, model.findGroup(3L));

    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());

    assertEquals(2, model.getGroups().size());
    assertEquals(2, model.getGroupSize(100L));
    assertEquals(1, model.getGroupSize(101L));

    Map<Long, List<Long>> groups = model.getGroups();
    List<Long> group_1 = groups.get(100L);
    List<Long> group_2 = groups.get(101L);
    assertEquals(1, group_1.get(0).longValue());
    assertEquals(2, group_1.get(1).longValue());
    assertEquals(3, group_2.get(0).longValue());

    List<Long> newsIds = model.getNewsIds(100L);
    assertEquals(1, newsIds.get(0).longValue());
    assertEquals(2, newsIds.get(1).longValue());

    assertFalse(model.hasGroup(5L));
    assertTrue(model.hasGroup(100L));
    assertTrue(model.hasGroup(101L));

    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));

    assertTrue(model.isGroupExpanded(5L));
    model.setGroupExpanded(100L, false);
    assertFalse(model.isGroupExpanded(100L));

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    assertEquals(100L, model.removeNews(news1));
    assertTrue(model.hasGroup(100L));
    assertEquals(100L, model.removeNews(news2));
    assertFalse(model.hasGroup(100L));
    assertEquals(101L, model.removeNews(news3));
    assertFalse(model.hasGroup(101L));

    assertFalse(model.isNewsExpanded(news1));
    assertFalse(model.isNewsExpanded(news2));
    assertFalse(model.isNewsExpanded(news3));

    assertEquals(0, model.getGroups().size());
    assertEquals(0, model.getGroupSize(100L));
    assertEquals(0, model.getGroupSize(101L));

    assertTrue(model.getNewsIds(100L).isEmpty());

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(0);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    nextPage = model.getNextPage(5);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    Pair<List<Long>, List<Long>> revealed = model.reveal(fFactory.createNews(5L, fFactory.createFeed(null, URI.create("rssowl.org")), new Date()));
    assertTrue(revealed.getFirst().isEmpty());
    assertTrue(revealed.getSecond().isEmpty());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGroupedModelWithSmallPage() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));
    INews news1 = fFactory.createNews(1L, feed, new Date());
    INews news2 = fFactory.createNews(2L, feed, new Date());
    news2.setState(INews.State.READ);
    INews news3 = fFactory.createNews(3L, feed, new Date());

    DynamicDAO.save(feed);

    EntityGroup group1 = new EntityGroup(100, "foo");
    EntityGroup group2 = new EntityGroup(101, "foo");

    Object[] elements = new Object[] { group1, news1, news2, group2, news3, feed };
    NewsBrowserViewModel model = new NewsBrowserViewModel();
    model.setInput(elements, 2);

    assertFalse(model.isFirstItemUnread());

    assertEquals(-1L, model.findGroup(5L));
    assertEquals(100L, model.findGroup(1L));
    assertEquals(100L, model.findGroup(2L));
    assertEquals(101L, model.findGroup(3L));

    assertEquals(-1L, model.getExpandedNews());
    model.setNewsExpanded(news3, true);
    assertEquals(3L, model.getExpandedNews());

    assertEquals(2, model.getGroups().size());
    assertEquals(2, model.getGroupSize(100L));
    assertEquals(1, model.getGroupSize(101L));

    Map<Long, List<Long>> groups = model.getGroups();
    List<Long> group_1 = groups.get(100L);
    List<Long> group_2 = groups.get(101L);
    assertEquals(1, group_1.get(0).longValue());
    assertEquals(2, group_1.get(1).longValue());
    assertEquals(3, group_2.get(0).longValue());

    List<Long> newsIds = model.getNewsIds(100L);
    assertEquals(1, newsIds.get(0).longValue());
    assertEquals(2, newsIds.get(1).longValue());

    assertFalse(model.hasGroup(5L));
    assertTrue(model.hasGroup(100L));
    assertTrue(model.hasGroup(101L));

    assertFalse(model.isNewsExpanded(news2));
    assertTrue(model.isNewsExpanded(news3));

    assertTrue(model.isGroupExpanded(5L));
    model.setGroupExpanded(100L, false);
    assertFalse(model.isGroupExpanded(100L));

    assertEquals(1L, model.nextNews(false, -1L));
    assertEquals(1L, model.nextNews(true, -1L));
    assertEquals(2L, model.nextNews(false, 1L));
    assertEquals(3L, model.nextNews(true, 1L));
    assertEquals(-1L, model.nextNews(true, 3L));
    assertEquals(1L, model.nextNews(true, 5L));
    assertEquals(-1L, model.nextNews(false, 3L));
    assertEquals(1L, model.nextNews(false, 5L));

    assertEquals(3L, model.previousNews(false, -1L));
    assertEquals(3L, model.previousNews(true, -1L));
    assertEquals(-1L, model.previousNews(false, 1L));
    assertEquals(-1L, model.previousNews(true, 1L));
    assertEquals(2L, model.previousNews(false, 3L));
    assertEquals(1L, model.previousNews(true, 3L));
    assertEquals(3L, model.previousNews(true, 5L));
    assertEquals(3L, model.previousNews(false, 5L));

    Triple<List<Long>, List<Long>, Boolean> nextPage = model.getNextPage(2);
    assertEquals(1, nextPage.getFirst().size());
    assertEquals(group2.getId(), nextPage.getFirst().get(0).longValue());
    assertEquals(1, nextPage.getSecond().size());
    assertEquals(news3.getId(), nextPage.getSecond().get(0));
    assertFalse(nextPage.getThird());

    model.setGroupVisible(group2.getId(), true);
    model.setNewsVisible(news3, true);

    nextPage = model.getNextPage(2);
    assertTrue(nextPage.getFirst().isEmpty());
    assertTrue(nextPage.getSecond().isEmpty());
    assertFalse(nextPage.getThird());

    model.setGroupVisible(group2.getId(), false);
    model.setNewsVisible(news3, false);

    Pair<List<Long>, List<Long>> revealed = model.reveal(news3);
    assertEquals(1, revealed.getFirst().size());
    assertEquals(group2.getId(), revealed.getFirst().get(0).longValue());
    assertEquals(1, revealed.getSecond().size());
    assertEquals(news3.getId(), revealed.getSecond().get(0));
  }
}