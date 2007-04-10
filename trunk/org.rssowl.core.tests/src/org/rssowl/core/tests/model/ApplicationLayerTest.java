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

package org.rssowl.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.BookMarkListener;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.events.NewsAdapter;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.events.SearchMarkListener;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.internal.persist.MergeResult;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.INews.State;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.tests.TestUtils;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.NewsService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This TestCase is for testing the IApplicationLayer.
 *
 * @author bpasero
 */
@SuppressWarnings("nls")
public class ApplicationLayerTest {
  private IModelTypesFactory fFactory;
  private IModelDAO fDao;
  private IApplicationLayer fAppLayer;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    Owl.getPersistenceService().getModelSearch().shutdown();
    Controller.getDefault().getNewsService().testDirtyShutdown();
    fFactory = Owl.getModelFactory();
    fDao = Owl.getPersistenceService().getModelDAO();
    fAppLayer = Owl.getPersistenceService().getApplicationLayer();
  }

  /**
   * See bug #184 : State change and async loading of the same news in different
   * feeds can lead to incorrect behaviour.
   *
   * @throws Exception
   */
  @Test
  public void testHandleReloadedWithAsyncLoadingOfEquivalentNews() throws Exception {
    IFeed feed0 = fFactory.createFeed(null, new URI("http://www.feed2.com"));
    URI newsLink = new URI("http://www.news.com");
    INews news0 = fFactory.createNews(null, feed0, new Date());
    news0.setLink(newsLink);
    news0.setState(INews.State.READ);
    fDao.saveFeed(feed0);

    IFolder folder = fFactory.createFolder(null, null, "Folder");
    IBookMark mark0 = fFactory.createBookMark(null, folder, new FeedLinkReference(feed0.getLink()), "Mark0");
    fDao.saveFolder(folder);
    fAppLayer.handleFeedReload(mark0, feed0, null, false);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed1.com"));
    fDao.saveFeed(feed1);
    IBookMark mark1 = fFactory.createBookMark(null, folder, new FeedLinkReference(feed1.getLink()), "Mark1");
    fDao.saveFolder(folder);

    feed1 = fFactory.createFeed(null, new URI("http://www.feed1.com"));
    INews news1 = fFactory.createNews(null, feed1, new Date());
    news1.setLink(newsLink);
    fAppLayer.handleFeedReload(mark1, feed1, null, false);

    assertEquals(INews.State.READ, fDao.loadNews(news1.getId()).getState());
  }

  /**
   * See bug #317 : Retention strategy works incorrectly if news is deleted
   * before being saved.
   *
   * @throws Exception
   */
  @Test
  public void testHandleFeedReloadWithRetentionStrategy() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.rssowl.org"));
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    IBookMark mark = fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark");
    fDao.saveFeed(feed);
    fDao.saveFolder(folder);

    IFeed emptyFeed = fFactory.createFeed(null, feed.getLink());
    INews news = fFactory.createNews(null, emptyFeed, new Date());
    news.setState(INews.State.READ);

    Owl.getPreferenceService().getEntityScope(mark).putBoolean(DefaultPreferences.DEL_READ_NEWS_STATE, true);
    fAppLayer.handleFeedReload(mark, emptyFeed, null, false);

    assertEquals(0, feed.getVisibleNews().size());
    assertEquals(INews.State.DELETED, fDao.loadNews(news.getId()).getState());
  }

  /**
   * See bug #318 : If attachments are deleted as part of a reload, oldNews are
   * not filled in NewsEvent.
   *
   * @throws Exception
   */
  @Test
  public void testHandleFeedReloadFillsOldNewsWithAttachmentDeleted() throws Exception {
    NewsListener newsListener = null;
    try {
      IFeed feed = fFactory.createFeed(null, new URI("http://www.rssowl.org"));
      INews news = fFactory.createNews(null, feed, new Date());
      news.setGuid(fFactory.createGuid(news, "newsguid"));
      fFactory.createAttachment(null, news);
      IFolder folder = fFactory.createFolder(null, null, "Folder");
      IBookMark mark = fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark");
      fDao.saveFeed(feed);
      fDao.saveFolder(folder);

      IFeed emptyFeed = fFactory.createFeed(null, feed.getLink());
      INews emptyNews = fFactory.createNews(null, emptyFeed, new Date());
      emptyNews.setGuid(fFactory.createGuid(news, news.getGuid().getValue()));

      newsListener = new NewsAdapter() {
        @Override
        public void newsUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());
          assertNotNull(events.iterator().next().getOldNews());
        }
      };
      Owl.getListenerService().addNewsListener(newsListener);
      fAppLayer.handleFeedReload(mark, emptyFeed, null, false);
    } finally {
      if (newsListener != null)
        Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests that calling setNewsState with force = true fires both events even
   * though the news state has not changed.
   *
   * @throws Exception
   */
  @Test
  public void testSetNewsStateWithEquivalentNewsAndForce() throws Exception {
    NewsListener newsListener = null;
    try {
      IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com"));
      IFeed feed2 = fFactory.createFeed(null, new URI("http://www.feed2.com"));

      INews news1 = fFactory.createNews(null, feed1, new Date());
      news1.setLink(new URI("www.link.com"));

      INews news2 = fFactory.createNews(null, feed2, new Date());
      news2.setLink(new URI("www.link.com"));

      fFactory.createNews(null, feed1, new Date());
      fFactory.createNews(null, feed2, new Date());

      fDao.saveFeed(feed1);
      feed2 = fDao.saveFeed(feed2);
      List<INews> newsList = new ArrayList<INews>(1);
      newsList.add(news2);
      final boolean[] newsUpdatedCalled = new boolean[1];
      newsListener = new NewsAdapter() {
        @Override
        public void newsUpdated(Set<NewsEvent> events) {
          newsUpdatedCalled[0] = true;
          assertEquals(2, events.size());
        }
      };
      Owl.getListenerService().addNewsListener(newsListener);
      fAppLayer.setNewsState(newsList, INews.State.NEW, true, true);
      assertEquals(true, newsUpdatedCalled[0]);
    } finally {
      if (newsListener != null)
        Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests that the all NewsEvents issued after a call to setNewsState are fully
   * activated even if there was an equivalent news that was not in memory.
   *
   * @throws Exception
   */
  @Test
  public void testSetNewsStateWithEquivalentNewsHasNewsEventEntityActivated() throws Exception {
    NewsListener newsListener = null;
    try {
      IFeed feed1 = Owl.getModelFactory().createFeed(null, new URI("http://www.feed.com"));
      IFeed feed2 = Owl.getModelFactory().createFeed(null, new URI("http://www.feed2.com"));

      INews news1 = Owl.getModelFactory().createNews(null, feed1, new Date());
      news1.setLink(new URI("www.link.com"));

      INews news2 = Owl.getModelFactory().createNews(null, feed2, new Date());
      news2.setLink(new URI("www.link.com"));

      Owl.getModelFactory().createNews(null, feed1, new Date());
      Owl.getModelFactory().createNews(null, feed2, new Date());

      fDao.saveFeed(feed1);
      feed2 = fDao.saveFeed(feed2);
      feed1 = null;
      feed2 = null;
      news1 = null;
      System.gc();

      List<INews> newsList = Collections.singletonList(news2);
      final boolean[] newsUpdatedCalled = new boolean[1];
      newsListener = new NewsAdapter() {
        @Override
        public void newsUpdated(Set<NewsEvent> events) {
          newsUpdatedCalled[0] = true;
          assertEquals(2, events.size());
          for (NewsEvent event : events) {
            IFeed feed = event.getEntity().getFeedReference().resolve();

            /* This should be enough to verify that the news is fully activated */
            assertNotNull(feed.getId());
            assertNotNull(feed.getNews());
            assertNotNull(feed.getNews().get(0));
          }
        }
      };
      Owl.getListenerService().addNewsListener(newsListener);
      fAppLayer.setNewsState(newsList, INews.State.READ, true, false);
      assertEquals(true, newsUpdatedCalled[0]);
    } finally {
      if (newsListener != null)
        Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests that calling ApplicationLayerImpl#saveFeed(MergeResult) does not
   * cause news to be lost in the feed. See bug #276.
   *
   * @throws Exception
   */
  @Test
  public void testSaveFeedNewsLost() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
    INews news0 = fFactory.createNews(null, feed, new Date());
    news0.setLink(new URI("http://www.feed.com/news1"));
    INews news1 = fFactory.createNews(null, feed, new Date());
    URI news1Link = new URI("http://www.feed.com/news2");
    news1.setLink(news1Link);
    fDao.saveFeed(feed);
    long feedId = feed.getId();
    INews news = feed.getNews().get(0);
    feed.removeNews(news);
    MergeResult mergeResult = new MergeResult();
    mergeResult.addRemovedObject(news);
    mergeResult.addUpdatedObject(feed);
    TestUtils.saveFeed(mergeResult);
    feed = null;
    news0 = null;
    news1 = null;
    mergeResult = null;
    news = null;
    System.gc();
    feed = fDao.loadFeed(feedId);
    assertEquals(1, feed.getNews().size());
    assertEquals(news1Link, feed.getNews().get(0).getLink());
  }

  /**
   * Tests that {@link IApplicationLayer#saveNews(List)} sets the current and
   * old state correctly when firing a newsUpdated event.
   *
   * @throws Exception
   */
  @Test
  public void testSaveNewsSetsCurrentAndOldState() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle("News Title #1");
    news.setLink(new URI("http://www.link.com"));
    news.setState(INews.State.UNREAD);

    feed = fDao.saveFeed(feed);

    final INews savedNews = feed.getNews().get(0);
    savedNews.setTitle("News Title Updated #1");

    List<INews> newsList = new ArrayList<INews>();
    newsList.add(savedNews);

    NewsListener newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(1, events.size());
        NewsEvent event = events.iterator().next();
        assertEquals(true, event.getEntity().equals(savedNews));
        assertEquals(State.UNREAD, event.getOldNews().getState());
        assertEquals(State.UNREAD, event.getEntity().getState());
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    try {
      newsList = fAppLayer.saveNews(newsList);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
    newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(1, events.size());
        NewsEvent event = events.iterator().next();
        assertEquals(savedNews.getId().longValue(), event.getEntity().getId());
        assertEquals(State.UNREAD, event.getOldNews().getState());
        assertEquals(State.UPDATED, event.getEntity().getState());
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    newsList.get(0).setState(State.UPDATED);
    try {
      fAppLayer.saveNews(newsList);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests {@link IApplicationLayer#loadFeedReference(URI)}.
   *
   * @throws Exception
   */
  @Test
  public void testLoadFeedReference() throws Exception {
    URI feed1Url = new URI("http://www.feed1.com");
    IFeed feed1 = new Feed(feed1Url);
    feed1 = fDao.saveFeed(feed1);

    URI feed2Url = new URI("http://www.feed2.com");
    IFeed feed2 = new Feed(feed2Url);
    feed2 = fDao.saveFeed(feed2);

    assertEquals(feed1.getId().longValue(), fAppLayer.loadFeedReference(feed1Url).getId());

    assertEquals(feed2.getId().longValue(), fAppLayer.loadFeedReference(feed2Url).getId());
  }

  /**
   * Tests {@link IApplicationLayer#loadFeed(URI)}.
   *
   * @throws Exception
   */
  @Test
  public void testLoadFeed() throws Exception {
    URI feed1Url = new URI("http://www.feed1.com");
    IFeed feed1 = new Feed(feed1Url);
    feed1 = fDao.saveFeed(feed1);

    URI feed2Url = new URI("http://www.feed2.com");
    IFeed feed2 = new Feed(feed2Url);
    feed2 = fDao.saveFeed(feed2);

    assertEquals(feed1, fAppLayer.loadFeed(feed1Url));
    assertEquals(feed2, fAppLayer.loadFeed(feed2Url));
  }

  /**
   * Tests {@link IApplicationLayer#loadFeed(URI)}.
   *
   * @throws Exception
   */
  @Test
  public void testLoadFeedActivation() throws Exception {
    URI feed1Url = new URI("http://www.feed1.com");
    IFeed feed1 = fFactory.createFeed(null, feed1Url);
    fFactory.createNews(null, feed1, new Date());
    feed1 = fDao.saveFeed(feed1);
    long newsId = feed1.getNews().get(0).getId();
    feed1 = null;
    System.gc();
    feed1 = fAppLayer.loadFeed(feed1Url);
    assertNotNull(feed1);
    assertEquals(1, feed1.getNews().size());
    assertEquals(newsId, feed1.getNews().get(0).getId());
  }

  /**
   * Tests {@link IApplicationLayer#saveNews(List)}.
   *
   * @throws Exception
   */
  @Test
  public void testSaveNews() throws Exception {
    IFeed feed1 = new Feed(new URI("http://www.feed1.com"));
    INews news11 = fFactory.createNews(null, feed1, new Date());
    news11.setLink(new URI("http://www.link11.com"));
    INews news12 = fFactory.createNews(null, feed1, new Date());
    news12.setLink(new URI("http://www.link12.com"));
    feed1 = fDao.saveFeed(feed1);

    IFeed feed2 = new Feed(new URI("http://www.feed2.com"));
    INews news21 = fFactory.createNews(null, feed2, new Date());
    news21.setLink(new URI("http://www.link21.com"));
    INews news22 = fFactory.createNews(null, feed2, new Date());
    news22.setLink(new URI("http://www.link22.com"));
    feed2 = fDao.saveFeed(feed2);

    final List<INews> newsList = new ArrayList<INews>();

    for (INews news : feed1.getNews())
      newsList.add(news);

    for (INews news : feed2.getNews())
      newsList.add(news);

    for (INews news : newsList) {
      news.setComments("updated comments");
    }

    final boolean newsUpdatedCalled[] = new boolean[1];
    NewsListener newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(newsUpdatedCalled[0], false);
        newsUpdatedCalled[0] = true;
        assertEquals(newsList.size(), events.size());
        for (NewsEvent event : events) {
          assertEquals(true, event.isRoot());
          boolean newsFound = false;
          for (INews news : newsList) {
            if (event.getEntity().equals(news)) {
              newsFound = true;
              break;
            }
          }
          assertEquals(true, newsFound);
        }
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    try {
      List<INews> savedNews = fAppLayer.saveNews(newsList);
      assertEquals(newsList, savedNews);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
    assertEquals(true, newsUpdatedCalled[0]);
  }

  /**
   * Test {@link IApplicationLayer#reparent(List, List)}
   *
   * @throws Exception
   */
  @Test
  public void testReparentFolderAndMark() throws Exception {
    FolderListener folderListener = null;
    BookMarkListener bookMarkListener = null;
    SearchMarkListener searchMarkListener = null;
    try {
      /* Add */
      final IFolder oldMarkParent = fFactory.createFolder(null, null, "Old parent");
      final IBookMark bookMark = fFactory.createBookMark(null, oldMarkParent, new FeedLinkReference(new URI("http://www.link.com")), "bookmark");
      final ISearchMark searchMark = fFactory.createSearchMark(null, oldMarkParent, "searchmark");
      fDao.saveFolder(oldMarkParent);

      final IFolder newMarkParent = fFactory.createFolder(null, null, "New parent");
      fFactory.createFolder(null, newMarkParent, "New parent child");
      fDao.saveFolder(newMarkParent);

      /* Add */
      final IFolder oldFolderParent = fFactory.createFolder(null, null, "Old parent");
      final IFolder folder = fFactory.createFolder(null, oldFolderParent, "Folder");
      fDao.saveFolder(oldFolderParent);

      final IFolder newFolderParent = fFactory.createFolder(null, null, "New parent");
      fFactory.createFolder(null, newFolderParent, "New parent child");
      fDao.saveFolder(newFolderParent);

      final boolean[] folderUpdateEventOccurred = new boolean[1];
      folderListener = new FolderListener() {
        public void folderAdded(Set<FolderEvent> events) {
          fail("Unexpected event");
        }

        public void folderDeleted(Set<FolderEvent> events) {
          fail("Unexpected event");
        }

        public void folderUpdated(Set<FolderEvent> events) {
          folderUpdateEventOccurred[0] = true;
          assertEquals(7, events.size());
          boolean foundFolder = false;
          for (FolderEvent event : events) {
            if (event.getEntity().equals(folder)) {
              foundFolder = true;
              assertTrue("Expected this Event to be Root Event", event.isRoot());
              assertEquals(oldFolderParent, event.getOldParent());
              assertEquals(newFolderParent, event.getEntity().getParent());
            } else
              assertFalse("Expected this Event to be NO Root Event", event.isRoot());
          }
          assertTrue("No event was issued for folder", foundFolder);
        }
      };
      final boolean[] bookMarkUpdateEventOccurred = new boolean[1];
      bookMarkListener = new BookMarkListener() {
        public void bookMarkAdded(Set<BookMarkEvent> events) {
          fail("Unexpected event");
        }

        public void bookMarkDeleted(Set<BookMarkEvent> events) {
          fail("Unexpected event");
        }

        public void bookMarkUpdated(Set<BookMarkEvent> events) {
          bookMarkUpdateEventOccurred[0] = true;
          assertEquals(1, events.size());
          BookMarkEvent event = events.iterator().next();
          assertEquals(bookMark, event.getEntity());
          assertTrue("Expected this Event to be Root Event", event.isRoot());
          assertEquals(oldMarkParent, event.getOldParent());
          assertEquals(newMarkParent, event.getEntity().getFolder());
        }
      };

      final boolean[] searchMarkUpdateEventOccurred = new boolean[1];
      searchMarkListener = new SearchMarkListener() {
        public void searchMarkAdded(Set<SearchMarkEvent> events) {
          fail("Unexpected event");
        }

        public void searchMarkDeleted(Set<SearchMarkEvent> events) {
          fail("Unexpected event");
        }

        public void searchMarkUpdated(Set<SearchMarkEvent> events) {
          searchMarkUpdateEventOccurred[0] = true;
          assertEquals(1, events.size());
          SearchMarkEvent event = events.iterator().next();
          assertEquals(searchMark, event.getEntity());
          assertTrue("Expected this Event to be Root Event", event.isRoot());
          assertEquals(oldMarkParent, event.getOldParent());
          assertEquals(newMarkParent, event.getEntity().getFolder());
        }
      };

      Owl.getListenerService().addFolderListener(folderListener);
      Owl.getListenerService().addBookMarkListener(bookMarkListener);
      Owl.getListenerService().addSearchMarkListener(searchMarkListener);

      ReparentInfo<IFolder, IFolder> folderInfo = new ReparentInfo<IFolder, IFolder>(folder, newFolderParent, null, null);
      List<ReparentInfo<IFolder, IFolder>> folderInfos = Collections.singletonList(folderInfo);

      List<ReparentInfo<IMark, IFolder>> markInfos = new ArrayList<ReparentInfo<IMark, IFolder>>();
      markInfos.add(new ReparentInfo<IMark, IFolder>(bookMark, newMarkParent, null, null));
      markInfos.add(new ReparentInfo<IMark, IFolder>(searchMark, newMarkParent, null, null));
      fAppLayer.reparent(folderInfos, markInfos);

      /* Asserts Follow */

      /* Folder reparenting */
      assertFalse(oldFolderParent.getFolders().contains(folder));
      assertEquals(0, oldFolderParent.getFolders().size());
      assertTrue(newFolderParent.getFolders().contains(folder));
      assertEquals(newFolderParent, folder.getParent());
      assertEquals(2, newFolderParent.getFolders().size());

      /* Marks reparenting */
      assertFalse(oldMarkParent.getFolders().contains(bookMark));
      assertFalse(oldMarkParent.getFolders().contains(searchMark));
      assertEquals(0, oldMarkParent.getMarks().size());
      assertTrue(newMarkParent.getMarks().contains(bookMark));
      assertTrue(newMarkParent.getMarks().contains(searchMark));
      assertEquals(newMarkParent, bookMark.getFolder());
      assertEquals(newMarkParent, searchMark.getFolder());
      assertEquals(2, newMarkParent.getMarks().size());

      /* Events fired */
      assertTrue("Missing folderUpdated Event", folderUpdateEventOccurred[0]);
      assertTrue("Missing bookMarkUpdated Event", bookMarkUpdateEventOccurred[0]);
      assertTrue("Missing searchMarkUpdated Event", searchMarkUpdateEventOccurred[0]);

      Owl.getListenerService().removeFolderListener(folderListener);
      Owl.getListenerService().removeBookMarkListener(bookMarkListener);
      Owl.getListenerService().removeSearchMarkListener(searchMarkListener);
    } finally {
      /* Cleanup */
      if (folderListener != null)
        Owl.getListenerService().removeFolderListener(folderListener);
      if (bookMarkListener != null)
        Owl.getListenerService().removeBookMarkListener(bookMarkListener);
      if (searchMarkListener != null)
        Owl.getListenerService().removeSearchMarkListener(searchMarkListener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadBookMarks() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.myfeed.com"));
    fDao.saveFeed(feed);
    FeedLinkReference feedLinkRef = new FeedLinkReference(feed.getLink());

    List<IBookMark> emptyBookmarks = fAppLayer.loadBookMarks(feedLinkRef);
    assertEquals(0, emptyBookmarks.size());

    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    FolderReference root1Ref = new FolderReference(fDao.saveFolder(root1).getId());

    IFolder childOfRoot1 = fFactory.createFolder(null, root1Ref.resolve(), "Child of Root 1");
    FolderReference childOfRoot1Ref = new FolderReference(fDao.saveFolder(childOfRoot1).getId());

    IBookMark bookmark1 = fFactory.createBookMark(null, root1Ref.resolve(), new FeedLinkReference(feed.getLink()), "Bookmark 1");
    IBookMark bookmark2 = fFactory.createBookMark(null, root1Ref.resolve(), new FeedLinkReference(feed.getLink()), "Bookmark 2");
    IBookMark bookmark3 = fFactory.createBookMark(null, childOfRoot1Ref.resolve(), new FeedLinkReference(feed.getLink()), "Bookmark 3");

    BookMarkReference bookmarkRef1 = new BookMarkReference(fDao.saveBookMark(bookmark1).getId());
    BookMarkReference bookmarkRef2 = new BookMarkReference(fDao.saveBookMark(bookmark2).getId());
    BookMarkReference bookmarkRef3 = new BookMarkReference(fDao.saveBookMark(bookmark3).getId());

    List<IBookMark> filledBookmarks = fAppLayer.loadBookMarks(feedLinkRef);
    assertEquals(3, filledBookmarks.size());
    for (IBookMark mark : filledBookmarks) {
      if (bookmarkRef1.resolve().equals(mark))
        assertEquals(bookmark1.getName(), mark.getName());
      else if (bookmarkRef2.resolve().equals(mark))
        assertEquals(bookmark2.getName(), mark.getName());
      else if (bookmarkRef3.resolve().equals(mark))
        assertEquals(bookmark3.getName(), mark.getName());
      else
        fail();
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadBookMarksActivation() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.myfeed.com"));
    FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());
    long feedId = feedRef.getId();
    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    final String folderName = root1.getName();
    fFactory.createBookMark(null, root1, new FeedLinkReference(feed.getLink()), "Bookmark 1");
    feedRef = null;
    feed = null;
    fDao.saveFolder(root1);
    root1 = null;
    System.gc();

    feed = fDao.loadFeed(feedId);
    FeedLinkReference feedLinkRef = new FeedLinkReference(feed.getLink());
    List<IBookMark> marks = fAppLayer.loadBookMarks(feedLinkRef);
    assertEquals(1, marks.size());
    assertEquals(folderName, marks.get(0).getFolder().getName());
    marks = null;
    System.gc();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadAllBookMarks() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.myfeed.com"));
    fDao.saveFeed(feed);

    List<IBookMark> emptyBookmarks = fAppLayer.loadAllBookMarks(false);
    emptyBookmarks = fAppLayer.loadAllBookMarks(true);
    assertEquals(0, emptyBookmarks.size());

    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    root1 = fDao.saveFolder(root1);

    IFolder childOfRoot1 = fFactory.createFolder(null, root1, "Child of Root 1");
    childOfRoot1 = fDao.saveFolder(childOfRoot1);

    IBookMark bookmark1 = fFactory.createBookMark(null, root1, new FeedLinkReference(feed.getLink()), "Bookmark 1");
    IBookMark bookmark2 = fFactory.createBookMark(null, root1, new FeedLinkReference(feed.getLink()), "Bookmark 2");
    IBookMark bookmark3 = fFactory.createBookMark(null, childOfRoot1, new FeedLinkReference(feed.getLink()), "Bookmark 3");

    BookMarkReference bookmarkRef1 = new BookMarkReference(fDao.saveBookMark(bookmark1).getId());
    BookMarkReference bookmarkRef2 = new BookMarkReference(fDao.saveBookMark(bookmark2).getId());
    BookMarkReference bookmarkRef3 = new BookMarkReference(fDao.saveBookMark(bookmark3).getId());

    List<IBookMark> filledBookmarks = fAppLayer.loadAllBookMarks(true);
    assertEquals(3, filledBookmarks.size());

    filledBookmarks = fAppLayer.loadAllBookMarks(false);
    assertEquals(3, filledBookmarks.size());

    for (IBookMark mark : filledBookmarks) {
      if (bookmarkRef1.resolve().equals(mark))
        assertEquals(bookmark1.getName(), mark.getName());
      else if (bookmarkRef2.resolve().equals(mark))
        assertEquals(bookmark2.getName(), mark.getName());
      else if (bookmarkRef3.resolve().equals(mark))
        assertEquals(bookmark3.getName(), mark.getName());
      else
        fail();
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadAllBookMarksActivation() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.myfeed.com"));
    fDao.saveFeed(feed);

    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    final String folderName = root1.getName();
    fFactory.createBookMark(null, root1, new FeedLinkReference(feed.getLink()), "Bookmark 1");
    feed = null;
    fDao.saveFolder(root1);
    root1 = null;
    System.gc();

    List<IBookMark> marks = fAppLayer.loadAllBookMarks(true);
    assertEquals(1, marks.size());
    assertEquals(folderName, marks.get(0).getFolder().getName());
    marks = null;
    System.gc();

    marks = fAppLayer.loadAllBookMarks(false);
    assertEquals(1, marks.size());
    //TODO Using an activation depth of 1 seems to be buggy. Using 2 for now
    //    assertNull(marks.get(0).getFolder().getName());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadLabels() throws Exception {
    ILabel label1 = fFactory.createLabel(null, "Important");
    label1.setColor("159,63,63");
    fDao.saveLabel(label1);

    ILabel label2 = fFactory.createLabel(null, "Important");
    label2.setColor("255,153,0");
    fDao.saveLabel(label2);

    ILabel label3 = fFactory.createLabel(null, "Personal");
    label3.setColor("0,153,0");
    fDao.saveLabel(label3);

    List<ILabel> labels = fAppLayer.loadLabels();

    assertEquals(3, labels.size());
    for (ILabel label : labels) {
      if (label.equals(label1)) {
        assertEquals("Important", label.getName());
        assertEquals("159,63,63", label.getColor());
      }

      else if (label.equals(label2)) {
        assertEquals("Important", label.getName());
        assertEquals("255,153,0", label.getColor());
      }

      else if (label.equals(label3)) {
        assertEquals("Personal", label.getName());
        assertEquals("0,153,0", label.getColor());
      }
    }
  }

  /**
   *
   */
  public void testLoadLabelsActivation() {
    String colour = "159,63,63";
    ILabel label1 = fFactory.createLabel(null, "Important");
    label1.setColor(colour);
    fDao.saveLabel(label1);
    label1 = null;
    System.gc();

    List<ILabel> labels = fAppLayer.loadLabels();

    assertEquals(1, labels.size());
    assertEquals(colour, labels.get(0).getColor());
  }

  /**
   * Test the Method loadRootFolders()
   *
   * @throws Exception
   */
  @Test
  public void testLoadRootFolders() throws Exception {
    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    IFolder root2 = fFactory.createFolder(null, null, "Root 2");
    IFolder root3 = fFactory.createFolder(null, null, "Root 3");

    fFactory.createFolder(null, root1, "Child of Root 1");
    fFactory.createFolder(null, root2, "Child of Root 2");

    FolderReference root1Ref = new FolderReference(fDao.saveFolder(root1).getId());
    FolderReference root2Ref = new FolderReference(fDao.saveFolder(root2).getId());
    FolderReference root3Ref = new FolderReference(fDao.saveFolder(root3).getId());

    List<IFolder> rootFolders = fAppLayer.loadRootFolders();
    assertEquals(3, rootFolders.size());
    for (IFolder folder : rootFolders) {
      if (root1Ref.resolve().equals(folder))
        assertEquals("Root 1", folder.getName());
      else if (root2Ref.resolve().equals(folder))
        assertEquals("Root 2", folder.getName());
      else if (root3Ref.resolve().equals(folder))
        assertEquals("Root 3", folder.getName());
      else
        fail();
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLoadRootFoldersActivation() throws Exception {
    IFolder root1 = fFactory.createFolder(null, null, "Root 1");
    fFactory.createFolder(null, root1, "Child of Root 1");
    String childFolderName = root1.getFolders().get(0).getName();
    fDao.saveFolder(root1);
    root1 = null;
    System.gc();

    List<IFolder> rootFolders = fAppLayer.loadRootFolders();
    assertEquals(1, rootFolders.size());
    IFolder folder = rootFolders.get(0);
    assertEquals(1, folder.getFolders().size());
    assertEquals(childFolderName, folder.getFolders().get(0).getName());
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testSetNewsState() throws Exception {
    NewsListener newsListener = null;
    try {
      IFeed feed = Owl.getModelFactory().createFeed(null, new URI("http://www.feed.com"));
      Owl.getModelFactory().createNews(null, feed, new Date());
      Owl.getModelFactory().createNews(null, feed, new Date());
      Owl.getModelFactory().createNews(null, feed, new Date());

      Feed savedFeed = (Feed) fDao.saveFeed(feed);
      assertTrue(savedFeed.isIdentical(fDao.loadFeed(savedFeed.getId())));

      NewsReference news1 = new NewsReference(savedFeed.getNews().get(0).getId());
      NewsReference news2 = new NewsReference(savedFeed.getNews().get(1).getId());
      NewsReference news3 = new NewsReference(savedFeed.getNews().get(2).getId());

      List<INews> news = new ArrayList<INews>();
      news.add(news1.resolve());
      news.add(news2.resolve());

      assertEquals(news1.resolve().getState(), INews.State.NEW);
      assertEquals(news2.resolve().getState(), INews.State.NEW);
      assertEquals(news3.resolve().getState(), INews.State.NEW);

      newsListener = new NewsListener() {
        public void newsAdded(Set<NewsEvent> events) {
          fail("Unexpected Event");
        }

        public void newsDeleted(Set<NewsEvent> events) {
          fail("Unexpected Event");
        }

        public void newsUpdated(Set<NewsEvent> events) {
          assertEquals(2, events.size());
          for (NewsEvent event : events)
            assertEquals(true, event.isRoot());
        }
      };
      Owl.getListenerService().addNewsListener(newsListener);

      fAppLayer.setNewsState(news, INews.State.UNREAD, true, false);

      assertEquals(news1.resolve().getState(), INews.State.UNREAD);
      assertEquals(news2.resolve().getState(), INews.State.UNREAD);
      assertEquals(news3.resolve().getState(), INews.State.NEW);

      fAppLayer.setNewsState(news, INews.State.READ, true, false);

      assertEquals(news1.resolve().getState(), INews.State.READ);
      assertEquals(news2.resolve().getState(), INews.State.READ);
      assertEquals(news3.resolve().getState(), INews.State.NEW);

      fAppLayer.setNewsState(news, INews.State.DELETED, true, false);

      assertEquals(news1.resolve().getState(), INews.State.DELETED);
      assertEquals(news2.resolve().getState(), INews.State.DELETED);
      assertEquals(news3.resolve().getState(), INews.State.NEW);
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    } finally {
      if (newsListener != null)
        Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSetNewsStateOnPlanet() throws Exception {
    NewsListener newsListener = null;
    try {
      NewsService service = Controller.getDefault().getNewsService();

      IFeed feed1 = Owl.getModelFactory().createFeed(null, new URI("http://www.feed.com"));
      IFeed feed2 = Owl.getModelFactory().createFeed(null, new URI("http://www.feed2.com"));

      INews news1 = Owl.getModelFactory().createNews(null, feed1, new Date());
      news1.setLink(new URI("www.link.com"));

      INews news2 = Owl.getModelFactory().createNews(null, feed2, new Date());
      news2.setLink(new URI("www.link.com"));

      Owl.getModelFactory().createNews(null, feed1, new Date());
      Owl.getModelFactory().createNews(null, feed2, new Date());

      feed1 = fDao.saveFeed(feed1);
      feed2 = fDao.saveFeed(feed2);

      assertEquals(2, service.getUnreadCount(news1.getFeedReference()));
      assertEquals(2, service.getNewCount(news1.getFeedReference()));
      assertEquals(2, service.getUnreadCount(news2.getFeedReference()));
      assertEquals(2, service.getNewCount(news2.getFeedReference()));

      final long feed1ID = feed1.getId();
      final long feed2ID = feed2.getId();
      final long news1ID = feed1.getNews().get(0).getId();
      final long news2ID = feed2.getNews().get(0).getId();

      newsListener = new NewsListener() {
        public void newsAdded(Set<NewsEvent> events) {
          fail("Unexpected Event!");
        }

        public void newsDeleted(Set<NewsEvent> events) {
          fail("Unexpected Event!");
        }

        public void newsUpdated(Set<NewsEvent> events) {
          assertEquals(2, events.size());
          for (NewsEvent event : events) {
            INews news = event.getEntity();
            IFeed parent = news.getFeedReference().resolve();

            if (news.getId() == news1ID)
              assertEquals(feed1ID, parent.getId());
            else if (news.getId() == news2ID)
              assertEquals(feed2ID, parent.getId());
            else
              fail("Unexpected Reference in Event!");
          }
        }
      };
      Owl.getListenerService().addNewsListener(newsListener);

      fAppLayer.setNewsState(Arrays.asList(new INews[] { new NewsReference(news1ID).resolve() }), INews.State.READ, true, false);

      assertEquals(1, service.getUnreadCount(news1.getFeedReference()));
      assertEquals(1, service.getNewCount(news1.getFeedReference()));
      assertEquals(1, service.getUnreadCount(news2.getFeedReference()));
      assertEquals(1, service.getNewCount(news2.getFeedReference()));
    } finally {
      if (newsListener != null)
        Owl.getListenerService().removeNewsListener(newsListener);
    }
  }
}