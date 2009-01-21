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

package org.rssowl.core.tests.model;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.IApplicationService;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * Tests functionality of news filters.
 *
 * @author bpasero
 */
public class NewsFilterTest {
  private IModelFactory fFactory;
  private IApplicationService fAppService;

  /* IDs of contributed News Actions */
  private static final String MOVE_NEWS_ID = "org.rssowl.core.MoveNewsAction";
  private static final String COPY_NEWS_ID = "org.rssowl.core.CopyNewsAction";
  private static final String MARK_READ_ID = "org.rssowl.core.MarkReadNewsAction";
  private static final String MARK_STICKY_ID = "org.rssowl.core.MarkStickyNewsAction";
  private static final String LABEL_NEWS_ID = "org.rssowl.core.LabelNewsAction";
  private static final String STOP_FILTER_ID = "org.rssowl.core.StopFilterAction";
  private static final String DELETE_NEWS_ID = "org.rssowl.core.DeleteNewsAction";

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
    fAppService = Owl.getApplicationService();
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_MarkRead_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(MARK_READ_ID);
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(INews.State.READ, newsitem.getState());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_MarkSticky_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(MARK_STICKY_ID);
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(true, newsitem.isFlagged());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_AddLabel_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    ILabel label = fFactory.createLabel(null, "New Label");
    DynamicDAO.save(label);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(LABEL_NEWS_ID);
    action.setData(label.getId());
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(1, newsitem.getLabels().size());
      assertEquals(label, newsitem.getLabels().iterator().next());
      assertEquals("New Label", newsitem.getLabels().iterator().next().getName());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_Delete_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(DELETE_NEWS_ID);
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(INews.State.HIDDEN, newsitem.getState());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_CopyNews_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    INewsBin bin = fFactory.createNewsBin(null, bm.getParent(), "Bin");
    DynamicDAO.save(bin);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(COPY_NEWS_ID);
    action.setData(new Long[] { bin.getId() });
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(INews.State.NEW, newsitem.getState());
      assertEquals(0, newsitem.getParentId());
    }

    List<INews> binNews = bin.getNews();
    for (INews newsitem : binNews) {
      assertEquals(INews.State.NEW, newsitem.getState());
      assertEquals(bin.getId(), newsitem.getParentId());
    }

    assertEquals(3, bin.getNewsCount(EnumSet.of(INews.State.NEW)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_MoveNews_MatchAll() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    INewsBin bin = fFactory.createNewsBin(null, bm.getParent(), "Bin");
    DynamicDAO.save(bin);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(MOVE_NEWS_ID);
    action.setData(new Long[] { bin.getId() });
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(INews.State.HIDDEN, newsitem.getState());
    }

    List<INews> binNews = bin.getNews();
    for (INews newsitem : binNews) {
      assertEquals(INews.State.NEW, newsitem.getState());
      assertEquals(bin.getId(), newsitem.getParentId());
    }

    assertEquals(3, bin.getNewsCount(EnumSet.of(INews.State.NEW)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void test_MoveNews_MatchAll_RunAllActions() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    INewsBin bin = fFactory.createNewsBin(null, bm.getParent(), "Bin");
    DynamicDAO.save(bin);

    ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);

    IFilterAction action = fFactory.createFilterAction(MOVE_NEWS_ID);
    action.setData(new Long[] { bin.getId() });
    filter.addAction(action);

    action = fFactory.createFilterAction(MARK_STICKY_ID);
    filter.addAction(action);

    DynamicDAO.save(filter);

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      assertEquals(INews.State.HIDDEN, newsitem.getState());
    }

    List<INews> binNews = bin.getNews();
    for (INews newsitem : binNews) {
      assertEquals(INews.State.NEW, newsitem.getState());
      assertEquals(bin.getId(), newsitem.getParentId());
      assertEquals(true, newsitem.isFlagged());
    }

    assertEquals(3, bin.getNewsCount(EnumSet.of(INews.State.NEW)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStopFilter() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);
    news1.setFlagged(true);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    {
      ISearch search = createStickySearch(true);

      ISearchFilter filter = fFactory.createSearchFilter(null, search, "New News");
      filter.setEnabled(true);

      IFilterAction action = fFactory.createFilterAction(STOP_FILTER_ID);
      filter.addAction(action);
      filter.setOrder(0);

      action = fFactory.createFilterAction(MARK_STICKY_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    {
      ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
      filter.setEnabled(true);
      filter.setMatchAllNews(true);
      filter.setOrder(1);

      IFilterAction action = fFactory.createFilterAction(MARK_READ_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      if (newsitem.equals(news1)) {
        assertEquals(INews.State.NEW, news1.getState());
        assertTrue(news1.isFlagged());
      } else if (newsitem.equals(news2))
        assertEquals(INews.State.READ, news2.getState());
      else if (newsitem.equals(news3))
        assertEquals(INews.State.READ, news3.getState());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDisabledFilter() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);
    news1.setFlagged(true);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    {
      ISearch search = createStickySearch(true);

      ISearchFilter filter = fFactory.createSearchFilter(null, search, "New News");
      filter.setEnabled(false);

      IFilterAction action = fFactory.createFilterAction(STOP_FILTER_ID);
      filter.addAction(action);
      filter.setOrder(0);

      action = fFactory.createFilterAction(MARK_STICKY_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    {
      ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
      filter.setEnabled(true);
      filter.setMatchAllNews(true);
      filter.setOrder(1);

      IFilterAction action = fFactory.createFilterAction(MARK_READ_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      if (newsitem.equals(news1))
        assertEquals(INews.State.READ, news1.getState());
      else if (newsitem.equals(news2))
        assertEquals(INews.State.READ, news2.getState());
      else if (newsitem.equals(news3))
        assertEquals(INews.State.READ, news3.getState());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFilterOrder_AllNews() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "News1");
    news1.setState(INews.State.NEW);
    news1.setFlagged(true);

    INews news2 = createNews(feed, "News2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "News3");
    news3.setState(INews.State.NEW);

    {
      ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
      filter.setEnabled(true);
      filter.setMatchAllNews(true);
      filter.setOrder(0);

      IFilterAction action = fFactory.createFilterAction(MARK_READ_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    {
      ISearch search = createStickySearch(true);

      ISearchFilter filter = fFactory.createSearchFilter(null, search, "New News");
      filter.setEnabled(true);

      IFilterAction action = fFactory.createFilterAction(STOP_FILTER_ID);
      filter.addAction(action);
      filter.setOrder(1);

      action = fFactory.createFilterAction(MARK_STICKY_ID);
      filter.addAction(action);

      DynamicDAO.save(filter);
    }

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      if (newsitem.equals(news1))
        assertEquals(INews.State.READ, news1.getState());
      else if (newsitem.equals(news2))
        assertEquals(INews.State.READ, news2.getState());
      else if (newsitem.equals(news3))
        assertEquals(INews.State.READ, news3.getState());
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testComplexFilter() throws Exception {
    IBookMark bm = createBookMark("local1");
    IFeed feed = fFactory.createFeed(null, bm.getFeedLinkReference().getLink());

    INews news1 = createNews(feed, "Title", "Link1");
    news1.setState(INews.State.NEW);

    INews news2 = createNews(feed, "Title", "Link2");
    news2.setState(INews.State.NEW);

    INews news3 = createNews(feed, "Other");
    news3.setState(INews.State.NEW);

    INews news4 = createNews(feed, "Nothing");
    news4.setState(INews.State.NEW);

    ILabel label = fFactory.createLabel(null, "New Label");
    DynamicDAO.save(label);

    INewsBin bin1 = fFactory.createNewsBin(null, bm.getParent(), "Bin 1");
    INewsBin bin2 = fFactory.createNewsBin(null, bm.getParent(), "Bin 2");

    DynamicDAO.save(bin1);
    DynamicDAO.save(bin2);

    /* Filter "Title is Title": Mark Read, Sticky, Label, Copy */
    {
      ISearch search = createTitleSearch("Title");

      ISearchFilter filter = fFactory.createSearchFilter(null, search, "Title is Title");
      filter.setEnabled(true);
      filter.setOrder(0);

      filter.addAction(fFactory.createFilterAction(MARK_READ_ID));
      filter.addAction(fFactory.createFilterAction(MARK_STICKY_ID));

      IFilterAction labelAction = fFactory.createFilterAction(LABEL_NEWS_ID);
      labelAction.setData(label.getId());
      filter.addAction(labelAction);

      IFilterAction copyAction = fFactory.createFilterAction(COPY_NEWS_ID);
      copyAction.setData(new Long[] { bin1.getId(), bin2.getId() });
      filter.addAction(copyAction);

      DynamicDAO.save(filter);
    }

    /* Filter "Title is Other": Move */
    {
      ISearch search = createTitleSearch("Other");

      ISearchFilter filter = fFactory.createSearchFilter(null, search, "Title is Other");
      filter.setEnabled(true);
      filter.setOrder(1);

      IFilterAction moveAction = fFactory.createFilterAction(MOVE_NEWS_ID);
      moveAction.setData(new Long[] { bin1.getId(), bin2.getId() });
      filter.addAction(moveAction);

      DynamicDAO.save(filter);
    }

    /* Filter "Match All": Label News */
    {

      ISearchFilter filter = fFactory.createSearchFilter(null, null, "All News");
      filter.setEnabled(true);
      filter.setMatchAllNews(true);
      filter.setOrder(2);

      IFilterAction labelAction = fFactory.createFilterAction(LABEL_NEWS_ID);
      labelAction.setData(label.getId());
      filter.addAction(labelAction);

      DynamicDAO.save(filter);
    }

    fAppService.handleFeedReload(bm, feed, null, false);

    List<INews> news = bm.getFeedLinkReference().resolve().getNews();
    for (INews newsitem : news) {
      if (newsitem.equals(news1)) {
        assertEquals(INews.State.READ, news1.getState());
        assertTrue(newsitem.isFlagged());
        assertTrue(!news1.getLabels().isEmpty());
        assertEquals(label, news1.getLabels().iterator().next());
      } else if (newsitem.equals(news2)) {
        assertEquals(INews.State.READ, news2.getState());
        assertTrue(newsitem.isFlagged());
        assertTrue(!news2.getLabels().isEmpty());
        assertEquals(label, news2.getLabels().iterator().next());
      } else if (newsitem.equals(news3)) {
        assertEquals(INews.State.HIDDEN, news3.getState());
      } else if (newsitem.equals(news4)) {
        assertTrue(!news4.getLabels().isEmpty());
        assertEquals(label, news4.getLabels().iterator().next());
      }
    }

    assertEquals(3, bin1.getNews().size());
    assertEquals(1, bin1.getNewsCount(EnumSet.of(INews.State.NEW)));
    assertEquals(2, bin1.getNewsCount(EnumSet.of(INews.State.READ)));

    List<INews> binNews = bin1.getNews();
    for (INews newsitem : binNews) {
      if (newsitem.equals(news1)) {
        assertEquals(INews.State.READ, news1.getState());
        assertTrue(newsitem.isFlagged());
        assertTrue(!news1.getLabels().isEmpty());
        assertEquals(label, news1.getLabels().iterator().next());
      } else if (newsitem.equals(news2)) {
        assertEquals(INews.State.READ, news2.getState());
        assertTrue(newsitem.isFlagged());
        assertTrue(!news2.getLabels().isEmpty());
        assertEquals(label, news2.getLabels().iterator().next());
      } else if (newsitem.equals(news3)) {
        assertEquals(INews.State.NEW, news3.getState());
        assertTrue(newsitem.getLabels().isEmpty());
        assertTrue(!newsitem.isFlagged());
      }
    }

    assertEquals(3, bin2.getNews().size());
    assertEquals(1, bin2.getNewsCount(EnumSet.of(INews.State.NEW)));
    assertEquals(2, bin2.getNewsCount(EnumSet.of(INews.State.READ)));

    binNews = bin2.getNews();
    for (INews newsitem : binNews) {
      if (newsitem.equals(news1)) {
        assertEquals(INews.State.READ, news1.getState());
        assertTrue(newsitem.isFlagged());
      } else if (newsitem.equals(news2)) {
        assertEquals(INews.State.READ, news2.getState());
        assertTrue(newsitem.isFlagged());
      } else if (newsitem.equals(news3)) {
        assertEquals(INews.State.NEW, news3.getState());
        assertTrue(newsitem.getLabels().isEmpty());
        assertTrue(!newsitem.isFlagged());
      }
    }
  }

  private ISearch createStickySearch(boolean sticky) {
    ISearch search = fFactory.createSearch(null);
    ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, INews.class.getName());

    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, sticky);
    search.addSearchCondition(condition);

    return search;
  }

  private ISearch createTitleSearch(String title) {
    ISearch search = fFactory.createSearch(null);
    ISearchField field = fFactory.createSearchField(INews.TITLE, INews.class.getName());

    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, title);
    search.addSearchCondition(condition);

    return search;
  }

  private INews createNews(IFeed feed, String title) {
    INews news = fFactory.createNews(null, feed, new Date());
    news.setTitle(title);
    return news;
  }

  private INews createNews(IFeed feed, String title, String link) throws URISyntaxException {
    INews news = fFactory.createNews(null, feed, new Date());
    news.setLink(new URI(link));
    news.setTitle(title);
    return news;
  }

  private IBookMark createBookMark(String link) throws URISyntaxException {
    IFolder folder = fFactory.createFolder(null, null, "Root");

    IFeed feed = fFactory.createFeed(null, new URI(link));
    DynamicDAO.save(feed);

    IBookMark bm = fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark");
    DynamicDAO.save(folder);

    return bm;
  }
}