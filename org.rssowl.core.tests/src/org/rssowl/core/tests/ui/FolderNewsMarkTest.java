/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderNewsMark;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for the {@link FolderNewsMark}.
 *
 * @author bpasero
 */
public class FolderNewsMarkTest {
  private IModelFactory fFactory = Owl.getModelFactory();

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSimpleFolderNewsMark() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");
    childFolder.setProperty("foo", "bar");

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);
    news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");
    folder = DynamicDAO.save(folder);

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    assertEquals(childFolder.getId(), mark.getId());
    assertEquals(childFolder, mark.getFolder());
    assertEquals("bar", mark.getProperty("foo"));
    assertTrue(Long.valueOf(mark.toReference().getId()).equals(childFolder.getId()));

    assertEquals(2, mark.getNews().size());
    assertEquals(2, mark.getNews(EnumSet.of(INews.State.NEW, INews.State.READ)).size());
    assertEquals(1, mark.getNews(EnumSet.of(INews.State.NEW)).size());
    assertEquals(1, mark.getNews(EnumSet.of(INews.State.READ)).size());
    assertEquals(2, mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.READ)));
    assertEquals(1, mark.getNewsCount(EnumSet.of(INews.State.NEW)));
    assertEquals(1, mark.getNewsCount(EnumSet.of(INews.State.READ)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFolderNewsMark() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setState(INews.State.NEW);
    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setState(INews.State.UNREAD);
    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");

    IFeed otherFeed = fFactory.createFeed(null, new URI("otherfeed"));
    INews othernews1 = fFactory.createNews(null, otherFeed, new Date());
    othernews1.setState(INews.State.NEW);
    INews othernews2 = fFactory.createNews(null, otherFeed, new Date());
    othernews2.setState(INews.State.UNREAD);
    INews othernews3 = fFactory.createNews(null, otherFeed, new Date());
    othernews3.setState(INews.State.READ);
    DynamicDAO.save(otherFeed);

    fFactory.createBookMark(null, folder, new FeedLinkReference(otherFeed.getLink()), "Other Mark");

    INewsBin bin = fFactory.createNewsBin(null, childFolder, "bin");
    DynamicDAO.save(bin);
    INews copiedNews1 = fFactory.createNews(news1, bin);
    INews copiedNews2 = fFactory.createNews(news2, bin);
    INews copiedNews3 = fFactory.createNews(news3, bin);
    DynamicDAO.save(copiedNews1);
    DynamicDAO.save(copiedNews2);
    DynamicDAO.save(copiedNews3);

    ISearchField stateField = fFactory.createSearchField(INews.STATE, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
    ISearchMark search = fFactory.createSearchMark(null, childFolder, "search");
    search.addSearchCondition(condition);

    folder = DynamicDAO.save(folder);

    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    mark.resolve(null);

    {
      List<INews> news = mark.getNews();
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(INews.State.getVisible());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertTrue(news.contains(news1));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(othernews1));
    }

    {
      assertEquals(7, mark.getNewsCount(INews.State.getVisible()));
      assertEquals(3, mark.getNewsCount(EnumSet.of(INews.State.NEW)));
      assertEquals(5, mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
    }

    {
      List<NewsReference> news = mark.getNewsRefs();
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(INews.State.getVisible());
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW));
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      assertTrue(mark.containsNews(news1));
      assertTrue(mark.containsNews(news2));
      assertTrue(mark.containsNews(news3));
      assertTrue(mark.containsNews(copiedNews1));
      assertTrue(mark.containsNews(copiedNews2));
      assertTrue(mark.containsNews(copiedNews3));
      assertTrue(mark.containsNews(othernews1));
    }

    {
      assertTrue(mark.isRelatedTo(getEvent(news1), false));
      assertTrue(mark.isRelatedTo(getEvent(news2), false));
      assertTrue(mark.isRelatedTo(getEvent(news3), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews1), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews2), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews3), false));
    }

    {
      assertTrue(mark.isRelatedTo(search));
    }
  }

  private NewsEvent getEvent(INews news) {
    return new NewsEvent(news, news, true);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFolderNewsMarkAdd() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");

    DynamicDAO.save(folder);

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    mark.resolve(null);

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setState(INews.State.NEW);
    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setState(INews.State.UNREAD);
    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");

    IFeed otherFeed = fFactory.createFeed(null, new URI("otherfeed"));
    INews othernews1 = fFactory.createNews(null, otherFeed, new Date());
    othernews1.setState(INews.State.NEW);
    INews othernews2 = fFactory.createNews(null, otherFeed, new Date());
    othernews2.setState(INews.State.UNREAD);
    INews othernews3 = fFactory.createNews(null, otherFeed, new Date());
    othernews3.setState(INews.State.READ);
    DynamicDAO.save(otherFeed);

    fFactory.createBookMark(null, folder, new FeedLinkReference(otherFeed.getLink()), "Other Mark");

    INewsBin bin = fFactory.createNewsBin(null, childFolder, "bin");
    DynamicDAO.save(bin);
    INews copiedNews1 = fFactory.createNews(news1, bin);
    INews copiedNews2 = fFactory.createNews(news2, bin);
    INews copiedNews3 = fFactory.createNews(news3, bin);
    DynamicDAO.save(copiedNews1);
    DynamicDAO.save(copiedNews2);
    DynamicDAO.save(copiedNews3);

    ISearchField stateField = fFactory.createSearchField(INews.STATE, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
    ISearchMark search = fFactory.createSearchMark(null, childFolder, "search");
    search.addSearchCondition(condition);

    folder = DynamicDAO.save(folder);

    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    mark.add(Collections.singleton(news1));
    mark.add(Collections.singleton(news2));
    mark.add(Collections.singleton(news3));
    mark.add(Collections.singleton(copiedNews1));
    mark.add(Collections.singleton(copiedNews2));
    mark.add(Collections.singleton(copiedNews3));
    mark.add(Collections.singleton(othernews1));

    {
      List<INews> news = mark.getNews();
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(INews.State.getVisible());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertTrue(news.contains(news1));
      assertTrue(news.contains(othernews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(othernews1));
    }

    {
      assertEquals(7, mark.getNewsCount(INews.State.getVisible()));
      assertEquals(3, mark.getNewsCount(EnumSet.of(INews.State.NEW)));
      assertEquals(5, mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
    }

    {
      List<NewsReference> news = mark.getNewsRefs();
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(INews.State.getVisible());
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW));
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(othernews1.toReference()));
    }

    {
      assertTrue(mark.containsNews(news1));
      assertTrue(mark.containsNews(news2));
      assertTrue(mark.containsNews(news3));
      assertTrue(mark.containsNews(copiedNews1));
      assertTrue(mark.containsNews(copiedNews2));
      assertTrue(mark.containsNews(copiedNews3));
      assertTrue(mark.containsNews(othernews1));
    }

    {
      assertTrue(mark.isRelatedTo(getEvent(news1), false));
      assertTrue(mark.isRelatedTo(getEvent(news2), false));
      assertTrue(mark.isRelatedTo(getEvent(news3), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews1), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews2), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews3), false));
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFolderNewsMarkRemove() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setState(INews.State.NEW);
    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setState(INews.State.UNREAD);
    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");

    IFeed otherFeed = fFactory.createFeed(null, new URI("otherfeed"));
    INews otherNews1 = fFactory.createNews(null, otherFeed, new Date());
    otherNews1.setState(INews.State.NEW);
    INews othernews2 = fFactory.createNews(null, otherFeed, new Date());
    othernews2.setState(INews.State.UNREAD);
    INews othernews3 = fFactory.createNews(null, otherFeed, new Date());
    othernews3.setState(INews.State.READ);
    DynamicDAO.save(otherFeed);

    fFactory.createBookMark(null, folder, new FeedLinkReference(otherFeed.getLink()), "Other Mark");

    INewsBin bin = fFactory.createNewsBin(null, childFolder, "bin");
    DynamicDAO.save(bin);
    INews copiedNews1 = fFactory.createNews(news1, bin);
    INews copiedNews2 = fFactory.createNews(news2, bin);
    INews copiedNews3 = fFactory.createNews(news3, bin);
    DynamicDAO.save(copiedNews1);
    DynamicDAO.save(copiedNews2);
    DynamicDAO.save(copiedNews3);

    ISearchField stateField = fFactory.createSearchField(INews.STATE, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
    ISearchMark search = fFactory.createSearchMark(null, childFolder, "search");
    search.addSearchCondition(condition);

    folder = DynamicDAO.save(folder);

    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    mark.resolve(null);

    List<NewsEvent> events = new ArrayList<NewsEvent>();
    News oldNews = new News((News) news1, -1);
    oldNews.setId(news1.getId());
    NewsEvent event1 = new NewsEvent(oldNews, news1, true);
    news1.setState(INews.State.HIDDEN);
    News oldNews2 = new News((News) copiedNews1, -1);
    oldNews2.setId(copiedNews1.getId());
    NewsEvent event2 = new NewsEvent(oldNews2, copiedNews1, true);
    copiedNews1.setState(INews.State.HIDDEN);
    News oldNews3 = new News((News) otherNews1, -1);
    oldNews3.setId(otherNews1.getId());
    NewsEvent event3 = new NewsEvent(oldNews3, otherNews1, true);
    otherNews1.setState(INews.State.HIDDEN);
    events.add(event1);
    events.add(event2);
    events.add(event3);

    mark.remove(events);

    {
      List<INews> news = mark.getNews();
      assertEquals(4, news.size());
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
    }

    {
      List<INews> news = mark.getNews(INews.State.getVisible());
      assertEquals(4, news.size());
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertTrue(news.isEmpty());
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertEquals(2, news.size());
      assertTrue(news.contains(news2));
      assertTrue(news.contains(copiedNews2));
    }

    {
      assertEquals(4, mark.getNewsCount(INews.State.getVisible()));
      assertEquals(0, mark.getNewsCount(EnumSet.of(INews.State.NEW)));
      assertEquals(2, mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
    }

    {
      List<NewsReference> news = mark.getNewsRefs();
      assertEquals(4, news.size());
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(INews.State.getVisible());
      assertEquals(4, news.size());
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW));
      assertEquals(0, news.size());
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertEquals(2, news.size());
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
    }

    {
      assertFalse(mark.containsNews(news1));
      assertTrue(mark.containsNews(news2));
      assertTrue(mark.containsNews(news3));
      assertFalse(mark.containsNews(copiedNews1));
      assertTrue(mark.containsNews(copiedNews2));
      assertTrue(mark.containsNews(copiedNews3));
      assertFalse(mark.containsNews(otherNews1));
    }

    {
      assertTrue(mark.isRelatedTo(getEvent(news1), false));
      assertTrue(mark.isRelatedTo(getEvent(news2), false));
      assertTrue(mark.isRelatedTo(getEvent(news3), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews1), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews2), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews3), false));
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFolderNewsMarkUpdate() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setState(INews.State.NEW);
    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setState(INews.State.UNREAD);
    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");

    IFeed otherFeed = fFactory.createFeed(null, new URI("otherfeed"));
    INews otherNews1 = fFactory.createNews(null, otherFeed, new Date());
    otherNews1.setState(INews.State.NEW);
    INews othernews2 = fFactory.createNews(null, otherFeed, new Date());
    othernews2.setState(INews.State.UNREAD);
    INews othernews3 = fFactory.createNews(null, otherFeed, new Date());
    othernews3.setState(INews.State.READ);
    DynamicDAO.save(otherFeed);

    fFactory.createBookMark(null, folder, new FeedLinkReference(otherFeed.getLink()), "Other Mark");

    INewsBin bin = fFactory.createNewsBin(null, childFolder, "bin");
    DynamicDAO.save(bin);
    INews copiedNews1 = fFactory.createNews(news1, bin);
    copiedNews1.setState(INews.State.READ);
    INews copiedNews2 = fFactory.createNews(news2, bin);
    INews copiedNews3 = fFactory.createNews(news3, bin);
    DynamicDAO.save(copiedNews1);
    DynamicDAO.save(copiedNews2);
    DynamicDAO.save(copiedNews3);

    ISearchField stateField = fFactory.createSearchField(INews.STATE, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
    ISearchMark search = fFactory.createSearchMark(null, childFolder, "search");
    search.addSearchCondition(condition);

    folder = DynamicDAO.save(folder);

    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    mark.resolve(null);

    List<NewsEvent> events = new ArrayList<NewsEvent>();

    News oldNews = new News((News) news1, -1);
    oldNews.setId(news1.getId());
    NewsEvent event1 = new NewsEvent(oldNews, news1, true);
    news1.setState(INews.State.READ);

    News oldNews2 = new News((News) copiedNews1, -1);
    oldNews2.setId(copiedNews1.getId());
    NewsEvent event2 = new NewsEvent(oldNews2, copiedNews1, true);
    copiedNews1.setState(INews.State.NEW);

    News oldNews3 = new News((News) otherNews1, -1);
    oldNews3.setId(otherNews1.getId());
    NewsEvent event3 = new NewsEvent(oldNews3, otherNews1, true);
    otherNews1.setState(INews.State.UNREAD);

    events.add(event1);
    events.add(event2);
    events.add(event3);

    mark.update(events);

    {
      List<INews> news = mark.getNews();
      assertEquals(7, news.size());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(otherNews1));
    }

    {
      List<INews> news = mark.getNews(INews.State.getVisible());
      assertEquals(7, news.size());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(news2));
      assertTrue(news.contains(news3));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(copiedNews3));
      assertTrue(news.contains(otherNews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertEquals(1, news.size());
      assertTrue(news.contains(copiedNews1));
    }

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertEquals(4, news.size());
      assertTrue(news.contains(news2));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(copiedNews2));
      assertTrue(news.contains(otherNews1));
    }

    {
      assertEquals(7, mark.getNewsCount(INews.State.getVisible()));
      assertEquals(1, mark.getNewsCount(EnumSet.of(INews.State.NEW)));
      assertEquals(4, mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
    }

    {
      List<NewsReference> news = mark.getNewsRefs();
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(otherNews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(INews.State.getVisible());
      assertTrue(news.contains(news1.toReference()));
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(news3.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(copiedNews3.toReference()));
      assertTrue(news.contains(otherNews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW));
      assertEquals(1, news.size());
      assertTrue(news.contains(copiedNews1.toReference()));
    }

    {
      List<NewsReference> news = mark.getNewsRefs(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      assertEquals(4, news.size());
      assertTrue(news.contains(news2.toReference()));
      assertTrue(news.contains(copiedNews1.toReference()));
      assertTrue(news.contains(copiedNews2.toReference()));
      assertTrue(news.contains(otherNews1.toReference()));
    }

    {
      assertTrue(mark.containsNews(news1));
      assertTrue(mark.containsNews(news2));
      assertTrue(mark.containsNews(news3));
      assertTrue(mark.containsNews(copiedNews1));
      assertTrue(mark.containsNews(copiedNews2));
      assertTrue(mark.containsNews(copiedNews3));
      assertTrue(mark.containsNews(otherNews1));
    }

    {
      assertTrue(mark.isRelatedTo(getEvent(news1), false));
      assertTrue(mark.isRelatedTo(getEvent(news2), false));
      assertTrue(mark.isRelatedTo(getEvent(news3), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews1), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews2), false));
      assertTrue(mark.isRelatedTo(getEvent(copiedNews3), false));
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFolderNewsMarkResultsChange() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Root");
    IFolder childFolder = fFactory.createFolder(null, folder, "Child");

    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setState(INews.State.NEW);
    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setState(INews.State.UNREAD);
    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setState(INews.State.READ);
    DynamicDAO.save(feed);

    fFactory.createBookMark(null, childFolder, new FeedLinkReference(feed.getLink()), "Mark");

    IFeed otherFeed = fFactory.createFeed(null, new URI("otherfeed"));
    INews otherNews1 = fFactory.createNews(null, otherFeed, new Date());
    otherNews1.setState(INews.State.NEW);
    INews othernews2 = fFactory.createNews(null, otherFeed, new Date());
    othernews2.setState(INews.State.UNREAD);
    INews othernews3 = fFactory.createNews(null, otherFeed, new Date());
    othernews3.setState(INews.State.READ);
    DynamicDAO.save(otherFeed);

    fFactory.createBookMark(null, folder, new FeedLinkReference(otherFeed.getLink()), "Other Mark");

    INewsBin bin = fFactory.createNewsBin(null, childFolder, "bin");
    DynamicDAO.save(bin);
    INews copiedNews1 = fFactory.createNews(news1, bin);
    INews copiedNews2 = fFactory.createNews(news2, bin);
    INews copiedNews3 = fFactory.createNews(news3, bin);
    DynamicDAO.save(copiedNews1);
    DynamicDAO.save(copiedNews2);
    DynamicDAO.save(copiedNews3);

    ISearchField stateField = fFactory.createSearchField(INews.STATE, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(stateField, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
    ISearchMark search = fFactory.createSearchMark(null, childFolder, "search");
    search.addSearchCondition(condition);

    folder = DynamicDAO.save(folder);

    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    FolderNewsMark mark = new FolderNewsMark(childFolder);
    mark.resolve(null);

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertEquals(3, news.size());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(otherNews1));
    }

    othernews2.setState(INews.State.NEW);
    DynamicDAO.save(othernews2);
    waitForIndexer();
    Controller.getDefault().getSavedSearchService().forceQuickUpdate();
    waitForIndexer();

    SearchMarkEvent event = new SearchMarkEvent(search, null, true);
    Set<SearchMarkEvent> events = Collections.singleton(event);
    mark.newsChanged(events);

    {
      List<INews> news = mark.getNews(EnumSet.of(INews.State.NEW));
      assertEquals(4, news.size());
      assertTrue(news.contains(news1));
      assertTrue(news.contains(copiedNews1));
      assertTrue(news.contains(othernews2));
      assertTrue(news.contains(otherNews1));
    }
  }

  /**
   * @throws InterruptedException
   */
  protected void waitForIndexer() throws InterruptedException {
    Thread.sleep(500);
  }
}