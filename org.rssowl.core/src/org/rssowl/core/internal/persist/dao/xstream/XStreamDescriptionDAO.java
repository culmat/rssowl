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

import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.dao.IDescriptionDAO;
import org.rssowl.core.internal.persist.service.IOHelper;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class XStreamDescriptionDAO extends XStreamPersistableDAO<Description> implements IDescriptionDAO {

  private final File directory;

  public XStreamDescriptionDAO(File baseDir, XStream xStream) {
    super(Description.class, baseDir, xStream);
    directory = new File(baseDir, "descriptions");
    if (!directory.exists())
      directory.mkdirs();
  }

  public Description load(long newsId) {
    return fromXML(getFile(newsId));
  }

  private File getFile(long newsId) {
    return new File(directory, String.valueOf(newsId) + ".xml");
  }

  public String loadValue(long newsId) {
    Description description = load(newsId);
    return description == null ? null : description.getValue();
  }

  public long countAll() throws PersistenceException {
    return directory.listFiles().length;
  }

  public void delete(Description persistable) throws PersistenceException {
    IOHelper.delete(getFile(persistable.getNews().getId()));
  }

  public void deleteAll(Collection<Description> persistables) throws PersistenceException {
    for (Description description : persistables)
      delete(description);
  }

  public Collection<Description> loadAll() throws PersistenceException {
    Collection<Description> descriptions = new ArrayList<Description>();
    for (File file : directory.listFiles())
      descriptions.add(fromXML(file));
    return descriptions;
  }

  public Description save(Description persistable) throws PersistenceException {
    XStreamHelper.toXML(fXStream, persistable, getFile(persistable.getNews().getId()), false);
    return persistable;
  }

  public void saveAll(Collection<Description> persistables) throws PersistenceException {
    for (Description description : persistables)
      save(description);
  }

}
