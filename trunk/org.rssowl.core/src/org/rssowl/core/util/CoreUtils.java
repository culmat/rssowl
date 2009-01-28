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

package org.rssowl.core.util;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.newsaction.CopyNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Helper class for various Core operations.
 *
 * @author bpasero
 */
public class CoreUtils {

  /** Folder Index Value for Long Arrays */
  public static final int FOLDER = 0;

  /** Bookmark Index Value for Long Arrays */
  public static final int BOOKMARK = 1;

  /** Newsbin Index Value for Long Arrays */
  public static final int NEWSBIN = 2;

  /* A Set of Stop Words in English */
  private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(StringUtils.ENGLISH_STOP_WORDS)));

  /* Special case structural actions that need to run as last action */
  private static List<String> STRUCTURAL_ACTIONS = Arrays.asList(new String[] { MoveNewsAction.ID, CopyNewsAction.ID });

  /* This utility class constructor is hidden */
  private CoreUtils() {
  // Protect default constructor
  }

  /**
   * @param filter an instance of {@link ISearchFilter} to obtain a collection
   * of {@link IFilterAction}.
   * @return a collection of {@link IFilterAction}. the collection is sorted
   * such as structural actions are moved to the end of the list.
   */
  public static Collection<IFilterAction> getActions(ISearchFilter filter) {
    Set<IFilterAction> actions = new TreeSet<IFilterAction>(new Comparator<IFilterAction>() {
      public int compare(IFilterAction o1, IFilterAction o2) {
        if (STRUCTURAL_ACTIONS.contains(o1.getActionId()))
          return 1;

        if (STRUCTURAL_ACTIONS.contains(o2.getActionId()))
          return -1;

        return 1;
      }
    });

    actions.addAll(filter.getActions());
    return actions;
  }

  /**
   * @param conditions The search conditions to find a human readable name for.
   * @param matchAllConditions Either true or false depending on the search.
   * @return A human readable name for all the conditions.
   */
  public static String getName(List<ISearchCondition> conditions, boolean matchAllConditions) {
    StringBuilder name = new StringBuilder();
    List<ISearchCondition> locationConditions = new ArrayList<ISearchCondition>(conditions.size());

    /* First group Conditions by Field */
    Map<String, List<ISearchCondition>> mapFieldNameToConditions = new HashMap<String, List<ISearchCondition>>();
    for (ISearchCondition condition : conditions) {

      /* Handle Location at the End */
      if (condition.getField().getId() == INews.LOCATION) {
        locationConditions.add(condition);
        continue;
      }

      String fieldName = condition.getField().getName();
      String condValue = condition.getValue().toString();

      if (condValue.length() > 0) {
        List<ISearchCondition> fieldConditions = mapFieldNameToConditions.get(fieldName);
        if (fieldConditions == null) {
          fieldConditions = new ArrayList<ISearchCondition>();
          mapFieldNameToConditions.put(fieldName, fieldConditions);
        }

        fieldConditions.add(condition);
      }
    }

    /* For each Field Group */
    Set<Entry<String, List<ISearchCondition>>> entries = mapFieldNameToConditions.entrySet();
    DateFormat dateFormat = DateFormat.getDateInstance();
    for (Entry<String, List<ISearchCondition>> entry : entries) {
      String prevSpecName = null;
      String fieldName = entry.getKey();
      List<ISearchCondition> fieldConditions = entry.getValue();
      StringBuilder fieldExpression = new StringBuilder();

      /* Append Field Name */
      fieldExpression.append(fieldName).append(" ");

      /* For each Field Condition */
      for (ISearchCondition fieldCondition : fieldConditions) {
        String condValue = fieldCondition.getValue().toString();
        String specName = fieldCondition.getSpecifier().getName();
        int typeId = fieldCondition.getField().getSearchValueType().getId();

        /* Condition Value provided */
        if (condValue.length() > 0) {

          /* Append specifier if not identical with previous */
          if (prevSpecName == null || !prevSpecName.equals(specName)) {
            fieldExpression.append(specName).append(" ");
            prevSpecName = specName;
          }

          /* Append Condition Value based on Type */
          switch (typeId) {
            case ISearchValueType.STRING:
              fieldExpression.append("'").append(condValue).append("'");
              break;
            case ISearchValueType.LINK:
              fieldExpression.append("'").append(condValue).append("'");
              break;
            case ISearchValueType.ENUM:
              condValue = condValue.toLowerCase();
              condValue = condValue.replace("[", "");
              condValue = condValue.replace("]", "");

              fieldExpression.append(condValue.toLowerCase());

              break;
            case ISearchValueType.DATE:
              fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
              break;
            case ISearchValueType.TIME:
              fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
              break;
            case ISearchValueType.DATETIME:
              fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
              break;

            default:
              fieldExpression.append(condValue);
          }

          fieldExpression.append(matchAllConditions ? " and " : " or ");
        }
      }

      if (fieldExpression.length() > 0)
        fieldExpression.delete(fieldExpression.length() - (matchAllConditions ? " and ".length() : " or ".length()), fieldExpression.length());

      name.append(fieldExpression).append(matchAllConditions ? " and " : " or ");
    }

    if (name.length() > 0)
      name.delete(name.length() - (matchAllConditions ? " and ".length() : " or ".length()), name.length());

    /* Append location if provided */
    if (!locationConditions.isEmpty()) {
      if (name.length() == 0)
        name.append("All News in ");
      else
        name.append(" in ");

      for (ISearchCondition locationCondition : locationConditions) {
        List<IFolderChild> locations = toEntities((Long[][]) locationCondition.getValue());
        for (IFolderChild location : locations) {
          name.append(location.getName()).append(", ");
        }
      }

      name.delete(name.length() - 2, name.length());
    }

    return name.toString();
  }

  /**
   * @param primitives A multi-dimensional array where an array of {@link Long}
   * is stored in the first index representing IDs of all {@link IFolder} in the
   * list. The second index is an array of {@link Long} that represents the IDs
   * of all {@link IBookMark} in the list. The third index is an array of
   * {@link Long} that represents the IDs of all {@link INewsBin} in the list.
   * @return A list of folder childs (limited to folders, bookmarks and news
   * bins).
   */
  public static List<IFolderChild> toEntities(Long[][] primitives) {
    List<IFolderChild> childs = new ArrayList<IFolderChild>();

    /* Folders */
    for (int i = 0; primitives[FOLDER] != null && i < primitives[FOLDER].length; i++) {
      try {
        if (primitives[FOLDER][i] != null && primitives[FOLDER][i] != 0) {
          IFolder folder = new FolderReference(primitives[FOLDER][i]).resolve();
          if (folder != null)
            childs.add(folder);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    /* BookMarks */
    for (int i = 0; primitives[BOOKMARK] != null && i < primitives[BOOKMARK].length; i++) {
      try {
        if (primitives[BOOKMARK][i] != null && primitives[BOOKMARK][i] != 0) {
          IBookMark bookmark = new BookMarkReference(primitives[BOOKMARK][i]).resolve();
          if (bookmark != null)
            childs.add(bookmark);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    /* News Bins */
    if (primitives.length == 3) {
      for (int i = 0; primitives[NEWSBIN] != null && i < primitives[NEWSBIN].length; i++) {
        try {
          if (primitives[NEWSBIN][i] != null && primitives[NEWSBIN][i] != 0) {
            INewsBin newsbin = new NewsBinReference(primitives[NEWSBIN][i]).resolve();
            if (newsbin != null)
              childs.add(newsbin);
          }
        } catch (PersistenceException e) {
          /* Ignore - Could be deleted already */
        }
      }
    }

    return childs;
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
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code> changed
   * its value for any of the given Events, <code>FALSE</code> otherwise.
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
    return isStickyStateChange(events, false);
  }

  /**
   * @param events
   * @param onlyHasBecomeSticky if <code>true</code>, only return
   * <code>true</code> if a news has become sticky.
   * @return <code>TRUE</code> in case the Sticky-State of the given News
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise. Respects the onlyHasBecomeSticky parameter.
   */
  public static boolean isStickyStateChange(Set<? extends ModelEvent> events, boolean onlyHasBecomeSticky) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldSticky = (newsEvent.getOldNews() != null) ? newsEvent.getOldNews().isFlagged() : false;
        boolean currentSticky = newsEvent.getEntity().isFlagged();

        /* Only return true if sticky state is now TRUE */
        if (onlyHasBecomeSticky) {
          if (!oldSticky && currentSticky)
            return true;
        }

        /* Return TRUE on sticky state change */
        else {
          if (oldSticky != currentSticky)
            return true;
        }
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
    return isReadStateChange(events, false);
  }

  /**
   * @param events
   * @param onlyHasBecomeUnread if <code>true</code>, only return
   * <code>true</code> if a news has become unread.
   * @return <code>TRUE</code> in case any state changed from NEW, UPDATED or
   * UNREAD to a different one, <code>FALSE</code> otherwise. Respects the
   * onlyHasBecomeUnread parameter.
   */
  public static boolean isReadStateChange(Set<? extends ModelEvent> events, boolean onlyHasBecomeUnread) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldStateUnread = isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean newStateUnread = isUnread(newsEvent.getEntity().getState());

        /* Only return true if unread state is now TRUE */
        if (onlyHasBecomeUnread) {
          if (!oldStateUnread && newStateUnread)
            return true;
        }

        /* Return TRUE on sticky state change */
        else {
          if (oldStateUnread != newStateUnread)
            return true;
        }
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code> or any
   * unread-state (NEW, UPDATED, UNREAD) changed its value for any of the given
   * Events, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case any State changed for ther given Events,
   * <code>FALSE</code> otherwise.
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
   * @param events
   * @return <code>TRUE</code> in case any of the News got restored and
   * <code>FALSE</code> otherwise.
   */
  public static boolean gotRestored(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean isVisible = newsEvent.getEntity().isVisible();
        boolean wasVisible = newsEvent.getOldNews() != null ? newsEvent.getOldNews().isVisible() : false;

        if (isVisible && !wasVisible && newsEvent.getOldNews() != null)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Publish-Date of the News, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Author of the News, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Category of the News, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Label of the News, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Title of the News, <code>FALSE</code> otherwise.
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
   * @return <code>TRUE</code> in case the given Entity is a child of the given
   * Folder, <code>FALSE</code> otherwise.
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

  /**
   * Returns a Set of all Links that are added as Bookmarks.
   *
   * @return Returns a Set of all Links that are added as Bookmarks.
   */
  public static Set<String> getFeedLinks() {
    IBookMarkDAO bookMarkDAO = Owl.getPersistenceService().getDAOService().getBookMarkDAO();
    Collection<IBookMark> bookMarks = bookMarkDAO.loadAll();
    Set<String> links = new HashSet<String>(bookMarks.size());

    for (IBookMark bookmark : bookMarks) {
      links.add(bookmark.getFeedLinkReference().getLinkAsText());
    }

    return links;
  }

  /**
   * Returns the first <code>IBookMark</code> that references the same feed as
   * <code>feedRef</code> or <code>null</code> if none.
   *
   * @param feedRef The desired Feed.
   * @return Returns the first <code>IBookMark</code> that references the given
   * Feed or <code>null</code> if none.
   */
  public static IBookMark getBookMark(FeedLinkReference feedRef) {
    IBookMarkDAO bookMarkDAO = Owl.getPersistenceService().getDAOService().getBookMarkDAO();
    Collection<IBookMark> bookMarks = bookMarkDAO.loadAll();
    for (IBookMark bookMark : bookMarks) {
      if (bookMark.getFeedLinkReference().equals(feedRef))
        return bookMark;
    }

    return null;
  }

  /**
   * @param news
   * @return Returns a Map mapping from a news-state to a list of
   * news-references.
   */
  public static Map<INews.State, List<NewsReference>> toStateMap(Collection<INews> news) {
    Map<INews.State, List<NewsReference>> map = new HashMap<State, List<NewsReference>>();
    for (INews newsitem : news) {
      INews.State state = newsitem.getState();
      List<NewsReference> newsrefs = map.get(state);
      if (newsrefs == null) {
        newsrefs = new ArrayList<NewsReference>();
        map.put(state, newsrefs);
      }

      newsrefs.add(newsitem.toReference());
    }

    return map;
  }

  /**
   * @param map
   * @return Returns a List of all News resolved.
   */
  public static List<INews> resolveAll(Map<State, List<NewsReference>> map) {
    List<INews> news = new ArrayList<INews>();

    Collection<List<NewsReference>> values = map.values();
    for (List<NewsReference> value : values) {
      for (NewsReference newsRef : value) {
        INews newsitem = newsRef.resolve();
        if (newsitem != null)
          news.add(newsitem);
      }
    }

    return news;
  }

  /**
   * @param conditions
   * @param ignoreWildcards
   * @param ignoreStopWords
   * @return Returns a set of words from the given conditions.
   */
  public static Set<String> extractWords(List<ISearchCondition> conditions, boolean ignoreWildcards, boolean ignoreStopWords) {
    Set<String> words = new HashSet<String>(conditions.size());

    /* Check Search Conditions for String-Values */
    for (ISearchCondition cond : conditions) {
      if (cond.getValue() instanceof String) {
        String value = cond.getValue().toString();

        /* Ignore Wildcard Only Values (e.g. search for Labels) */
        if ("?".equals(value) || "*".equals(value))
          continue;

        /* Ignore Wildcards if set */
        if (ignoreWildcards && (value.contains("?") || value.contains("*")))
          continue;

        /* Split into Words */
        value = StringUtils.replaceAll(value, "\"", "");
        StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
          String nextWord = tokenizer.nextElement().toString().toLowerCase();

          /* Ignore Stop Words if required */
          if (!ignoreStopWords || !STOP_WORDS.contains(nextWord))
            words.add(nextWord);
        }
      }
    }

    return words;
  }

  /**
   * @param news the {@link INews} to check.
   * @return <code>true</code> if the content is either empty or identical with
   * the title, <code>false</code> otherwise.
   */
  public static boolean isEmpty(INews news) {
    if (!StringUtils.isSet(news.getDescription()))
      return true;

    if (StringUtils.isSet(news.getTitle()) && news.getTitle().equals(news.getDescription()))
      return true;

    return false;
  }

  /**
   * @return all root folders sorted by their ID.
   */
  public static Set<IFolder> loadRootFolders() {

    /* Sort by ID to respect order */
    Set<IFolder> rootFolders = new TreeSet<IFolder>(new Comparator<IFolder>() {
      public int compare(IFolder f1, IFolder f2) {
        if (f1.equals(f2))
          return 0;

        return f1.getId() < f2.getId() ? -1 : 1;
      }
    });

    /* Add Root-Folders */
    rootFolders.addAll(DynamicDAO.getDAO(IFolderDAO.class).loadRoots());

    return rootFolders;
  }

  /**
   * @return all labels sorted by their sort value.
   */
  public static Set<ILabel> loadSortedLabels() {

    /* Sort by Sort Key to respect order */
    Set<ILabel> labels = new TreeSet<ILabel>(new Comparator<ILabel>() {
      public int compare(ILabel l1, ILabel l2) {
        if (l1.equals(l2))
          return 0;

        return l1.getOrder() < l2.getOrder() ? -1 : 1;
      }
    });

    /* Add Labels */
    labels.addAll(DynamicDAO.loadAll(ILabel.class));

    return labels;
  }
}