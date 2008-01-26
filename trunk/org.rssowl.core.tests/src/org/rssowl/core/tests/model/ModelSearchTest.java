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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.tests.TestUtils;
import org.rssowl.core.util.SearchHit;
import org.rssowl.ui.internal.util.ModelUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * Test searching types from the persistence layer.
 *
 * @author bpasero
 */
public class ModelSearchTest {

  /* One Day in Millis */
  private static final Long DAY = 1000 * 3600 * 24L;

  private IModelFactory fFactory;
  private IModelSearch fModelSearch;
  private String fNewsEntityName;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    fModelSearch = Owl.getPersistenceService().getModelSearch();
    fFactory = Owl.getModelFactory();
    fNewsEntityName = INews.class.getName();

    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_Specifier() throws Exception {
    try {
      Calendar cal = Calendar.getInstance();

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(10);
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("windows");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");
      cal.setTimeInMillis(System.currentTimeMillis() - DAY);
      news2.setPublishDate(cal.getTime());

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");
      cal.setTimeInMillis(System.currentTimeMillis() - 5 * DAY);
      news3.setModifiedDate(cal.getTime());
      cal.setTimeInMillis(System.currentTimeMillis() - 10 * DAY);
      news3.setPublishDate(cal.getTime());

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");
      ICategory news5cat1 = fFactory.createCategory(null, news5);
      news5cat1.setName("Apache Lucene");
      ICategory news5cat2 = fFactory.createCategory(null, news5);
      news5cat2.setName("Java");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Enum (match) */
      {
        ISearchField field = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.READ));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      /* Condition 1b: Enum (no match) */
      {
        ISearchField field = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.DELETED));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 2a: Integer (match) */
      {

        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 10);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 0);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news5);

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 5);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 1);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      /* Condition 2b: Integer (no match) */
      {

        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 15);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 100);

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, 8);

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 3a: String (match) */
      {

        /* Categories */
        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "apple");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "windows");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "slash*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "a*le");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "apache lucene");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, label1.getName());

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "Source for News 5");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);
      }

      /* Condition 3b: String (no match) */
      {

        /* Author */
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "Pasero Benjamin");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Categories */
        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "apple slashdot");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "sleshdod");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "lucene apache");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "Source for");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 4a: Date (match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, news4Date);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4);
      }

      /* Condition 4b: Date (no match) */
      {
        Date wrongDate = new Date();
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, wrongDate);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 5a: Boolean (one match) */
      {
        ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Attachments */
        field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2);
      }

      /* Condition 5b: Boolean (other matches) */
      {
        ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, false);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        /* Attachments */
        field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, false);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4, news5);
      }

      /* Condition 6a: Link (match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.news.com/news1.html");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.news.com/news?.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "*www.news.com/news1.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.source.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4);

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.feed.com/feed.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.feed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 6b: Link (no match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.news.com/news6.html");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.news.com/news?");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "*www.news.com/");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.othersource.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.feed.com/feed2.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "http://www.otherfeed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_NOT_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(10);
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("windows");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");
      ICategory news5cat1 = fFactory.createCategory(null, news5);
      news5cat1.setName("Apache Lucene");
      ICategory news5cat2 = fFactory.createCategory(null, news5);
      news5cat2.setName("Java");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Enum */
      {
        ISearchField field = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, EnumSet.of(State.READ));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      /* Condition 1b: Enum */
      {
        ISearchField field = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, EnumSet.of(State.DELETED));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 2a: Integer */
      {

        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, 10);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4, news5);
      }

      /* Condition 2b: Integer */
      {

        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, 15);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 3a: String (match) */
      {

        /* Categories */
        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "apple");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "windows");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "slash*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "a*le");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "apache lucene");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, label1.getName());

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "Source for News 5");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);
      }

      /* Condition 3b: String (no match) */
      {

        /* Author */
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "Pasero Benjamin");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Categories */
        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "apple slashdot");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "sleshdod");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "lucene apache");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "Source for");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 4a: Date (match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, news4Date);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news5);
      }

      /* Condition 4b: Date */
      {
        Date wrongDate = new Date();
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, wrongDate);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 5a: Boolean (one match) */
      {
        ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, true);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        /* Attachments */
        field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, true);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4, news5);
      }

      /* Condition 5b: Boolean (other matches) */
      {
        ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, false);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Attachments */
        field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, false);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2);
      }

      /* Condition 6a: Link (match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.news.com/news1.html");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.news.com/news?.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "*www.news.com/news1.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.source.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news5);

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.feed.com/feed.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.feed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 6b: Link */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.news.com/news6.html");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.news.com/news?");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "*www.news.com/");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.othersource.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.feed.com/feed2.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.otherfeed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_CONTAINS_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer description with <html><h2>included!</h2></html>");
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("pasero");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news3.setDescription("This is a longer description with \n newlines and <html><h2>included!</h2></html>");
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, "BAR FOO", "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: String (match) */
      {

        /* Title */
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo bar");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "bar foo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "b* f*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "fo? b*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        /* Description */
        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "included");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "lon?er description");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        /* Attachments */
        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "bin/mp3");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "*mp3");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "www.attachment.com*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2);

        /* Author */
        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Benjamin Pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Ben?amin Pase*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Ben*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "test@rssowl.org");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "test@rssowl?*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* All Fields */
        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4);

        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "*pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4);

        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "description new?ines");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);
      }

      /* Condition 1b: String (no match) */
      {

        /* Title */
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "barfoo");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "f? b?");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Description */
        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "html");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "loner desription");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Attachment */
        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "bin/ogg");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "*ogg");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "www.attachments.com*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* All Fields */
        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foobar");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "*barfoo");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_CONTAINS_NOT_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("pasero");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news3.setDescription("This is a longer description with \n newlines and <html><h2>included!</h2></html>");
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, "BAR FOO", "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: String (match) */
      {

        /* Title */
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "foo bar");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "bar foo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "b* f*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "fo? b*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Description */
        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "included");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news4, news5);

        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "lon?er description");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news4, news5);

        /* Attachments */
        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "bin/mp3");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "*mp3");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "www.attachment.com*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4, news5);

        /* Author */
        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "Benjamin Pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news5);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "Pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news5);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "Ben?amin Pase*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news5);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "Ben*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news4, news5);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "test@rssowl.org");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "test@rssowl?*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        /* All Fields */
        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "foo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news5);

        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "*pasero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news5);
      }

      /* Condition 1b: String (no match) */
      {

        /* Title */
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "barfoo");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "f? b?");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Description */
        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "loner desription");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* Attachment */
        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "bin/ogg");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "*ogg");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.ATTACHMENTS_CONTENT, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "www.attachments.com*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        /* All Fields */
        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "foobar");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "*barfoo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_BEGINS_WITH_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      IPerson author2 = fFactory.createPerson(null, news2);
      author2.setName("Benjamin Pilazzi");
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("windows");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");
      ICategory news5cat1 = fFactory.createCategory(null, news5);
      news5cat1.setName("Apache Lucene");
      ICategory news5cat2 = fFactory.createCategory(null, news5);
      news5cat2.setName("Java");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: String (match) */
      {

        /* Categories */
        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "app");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "wind");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "slash*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "a*le");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "a?ache");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "Source for");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "wo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      /* Condition 1b: String (no match) */
      {

        /* Author */
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "Pasero Benj");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Categories */
        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "apple slash");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "slesh");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "lucene");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "For Sourc");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "ork");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 2a: Link (match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.news.com/news1");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.news.com/news?.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.news.com/news1.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.source");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4);

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.feed.");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.feed.com/");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 2b: Link (no match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.news.com/news6");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.othernews.com/news?");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "www.news.com/");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.others");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.feed.com/feed2.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.BEGINS_WITH, "http://www.otherfeed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_ENDS_WITH_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      IPerson author2 = fFactory.createPerson(null, news2);
      author2.setName("Benjamin Pilazzi");
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("windows");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");
      ICategory news5cat1 = fFactory.createCategory(null, news5);
      news5cat1.setName("Apache Lucene");
      ICategory news5cat2 = fFactory.createCategory(null, news5);
      news5cat2.setName("Java");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: String (match) */
      {

        /* Categories */
        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "ple");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "ows");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "d?t");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "p*le");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "che lucene");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "News 5");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "rk");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      /* Condition 1b: String (no match) */
      {

        /* Author */
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "ero Benj");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Categories */
        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "ple slash");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "eshdot");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "che java");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Name */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "News 4");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Labels */
        field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "wo");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }

      /* Condition 2a: Link (match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "news.com/news1.html");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "news.com/news?.html");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "/www.news.com/news1.*");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "source.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4);

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "feed.com/feed.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "feed.com?feed.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      /* Condition 2b: Link (no match) */
      {

        /* News Link */
        ISearchField field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "news.com/news6.ht,ö");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "www.othernews.com/news*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "www.news.com/");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Source Link */
        field = fFactory.createSearchField(INews.SOURCE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "www.others.com");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Feed Link */
        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "feed.com/feed2.xml");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.FEED, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.ENDS_WITH, "*www.otherfeed.com/*");

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_BEFORE_Specifier() throws Exception {
    try {
      Calendar cal = Calendar.getInstance();

      cal.set(2006, Calendar.DECEMBER, 24, 17, 25);
      Date d1 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.JANUARY, 2, 13, 4);
      Date d2 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.FEBRUARY, 10, 10, 10);
      Date d3 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.MARCH, 10, 10, 10);
      Date d4 = new Date(cal.getTimeInMillis());

      cal.set(2008, Calendar.JUNE, 13, 8, 50);
      Date d5 = new Date(cal.getTimeInMillis());

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news1.setPublishDate(d1);

      INews news2 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      news2.setPublishDate(d2);

      INews news3 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      news3.setPublishDate(d3);

      INews news4 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news4.setPublishDate(d4);

      INews news5 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news5.setPublishDate(d5);

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Date (match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_BEFORE, d5);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_BEFORE, d4);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_BEFORE, d3);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_BEFORE, d2);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      /* Condition 1b: Date (no match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_BEFORE, d1);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_AFTER_Specifier() throws Exception {
    try {
      Calendar cal = Calendar.getInstance();

      cal.set(2006, Calendar.DECEMBER, 24, 17, 25);
      Date d1 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.JANUARY, 2, 13, 4);
      Date d2 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.FEBRUARY, 10, 10, 10);
      Date d3 = new Date(cal.getTimeInMillis());

      cal.set(2007, Calendar.MARCH, 10, 10, 10);
      Date d4 = new Date(cal.getTimeInMillis());

      cal.set(2008, Calendar.JUNE, 13, 8, 50);
      Date d5 = new Date(cal.getTimeInMillis());

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news1.setPublishDate(d1);

      INews news2 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      news2.setPublishDate(d2);

      INews news3 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      news3.setPublishDate(d3);

      INews news4 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news4.setPublishDate(d4);

      INews news5 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news5.setPublishDate(d5);

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Date (match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, new Date(0));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, d1);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, d2);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4, news5);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, d3);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4, news5);

        field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, d4);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news5);
      }

      /* Condition 1b: Date (no match) */
      {
        ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_AFTER, d5);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_HIGHER_Specifier() throws Exception {
    try {
      Calendar cal = Calendar.getInstance();

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      news1.setRating(2);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(4);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");
      cal.setTimeInMillis(System.currentTimeMillis() - DAY);
      news2.setPublishDate(cal.getTime());

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news3.setRating(6);
      cal.setTimeInMillis(System.currentTimeMillis() - 5 * DAY);
      news3.setModifiedDate(cal.getTime());
      cal.setTimeInMillis(System.currentTimeMillis() - 10 * DAY);
      news3.setPublishDate(cal.getTime());

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      news4.setRating(8);

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setRating(10);

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Integer (match) */
      {
        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 0);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 2);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 4);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4, news5);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 6);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news4, news5);

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 0);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 4);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);
      }

      /* Condition 1b: Integer (no match) */
      {

        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 10);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 100);

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 8);

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_LOWER_Specifier() throws Exception {
    try {
      Calendar cal = Calendar.getInstance();

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      news1.setRating(2);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(4);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");
      cal.setTimeInMillis(System.currentTimeMillis() - DAY);
      news2.setPublishDate(cal.getTime());

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      news3.setRating(6);
      cal.setTimeInMillis(System.currentTimeMillis() - 5 * DAY);
      news3.setModifiedDate(cal.getTime());
      cal.setTimeInMillis(System.currentTimeMillis() - 10 * DAY);
      news3.setPublishDate(cal.getTime());

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      news4.setRating(8);

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setRating(10);

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1a: Integer (match) */
      {
        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 12);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 10);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 8);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 6);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2);

        field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 4);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 1);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 6);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news3, news4, news5);

        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 2);

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news2, news4, news5);
      }

      /* Condition 1b: Integer (no match) */
      {
        /* Rating */
        ISearchField field = fFactory.createSearchField(INews.RATING, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 2);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());

        /* Age in Days */
        field = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 0);

        result = fModelSearch.searchNews(list(condition), false);
        assertEquals(0, result.size());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWith_IS_SIMILIAR_TO_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(10);
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("windows");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");

      INews news3 = createNews(feed, "Foo Bar", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition: Strings */
      {
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.SIMILIAR_TO, "Pajero");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3, news4);

        field = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.SIMILIAR_TO, "sleshdot");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.SIMILIAR_TO, "foo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.SIMILIAR_TO, "Benjmin Psero");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);

        field = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        condition = fFactory.createSearchCondition(field, SearchSpecifier.SIMILIAR_TO, "Benj?min P?seo");

        result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news3);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWithMixedSpecifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
      ICategory news1cat1 = fFactory.createCategory(null, news1);
      news1cat1.setName("apple");
      ILabel label1 = fFactory.createLabel(null, "work");
      news1.addLabel(label1);
      IAttachment att1news1 = fFactory.createAttachment(null, news1);
      att1news1.setLink(new URI("http://www.attachment.com/att1news1.file"));
      att1news1.setType("bin/mp3 Pasero");

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setRating(10);
      ICategory news2cat1 = fFactory.createCategory(null, news2);
      news2cat1.setName("apple");
      ICategory news2cat2 = fFactory.createCategory(null, news2);
      news2cat2.setName("pasero");
      ILabel label2 = fFactory.createLabel(null, "todo");
      news2.addLabel(label2);
      IAttachment att1news2 = fFactory.createAttachment(null, news2);
      att1news2.setLink(new URI("http://www.attachment.com/att1news2.file"));
      att1news2.setType("bin/doc");
      IAttachment att2news2 = fFactory.createAttachment(null, news2);
      att2news2.setLink(new URI("http://www.attachment.com/att2news2.file"));
      att2news2.setType("bin/wav");

      INews news3 = createNews(feed, "Foo Bar Pasero", "http://www.news.com/news3.html", State.NEW);
      IPerson author3 = fFactory.createPerson(null, news3);
      author3.setName("Benjamin Pasero");
      ICategory news3cat1 = fFactory.createCategory(null, news3);
      news3cat1.setName("apple");
      ICategory news3cat2 = fFactory.createCategory(null, news3);
      news3cat2.setName("windows");
      ICategory news3cat3 = fFactory.createCategory(null, news3);
      news3cat3.setName("slashdot");

      INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.UPDATED);
      Date news4Date = new Date(1000000);
      news4.setPublishDate(news4Date);
      IPerson author4 = fFactory.createPerson(null, news4);
      author4.setName("Pasero");
      ISource source4 = fFactory.createSource(news4);
      source4.setLink(new URI("http://www.source.com"));

      INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.NEW);
      news5.setFlagged(true);
      IPerson author5 = fFactory.createPerson(null, news5);
      author5.setEmail(new URI("test@rssowl.org"));
      ISource source5 = fFactory.createSource(news5);
      source5.setName("Source for News 5");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /*
       * Condition 1: Title CONTAINS Foo OR Author IS test@rssowl.org OR Author
       * IS Benjamin Pasero
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo");

        ISearchField field2 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, "test@rssowl.org");

        ISearchField field3 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS, "Benjamin Pasero");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
        assertSame(result, news1, news3, news5);
      }

      /*
       * Condition 2: Title CONTAINS Foo AND Author IS test@rssowl.org
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo");

        ISearchField field2 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, "test@rssowl.org");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
        assertEquals(0, result.size());
      }

      /*
       * Condition 3: Title CONTAINS Foo AND Author IS Benjamin?Pasero AND
       * Categories IS slash* AND Link BEGINS_WITH http://www.news.com/
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "Foo");

        ISearchField field2 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
        ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "benjami?");

        ISearchField field3 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
        ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS, "slash*");

        ISearchField field4 = fFactory.createSearchField(INews.LINK, fNewsEntityName);
        ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.BEGINS_WITH, "http://www.news.com/");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3, cond4), true);
        assertSame(result, news3);
      }

      /*
       * Condition 4: (State IS *new* OR State is *unread* OR State IS
       * *updated*) AND Has Attachments
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

        ISearchField field2 = fFactory.createSearchField(INews.HAS_ATTACHMENTS, fNewsEntityName);
        ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, true);

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
        assertSame(result, news2);
      }

      /*
       * Condition 5: (State IS *new* OR State is *unread* OR State IS
       * *updated*) AND All_Fields CONTAINS foo
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

        ISearchField field2 = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "pasero");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
        assertSame(result, news2, news3, news4);
      }

      /*
       * Condition 6: (State IS *new* OR State is *unread* OR State IS
       * *updated*)
       */
      {
        ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
        ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
        assertSame(result, news2, news3, news4, news5);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWithComplexSearch() throws Exception {
    Calendar cal = Calendar.getInstance();

    /* First add some Types */
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

    INews news1 = createNews(feed, "Foo", "http://www.news.com/news1.html", State.READ);
    ICategory news1cat1 = fFactory.createCategory(null, news1);
    news1cat1.setName("apple");
    ILabel label1 = fFactory.createLabel(null, "work");
    news1.addLabel(label1);

    INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
    news2.setRating(10);
    ICategory news2cat1 = fFactory.createCategory(null, news2);
    news2cat1.setName("apple");
    ICategory news2cat2 = fFactory.createCategory(null, news2);
    news2cat2.setName("fafa");
    ILabel label2 = fFactory.createLabel(null, "todo");
    news2.addLabel(label2);
    cal.setTimeInMillis(System.currentTimeMillis() - 5 * DAY);
    news2.setModifiedDate(cal.getTime());

    INews news3 = createNews(feed, "Foo Bar Pasero", "http://www.news.com/news3.html", State.NEW);
    IPerson author3 = fFactory.createPerson(null, news3);
    author3.setName("Benjamin Pasero");
    ICategory news3cat1 = fFactory.createCategory(null, news3);
    news3cat1.setName("apple");
    ICategory news3cat2 = fFactory.createCategory(null, news3);
    news3cat2.setName("windows");
    ICategory news3cat3 = fFactory.createCategory(null, news3);
    news3cat3.setName("slashdot");

    INews news4 = createNews(feed, null, "http://www.news.com/news4.html", State.HIDDEN);
    Date news4Date = new Date(1000000);
    news4.setPublishDate(news4Date);
    IPerson author4 = fFactory.createPerson(null, news4);
    author4.setName("Benjamin Pasero");
    ISource source4 = fFactory.createSource(news4);
    source4.setLink(new URI("http://www.source.com"));

    INews news5 = createNews(feed, null, "http://www.news.com/news5.html", State.DELETED);
    news5.setFlagged(true);
    IPerson author5 = fFactory.createPerson(null, news5);
    author5.setEmail(new URI("test@rssowl.org"));
    ISource source5 = fFactory.createSource(news5);
    source5.setName("Source for News 5");

    DynamicDAO.save(feed);

    /* Wait for Indexer */
    waitForIndexer();

    /*
     * Condition 1: (State IS *new* OR State is *unread* OR State IS *updated*)
     * OR (Entire News contains "Foo") OR (Author is "Benjamin Pasero")
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field2 = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "Foo");

      ISearchField field3 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
      ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS, "Benjamin Pasero");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news2, news3);
    }

    /*
     * Condition 2: +(State IS *new* OR State is *unread* OR State IS *updated*)
     * AND (Entire News contains "Foo") AND (Author is "Benjamin Pasero")
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field2 = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "Foo");

      ISearchField field3 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
      ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.CONTAINS, "Benjamin Pasero");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news3);
    }

    /*
     * Condition 3: (Entire News contains "Foo") AND (Title contains "Bar") AND
     * (Author is not "Benjamin Pasero")
     */
    {
      ISearchField field1 = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "fafa");

      ISearchField field2 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "Bar");

      ISearchField field3 = fFactory.createSearchField(INews.AUTHOR, fNewsEntityName);
      ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS_NOT, "Benjamin Pasero");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news2);
    }

    /*
     * Condition 4: +(State IS *new* OR State is *unread* OR State IS *updated*)
     * AND (Category IS Windows) AND (Category IS Apple)
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field4 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS, "windows");

      ISearchField field5 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond5 = fFactory.createSearchCondition(field5, SearchSpecifier.IS, "apple");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond4, cond5), true);
      assertSame(result, news3);
    }

    /*
     * Condition 5: +(State IS *new* OR State is *unread* OR State IS *updated*)
     * AND (Category IS Windows) AND (Category IS Apple) AND (Category IS NOT
     * Slashdot)
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field4 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS, "windows");

      ISearchField field5 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond5 = fFactory.createSearchCondition(field5, SearchSpecifier.IS, "apple");

      ISearchField field6 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond6 = fFactory.createSearchCondition(field6, SearchSpecifier.IS_NOT, "slashdot");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond4, cond5, cond6), true);
      assertEquals(0, result.size());
    }

    /*
     * Condition 6: +(State IS *new* OR State is *unread* OR State IS *updated*)
     * AND (Category IS NOT Windows)
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field4 = fFactory.createSearchField(INews.CATEGORIES, fNewsEntityName);
      ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS_NOT, "windows");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond4), true);
      assertSame(result, news2);
    }

    /*
     * Condition 7: +(State IS *new* OR State is *unread* OR State IS *updated*)
     * AND (Age is Less than 5 Days)
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchField field4 = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);
      ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS_LESS_THAN, 5);

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond4), true);
      assertSame(result, news3);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWithNegation() throws Exception {

    /* First add some Types */
    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://www.feed.com/feed2.xml"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);

    INews news3 = createNews(feed2, "First News of Feed Two", "http://www.news.com/news3.html", State.NEW);
    INews news4 = createNews(feed2, "Second News of Feed Two", "http://www.news.com/news4.html", State.HIDDEN);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);

    /* Wait for Indexer */
    waitForIndexer();

    /*
     * Condition 1: Title contains First OR Feed is not
     * "http://www.feed.com/feed1.xml"
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "First");

      ISearchField field2 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, "http://www.feed.com/feed1.xml");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertSame(result, news1, news3, news4);
    }

    /*
     * Condition 2: Title contains First OR Feed is not
     * "http://www.feed.com/feed1.xml" OR Feed is not
     * "http://www.feed.com/feed2.xml"
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "First");

      ISearchField field2 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, "http://www.feed.com/feed1.xml");

      ISearchField field3 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS_NOT, "http://www.feed.com/feed2.xml");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news2, news3, news4);
    }

    /*
     * Condition 3: State is Unread OR Feed is not
     * "http://www.feed.com/feed1.xml" OR Feed is not
     * "http://www.feed.com/feed2.xml"
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.UNREAD));

      ISearchField field2 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, "http://www.feed.com/feed1.xml");

      ISearchField field3 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS_NOT, "http://www.feed.com/feed2.xml");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news2, news3, news4);
    }

    /*
     * Condition 3a: State is Unread AND Feed is not
     * "http://www.feed.com/feed1.xml"
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, EnumSet.of(State.HIDDEN));

      ISearchField field2 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, "http://www.feed.com/feed1.xml");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news4);
    }

    /*
     * Condition 4: Title contains First AND Feed is not
     * "http://www.feed.com/feed1.xml"
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "First");

      ISearchField field2 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, "http://www.feed.com/feed1.xml");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news3);
    }

    /*
     * Condition 5: State is not *new* AND State is not *unread*
     */
    {
      ISearchField field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, EnumSet.of(INews.State.NEW));

      ISearchField field2 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS_NOT, EnumSet.of(INews.State.UNREAD));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news4);

      /* Variant */
      field1 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, EnumSet.of(INews.State.NEW, INews.State.UNREAD));

      result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news4);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsWithUppercaseInFeedLink() throws Exception {

    /* First add some Types */
    IFeed feed1 = fFactory.createFeed(null, new URI("http://rss.golem.de/rss.php?feed=RSS0.91"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://192.168.158.4:8080/rssFailed"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);

    INews news3 = createNews(feed2, "First News of Feed Two", "http://www.news.com/news3.html", State.NEW);
    INews news4 = createNews(feed2, "Second News of Feed Two", "http://www.news.com/news4.html", State.HIDDEN);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);

    /* Wait for Indexer */
    waitForIndexer();

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "http://rss.golem.de/rss.php?feed=RSS0.91");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.BEGINS_WITH, "http://rss.golem.de/rss.php?feed=RSS0.9");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.BEGINS_WITH, "http://rss.golem.de/rss.php?feed=RSS0");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.BEGINS_WITH, "http://rss.golem.de/rss.php?feed=");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.BEGINS_WITH, "http://rss.golem.de/rss.php?");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "http://192.168.158.4:8080/rssFailed");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news3, news4);
    }

    {
      ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.BEGINS_WITH, "http://192.168.158.4:8080/rssF");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news3, news4);
    }
  }

  /**
   * @throws Exception
   */
  public void testSearchNewsWithMultipleLabels() throws Exception {

    /* First add some Types */
    IFolder rootFolder = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(rootFolder);

    IFolder subFolder = fFactory.createFolder(null, rootFolder, "Sub Folder");
    DynamicDAO.save(subFolder);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed1.com"));

    ILabel label1 = fFactory.createLabel(null, "work");
    ILabel label2 = fFactory.createLabel(null, "important");

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    news1.addLabel(label1);
    news1.addLabel(label2);

    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);
    news2.addLabel(label2);

    DynamicDAO.save(feed1);

    IBookMark rootMark1 = fFactory.createBookMark(null, rootFolder, new FeedLinkReference(feed1.getLink()), "rootMark1");
    DynamicDAO.save(rootMark1);

    /* Wait for Indexer */
    waitForIndexer();

    /* Label IS "work" */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "work");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    /* Label IS "important" */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LABEL, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "important");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsWithLocation() throws Exception {

    /* First add some Types */
    IFolder rootFolder = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(rootFolder);

    IFolder subFolder = fFactory.createFolder(null, rootFolder, "Sub Folder");
    DynamicDAO.save(subFolder);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed1.com"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed2.com"));
    IFeed feed3 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed3.com"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);

    INews news3 = createNews(feed2, "First News of Feed Two", "http://www.news.com/news3.html", State.READ);
    INews news4 = createNews(feed2, "Second News of Feed Two", "http://www.news.com/news4.html", State.NEW);

    INews news5 = createNews(feed3, "First News of Feed Three", "http://www.news.com/news5.html", State.UPDATED);
    INews news6 = createNews(feed3, "Second News of Feed Three", "http://www.news.com/news6.html", State.NEW);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);
    DynamicDAO.save(feed3);

    IBookMark rootMark1 = fFactory.createBookMark(null, rootFolder, new FeedLinkReference(feed1.getLink()), "rootMark1");
    DynamicDAO.save(rootMark1);

    IBookMark subRootMark1 = fFactory.createBookMark(null, subFolder, new FeedLinkReference(feed2.getLink()), "subRootMark1");
    DynamicDAO.save(subRootMark1);

    IBookMark subRootMark2 = fFactory.createBookMark(null, subFolder, new FeedLinkReference(feed3.getLink()), "subRootMark2");
    DynamicDAO.save(subRootMark2);

    /* Wait for Indexer */
    waitForIndexer();

    /* Location IS Root Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2, news3, news4, news5, news6);
    }

    /* Location IS NOT Root Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertTrue(result.isEmpty());
    }

    /* Location IS Sub Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news3, news4, news5, news6);
    }

    /* Location IS NOT Sub Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    /* Location IS Bookmark 1 */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootMark1 })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1, news2);
    }

    /* Location IS NOT Bookmark 1 */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootMark1 })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news3, news4, news5, news6);
    }

    /* Location IS Bookmark1 OR Location IS Bookmark2 */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootMark1 })));

      ISearchField field2 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subRootMark1 })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertSame(result, news1, news2, news3, news4);
    }

    /* Location IS (rootMark1, subMark2) */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootMark1, subRootMark2 })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1, news2, news5, news6);
    }

    /* Location IS NOT (rootMark1, subMark2) */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS_NOT, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootMark1, subRootMark2 })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news3, news4);
    }

    /* Location IS Sub Folder AND State is *new* */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder })));

      ISearchField field2 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news4, news6);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsWithBINLocation() throws Exception {

    /* First add some Types */
    IFolder rootFolder = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(rootFolder);

    IFolder subFolder = fFactory.createFolder(null, rootFolder, "Sub Folder");
    DynamicDAO.save(subFolder);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed1.com"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed2.com"));
    IFeed feed3 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed3.com"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);

    INews news3 = createNews(feed2, "First News of Feed Two", "http://www.news.com/news3.html", State.READ);
    INews news4 = createNews(feed2, "Second News of Feed Two", "http://www.news.com/news4.html", State.NEW);

    INews news5 = createNews(feed3, "First News of Feed Three", "http://www.news.com/news5.html", State.UPDATED);
    INews news6 = createNews(feed3, "Second News of Feed Three", "http://www.news.com/news6.html", State.NEW);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);
    DynamicDAO.save(feed3);

    INewsBin rootBin = fFactory.createNewsBin(null, rootFolder, "Root Bin");
    INewsBin subRootBin = fFactory.createNewsBin(null, subFolder, "Sub Root Bin");

    DynamicDAO.save(rootFolder);
    List<INews> copiedNews = new ArrayList<INews>();
    INews news1Copy = fFactory.createNews(news1, rootBin);
    copiedNews.add(news1Copy);
    INews news2Copy = fFactory.createNews(news2, rootBin);
    copiedNews.add(news2Copy);

    INews news3CopyRoot = fFactory.createNews(news3, rootBin);
    copiedNews.add(news3CopyRoot);
    INews news3CopySubRoot = fFactory.createNews(news3, subRootBin);
    copiedNews.add(news3CopySubRoot);

    INews news4Copy = fFactory.createNews(news4, subRootBin);
    copiedNews.add(news4Copy);
    INews news5Copy = fFactory.createNews(news5, subRootBin);
    copiedNews.add(news5Copy);
    INews news6Copy = fFactory.createNews(news6, subRootBin);
    copiedNews.add(news6Copy);

    DynamicDAO.saveAll(copiedNews);
    DynamicDAO.save(rootBin);
    DynamicDAO.save(subRootBin);

    /* Wait for Indexer */
    waitForIndexer();

    /* Location IS Root Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1Copy, news2Copy, news3CopyRoot, news3CopySubRoot, news4Copy, news5Copy, news6Copy);
    }

    /* Location IS Sub Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news3CopySubRoot, news4Copy, news5Copy, news6Copy);
    }

    /* Location IS Root Bin */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootBin })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertSame(result, news1Copy, news2Copy, news3CopyRoot);
    }

    /* Location IS Root Bin or Sub Root Bin */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootBin })));

      ISearchField field2 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subRootBin })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertSame(result, news1Copy, news2Copy, news3CopyRoot, news3CopySubRoot, news4Copy, news5Copy, news6Copy);
    }

    /* Location IS (Root Bin, Sub Root Bin) */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootBin, subRootBin })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1Copy, news2Copy, news3CopyRoot, news3CopySubRoot, news4Copy, news5Copy, news6Copy);
    }

    /* Location IS Sub Folder AND State is *new* */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder })));

      ISearchField field2 = fFactory.createSearchField(INews.STATE, fNewsEntityName);
      ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news4Copy, news6Copy);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchEntireNewsWith_CONTAINS_Specifier() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news1 = createNews(feed, "This is Radio no (DVD)", "http://www.news.com/news1.html", State.READ);

      INews news2 = createNews(feed, " Bar", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer Radio no (DVD) description with <html><h2>included!</h2></html>");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* All Fields */
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "(DVD)");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
      assertSame(result, news1, news2);

      field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "(DVD)");
      ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "no");

      result = fModelSearch.searchNews(list(condition1, condition2), true);
      assertSame(result, news1, news2);

      field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
      condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "(DVD)");
      condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "no");
      ISearchCondition condition3 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "RadIO");

      result = fModelSearch.searchNews(list(condition1, condition2, condition3), true);
      assertSame(result, news1, news2);
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testEmptyFolderLocationCondition() throws Exception {

    /* First add some Types */
    IFolder rootFolder = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(rootFolder);

    IFolder subFolder = fFactory.createFolder(null, rootFolder, "Sub Folder");
    DynamicDAO.save(subFolder);

    IFolder emptyFolder = fFactory.createFolder(null, rootFolder, "Empty Folder");
    DynamicDAO.save(emptyFolder);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed1.com"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed2.com"));
    IFeed feed3 = fFactory.createFeed(null, new URI("http://www.testSearchNewsWithLocationFeed3.com"));

    createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.UNREAD);
    createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.NEW);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);
    DynamicDAO.save(feed3);

    IBookMark rootMark1 = fFactory.createBookMark(null, rootFolder, new FeedLinkReference(feed1.getLink()), "rootMark1");
    DynamicDAO.save(rootMark1);

    IBookMark subRootMark1 = fFactory.createBookMark(null, subFolder, new FeedLinkReference(feed2.getLink()), "subRootMark1");
    DynamicDAO.save(subRootMark1);

    IBookMark subRootMark2 = fFactory.createBookMark(null, subFolder, new FeedLinkReference(feed3.getLink()), "subRootMark2");
    DynamicDAO.save(subRootMark2);

    /* Wait for Indexer */
    waitForIndexer();

    /* Location IS Empty Folder */
    {
      ISearchField field1 = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { emptyFolder })));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), true);
      assertEquals(0, result.size());
    }
  }

  private INews createNews(IFeed feed, String title, String link, INews.State state) throws URISyntaxException {
    INews news = fFactory.createNews(null, feed, new Date(System.currentTimeMillis()));
    news.setState(state);
    news.setLink(new URI(link));
    news.setTitle(title);

    return news;
  }

  private void waitForIndexer() throws InterruptedException {
    Thread.sleep(500);
  }

  private List<ISearchCondition> list(ISearchCondition... condition) {
    return new ArrayList<ISearchCondition>(Arrays.asList(condition));
  }

  private void assertSame(List<SearchHit<NewsReference>> result, INews... news) {
    if (result.size() != news.length)
      fail("Results don't have the same number of Elements (" + news.length + " expected, " + result.size() + " actual)!");

    for (INews newsitem : news) {
      boolean found = false;
      for (SearchHit<NewsReference> hit : result) {
        if (hit.getResult().getId() == newsitem.getId()) {
          found = true;
          break;
        }
      }

      assertEquals(true, found);
    }
  }
}