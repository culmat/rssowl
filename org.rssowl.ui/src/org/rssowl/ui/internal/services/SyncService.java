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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.internal.newsaction.MarkReadNewsAction;
import org.rssowl.core.internal.newsaction.MarkStickyNewsAction;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.SearchFilterAdapter;
import org.rssowl.core.util.BatchedBuffer;
import org.rssowl.core.util.BatchedBuffer.Receiver;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A service that listens to changes of {@link INews} and then synchronizes with
 * an online server to notify about changes.
 *
 * @author bpasero
 */
//TODO Must properly handle AuthenticationExceptions, how would the user know otherwise or be able to login?
public class SyncService {

  /* Delay in Milies before syncing */
  private static final int SYNC_DELAY = 5000;

  /* Set of unread states */
  private static final Set<INews.State> UNREAD_STATES = EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED);

  /* HTTP Constants */
  private static final String REQUEST_HEADER_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$
  private static final String REQUEST_HEADER_AUTHORIZATION = "Authorization"; //$NON-NLS-1$
  private static final String CONTENT_TYPE_FORM_ENCODED = "application/x-www-form-urlencoded"; //$NON-NLS-1$

  private final BatchedBuffer<SyncItem> fSynchronizer;
  private NewsListener fNewsListener;
  private SearchFilterAdapter fSearchFilterListener;

  /* Contains everything needed to synchronize */
  private static class SyncItem {
    private final String fId;
    private final String fStreamId;
    private boolean fMarkedRead;
    private boolean fMarkedUnread;
    private boolean fStarred;
    private boolean fUnStarred;
    private List<String> fAddedLabels;
    private List<String> fRemovedLabels;

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
    }

    void setMarkedUnread() {
      fMarkedUnread = true;
    }

    void setStarred() {
      fStarred = true;
    }

    void setUnStarred() {
      fUnStarred = true;
    }

    void addLabel(String label) {
      if (fAddedLabels == null)
        fAddedLabels = new ArrayList<String>(3);

      fAddedLabels.add(label);
    }

    void removeLabel(String label) {
      if (fRemovedLabels == null)
        fRemovedLabels = new ArrayList<String>(1);

      fRemovedLabels.add(label);
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
        fMarkedRead = true;

      if (item.fMarkedUnread)
        fMarkedUnread = true;

      if (item.fStarred)
        fStarred = true;

      if (item.fUnStarred)
        fUnStarred = true;

      if (item.fAddedLabels != null)
        fAddedLabels = item.fAddedLabels;

      if (item.fRemovedLabels != null)
        fRemovedLabels = item.fRemovedLabels;
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

  /* Receiver to process news events for syncing */
  private class SyncReceiver implements Receiver<SyncItem> {
    public void receive(Collection<SyncItem> items, IProgressMonitor monitor) {
      try {
        sync(items, monitor);
      } catch (ConnectionException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  /**
   * Starts the synchronizer by listening to news events.
   */
  public SyncService() {
    fSynchronizer = new BatchedBuffer<SyncItem>(new SyncReceiver(), SYNC_DELAY);
    registerListeners();
  }

  private void registerListeners() {

    /* News Listener */
    fNewsListener = new NewsAdapter() {
      @Override
      public void entitiesUpdated(Set<NewsEvent> events) {
        fSynchronizer.addAll(filter(events));
      }
    };
    DynamicDAO.addEntityListener(INews.class, fNewsListener);

    /* News Filter Listener */
    fSearchFilterListener = new SearchFilterAdapter() {
      @Override
      public void filterApplied(ISearchFilter filter, Collection<INews> news) {
        fSynchronizer.addAll(filter(filter, news));
      }
    };
    DynamicDAO.addEntityListener(ISearchFilter.class, fSearchFilterListener);
  }

  private Collection<SyncItem> filter(ISearchFilter filter, Collection<INews> news) {
    List<SyncItem> filteredEvents = new ArrayList<SyncItem>();
    for (INews item : news) {
      if (!SyncUtils.isSynchronized(item))
        continue;

      SyncItem syncItem = toSyncItem(filter, item);
      if (syncItem != null)
        filteredEvents.add(syncItem);
    }

    return filteredEvents;
  }

  private Collection<SyncItem> filter(Set<NewsEvent> events) {
    List<SyncItem> filteredEvents = new ArrayList<SyncItem>();
    for (NewsEvent event : events) {
      if (event.getOldNews() == null || !SyncUtils.isSynchronized(event.getEntity()))
        continue;

      SyncItem syncItem = toSyncItem(event);
      if (syncItem != null)
        filteredEvents.add(syncItem);
    }

    return filteredEvents;
  }

  private SyncItem toSyncItem(NewsEvent event) {
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

  private SyncItem toSyncItem(ISearchFilter filter, INews item) {
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

  private SyncItem toSyncItem(INews news) {
    String itemId = news.getGuid().getValue();
    String streamId = news.getInReplyTo();

    return new SyncItem(itemId, streamId);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
    DynamicDAO.removeEntityListener(ISearchFilter.class, fSearchFilterListener);
  }

  /**
   * Stops the Synchronizer.
   */
  public void stopService() {
    unregisterListeners();
    fSynchronizer.cancel();
  }

  private void sync(Collection<SyncItem> items, IProgressMonitor monitor) throws ConnectionException {

    /* Return on cancellation or shutdown */
    if (isCanceled(monitor))
      return;

    /* Group Sync Items by Feed and Merge Duplictates */
    Map<String, Map<String, SyncItem>> mapFeedToSyncItems = new HashMap<String, Map<String, SyncItem>>();
    for (SyncItem item : items) {
      Map<String, SyncItem> streamItems = mapFeedToSyncItems.get(item.getStreamId());
      if (streamItems == null) {
        streamItems = new HashMap<String, SyncItem>();
        mapFeedToSyncItems.put(item.getStreamId(), streamItems);
      }

      SyncItem existingItem = streamItems.get(item.getId());
      if (existingItem == null)
        streamItems.put(item.getId(), item);
      else
        existingItem.merge(item);
    }

    /* Return on cancellation or shutdown */
    if (isCanceled(monitor))
      return;

    /* Obtain API Token */
    String token = getGoogleApiToken(monitor);
    String authToken = SyncUtils.getGoogleAuthToken(null, null, false, monitor); //Already up to date from previous call to getGoogleApiToken()
    if (token == null || authToken == null)
      throw new ConnectionException(Activator.getDefault().createErrorStatus("Unable to obtain a token for Google API access.")); //$NON-NLS-1$

    /* Return on cancellation or shutdown */
    if (isCanceled(monitor))
      return;

    /* Synchronize for each Stream */
    Set<Entry<String, Map<String, SyncItem>>> entries = mapFeedToSyncItems.entrySet();
    for (Entry<String, Map<String, SyncItem>> entry : entries) {
      Collection<SyncItem> syncItems = (entry.getValue() != null) ? entry.getValue().values() : Collections.<SyncItem> emptyList();
      if (syncItems.isEmpty())
        continue;

      /* Find Equivalent Items to Sync with 1 Connection */
      List<List<SyncItem>> equivalentItemLists = new ArrayList<List<SyncItem>>();
      List<SyncItem> currentItemList = new ArrayList<SyncService.SyncItem>();
      equivalentItemLists.add(currentItemList);
      for (SyncItem item : syncItems) {
        if (currentItemList.isEmpty())
          currentItemList.add(item);
        else if (item.isEquivalent(currentItemList.get(0)))
          currentItemList.add(item);
        else {
          currentItemList = new ArrayList<SyncService.SyncItem>();
          currentItemList.add(item);
          equivalentItemLists.add(currentItemList);
        }
      }

      /* For each list of equivalent items */
      for (List<SyncItem> equivalentItems : equivalentItemLists) {
        if (equivalentItems.isEmpty())
          continue;

        /* Connection Headers */
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(REQUEST_HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM_ENCODED);
        headers.put(REQUEST_HEADER_AUTHORIZATION, SyncUtils.getGoogleAuthorizationHeader(authToken));

        /* POST Parameters */
        Map<String, String[]> parameters = new HashMap<String, String[]>();
        parameters.put(SyncUtils.API_PARAM_TOKEN, new String[] { token });

        List<String> identifiers = new ArrayList<String>();
        List<String> streamIds = new ArrayList<String>();
        Set<String> tagsToAdd = new HashSet<String>();
        Set<String> tagsToRemove = new HashSet<String>();
        for (SyncItem item : equivalentItems) {
          identifiers.add(item.getId());
          streamIds.add(item.getStreamId());

          if (item.isMarkedRead())
            tagsToAdd.add(SyncUtils.CATEGORY_READ);

          if (item.isMarkedUnread()) {
            tagsToAdd.add(SyncUtils.CATEGORY_UNREAD);
            tagsToRemove.add(SyncUtils.CATEGORY_READ);
          }

          if (item.isStarred())
            tagsToAdd.add(SyncUtils.CATEGORY_STARRED);

          if (item.isUnStarred())
            tagsToRemove.add(SyncUtils.CATEGORY_STARRED);

          List<String> addedLabels = item.getAddedLabels();
          if (addedLabels != null) {
            for (String label : addedLabels) {
              tagsToAdd.add(SyncUtils.CATEGORY_LABEL_PREFIX + label);
            }
          }

          List<String> removedLabels = item.getRemovedLabels();
          if (removedLabels != null) {
            for (String label : removedLabels) {
              tagsToRemove.add(SyncUtils.CATEGORY_LABEL_PREFIX + label);
            }
          }
        }

        parameters.put(SyncUtils.API_PARAM_IDENTIFIER, identifiers.toArray(new String[identifiers.size()]));
        parameters.put(SyncUtils.API_PARAM_STREAM, streamIds.toArray(new String[streamIds.size()]));
        if (!tagsToAdd.isEmpty())
          parameters.put(SyncUtils.API_PARAM_TAG_TO_ADD, tagsToAdd.toArray(new String[tagsToAdd.size()]));
        if (!tagsToRemove.isEmpty())
          parameters.put(SyncUtils.API_PARAM_TAG_TO_REMOVE, tagsToRemove.toArray(new String[tagsToRemove.size()]));

        /* Connection Properties */
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(IConnectionPropertyConstants.HEADERS, headers);
        properties.put(IConnectionPropertyConstants.POST, Boolean.TRUE);
        properties.put(IConnectionPropertyConstants.PARAMETERS, parameters);

        /* Return on cancellation or shutdown */
        if (isCanceled(monitor))
          return;

        URI uri = URI.create(SyncUtils.GOOGLE_EDIT_TAG_URL + "?client=scroll"); //$NON-NLS-1$
        IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);
        InputStream inS = null;
        try {
          inS = handler.openStream(uri, monitor, properties);
        } finally {
          if (inS != null) {
            try {
              inS.close();
            } catch (IOException e) {
              throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
            }
          }
        }

        /* Return on cancellation or shutdown */
        if (isCanceled(monitor))
          return;
      }
    }
  }

  private String getGoogleApiToken(IProgressMonitor monitor) throws ConnectionException {
    ICredentialsProvider provider = Owl.getConnectionService().getCredentialsProvider(URI.create(SyncUtils.GOOGLE_LOGIN_URL));
    ICredentials creds = provider.getAuthCredentials(URI.create(SyncUtils.GOOGLE_LOGIN_URL), null);
    if (creds == null)
      throw new AuthenticationRequiredException(null, Status.CANCEL_STATUS);

    return SyncUtils.getGoogleApiToken(creds.getUsername(), creds.getPassword(), monitor);
  }

  private boolean isCanceled(IProgressMonitor monitor) {
    return Controller.getDefault().isShuttingDown() || (monitor != null && monitor.isCanceled());
  }
}