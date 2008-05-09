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
package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;

import java.util.ArrayList;
import java.util.List;

public final class EntityWithChildrenFeedAdapter implements EntityWithChildren<IFeed> {

  private final IFeed fFeed;

  public EntityWithChildrenFeedAdapter(IFeed feed) {
    Assert.isNotNull(feed, "feed");
    this.fFeed = feed;
  }

  public List<IEntity> getChildEntities() {
    List<IEntity> entities = new ArrayList<IEntity>(fFeed.getCategories());
    entities.add(fFeed.getAuthor());
    for (INews news : fFeed.getNews()) {
      entities.add(news);
      entities.addAll(new EntityWithChildrenNewsAdapter(news).getChildEntities());
    }
    return entities;
  }

  public IFeed getEntity() {
    return fFeed;
  }

}
