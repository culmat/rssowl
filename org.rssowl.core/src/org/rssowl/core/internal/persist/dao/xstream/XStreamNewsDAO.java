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

import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.internal.persist.service.NewsSummaries;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class XStreamNewsDAO extends XStreamEntityDAO<INews, NewsListener, NewsEvent> implements INewsDAO {

  private final IFeedDAO fFeedDAO;
  private final INewsBinDAO fNewsBinDAO;
  private final NewsSummaries fNewsSummaries;

  public XStreamNewsDAO(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager, XStreamFeedDAO feedDAO, INewsBinDAO newsBinDAO) {
    super(News.class, baseDir, xStream, idGenerator, eventManager);
    fFeedDAO = feedDAO;
    fNewsBinDAO = newsBinDAO;
    File file = new File(getDirectory(), "newsSummaries.xml");
    if (file.exists())
      fNewsSummaries = fromXML(NewsSummaries.class, file);
    else
      fNewsSummaries = new NewsSummaries(100);
  }

  public Collection<INews> loadAll(FeedLinkReference feedRef, Set<State> states) {
    return feedRef.resolve().getNewsByStates(states);
  }

  public void setState(Collection<INews> news, State state, boolean affectEquivalentNews, boolean force) throws PersistenceException {
    // TODO Auto-generated method stub
  }

  public void setState(Set<State> originalStates, State state, boolean affectEquivalentNews) throws PersistenceException {
    // TODO Auto-generated method stub

  }

  public boolean exists(long id) throws PersistenceException {
    return fNewsSummaries.contains(id);
  }

  public INews load(long id) throws PersistenceException {
    return null;
  }

  public long countAll() throws PersistenceException {
    return fNewsSummaries.countAll();
  }

  public void delete(INews persistable) throws PersistenceException {
    // TODO Auto-generated method stub

  }

  public void deleteAll(Collection<INews> persistables) throws PersistenceException {
    // TODO Auto-generated method stub

  }

  public Collection<INews> loadAll() throws PersistenceException {
    Collection<IFeed> feeds = fFeedDAO.loadAll();
    List<INews> newsList = new ArrayList<INews>(feeds.size() * 10);
    for (IFeed feed : feeds)
      newsList.addAll(feed.getNews());
    for (INewsBin bin : fNewsBinDAO.loadAll())
      newsList.addAll(bin.getNews());
    return newsList;
  }

  public INews save(INews news) throws PersistenceException {
    //FIXME Events
    //FIXME Consider the case where resolving the feed returns a feed in memory
    //and the news in the feed is the same as news
    if (news.getParentId() == 0) {
      Feed feed = (Feed) news.getFeedReference().resolve();
      if (news.getId() == null) {
        news.setId(fIdGenerator.getNext());
        feed.addNews(news);
      }
      else {
        feed.mergeNews(news);
      }
      fFeedDAO.save(feed);
      return news;
    }
    //FIXME Modify parent news bin if state changed
    XStreamHelper.toXML(fXStream, news, getNewsCopyFile(news.getId()), false);
    return news;
  }

  private File getNewsCopyFile(long id) {
    return new File(getDirectory(), String.valueOf(id) + ".xml");
  }

  private File getDirectory() {
    return new File(fBaseDir, "news");
  }

  public void saveAll(Collection<INews> persistables) throws PersistenceException {
    //FIXME Optimize this by grouping per feed where appropriate
    for (INews news : persistables)
      save(news);
  }
}
