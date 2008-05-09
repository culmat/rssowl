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
import org.rssowl.core.persist.INews;

import java.util.ArrayList;
import java.util.List;

public final class EntityWithChildrenNewsAdapter implements EntityWithChildren<INews>    {

  private final INews fNews;

  public EntityWithChildrenNewsAdapter(INews news) {
    Assert.isNotNull(news, "news");
    this.fNews = news;
  }

  public List<IEntity> getChildEntities() {
    List<IEntity> entities = new ArrayList<IEntity>(fNews.getAttachments());
    entities.addAll(fNews.getCategories());
    if (fNews.getAuthor() != null)
      entities.add(fNews.getAuthor());
    return entities;
  }

  public INews getEntity() {
    return fNews;
  }
}
