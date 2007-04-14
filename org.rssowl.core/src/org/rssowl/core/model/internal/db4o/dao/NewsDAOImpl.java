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

import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.model.internal.persist.News;
import org.rssowl.core.model.persist.INews;

public final class NewsDAOImpl extends AbstractEntityDAO<News, NewsListener, NewsEvent> {

  public NewsDAOImpl() {
    super(News.class);
  }
  
  @Override
  protected final void doSave(News entity) {
    DBHelper.saveAndCascadeNews(fDb, entity, true);
  }

  @Override
  protected final NewsEvent createDeleteEventTemplate(News entity) {
    return new NewsEvent(null, entity, true);
  }

  @Override
  protected final NewsEvent createSaveEventTemplate(News entity) {
    INews oldNews = fDb.ext().peekPersisted(entity, 2, true);
    return new NewsEvent(oldNews, entity, true);
  }

  @Override
  protected final boolean isSaveFully() {
    return false;
  }

}
