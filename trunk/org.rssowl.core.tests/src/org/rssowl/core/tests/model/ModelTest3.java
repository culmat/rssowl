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
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.AttachmentAdapter;
import org.rssowl.core.model.events.AttachmentEvent;
import org.rssowl.core.model.events.AttachmentListener;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.BookMarkListener;
import org.rssowl.core.model.events.CategoryEvent;
import org.rssowl.core.model.events.CategoryListener;
import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.FeedListener;
import org.rssowl.core.model.events.FolderAdapter;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.events.LabelEvent;
import org.rssowl.core.model.events.LabelListener;
import org.rssowl.core.model.events.NewsAdapter;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.events.PersonEvent;
import org.rssowl.core.model.events.PersonListener;
import org.rssowl.core.model.events.SearchConditionEvent;
import org.rssowl.core.model.events.SearchConditionListener;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.events.SearchMarkListener;
import org.rssowl.core.model.internal.types.MergeResult;
import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.INews.State;
import org.rssowl.core.model.reference.AttachmentReference;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.CategoryReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.reference.LabelReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.model.reference.PersonReference;
import org.rssowl.core.model.reference.SearchConditionReference;
import org.rssowl.core.model.reference.SearchMarkReference;
import org.rssowl.core.model.search.ISearchCondition;
import org.rssowl.core.model.search.ISearchField;
import org.rssowl.core.model.search.SearchSpecifier;
import org.rssowl.core.tests.TestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This TestCase is for testing the Model Plugin (3 of 3).
 *
 * @author bpasero
 */
