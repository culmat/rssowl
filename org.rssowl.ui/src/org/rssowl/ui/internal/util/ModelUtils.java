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

package org.rssowl.ui.internal.util;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.EntityGroupItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for various Model-Transformations required by the UI.
 *
 * @author bpasero
 */
public class ModelUtils {

  /**
   * @param entities A list of folder childs.
   * @return Returns a multi-dimensional array where an array of {@link Long} is
   * stored in the first index representing IDs of all {@link IFolder} in the
   * list. The second index is an array of {@link Long} that represents the IDs
   * of all {@link IBookMark} in the list. Returns <code>null</code> if the
   * list of {@link IFolderChild} did not contain any folders or bookmarks.
   */
  public static Long[][] toPrimitive(List<IFolderChild> entities) {
    List<Long> folderIds = null;
    List<Long> bookmarkIds = null;

    for (IEntity entity : entities) {

      /* Folder */
      if (entity instanceof IFolder) {
        if (folderIds == null)
          folderIds = new ArrayList<Long>();

        folderIds.add(entity.getId());
      }

      /* BookMark */
      else if (entity instanceof IBookMark) {
        if (bookmarkIds == null)
          bookmarkIds = new ArrayList<Long>();

        bookmarkIds.add(entity.getId());
      }

      /* Other type not supported */
      else
        throw new IllegalArgumentException("Only Folders and Bookmars are allowed!");
    }

    Long[][] result = new Long[2][];

    if (folderIds != null)
      result[0] = folderIds.toArray(new Long[folderIds.size()]);

    if (bookmarkIds != null)
      result[1] = bookmarkIds.toArray(new Long[bookmarkIds.size()]);

    if (folderIds == null && bookmarkIds == null)
      return null;

    return result;
  }

