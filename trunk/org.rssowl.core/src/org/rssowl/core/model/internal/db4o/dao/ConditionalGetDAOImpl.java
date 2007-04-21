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

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.internal.persist.ConditionalGet;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.dao.IConditionalGetDAO;

import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.net.URI;

public final class ConditionalGetDAOImpl extends AbstractPersistableDAO<IConditionalGet>
    implements IConditionalGetDAO   {

  public ConditionalGetDAOImpl() {
    super(ConditionalGet.class, true);
  }

  public IConditionalGet load(URI link) {
    Assert.isNotNull(link, "link cannot be null"); //$NON-NLS-1$
    try {
      Query query = fDb.query();
      query.constrain(fEntityClass);
      query.descend("fLink").constrain(link.toString()); //$NON-NLS-1$

      for (IConditionalGet entity : getObjectSet(query)) {
        fDb.activate(entity, Integer.MAX_VALUE);
        return entity;
      }
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
    return null;
  }
}
