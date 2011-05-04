/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.editors.feed;

import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.Triple;
import org.rssowl.ui.internal.EntityGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The news view model is a representation of the visible news in the
 * {@link NewsBrowserViewer}. This includes groups (if enabled), sorting and
 * other UI related state.
 * <p>
 * The model is safe to be used from multiple threads.
 * </p>
 *
 * @author bpasero
 */
public class NewsBrowserViewModel {
  private final List<Item> fItemList = new ArrayList<NewsBrowserViewModel.Item>();
  private final Map<Long, Item> fItemMap = new HashMap<Long, NewsBrowserViewModel.Item>();
  private final Map<Long, List<Long>> fEntityGroupToNewsMap = new HashMap<Long, List<Long>>();
  private final Set<Long> fExpandedNews = new HashSet<Long>();
  private final Set<Long> fCollapsedGroups = new HashSet<Long>();
  private final Set<Long> fHiddenNews = new HashSet<Long>();
  private final Set<Long> fHiddenGroups = new HashSet<Long>();
  private final Object fLock = new Object();

  /* Base Class of all Items in the Model */
  private static class Item {
    private final long fId;

    public Item(long id) {
      fId = id;
    }

    public long getId() {
      return fId;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (fId ^ (fId >>> 32));

      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;

      if (obj == null)
        return false;

      if (getClass() != obj.getClass())
        return false;

      Item other = (Item) obj;
      if (fId != other.fId)
        return false;

      return true;
    }
  }

  /* Special Item that contains other Items in the View */
  private static class Group extends Item {
    public Group(long id) {
      super(id);
    }
  }

  /**
   * Updates this view model with the contents of the provided elements.
   *
   * @param elements the elements to create the view model from.
   */
  public void setInput(Object[] elements) {
    setInput(elements, 0);
  }

  /**
   * Updates this view model with the contents of the provided elements.
   *
   * @param elements the elements to create the view model from.
   * @param pageSize the number of elements per page or <code>0</code> in case
   * paging is disabled.
   */
  public void setInput(Object[] elements, int pageSize) {
    synchronized (fLock) {

      /* Clear Caches */
      fItemList.clear();
      fItemMap.clear();
      fEntityGroupToNewsMap.clear();
      fExpandedNews.clear();
      fCollapsedGroups.clear();
      fHiddenNews.clear();
      fHiddenGroups.clear();

      /* Build the Model based on the Elements */
      if (elements != null && elements.length > 0) {

        /* Build Model */
        int newsCounter = 0;
        List<Long> currentGroupEntryList = null;
        for (Object element : elements) {
          Item entry = null;

          /* Entity Group */
          if (element instanceof EntityGroup) {
            EntityGroup group = (EntityGroup) element;
            entry = new Group(group.getId());

            currentGroupEntryList = new ArrayList<Long>();
            fEntityGroupToNewsMap.put(group.getId(), currentGroupEntryList);

            /*
             * We use ">=" here to check if the group is visible or not to avoid the case
             * of a group being made visible that contains no visible news at all.
             */
            if (pageSize != 0 && newsCounter >= pageSize)
              setGroupVisible(group.getId(), false);
          }

          /* News Item */
          else if (element instanceof INews) {
            newsCounter++;
            INews news = (INews) element;
            entry = new Item(news.getId());

            if (currentGroupEntryList != null)
              currentGroupEntryList.add(news.getId());

            if (pageSize != 0 && newsCounter > pageSize)
              setNewsVisible(news, false);
          }

          /* Add Entry into Collections */
          if (entry != null) {
            fItemList.add(entry);
            fItemMap.put(entry.getId(), entry);
          }
        }
      }
    }
  }

  /**
   * @return the {@link Map} of groups if grouping is enabled.
   */
  public Map<Long, List<Long>> getGroups() {
    synchronized (fLock) {
      return new HashMap<Long, List<Long>>(fEntityGroupToNewsMap);
    }
  }

  /**
   * @param groupId the group identifier to look for
   * @return <code>true</code> if a group with the given identifier exists and
   * <code>false</code> otherwise.
   */
  public boolean hasGroup(long groupId) {
    synchronized (fLock) {
      return fEntityGroupToNewsMap.containsKey(groupId);
    }
  }

