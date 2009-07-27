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

package org.rssowl.ui.internal.util;

import org.eclipse.jface.viewers.StructuredSelection;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods mainly to support location conditions when importing OPML
 * files.
 *
 * @author bpasero
 */
public class ImportUtils {

  /**
   * @param fileName
   * @throws FileNotFoundException In case of an error.
   * @throws ParserException In case of an error.
   * @throws InterpreterException In case of an error.
   */
  public static void importFeeds(String fileName) throws FileNotFoundException, InterpreterException, ParserException {
    List<IEntity> entitiesToReload = new ArrayList<IEntity>();
    IPreferenceDAO prefsDAO = Owl.getPersistenceService().getDAOService().getPreferencesDAO();
    IFolderDAO folderDAO = DynamicDAO.getDAO(IFolderDAO.class);

    /* Import from File */
    File file = new File(fileName);
    InputStream inS = new FileInputStream(file);
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(inS);
    IFolder defaultContainer = (IFolder) types.get(0);

    /* Map Old Id to IFolderChild */
    Map<Long, IFolderChild> mapOldIdToFolderChild = ImportUtils.createOldIdToEntityMap(types);

    /* Load SearchMarks containing location condition */
    List<ISearchMark> locationConditionSavedSearches = ImportUtils.getLocationConditionSavedSearches(types);

    /* Load the current selected Set */
    IFolder selectedRootFolder;
    if (!InternalOwl.TESTING) {
      String selectedBookMarkSetPref = BookMarkExplorer.getSelectedBookMarkSetPref(OwlUI.getWindow());
      Long selectedFolderID = prefsDAO.load(selectedBookMarkSetPref).getLong();
      selectedRootFolder = folderDAO.load(selectedFolderID);
    } else {
      Collection<IFolder> rootFolders = CoreUtils.loadRootFolders();
      selectedRootFolder = rootFolders.iterator().next();
    }

    /* Load all Root Folders */
    Set<IFolder> rootFolders = CoreUtils.loadRootFolders();

    /* 1.) Handle Folders and Marks from default Container */
    {

      /* Also update Map of Old ID */
      if (defaultContainer.getProperty(ITypeImporter.ID_KEY) != null)
        mapOldIdToFolderChild.put((Long) defaultContainer.getProperty(ITypeImporter.ID_KEY), selectedRootFolder);

      /* Reparent and Save */
      reparentAndSaveChildren(defaultContainer, selectedRootFolder);
      entitiesToReload.addAll(defaultContainer.getChildren());
    }

    /* 2.) Handle other Sets */
    for (int i = 1; i < types.size(); i++) {
      if (!(types.get(i) instanceof IFolder))
        continue;

      IFolder setFolder = (IFolder) types.get(i);
      IFolder existingSetFolder = null;

      /* Check if set already exists */
      for (IFolder rootFolder : rootFolders) {
        if (rootFolder.getName().equals(setFolder.getName())) {
          existingSetFolder = rootFolder;
          break;
        }
      }

      /* Reparent into Existing Set */
      if (existingSetFolder != null) {

        /* Also update Map of Old ID */
        if (setFolder.getProperty(ITypeImporter.ID_KEY) != null)
          mapOldIdToFolderChild.put((Long) setFolder.getProperty(ITypeImporter.ID_KEY), existingSetFolder);

        /* Reparent and Save */
        reparentAndSaveChildren(setFolder, existingSetFolder);
        entitiesToReload.addAll(existingSetFolder.getChildren());
      }

      /* Otherwise save as new Set */
      else {

        /* Unset ID Property first */
        ImportUtils.unsetIdProperty(setFolder);

        /* Save */
        folderDAO.save(setFolder);
        entitiesToReload.add(setFolder);
      }
    }

    /* Fix locations in Search Marks if required and save */
    if (!locationConditionSavedSearches.isEmpty()) {
      ImportUtils.updateLocationConditions(mapOldIdToFolderChild, locationConditionSavedSearches);
      DynamicDAO.getDAO(ISearchMarkDAO.class).saveAll(locationConditionSavedSearches);
    }

    /* Look for Labels (from backup OPML) */
    Map<String, ILabel> mapExistingLabelToName = new HashMap<String, ILabel>();
    Map<Long, ILabel> mapOldIdToImportedLabel = new HashMap<Long, ILabel>();
    List<ILabel> importedLabels = ImportUtils.getLabels(types);
    if (!importedLabels.isEmpty()) {
      Collection<ILabel> existingLabels = DynamicDAO.loadAll(ILabel.class);
      for (ILabel existingLabel : existingLabels) {
        mapExistingLabelToName.put(existingLabel.getName(), existingLabel);
      }

      for (ILabel importedLabel : importedLabels) {
        Object oldIdValue = importedLabel.getProperty(ITypeImporter.ID_KEY);
        if (oldIdValue != null && oldIdValue instanceof Long)
          mapOldIdToImportedLabel.put((Long) oldIdValue, importedLabel);
      }

      for (ILabel importedLabel : importedLabels) {
        ILabel existingLabel = mapExistingLabelToName.get(importedLabel.getName());

        /* Update Existing */
        if (existingLabel != null) {
          existingLabel.setColor(importedLabel.getColor());
          existingLabel.setOrder(importedLabel.getOrder());
          DynamicDAO.save(existingLabel);
        }

        /* Save as New */
        else {
          importedLabel.removeProperty(ITypeImporter.ID_KEY);
          DynamicDAO.save(importedLabel);
        }
      }
    }

    /* Look for Filters (from backup OPML) */
    List<ISearchFilter> filters = ImportUtils.getFilters(types);
    if (!filters.isEmpty()) {

      /* Fix locations in Searches if required */
      List<ISearch> locationConditionSearches = ImportUtils.getLocationConditionSearchesFromFilters(filters);
      if (!locationConditionSearches.isEmpty())
        ImportUtils.updateLocationConditions(mapOldIdToFolderChild, locationConditionSearches);

      /* Fix locations in Actions if required */
      for (ISearchFilter filter : filters) {
        List<IFilterAction> actions = filter.getActions();
        for (IFilterAction action : actions) {
          if (OwlUI.MOVE_NEWS_ACTION_ID.equals(action.getActionId()) || OwlUI.COPY_NEWS_ACTION_ID.equals(action.getActionId())) {
            Object data = action.getData();
            if (data != null && data instanceof Long[]) {
              Long[] oldBinLocations = (Long[]) data;
              List<Long> newBinLocations = new ArrayList<Long>(oldBinLocations.length);

              for (int i = 0; i < oldBinLocations.length; i++) {
                Long oldLocation = oldBinLocations[i];
                if (mapOldIdToFolderChild.containsKey(oldLocation)) {
                  IFolderChild location = mapOldIdToFolderChild.get(oldLocation);
                  newBinLocations.add(location.getId());
                }
              }

              action.setData(newBinLocations.toArray(new Long[newBinLocations.size()]));
            }
          }
        }
      }

      /* Fix labels in Actions if required */
      for (ISearchFilter filter : filters) {
        List<IFilterAction> actions = filter.getActions();
        for (IFilterAction action : actions) {
          if (OwlUI.LABEL_NEWS_ACTION_ID.equals(action.getActionId())) {
            Object data = action.getData();
            if (data != null && data instanceof Long) {
              ILabel label = mapOldIdToImportedLabel.get(data);
              if (label != null) {
                String name = label.getName();
                ILabel existingLabel = mapExistingLabelToName.get(name);
                if (existingLabel != null)
                  action.setData(existingLabel.getId());
                else
                  action.setData(label.getId());
              }
            }
          }
        }
      }

      /* Save */
      DynamicDAO.saveAll(filters);
    }

    /* Reload imported Feeds */
    if (!InternalOwl.TESTING)
      new ReloadTypesAction(new StructuredSelection(entitiesToReload), OwlUI.getPrimaryShell()).run();
  }

