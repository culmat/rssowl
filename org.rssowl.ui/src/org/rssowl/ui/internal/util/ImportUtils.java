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

import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.util.CoreUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper methods mainly to support location conditions when importing OPML
 * files.
 *
 * @author bpasero
 */
public class ImportUtils {

  /**
   * @param oldIdToFolderChildMap
   * @param searches
   */
  public static void updateLocationConditions(Map<Long, IFolderChild> oldIdToFolderChildMap, List<ISearchMark> searches) {
    for (ISearchMark searchMark : searches) {
      List<ISearchCondition> conditions = searchMark.getSearchConditions();
      for (ISearchCondition condition : conditions) {

        /* Location Condition */
        if (condition.getField().getId() == INews.LOCATION) {
          Long[][] value = (Long[][]) condition.getValue();
          List<IFolderChild> newLocations = new ArrayList<IFolderChild>();

          /* Folders */
          for (int i = 0; value[CoreUtils.FOLDER] != null && i < value[CoreUtils.FOLDER].length; i++) {
            if (value[CoreUtils.FOLDER][i] != null && value[CoreUtils.FOLDER][i] != 0) {
              Long id = value[CoreUtils.FOLDER][i];
              newLocations.add(oldIdToFolderChildMap.get(id));
            }
          }

          /* BookMarks */
          for (int i = 0; value[CoreUtils.BOOKMARK] != null && i < value[CoreUtils.BOOKMARK].length; i++) {
            if (value[CoreUtils.BOOKMARK][i] != null && value[CoreUtils.BOOKMARK][i] != 0) {
              Long id = value[CoreUtils.BOOKMARK][i];
              newLocations.add(oldIdToFolderChildMap.get(id));
            }
          }

          /* NewsBins */
          if (value.length == 3) {
            for (int i = 0; value[CoreUtils.NEWSBIN] != null && i < value[CoreUtils.NEWSBIN].length; i++) {
              if (value[CoreUtils.NEWSBIN][i] != null && value[CoreUtils.NEWSBIN][i] != 0) {
                Long id = value[CoreUtils.NEWSBIN][i];
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
   * @return List
   */
  public static List<ISearchMark> getLocationConditionSearches(List<? extends IEntity> types) {
    List<ISearchMark> locationConditionSearches = new ArrayList<ISearchMark>();

    for (IEntity entity : types)
      fillLocationConditionSearches(locationConditionSearches, entity);

    return locationConditionSearches;
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

  private static void fillLocationConditionSearches(List<ISearchMark> searchmarks, IEntity entity) {
    if (entity instanceof ISearchMark && containsLocationCondition((ISearchMark) entity)) {
      searchmarks.add((ISearchMark) entity);
    } else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        fillLocationConditionSearches(searchmarks, child);
      }
    }
  }

  private static boolean containsLocationCondition(ISearchMark mark) {
    List<ISearchCondition> searchConditions = mark.getSearchConditions();
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