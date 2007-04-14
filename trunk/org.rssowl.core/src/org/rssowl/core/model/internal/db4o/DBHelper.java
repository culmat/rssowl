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
package org.rssowl.core.model.internal.db4o;

import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.EventsMap;
import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.runnable.EventRunnable;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public class DBHelper {

  public static final void cleanUpAndFireEvents() {
    List<EventRunnable> eventNotifiers = EventsMap.getInstance().removeEventRunnables();
    EventsMap.getInstance().removeEventTemplatesMap();
    EventManager.getInstance().clear();
    fireEvents(eventNotifiers);
  }
  
  public static final void fireEvents(List<EventRunnable> eventNotifiers) {
    if (eventNotifiers == null) {
      return;
    }
    for (EventRunnable runnable : eventNotifiers) {
      runnable.run();
    }
  }

  public static final void putEventTemplate(ModelEvent modelEvent) {
    int id = System.identityHashCode(modelEvent.getEntity());
    EventsMap.getInstance().putEventTemplate(id, modelEvent);
  }
  
  public static final void saveFeed(ObjectContainer db, IFeed feed) {
    if (feed.getId() == null && feedExists(db, feed.getLink()))
        throw new IllegalArgumentException("This feed already exists, but it has no id."); //$NON-NLS-1$
    
    ModelEvent feedEventTemplate = new FeedEvent(feed, true);
    DBHelper.putEventTemplate(feedEventTemplate);
    saveAndCascadeAllNews(db, feed.getNews(), false);
    saveEntities(db, feed.getCategories());
    saveEntity(db, feed.getAuthor());
    saveEntity(db, feed.getImage());
    
    db.ext().set(feed, 2);
  }
  
  private static void saveEntity(ObjectContainer db, IPersistable entity) {
    if (entity != null)
      db.set(entity);
  }
  

  private static void saveEntities(ObjectContainer db, List<? extends IEntity> entities) {
    for (IEntity entity : entities)
      db.ext().set(entity, 1);
  }

  static void saveAndCascadeAllNews(ObjectContainer db, Collection<INews> newsCollection,
      boolean root) {
    for (INews news : newsCollection)
      saveAndCascadeNews(db, news, root);
  }
  
  static void saveNews(ObjectContainer db, INews news) {
    INews oldNews = db.ext().peekPersisted(news, 2, true);
    if (oldNews != null) {
      ModelEvent newsEventTemplate = new NewsEvent(oldNews, news, false);
      DBHelper.putEventTemplate(newsEventTemplate);
    }
    db.ext().set(news, 2);
  }
  
  static final boolean feedExists(ObjectContainer db, URI link) {
    return getFeedSet(db, link).hasNext();
  }
  
  @SuppressWarnings("unchecked")
  private static ObjectSet<IFeed> getFeedSet(ObjectContainer db, URI link){
    Query query = db.query();
    query.constrain(Feed.class);
    query.descend("fLinkText").constrain(link.toString()); //$NON-NLS-1$
    ObjectSet<IFeed> set = query.execute();
    return set;
  }

  static final IFeed loadFeed(ObjectContainer db, URI link, Integer activationDepth) {
    try {
      ObjectSet<IFeed> set = getFeedSet(db, link);
      if (set.hasNext()) {
        IFeed feed = set.next();
        if (activationDepth != null)
          db.ext().activate(feed, activationDepth.intValue());

        return feed;
      }
      return null;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public static final void saveAndCascadeNews(ObjectContainer db, INews news, boolean root) {
    INews oldNews = db.ext().peekPersisted(news, 2, true);
    if (oldNews != null || root) {
      ModelEvent event = new NewsEvent(oldNews, news, root);
      putEventTemplate(event);
    }
    saveEntities(db, news.getCategories());
    saveEntity(db, news.getAuthor());
    saveEntities(db, news.getAttachments());
    saveEntity(db, news.getSource());
    db.ext().set(news, 2);
  }
}