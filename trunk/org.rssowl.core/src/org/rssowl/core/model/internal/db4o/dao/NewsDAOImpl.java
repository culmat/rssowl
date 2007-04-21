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
package org.rssowl.core.model.internal.db4o.dao;

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.model.internal.persist.News;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.events.ModelEvent;
import org.rssowl.core.persist.events.NewsEvent;
import org.rssowl.core.persist.events.NewsListener;

import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NewsDAOImpl extends AbstractEntityDAO<INews, NewsListener, NewsEvent>
    implements INewsDAO   {

  public NewsDAOImpl() {
    super(News.class, false);
  }
  
  @Override
  protected final void doSave(INews entity) {
    DBHelper.saveAndCascadeNews(fDb, entity, true);
  }

  @Override
  protected final NewsEvent createDeleteEventTemplate(INews entity) {
    return new NewsEvent(null, entity, true);
  }

  @Override
  protected final NewsEvent createSaveEventTemplate(INews entity) {
    INews oldNews = fDb.ext().peekPersisted(entity, 2, true);
    return new NewsEvent(oldNews, entity, true);
  }

  public void setState(Collection<INews> news, State state, boolean affectEquivalentNews, boolean force) throws PersistenceException {
    if (news.isEmpty())
      return;
    fWriteLock.lock();
    try {
      Set<INews> changedNews;

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
          }
          else if (newsItem.getLink() != null) {
            equivalentNews = getNewsFromLink(newsItem);
            if (equivalentNews.isEmpty()) {
              throw createIllegalException("No news were found with link: " + //$NON-NLS-1$
                  newsItem.getLink().toString(), newsItem);
            }
          }
          else
            equivalentNews = Collections.singletonList(newsItem);

          changedNews.addAll(setState(equivalentNews, state, force));
        }
      } else {
        changedNews = setState(news, state, force);
      }
      save(changedNews);
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  private void save(Set<INews> newsList) {
    for (INews news : newsList) {
      ModelEvent newsEventTemplate = createSaveEventTemplate(news);
      DBHelper.putEventTemplate(newsEventTemplate);
      fDb.ext().set(news, 1);
    }
  }
  
  private RuntimeException createIllegalException(String message, INews newsItem) {
    News dbNews = (News) fDb.ext().peekPersisted(newsItem, 2, true);
    if (dbNews == null)
      return new IllegalArgumentException("The news has been deleted from the persistence layer: " + newsItem);

    return new IllegalStateException(message + ". This news in the db looks like: "  //$NON-NLS-1$
        + dbNews.toLongString());
  }
  
  private List<INews> getNewsFromGuid(INews newsItem) {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fGuidValue").constrain(newsItem.getGuid().getValue()); //$NON-NLS-1$
    return activateAll(getObjectSet(query));
  }
  
  private List<INews> getNewsFromLink(INews newsItem) {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fLinkText").constrain(newsItem.getLink().toString()); //$NON-NLS-1$
    return activateAll(getObjectSet(query));
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
}
