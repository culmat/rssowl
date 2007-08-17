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

package org.rssowl.core.internal;

import org.rssowl.core.IApplicationService;
import org.rssowl.core.internal.persist.MergeResult;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.DBManager;
import org.rssowl.core.internal.persist.service.DatabaseEvent;
import org.rssowl.core.internal.persist.service.DatabaseListener;
import org.rssowl.core.internal.persist.service.EventManager;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.RetentionStrategy;

import com.db4o.ObjectContainer;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * db4o implementation of IApplicationService
 */
public class ApplicationServiceImpl implements IApplicationService {
  private ObjectContainer fDb;
  private ReadWriteLock fLock;
  private Lock fWriteLock;

  /**
   * Creates an instance of this class.
   */
  public ApplicationServiceImpl() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
        fLock = event.getLock();
        fWriteLock = fLock.writeLock();
      }

      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }

  /*
   * @see org.rssowl.core.model.dao.IApplicationLayer#handleFeedReload(org.rssowl.core.model.persist.IBookMark,
   * org.rssowl.core.model.persist.IFeed,
   * org.rssowl.core.model.persist.IConditionalGet, boolean)
   */
  public final void handleFeedReload(IBookMark bookMark, IFeed emptyFeed, IConditionalGet conditionalGet, boolean deleteConditionalGet) {
    fWriteLock.lock();
    try {
      /* Resolve reloaded Feed */
      IFeed feed = bookMark.getFeedLinkReference().resolve();

      /* Feed could have been deleted meanwhile! */
      if (feed == null)
        return;

      /* Copy over Properties to reloaded Feed to keep them */
      Map<String, ? > feedProperties = feed.getProperties();
      if (feedProperties != null) {
        feedProperties.entrySet();
        for (Map.Entry<String, ? > entry : feedProperties.entrySet())
          emptyFeed.setProperty(entry.getKey(), entry.getValue());
      }

      /* Merge with existing (remember number of added new news) */
      List<INews> newNewsBeforeMerge = feed.getNewsByStates(EnumSet.of(INews.State.NEW));
      MergeResult mergeResult = feed.mergeAndCleanUp(emptyFeed);
      List<INews> newNewsAdded = getNewNewsAdded(feed, newNewsBeforeMerge);
      updateStateOfUnsavedNewNews(newNewsAdded);

      /* Retention Policy */
      List<INews> deletedNews = RetentionStrategy.process(bookMark, feed, newNewsAdded.size());

      for (INews news : deletedNews)
        mergeResult.addUpdatedObject(news);

      saveFeed(mergeResult);

      /* Update Conditional GET */
      if (conditionalGet != null) {
        if (deleteConditionalGet)
          fDb.delete(conditionalGet);
        else
          fDb.ext().set(conditionalGet, 1);
      }
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  private List<INews> getNewNewsAdded(IFeed feed, List<INews> newNewsBeforeMerge) {
    List<INews> newNewsAdded = feed.getNewsByStates(EnumSet.of(INews.State.NEW));
    newNewsAdded.removeAll(newNewsBeforeMerge);
    return newNewsAdded;
  }

  private <T> List<T> activateAll(List<T> list) {
    for (T o : list)
      fDb.ext().activate(o, Integer.MAX_VALUE);

    return list;
  }

  private void updateStateOfUnsavedNewNews(List<INews> news) {
    for (INews newsItem : news) {
      List<INews> equivalentNews = Collections.emptyList();
      if (newsItem.getGuid() != null)
        equivalentNews = getNewsFromGuid(newsItem);
      else if (newsItem.getLink() != null)
        equivalentNews = getNewsFromLink(newsItem);

      if (!equivalentNews.isEmpty())
        newsItem.setState(equivalentNews.get(0).getState());
    }
  }

  @SuppressWarnings("unchecked")
  private List<INews> getNewsFromLink(INews newsItem) {
    Query query = fDb.query();
    query.constrain(News.class);
    query.descend("fLinkText").constrain(newsItem.getLink().toString()); //$NON-NLS-1$
    return activateAll(query.execute());
  }

  @SuppressWarnings("unchecked")
  private List<INews> getNewsFromGuid(INews newsItem) {
    Query query = fDb.query();
    query.constrain(News.class);
    query.descend("fGuidValue").constrain(newsItem.getGuid().getValue()); //$NON-NLS-1$
    return activateAll(query.execute());
  }

  private void saveFeed(MergeResult mergeResult) {
    for (Object o : mergeResult.getRemovedObjects()) {
      /* We know that in these cases, the parent entity will be updated */
      if (o instanceof INews)
        EventManager.getInstance().addItemBeingDeleted(((INews) o).getFeedReference());
      else if (o instanceof IAttachment)
        EventManager.getInstance().addItemBeingDeleted(((IAttachment) o).getNews());

      fDb.delete(o);
    }

    List<Object> updatedEntities = new ArrayList<Object>(mergeResult.getUpdatedObjects());
    for (Object o : updatedEntities) {
      if (o instanceof INews)
        DBHelper.saveNews(fDb, (INews) o);
      else if (o instanceof IFeed)
        fDb.ext().set(o, 2);
      else
        fDb.ext().set(o, 1);
    }
  }
}