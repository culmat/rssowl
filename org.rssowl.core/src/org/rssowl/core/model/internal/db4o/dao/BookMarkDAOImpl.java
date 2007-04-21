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
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.BookMarkListener;
import org.rssowl.core.model.internal.persist.BookMark;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.dao.IBookMarkDAO;
import org.rssowl.core.model.reference.FeedLinkReference;

import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.Collection;

public final class BookMarkDAOImpl extends AbstractEntityDAO<IBookMark, BookMarkListener,
    BookMarkEvent> implements IBookMarkDAO  {

  public BookMarkDAOImpl() {
    super(BookMark.class, false);
  }

  @Override
  protected final BookMarkEvent createDeleteEventTemplate(IBookMark entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final BookMarkEvent createSaveEventTemplate(IBookMark entity) {
    return new BookMarkEvent(entity, null, true);
  }

  public final Collection<IBookMark> loadAll(FeedLinkReference feedRef) {
    try {
      Query query = fDb.query();
      query.constrain(fEntityClass);
      query.descend("fFeedLink").constrain(feedRef.getLink().toString()); //$NON-NLS-1$
      ObjectSet<IBookMark> marks = getObjectSet(query);
      activateAll(marks);
      return new ArrayList<IBookMark>(marks);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }
}