  /**
   * @param groupId the group identifier to use
   * @return the number of elements inside the group with the given identifier
   * or 0 if none.
   */
  public int getGroupSize(long groupId) {
    synchronized (fLock) {
      List<Long> entries = fEntityGroupToNewsMap.get(groupId);
      return entries != null ? entries.size() : 0;
    }
  }

  /**
   * @param groupId the group identifier to use
   * @return the list of news ids being held by the given group.
   */
  @SuppressWarnings("unchecked")
  public List<Long> getNewsIds(long groupId) {
    synchronized (fLock) {
      List<Long> newsIds = fEntityGroupToNewsMap.get(groupId);
      return newsIds != null ? new ArrayList<Long>(newsIds) : Collections.EMPTY_LIST;
    }
  }

  /**
   * @param news the news item to check for expanded state
   * @return <code>true</code> if the news is expanded and <code>false</code>
   * otherwise.
   */
  public boolean isNewsExpanded(INews news) {
    synchronized (fLock) {
      return fExpandedNews.contains(news.getId());
    }
  }

  /**
   * @param news the news item to check for being visible or not
   * @return <code>true</code> if the news is visible and <code>false</code>
   * otherwise.
   */
  public boolean isNewsVisible(INews news) {
    return isNewsVisible(news.getId());
  }

  /**
   * @param newsId the news item to check for being visible or not
   * @return <code>true</code> if the news is visible and <code>false</code>
   * otherwise.
   */
  public boolean isNewsVisible(long newsId) {
    synchronized (fLock) {
      return !fHiddenNews.contains(newsId);
    }
  }

  /**
   * @param groupId
   * @return <code>true</code> if the group is expanded and <code>false</code>
   * otherwise.
   */
  public boolean isGroupExpanded(long groupId) {
    synchronized (fLock) {
      return !fCollapsedGroups.contains(groupId);
    }
  }

  /**
   * @param groupId
   * @return <code>true</code> if the group is visible and <code>false</code>
   * otherwise.
   */
  public boolean isGroupVisible(long groupId) {
    synchronized (fLock) {
      return !fHiddenGroups.contains(groupId);
    }
  }

  /**
   * @return the identifier of the currently expanded news or -1 if none.
   */
  public long getExpandedNews() {
    synchronized (fLock) {
      if (!fExpandedNews.isEmpty())
        return fExpandedNews.iterator().next();

      return -1L;
    }
  }

  /**
   * @param news the news to expand or collapse
   * @param expanded <code>true</code> if expanded and <code>false</code> if
   * collapsed
   */
  public void setNewsExpanded(INews news, boolean expanded) {
    synchronized (fLock) {
      if (expanded)
        fExpandedNews.add(news.getId());
      else
        fExpandedNews.remove(news.getId());
    }
  }

  /**
   * @param news the news to hide or show
   * @param visible <code>true</code> if visible and <code>false</code> if
   * hidden
   */
  public void setNewsVisible(INews news, boolean visible) {
    synchronized (fLock) {
      if (visible)
        fHiddenNews.remove(news.getId());
      else
        fHiddenNews.add(news.getId());
    }
  }

  /**
   * @param groupId the group to hide or show
   * @param visible <code>true</code> if visible and <code>false</code> if
   * hidden
   */
  public void setGroupVisible(long groupId, boolean visible) {
    synchronized (fLock) {
      if (visible)
        fHiddenGroups.remove(groupId);
      else
        fHiddenGroups.add(groupId);
    }
  }

  /**
   * @param groupId the group to expand or collapse
   * @param expanded <code>true</code> if expanded and <code>false</code> if
   * collapsed
   */
  public void setGroupExpanded(long groupId, boolean expanded) {
    synchronized (fLock) {
      if (expanded)
        fCollapsedGroups.remove(groupId);
      else
        fCollapsedGroups.add(groupId);
    }
  }

