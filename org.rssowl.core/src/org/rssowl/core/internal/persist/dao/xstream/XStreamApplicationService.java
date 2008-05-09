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
package org.rssowl.core.internal.persist.dao.xstream;

import org.rssowl.core.IApplicationService;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.MergeResult;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.dao.IDescriptionDAO;
import org.rssowl.core.internal.persist.search.ModelSearchImpl;
import org.rssowl.core.internal.persist.service.EventsMap2;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.internal.persist.service.PersistHelper;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.RetentionStrategy;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class XStreamApplicationService implements IApplicationService   {

  private volatile XStreamFeedDAO fFeedDAO;
  private volatile ManualEventManager fEventManager;
  private volatile IDescriptionDAO fDescriptionDAO;
  private volatile IBookMarkDAO fBookMarkDAO;
  private volatile INewsCounterDAO fNewsCounterDAO;

  /**
   * Creates an instance of this class.
   */
  public XStreamApplicationService() {
    super();
  }

  public void startup(IDescriptionDAO descriptionDAO, XStreamFeedDAO feedDao, IBookMarkDAO bookMarkDao, INewsCounterDAO newsCounterDao, ManualEventManager eventManager) {
    fDescriptionDAO = descriptionDAO;
    fFeedDAO = feedDao;
    fBookMarkDAO = bookMarkDao;
    fNewsCounterDAO = newsCounterDao;
    fEventManager = eventManager;
  }

  /*
   * @see org.rssowl.core.model.dao.IApplicationLayer#handleFeedReload(org.rssowl.core.model.persist.IBookMark,
   * org.rssowl.core.model.persist.IFeed,
   * org.rssowl.core.model.persist.IConditionalGet, boolean)
   */
  public final void handleFeedReload(IBookMark bookMark, IFeed emptyFeed, IConditionalGet conditionalGet, boolean deleteConditionalGet) {
    MergeResult mergeResult = null;
    /* Resolve reloaded Feed */
    IFeed feed = bookMark.getFeedLinkReference().resolve();

    /* Feed could have been deleted meanwhile! */
    if (feed == null)
      return;

    /* Copy over Properties to reloaded Feed to keep them */
    Map<String, Serializable> feedProperties = feed.getProperties();
    if (feedProperties != null) {
      feedProperties.entrySet();
      for (Map.Entry<String, Serializable> entry : feedProperties.entrySet())
        emptyFeed.setProperty(entry.getKey(), entry.getValue());
    }

    /* Merge with existing */
    mergeResult = feed.mergeAndCleanUp(emptyFeed);
    List<INews> newNewsAdded = getNewNewsAdded(feed);

    /* Update Date of last added news in Bookmark */
    if (!newNewsAdded.isEmpty()) {
      Date mostRecentDate = DateUtils.getRecentDate(newNewsAdded);
      Date previousMostRecentDate = bookMark.getMostRecentNewsDate();
      if (previousMostRecentDate == null || mostRecentDate.after(previousMostRecentDate)) {
        bookMark.setMostRecentNewsDate(mostRecentDate);
        fBookMarkDAO.save(bookMark);
      }
    }

    updateStateOfUnsavedNewNews(newNewsAdded);

    /* Retention Policy */
    List<INews> deletedNews = RetentionStrategy.process(bookMark, feed, newNewsAdded.size());

    for (INews news : deletedNews)
      mergeResult.addUpdatedObject(news);

    IDGenerator generator = Owl.getPersistenceService().getIDGenerator();
    for (INews news : newNewsAdded) {
      news.setId(generator.getNext());
      String description = ((News) news).getTransientDescription();
      if (description != null) {
        mergeResult.addUpdatedObject(new Description(news, description));
      }
    }

    EventsMap2 eventsMap = null;
    try {
      lockNewsObjects(mergeResult);
      eventsMap = saveFeed(feed, mergeResult);

      //FIXME Update Conditional Get
      /* Update Conditional GET */
//        if (conditionalGet != null) {
//          if (deleteConditionalGet)
//            fDb.delete(conditionalGet);
//          else
//            fDb.ext().set(conditionalGet, 1);
//        }
      //FIXME Call precommit
      //DBHelper.preCommit(fDb);
    } finally {
      unlockNewsObjects(mergeResult);
    }
    XStreamHelper.fireEvents(eventsMap);
  }

  private void lockNewsObjects(MergeResult mergeResult) {
    for (Object object : mergeResult.getUpdatedObjects()) {
      if (object instanceof News) {
        ((News) object).acquireReadLockSpecial();
      }
    }
  }

  private void unlockNewsObjects(MergeResult mergeResult) {
    if (mergeResult != null) {
      for (Object object : mergeResult.getUpdatedObjects()) {
        if (object instanceof News) {
          News news = (News) object;
          news.releaseReadLockSpecial();
          news.clearTransientDescription();
        }
      }
    }
  }

  private List<INews> getNewNewsAdded(IFeed feed) {
    List<INews> newsList = feed.getNewsByStates(EnumSet.of(INews.State.NEW));

    for (ListIterator<INews> it = newsList.listIterator(newsList.size()); it.hasPrevious(); ) {
      INews news = it.previous();
      /* Relies on the fact that news added during merge have no id assigned yet. */
      if (news.getId() != null)
        it.remove();
    }
    return newsList;
  }

  private void updateStateOfUnsavedNewNews(List<INews> news) {
    if (news.isEmpty())
      return;

    List<URI> links = new ArrayList<URI>();
    List<IGuid> guids = new ArrayList<IGuid>();
    for (INews newsItem : news) {
      if (newsItem.getGuid() != null)
        guids.add(newsItem.getGuid());
      else if (newsItem.getLink() != null)
        links.add(newsItem.getLink());
    }

    ModelSearchImpl modelSearch = (ModelSearchImpl) Owl.getPersistenceService().getModelSearch();
    Map<URI, List<NewsReference>> linkToNewsRefs = modelSearch.searchNewsByLinks(links, false);
    Map<IGuid, List<NewsReference>> guidToNewsRefs = modelSearch.searchNewsByGuids(guids, false);
    for (INews newsItem : news) {
      List<NewsReference> equivalentNewsRefs = guidToNewsRefs.get(newsItem.getGuid());
      if (equivalentNewsRefs != null)
        newsItem.setState(equivalentNewsRefs.get(0).resolve().getState());
      else {
        equivalentNewsRefs = linkToNewsRefs.get(newsItem.getLink());
        if (equivalentNewsRefs != null)
          newsItem.setState(equivalentNewsRefs.get(0).resolve().getState());
      }
    }
  }

  /**
   * All news have an id assigned at this stage. For the other object types, we
   * can rely on the id being null to know which event to fire.
   * @return
   */
  private EventsMap2 saveFeed(IFeed feed, MergeResult mergeResult) {
    Map<IEntity, ModelEvent> emptyMap = Collections.emptyMap();
    Set<Long> descriptionsRemovedOrUpdated = new HashSet<Long>();
    EventsMap2 eventsMap = new EventsMap2();

    /*
     * We only need to delete description manually, the rest will be done
     * automatically as we save the updated feeds.
     */
    for (Object o : mergeResult.getRemovedObjects()) {
      if (o instanceof Description) {
        descriptionsRemovedOrUpdated.add(((Description) o).getNews().getId());
        fDescriptionDAO.delete((Description) o);
      }
      if (o instanceof IEntity) {
        eventsMap.putRemoveEvent(fEventManager.createModelEvent(emptyMap,
          (IEntity) o));
      }
    }

    boolean shouldFireFeedEvent = false;
    Map<Long, INews> newsMap = new HashMap<Long, INews>();
    for (Object o : mergeResult.getUpdatedObjects()) {
      if (o instanceof INews) {
        INews news = (INews) o;
        newsMap.put(news.getId(), news);
      }
      else if (o instanceof IFeed)
        shouldFireFeedEvent = true;
      else if (o instanceof Description) {
        descriptionsRemovedOrUpdated.add(((Description) o).getNews().getId());
        fDescriptionDAO.save((Description) o);
      }
      else if (o instanceof IEntity) {
        IEntity entity = (IEntity) o;
        EventType eventType;
        if (entity.getId() == null)
          eventType = EventType.PERSIST;
        else
          eventType = EventType.UPDATE;
        eventsMap.putEvent(fEventManager.createModelEvent(emptyMap, entity), eventType);
      }
    }

    /*
     * If the description was removed or updated, we must fire an event for
     * the parent news.
     */
    for (Long newsId : descriptionsRemovedOrUpdated) {
      if (!newsMap.containsKey(newsId))
        newsMap.put(newsId, getNews(feed, newsId));
    }

    if (!newsMap.isEmpty()) {
      IFeed oldFeed = fFeedDAO.loadFromDisk(feed.getId());
      for (INews newsItem : newsMap.values()) {
        INews oldNews = getNews(oldFeed, newsItem.getId());
        EventType eventType;
        if (oldNews == null)
          eventType = EventType.PERSIST;
        else
          eventType = EventType.UPDATE;
        eventsMap.putEvent(new NewsEvent(oldNews, newsItem, false), eventType);
      }
    }

    if (shouldFireFeedEvent) {
      eventsMap.putPersistEvent(new FeedEvent(feed, true));
      fFeedDAO.save(feed, false);
    }

    PersistHelper.updateNewsCounter(fNewsCounterDAO, eventsMap);

    return eventsMap;
  }

  private INews getNews(IFeed feed, long newsId) {
    for (INews news : feed.getNews()) {
      if (news.getId().longValue() == newsId)
        return news;
    }
    return null;
  }
}
