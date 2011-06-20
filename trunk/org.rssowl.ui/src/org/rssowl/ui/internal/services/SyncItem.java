/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2011 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.services;

import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.internal.newsaction.MarkReadNewsAction;
import org.rssowl.core.internal.newsaction.MarkStickyNewsAction;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link SyncItem} is used to synchronize changes to {@link INews} with an
 * online service like Google Reader.
 *
 * @author bpasero
 */
public class SyncItem implements Serializable {

  /* Serial Version UID to support class changes */
  private static final long serialVersionUID = 4093540431879243015L;

  /* Set of unread states */
  private static final Set<INews.State> UNREAD_STATES = EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED);

  private final String fId;
  private final String fStreamId;
  private boolean fMarkedRead;
  private boolean fMarkedUnread;
  private boolean fStarred;
  private boolean fUnStarred;
  private List<String> fAddedLabels;
  private List<String> fRemovedLabels;

  /* Creates a SyncItem out of a NewsEvent */
  static SyncItem toSyncItem(NewsEvent event) {
    boolean requiresSync = false;
    INews item = event.getEntity();
    SyncItem syncItem = toSyncItem(item);

    /* State Change */
    INews.State oldState = event.getOldNews().getState();
    INews.State newState = item.getState();
    if (oldState != newState) {

      /* Marked Read */
      if (newState == INews.State.READ && UNREAD_STATES.contains(oldState)) {
        syncItem.setMarkedRead();
        requiresSync = true;
      }

      /* Marked Unread */
      else if (newState == INews.State.UNREAD && oldState == INews.State.READ) {
        syncItem.setMarkedUnread();
        requiresSync = true;
      }
    }

    /* Sticky Change */
    boolean oldSticky = event.getOldNews().isFlagged();
    boolean newSticky = item.isFlagged();
    if (oldSticky != newSticky) {
      if (oldSticky)
        syncItem.setUnStarred();
      else
        syncItem.setStarred();

      requiresSync = true;
    }

    /* Label Change */
    Set<ILabel> oldLabels = event.getOldNews().getLabels();
    Set<ILabel> newLabels = item.getLabels();

    if (!Arrays.equals(oldLabels.toArray(), newLabels.toArray())) {
      Set<String> oldLabelNames = new HashSet<String>(oldLabels.size());
      for (ILabel oldLabel : oldLabels) {
        oldLabelNames.add(oldLabel.getName());
      }

      for (ILabel newLabel : newLabels) {
        if (!oldLabelNames.remove(newLabel.getName())) {
          syncItem.addLabel(newLabel.getName());
          requiresSync = true;
        }
      }

      for (String oldLabelName : oldLabelNames) {
        syncItem.removeLabel(oldLabelName);
        requiresSync = true;
      }
    }

    return requiresSync ? syncItem : null;
  }

  /* Creates a SyncItem out of a Filter operating on a News */
  static SyncItem toSyncItem(ISearchFilter filter, INews item) {
    boolean requiresSync = false;
    SyncItem syncItem = toSyncItem(item);

    List<IFilterAction> actions = filter.getActions();
    for (IFilterAction action : actions) {
      String actionId = action.getActionId();

      /* State Change */
      if (MarkReadNewsAction.ID.equals(actionId)) {
        syncItem.setMarkedRead();
        requiresSync = true;
      }

      /* Sticky Change */
      if (MarkStickyNewsAction.ID.equals(actionId)) {
        syncItem.setStarred();
        requiresSync = true;
      }

      /* Label Change */
      if (LabelNewsAction.ID.equals(actionId)) {
        Object data = action.getData();
        if (data != null && data instanceof Long) {
          Long labelId = (Long) data;
          ILabel label = DynamicDAO.load(ILabel.class, labelId);
          if (label != null) {
            syncItem.addLabel(label.getName());
            requiresSync = true;
          }
        }
      }
    }

    return requiresSync ? syncItem : null;
  }

  private static SyncItem toSyncItem(INews news) {
    String itemId = news.getGuid().getValue();
    String streamId = news.getInReplyTo();

    return new SyncItem(itemId, streamId);
  }

  SyncItem(String id, String streamId) {
    fId = id;
    fStreamId = streamId;
  }

  String getId() {
    return fId;
  }

  String getStreamId() {
    return fStreamId;
  }

  void setMarkedRead() {
    fMarkedRead = true;
    fMarkedUnread = false;
  }

  void setMarkedUnread() {
    fMarkedUnread = true;
    fMarkedRead = false;
  }

  void setStarred() {
    fStarred = true;
    fUnStarred = false;
  }

  void setUnStarred() {
    fUnStarred = true;
    fStarred = false;
  }

  void addLabel(String label) {
    if (fAddedLabels == null)
      fAddedLabels = new ArrayList<String>(3);

    fAddedLabels.add(label);

    if (fRemovedLabels != null)
      fRemovedLabels.remove(label);
  }

  void removeLabel(String label) {
    if (fRemovedLabels == null)
      fRemovedLabels = new ArrayList<String>(1);

    fRemovedLabels.add(label);

    if (fAddedLabels != null)
      fAddedLabels.remove(label);
  }

  boolean isMarkedRead() {
    return fMarkedRead;
  }

  boolean isMarkedUnread() {
    return fMarkedUnread;
  }

  boolean isStarred() {
    return fStarred;
  }

  boolean isUnStarred() {
    return fUnStarred;
  }

  List<String> getAddedLabels() {
    return fAddedLabels != null ? fAddedLabels : Collections.<String> emptyList();
  }

  List<String> getRemovedLabels() {
    return fRemovedLabels != null ? fRemovedLabels : Collections.<String> emptyList();
  }

  void merge(SyncItem item) {
    if (item.fMarkedRead)
      setMarkedRead();

    if (item.fMarkedUnread)
      setMarkedUnread();

    if (item.fStarred)
      setStarred();

    if (item.fUnStarred)
      setUnStarred();

    if (item.fAddedLabels != null) {
      for (String label : item.fAddedLabels) {
        addLabel(label);
      }
    }

    if (item.fRemovedLabels != null) {
      for (String label : item.fRemovedLabels) {
        removeLabel(label);
      }
    }
  }

  boolean isEquivalent(SyncItem item) {
    if (fMarkedRead != item.fMarkedRead)
      return false;

    if (fMarkedUnread != item.fMarkedUnread)
      return false;

    if (fStarred != item.fStarred)
      return false;

    if (fUnStarred != item.fUnStarred)
      return false;

    if (!isLabelsEquivalent(fAddedLabels, item.fAddedLabels))
      return false;

    if (!isLabelsEquivalent(fRemovedLabels, item.fRemovedLabels))
      return false;

    return true;
  }

  @SuppressWarnings("null")
  boolean isLabelsEquivalent(List<String> labelsA, List<String> labelsB) {
    if (labelsA == null && labelsB != null)
      return false;

    if (labelsA != null && labelsB == null)
      return false;

    if (labelsA == null && labelsB == null)
      return true;

    return Arrays.equals(labelsA.toArray(), labelsB.toArray());
  }
}