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
import org.rssowl.ui.internal.EntityGroup;

import java.util.ArrayList;
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
class NewsBrowserViewModel {
  private final List<Item> fItemList = new ArrayList<NewsBrowserViewModel.Item>();
  private final Map<Long, Item> fItemMap = new HashMap<Long, NewsBrowserViewModel.Item>();
  private final Map<Long, List<Long>> fEntityGroupToNewsMap = new HashMap<Long, List<Long>>();
  private final Set<Long> fExpandedNews = new HashSet<Long>();
  private final Object fLock = new Object();

  /* Base Class of all Items in the Model */
  static class Item {
    private final long fId;

    Item(long id) {
      fId = id;
    }

    long getId() {
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
  static class Group extends Item {
    public Group(long id) {
      super(id);
    }
  }

  void setInput(Object[] elements) {
    synchronized (fLock) {

      /* Clear Caches */
      fItemList.clear();
      fItemMap.clear();
      fEntityGroupToNewsMap.clear();
      fExpandedNews.clear();

      /* Build the Model based on the Elements */
      List<Long> currentGroupEntryList = null;
      for (Object element : elements) {
        Item entry = null;

        /* Entity Group */
        if (element instanceof EntityGroup) {
          EntityGroup group = (EntityGroup) element;
          entry = new Group(group.getId());

          currentGroupEntryList = new ArrayList<Long>();
          fEntityGroupToNewsMap.put(group.getId(), currentGroupEntryList);
        }

        /* News Item */
        else if (element instanceof INews) {
          INews news = (INews) element;
          entry = new Item(news.getId());

          if (currentGroupEntryList != null)
            currentGroupEntryList.add(news.getId());
        }

        /* Add Entry into Collections */
        if (entry != null) {
          fItemList.add(entry);
          fItemMap.put(entry.getId(), entry);
        }
      }
    }
  }

  Map<Long, List<Long>> getGroups() {
    synchronized (fLock) {
      return new HashMap<Long, List<Long>>(fEntityGroupToNewsMap);
    }
  }

  boolean hasGroup(long groupId) {
    synchronized (fLock) {
      return fEntityGroupToNewsMap.containsKey(groupId);
    }
  }

  int getGroupSize(long groupId) {
    synchronized (fLock) {
      List<Long> entries = fEntityGroupToNewsMap.get(groupId);
      return entries != null ? entries.size() : 0;
    }
  }

  List<Long> getNewsIds(long groupId) {
    synchronized (fLock) {
      List<Long> newsIds = fEntityGroupToNewsMap.get(groupId);
      return newsIds != null ? new ArrayList<Long>(newsIds) : null;
    }
  }

  boolean isExpanded(INews news) {
    synchronized (fLock) {
      return fExpandedNews.contains(news.getId());
    }
  }

  ArrayList<Long> getExpandedNews() {
    synchronized (fLock) {
      return new ArrayList<Long>(fExpandedNews);
    }
  }

  void setExpanded(INews news, boolean expanded) {
    synchronized (fLock) {
      if (expanded)
        fExpandedNews.add(news.getId());
      else
        fExpandedNews.remove(news.getId());
    }
  }

  /**
   * @param news the news to remove from the view model.
   * @return the identifier of a group that needs an update now that the news
   * has been removed or -1 if none.
   */
  Long removeNews(INews news) {
    synchronized (fLock) {

      /* Remove from generic Item Collections */
      Item item = fItemMap.get(news.getId());
      if (item != null) {
        fItemList.remove(item);
        fItemMap.remove(item.getId());
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
            }
          }

          return groupId; //News can only be part of one group
        }
      }

      return -1L;
    }
  }
}