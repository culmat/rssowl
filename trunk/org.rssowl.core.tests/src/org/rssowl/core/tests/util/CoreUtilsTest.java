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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.internal.util.ModelUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests methods in {@link CoreUtils}.
 *
 * @author bpasero
 */
public class CoreUtilsTest {
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

  /**
   * @throws Exception
   */
  @Test
  public void testGenerateNameForSearch() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    ISearchField field1 = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchField field2 = fFactory.createSearchField(INews.LOCATION, INews.class.getName());
    ISearchField field3 = fFactory.createSearchField(INews.IS_FLAGGED, INews.class.getName());
    ISearchField field4 = fFactory.createSearchField(INews.PUBLISH_DATE, INews.class.getName());
    ISearchField field5 = fFactory.createSearchField(INews.STATE, INews.class.getName());

    ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo bar");
    ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root)));
    ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS, true);
    ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS, new Date());
    ISearchCondition cond5 = fFactory.createSearchCondition(field5, SearchSpecifier.IS, INews.State.getVisible());

    String name = CoreUtils.getName(Arrays.asList(new ISearchCondition[] { cond1, cond2, cond3, cond4, cond5 }), true);
    assertNotNull(name);
    assertTrue(name.contains("foo bar"));
    assertTrue(name.contains("Root"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetHeadline() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());

    assertNotNull(CoreUtils.getHeadline(news1, false));

    news1.setDescription("Foo Bar");
    assertEquals("Foo Bar", CoreUtils.getHeadline(news1, false));

    news1.setDescription("Foo &auml; Bar");
    assertEquals("Foo &auml; Bar", CoreUtils.getHeadline(news1, false));
    assertEquals("Foo ä Bar", CoreUtils.getHeadline(news1, true));

    news1.setTitle("A Foo Bar");
    assertEquals("A Foo Bar", CoreUtils.getHeadline(news1, false));

    news1.setTitle("A Foo &auml; Bar");
    assertEquals("A Foo &auml; Bar", CoreUtils.getHeadline(news1, false));
    assertEquals("A Foo ä Bar", CoreUtils.getHeadline(news1, true));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetLink() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());

    assertNull(CoreUtils.getLink(news1));

    IGuid guid = fFactory.createGuid(news1, "www.guid.de", false);
    news1.setGuid(guid);
    assertEquals("www.guid.de", CoreUtils.getLink(news1));

    news1.setLink(new URI("link"));
    assertEquals("link", CoreUtils.getLink(news1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStickyStateChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStickyStateChange(events));
            assertTrue(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertFalse(CoreUtils.isStickyStateChange(events));
            assertFalse(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertTrue(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStickyStateChange(events));
            assertFalse(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setFlagged(true);
      DynamicDAO.save(news);

      mode.set(1);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(2);
      news.setFlagged(false);
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStateStateChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isStateChange(events.iterator().next()));
            assertFalse(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertTrue(CoreUtils.isStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.READ);
      DynamicDAO.save(news);

      mode.set(1);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(2);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGotDeleted() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertFalse(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.READ);
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.HIDDEN);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGotRestored() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.gotRestored(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.HIDDEN);
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLabelChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isLabelChange(events.iterator().next()));
            assertTrue(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.isLabelChange(events.iterator().next()));
            assertTrue(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertFalse(CoreUtils.isLabelChange(events.iterator().next()));
            assertFalse(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      ILabel label = DynamicDAO.save(fFactory.createLabel(null, "Label"));
      news.addLabel(label);
      DynamicDAO.save(news);

      mode.set(1);
      news.removeLabel(label);
      DynamicDAO.save(news);

      mode.set(2);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTitleChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isTitleChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isTitleChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(1);
      news.setDescription("Bar");
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFeedLinks() throws Exception {
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed1")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed2")));
    DynamicDAO.save(fFactory.createFeed(null, new URI("feed3")));

    IFolder root = fFactory.createFolder(null, null, "root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed1.getLink()), "Mark 1");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 2");

    DynamicDAO.save(root);

    Set<String> feedLinks = CoreUtils.getFeedLinks();
    assertEquals(2, feedLinks.size());
    assertTrue(feedLinks.contains("feed1"));
    assertTrue(feedLinks.contains("feed2"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetBookMark() throws Exception {
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed1")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed2")));

    IFolder root = fFactory.createFolder(null, null, "root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed1.getLink()), "Mark 1");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 2");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 3");

    DynamicDAO.save(root);

    assertEquals("Mark 1", CoreUtils.getBookMark(new FeedLinkReference(feed1.getLink())).getName());
    assertNotNull(CoreUtils.getBookMark(new FeedLinkReference(feed2.getLink())));
    assertNull(CoreUtils.getBookMark(new FeedLinkReference(new URI("feed3"))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExtractWords() throws Exception {
    ISearchField field1 = fFactory.createSearchField(INews.TITLE, INews.class.getName());
    ISearchField field2 = fFactory.createSearchField(INews.DESCRIPTION, INews.class.getName());
    ISearchField field3 = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();
    conditions.add(fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo bar"));
    conditions.add(fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "benjamin ?asero"));
    conditions.add(fFactory.createSearchCondition(field3, SearchSpecifier.CONTAINS, "see the code"));

    Set<String> words = CoreUtils.extractWords(conditions, false);
    assertEquals(7, words.size());
    assertTrue(words.containsAll(Arrays.asList(new String[] { "foo", "bar", "benjamin", "?asero", "see", "the", "code" })));

    words = CoreUtils.extractWords(conditions, true);
    assertEquals(6, words.size());
    assertTrue(words.containsAll(Arrays.asList(new String[] { "foo", "bar", "benjamin", "?asero", "see", "code" })));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testIsEmpty() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));

    INews news = fFactory.createNews(null, feed, new Date());
    assertTrue(CoreUtils.isEmpty(news));

    news.setTitle("Foo");
    assertTrue(CoreUtils.isEmpty(news));

    news.setDescription("Bar");
    assertFalse(CoreUtils.isEmpty(news));

    news.setDescription("Foo");
    assertTrue(CoreUtils.isEmpty(news));
  }

  /**
   * @throws Exception
   */
  @Test
  public void loadSortedSearchMarks() throws Exception {
    IFolder root = fFactory.createFolder(null, null, "Root");
    fFactory.createSearchMark(null, root, "C Search");
    fFactory.createSearchMark(null, root, "A Search");
    fFactory.createSearchMark(null, root, "B Search");

    DynamicDAO.save(root);

    Set<ISearchMark> searches = CoreUtils.loadSortedSearchMarks();
    Iterator<ISearchMark> iterator = searches.iterator();
    assertEquals("A Search", iterator.next().getName());
    assertEquals("B Search", iterator.next().getName());
    assertEquals("C Search", iterator.next().getName());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testContainsState() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.containsState(events, INews.State.NEW));
            assertFalse(CoreUtils.containsState(events, INews.State.READ));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.containsState(events, INews.State.READ));
            assertFalse(CoreUtils.containsState(events, INews.State.NEW));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.READ);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSplitScope() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.TITLE, INews.class.getName()), SearchSpecifier.CONTAINS, "foo"));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root))));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Collections.singletonList(root))));

    Pair<ISearchCondition, List<ISearchCondition>> result = CoreUtils.splitScope(conditions);
    assertNotNull(result.getFirst());
    assertEquals(2, result.getSecond().size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testIsLocationConflict() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.TITLE, INews.class.getName()), SearchSpecifier.CONTAINS, "foo"));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root))));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Collections.singletonList(root))));

    assertTrue(CoreUtils.isLocationConflict(conditions));

    conditions.remove(1);
    assertFalse(CoreUtils.isLocationConflict(conditions));

    conditions.clear();
    assertFalse(CoreUtils.isLocationConflict(conditions));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReparentWithProperties() throws Exception {
    IFolder root = fFactory.createFolder(null, null, "Root");

    IFolder child1 = fFactory.createFolder(null, root, "Child 1");
    IFolder child2 = fFactory.createFolder(null, root, "Child 2");
    child2.setProperty("foo", "bar");
    IFolder child3 = fFactory.createFolder(null, child2, "Child 3");

    root = DynamicDAO.save(root);

    ReparentInfo<IFolderChild, IFolder> info = ReparentInfo.create((IFolderChild) child1, child2, child3, true);

    CoreUtils.reparentWithProperties(Collections.singletonList(info));

    assertEquals("bar", child1.getProperty("foo"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExtractLinksFromText() throws Exception {
    List<String> links = RegExUtils.extractLinksFromText("", false);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("foo bar", true);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("this is a www.rssowl.org short link to www.google.com as well", false);
    assertEquals(2, links.size());
    assertTrue(links.containsAll(Arrays.asList(new String[] { "www.rssowl.org", "www.google.com" })));

    links = RegExUtils.extractLinksFromText("this is a www.rssowl.org short link to www.google.com as well", true);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("this is a http://www.rssowl.org short link to http://www.google.com as well", true);
    assertEquals(2, links.size());
    assertTrue(links.containsAll(Arrays.asList(new String[] { "http://www.rssowl.org", "http://www.google.com" })));
  }
}