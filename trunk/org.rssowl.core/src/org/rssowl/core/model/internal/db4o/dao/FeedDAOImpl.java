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

import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.FeedListener;
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.model.internal.persist.Feed;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.dao.IFeedDAO;
import org.rssowl.core.model.reference.FeedReference;

import java.net.URI;

public final class FeedDAOImpl extends AbstractEntityDAO<Feed, FeedListener,
    FeedEvent> implements IFeedDAO<Feed>  {

  public FeedDAOImpl() {
    super(Feed.class);
  }
  
  @Override
  protected final void doSave(Feed entity) {
    DBHelper.saveFeed(fDb, entity);
  }

  @Override
  protected final FeedEvent createDeleteEventTemplate(Feed entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final FeedEvent createSaveEventTemplate(Feed entity) {
    return new FeedEvent(entity, true);
  }

  @Override
  protected final boolean isSaveFully() {
    return false;
  }

  public final Feed load(URI link) {
    return DBHelper.loadFeed(fDb, link, Integer.MAX_VALUE);
  }

  // FIXME Not sure if this makes sense anymore. If we decide to keep it, try
  // to make it more efficient
  public final FeedReference loadReference(URI link) {
    IFeed feed = DBHelper.loadFeed(fDb, link, null);
    if (feed == null) {
      return null;
    }
    return new FeedReference(feed.getId());
  }
}