  /**
   * @param newsId the identifier of the news to find the group for
   * @return the identifier of the group for the given news or -1 if none
   */
  public long findGroup(long newsId) {
    synchronized (fLock) {
      Set<java.util.Map.Entry<Long, List<Long>>> entries = fEntityGroupToNewsMap.entrySet();
      for (java.util.Map.Entry<Long, List<Long>> entry : entries) {
        List<Long> newsInGroup = entry.getValue();
        if (newsInGroup.contains(newsId))
          return entry.getKey();
      }
    }

    return -1L;
  }

  /**
   * @return <code>true</code> if the first item showing in the browser is
   * unread and <code>false</code>otherwise. Will always return false if
   * grouping is enabled as the first item then will be a group.
   */
  public boolean isFirstItemUnread() {
    synchronized (fLock) {
      if (!fItemList.isEmpty()) {
        Item item = fItemList.get(0);
        return isUnread(item);
      }
    }

    return false;
  }

  /**
   * @return <code>true</code> if the browser viewer is displaying items and
   * <code>false</code> otherwise.
   */
  public boolean hasItems() {
    synchronized (fLock) {
      return !fItemList.isEmpty();
    }
  }

  /**
   * @return the number of news in the model (both visible and hidden).
   */
  public int getNewsCount() {
    synchronized (fLock) {
      return fItemList.size() - fEntityGroupToNewsMap.keySet().size();
    }
  }

  /**
   * @return the number of visible news displayed.
   */
  public int getVisibleNewsCount() {
    synchronized (fLock) {
      return fItemList.size() - fEntityGroupToNewsMap.keySet().size() - fHiddenNews.size();
    }
  }

  /**
   * @param news the news to remove from the view model.
   * @return the identifier of a group that needs an update now that the news
   * has been removed or -1 if none.
   */
  public long removeNews(INews news) {
    synchronized (fLock) {

      /* Remove from generic Item Collections */
      Item item = fItemMap.get(news.getId());
      if (item != null) {
        fItemList.remove(item);
        fItemMap.remove(item.getId());
        fHiddenNews.remove(item.getId());
      }

      /* Remove from Collection of expanded Elements */
      fExpandedNews.remove(news.getId());

      /* Remove from Group Mapping */
      Set<java.util.Map.Entry<Long, List<Long>>> entries = fEntityGroupToNewsMap.entrySet();
      for (java.util.Map.Entry<Long, List<Long>> entry : entries) {
        Long groupId = entry.getKey();
        List<Long> newsInGroup = entry.getValue();
        if (newsInGroup.contains(news.getId())) {
          newsInGroup.remove(news.getId());

          /* In case the group is now empty, remove it as well */
          if (newsInGroup.isEmpty()) {
            fEntityGroupToNewsMap.remove(groupId);

            Item group = fItemMap.get(groupId);
            if (group != null) {
              fItemList.remove(group);
              fItemMap.remove(group.getId());
              fCollapsedGroups.remove(group.getId());
              fHiddenGroups.remove(group.getId());
            }
          }

          return groupId; //News can only be part of one group
        }
      }

      return -1L;
    }
  }

  /**
   * @param unread if the next news should be unread or not
   * @param offset the offset to start navigating from
   * @return the identifier of the next news or -1 if none
   */
  public long nextNews(boolean unread, long offset) {
    synchronized (fLock) {

      /* Get the next news using provided one as starting location or from beginning if no location provided */
      Item item = new Item(offset);
      int nextIndex = (offset != -1 && fItemList.contains(item)) ? fItemList.indexOf(item) + 1 : 0;

      /* More Elements available */
      for (int i = nextIndex; i < fItemList.size(); i++) {
        Item nextItem = fItemList.get(i);
        if (nextItem instanceof Group)
          continue; //We only want to navigate to News Items

        /* Return Item if it matches the criteria */
        if (!unread || isUnread(nextItem))
          return nextItem.getId();
      }
    }

    return -1L;
  }

