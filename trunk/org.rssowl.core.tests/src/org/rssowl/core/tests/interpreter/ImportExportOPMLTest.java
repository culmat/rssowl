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

package org.rssowl.core.tests.interpreter;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.interpreter.ITypeExporter.Options;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests a full export and import using OPML including Folders, Bookmarks, Saved
 * Searches and News Bins. For saved searches most combinations of Field,
 * Specifier and Value are used to make sure everything works as expected. Also
 * tested is import export of a OPML file from a backup containing Labels and
 * Filters.
 *
 * @author bpasero
 */
public class ImportExportOPMLTest {
  private File fTmpFile;
  private File fTmpBackupFile;
  private IModelFactory fFactory;
  private IFolder fDefaultSet;
  private IFolder fCustomSet;
  private IFolder fDefaultFolder1;
  private IBookMark fBookMark1;
  private IFolder fDefaultFolder2;
  private IFolder fCustomFolder2;
  private INewsBin fNewsBin;
  private ILabel fImportantLabel;
  private ISearchMark fSearchmark;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();

    fFactory = Owl.getModelFactory();
    fTmpFile = File.createTempFile("rssowl", "opml");
    fTmpFile.deleteOnExit();

    fTmpBackupFile = File.createTempFile("rssowl_backup", "opml");
    fTmpBackupFile.deleteOnExit();

    /* Fill Defaults */
    fillDefaults();
    DynamicDAO.getDAO(IFolderDAO.class).save(fDefaultSet);
    DynamicDAO.getDAO(IFolderDAO.class).save(fCustomSet);

    /* Export */
    Set<IFolder> rootFolders = new HashSet<IFolder>();
    rootFolders.add(fDefaultSet);
    rootFolders.add(fCustomSet);

    Owl.getInterpreter().exportTo(fTmpFile, rootFolders, null);
    Owl.getInterpreter().exportTo(fTmpBackupFile, rootFolders, EnumSet.of(Options.EXPORT_FILTERS, Options.EXPORT_LABELS, Options.EXPORT_PREFERENCES));

    /* Clear */
    Owl.getPersistenceService().recreateSchema();

