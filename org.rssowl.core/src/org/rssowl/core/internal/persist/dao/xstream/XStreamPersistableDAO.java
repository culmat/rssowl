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

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.dao.IPersistableDAO;

import com.thoughtworks.xstream.XStream;

import java.io.File;

public abstract class XStreamPersistableDAO<T extends IPersistable> implements IPersistableDAO<T> {
  protected final Class<? extends T> fEntityClass;
  protected final File fBaseDir;
  protected final XStream fXStream;

  public XStreamPersistableDAO(Class<? extends T> entityClass, File baseDir, XStream xStream) {
    Assert.isNotNull(entityClass, "entityClass");
    Assert.isNotNull(baseDir, "baseDir");
    Assert.isNotNull(xStream, "xStream");
    Assert.isLegal(baseDir.exists(), "baseDir should exist");
    fEntityClass = entityClass;
    fXStream = xStream;
    fBaseDir = baseDir;
  }

  public final Class<? extends T> getEntityClass() {
    return fEntityClass;
  }

  protected T fromXML(File file) {
    return fromXML(getEntityClass(), file);
  }

  protected <E> E fromXML(Class<E> expectedClass, File file) {
    return XStreamHelper.fromXML(fXStream, expectedClass, file);
  }
}
