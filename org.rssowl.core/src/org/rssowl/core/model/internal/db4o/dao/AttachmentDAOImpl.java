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

import org.rssowl.core.model.events.AttachmentEvent;
import org.rssowl.core.model.events.AttachmentListener;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.internal.db4o.DBHelper;
import org.rssowl.core.model.internal.persist.Attachment;
import org.rssowl.core.model.persist.INews;

public final class AttachmentDAOImpl extends AbstractEntityDAO<Attachment,
    AttachmentListener, AttachmentEvent> {

  public AttachmentDAOImpl() {
    super(Attachment.class);
  }

  @Override
  protected final AttachmentEvent createDeleteEventTemplate(Attachment entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final AttachmentEvent createSaveEventTemplate(Attachment entity) {
    return new AttachmentEvent(entity, true);
  }
  
  @Override
  public final void doDelete(Attachment entity) {
    //TODO Not sure about this, but let's do it for now to help us track a bug
    //in NewsService where never having a newsUpdated with a null oldNews is
    //helpful
    INews news = entity.getNews();
    INews oldNews = fDb.ext().peekPersisted(news, 2, true);
    NewsEvent newsEvent = new NewsEvent(oldNews, news, false);
    DBHelper.putEventTemplate(newsEvent);
    super.doDelete(entity);
  }

  @Override
  protected final boolean isSaveFully() {
    return false;
  }

}
