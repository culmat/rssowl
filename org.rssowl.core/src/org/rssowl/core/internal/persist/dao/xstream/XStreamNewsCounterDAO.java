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

import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class XStreamNewsCounterDAO extends XStreamPersistableDAO<NewsCounter> implements INewsCounterDAO {

  private final File fFile;
  private final NewsCounter fNewsCounter;

  //FIXME Are there cases where we need to count?
  public XStreamNewsCounterDAO(File baseDir, XStream xStream) {
    super(NewsCounter.class, baseDir, xStream);
    fFile = new File(baseDir, "newsCounters.xml");
    if (fFile.exists())
      fNewsCounter = XStreamHelper.fromXML(xStream, NewsCounter.class, fFile);
    else
      fNewsCounter = new NewsCounter();
  }

  public void delete() {
    throw new UnsupportedOperationException();
  }

  public NewsCounter load() {
    return fNewsCounter;
  }

  public void save() {
    XStreamHelper.toXML(fXStream, fNewsCounter, fFile, false);
  }

  public long countAll() throws PersistenceException {
    return 1;
  }

  public void delete(NewsCounter persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void deleteAll(Collection<NewsCounter> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public Collection<NewsCounter> loadAll() throws PersistenceException {
    return new ArrayList<NewsCounter>(Collections.singleton(fNewsCounter));
  }

  public NewsCounter save(NewsCounter persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void saveAll(Collection<NewsCounter> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }
}