  /**
   * @param primitives A multi-dimensional array where an array of {@link Long}
   * is stored in the first index representing IDs of all {@link IFolder} in the
   * list. The second index is an array of {@link Long} that represents the IDs
   * of all {@link IBookMark} in the list.
   * @return A list of folder childs.
   */
  public static List<IFolderChild> toEntities(Long[][] primitives) {
    List<IFolderChild> childs = new ArrayList<IFolderChild>();

    /* Folders */
    for (int i = 0; primitives[0] != null && i < primitives[0].length; i++) {
      try {
        if (primitives[0][i] != null) {
          IFolder folder = new FolderReference(primitives[0][i]).resolve();
          if (folder != null)
            childs.add(folder);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    /* BookMarks */
    for (int i = 0; primitives[1] != null && i < primitives[1].length; i++) {
      try {
        if (primitives[1][i] != null) {
          IBookMark bookmark = new BookMarkReference(primitives[1][i]).resolve();
          if (bookmark != null)
            childs.add(bookmark);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    return childs;
  }

  /**
   * @param selection Any structured selection.
   * @return A List of Entities from the given Selection. In case the selection
   * contains an instanceof <code>EntityGroup</code>, only the content of the
   * group is considered.
   */
  public static List<IEntity> getEntities(IStructuredSelection selection) {
    if (selection.isEmpty())
      return new ArrayList<IEntity>(0);

    List<?> elements = selection.toList();
    List<IEntity> entities = new ArrayList<IEntity>(elements.size());

    for (Object object : elements) {
      if (object instanceof IEntity)
        entities.add((IEntity) object);
      else if (object instanceof EntityGroup) {
        List<EntityGroupItem> items = ((EntityGroup) object).getItems();
        for (EntityGroupItem item : items)
          entities.add(item.getEntity());
      }
    }

    return entities;
  }

  /**
   * @param selection Any structured selection.
   * @return A List of {@link IFolderChild} from the given Selection containing
   * {@link IFolder} and {@link IBookMark}.
   */
  public static List<IFolderChild> getFoldersAndBookMarks(IStructuredSelection selection) {
    if (selection.isEmpty())
      return new ArrayList<IFolderChild>(0);

    List<?> elements = selection.toList();
    List<IFolderChild> entities = new ArrayList<IFolderChild>(elements.size());

    for (Object object : elements) {
      if (object instanceof IFolder || object instanceof IBookMark)
        entities.add((IFolderChild) object);
    }

    return entities;
  }

  /**
   * @param <T>
   * @param selection
   * @param entityClass
   * @return A List of Entities that are instances of <code>entityClass</code>
   * from the given selection. In case the selection contains an instanceof
   * <code>EntityGroup</code>, only the content of the group is considered.
   */
  public static <T extends IEntity> List<T> getEntities(IStructuredSelection selection, Class<T> entityClass) {
    if (selection.isEmpty())
      return new ArrayList<T>(0);

    List<?> elements = selection.toList();
    List<T> entities = new ArrayList<T>(elements.size());

    for (Object object : elements) {
      if (entityClass.isInstance(object))
        entities.add(entityClass.cast(object));
      else if (object instanceof EntityGroup) {
        List<EntityGroupItem> items = ((EntityGroup) object).getItems();
        for (EntityGroupItem item : items) {
          if (entityClass.isInstance(item.getEntity()))
            entities.add(entityClass.cast(item.getEntity()));
        }
      }
    }

    return entities;
  }

  /**
   * Delete any Folder and Mark that is child of the given Folder
   *
   * @param folder
   * @param entities
   */
  public static void normalize(IFolder folder, List<? extends IEntity> entities) {

    /* Cleanup Marks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks)
      entities.remove(mark);

    /* Cleanup Folders and recursively treat Subfolders */
    List<IFolder> subFolders = folder.getFolders();
    for (IFolder subFolder : subFolders) {
      entities.remove(subFolder);
      normalize(subFolder, entities);
    }
  }

  /**
   * Returns a Headline for the given News. In general this will be the Title of
   * the News, but if not provided, parts of the Content will be taken instead.
   *
   * @param news The News to get the Headline from.
   * @return the Headline of the given News or "No Headline" if none.
   */
  public static String getHeadline(INews news) {

    /* Title provided */
    String title = StringUtils.stripTags(news.getTitle());
    title = StringUtils.normalizeString(title);
    if (StringUtils.isSet(title))
      return title;

    /* Try Content instead */
    String content = news.getDescription();
    if (StringUtils.isSet(content)) {
      content = StringUtils.stripTags(content);
      content = StringUtils.normalizeString(content);
      content = StringUtils.smartTrim(content, 50);

      if (StringUtils.isSet(content))
        return content;
    }

    return "No Headline";
  }

  /**
   * Normalizes the given Title by removing various kinds of response codes
   * (e.g. Re).
   *
   * @param title The title to normalize.
   * @return Returns the normalized Title (that is, response codes have been
   * removed).
   */
  public static String normalizeTitle(String title) {

    /* Check that title is provided, otherwise return */
    if (!StringUtils.isSet(title))
      return title;

    String normalizedTitle = null;
    int start = 0;
    int len = title.length();
    boolean done = false;

    /* Strip response codes */
    while (!done) {
      done = true;

      /* Skip Whitespaces */
      while (start < len && title.charAt(start) == ' ')
        start++;

      if (start < (len - 2)) {
        char c1 = title.charAt(start);
        char c2 = title.charAt(start + 1);
        char c3 = title.charAt(start + 2);

        /* Beginning "Re" */
        if ((c1 == 'r' || c1 == 'R') && (c2 == 'e' || c2 == 'E')) {

          /* Skip "Re:" */
          if (c3 == ':') {
            start += 3;
            done = false;
          }

          /* Skip numbered response codes like [12] */
          else if (start < (len - 2) && (c3 == '[' || c3 == '(')) {
            int i = start + 3;

            /* Skip entire number */
            while (i < len && title.charAt(i) >= '0' && title.charAt(i) <= '9')
              i++;

            char ci1 = title.charAt(i);
            char ci2 = title.charAt(i + 1);
            if (i < (len - 1) && (ci1 == ']' || ci1 == ')') && ci2 == ':') {
              start = i + 2;
              done = false;
            }
          }
        }
      }

      int end = len;

      /* Unread whitespace */
      while (end > start && title.charAt(end - 1) < ' ')
        end--;

      /* Build simplified Title */
      if (start == 0 && end == len)
        normalizedTitle = title;
      else
        normalizedTitle = title.substring(start, end);
    }

    return normalizedTitle;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code>
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise.
   */
  public static boolean isNewStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldStateNew = INews.State.NEW.equals(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean currentStateNew = INews.State.NEW.equals(newsEvent.getEntity().getState());

        if (oldStateNew != currentStateNew)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the Sticky-State of the given News
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise.
   */
  public static boolean isStickyStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldSticky = (newsEvent.getOldNews() != null) ? newsEvent.getOldNews().isFlagged() : false;
        boolean currentSticky = newsEvent.getEntity().isFlagged();

        if (oldSticky != currentSticky)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any state changed from NEW, UPDATED or
   * UNREAD to a different one, <code>FALSE</code> otherwise.
   */
  public static boolean isReadStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldStateUnread = isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean newStateUnread = isUnread(newsEvent.getEntity().getState());

        if (oldStateUnread != newStateUnread)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code> or
   * any unread-state (NEW, UPDATED, UNREAD) changed its value for any of the
   * given Events, <code>FALSE</code> otherwise.
   */
  public static boolean isNewOrReadStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean oldStateNew = INews.State.NEW.equals(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean currentStateNew = INews.State.NEW.equals(newsEvent.getEntity().getState());

        if (oldStateNew != currentStateNew)
          return true;

        boolean oldStateUnread = isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean newStateUnread = isUnread(newsEvent.getEntity().getState());

        if (oldStateUnread != newStateUnread)
          return true;
      }
    }

    return false;
  }

  /**
   * @param state
   * @return TRUE if the State is NEW, UPDATED or UNREAD and FALSE otherwise.
   */
  public static boolean isUnread(INews.State state) {
    return state == INews.State.NEW || state == INews.State.UPDATED || state == INews.State.UNREAD;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any State changed for ther given
   * Events, <code>FALSE</code> otherwise.
   */
  public static boolean isStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        INews.State oldState = newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null;
        if (oldState != newsEvent.getEntity().getState())
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the News got deleted and
   * <code>FALSE</code> otherwise.
   */
  public static boolean gotDeleted(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean isVisible = newsEvent.getEntity().isVisible();
        boolean wasVisible = newsEvent.getOldNews() != null ? newsEvent.getOldNews().isVisible() : false;

        if (!isVisible && wasVisible)
          return true;
      }
    }

    return false;
  }

  /**
   * @param selection Any list of selected <code>INews</code> or
   * <code>EntityGroup</code>.
   * @return Returns a Set of <code>ILabel</code> that <em>all entities</em>
   * of the given Selection had applied to.
   */
  public static Set<ILabel> getLabelsForAll(IStructuredSelection selection) {
    Set<ILabel> labelsForAll = new HashSet<ILabel>(5);

    List<INews> selectedNews = getEntities(selection, INews.class);

    /* For each selected News */
    for (INews news : selectedNews) {
      Set<ILabel> newsLabels = news.getLabels();

      /* Only add Label if contained in all News */
      LabelLoop: for (ILabel newsLabel : newsLabels) {
        if (!labelsForAll.contains(newsLabel)) {
          for (INews news2 : selectedNews) {
            if (!news2.getLabels().contains(newsLabel))
              break LabelLoop;
          }

          labelsForAll.add(newsLabel);
        }
      }
    }

    return labelsForAll;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change
   * in the Publish-Date of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isDateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        Date oldDate = event.getOldNews() != null ? DateUtils.getRecentDate(event.getOldNews()) : null;
        Date newDate = DateUtils.getRecentDate(event.getEntity());

        if (!newDate.equals(oldDate))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change
   * in the Author of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isAuthorChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        IPerson oldAuthor = event.getOldNews() != null ? event.getOldNews().getAuthor() : null;
        IPerson newAuthor = event.getEntity().getAuthor();

        if (newAuthor != null && !newAuthor.equals(oldAuthor))
          return true;
        else if (oldAuthor != null && !oldAuthor.equals(newAuthor))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change
   * in the Category of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isCategoryChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        List<ICategory> oldCategories = event.getOldNews() != null ? event.getOldNews().getCategories() : null;
        List<ICategory> newCategories = event.getEntity().getCategories();

        if (!newCategories.equals(oldCategories))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change
   * in the Label of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isLabelChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        Set<ILabel> oldLabels = event.getOldNews() != null ? event.getOldNews().getLabels() : null;
        Set<ILabel> newLabels = event.getEntity().getLabels();

        if (!newLabels.equals(oldLabels))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change
   * in the Title of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isTitleChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        String oldTopic = event.getOldNews() != null ? getHeadline(event.getOldNews()) : null;
        String newTopic = getHeadline(event.getEntity());

        if (!newTopic.equals(oldTopic))
          return true;
      }
    }
    return false;
  }

  /**
   * @param parent
   * @param entityToCheck
   * @return <code>TRUE</code> in case the given Entity is a child of the
   * given Folder, <code>FALSE</code> otherwise.
   */
  public static boolean hasChildRelation(IFolder parent, IEntity entityToCheck) {
    if (entityToCheck instanceof IFolder) {
      IFolder folder = (IFolder) entityToCheck;
      if (parent.equals(folder))
        return true;

      return hasChildRelation(parent, folder.getParent());
    }

    else if (entityToCheck instanceof IMark) {
      IMark mark = (IMark) entityToCheck;
      if (mark.getParent().equals(parent))
        return true;

      return hasChildRelation(parent, mark.getParent());
    }

    return false;
  }
}