  private static void reparentAndSaveChildren(IFolder from, IFolder to) {
    boolean changed = false;

    List<IFolderChild> children = from.getChildren();
    for (IFolderChild child : children) {

      /* Reparent Folder */
      if (child instanceof IFolder) {
        IFolder folder = (IFolder) child;
        folder.setParent(to);
        to.addFolder(folder, null, null);
        changed = true;
      }

      /* Reparent Mark */
      else if (child instanceof IMark) {
        IMark mark = (IMark) child;
        mark.setParent(to);
        to.addMark(mark, null, null);
        changed = true;
      }
    }

    /* Save Set */
    if (changed)
      DynamicDAO.getDAO(IFolderDAO.class).save(to);
  }

  /**
   * @param oldIdToFolderChildMap
   * @param searches
   */
  public static void updateLocationConditions(Map<Long, IFolderChild> oldIdToFolderChildMap, List<? extends ISearch> searches) {
    for (ISearch search : searches) {
      List<ISearchCondition> conditions = search.getSearchConditions();
      for (ISearchCondition condition : conditions) {

        /* Location Condition */
        if (condition.getField().getId() == INews.LOCATION) {
          Long[][] value = (Long[][]) condition.getValue();
          List<IFolderChild> newLocations = new ArrayList<IFolderChild>();

          /* Folders */
          for (int i = 0; value[CoreUtils.FOLDER] != null && i < value[CoreUtils.FOLDER].length; i++) {
            if (value[CoreUtils.FOLDER][i] != null && value[CoreUtils.FOLDER][i] != 0) {
              Long id = value[CoreUtils.FOLDER][i];
              if (oldIdToFolderChildMap.containsKey(id))
                newLocations.add(oldIdToFolderChildMap.get(id));
            }
          }

          /* BookMarks */
          for (int i = 0; value[CoreUtils.BOOKMARK] != null && i < value[CoreUtils.BOOKMARK].length; i++) {
            if (value[CoreUtils.BOOKMARK][i] != null && value[CoreUtils.BOOKMARK][i] != 0) {
              Long id = value[CoreUtils.BOOKMARK][i];
              if (oldIdToFolderChildMap.containsKey(id))
                newLocations.add(oldIdToFolderChildMap.get(id));
            }
          }

          /* NewsBins */
          if (value.length == 3) {
            for (int i = 0; value[CoreUtils.NEWSBIN] != null && i < value[CoreUtils.NEWSBIN].length; i++) {
              if (value[CoreUtils.NEWSBIN][i] != null && value[CoreUtils.NEWSBIN][i] != 0) {
                Long id = value[CoreUtils.NEWSBIN][i];
                if (oldIdToFolderChildMap.containsKey(id))
                  newLocations.add(oldIdToFolderChildMap.get(id));
              }
            }
          }

          /* Update */
          condition.setValue(ModelUtils.toPrimitive(newLocations));
        }
      }
    }
  }

