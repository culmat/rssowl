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

package org.rssowl.core.tests.importer;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.SearchFilter;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.util.ImportUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Tests import of various OPML files.
 *
 * @author bpasero
 */
public class FileImportTest {

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
  @SuppressWarnings( { "nls", "null", "unused" })
  public void testImport_Complex_All_Root() throws Exception {

    /* Import */
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    IFolder root = Owl.getModelFactory().createFolder(null, null, "Root");
    IFolder target = Owl.getModelFactory().createFolder(null, root, "Target");
    IFolder otherRoot = Owl.getModelFactory().createFolder(null, null, "Other Root");
    DynamicDAO.save(root);
    DynamicDAO.save(otherRoot);

    ImportUtils.doImport(root, elements);
    assertTrue(root.getMarks().isEmpty());
    List<IFolder> folders = root.getFolders();
    folders.remove(target);
    validate_Complex(folders, elements);
    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused" })
  public void testImport_Complex_All_Target() throws Exception {

    /* Import */
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    IFolder root = Owl.getModelFactory().createFolder(null, null, "Root");
    IFolder target = Owl.getModelFactory().createFolder(null, root, "Target");
    IFolder otherRoot = Owl.getModelFactory().createFolder(null, null, "Other Root");
    DynamicDAO.save(root);
    DynamicDAO.save(otherRoot);

    ImportUtils.doImport(target, elements);
    assertTrue(root.getMarks().isEmpty());
    validate_Complex(target.getFolders(), elements);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused" })
  public void testImport_Complex_All_OtherRoot() throws Exception {

    /* Import */
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    IFolder root = Owl.getModelFactory().createFolder(null, null, "Root");
    IFolder target = Owl.getModelFactory().createFolder(null, root, "Target");
    IFolder otherRoot = Owl.getModelFactory().createFolder(null, null, "Other Root");
    DynamicDAO.save(root);
    DynamicDAO.save(otherRoot);

    ImportUtils.doImport(otherRoot, elements);
    assertTrue(otherRoot.getMarks().isEmpty());
    validate_Complex(otherRoot.getFolders(), elements);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused" })
  public void testImport_Complex_All_Direct_Import() throws Exception {

    /* Import */
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    ImportUtils.doImport(null, elements);

    /* Validate */
    Collection<IFolder> roots = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    assertEquals(2, roots.size());
    validate_Complex(roots, elements);
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused", "unchecked" })
  public void testImport_Complex_Bookmarks_Direct_Import() throws Exception {

    /* Import */
    List<IBookMark> bookmarks = new ArrayList<IBookMark>();
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    for (IEntity entity : elements) {
      fillBookmarks(entity, bookmarks);
    }

    List elementsToImport = new ArrayList();
    elementsToImport.addAll(bookmarks);
    for (IEntity element : elements) {
      if (element instanceof ILabel || element instanceof ISearchFilter || element instanceof IPreference)
        elementsToImport.add(element);
    }

    IFolder root = DynamicDAO.save(Owl.getModelFactory().createFolder(null, null, "Root"));
    ImportUtils.doImport(root, elementsToImport);

    /* Validate */
    Collection<IFolder> roots = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    assertEquals(1, roots.size());
    assertEquals("Root", roots.iterator().next().getName());
    root = roots.iterator().next();

    assertEquals(3, root.getMarks().size());
    List<IMark> marks = root.getMarks();
    for (IMark mark : marks) {
      assertTrue(mark instanceof IBookMark);
    }
    assertEquals(0, root.getFolders().size());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused", "unchecked" })
  public void testImport_Complex_Bins_Direct_Import() throws Exception {

    /* Import */
    List<INewsBin> bins = new ArrayList<INewsBin>();
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    for (IEntity entity : elements) {
      fillNewsBins(entity, bins);
    }

    List elementsToImport = new ArrayList();
    elementsToImport.addAll(bins);
    for (IEntity element : elements) {
      if (element instanceof ILabel || element instanceof ISearchFilter || element instanceof IPreference)
        elementsToImport.add(element);
    }

    IFolder root = DynamicDAO.save(Owl.getModelFactory().createFolder(null, null, "Root"));
    ImportUtils.doImport(root, elementsToImport);

    /* Validate */
    Collection<IFolder> roots = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    assertEquals(1, roots.size());
    assertEquals("Root", roots.iterator().next().getName());
    root = roots.iterator().next();

    assertEquals(3, root.getMarks().size());
    List<IMark> marks = root.getMarks();
    for (IMark mark : marks) {
      assertTrue(mark instanceof INewsBin);
    }
    assertEquals(0, root.getFolders().size());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused", "unchecked" })
  public void testImport_Complex_Searches_Direct_Import() throws Exception {

    /* Import */
    List<ISearchMark> searches = new ArrayList<ISearchMark>();
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    for (IEntity entity : elements) {
      fillSearches(entity, searches);
    }

    List elementsToImport = new ArrayList();
    elementsToImport.addAll(searches);
    for (IEntity element : elements) {
      if (element instanceof ILabel || element instanceof ISearchFilter || element instanceof IPreference)
        elementsToImport.add(element);
    }

    IFolder root = DynamicDAO.save(Owl.getModelFactory().createFolder(null, null, "Root"));
    ImportUtils.doImport(root, elementsToImport);

    /* Validate */
    Collection<IFolder> roots = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    assertEquals(1, roots.size());
    assertEquals("Root", roots.iterator().next().getName());
    root = roots.iterator().next();

    assertEquals(1, root.getMarks().size());
    List<IMark> marks = root.getMarks();
    for (IMark mark : marks) {
      assertTrue(mark instanceof ISearchMark);
    }
    assertEquals(0, root.getFolders().size());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null", "unused", "unchecked" })
  public void testImport_Complex_Bookmarks_Target() throws Exception {

    /* Import */
    List<IBookMark> bookmarks = new ArrayList<IBookMark>();
    List<? extends IEntity> elements = Owl.getInterpreter().importFrom(getClass().getResourceAsStream("/data/importer/complex.opml"));
    for (IEntity entity : elements) {
      fillBookmarks(entity, bookmarks);
    }

    List elementsToImport = new ArrayList();
    elementsToImport.addAll(bookmarks);
    for (IEntity element : elements) {
      if (element instanceof ILabel || element instanceof ISearchFilter || element instanceof IPreference)
        elementsToImport.add(element);
    }

    IFolder root = Owl.getModelFactory().createFolder(null, null, "Root");
    IFolder target = Owl.getModelFactory().createFolder(null, root, "Target");
    DynamicDAO.save(root);

    ImportUtils.doImport(target, elementsToImport);

    /* Validate */
    Collection<IFolder> roots = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    assertEquals(1, roots.size());
    assertEquals("Root", roots.iterator().next().getName());
    root = roots.iterator().next();

    assertEquals(0, root.getMarks().size());
    assertEquals(1, root.getFolders().size());

    assertEquals(0, root.getFolders().iterator().next().getFolders().size());
    assertEquals(3, root.getFolders().iterator().next().getMarks().size());
  }

  private void fillBookmarks(IEntity entity, List<IBookMark> bookmarks) {
    if (entity instanceof IBookMark)
      bookmarks.add((IBookMark) entity);
    else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillBookmarks(child, bookmarks);
      }
    }
  }

  private void fillNewsBins(IEntity entity, List<INewsBin> bins) {
    if (entity instanceof INewsBin)
      bins.add((INewsBin) entity);
    else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillNewsBins(child, bins);
      }
    }
  }

  private void fillSearches(IEntity entity, List<ISearchMark> searches) {
    if (entity instanceof ISearchMark)
      searches.add((ISearchMark) entity);
    else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillSearches(child, searches);
      }
    }
  }

  @SuppressWarnings( { "nls", "null", "unused" })
  private void validate_Complex(Collection<IFolder> importedFolders, List<? extends IEntity> elements) throws Exception {
    IFolder firstSet = null;
    IFolder secondSet = null;
    for (IFolder set : importedFolders) {
      if ("First Set".equals(set.getName()))
        firstSet = set;
      else if ("Second Set".equals(set.getName()))
        secondSet = set;
      else
        fail("Unexpected Set");
    }

    assertNotNull(firstSet);
    assertNotNull(secondSet);

    assertEquals(1, firstSet.getChildren().size());
    IFolderChild child = firstSet.getChildren().iterator().next();
    assertTrue(child instanceof IFolder);
    assertEquals("Folder A", child.getName());

    IFolder folderA = (IFolder) child;

    assertEquals(1, folderA.getMarks().size());
    assertEquals(1, folderA.getFolders().size());

    List<IFolderChild> children = folderA.getChildren();
    IBookMark rssowlMark = null;
    IFolder bins = null;
    for (IFolderChild foo : children) {
      if (foo instanceof IBookMark && "RSSOwl - A Java RSS / RDF / Atom Newsreader - May the owl be with you".equals(foo.getName()))
        rssowlMark = (IBookMark) foo;
      else if (foo instanceof IFolder && "Bins".equals(foo.getName()))
        bins = (IFolder) foo;
      else
        fail();
    }

    assertNotNull(rssowlMark);
    assertNotNull(bins);

    assertEquals(1, bins.getChildren().size());
    IFolderChild next = bins.getChildren().iterator().next();
    assertEquals("Bin A", next.getName());
    assertTrue(next instanceof INewsBin);

    INewsBin binA = (INewsBin) next;

    children = secondSet.getChildren();
    assertEquals(4, children.size());
    IFolder folderB = null;
    IBookMark macOsHints = null;
    INewsBin binC = null;
    ISearchMark searchA = null;
    for (IFolderChild foo : children) {
      if (foo instanceof IFolder && "Folder B".equals(foo.getName()))
        folderB = (IFolder) foo;
      else if (foo instanceof IBookMark && "MacOSXHints.com".equals(foo.getName()))
        macOsHints = (IBookMark) foo;
      else if (foo instanceof INewsBin && "Bin C".equals(foo.getName()))
        binC = (INewsBin) foo;
      else if (foo instanceof ISearchMark && "Search A".equals(foo.getName()))
        searchA = (ISearchMark) foo;
    }

    assertEquals(2, searchA.getSearchConditions().size());
    List<ISearchCondition> conditions = searchA.getSearchConditions();
    for (ISearchCondition condition : conditions) {
      if (condition.getField().getId() == INews.LOCATION) {
        List<IFolderChild> entities = CoreUtils.toEntities((Long[][]) condition.getValue());
        assertEquals(1, entities.size());
        assertTrue(entities.contains(folderA));
      }
    }

    assertNotNull(folderB);
    assertNotNull(macOsHints);
    assertNotNull(binC);
    assertNotNull(searchA);

    children = folderB.getChildren();
    IBookMark golemMark = null;
    IFolder bins2 = null;
    for (IFolderChild foo : children) {
      if (foo instanceof IBookMark && "Golem.de".equals(foo.getName()))
        golemMark = (IBookMark) foo;
      else if (foo instanceof IFolder && "Bins".equals(foo.getName()))
        bins2 = (IFolder) foo;
      else
        fail();
    }

    assertNotNull(golemMark);
    assertNotNull(bins2);

    assertEquals(1, bins2.getChildren().size());
    next = bins2.getChildren().iterator().next();
    assertEquals("Bin B", next.getName());
    assertTrue(next instanceof INewsBin);

    INewsBin binB = (INewsBin) next;

    assertEquals(6, count(Label.class.getName(), elements));
    assertEquals(1, count(SearchFilter.class.getName(), elements));
    assertTrue(count(Preference.class.getName(), elements) > 0);

    ISearchFilter filter = null;
    for (IEntity element : elements) {
      if (element instanceof ISearchFilter) {
        filter = (ISearchFilter) element;
      }
    }

    assertNotNull(filter);

    ILabel label = null;
    for (IEntity element : elements) {
      if (element instanceof ILabel && "RSSOwl".equals(((ILabel) element).getName())) {
        label = (ILabel) element;
      }
    }

    assertNotNull(label);

    List<IFilterAction> actions = filter.getActions();
    assertEquals(2, actions.size());
    for (IFilterAction action : actions) {
      if (MoveNewsAction.ID.equals(action.getActionId())) {
        Object data = action.getData();
        assertTrue(data instanceof Long[]);
        assertEquals(binB.getId().longValue(), ((Long[]) data)[0].longValue());
      } else if (LabelNewsAction.ID.equals(action.getActionId())) {
        Object data = action.getData();
        assertEquals(label.getId().longValue(), ((Long) data).longValue());
      } else
        fail();
    }
  }

  private int count(String clazz, List<?> objs) {
    int count = 0;
    for (Object obj : objs) {
      if (obj.getClass().getName().equals(clazz))
        count++;
    }

    return count;
  }
}