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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.events.FolderAdapter;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.events.NewsAdapter;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.internal.persist.search.SearchValueType;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.INews.State;
import org.rssowl.core.model.persist.search.ISearchField;
import org.rssowl.core.model.persist.search.ISearchValueType;
import org.rssowl.core.model.persist.search.SearchSpecifier;
import org.rssowl.core.model.reference.AttachmentReference;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.CategoryReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.model.reference.PersonReference;
import org.rssowl.core.model.reference.SearchConditionReference;
import org.rssowl.core.model.reference.SearchMarkReference;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * This TestCase is for testing the Model Plugin (1 of 3).
 *
 * @author bpasero
 */
public class ModelTest1 {
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
    fFactory = Owl.getModelFactory();
    fDao = Owl.getPersistenceService().getModelDAO();
    fAppLayer = Owl.getPersistenceService().getApplicationLayer();
  }

  /**
   * Tests that no UPDATE event is issued for a type that has been deleted (and
   * as such there is also a REMOVE event). See bug #189 for more information.
   *
   * @throws Exception
   */
  @Test
  public void testNoUpdateEventWithRemoveEvent() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    fFactory.createFolder(null, folder, "Child folder #1");
    fFactory.createFolder(null, folder, "Child folder #2");
    fFactory.createFolder(null, folder, "Child folder #3");
    final IFolder savedFolder = fDao.saveFolder(folder);
    final IFolder savedChildFolder1 = savedFolder.getFolders().get(0);
    final IFolder savedChildFolder2 = savedFolder.getFolders().get(1);
    final IFolder savedChildFolder3 = savedFolder.getFolders().get(2);
    List<IFolder> foldersToRemove = new ArrayList<IFolder>();
    foldersToRemove.add(savedChildFolder1);
    foldersToRemove.add(savedChildFolder2);

    final boolean[] folderDeletedCalled = new boolean[1];
    final boolean[] folderUpdatedCalled = new boolean[1];
    FolderListener listener = new FolderAdapter() {
      @Override
      public void folderAdded(Set<FolderEvent> events) {
        fail("Unexpected folder added event");
      }

      @Override
      public void folderDeleted(Set<FolderEvent> events) {
        assertEquals(2, events.size());
        for (FolderEvent event : events) {
          IFolder folder = event.getEntity();
          if (!folder.equals(savedChildFolder1) && (!folder.equals(savedChildFolder2)))
            fail("No delete event expected for folder: " + folder.getId());

          folderDeletedCalled[0] = true;
        }
      }

      @Override
      public void folderUpdated(Set<FolderEvent> events) {
        assertEquals(2, events.size());
        for (FolderEvent event : events) {
          Long id = event.getEntity().getId();
          if (!id.equals(savedChildFolder3.getId()) && (!id.equals(savedFolder.getId())))
            fail("No update event expected for folder: " + id);

        }
        folderUpdatedCalled[0] = true;
      }
    };
    Owl.getListenerService().addFolderListener(listener);
    try {
      fAppLayer.deleteFolders(foldersToRemove);
      assertEquals(true, folderDeletedCalled[0]);
      assertEquals(true, folderUpdatedCalled[0]);
    } finally {
      Owl.getListenerService().removeFolderListener(listener);
    }
  }

  /**
   * Tests that updating a folder's property and saving it again has the desired
   * effect.
   */
  @Test
  public void testUpdateFolderProperties() {
    IFolder folder = fFactory.createFolder(null, null, "folder");
    String key = "key";
    String value = "value";
    folder.setProperty(key, value);
    fDao.saveFolder(folder);

    String newValue = "newValue";
    folder.setProperty(key, newValue);
    fDao.saveFolder(folder);

    folder = null;
    System.gc();
    folder = fAppLayer.loadRootFolders().get(0);
    assertEquals(newValue, folder.getProperty(key));
  }

  /**
   * Tests that merging a news with categories doesn't throw any exception.
   *
   * @throws Exception
   */
  @Test
  public void testNewsWithCategoriesMerge() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setLink(new URI("http://news.com"));
    fFactory.createCategory(null, news);
    INews anotherNews = fFactory.createNews(null, feed, new Date());
    anotherNews.setLink(new URI("http://anothernews.com"));
    fFactory.createCategory(null, anotherNews);
    ICategory category = fFactory.createCategory(null, anotherNews);
    category.setName("name");
    news.merge(anotherNews);
  }

  /**
   * Tests that merging a feed with categories doesn't throw any exception.
   *
   * @throws Exception
   */
  @Test
  public void testFeedWithCategoriesMerge() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
    fFactory.createCategory(null, feed);
    IFeed anotherFeed = fFactory.createFeed(null, new URI("http://www.feed.com"));
    fFactory.createCategory(null, anotherFeed);
    ICategory category = fFactory.createCategory(null, anotherFeed);
    category.setName("name");
    feed.merge(anotherFeed);
  }

  /**
   * Tests that {@link IModelDAO#saveFeed(IFeed)} sets the current and old state
   * correctly in the news when firing a newsUpdated event.
   *
   * @throws Exception
   */
  @Test
  public void testSaveFeedSetsCurrentAndOldStateInNews() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle("News Title #1");
    news.setLink(new URI("http://www.link.com"));
    news.setState(INews.State.UNREAD);

    feed = fDao.saveFeed(feed);

    final INews savedNews = feed.getNews().get(0);
    savedNews.setTitle("News Title Updated #1");

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
      feed = fDao.saveFeed(feed);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
    newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(1, events.size());
        NewsEvent event = events.iterator().next();
        assertEquals(savedNews.getId(), event.getEntity().getId());
        assertEquals(State.UNREAD, event.getOldNews().getState());
        assertEquals(State.UPDATED, event.getEntity().getState());
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    feed.getNews().get(0).setState(State.UPDATED);
    try {
      feed = fDao.saveFeed(feed);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests that {@link IModelDAO#saveNews(INews)} sets the current and old state
   * correctly when firing a newsUpdated event.
   *
   * @throws Exception
   */
  @Test
  public void testSaveNewsSetsCurrentAndOldState() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle("News Title #1");
    news.setLink(new URI("http://www.link.com"));
    news.setState(INews.State.UNREAD);

    feed = fDao.saveFeed(feed);

    INews savedNews = feed.getNews().get(0);
    final Long savedNewsId = savedNews.getId();
    savedNews.setTitle("News Title Updated #1");

    NewsListener newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(1, events.size());
        NewsEvent event = events.iterator().next();
        assertEquals(savedNewsId, event.getEntity().getId());
        assertEquals(State.UNREAD, event.getOldNews().getState());
        assertEquals(State.UNREAD, event.getEntity().getState());
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    try {
      savedNews = fDao.saveNews(savedNews);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
    newsListener = new NewsAdapter() {
      @Override
      public void newsUpdated(Set<NewsEvent> events) {
        assertEquals(1, events.size());
        NewsEvent event = events.iterator().next();
        assertEquals(savedNewsId, event.getEntity().getId());
        assertEquals(State.UNREAD, event.getOldNews().getState());
        assertEquals(State.UPDATED, event.getEntity().getState());
      }
    };
    Owl.getListenerService().addNewsListener(newsListener);
    savedNews.setState(State.UPDATED);
    try {
      fDao.saveNews(savedNews);
    } finally {
      Owl.getListenerService().removeNewsListener(newsListener);
    }
  }

  /**
   * Tests equals and hashCode for FeedLinkReference.
   *
   * @throws Exception
   */
  @Test
  public void testFeedLinkReferenceEqualsAndHashCode() throws Exception {
    String url1 = "http://url1.com";
    String url3 = "http://url3.com";

    FeedLinkReference feedRef1 = new FeedLinkReference(new URI(url1));
    FeedLinkReference feedRef2 = new FeedLinkReference(new URI(url1));

    assertEquals(feedRef1, feedRef2);
    assertEquals(feedRef1.hashCode(), feedRef2.hashCode());

    FeedLinkReference feedRef3 = new FeedLinkReference(new URI(url3));
    assertFalse(feedRef1.equals(feedRef3));
    assertFalse(feedRef1.hashCode() == feedRef3.hashCode());
  }

  /**
   * Tests {@link IFeed#getVisibleNews()} and
   * {@link IFeed#getNewsByStates(EnumSet)}
   *
   * @throws Exception
   */
  @Test
  public void testGetNewsByStatesAndGetVisibleNews() throws Exception {
    IFeed feed = createFeed("http://feed1.com");
    feed = fDao.saveFeed(feed);

    int newNewsCount = 4;
    List<INews> newNews = new ArrayList<INews>();
    for (int i = 0; i < newNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("new News: " + i);
      news.setState(State.NEW);
      newNews.add(news);
    }

    int readNewsCount = 2;
    List<INews> readNews = new ArrayList<INews>();
    for (int i = 0; i < readNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("read News: " + i);
      news.setState(State.READ);
      readNews.add(news);
    }

    int unreadNewsCount = 3;
    List<INews> unreadNews = new ArrayList<INews>();
    for (int i = 0; i < unreadNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("unread News: " + i);
      news.setState(State.UNREAD);
      unreadNews.add(news);
    }

    int updatedNewsCount = 6;
    List<INews> updatedNews = new ArrayList<INews>();
    for (int i = 0; i < updatedNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("updated News: " + i);
      news.setState(State.UPDATED);
      updatedNews.add(news);
    }

    int hiddenNewsCount = 8;
    List<INews> hiddenNews = new ArrayList<INews>();
    for (int i = 0; i < hiddenNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("hidden News: " + i);
      news.setState(State.HIDDEN);
      hiddenNews.add(news);
    }

    int deletedNewsCount = 7;
    List<INews> deletedNews = new ArrayList<INews>();
    for (int i = 0; i < deletedNewsCount; ++i) {
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("deleted News: " + i);
      news.setState(State.DELETED);
      deletedNews.add(news);
    }

    assertEquals(newNewsCount, feed.getNewsByStates(EnumSet.of(State.NEW)).size());
    int counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.NEW))) {
      INews newsItem = newNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    assertEquals(readNewsCount, feed.getNewsByStates(EnumSet.of(State.READ)).size());
    counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.READ))) {
      INews newsItem = readNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    assertEquals(unreadNewsCount, feed.getNewsByStates(EnumSet.of(State.UNREAD)).size());
    counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.UNREAD))) {
      INews newsItem = unreadNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    assertEquals(updatedNewsCount, feed.getNewsByStates(EnumSet.of(State.UPDATED)).size());
    counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.UPDATED))) {
      INews newsItem = updatedNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    assertEquals(hiddenNewsCount, feed.getNewsByStates(EnumSet.of(State.HIDDEN)).size());
    counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.HIDDEN))) {
      INews newsItem = hiddenNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    assertEquals(deletedNewsCount, feed.getNewsByStates(EnumSet.of(State.DELETED)).size());
    counter = 0;
    for (INews news : feed.getNewsByStates(EnumSet.of(State.DELETED))) {
      INews newsItem = deletedNews.get(counter++);
      assertEquals(newsItem.getTitle(), news.getTitle());
    }

    int visibleNewsCount = newNewsCount + readNewsCount + unreadNewsCount + updatedNewsCount;
    assertEquals(visibleNewsCount, feed.getVisibleNews().size());

    for (INews news : feed.getVisibleNews()) {
      boolean matchFound = false;
      for (INews newsItem : newNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;

      for (INews newsItem : readNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;

      for (INews newsItem : unreadNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;

      for (INews newsItem : updatedNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;
      fail("No match was found. A news that had the wrong state was returned");
    }

    for (INews news : feed.getNewsByStates(EnumSet.of(State.NEW, State.HIDDEN, State.DELETED))) {
      boolean matchFound = false;
      for (INews newsItem : newNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;

      for (INews newsItem : hiddenNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;

      for (INews newsItem : deletedNews) {
        if (news.getTitle().equals(newsItem.getTitle())) {
          matchFound = true;
          break;
        }
      }
      if (matchFound)
        continue;
      fail("No match was found. A news that had the wrong state was returned");
    }

  }

  /**
   * Tests that removing a INews doesn't also remove its parent feed.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveNewsWithoutFeed() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    fFactory.createNews(null, feed, new Date());
    IFeed savedFeed = fDao.saveFeed(feed);
    fDao.deleteNews(new NewsReference(feed.getNews().get(0).getId()));
    savedFeed = fDao.loadFeed(savedFeed.getId());
    assertNotNull(savedFeed);
  }

  /**
   * Tests that removing a ISearchCondition doesn't also remove its parent
   * ISearchMark.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveSearchConditionWithoutSearchMark() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    ISearchMark searchMark = fFactory.createSearchMark(null, folder, "Mark");
    ISearchField searchField = fFactory.createSearchField(0, INews.class.getName());
    fFactory.createSearchCondition(null, searchMark, searchField, SearchSpecifier.BEGINS_WITH, "Some value");
    IFolder savedFolder = fDao.saveFolder(folder);
    ISearchMark savedMark = (ISearchMark) savedFolder.getMarks().get(0);
    fDao.deleteSearchCondition(new SearchConditionReference(savedMark.getSearchConditions().get(0).getId()));
    assertNotNull(fDao.loadSearchMark(savedMark.getId()));
  }

  /**
   * Tests that removing a IPerson doesn't also remove its parent INews.
   *
   * @throws Exception
   */
  @Test
  public void testRemovePersonWithoutNews() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    feed = fDao.saveFeed(feed);
    INews news = fFactory.createNews(null, feed, new Date());
    fFactory.createPerson(null, news);
    INews savedNews = fDao.saveNews(news);
    fDao.deletePerson(new PersonReference(savedNews.getAuthor().getId()));
    savedNews = fDao.loadNews(savedNews.getId());
    assertNotNull(savedNews);
  }

  /**
   * Tests that removing a IPerson doesn't also remove its parent feed.
   *
   * @throws Exception
   */
  @Test
  public void testRemovePersonWithoutFeed() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    fFactory.createPerson(null, feed);
    IFeed savedFeed = fDao.saveFeed(feed);
    fDao.deletePerson(new PersonReference(savedFeed.getAuthor().getId()));
    savedFeed = fDao.loadFeed(savedFeed.getId());
    assertNotNull(savedFeed);
  }

  /**
   * Tests that removing a ICategory doesn't also remove its parent news.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveCategoryWithoutNews() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    feed = fDao.saveFeed(feed);
    INews news = fFactory.createNews(null, feed, new Date());
    fFactory.createCategory(null, news);
    INews savedNews = fDao.saveNews(news);
    fDao.deleteCategory(new CategoryReference(news.getCategories().get(0).getId()));
    savedNews = fDao.loadNews(savedNews.getId());
    assertNotNull(savedNews);
  }

  /**
   * Tests that removing a ICategory doesn't also remove its parent feed.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveCategoryWithoutFeed() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    fFactory.createCategory(null, feed);
    IFeed savedFeed = fDao.saveFeed(feed);
    fDao.deleteCategory(new CategoryReference(feed.getCategories().get(0).getId()));
    savedFeed = fDao.loadFeed(savedFeed.getId());
    assertNotNull(savedFeed);
  }

  /**
   * Tests that removing a child IFolder doesn't also remove its parent IFolder.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveFolderWithoutParentFolder() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    fFactory.createFolder(null, folder, "Child folder");
    IFolder savedFolder = fDao.saveFolder(folder);
    IFolder savedChildFolder = savedFolder.getFolders().get(0);
    fDao.deleteFolder(new FolderReference(savedChildFolder.getId()));
    assertNotNull(fDao.loadFolder(savedFolder.getId()));
  }

  /**
   * Tests that removing a ISearchMark doesn't also remove its parent folder.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveSearchMarkWithoutFolder() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    fFactory.createSearchMark(null, folder, "Mark");
    IFolder savedFolder = fDao.saveFolder(folder);
    ISearchMark savedMark = (ISearchMark) savedFolder.getMarks().get(0);
    fDao.deleteSearchMark(new SearchMarkReference(savedMark.getId()));
    assertNotNull(fDao.loadFolder(savedFolder.getId()));
  }

  /**
   * Tests that removing a ISearchMark causes an update event in the parent
   * folder.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveSearchUpdatesFolder() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    fFactory.createSearchMark(null, folder, "Mark");
    final IFolder savedFolder = fDao.saveFolder(folder);

    final boolean[] folderUpdatedCalled = new boolean[1];
    FolderListener folderListener = new FolderListener() {
      public void folderAdded(Set<FolderEvent> events) {
        fail("folderAdded should not be called");
      }

      public void folderDeleted(Set<FolderEvent> events) {
        fail("folderDeleted should not be called");
      }

      public void folderUpdated(Set<FolderEvent> events) {
        assertEquals(1, events.size());
        assertEquals(true, events.iterator().next().getEntity().equals(savedFolder));
        folderUpdatedCalled[0] = true;
      }
    };
    Owl.getListenerService().addFolderListener(folderListener);
    try {
      ISearchMark savedMark = (ISearchMark) savedFolder.getMarks().get(0);
      fDao.deleteSearchMark(new SearchMarkReference(savedMark.getId()));
      assertTrue("folderUpdated was not called", folderUpdatedCalled[0]);
    } finally {
      Owl.getListenerService().removeFolderListener(folderListener);
    }
  }

  /**
   * Tests that removing a IBookMark doesn't also remove its parent folder.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveBookMarkWithoutFolder() throws Exception {
    IFolder folder = fFactory.createFolder(null, null, "Folder");
    IFeed feed = createFeed("http://www.someurl.com");
    feed = fDao.saveFeed(feed);
    fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark");
    IFolder savedFolder = fDao.saveFolder(folder);
    IBookMark savedMark = (IBookMark) savedFolder.getMarks().get(0);
    fDao.deleteBookMark(new BookMarkReference(savedMark.getId()));
    assertNotNull(fDao.loadFolder(savedFolder.getId()));
  }

  /**
   * Tests that removing a IAttachment doesn't also remove its parent news.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveAttachmentWithoutNews() throws Exception {
    IFeed feed = createFeed("http://www.rssowl.org");
    feed = fDao.saveFeed(feed);
    INews news = fFactory.createNews(null, feed, new Date());
    fFactory.createAttachment(null, news);
    INews savedNews = fDao.saveNews(news);
    fDao.deleteAttachment(new AttachmentReference(news.getAttachments().get(0).getId()));
    savedNews = fDao.loadNews(savedNews.getId());
    assertNotNull(savedNews);
  }

  /**
   * Test removing a IBookmark when it's the only parent of a IFeed. It should
   * cascade in that case. And test removing it when there are two IBookMarks
   * that reference the same IFeed. It should not cascade in that case.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveBookMarkAndFeed() throws Exception {
    {
      IFolder folder = fFactory.createFolder(null, null, "Folder");
      IFeed feed = createFeed("http://www.someurl.com");
      feed = fDao.saveFeed(feed);
      fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark");
      IFolder savedFolder = fDao.saveFolder(folder);
      IBookMark savedMark = (IBookMark) savedFolder.getMarks().get(0);
      IFeed savedFeed = savedMark.getFeedLinkReference().resolve();
      fDao.deleteBookMark(new BookMarkReference(savedMark.getId()));
      assertNull("Feed must also be deleted since no more bookmarks reference it", fDao.loadFeed(savedFeed.getId()));
    }
    {
      IFolder folder = fFactory.createFolder(null, null, "AnotherFolder");
      IFeed feed = createFeed("http://www.anotherurl.com");
      feed = fDao.saveFeed(feed);
      fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark1");
      fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "Mark2");
      IFolder savedFolder = fDao.saveFolder(folder);
      IBookMark savedMark1 = (IBookMark) savedFolder.getMarks().get(0);
      IBookMark savedMark2 = (IBookMark) savedFolder.getMarks().get(1);
      if (savedMark1.getName().equals("Mark2")) {
        IBookMark tempMark = savedMark1;
        savedMark1 = savedMark2;
        savedMark2 = tempMark;
      }
      IFeed savedFeed = savedMark1.getFeedLinkReference().resolve();
      fDao.deleteBookMark(new BookMarkReference(savedMark1.getId()));
      assertNotNull("Feed must not be deleted since one bookmark references it", fDao.loadFeed(savedFeed.getId()));
      fDao.deleteBookMark(new BookMarkReference(savedMark2.getId()));
      assertNull("Feed must also be deleted since no more bookmarks reference it", fDao.loadFeed(savedFeed.getId()));
    }
  }

  /**
   * Test the equals method in model types.
   *
   * @throws Exception
   */
  @Test
  public void testEquals() throws Exception {

    /* IExtendableType */
    IPersistable type1 = fFactory.createLabel(null, "name");
    IPersistable type2 = fFactory.createLabel(null, "name");

    IPersistable type3 = fFactory.createLabel(Long.valueOf(1), "name");
    IPersistable type4 = fFactory.createLabel(Long.valueOf(1), "name");

    IPersistable type5 = fFactory.createLabel(Long.valueOf(1), "name");
    IPersistable type6 = fFactory.createLabel(Long.valueOf(2), "name");

    assertFalse(type1.equals(type2));
    assertTrue(type3.equals(type4));
    assertFalse(type5.equals(type6));

    /* ISearchField */
    ISearchField fieldLabelName1 = fFactory.createSearchField(ILabel.NAME, ILabel.class.getName());
    ISearchField fieldLabelName2 = fFactory.createSearchField(ILabel.NAME, ILabel.class.getName());
    ISearchField fieldLabelAllFields = fFactory.createSearchField(IEntity.ALL_FIELDS, ILabel.class.getName());
    ISearchField fieldNewsTitle = fFactory.createSearchField(INews.TITLE, INews.class.getName());

    assertTrue(fieldLabelName1.equals(fieldLabelName2));
    assertFalse(fieldLabelName1.equals(fieldLabelAllFields));
    assertFalse(fieldLabelName1.equals(fieldNewsTitle));

    /* ISearchValueType */
    SearchValueType valueTypeString1 = new SearchValueType(ISearchValueType.STRING);
    SearchValueType valueTypeString2 = new SearchValueType(ISearchValueType.STRING);
    SearchValueType valueTypeDate = new SearchValueType(ISearchValueType.DATE);

    SearchValueType valueTypeEnum1 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo", "Bar" })));
    SearchValueType valueTypeEnum2 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo", "Bar" })));
    SearchValueType valueTypeEnum3 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo" })));

    assertTrue(valueTypeString1.equals(valueTypeString2));
    assertFalse(valueTypeString1.equals(valueTypeDate));

    assertTrue(valueTypeEnum1.equals(valueTypeEnum2));
    assertFalse(valueTypeEnum1.equals(valueTypeEnum3));
  }

  /**
   * Test the hashCode method in model types.
   *
   * @throws Exception
   */
  @Test
  public void testHashCode() throws Exception {

    /* ExtendableType */
    IPersistable type1 = fFactory.createLabel(null, "name");
    IPersistable type2 = fFactory.createLabel(null, "name");

    IPersistable type3 = fFactory.createLabel(Long.valueOf(1), "name");
    IPersistable type4 = fFactory.createLabel(Long.valueOf(1), "name");

    IPersistable type5 = fFactory.createLabel(Long.valueOf(1), "name");
    IPersistable type6 = fFactory.createLabel(Long.valueOf(2), "name");

    assertFalse(type1.hashCode() == type2.hashCode());
    assertTrue(type3.hashCode() == type4.hashCode());
    assertFalse(type5.hashCode() == type6.hashCode());

    /* ISearchField */
    ISearchField fieldLabelName1 = fFactory.createSearchField(ILabel.NAME, ILabel.class.getName());
    ISearchField fieldLabelName2 = fFactory.createSearchField(ILabel.NAME, ILabel.class.getName());
    ISearchField fieldLabelAllFields = fFactory.createSearchField(IEntity.ALL_FIELDS, ILabel.class.getName());
    ISearchField fieldNewsTitle = fFactory.createSearchField(INews.TITLE, INews.class.getName());

    assertTrue(fieldLabelName1.hashCode() == fieldLabelName2.hashCode());
    assertFalse(fieldLabelName1.hashCode() == fieldLabelAllFields.hashCode());
    assertFalse(fieldLabelName1.hashCode() == fieldNewsTitle.hashCode());

    /* ISearchValueType */
    SearchValueType valueTypeString1 = new SearchValueType(ISearchValueType.STRING);
    SearchValueType valueTypeString2 = new SearchValueType(ISearchValueType.STRING);
    SearchValueType valueTypeDate = new SearchValueType(ISearchValueType.DATE);

    SearchValueType valueTypeEnum1 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo", "Bar" })));
    SearchValueType valueTypeEnum2 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo", "Bar" })));
    SearchValueType valueTypeEnum3 = new SearchValueType(new ArrayList<String>(Arrays.asList(new String[] { "Foo" })));

    assertTrue(valueTypeString1.hashCode() == valueTypeString2.hashCode());
    assertFalse(valueTypeString1.hashCode() == valueTypeDate.hashCode());

    assertTrue(valueTypeEnum1.hashCode() == valueTypeEnum2.hashCode());
    assertFalse(valueTypeEnum1.hashCode() == valueTypeEnum3.hashCode());
  }

  private IFeed createFeed(String url) throws URISyntaxException {
    return fFactory.createFeed(null, new URI(url));
  }
}