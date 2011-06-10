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
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.util.BatchedBuffer;
import org.rssowl.core.util.BatchedBuffer.Receiver;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;

import java.util.Collection;
import java.util.Set;

/**
 * A service that listens to changes of {@link INews} and then synchronizes with
 * an online server to notify about changes.
 *
 * @author bpasero
 */
public class SyncService {

  /* Delay in Milies before syncing */
  private static final int SYNC_DELAY = 2000;

  private NewsListener fListener;
  private final BatchedBuffer<NewsEvent> fSynchronizer;

  /* Receiver to process news events for syncing */
  private class SyncReceiver implements Receiver<NewsEvent> {
    public void receive(Collection<NewsEvent> events, IProgressMonitor monitor) {
      try {
        sync(events, monitor);
      } catch (ConnectionException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  /**
   * Starts the synchronizer by listening to news events.
   */
  public SyncService() {
    fSynchronizer = new BatchedBuffer<NewsEvent>(new SyncReceiver(), SYNC_DELAY);
    registerListeners();
  }

  private void registerListeners() {
    fListener = new NewsListener() {
      public void entitiesUpdated(Set<NewsEvent> events) {
        fSynchronizer.addAll(events);
      }

      public void entitiesDeleted(Set<NewsEvent> events) {
        /* Not Used For Syncing */
      }

      public void entitiesAdded(Set<NewsEvent> events) {
        /* Not Used For Syncing */
      }
    };
    DynamicDAO.addEntityListener(INews.class, fListener);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(INews.class, fListener);
  }

  /**
   * Stops the Synchronizer.
   */
  public void stopService() {
    unregisterListeners();
    fSynchronizer.cancel();
  }

  //TODO There seems to be an API call api/0/mark-all-as-read that could be used as optimization if user marks a feed as read
  //TODO Also looks like another parameter "ts" is used maybe to avoid to mark too many items as read?

  //API
  // a=[tag to add]
  // r=[tag to remove]
  // s=[feed the item belongs to]
  // i=[identifier]
  // T=[auth token]
  // --> all form url encoded!

  private void sync(Collection<NewsEvent> events, IProgressMonitor monitor) throws ConnectionException {
    for (NewsEvent event : events) {
      INews item = event.getEntity();
      if (!SyncUtils.isSynchronized(item) || event.getOldNews() == null)
        continue;

      if (isCanceled(monitor))
        return;

      //      ICredentialsProvider p = Owl.getConnectionService().getCredentialsProvider(URI.create(SyncUtils.GOOGLE_LOGIN));
      //      ICredentials creds = p.getAuthCredentials(URI.create(SyncUtils.GOOGLE_LOGIN), null);
      //
      //      String authToken = SyncUtils.getGoogleAuthToken(creds.getUsername(), creds.getPassword(), monitor);
      //      String token = SyncUtils.getGoogleToken(creds.getUsername(), creds.getPassword(), monitor);
      //      String itemId = item.getGuid().getValue();
      //      String streamId = item.getInReplyTo();
      //
      //      INews.State newState = item.getState();
      //      INews.State oldState = event.getOldNews().getState();
      //
      //      boolean newSticky = item.isFlagged();
      //      boolean oldSticky = event.getOldNews().isFlagged();
      //
      //      String state;
      //      if (true)
      //        state = "user/-/state/com.google/starred"; //$NON-NLS-1$
      //      else
      //        state = "user/-/state/com.google/read"; //$NON-NLS-1$
      //
      //      /* Sync: Mark News Read */
      //      if (newState == INews.State.READ && oldState != INews.State.READ && event.getOldNews().isVisible()) {
      //        Map<String, String> headers = new HashMap<String, String>();
      //        headers.put("Content-Type", "application/x-www-form-urlencoded"); //$NON-NLS-1$ //$NON-NLS-2$
      //        headers.put("Authorization", SyncUtils.getGoogleAuthorizationHeader(authToken)); //$NON-NLS-1$
      //
      //        Map<Object, Object> properties = new HashMap<Object, Object>();
      //        properties.put(IConnectionPropertyConstants.HEADERS, headers);
      //        properties.put(IConnectionPropertyConstants.POST, Boolean.TRUE);
      //
      //        String url = "http://www.google.com/reader/api/0/edit-tag?client=rssowl"; //$NON-NLS-1$
      //
      //        String body = ""; //$NON-NLS-1$
      //        body += "T=" + token; //$NON-NLS-1$
      //        body += "&i=" + itemId; //$NON-NLS-1$
      //        body += "&s=" + streamId; //$NON-NLS-1$
      //        body += "&r=" + state; //$NON-NLS-1$
      //
      //        properties.put(IConnectionPropertyConstants.POST_BODY, body);
      //
      //        URI uri = URI.create(url);
      //
      //        IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);
      //        InputStream inS = handler.openStream(uri, monitor, properties);
      //        try {
      //          inS.close();
      //        } catch (IOException e) {
      //          e.printStackTrace();
      //        }
      //      }
      //
      //      /* Sync: Mark News Unread */
      //      else if (newState == INews.State.UNREAD && oldState == INews.State.READ) {
      //
      //      }
    }
  }

  private boolean isCanceled(IProgressMonitor monitor) {
    return Controller.getDefault().isShuttingDown() || (monitor != null && monitor.isCanceled());
  }
}