    /* Add Default Set */
    DynamicDAO.getDAO(IFolderDAO.class).save(fFactory.createFolder(null, null, "My Bookmarks"));
  }

  private void fillDefaults() throws URISyntaxException {

    /* Set: Default */
    fillDefaultSet();

    /* Set: Custom */
    fillCustomSet();

    DynamicDAO.getDAO(IFolderDAO.class).save(fDefaultSet);
    DynamicDAO.getDAO(IFolderDAO.class).save(fCustomSet);

    /* Default > List of SearchMarks */
    fillSearchMarks(fDefaultSet);

    /* Default > Folder 2 > List of SearchMarks */
    fillSearchMarks(fDefaultFolder2);

    /* Custom > List of SearchMarks */
    fillSearchMarks(fCustomSet);

    /* Custom > Folder 2 > List of SearchMarks */
    fillSearchMarks(fCustomFolder2);

    /* Labels */
    fillLabels();

    /* Filters */
    fillFilters();
  }

  private void fillFilters() {

    /* 1) Match All News - Enabled - Mark Read */
    ISearchFilter filter = fFactory.createSearchFilter(null, null, "Filter 1");
    filter.setMatchAllNews(true);
    filter.setEnabled(true);
    filter.setOrder(5);
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkReadNewsAction"));
    DynamicDAO.save(filter);

    /* 2) Match All News - Disabled - Mark Read + Mark Sticky */
    filter = fFactory.createSearchFilter(null, null, "Filter 2");
    filter.setMatchAllNews(true);
    filter.setEnabled(false);
    filter.setOrder(0);
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkReadNewsAction"));
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkStickyNewsAction"));
    DynamicDAO.save(filter);

    /* 3) Entire News contains "Foo" - Enabled - Mark Read */
    ISearch search = fFactory.createSearch(null);
    ISearchField entireNewsField = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    search.addSearchCondition(fFactory.createSearchCondition(entireNewsField, SearchSpecifier.CONTAINS, "Foo"));
    filter = fFactory.createSearchFilter(null, search, "Filter 3");
    filter.setMatchAllNews(false);
    filter.setOrder(3);
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkReadNewsAction"));
    DynamicDAO.save(filter);

    /* 4) Entire News contains "Foo" or "Bar" - Enabled - Mark Read */
    search = fFactory.createSearch(null);
    search.setMatchAllConditions(true);
    search.addSearchCondition(fFactory.createSearchCondition(entireNewsField, SearchSpecifier.CONTAINS, "Foo"));
    search.addSearchCondition(fFactory.createSearchCondition(entireNewsField, SearchSpecifier.CONTAINS, "Bar"));
    filter = fFactory.createSearchFilter(null, search, "Filter 4");
    filter.setMatchAllNews(false);
    filter.setOrder(4);
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkReadNewsAction"));
    DynamicDAO.save(filter);

    /* 5) Location is "XY" - Enabled - Mark Read */
    search = fFactory.createSearch(null);
    ISearchField locationField = fFactory.createSearchField(INews.LOCATION, INews.class.getName());
    search.addSearchCondition(fFactory.createSearchCondition(locationField, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fBookMark1, fNewsBin }))));
    filter = fFactory.createSearchFilter(null, search, "Filter 5");
    filter.setMatchAllNews(false);
    filter.setOrder(8);
    filter.addAction(fFactory.createFilterAction("org.rssowl.core.MarkReadNewsAction"));
    DynamicDAO.save(filter);

    /* 6) Match All News - Enabled - Label News */
    filter = fFactory.createSearchFilter(null, null, "Filter 6");
    filter.setMatchAllNews(true);
    filter.setOrder(5);
    IFilterAction action = fFactory.createFilterAction(LabelNewsAction.ID);
    action.setData(fImportantLabel.getId());
    filter.addAction(action);
    DynamicDAO.save(filter);

    /* 7) Match All News - Enabled - Label News + Move News + Play Sound */
    filter = fFactory.createSearchFilter(null, null, "Filter 7");
    filter.setMatchAllNews(true);
    filter.setOrder(5);
    action = fFactory.createFilterAction(LabelNewsAction.ID);
    action.setData(fImportantLabel.getId());
    filter.addAction(action);

    action = fFactory.createFilterAction(MoveNewsAction.ID);
    action.setData(new Long[] { fNewsBin.getId() });
    filter.addAction(action);

    action = fFactory.createFilterAction("org.rssowl.ui.PlaySoundAction");
    action.setData("C:\\ProgramData\\Microsoft\\Windows & Help\\Start Menu");
    filter.addAction(action);

    DynamicDAO.save(filter);
  }

  private void fillLabels() {
    ILabel label = fFactory.createLabel(null, "Later");
    label.setColor("113,21,88");
    label.setOrder(4);
    DynamicDAO.save(label);

    label = fFactory.createLabel(null, "Personal");
    label.setColor("105,130,73");
    label.setOrder(3);
    DynamicDAO.save(label);

    fImportantLabel = fFactory.createLabel(null, "Important");
    fImportantLabel.setColor("177,39,52");
    fImportantLabel.setOrder(2);
    DynamicDAO.save(fImportantLabel);

    label = fFactory.createLabel(null, "Work");
    label.setColor("234,152,79");
    label.setOrder(1);
    DynamicDAO.save(label);

    label = fFactory.createLabel(null, "To Do");
    label.setColor("113,160,168");
    label.setOrder(0);
    DynamicDAO.save(label);
  }

  private void fillDefaultSet() throws URISyntaxException {
    fDefaultSet = fFactory.createFolder(null, null, "My Bookmarks");

    fDefaultFolder1 = fFactory.createFolder(null, fDefaultSet, "Default Folder 1");
    addProperties(fDefaultFolder1);

    fDefaultFolder2 = fFactory.createFolder(null, fDefaultSet, "Default Folder 2");

    /* Default > BookMark 1 */
    IFeed feed1 = fFactory.createFeed(null, new URI("feed1"));
    fBookMark1 = fFactory.createBookMark(null, fDefaultSet, new FeedLinkReference(feed1.getLink()), "Bookmark 1");
    addProperties(fBookMark1);

    /* Default > Folder 1 > BookMark 3 */
    IFeed feed3 = fFactory.createFeed(null, new URI("feed3"));
    fFactory.createBookMark(null, fDefaultFolder1, new FeedLinkReference(feed3.getLink()), "Bookmark 3");

    /* Default > News Bin 1 */
    fNewsBin = fFactory.createNewsBin(null, fDefaultSet, "Bin 1");
    addProperties(fNewsBin);
  }

  private void addProperties(IFolderChild child) {
    IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(child);
    prefs.putBoolean("boolean", true);
    prefs.putInteger("integer", 5);
    prefs.putIntegers("integers", new int[] { -1, 0, 1, 2, 3 });
    prefs.putLong("long", 8);
    prefs.putLongs("longs", new long[] { -3, -2, -1, 0, 1, 2, 3 });
    prefs.putString("string", "hello world");
    prefs.putStrings("strings", new String[] { "hello", "world", "foo", "bar" });
  }

  private void assertProperties(IFolderChild child) {
    IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(child);
    assertEquals(true, prefs.getBoolean("boolean"));
    assertEquals(5, prefs.getInteger("integer"));
    assertTrue(Arrays.equals(new int[] { -1, 0, 1, 2, 3 }, prefs.getIntegers("integers")));
    assertEquals(8, prefs.getLong("long"));
    assertTrue(Arrays.equals(new long[] { -3, -2, -1, 0, 1, 2, 3 }, prefs.getLongs("longs")));
    assertEquals("hello world", prefs.getString("string"));
    assertTrue(Arrays.equals(new String[] { "hello", "world", "foo", "bar" }, prefs.getStrings("strings")));
  }

  private void fillCustomSet() throws URISyntaxException {
    fCustomSet = fFactory.createFolder(null, null, "Custom");

    /* Custom > Folder 1 */
    IFolder folder1 = fFactory.createFolder(null, fCustomSet, "Custom Folder 1");

    fCustomFolder2 = fFactory.createFolder(null, fCustomSet, "Custom Folder 2");

    /* Custom > BookMark 2 */
    IFeed feed2 = fFactory.createFeed(null, new URI("feed2"));
    fFactory.createBookMark(null, fCustomSet, new FeedLinkReference(feed2.getLink()), "Bookmark 2");

    /* Custom > Folder 1 > BookMark 4 */
    IFeed feed4 = fFactory.createFeed(null, new URI("feed4"));
    fFactory.createBookMark(null, folder1, new FeedLinkReference(feed4.getLink()), "Bookmark 4");

  }

  private void fillSearchMarks(IFolder parent) {
    String newsName = INews.class.getName();

    /* 1) State ISnew */
    {
      ISearchField field = fFactory.createSearchField(INews.STATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW));

      fSearchmark = fFactory.createSearchMark(null, parent, "Search");
      fSearchmark.addSearchCondition(condition);
      addProperties(fSearchmark);
    }

    /* 2) State ISnewunreadupdated */
    {
      ISearchField field = fFactory.createSearchField(INews.STATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 3) Entire News CONTAINS foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 4) Age in Days is > 5 */
    {
      ISearchField field = fFactory.createSearchField(INews.AGE_IN_DAYS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 5);

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 5) Publish Date is 26.12.1981 */
    {
      Calendar cal = DateUtils.getToday();
      cal.set(Calendar.YEAR, 1981);
      cal.set(Calendar.MONTH, Calendar.DECEMBER);
      cal.set(Calendar.DATE, 26);

      ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, cal.getTime());

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 6) Feed Links is not http://www.rssowl.org/node/feed */
    {
      ISearchField field = fFactory.createSearchField(INews.FEED, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.rssowl.org/node/feed");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 7) Has Attachments is TRUE */
    {
      ISearchField field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /*
     * 8) Entire News CONTAINS foo?bar AND State ISnew AND Has Attachments is
     * TRUE
     */
    {
      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");

      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo?bar");
      searchmark.addSearchCondition(condition);

      field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, newsName);
      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);
      searchmark.addSearchCondition(condition);

      field = fFactory.createSearchField(INews.STATE, newsName);
      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW));
      searchmark.addSearchCondition(condition);

      searchmark.setMatchAllConditions(true);
    }

    /* 9) Location is Default Set */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 10) Location is Default Set OR Location is Custom Set */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet, fCustomSet })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 11) Location is Folder 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultFolder1 })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 12) Location is BookMark 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fBookMark1 })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /*
     * 13) Location is Default Set OR Location is Custom Set OR Location is
     * BookMark1
     */
    {
      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");

      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);

      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet })));
      searchmark.addSearchCondition(condition);

      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fCustomSet })));
      searchmark.addSearchCondition(condition);

      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fBookMark1 })));
      searchmark.addSearchCondition(condition);
    }

    /* 14) Location is Bin 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fNewsBin })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 15) Entire News CONTAINS_ALL foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 16) Entire News CONTAINS_NOT foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null" })
  public void testExportImportCompleteOPML() throws Exception {
    exportImportCompleteOPML(false);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null" })
  public void testExportImportCompleteBackupOPML() throws Exception {

    /* Pre-Create some Labels for Testing merge behavior */
    ILabel label = fFactory.createLabel(null, "Later");
    label.setColor("113,21,88");
    label.setOrder(0);
    DynamicDAO.save(label);

    label = fFactory.createLabel(null, "Personal");
    label.setColor("0,0,0");
    label.setOrder(1);
    DynamicDAO.save(label);

    exportImportCompleteOPML(true);
  }

  @SuppressWarnings( { "nls", "null" })
  private void exportImportCompleteOPML(boolean useBackup) throws Exception {

    /* Import */
    ImportUtils.importFeeds(useBackup ? fTmpBackupFile.getAbsolutePath() : fTmpFile.getAbsolutePath());

    /* Validate */
    Collection<IFolder> rootFolders = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();

    assertEquals(2, rootFolders.size());

    IFolder defaultSet = null;
    IFolder customSet = null;
    for (IFolder rootFolder : rootFolders) {
      if (rootFolder.getName().equals("My Bookmarks"))
        defaultSet = rootFolder;
      else if (rootFolder.getName().equals("Custom"))
        customSet = rootFolder;
    }

    assertNotNull(defaultSet);
    assertNotNull(customSet);

    List<IFolder> defaultFolders = defaultSet.getFolders();
    assertEquals(2, defaultFolders.size());

    IFolder defaultFolder1 = null;
    IFolder defaultFolder2 = null;

    for (IFolder defaultFolder : defaultFolders) {
      if (defaultFolder.getName().equals("Default Folder 1"))
        defaultFolder1 = defaultFolder;
      else if (defaultFolder.getName().equals("Default Folder 2"))
        defaultFolder2 = defaultFolder;
    }

    if (useBackup)
      assertProperties(defaultFolder1);

    assertNotNull(defaultFolder1);
    assertNotNull(defaultFolder2);

    List<IFolder> customFolders = customSet.getFolders();
    assertEquals(2, customFolders.size());

    IFolder customFolder1 = null;
    IFolder customFolder2 = null;

    for (IFolder customFolder : customFolders) {
      if (customFolder.getName().equals("Custom Folder 1"))
        customFolder1 = customFolder;
      else if (customFolder.getName().equals("Custom Folder 2"))
        customFolder2 = customFolder;
    }

    assertNotNull(customFolder1);
    assertNotNull(customFolder2);

    List<IMark> defaultMarks = defaultSet.getMarks();
    assertEquals(18, defaultMarks.size());

    IBookMark bookmark1 = null;
    for (IMark mark : defaultMarks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 1"))
        bookmark1 = (IBookMark) mark;
    }



    assertNotNull(bookmark1);
    assertEquals("feed1", bookmark1.getFeedLinkReference().getLink().toString());
    if (useBackup)
      assertProperties(bookmark1);

    INewsBin bin = null;
    for (IMark mark : defaultMarks) {
      if (mark instanceof INewsBin && mark.getName().equals("Bin 1"))
        bin = (INewsBin) mark;
    }

    assertNotNull(bin);
    if (useBackup)
      assertProperties(bin);

    List<IMark> customMarks = customSet.getMarks();
    assertEquals(17, customMarks.size());

    IBookMark bookmark2 = null;
    for (IMark mark : customMarks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 2"))
        bookmark2 = (IBookMark) mark;
    }

    assertNotNull(bookmark2);
    assertEquals("feed2", bookmark2.getFeedLinkReference().getLink().toString());

    List<IMark> marks = defaultFolder1.getMarks();
    assertEquals(1, marks.size());

    IBookMark bookmark3 = null;
    for (IMark mark : marks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 3"))
        bookmark3 = (IBookMark) mark;
    }

    assertNotNull(bookmark3);
    assertEquals("feed3", bookmark3.getFeedLinkReference().getLink().toString());

    marks = customFolder1.getMarks();
    assertEquals(1, marks.size());

    IBookMark bookmark4 = null;
    for (IMark mark : marks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 4"))
        bookmark4 = (IBookMark) mark;
    }

    assertNotNull(bookmark4);
    assertEquals("feed4", bookmark4.getFeedLinkReference().getLink().toString());

    assertSearchMarks(defaultSet, useBackup);
    assertSearchMarks(customSet, useBackup);
    assertSearchMarks(defaultFolder2, useBackup);
    assertSearchMarks(customFolder2, useBackup);

    if (useBackup) {
      assertLabels();
      assertFilters();
    }
  }

  private void assertFilters() {
    Collection<ISearchFilter> filters = DynamicDAO.loadAll(ISearchFilter.class);
    assertEquals(7, filters.size());

    for (ISearchFilter filter : filters) {
      if ("Filter 1".equals(filter.getName())) {
        assertEquals(true, filter.isEnabled());
        assertNull(filter.getSearch());
        assertTrue(filter.matchAllNews());
        assertEquals(5, filter.getOrder());
        assertEquals(1, filter.getActions().size());
        assertEquals("org.rssowl.core.MarkReadNewsAction", filter.getActions().get(0).getActionId());
        assertNull(filter.getActions().get(0).getData());
      }

      else if ("Filter 2".equals(filter.getName())) {
        assertEquals(false, filter.isEnabled());
        assertNull(filter.getSearch());
        assertTrue(filter.matchAllNews());
        assertEquals(0, filter.getOrder());
        assertEquals(2, filter.getActions().size());
        assertEquals("org.rssowl.core.MarkReadNewsAction", filter.getActions().get(0).getActionId());
        assertNull(filter.getActions().get(0).getData());
        assertEquals("org.rssowl.core.MarkStickyNewsAction", filter.getActions().get(1).getActionId());
        assertNull(filter.getActions().get(1).getData());
      }

      else if ("Filter 3".equals(filter.getName())) {
        assertNotNull(filter.getSearch());
        assertEquals(false, filter.getSearch().matchAllConditions());
        assertEquals(1, filter.getSearch().getSearchConditions().size());
        ISearchCondition cond = filter.getSearch().getSearchConditions().get(0);
        assertEquals(IEntity.ALL_FIELDS, cond.getField().getId());
        assertEquals(INews.class.getName(), cond.getField().getEntityName());
        assertEquals(SearchSpecifier.CONTAINS, cond.getSpecifier());
        assertEquals("Foo", cond.getValue());

        assertEquals(false, filter.matchAllNews());
        assertEquals(3, filter.getOrder());
        assertEquals(1, filter.getActions().size());
        assertEquals("org.rssowl.core.MarkReadNewsAction", filter.getActions().get(0).getActionId());
        assertNull(filter.getActions().get(0).getData());
      }

      else if ("Filter 4".equals(filter.getName())) {
        assertNotNull(filter.getSearch());
        assertEquals(true, filter.getSearch().matchAllConditions());
        assertEquals(2, filter.getSearch().getSearchConditions().size());
        ISearchCondition cond1 = filter.getSearch().getSearchConditions().get(0);
        assertEquals(IEntity.ALL_FIELDS, cond1.getField().getId());
        assertEquals(INews.class.getName(), cond1.getField().getEntityName());
        assertEquals(SearchSpecifier.CONTAINS, cond1.getSpecifier());
        assertEquals("Foo", cond1.getValue());

        ISearchCondition cond2 = filter.getSearch().getSearchConditions().get(1);
        assertEquals(IEntity.ALL_FIELDS, cond2.getField().getId());
        assertEquals(INews.class.getName(), cond2.getField().getEntityName());
        assertEquals(SearchSpecifier.CONTAINS, cond2.getSpecifier());
        assertEquals("Bar", cond2.getValue());

        assertEquals(false, filter.matchAllNews());
        assertEquals(4, filter.getOrder());
        assertEquals(1, filter.getActions().size());
        assertEquals("org.rssowl.core.MarkReadNewsAction", filter.getActions().get(0).getActionId());
        assertNull(filter.getActions().get(0).getData());
      }

      else if ("Filter 5".equals(filter.getName())) {
        assertNotNull(filter.getSearch());
        assertEquals(1, filter.getSearch().getSearchConditions().size());
        ISearchCondition cond = filter.getSearch().getSearchConditions().get(0);
        assertEquals(INews.LOCATION, cond.getField().getId());
        assertEquals(INews.class.getName(), cond.getField().getEntityName());
        assertEquals(SearchSpecifier.SCOPE, cond.getSpecifier());

        List<IFolderChild> locations = CoreUtils.toEntities((Long[][]) cond.getValue());
        assertEquals(2, locations.size());
        for (IFolderChild location : locations) {
          if (!fBookMark1.getName().equals(location.getName()) && !fNewsBin.getName().equals(location.getName()))
            fail("Unexpected location: " + location.getName());
        }
      }

      else if ("Filter 6".equals(filter.getName())) {
        assertEquals(1, filter.getActions().size());
        assertEquals(LabelNewsAction.ID, filter.getActions().get(0).getActionId());
        Object data = filter.getActions().get(0).getData();
        assertNotNull(data);
        assertEquals(true, data instanceof Long);
        ILabel label = DynamicDAO.getDAO(ILabelDAO.class).load(((Long) data).longValue());
        assertNotNull(label);
        assertEquals(fImportantLabel.getName(), label.getName());
      }

      else if ("Filter 7".equals(filter.getName())) {
        assertEquals(3, filter.getActions().size());
        assertEquals(LabelNewsAction.ID, filter.getActions().get(0).getActionId());
        Object data = filter.getActions().get(0).getData();
        assertNotNull(data);
        assertEquals(true, data instanceof Long);
        ILabel label = DynamicDAO.getDAO(ILabelDAO.class).load(((Long) data).longValue());
        assertNotNull(label);
        assertEquals(fImportantLabel.getName(), label.getName());

        assertEquals(MoveNewsAction.ID, filter.getActions().get(1).getActionId());
        data = filter.getActions().get(1).getData();
        assertNotNull(data);
        assertEquals(true, data instanceof Long[]);
        assertEquals(1, ((Long[]) data).length);
        INewsBin bin = DynamicDAO.getDAO(INewsBinDAO.class).load(((Long[]) data)[0].longValue());
        assertNotNull(bin);
        assertEquals(fNewsBin.getName(), bin.getName());

        assertEquals("org.rssowl.ui.PlaySoundAction", filter.getActions().get(2).getActionId());
        data = filter.getActions().get(2).getData();
        assertNotNull(data);
        assertEquals("C:\\ProgramData\\Microsoft\\Windows & Help\\Start Menu", data);
      }

      else
        fail("Unexpected Filter found with name: " + filter.getName());
    }
  }

  private void assertLabels() {
    Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
    assertEquals(5, labels.size());
    for (ILabel label : labels) {
      if ("Later".equals(label.getName())) {
        assertEquals("113,21,88", label.getColor());
        assertEquals(4, label.getOrder());
      } else if ("Personal".equals(label.getName())) {
        assertEquals("105,130,73", label.getColor());
        assertEquals(3, label.getOrder());
      } else if ("Important".equals(label.getName())) {
        assertEquals("177,39,52", label.getColor());
        assertEquals(2, label.getOrder());
        label.setColor("177,39,52");
      } else if ("Work".equals(label.getName())) {
        assertEquals("234,152,79", label.getColor());
        assertEquals(1, label.getOrder());
      } else if ("To Do".equals(label.getName())) {
        assertEquals("113,160,168", label.getColor());
        assertEquals(0, label.getOrder());
      } else
        fail("Unexpected Label found with name: " + label.getName());
    }
  }

  private void assertSearchMarks(IFolder folder, boolean isBackup) {
    List<IMark> marks = folder.getMarks();
    List<ISearchMark> searchmarks = new ArrayList<ISearchMark>();
    for (IMark mark : marks) {
      if (mark instanceof ISearchMark)
        searchmarks.add((ISearchMark) mark);
    }

    /* 1) State ISnew */
    ISearchMark searchmark = searchmarks.get(0);
    assertEquals("Search", searchmark.getName());
    List<ISearchCondition> conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.STATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(EnumSet.of(INews.State.NEW), conditions.get(0).getValue());
    if (isBackup)
      assertProperties(searchmark);

    /* 2) State ISnewunreadupdated */
    searchmark = searchmarks.get(1);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.STATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED), conditions.get(0).getValue());

    /* 3) Entire News CONTAINS foo?bar */
    searchmark = searchmarks.get(2);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());

    /* 4) Age in Days is > 5 */
    searchmark = searchmarks.get(3);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.AGE_IN_DAYS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS_GREATER_THAN, conditions.get(0).getSpecifier());
    assertEquals(5, conditions.get(0).getValue());

    /* 5) Publish Date is 26.12.1981 */
    Calendar cal = DateUtils.getToday();
    cal.set(Calendar.YEAR, 1981);
    cal.set(Calendar.MONTH, Calendar.DECEMBER);
    cal.set(Calendar.DATE, 26);

    searchmark = searchmarks.get(4);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.PUBLISH_DATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(cal.getTime(), conditions.get(0).getValue());

    /* 6) Feed Links is not http://www.rssowl.org/node/feed */
    searchmark = searchmarks.get(5);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.FEED, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS_NOT, conditions.get(0).getSpecifier());
    assertEquals("http://www.rssowl.org/node/feed", conditions.get(0).getValue());

    /* 7) Has Attachments is TRUE */
    searchmark = searchmarks.get(6);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.HAS_ATTACHMENTS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(true, conditions.get(0).getValue());

    /*
     * 8) Entire News CONTAINS foo?bar AND State ISnew AND Has Attachments is
     * TRUE
     */
    searchmark = searchmarks.get(7);
    conditions = searchmark.getSearchConditions();
    assertEquals(3, conditions.size());
    assertEquals(true, searchmark.matchAllConditions());

    for (ISearchCondition condition : conditions) {
      switch (condition.getField().getId()) {
        case IEntity.ALL_FIELDS:
          assertEquals(SearchSpecifier.CONTAINS, condition.getSpecifier());
          assertEquals("foo?bar", condition.getValue());
          break;

        case INews.STATE:
          assertEquals(SearchSpecifier.IS, condition.getSpecifier());
          assertEquals(EnumSet.of(INews.State.NEW), condition.getValue());
          break;

        case INews.HAS_ATTACHMENTS:
          assertEquals(SearchSpecifier.IS, condition.getSpecifier());
          assertEquals(true, condition.getValue());
          break;

        default:
          fail();
      }
    }

    /* 9) Location is Default Set */
    searchmark = searchmarks.get(8);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(Arrays.asList(new IFolderChild[] { fDefaultSet }), CoreUtils.toEntities((Long[][]) conditions.get(0).getValue()));

    /* 10) Location is Default Set OR Location is Custom Set */
    searchmark = searchmarks.get(9);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    List<IFolderChild> locations = CoreUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(2, locations.size());
    assertContains("My Bookmarks", locations);
    assertContains("Custom", locations);

    /* 11) Location is Folder 1 */
    searchmark = searchmarks.get(10);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = CoreUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof IFolder);
    assertEquals("Default Folder 1", locations.get(0).getName());

    /* 12) Location is BookMark 1 */
    searchmark = searchmarks.get(11);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = CoreUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof IBookMark);
    assertEquals("Bookmark 1", locations.get(0).getName());

    /*
     * 13) Location is Default Set OR Location is Custom Set OR Location is
     * BookMark1
     */
    searchmark = searchmarks.get(12);
    conditions = searchmark.getSearchConditions();
    assertEquals(3, conditions.size());

    locations = new ArrayList<IFolderChild>();

    for (ISearchCondition condition : conditions) {
      assertEquals(INews.LOCATION, condition.getField().getId());
      assertEquals(SearchSpecifier.IS, condition.getSpecifier());

      locations.addAll(CoreUtils.toEntities((Long[][]) condition.getValue()));
    }

    assertEquals(3, locations.size());
    assertContains("My Bookmarks", locations);
    assertContains("Custom", locations);
    assertContains("Bookmark 1", locations);

    /* 14) Location is Bin 1 */
    searchmark = searchmarks.get(13);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = CoreUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof INewsBin);
    assertEquals(fNewsBin.getName(), locations.get(0).getName());

    /* 15) Entire News CONTAINS_ALL foo?bar */
    searchmark = searchmarks.get(14);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS_ALL, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());

    /* 16) Entire News CONTAINS_NOT foo?bar */
    searchmark = searchmarks.get(15);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS_NOT, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());
  }

  private void assertContains(String name, List<IFolderChild> childs) {
    boolean found = false;
    for (IFolderChild child : childs) {
      if (child.getName().equals(name)) {
        found = true;
        break;
      }
    }

    assertEquals(true, found);
  }
}