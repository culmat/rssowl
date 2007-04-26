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

package org.rssowl.core.internal.persist.dao;

import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.dao.INewsCounterDAO;

import java.util.Collection;

/**
 * A data-access-object for <code>NewsCounter</code>.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public final class NewsCounterDAOImpl extends AbstractPersistableDAO<NewsCounter> implements INewsCounterDAO {

  /** Default constructor using the specific IPersistable for this DAO */
  public NewsCounterDAOImpl() {
    super(NewsCounter.class, true);
  }

  public final void delete() {
    super.delete(load());
  }

  @Override
  public final void delete(NewsCounter newsCounter) {
    if (!newsCounter.equals(load()))
      throw new IllegalArgumentException("Only a single newsCounter should be used. " + "Trying to delete a non-existent one.");

    super.delete(newsCounter);
  }

  public final NewsCounter load() {
    Collection<NewsCounter> newsCounters = loadAll();
    if (newsCounters.isEmpty())
      return null;

    if (newsCounters.size() > 1)
      throw new IllegalStateException("Only one NewsCounter should exist, but " + "there are: " + newsCounters.size());

    return newsCounters.iterator().next();
  }

  @Override
  public final NewsCounter load(long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void saveAll(Collection<NewsCounter> entities) {
    if (entities.size() > 1) {
      throw new IllegalArgumentException("Only a single newsCounter can be stored");
    }
    super.saveAll(entities);
  }

  @Override
  protected final void doSave(NewsCounter entity) {
    if (!fDb.ext().isStored(entity) && (load() != null))
      throw new IllegalArgumentException("Only a single newsCounter can be stored");

    super.doSave(entity);
  }
}