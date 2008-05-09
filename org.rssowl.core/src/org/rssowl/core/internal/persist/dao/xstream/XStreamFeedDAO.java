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
import org.rssowl.core.internal.persist.service.FeedSummaries;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FeedListener;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class XStreamFeedDAO extends XStreamEntityDAO<IFeed, FeedListener, FeedEvent> implements IFeedDAO {

  private final FeedSummaries fFeedSummaries;
  public XStreamFeedDAO(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    super(Feed.class, baseDir, xStream, idGenerator, eventManager);
    if (XStreamHelper.fileExists(getSummariesFile()))
      fFeedSummaries = XStreamHelper.fromXML(xStream, FeedSummaries.class, getSummariesFile());
    else
      fFeedSummaries = new FeedSummaries(32);

    if (!getDirectory().exists())
      getDirectory().mkdirs();
  }

  public IFeed load(URI link) {
    Long id = fFeedSummaries.getId(link);
    if (id == null)
      return null;
    return load(id);
  }

  public FeedReference loadReference(URI link) {
    Long id = fFeedSummaries.getId(link);
    if (id == null)
      return null;
    return new FeedReference(id);
  }

  public File getDirectory() {
    return new File(fBaseDir, "feeds");
  }

  public File getSummariesFile() {
    return new File(fBaseDir, "feedSummaries.xml");
  }

  public File getFeedFile(long id) {
    return new File(getDirectory(), String.valueOf(id) + ".xml");
  }

  public boolean exists(long id) throws PersistenceException {
    return getFeedFile(id).exists();
  }

  /**
   * Guarantees that no caching is performed.
   * @param id
   * @return
   * @throws PersistenceException
   */
  public IFeed loadFromDisk(long id) throws PersistenceException {
    return XStreamHelper.fromXML(fXStream, IFeed.class, getFeedFile(id));
  }

  public IFeed load(long id) throws PersistenceException {
    return loadFromDisk(id);
  }

  public long countAll() throws PersistenceException {
    return fFeedSummaries.countAll();
  }

  public void delete(IFeed persistable) throws PersistenceException {
    //TODO Delete file in the appropriate folder
  }

  public void deleteAll(Collection<IFeed> persistables) throws PersistenceException {
    //TODO Delete all files in the appropriate folder
  }

  public Collection<IFeed> loadAll() throws PersistenceException {
    Set<Long> ids = fFeedSummaries.getIds();
    List<IFeed> feeds = new ArrayList<IFeed>(ids.size());
    for (Long id : ids)
      feeds.add(load(id));
    return feeds;
  }

  public IFeed save(IFeed feed, boolean fireEvents) throws PersistenceException {
    if (fireEvents) {
      //FIXME Implement
//      Map<IEntity, ModelEvent>
//      Collections.singletonMap(feed, new FeedEvent(feed, true));
//      eventManager.createEventRunnables(in)
    }
    if (feed.getId() == null)
      feed.setId(fIdGenerator.getNext());
    XStreamHelper.toXML(fXStream, feed, getFeedFile(feed.getId()), false);
    if (!fFeedSummaries.addIfAbsent(feed))
      XStreamHelper.toXML(fXStream, fFeedSummaries, getSummariesFile(), false);
    return feed;
  }

  public IFeed save(IFeed feed) throws PersistenceException {
    return save(feed, true);
  }

  public void saveAll(Collection<IFeed> feeds) throws PersistenceException {
    for (IFeed feed : feeds)
      save(feed);
  }

}
