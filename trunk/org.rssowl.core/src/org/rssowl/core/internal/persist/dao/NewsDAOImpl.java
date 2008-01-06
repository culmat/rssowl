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

package org.rssowl.core.internal.persist.dao;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.runnable.NewsEventRunnable;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.service.PersistenceException;

import com.db4o.ext.Db4oException;
import com.db4o.query.Constraint;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A data-access-object for <code>INews</code>s.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public final class NewsDAOImpl extends AbstractEntityDAO<INews, NewsListener, NewsEvent> implements INewsDAO {

  private final ExecutorService fExecutorService = Executors.newFixedThreadPool(1);

  /** Default constructor using the specific IPersistable for this DAO */
  public NewsDAOImpl() {
    super(News.class, false);
  }

  @Override
  protected final void doSave(INews entity) {
    DBHelper.saveAndCascadeNews(fDb, entity, true);
  }

  @Override
  protected void preSaveAll(Collection<INews> objects) {
    for (INews news : objects) {
      DBHelper.putEventTemplate(createSaveEventTemplate(news));
      ((News) news).acquireReadLockSpecial();
    }
  }

  @Override
  protected void postSaveAll(Collection<INews> objects) {
    for (INews news : objects) {
      ((News) news).releaseReadLockSpecial();
    }
  }

  @Override
  protected final NewsEvent createDeleteEventTemplate(INews entity) {
    return new NewsEvent(null, entity, true);
  }

  @Override
  protected final NewsEvent createSaveEventTemplate(INews entity) {
    INews oldNews = DBHelper.peekPersistedNews(fDb, entity);
    return new NewsEvent(oldNews, entity, true);
  }

  public void setState(Collection<INews> news, State state, boolean affectEquivalentNews, boolean force) throws PersistenceException {
    if (news.isEmpty())
      return;
    fWriteLock.lock();
    Set<INews> changedNews = null;
    try {
      try {
        if (affectEquivalentNews) {
          /*
           * Give extra 25% size to take into account news that have same guid
           * or link.
           */
          int capacity = news.size() + (news.size() / 4);
          changedNews = new HashSet<INews>(capacity);
          for (INews newsItem : news) {
            if (newsItem.getId() == null)
              throw new IllegalArgumentException("newsItem was never saved to the database"); //$NON-NLS-1$

            List<INews> equivalentNews;

            if (newsItem.getGuid() != null && newsItem.getGuid().isPermaLink()) {
              equivalentNews = getNewsFromGuid(newsItem);
              if (equivalentNews.isEmpty()) {
                throw createIllegalException("No news were found with guid: " + //$NON-NLS-1$
                    newsItem.getGuid().getValue(), newsItem);
              }
            } else if (newsItem.getLink() != null) {
              equivalentNews = getNewsFromLink(newsItem);
              if (equivalentNews.isEmpty()) {
                throw createIllegalException("No news were found with link: " + //$NON-NLS-1$
                    newsItem.getLink().toString(), newsItem);
              }
            } else
              equivalentNews = Collections.singletonList(newsItem);

            changedNews.addAll(setState(equivalentNews, state, force));
          }
        } else {
          changedNews = setState(news, state, force);
        }
        preSaveAll(changedNews);
        save(changedNews);
        preCommit();
        fDb.commit();
      } catch (Db4oException e) {
        throw new PersistenceException(e);
      } finally {
        fWriteLock.unlock();
      }
      DBHelper.cleanUpAndFireEvents();
    } finally {
      postSaveAll(changedNews);
    }
  }

  @SuppressWarnings("unused")
  private void asyncSetState(final Collection<INews> news, final State state, final boolean affectEquivalentNews, final boolean force) throws PersistenceException {
    if (news.isEmpty())
      return;
    final NewsEventRunnable eventRunnable = new NewsEventRunnable();;
    final Lock setStateLock = new ReentrantLock();
    setStateLock.lock();
    final Condition condition = setStateLock.newCondition();
    fExecutorService.execute(new Runnable() {
      public void run() {
        Set<INews> changedNews = null;
        try {
          fWriteLock.lock();
          setStateLock.lock();

          if (affectEquivalentNews) {
            /*
             * Give extra 25% size to take into account news that have same guid or
             * link.
             */
            int capacity = news.size() + (news.size() / 4);
            changedNews = new HashSet<INews>(capacity);
            for (INews newsItem : news) {
              if (newsItem.getId() == null)
                throw new IllegalArgumentException("newsItem was never saved to the database"); //$NON-NLS-1$

              List<INews> equivalentNews;

              if (newsItem.getGuid() != null) {
                equivalentNews = getNewsFromGuid(newsItem);
                if (equivalentNews.isEmpty()) {
                  throw createIllegalException("No news were found with guid: " + //$NON-NLS-1$
                      newsItem.getGuid().getValue(), newsItem);
                }
              } else if (newsItem.getLink() != null) {
                equivalentNews = getNewsFromLink(newsItem);
                if (equivalentNews.isEmpty()) {
                  throw createIllegalException("No news were found with link: " + //$NON-NLS-1$
                      newsItem.getLink().toString(), newsItem);
                }
              } else
                equivalentNews = Collections.singletonList(newsItem);

              changedNews.addAll(setState(equivalentNews, state, force));
            }
          } else {
            changedNews = setState(news, state, force);
          }
          for (INews changedNewsItem : changedNews) {
            //TODO Investigate why we add the news twice to the event runnable
            //(we do the same in the finally block). This is harmless but
            //wasteful. Also we should not release the news locks before firing
            //the events.
            ((News) changedNewsItem).acquireReadLockSpecial();
            eventRunnable.addCheckedUpdateEvent(createSaveEventTemplate(changedNewsItem));
          }
          condition.signal();
          setStateLock.unlock();
          save(changedNews);
          fDb.commit();
        } catch (Db4oException e) {
          throw new PersistenceException(e);
        } finally {
          if (changedNews != null) {
            for (INews changedNewsItem : changedNews) {
              ((News) changedNewsItem).releaseReadLockSpecial();
              eventRunnable.addCheckedUpdateEvent(createSaveEventTemplate(changedNewsItem));
            }
          }
          DBHelper.cleanUpEvents();
          fWriteLock.unlock();
        }
      }
    });
    try {
      condition.awaitUninterruptibly();
    } finally {
      setStateLock.unlock();
    }
    eventRunnable.run();
  }

  private void save(Set<INews> newsList) {
    for (INews news : newsList) {
      fDb.ext().set(news, 1);
    }
  }

  private RuntimeException createIllegalException(String message, INews newsItem) {
    News dbNews = (News) DBHelper.peekPersistedNews(fDb, newsItem);
    if (dbNews == null)
      return new IllegalArgumentException("The news has been deleted from the persistence layer: " + newsItem);

    return new IllegalStateException(message + ". This news in the db looks like: " //$NON-NLS-1$
        + dbNews.toLongString());
  }

  private List<INews> getNewsFromGuid(INews newsItem) {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fGuidValue").constrain(newsItem.getGuid().getValue()); //$NON-NLS-1$
    query.descend("fCopy").constrain(false);
    return activateAll(getList(query));
  }

  private List<INews> getNewsFromLink(INews newsItem) {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fLinkText").constrain(newsItem.getLink().toString()); //$NON-NLS-1$
    query.descend("fCopy").constrain(false);
    return activateAll(getList(query));
  }

  private Set<INews> setState(Collection<INews> news, State state, boolean force) {
    Set<INews> changedNews = new HashSet<INews>(news.size());
    for (INews newsItem : news) {
      if (newsItem.getState() != state || force) {
        newsItem.setState(state);
        changedNews.add(newsItem);
      }
    }
    return changedNews;
  }

  public Collection<INews> loadAll(FeedLinkReference feedRef, Set<State> states) {
    Assert.isNotNull(feedRef, "feedRef");
    Assert.isNotNull(states, "states");
    if (states.isEmpty())
      return new ArrayList<INews>(0);

    try {
      Query query = fDb.query();
      query.constrain(News.class);
      query.descend("fFeedLink").constrain(feedRef.getLink().toString());
      if (!states.containsAll(EnumSet.allOf(INews.State.class))) {
        Constraint constraint = null;
        for (INews.State state : states) {
          if (constraint == null)
            constraint = query.descend("fStateOrdinal").constrain(state.ordinal());
          else
            constraint = query.descend("fStateOrdinal").constrain(state.ordinal()).or(constraint);
        }
      }

      Collection<INews> news = getList(query);
      activateAll(news);

      return new ArrayList<INews>(news);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public Collection<INews> loadAll(ILabel label) {
    Query query = fDb.query();
    query.constrain(News.class);
    query.descend("fLabels").constrain(label);

    return new ArrayList<INews>(getList(query));
  }
}