  /**
   * @param unread if the previous news should be unread or not
   * @param offset the offset to start navigating from
   * @return the identifier of the previous news or -1 if none
   */
  public long previousNews(boolean unread, long offset) {
    synchronized (fLock) {

      /* Get the next news using provided one as starting location or from end if no location provided */
      Item item = new Item(offset);
      int previousIndex = (offset != -1 && fItemList.contains(item)) ? fItemList.indexOf(item) - 1 : fItemList.size() - 1;

      /* More Elements available */
      for (int i = previousIndex; i >= 0 && i < fItemList.size(); i--) {
        Item previousItem = fItemList.get(i);
        if (previousItem instanceof Group)
          continue; //We only want to navigate to News Items

        /* Return Item if it matches the criteria */
        if (!unread || isUnread(previousItem))
          return previousItem.getId();
      }
    }

    return -1L;
  }

  private boolean isUnread(Item item) {
    if (item instanceof Group)
      return false;

    INews news = DynamicDAO.load(INews.class, item.getId());
    if (news == null)
      return false;

    switch (news.getState()) {
      case NEW:
      case UNREAD:
      case UPDATED:
        return true;
    }

    return false;
  }

  /**
   * Retrieves the identifiers of the elements for the next page.
   *
   * @param pageSize the number of elements per page.
   * @return a {@link Triple} of lists. The first contains the identifiers of
   * groups revealed and the second the list of news identifiers. Finally
   * indicates if more pages are available or not.
   */
  public Triple<List<Long> /* Groups */, List<Long> /* News Items */, Boolean /* Has More Pages */> getNextPage(int pageSize) {
    List<Long> groups = new ArrayList<Long>(1);
    List<Long> news = new ArrayList<Long>(pageSize);
    Boolean hasMorePages = false;

    synchronized (fLock) {
      int indexOfFirstHiddenItem = indexOfFirstHiddenItem(-1);
      if (indexOfFirstHiddenItem != -1) {
        int newsCounter = 0;
        for (int i = indexOfFirstHiddenItem; i < fItemList.size(); i++) {
          Item item = fItemList.get(i);

          /* Group */
          if (item instanceof Group) {
            groups.add(item.getId());
          }

          /* News */
          else {
            newsCounter++;
            news.add(item.getId());
          }

          if (newsCounter == pageSize) {
            hasMorePages = (i + 1 < fItemList.size());
            break; //Reached the next page, so stop looping
          }
        }
      }
    }

    return Triple.create(groups, news, hasMorePages);
  }

  /**
   * Retrieves the identifiers of all hidden elements up to and including the
   * provided news item.
   *
   * @param newsitem the news item that is being revealed.
   * @return a {@link Pair} of lists. The first contains the identifiers of
   * groups revealed and the second the list of news identifiers in case not
   * visible.
   */
  public Pair<List<Long>, List<Long>> reveal(INews newsitem) {
    List<Long> groups = new ArrayList<Long>(1);
    List<Long> news = new ArrayList<Long>();

    synchronized (fLock) {
      int indexOfNewsItem = indexOf(newsitem);
      int indexOfFirstHiddenItem = indexOfFirstHiddenItem(newsitem.getId());
      if (indexOfFirstHiddenItem != -1 && indexOfNewsItem != -1) {
        for (int i = indexOfFirstHiddenItem; i < fItemList.size() && i <= indexOfNewsItem; i++) {
          Item item = fItemList.get(i);

          /* Group */
          if (item instanceof Group)
            groups.add(item.getId());

          /* News */
          else
            news.add(item.getId());
        }
      }
    }

    return Pair.create(groups, news);
  }

  private int indexOf(INews news) {
    synchronized (fLock) {
      for (int i = 0; i < fItemList.size(); i++) {
        Item item = fItemList.get(i);
        if (!(item instanceof Group) && item.getId() == news.getId())
          return i;
      }
    }

    return -1;
  }

  private int indexOfFirstHiddenItem(long toId) {
    synchronized (fLock) {
      for (int i = 0; i < fItemList.size(); i++) {
        Item item = fItemList.get(i);

        if (!isVisible(item))
          return i; //Hidden Item found

        if (toId != -1 && toId == item.getId())
          break; //Limit reached
      }
    }
    return -1;
  }

  private boolean isVisible(Item item) {
    if (item instanceof Group)
      return isGroupVisible(item.getId());

    return isNewsVisible(item.getId());
  }
}