@SuppressWarnings("nls")
public class ModelTest3 {
  private IModelTypesFactory fFactory;
  private IModelDAO fDao;
  private NewsModel fModel;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    NewsModel.getDefault().getPersistenceLayer().recreateSchema();
    NewsModel.getDefault().getPersistenceLayer().getModelSearch().shutdown();
    fFactory = NewsModel.getDefault().getTypesFactory();
    fDao = NewsModel.getDefault().getPersistenceLayer().getModelDAO();
    fModel = NewsModel.getDefault();
  }

  private IFeed createFeed(String url) throws URISyntaxException {
    return fFactory.createFeed(null, new URI(url));
  }

  /**
   * Tests {@link INews#merge(INews)}. Particularly: - If the state of both
   * news is different, it's changed to the new state with one exception: if the
   * second News is in the NEW state, the state of the first news won't be
   * changed.
   *
   * @throws Exception
   */
  @Test
  public void testNewsStateMerge() throws Exception {
    /* Initial Add News */
    String url = "http://www.feed-case1.com";
    IFeed feed = createFeed(url);
    final String newsTitle = "News Title Case_1";
    fFactory.createNews(null, feed, new Date()).setTitle(newsTitle);
    FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

    /*
     * Recreate the feed because the existing one got changed when it was saved
     */
    feed = createFeed(url);

    /* a) Different publish date and news to be merged is in NEW state */
    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());

    IFeed mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

    /* b) Different publish date and news to be merged is in DELETED state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.DELETED);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.DELETED, feedRef.resolve().getNews().get(0).getState());

    /* c) Different publish date and news to be merged is in HIDDEN state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.HIDDEN);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.HIDDEN, feedRef.resolve().getNews().get(0).getState());

    /* d) Different publish date and news to be merged is in READ state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.READ);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());

    /* e) Different publish date and news to be merged is in UNREAD state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.UNREAD);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.UNREAD, feedRef.resolve().getNews().get(0).getState());

    /* f) Different publish date and news to be merged is in UPDATED state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.UPDATED);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());

    /*
     * g) Different publish date and news to be merged is in NEW state, but
     * existing News is in UPDATED state
     */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.NEW);

    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());
  }

  /**
   * Tests {@link INews#merge(INews)}. Particularly: - The state does not
   * change to UPDATED if both news have the same state and it is NEW, DELETED
   * or HIDDEN.
   *
   * @throws Exception
   */
  @Test
  public void testNewsStateMerge2() throws Exception {
    /* Initial Add News */
    String url = "http://www.feed-case1.com";
    IFeed feed = createFeed(url);
    final String newsTitle = "News Title Case_1";
    fFactory.createNews(null, feed, new Date()).setTitle(newsTitle);
    FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

    /*
     * Recreate the feed because the existing one got changed when it was saved
     */
    feed = createFeed(url);

    /* a) Different publish date and both news are in NEW state */
    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());

    IFeed mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

    /* b) Different publish date and both news are in DELETED state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.DELETED);

    IFeed dbFeed = feedRef.resolve();
    dbFeed.getNews().get(0).setState(State.DELETED);
    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.DELETED, feedRef.resolve().getNews().get(0).getState());

    /* c) Different publish date and both news are in HIDDEN state */
    feed = createFeed(url);
    news = fFactory.createNews(null, feed, new Date());
    news.setTitle(newsTitle);
    news.setPublishDate(new Date());
    news.setState(State.HIDDEN);

    dbFeed = feedRef.resolve();
    dbFeed.getNews().get(0).setState(State.HIDDEN);
    mergedFeed = feedRef.resolve();
    mergedFeed.merge(feed);
    feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
    assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
    assertEquals("Existing News State changed unexpectedly!", INews.State.HIDDEN, feedRef.resolve().getNews().get(0).getState());
  }

  /**
   * Test all cases of a News being added to the DB, which is already present.
   * Check all possible combinatios that include description.
   *
   * @throws Exception
   */
  @Test
  public void testNewsAddedUpdatedWithDescription() throws Exception {
    String description = "Initial description";
    /* News with Title and Description */
    {
      /* Initial Add News */
      long time = System.currentTimeMillis();
      String url = "http://www.feed-case4.com";
      IFeed feed = createFeed(url);
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_4");
      news.setPublishDate(new Date(time));
      news.setDescription(description);
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      /* Mark News Read */
      news = feedRef.resolve().getNews().get(0);
      news.setState(INews.State.READ);
      fDao.saveNews(news);

      /* b) Add the same News with updated Description */
      feed = createFeed(url);
      news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_4");
      news.setPublishDate(new Date(time));
      news.setDescription(description + "updated");
      IFeed mergedFeed = feedRef.resolve();
      mergedFeed.merge(feed);
      feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
      assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
      assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());
    }

    /* News with Title, URL and Description */
    {
      /* Initial Add News */
      long time = System.currentTimeMillis();
      String url = "http://www.feed-case5.com";
      IFeed feed = createFeed(url);
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_5");
      news.setLink(new URI("http://www.news-case5.com/index.html"));
      news.setPublishDate(new Date(time));
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      /* Mark News Read */
      news = feedRef.resolve().getNews().get(0);
      news.setState(INews.State.READ);
      fDao.saveNews(news);

      /* c) Add the same News with updated description */
      feed = createFeed(url);
      news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_5 Updated");
      news.setLink(new URI("http://www.news-case5.com/index.html"));
      news.setPublishDate(new Date(time));
      news.setDescription(description + "updated#2");
      IFeed mergedFeed = feedRef.resolve();
      mergedFeed.merge(feed);
      feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
      assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
      assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());
    }

    /* News with Title, Guid and Description */
    {
      /* Initial Add News */
      long time = System.currentTimeMillis();
      String url = "http://www.feed-case6.com";
      IFeed feed = createFeed(url);
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_6");
      news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
      news.setPublishDate(new Date(time));
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      /* Mark News Read */
      news = feedRef.resolve().getNews().get(0);
      news.setState(INews.State.READ);
      fDao.saveNews(news);

      /* d) Add the same News with updated description */
      feed = createFeed(url);
      news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_6");
      news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
      news.setDescription(description + "updated#3");
      IFeed mergedFeed = feedRef.resolve();
      mergedFeed.merge(feed);
      feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
      assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
      assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());
    }

    /* News with Title, URL, Guid and Description */
    {
      /* Initial Add News */
      long time = System.currentTimeMillis();
      String url = "http://www.feed-case8.com";
      IFeed feed = createFeed(url);
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_8");
      news.setLink(new URI("http://www.news-case8.com/index.html"));
      news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
      news.setPublishDate(new Date(time));
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      /* Mark News Read */
      news = feedRef.resolve().getNews().get(0);
      news.setState(INews.State.READ);
      fDao.saveNews(news);

      /* d) Add the same News with updated Publish Date */
      feed = createFeed(url);
      news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News Title Case_8");
      news.setLink(new URI("http://www.news-case8.com/index.html"));
      news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
      news.setPublishDate(new Date(System.currentTimeMillis() + 1000));
      news.setDescription(description + "updated#4");
      IFeed mergedFeed = feedRef.resolve();
      mergedFeed.merge(feed);
      feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
      assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
      assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());
    }
  }

  /**
   * Test all cases of a News being added to the DB, which is already present.
   * Check all possible combinatios of a News containing Title, URL, Guid,
   * PublishDate.
   *
   * @throws Exception
   */
  @Test
  public void testNewsAddedUpdated() throws Exception {
    try {

      /* Case 1: News with Title */
      {
        /* Initial Add News */
        String url = "http://www.feed-case1.com";
        IFeed feed = createFeed(url);
        fFactory.createNews(null, feed, new Date()).setTitle("News Title Case_1");
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /*
         * Recreate the feed because the existing one got changed when it was
         * saved
         */
        feed = createFeed(url);

        /* a) Add the same News */
        fFactory.createNews(null, feed, new Date()).setTitle("News Title Case_1");
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());
      }

      /* Case 2: News with Title and URL */
      {
        /* Initial Add News */
        String url = "http://www.feed-case2.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_2");
        news.setLink(new URI("http://www.news-case2.com/index.html"));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_2");
        news.setLink(new URI("http://www.news-case2.com/index.html"));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_2 Updated");
        news.setLink(new URI("http://www.news-case2.com/index.html"));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());
      }

      /* Case 3: News with Title and Guid */
      {
        /* Initial Add News */
        String url = "http://www.feed-case3.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_3");
        news.setGuid(fFactory.createGuid(news, "News_Case_3_Guid"));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_3");
        news.setGuid(fFactory.createGuid(news, "News_Case_3_Guid"));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_3 Updated");
        news.setGuid(fFactory.createGuid(news, "News_Case_3_Guid"));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());
      }

      /* Case 4: News with Title and Publish Date */
      {
        /* Initial Add News */
        long time = System.currentTimeMillis();
        String url = "http://www.feed-case4.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_4");
        news.setPublishDate(new Date(time));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_4");
        news.setPublishDate(new Date(time));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Publish Date */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_4");
        news.setPublishDate(new Date(System.currentTimeMillis() + 1000));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());
      }

      /* Case 5: News with Title, URL and Publish Date */
      {
        /* Initial Add News */
        long time = System.currentTimeMillis();
        String url = "http://www.feed-case5.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_5");
        news.setLink(new URI("http://www.news-case5.com/index.html"));
        news.setPublishDate(new Date(time));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_5");
        news.setLink(new URI("http://www.news-case5.com/index.html"));
        news.setPublishDate(new Date(time));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_5 Updated");
        news.setLink(new URI("http://www.news-case5.com/index.html"));
        news.setPublishDate(new Date(time));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* c) Add the same News with updated Publish Date */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_5 Updated");
        news.setLink(new URI("http://www.news-case5.com/index.html"));
        news.setPublishDate(new Date(System.currentTimeMillis() + 1000));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());
      }

      /* Case 6: News with Title, Guid and Publish Date */
      {
        /* Initial Add News */
        long time = System.currentTimeMillis();
        String url = "http://www.feed-case6.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_6");
        news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
        news.setPublishDate(new Date(time));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_6");
        news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
        news.setPublishDate(new Date(time));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_6 Updated");
        news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
        news.setPublishDate(new Date(time));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());

        /* c) Add the same News with updated Guid */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_6");
        news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid_Updated"));
        news.setPublishDate(new Date(time));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Expected two News in this Feed!", 2, feedRef.resolve().getNews().size());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* d) Add the same News with updated Publish Date */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_6");
        news.setGuid(fFactory.createGuid(news, "News_Case_6_Guid"));
        news.setPublishDate(new Date(time + 1000));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 2, feedRef.resolve().getNews().size());

        List<INews> newsRefs = feedRef.resolve().getNews();
        for (INews newsRef : newsRefs) {
          if ("News_Case_6_Guid".equals(newsRef.getGuid().getValue()))
            assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, newsRef.getState());
        }
      }

      /* Case 7: News with Title, URL and Guid */
      {
        /* Initial Add News */
        String url = "http://www.feed-case7.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_7");
        news.setGuid(fFactory.createGuid(news, "News_Case_7_Guid"));
        news.setLink(new URI("http://www.news-case7.com/index.html"));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_7");
        news.setGuid(fFactory.createGuid(news, "News_Case_7_Guid"));
        news.setLink(new URI("http://www.news-case7.com/index.html"));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_7 Updated");
        news.setGuid(fFactory.createGuid(news, "News_Case_7_Guid"));
        news.setLink(new URI("http://www.news-case7.com/index.html"));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* c) Add the same News with updated URL */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_7 Updated");
        news.setGuid(fFactory.createGuid(news, "News_Case_7_Guid"));
        news.setLink(new URI("http://www.news-case7.com/index-updated.html"));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not READ!", INews.State.READ, feedRef.resolve().getNews().get(0).getState());

        /* d) Add the same News with updated Guid */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_7");
        news.setGuid(fFactory.createGuid(news, "News_Case_7_Guid_Updated"));
        news.setLink(new URI("http://www.news-case7.com/index.html"));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Expected two News in this Feed!", 2, feedRef.resolve().getNews().size());
      }

      /* Case 8: News with Title, URL, Guid and Publish Date */
      {
        /* Initial Add News */
        long time = System.currentTimeMillis();
        String url = "http://www.feed-case8.com";
        IFeed feed = createFeed(url);
        INews news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_8");
        news.setLink(new URI("http://www.news-case8.com/index.html"));
        news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
        news.setPublishDate(new Date(time));
        FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

        /* a) Add the same News */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_8");
        news.setLink(new URI("http://www.news-case8.com/index.html"));
        news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
        news.setPublishDate(new Date(time));
        IFeed mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State changed unexpectedly!", INews.State.NEW, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* b) Add the same News with updated Title */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_8 Updated");
        news.setLink(new URI("http://www.news-case8.com/index.html"));
        news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
        news.setPublishDate(new Date(time));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 1, feedRef.resolve().getNews().size());
        assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, feedRef.resolve().getNews().get(0).getState());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* c) Add the same News with updated Guid */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_8");
        news.setLink(new URI("http://www.news-case8.com/index.html"));
        news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid_Updated"));
        news.setPublishDate(new Date(time));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Expected two News in this Feed!", 2, feedRef.resolve().getNews().size());

        /* Mark News Read */
        news = feedRef.resolve().getNews().get(0);
        news.setState(INews.State.READ);
        fDao.saveNews(news);

        /* d) Add the same News with updated Publish Date */
        feed = createFeed(url);
        news = fFactory.createNews(null, feed, new Date());
        news.setTitle("News Title Case_8");
        news.setLink(new URI("http://www.news-case8.com/index.html"));
        news.setGuid(fFactory.createGuid(news, "News_Case_8_Guid"));
        news.setPublishDate(new Date(System.currentTimeMillis() + 1000));
        mergedFeed = feedRef.resolve();
        mergedFeed.merge(feed);
        feedRef = new FeedReference(fDao.saveFeed(mergedFeed).getId());
        assertEquals("Same News was added twice!", 2, feedRef.resolve().getNews().size());

        List<INews> newsRefs = feedRef.resolve().getNews();
        for (INews newsRef : newsRefs) {
          if ("News_Case_8_Guid".equals(newsRef.getGuid().getValue()))
            assertEquals("Existing News State is not UPDATED!", INews.State.UPDATED, newsRef.getState());
        }
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test setting a News' state to deleted and then check wether the DB is
   * correctly deleting it completly from the DB, if no longer contained in the
   * Feed.
   *
   * @throws Exception
   */
  @Test
  public void testReallyDeleteNews() throws Exception {
    NewsListener newsListener = null;
    try {

      /* Add initial News */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      INews news1 = fFactory.createNews(null, feed, new Date());
      news1.setTitle("News1 Title");
      INews news2 = fFactory.createNews(null, feed, new Date());
      news2.setTitle("News2 Title");
      INews news3 = fFactory.createNews(null, feed, new Date());
      news3.setTitle("News3 Title");

      final URI news1Link = new URI("http://www.news1.com/index.html");
      final URI news2Link = new URI("http://www.news2.com/index.html");
      final URI news3Link = new URI("http://www.news3.com/index.html");
      news1.setLink(news1Link);
      news2.setLink(news2Link);
      news3.setLink(news3Link);
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      assertEquals(3, fDao.loadFeed(feedRef.getId()).getNews().size());

      /* Mark 2 News as Deleted and save News */
      news1 = fDao.loadFeed(feedRef.getId()).getNews().get(0);
      news1.setState(INews.State.DELETED);
      news2 = fDao.loadFeed(feedRef.getId()).getNews().get(1);
      news2.setState(INews.State.DELETED);
      news3 = fDao.loadFeed(feedRef.getId()).getNews().get(2);
      news3.setState(INews.State.READ);

      final boolean newsUpdatedEvents[] = new boolean[2];

      newsListener = new NewsAdapter() {
        @Override
        public void newsUpdated(Set<NewsEvent> events) {
          for (NewsEvent event : events) {
            INews news = event.getEntity();
            if (news.getLink().equals(news1Link))
              newsUpdatedEvents[0] = true;
            else if (news.getLink().equals(news2Link))
              newsUpdatedEvents[1] = true;
          }
        }
      };
      fModel.addNewsListener(newsListener);

      NewsReference newsReference1 = new NewsReference(fDao.saveNews(news1).getId());
      NewsReference newsReference2 = new NewsReference(fDao.saveNews(news2).getId());
      NewsReference newsReference3 = new NewsReference(fDao.saveNews(news3).getId());

      assertEquals(INews.State.DELETED, fDao.loadFeed(feedRef.getId()).getNews().get(0).getState());
      assertEquals(INews.State.DELETED, fDao.loadFeed(feedRef.getId()).getNews().get(1).getState());
      assertEquals(INews.State.READ, fDao.loadFeed(feedRef.getId()).getNews().get(2).getState());

      /* Check Deleted News now being Deleted from DB */
      IFeed emptyFeed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      IFeed savedFeed = feedRef.resolve();
      MergeResult mergeResult = savedFeed.mergeAndCleanUp(emptyFeed);
      TestUtils.saveFeed(mergeResult);

      feed = null;
      news1 = null;
      news2 = null;
      news3 = null;
      System.gc();

      /* Asserts follow */
      assertEquals(1, fDao.loadFeed(feedRef.getId()).getNews().size());
      assertNull(fDao.loadNews(newsReference1.getId()));
      assertNull(fDao.loadNews(newsReference2.getId()));
      assertNotNull(fDao.loadNews(newsReference3.getId()));

      for (int i = 0; i < newsUpdatedEvents.length; i++)
        if (!newsUpdatedEvents[i])
          fail("Missing newsUpdated event in NewsListener!");
    } finally {
      if (newsListener != null)
        fModel.removeNewsListener(newsListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on Folder persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatFolderEvents() throws Exception {
    FolderListener folderListener = null;
    try {
      /* Add */
      final FolderReference rootFolder = new FolderReference(fDao.saveFolder(fFactory.createFolder(null, null, "Root")).getId());

      IFolder folder = fFactory.createFolder(null, rootFolder.resolve(), "Folder");
      final boolean folderEvents[] = new boolean[3];
      final FolderReference folderReference[] = new FolderReference[1];
      folderListener = new FolderListener() {
        boolean updateEventOccurred = false;

        public void folderAdded(Set<FolderEvent> events) {
          for (FolderEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            folderEvents[0] = true;
          }
        }

        public void folderDeleted(Set<FolderEvent> events) {
          for (FolderEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (folderReference[0].references(event.getEntity()))
              folderEvents[1] = true;
          }
        }

        public void folderUpdated(Set<FolderEvent> events) {
          for (FolderEvent event : events) {
            if (updateEventOccurred)
              return;

            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (folderReference[0].references(event.getEntity()))
              folderEvents[2] = true;

            updateEventOccurred = true;
          }
        }
      };
      fModel.addFolderListener(folderListener);
      folderReference[0] = new FolderReference(fDao.saveFolder(folder).getId());

      /* Update */
      folder = folderReference[0].resolve();
      folder.setName("Folder Updated");
      fDao.saveFolder(folder);

      /* Delete */
      fDao.deleteFolder(folderReference[0]);

      /* Asserts Follow */
      assertTrue("Missing folderAdded Event", folderEvents[0]);
      assertTrue("Missing folderUpdated Event", folderEvents[2]);
      assertTrue("Missing folderDeleted Event", folderEvents[1]);
    } finally {
      /* Cleanup */
      if (folderListener != null)
        fModel.removeFolderListener(folderListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on SearchMark persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatSearchMarkEvents() throws Exception {
    SearchMarkListener searchMarkListener = null;
    try {
      /* Add */
      final FolderReference folderRef = new FolderReference(fDao.saveFolder(fFactory.createFolder(null, null, "Folder")).getId());
      ISearchMark searchMark = fFactory.createSearchMark(null, folderRef.resolve(), "SearchMark");
      final boolean searchMarkEvents[] = new boolean[3];
      final SearchMarkReference searchMarkReference[] = new SearchMarkReference[1];
      searchMarkListener = new SearchMarkListener() {
        public void searchMarkAdded(Set<SearchMarkEvent> events) {
          for (SearchMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            searchMarkEvents[0] = true;
          }
        }

        public void searchMarkDeleted(Set<SearchMarkEvent> events) {
          for (SearchMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            if (searchMarkReference[0].references(event.getEntity()))
              searchMarkEvents[1] = true;
          }
        }

        public void searchMarkUpdated(Set<SearchMarkEvent> events) {
          for (SearchMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            if (searchMarkReference[0].references(event.getEntity()))
              searchMarkEvents[2] = true;
          }
        }
      };
      fModel.addSearchMarkListener(searchMarkListener);
      searchMarkReference[0] = new SearchMarkReference(fDao.saveSearchMark(searchMark).getId());

      /* Update */
      searchMark = searchMarkReference[0].resolve();
      searchMark.setName("SearchMark Updated");
      fDao.saveSearchMark(searchMark);

      /* Delete */
      fDao.deleteSearchMark(searchMarkReference[0]);

      /* Asserts Follow */
      assertTrue("Missing searchMarkAdded Event", searchMarkEvents[0]);
      assertTrue("Missing searchMarkUpdated Event", searchMarkEvents[2]);
      assertTrue("Missing searchMarkDeleted Event", searchMarkEvents[1]);
    } finally {
      /* Cleanup */
      if (searchMarkListener != null)
        fModel.removeSearchMarkListener(searchMarkListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on SearchCondition persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatSearchConditionEvents() throws Exception {
    SearchConditionListener searchConditionListener = null;
    try {
      /* Add */
      FolderReference folderRef = new FolderReference(fDao.saveFolder(fFactory.createFolder(null, null, "Folder")).getId());
      SearchMarkReference searchMarkRef = new SearchMarkReference(fDao.saveSearchMark(fFactory.createSearchMark(null, folderRef.resolve(), "SearchMark")).getId());
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
      ISearchCondition searchCondition = fFactory.createSearchCondition(null, searchMarkRef.resolve(), field, SearchSpecifier.CONTAINS, "Foo");
      final boolean searchConditionEvents[] = new boolean[3];
      final SearchConditionReference searchConditionReference[] = new SearchConditionReference[1];
      searchConditionListener = new SearchConditionListener() {
        public void searchConditionAdded(Set<SearchConditionEvent> events) {
          for (SearchConditionEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            searchConditionEvents[0] = true;
          }
        }

        public void searchConditionDeleted(Set<SearchConditionEvent> events) {
          for (SearchConditionEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (searchConditionReference[0].references(event.getEntity()))
              searchConditionEvents[1] = true;
          }
        }

        public void searchConditionUpdated(Set<SearchConditionEvent> events) {
          for (SearchConditionEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (searchConditionReference[0].references(event.getEntity()))
              searchConditionEvents[2] = true;
          }
        }
      };
      fModel.addSearchConditionListener(searchConditionListener);
      searchConditionReference[0] = new SearchConditionReference(fDao.saveSearchCondition(searchCondition).getId());

      /* Update */
      searchCondition = searchConditionReference[0].resolve();
      searchCondition.setValue("Bar");
      searchCondition.setSpecifier(SearchSpecifier.CONTAINS_NOT);
      fDao.saveSearchCondition(searchCondition);

      /* Delete */
      fDao.deleteSearchCondition(searchConditionReference[0]);

      /* Asserts Follow */
      assertTrue("Missing searchConditionAdded Event", searchConditionEvents[0]);
      assertTrue("Missing searchConditionUpdated Event", searchConditionEvents[2]);
      assertTrue("Missing searchConditionDeleted Event", searchConditionEvents[1]);

    } finally {
      fModel.removeSearchConditionListener(searchConditionListener);
      if (searchConditionListener != null)
        fModel.removeSearchConditionListener(searchConditionListener);
    }

  }

  /**
   * Test added, updated and deleted Events sent on BookMark persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatBookMarkEvents() throws Exception {
    BookMarkListener bookMarkListener = null;
    try {
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      /* Add */
      fDao.saveFeed(feed);
      final FolderReference folderRef = new FolderReference(fDao.saveFolder(fFactory.createFolder(null, null, "Folder")).getId());
      IBookMark bookMark = fFactory.createBookMark(null, folderRef.resolve(), new FeedLinkReference(feed.getLink()), "BookMark");
      final boolean bookMarkEvents[] = new boolean[3];
      final BookMarkReference bookMarkReference[] = new BookMarkReference[1];
      bookMarkListener = new BookMarkListener() {
        public void bookMarkAdded(Set<BookMarkEvent> events) {
          for (BookMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            bookMarkEvents[0] = true;
          }
        }

        public void bookMarkDeleted(Set<BookMarkEvent> events) {
          for (BookMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            if (bookMarkReference[0].references(event.getEntity()))
              bookMarkEvents[1] = true;
          }
        }

        public void bookMarkUpdated(Set<BookMarkEvent> events) {
          for (BookMarkEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertEquals(folderRef.getId(), event.getEntity().getFolder().getId());
            if (bookMarkReference[0].references(event.getEntity()))
              bookMarkEvents[2] = true;
          }
        }
      };
      fModel.addBookMarkListener(bookMarkListener);
      bookMarkReference[0] = new BookMarkReference(fDao.saveBookMark(bookMark).getId());

      /* Update */
      bookMark = bookMarkReference[0].resolve();
      bookMark.setName("BookMark Updated");
      fDao.saveBookMark(bookMark);

      /* Delete */
      fDao.deleteBookMark(bookMarkReference[0]);

      /* Asserts Follow */
      assertTrue("Missing bookMarkAdded Event", bookMarkEvents[0]);
      assertTrue("Missing bookMarkUpdated Event", bookMarkEvents[2]);
      assertTrue("Missing bookMarkDeleted Event", bookMarkEvents[1]);

    } finally {
      /* Cleanup */
      if (bookMarkListener != null)
        fModel.removeBookMarkListener(bookMarkListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on Feed persistence operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatFeedEvents() throws Exception {
    FeedListener feedListener = null;
    try {
      /* Add */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      final boolean feedEvents[] = new boolean[3];
      final FeedReference feedReference[] = new FeedReference[1];
      feedListener = new FeedListener() {
        public void feedAdded(Set<FeedEvent> events) {
          for (FeedEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            feedEvents[0] = true;
          }
        }

        public void feedDeleted(Set<FeedEvent> events) {
          for (FeedEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (feedReference[0].references(event.getEntity()))
              feedEvents[1] = true;
          }
        }

        public void feedUpdated(Set<FeedEvent> events) {
          for (FeedEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (feedReference[0].references(event.getEntity()))
              feedEvents[2] = true;
          }
        }
      };
      fModel.addFeedListener(feedListener);
      feedReference[0] = new FeedReference(fDao.saveFeed(feed).getId());

      /* Update */
      feed = feedReference[0].resolve();
      feed.setTitle("Feed Updated");
      fDao.saveFeed(feed);

      /* Delete */
      fDao.deleteFeed(feedReference[0]);

      /* Asserts Follow */
      assertTrue("Missing feedAdded Event", feedEvents[0]);
      assertTrue("Missing feedUpdated Event", feedEvents[2]);
      assertTrue("Missing feedDeleted Event", feedEvents[1]);

    } finally {
      /* Cleanup */
      if (feedListener != null)
        fModel.removeFeedListener(feedListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on News persistence operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatNewsEvents() throws Exception {
    NewsListener newsListener = null;
    try {
      /* Add */
      final IFeed feed = fDao.saveFeed(fFactory.createFeed(null, new URI("http://www.feed.com")));
      INews news = fFactory.createNews(null, feed, new Date());
      news.setTitle("News");
      final boolean newsEvents[] = new boolean[3];
      final NewsReference newsReference[] = new NewsReference[1];
      newsListener = new NewsListener() {
        public void newsAdded(Set<NewsEvent> events) {
          for (NewsEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertLinkEquals(feed.getLink(), event.getEntity().getFeedReference().getLink());
            newsEvents[0] = true;
          }
        }

        public void newsDeleted(Set<NewsEvent> events) {
          for (NewsEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertLinkEquals(feed.getLink(), event.getEntity().getFeedReference().getLink());
            if (newsReference[0].references(event.getEntity()))
              newsEvents[1] = true;
          }
        }

        public void newsUpdated(Set<NewsEvent> events) {
          for (NewsEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            assertLinkEquals(feed.getLink(), event.getEntity().getFeedReference().getLink());
            if (newsReference[0].references(event.getEntity()))
              newsEvents[2] = true;
          }
        }
      };
      fModel.addNewsListener(newsListener);
      newsReference[0] = new NewsReference(fDao.saveNews(news).getId());

      /* Update */
      news = newsReference[0].resolve();
      news.setTitle("News Updated");
      fDao.saveNews(news);

      /* Delete */
      fDao.deleteNews(newsReference[0]);

      /* Asserts Follow */
      assertTrue("Missing newsAdded Event", newsEvents[0]);
      assertTrue("Missing newsUpdated Event", newsEvents[2]);
      assertTrue("Missing newsDeleted Event", newsEvents[1]);

    } finally {
      /* Cleanup */
      if (newsListener != null)
        fModel.removeNewsListener(newsListener);
    }
  }

  private void assertLinkEquals(URI expected, URI actual) {
    assertEquals(expected.toString(), actual.toString());
  }

  /**
   * Test added, updated and deleted Events sent on Attachment persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatAttachmentEvents() throws Exception {
    AttachmentListener attachmentListener = null;
    try {
      /* Add */
      FeedReference feedRef = new FeedReference(fDao.saveFeed(fFactory.createFeed(null, new URI("http://www.feed1.com"))).getId());
      NewsReference newsRef = new NewsReference(fDao.saveNews(fFactory.createNews(null, feedRef.resolve(), new Date())).getId());
      IAttachment attachment = fFactory.createAttachment(null, newsRef.resolve());
      attachment.setLink(new URI("http://www.attachment.com"));
      final boolean attachmentEvents[] = new boolean[3];
      final AttachmentReference attachmentReference[] = new AttachmentReference[1];
      attachmentListener = new AttachmentAdapter() {
        @Override
        public void attachmentAdded(Set<AttachmentEvent> events) {
          for (AttachmentEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            attachmentEvents[0] = true;
          }
        }

        @Override
        public void attachmentDeleted(Set<AttachmentEvent> events) {
          for (AttachmentEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (attachmentReference[0].references(event.getEntity()))
              attachmentEvents[1] = true;
          }
        }

        @Override
        public void attachmentUpdated(Set<AttachmentEvent> events) {
          for (AttachmentEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (attachmentReference[0].references(event.getEntity()))
              attachmentEvents[2] = true;
          }
        }
      };
      fModel.addAttachmentListener(attachmentListener);
      attachmentReference[0] = new AttachmentReference(fDao.saveAttachment(attachment).getId());

      /* Update */
      attachment = attachmentReference[0].resolve();
      attachment.setType("MP3");
      fDao.saveAttachment(attachment);

      /* Delete */
      fDao.deleteAttachment(attachmentReference[0]);

      /* Asserts Follow */
      assertTrue("Missing attachmentAdded Event", attachmentEvents[0]);
      assertTrue("Missing attachmentUpdated Event", attachmentEvents[2]);
      assertTrue("Missing attachmentDeleted Event", attachmentEvents[1]);

    } finally {
      /* Cleanup */
      if (attachmentListener != null)
        fModel.removeAttachmentListener(attachmentListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on Category persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatCategoryEvents() throws Exception {
    CategoryListener categoryListener = null;
    try {
      /* Add */
      FeedReference feedRef = new FeedReference(fDao.saveFeed(fFactory.createFeed(null, new URI("http://www.feed2.com"))).getId());
      NewsReference newsRef = new NewsReference(fDao.saveNews(fFactory.createNews(null, feedRef.resolve(), new Date())).getId());
      ICategory category1 = fFactory.createCategory(null, feedRef.resolve());
      category1.setName("Category");
      ICategory category2 = fFactory.createCategory(null, newsRef.resolve());
      category2.setName("Category");
      final boolean categoryEvents[] = new boolean[6];
      final CategoryReference categoryReference[] = new CategoryReference[2];
      categoryListener = new CategoryListener() {
        public void categoryAdded(Set<CategoryEvent> events) {
          for (CategoryEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (categoryEvents[0])
              categoryEvents[1] = true;
            categoryEvents[0] = true;
          }
        }

        public void categoryDeleted(Set<CategoryEvent> events) {
          for (CategoryEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (categoryReference[0].references(event.getEntity()))
              categoryEvents[2] = true;
            else if (categoryReference[1].references(event.getEntity()))
              categoryEvents[3] = true;
          }
        }

        public void categoryUpdated(Set<CategoryEvent> events) {
          for (CategoryEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (categoryReference[0].references(event.getEntity()))
              categoryEvents[4] = true;
            else if (categoryReference[1].references(event.getEntity()))
              categoryEvents[5] = true;
          }
        }
      };
      fModel.addCategoryListener(categoryListener);
      categoryReference[0] = new CategoryReference(fDao.saveCategory(category1).getId());
      categoryReference[1] = new CategoryReference(fDao.saveCategory(category2).getId());

      /* Update */
      category1 = categoryReference[0].resolve();
      category1.setName("Category Updated");
      category2 = categoryReference[1].resolve();
      category2.setName("Category Updated");
      fDao.saveCategory(category1);
      fDao.saveCategory(category2);

      /* Delete */
      fDao.deleteCategory(categoryReference[0]);
      fDao.deleteCategory(categoryReference[1]);

      /* Asserts Follow */
      assertTrue("Missing categoryAdded Event", categoryEvents[0]);
      assertTrue("Missing categoryAdded Event", categoryEvents[1]);
      assertTrue("Missing categoryUpdated Event", categoryEvents[4]);
      assertTrue("Missing categoryUpdated Event", categoryEvents[5]);
      assertTrue("Missing categoryDeleted Event", categoryEvents[2]);
      assertTrue("Missing categoryDeleted Event", categoryEvents[3]);

    } finally {
      /* Cleanup */
      if (categoryListener != null)
        fModel.removeCategoryListener(categoryListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on Person persistence
   * operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatPersonEvents() throws Exception {
    PersonListener personListener = null;
    try {
      /* Add */
      FeedReference feedRef = new FeedReference(fDao.saveFeed(fFactory.createFeed(null, new URI("http://www.feed4.com"))).getId());
      NewsReference newsRef = new NewsReference(fDao.saveNews(fFactory.createNews(null, feedRef.resolve(), new Date())).getId());
      IPerson person1 = fFactory.createPerson(null, feedRef.resolve());
      person1.setName("Person1");
      IPerson person2 = fFactory.createPerson(null, newsRef.resolve());
      person2.setName("Person2");
      final boolean personEvents[] = new boolean[6];
      final PersonReference personReference[] = new PersonReference[2];
      personListener = new PersonListener() {
        public void personAdded(Set<PersonEvent> events) {
          for (PersonEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (personEvents[0])
              personEvents[1] = true;
            personEvents[0] = true;
          }
        }

        public void personDeleted(Set<PersonEvent> events) {
          for (PersonEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (personReference[0].references(event.getEntity()))
              personEvents[2] = true;
            else if (personReference[1].references(event.getEntity()))
              personEvents[3] = true;
          }
        }

        public void personUpdated(Set<PersonEvent> events) {
          for (PersonEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (personReference[0].references(event.getEntity()))
              personEvents[4] = true;
            else if (personReference[1].references(event.getEntity()))
              personEvents[5] = true;
          }
        }
      };
      fModel.addPersonListener(personListener);
      personReference[0] = new PersonReference(fDao.savePerson(person1).getId());
      personReference[1] = new PersonReference(fDao.savePerson(person2).getId());

      /* Update */
      person1 = personReference[0].resolve();
      person1.setName("Person Updated");
      person2 = personReference[1].resolve();
      person2.setName("Person Updated");
      fDao.savePerson(person1);
      fDao.savePerson(person2);

      /* Delete */
      fDao.deletePerson(personReference[0]);
      fDao.deletePerson(personReference[1]);

      /* Asserts Follow */
      assertTrue("Missing personAdded Event", personEvents[0]);
      assertTrue("Missing personAdded Event", personEvents[1]);
      assertTrue("Missing personUpdated Event", personEvents[4]);
      assertTrue("Missing personUpdated Event", personEvents[5]);
      assertTrue("Missing personDeleted Event", personEvents[2]);
      assertTrue("Missing personDeleted Event", personEvents[3]);

    } finally {
      /* Cleanup */
      if (personListener != null)
        fModel.removePersonListener(personListener);
    }
  }

  /**
   * Test added, updated and deleted Events sent on Label persistence operations
   *
   * @throws Exception
   */
  @Test
  public void testFlatLabelEvents() throws Exception {
    LabelListener labelListener = null;
    try {
      /* Add */
      ILabel label = fFactory.createLabel(null, "Label Name");
      final boolean labelEvents[] = new boolean[3];
      final LabelReference labelReference[] = new LabelReference[1];
      labelListener = new LabelListener() {
        public void labelAdded(Set<LabelEvent> events) {
          for (LabelEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            labelEvents[0] = true;
          }
        }

        public void labelDeleted(Set<LabelEvent> events) {
          for (LabelEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (labelReference[0].references(event.getEntity()))
              labelEvents[1] = true;
          }
        }

        public void labelUpdated(Set<LabelEvent> events) {
          for (LabelEvent event : events) {
            assertTrue("Expected this Event to be Root Event", event.isRoot());
            if (labelReference[0].references(event.getEntity()))
              labelEvents[2] = true;
          }
        }
      };
      fModel.addLabelListener(labelListener);
      labelReference[0] = new LabelReference(fDao.saveLabel(label).getId());

      /* Update */
      label = labelReference[0].resolve();
      label.setColor("255,255,128");
      fDao.saveLabel(label);

      /* Delete */
      fDao.deleteLabel(labelReference[0]);

      /* Asserts Follow */
      assertTrue("Missing labelAdded Event", labelEvents[0]);
      assertTrue("Missing labelUpdated Event", labelEvents[2]);
      assertTrue("Missing labelDeleted Event", labelEvents[1]);

    } finally {
      /* Cleanup */
      if (labelListener != null)
        fModel.removeLabelListener(labelListener);
    }
  }

  /**
   * Test adding Properties to Types.
   *
   * @throws Exception
   */
  @Test
  public void testTypeProperties() throws Exception {
    try {

      /* Add Properties to a Folder */
      IFolder folder = fFactory.createFolder(null, null, "Folder");
      folder.setProperty("String", "Foo");
      folder.setProperty("Integer", 1);
      folder.setProperty("Boolean", true);
      folder.setProperty("Double", 2.2D);
      folder.setProperty("Float", 3.3F);
      FolderReference folderRef = new FolderReference(fDao.saveFolder(folder).getId());
      folder = folderRef.resolve();
      assertEquals("Foo", folder.getProperty("String"));
      assertEquals(1, folder.getProperty("Integer"));
      assertEquals(true, folder.getProperty("Boolean"));
      assertEquals(2.2D, folder.getProperty("Double"));
      assertEquals(3.3F, folder.getProperty("Float"));

      /* Add Properties to a Feed */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.myfeed.com"));
      feed.setProperty("String", "Foo");
      feed.setProperty("Integer", 1);
      feed.setProperty("Boolean", true);
      feed.setProperty("Double", 2.2D);
      feed.setProperty("Float", 3.3F);
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());
      feed = feedRef.resolve();
      assertEquals("Foo", feed.getProperty("String"));
      assertEquals(1, feed.getProperty("Integer"));
      assertEquals(true, feed.getProperty("Boolean"));
      assertEquals(2.2D, feed.getProperty("Double"));
      assertEquals(3.3F, feed.getProperty("Float"));

      /* Add Properties to a BookMark */
      IBookMark bookMark = fFactory.createBookMark(null, folderRef.resolve(), new FeedLinkReference(feed.getLink()), "BookMark");
      bookMark.setProperty("String", "Foo");
      bookMark.setProperty("Integer", 1);
      bookMark.setProperty("Boolean", true);
      bookMark.setProperty("Double", 2.2D);
      bookMark.setProperty("Float", 3.3F);
      BookMarkReference bookMarkRef = new BookMarkReference(fDao.saveBookMark(bookMark).getId());
      bookMark = bookMarkRef.resolve();
      assertEquals("Foo", bookMark.getProperty("String"));
      assertEquals(1, bookMark.getProperty("Integer"));
      assertEquals(true, bookMark.getProperty("Boolean"));
      assertEquals(2.2D, bookMark.getProperty("Double"));
      assertEquals(3.3F, bookMark.getProperty("Float"));

      /* Add Properties to a News */
      INews news = fFactory.createNews(null, feedRef.resolve(), new Date());
      news.setProperty("String", "Foo");
      news.setProperty("Integer", 1);
      news.setProperty("Boolean", true);
      news.setProperty("Double", 2.2D);
      news.setProperty("Float", 3.3F);
      NewsReference newsRef = new NewsReference(fDao.saveNews(news).getId());
      news = newsRef.resolve();
      assertEquals("Foo", news.getProperty("String"));
      assertEquals(1, news.getProperty("Integer"));
      assertEquals(true, news.getProperty("Boolean"));
      assertEquals(2.2D, news.getProperty("Double"));
      assertEquals(3.3F, news.getProperty("Float"));

      /* Add Properties to an Attachment */
      IAttachment attachment = fFactory.createAttachment(null, newsRef.resolve());
      attachment.setLink(new URI("http://www.attachment.com"));
      attachment.setProperty("String", "Foo");
      attachment.setProperty("Integer", 1);
      attachment.setProperty("Boolean", true);
      attachment.setProperty("Double", 2.2D);
      attachment.setProperty("Float", 3.3F);
      AttachmentReference attachmentRef = new AttachmentReference(fDao.saveAttachment(attachment).getId());
      attachment = attachmentRef.resolve();
      assertEquals("Foo", attachment.getProperty("String"));
      assertEquals(1, attachment.getProperty("Integer"));
      assertEquals(true, attachment.getProperty("Boolean"));
      assertEquals(2.2D, attachment.getProperty("Double"));
      assertEquals(3.3F, attachment.getProperty("Float"));

    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test Adding, Deleting a Feed with no News.
   *
   * @throws Exception
   */
  @Test
  public void testAddDeleteFeedWithNoNews() throws Exception {
    NewsListener feedListener = null;
    try {
      IFeed feed = NewsModel.getDefault().getTypesFactory().createFeed(null, new URI("http://www.feed.com"));
      final boolean addedEvent[] = new boolean[1];
      final boolean deletedEvent[] = new boolean[1];

      feedListener = new NewsAdapter() {
        @Override
        public void newsAdded(Set<NewsEvent> events) {
          addedEvent[0] = true;
        }

        @Override
        public void newsDeleted(Set<NewsEvent> events) {
          deletedEvent[0] = true;
        }
      };
      NewsModel.getDefault().addNewsListener(feedListener);

      feed = NewsModel.getDefault().getPersistenceLayer().getModelDAO().saveFeed(feed);
      NewsModel.getDefault().getPersistenceLayer().getModelDAO().deleteFeed(new FeedReference(feed.getId()));

      if (addedEvent[0])
        fail("Unexpected newsAdded Event for Feed with 0 News");
      if (deletedEvent[0])
        fail("Unexpected newsDeleted Event for Feed with 0 News");

    } catch (PersistenceException e) {
      TestUtils.fail(e);
    } finally {
      if (feedListener != null)
        NewsModel.getDefault().removeNewsListener(feedListener);
    }
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testSetNewsState() throws Exception {
    IFeed feed = fModel.getTypesFactory().createFeed(null, new URI("http://www.feed.com"));

    fModel.getTypesFactory().createNews(null, feed, new Date());
    fModel.getTypesFactory().createNews(null, feed, new Date());
    fModel.getTypesFactory().createNews(null, feed, new Date());

    FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

    NewsReference news1 = new NewsReference(feedRef.resolve().getNews().get(0).getId());
    NewsReference news2 = new NewsReference(feedRef.resolve().getNews().get(1).getId());
    NewsReference news3 = new NewsReference(feedRef.resolve().getNews().get(2).getId());

    List<NewsReference> news = new ArrayList<NewsReference>();
    news.add(news1);
    news.add(news2);

    assertEquals(news1.resolve().getState(), INews.State.NEW);
    assertEquals(news2.resolve().getState(), INews.State.NEW);
    assertEquals(news3.resolve().getState(), INews.State.NEW);

    for (NewsReference reference : news) {
      INews newsitem = reference.resolve();
      newsitem.setState(INews.State.UNREAD);
      fDao.saveNews(newsitem);
    }

    assertEquals(news1.resolve().getState(), INews.State.UNREAD);
    assertEquals(news2.resolve().getState(), INews.State.UNREAD);
    assertEquals(news3.resolve().getState(), INews.State.NEW);

    for (NewsReference reference : news) {
      INews newsitem = reference.resolve();
      newsitem.setState(INews.State.READ);
      fDao.saveNews(newsitem);
    }

    assertEquals(news1.resolve().getState(), INews.State.READ);
    assertEquals(news2.resolve().getState(), INews.State.READ);
    assertEquals(news3.resolve().getState(), INews.State.NEW);

    for (NewsReference reference : news) {
      INews newsitem = reference.resolve();
      newsitem.setState(INews.State.DELETED);
      fDao.saveNews(newsitem);
    }

    assertEquals(news1.resolve().getState(), INews.State.DELETED);
    assertEquals(news2.resolve().getState(), INews.State.DELETED);
    assertEquals(news3.resolve().getState(), INews.State.NEW);
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testLoadNewsStates() throws Exception {
    IFeed feed = fModel.getTypesFactory().createFeed(null, new URI("http://www.feed.com"));
    FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

    for (int i = 0; i < 5; i++) {
      INews news = fModel.getTypesFactory().createNews(null, feed, new Date());
      fDao.saveNews(news);
      news.setState(INews.State.NEW);
      fDao.saveNews(news);
    }

    for (int i = 0; i < 4; i++) {
      INews news = fModel.getTypesFactory().createNews(null, feed, new Date());
      fDao.saveNews(news);
      news.setState(INews.State.UPDATED);
      fDao.saveNews(news);
    }

    for (int i = 0; i < 3; i++) {
      INews news = fModel.getTypesFactory().createNews(null, feed, new Date());
      fDao.saveNews(news);
      news.setState(INews.State.UNREAD);
      fDao.saveNews(news);
    }

    for (int i = 0; i < 2; i++) {
      INews news = fModel.getTypesFactory().createNews(null, feed, new Date());
      fDao.saveNews(news);
      news.setState(INews.State.READ);
      fDao.saveNews(news);
    }

    for (int i = 0; i < 1; i++) {
      INews news = fModel.getTypesFactory().createNews(null, feed, new Date());
      fDao.saveNews(news);
      news.setState(INews.State.HIDDEN);
      fDao.saveNews(news);
    }

    int newCount = 0, updatedCount = 0, unreadCount = 0, readCount = 0, hiddenCount = 0;

    List<State> states = new ArrayList<State>();

    feed = feedRef.resolve();
    List<INews> news = feed.getNews();

    for (INews newsitem : news) {
      states.add(newsitem.getState());
    }

    for (State state : states) {
      if (state == INews.State.NEW)
        newCount++;
      else if (state == INews.State.UPDATED)
        updatedCount++;
      else if (state == INews.State.UNREAD)
        unreadCount++;
      else if (state == INews.State.READ)
        readCount++;
      else if (state == INews.State.HIDDEN)
        hiddenCount++;
    }

    assertEquals(newCount, 5);
    assertEquals(updatedCount, 4);
    assertEquals(unreadCount, 3);
    assertEquals(readCount, 2);
    assertEquals(hiddenCount, 1);
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testDeleteTypeFromDeleteParent() throws Exception {

    /* Folder, BookMark, Feed, News (Folder Deleted) */
    {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      IBookMark mark = fFactory.createBookMark(null, root, new FeedLinkReference(feed.getLink()), "BookMark");
      root = fDao.saveFolder(root);
      mark = (IBookMark) root.getMarks().get(0);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      fDao.deleteFolder(new FolderReference(root.getId()));

      assertNull("Expected this Entity to be NULL", new FolderReference(root.getId()).resolve());
      assertNull("Expected this Entity to be NULL", new BookMarkReference(mark.getId()).resolve());
      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }

    /* Root Folder, Folder, BookMark, Feed, News (Folder Deleted) */
    {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFolder folder = fFactory.createFolder(null, root, "Folder");
      folder = fDao.saveFolder(folder);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed2.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      IBookMark mark = fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark");
      folder = fDao.saveFolder(folder);
      mark = (IBookMark) folder.getMarks().get(0);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      fDao.deleteFolder(new FolderReference(folder.getId()));

      assertNull("Expected this Entity to be NULL", new FolderReference(folder.getId()).resolve());
      assertNull("Expected this Entity to be NULL", new BookMarkReference(mark.getId()).resolve());
      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }

    /* Root Folder, Folder, BookMark, Feed, News (Folder Deleted #2) */
    {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFolder folder = fFactory.createFolder(null, root, "Folder");
      folder = fDao.saveFolder(folder);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed3.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      IBookMark mark = fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark");
      folder = fDao.saveFolder(folder);
      mark = (IBookMark) folder.getMarks().get(0);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      /* Delete by calling delete */
      fDao.deleteFolder(new FolderReference(folder.getId()));

      final long rootFolderId = root.getId();
      FolderListener folderListener = new FolderAdapter() {
        @Override
        public void folderUpdated(Set<FolderEvent> events) {
          for (FolderEvent event : events) {
            if (event.getEntity().getId() == rootFolderId)
              assertTrue(event.isRoot());
            else
              assertFalse(event.isRoot());
          }
        }
      };
      NewsModel.getDefault().addFolderListener(folderListener);
      try {
        fDao.saveFolder(root);
      } finally {
        NewsModel.getDefault().removeFolderListener(folderListener);
      }

      assertNull("Expected this Entity to be NULL", new FolderReference(folder.getId()).resolve());
      assertNull("Expected this Entity to be NULL", new BookMarkReference(mark.getId()).resolve());
      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }

    /* Folder, BookMark, Feed, News (BookMark Deleted) */
    {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed4.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      IBookMark mark = fFactory.createBookMark(null, root, new FeedLinkReference(feed.getLink()), "BookMark");
      root = fDao.saveFolder(root);
      mark = (IBookMark) root.getMarks().get(0);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      fDao.deleteBookMark(new BookMarkReference(mark.getId()));

      assertNull("Expected this Entity to be NULL", new BookMarkReference(mark.getId()).resolve());
      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }

    /* Folder, BookMark, Feed, News (BookMark Deleted #2) */
    {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed5.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      IBookMark mark = fFactory.createBookMark(null, root, new FeedLinkReference(feed.getLink()), "BookMark");
      root = fDao.saveFolder(root);
      mark = (IBookMark) root.getMarks().get(0);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      /* Delete by calling delete */
      fDao.deleteBookMark(new BookMarkReference(mark.getId()));

      assertNull("Expected this Entity to be NULL", new BookMarkReference(mark.getId()).resolve());
      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }

    /* Feed, News (Feed Deleted) */
    {
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed6.com"));
      fFactory.createNews(null, feed, new Date());
      FeedReference feedRef = new FeedReference(fDao.saveFeed(feed).getId());

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      NewsReference newsRef = new NewsReference(feedRef.resolve().getNews().get(0).getId());

      fDao.deleteFeed(feedRef);

      assertNull("Expected this Entity to be NULL", feedRef.resolve());
      assertNull("Expected this Entity to be NULL", newsRef.resolve());
    }
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNoUpdateEventForDeletedChildsOfSavedParent() throws Exception {
    FolderAdapter folderListener = null;

    try {
      IFolder root = fFactory.createFolder(null, null, "Root");
      root = fDao.saveFolder(root);

      IFolder folder1 = fFactory.createFolder(null, root, "Folder #1");
      root = fDao.saveFolder(root);
      folder1 = root.getFolders().get(0);

      IFolder folder2 = fFactory.createFolder(null, root, "Folder #2");
      root = fDao.saveFolder(root);
      folder2 = root.getFolders().get(1);

      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));
      fFactory.createNews(null, feed, new Date());
      fDao.saveFeed(feed);

      fFactory.createBookMark(null, folder1, new FeedLinkReference(feed.getLink()), "BookMark");
      folder1 = fDao.saveFolder(folder1);

      assertEquals(1, new FeedReference(feed.getId()).resolve().getNews().size());

      folderListener = new FolderAdapter() {
        @Override
        public void folderUpdated(Set<FolderEvent> events) {
          for (FolderEvent folderEvent : events) {
            IFolder folder = folderEvent.getEntity();
            if (folder.getName().startsWith("Folder"))
              fail("Unexpected Event");
          }
        }
      };

      fModel.addFolderListener(folderListener);

      root.removeFolder(folder1);
      root.removeFolder(folder2);
      fDao.saveFolder(root);
    } finally {
      if (folderListener != null)
        fModel.removeFolderListener(folderListener);
    }
  }
}