  /**
   * @param entity
   */
  public static void unsetIdProperty(IEntity entity) {
    entity.removeProperty(ITypeImporter.ID_KEY);

    if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        unsetIdProperty(child);
      }
    }
  }

  /**
   * @param types
   * @return List of Saved Searches with Location Conditions.
   */
  public static List<ISearchMark> getLocationConditionSavedSearches(List<? extends IEntity> types) {
    List<ISearchMark> locationConditionSavedSearches = new ArrayList<ISearchMark>();

    for (IEntity entity : types)
      fillLocationConditionSavedSearches(locationConditionSavedSearches, entity);

    return locationConditionSavedSearches;
  }

  /**
   * @param filters
   * @return List of Searches with Location Conditions.
   */
  public static List<ISearch> getLocationConditionSearchesFromFilters(List<ISearchFilter> filters) {
    List<ISearch> locationConditionSearches = new ArrayList<ISearch>();

    for (ISearchFilter filter : filters) {
      ISearch search = filter.getSearch();
      if (search != null && containsLocationCondition(search))
        locationConditionSearches.add(search);
    }

    return locationConditionSearches;
  }

  /**
   * @param types
   * @return List of Labels.
   */
  public static List<ILabel> getLabels(List<? extends IEntity> types) {
    List<ILabel> labels = new ArrayList<ILabel>();

    for (IEntity entity : types) {
      if (entity instanceof ILabel)
        labels.add((ILabel) entity);
    }

    return labels;
  }

  /**
   * @param types
   * @return List of Filters.
   */
  public static List<ISearchFilter> getFilters(List<? extends IEntity> types) {
    List<ISearchFilter> filters = new ArrayList<ISearchFilter>();

    for (IEntity entity : types) {
      if (entity instanceof ISearchFilter)
        filters.add((ISearchFilter) entity);
    }

    return filters;
  }

  /**
   * @param types
   * @return Map
   */
  public static Map<Long, IFolderChild> createOldIdToEntityMap(List<? extends IEntity> types) {
    Map<Long, IFolderChild> oldIdToEntityMap = new HashMap<Long, IFolderChild>();

    for (IEntity entity : types) {
      if (entity instanceof IFolderChild)
        fillOldIdToEntityMap(oldIdToEntityMap, (IFolderChild) entity);
    }

    return oldIdToEntityMap;
  }

  private static void fillLocationConditionSavedSearches(List<ISearchMark> searchmarks, IEntity entity) {
    if (entity instanceof ISearchMark && containsLocationCondition((ISearchMark) entity)) {
      searchmarks.add((ISearchMark) entity);
    } else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillLocationConditionSavedSearches(searchmarks, child);
      }
    }
  }

  private static boolean containsLocationCondition(ISearch search) {
    List<ISearchCondition> searchConditions = search.getSearchConditions();
    for (ISearchCondition condition : searchConditions) {
      if (condition.getField().getId() == INews.LOCATION)
        return true;
    }

    return false;
  }

  private static void fillOldIdToEntityMap(Map<Long, IFolderChild> oldIdToEntityMap, IFolderChild folderChild) {
    Long oldId = (Long) folderChild.getProperty(ITypeImporter.ID_KEY);
    if (oldId != null)
      oldIdToEntityMap.put(oldId, folderChild);

    if (folderChild instanceof IFolder) {
      IFolder folder = (IFolder) folderChild;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillOldIdToEntityMap(oldIdToEntityMap, child);
      }
    }
  }
}