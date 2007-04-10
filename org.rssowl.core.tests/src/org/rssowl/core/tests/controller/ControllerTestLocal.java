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

package org.rssowl.core.tests.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.NewsService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This TestCase covers use-cases for the Controller (local only).
 *
 * @author bpasero
 */
public class ControllerTestLocal {
  private IModelDAO fDao;
  private IApplicationLayer fAppLayer;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    fDao = Owl.getPersistenceService().getModelDAO();
    fAppLayer = Owl.getPersistenceService().getApplicationLayer();
    Owl.getPersistenceService().recreateSchema();
    Owl.getPersistenceService().getModelSearch().shutdown();
    Controller.getDefault().getNewsService().testDirtyShutdown();
  }

  private int getNewCount(IFeed feed) {
    NewsService service = Controller.getDefault().getNewsService();
    return service.getNewCount(new FeedLinkReference(feed.getLink()));
  }

  private int getUnreadCount(IFeed feed) {
    NewsService service = Controller.getDefault().getNewsService();
    return service.getUnreadCount(new FeedLinkReference(feed.getLink()));
  }

  private int getStickyCount(IFeed feed) {
    NewsService service = Controller.getDefault().getNewsService();
    return service.getStickyCount(new FeedLinkReference(feed.getLink()));
  }

  /**
   * Test the News-Service.
   *
   * @throws Exception
   */
  @Test
  public void testNewsService() throws Exception {
    NewsService service = Controller.getDefault().getNewsService();

    IFeed feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/manager/rss_2_0.xml")); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    Owl.getModelFactory().createNews(null, feed, new Date()); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(1, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.READ, true, false);
    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.UNREAD, true, false);
    feed.getNews().get(0).setFlagged(true);
    fDao.saveNews(feed.getNews().get(0));

    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.READ, true, false);
    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.UPDATED, true, false);
    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    feed.getNews().get(0).setFlagged(false);
    fDao.saveNews(feed.getNews().get(0));
    fAppLayer.setNewsState(feed.getNews(), INews.State.READ, true, false);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    /* Simulate Shutdown */
    service.stopService();

    Owl.getModelFactory().createNews(null, feed, new Date()); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    Owl.getModelFactory().createNews(null, feed, new Date()); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    assertEquals(2, getUnreadCount(feed));
    assertEquals(2, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.READ, true, false);
    feed.getNews().get(0).setFlagged(true);
    feed.getNews().get(1).setFlagged(true);
    fDao.saveFeed(feed);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(2, getStickyCount(feed));

    fAppLayer.setNewsState(feed.getNews(), INews.State.UNREAD, true, false);

    assertEquals(3, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(2, getStickyCount(feed));

    /* Simulate Dirty Shutdown */
    Owl.getPersistenceService().recreateSchema();

    feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/manager/rss_2_0.xml")); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    Owl.getModelFactory().createNews(null, feed, new Date()); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    Owl.getModelFactory().createNews(null, feed, new Date()); //$NON-NLS-1$
    feed = fDao.saveFeed(feed);

    feed.getNews().get(0).setFlagged(true);
    feed.getNews().get(1).setFlagged(true);
    fDao.saveFeed(feed);

    service.testDirtyShutdown();

    assertEquals(2, getUnreadCount(feed));
    assertEquals(2, getNewCount(feed));
    assertEquals(2, getStickyCount(feed));
  }

  /**
   * Test the News-Service with an Updated News.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithUpdatedNews() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news1 = Owl.getModelFactory().createNews(null, feed, new Date());
    news1.setTitle("News Title #1");
    news1.setLink(new URI("http://www.link.com"));
    news1.setFlagged(true);

    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(1, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    feed.getNews().get(0).setTitle("News Title Updated #1");
    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(1, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));
  }

  /**
   * Test the News-Service with an Updated News.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithUpdatedNews2() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news1 = Owl.getModelFactory().createNews(null, feed, new Date());
    news1.setTitle("News Title #1");
    news1.setLink(new URI("http://www.link.com"));
    news1.setState(INews.State.READ);

    feed = fDao.saveFeed(feed);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    feed.getNews().get(0).setTitle("News Title Updated #1");
    feed.getNews().get(0).setState(INews.State.UPDATED);
    feed.getNews().get(0).setFlagged(true);
    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(INews.State.UPDATED, feed.getNews().get(0).getState());
    assertEquals(1, getStickyCount(feed));

    feed.getNews().get(0).setState(INews.State.READ);
    feed = fDao.saveFeed(feed);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));
  }

  /**
   * Test the News-Service with an Deleted News.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithDeletedNews() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news1 = Owl.getModelFactory().createNews(null, feed, new Date());
    news1.setTitle("News Title #1");
    news1.setLink(new URI("http://www.link.com"));
    news1.setFlagged(true);

    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(1, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    fDao.deleteNews(new NewsReference(feed.getNews().get(0).getId()));

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));
  }

  /**
   * Test the News-Service with an Updated News.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithDeletedNews2() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news1 = Owl.getModelFactory().createNews(null, feed, new Date());
    news1.setTitle("News Title #1");
    news1.setLink(new URI("http://www.link.com"));
    news1.setState(INews.State.READ);

    feed = fDao.saveFeed(feed);

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));

    feed.getNews().get(0).setTitle("News Title Updated #1");
    feed.getNews().get(0).setState(INews.State.UPDATED);
    feed.getNews().get(0).setFlagged(true);
    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));
    assertEquals(INews.State.UPDATED, feed.getNews().get(0).getState());

    feed.getNews().get(0).setState(INews.State.READ);
    feed = fDao.saveFeed(feed);

    fDao.deleteNews(new NewsReference(feed.getNews().get(0).getId()));

    assertEquals(0, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(0, getStickyCount(feed));
  }

  /**
   * Test the News-Service with an Unread News.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithApplicationLayerSaveNews() throws Exception {
    IFeed feed = new Feed(new URI("http://www.feed.com"));
    feed = fDao.saveFeed(feed);

    INews news1 = Owl.getModelFactory().createNews(null, feed, new Date());
    news1.setTitle("News Title #1");
    news1.setLink(new URI("http://www.link.com"));
    news1.setState(INews.State.UNREAD);
    news1.setFlagged(true);

    feed = fDao.saveFeed(feed);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));

    feed.getNews().get(0).setTitle("News Title Updated #1");

    List<INews> news = new ArrayList<INews>();
    news.add(feed.getNews().get(0));

    fAppLayer.saveNews(news);

    assertEquals(1, getUnreadCount(feed));
    assertEquals(0, getNewCount(feed));
    assertEquals(1, getStickyCount(feed));
  }
}