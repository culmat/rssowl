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

import org.rssowl.core.model.persist.NewsCounter;
import org.rssowl.core.model.persist.dao.INewsCounterDAO;

import java.util.Collection;

public final class NewsCounterDAOImpl extends AbstractPersistableDAO<NewsCounter>
    implements INewsCounterDAO  {

  public NewsCounterDAOImpl() {
    super(NewsCounter.class);
  }
  
  public final NewsCounter load() {
    Collection<NewsCounter> newsCounters = loadAll();
    if (newsCounters.isEmpty())
      return null;
    
    if (newsCounters.size() > 1)
      throw new IllegalStateException("Only one NewsCounter should exist, but " +
          "there are: " + newsCounters.size());
    
    return newsCounters.iterator().next();
  }
  
  @Override
  public final NewsCounter load(long id) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public final <C extends Collection<NewsCounter>> C saveAll(C entities)  {
    if (entities.size() > 1) {
      throw new IllegalArgumentException("Only a single newsCounter can be stored");
    }
    return super.saveAll(entities);
  }

  @Override
  protected final void doSave(NewsCounter entity) {
    if (!fDb.ext().isStored(entity) && (load() != null))
      throw new IllegalArgumentException("Only a single newsCounter can be stored");
    
    super.doSave(entity);
  }

  @Override
  protected final boolean isSaveFully() {
    return true;
  }

}
