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
import org.rssowl.core.model.events.PersonEvent;
import org.rssowl.core.model.events.PersonListener;
import org.rssowl.core.model.internal.persist.Person;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.dao.IPersonDAO;
import org.rssowl.core.util.StringUtils;

import com.db4o.ext.Db4oException;

import java.util.Set;
import java.util.TreeSet;

public class PersonDAOImpl extends AbstractEntityDAO<IPerson, PersonListener,
    PersonEvent> implements IPersonDAO  {

  public PersonDAOImpl() {
    super(Person.class);
  }
  
  @Override
  protected PersonEvent createDeleteEventTemplate(IPerson entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final PersonEvent createSaveEventTemplate(IPerson entity) {
    return new PersonEvent(entity, true);
  }

  @Override
  protected final boolean isSaveFully() {
    return false;
  }

  public Set<String> loadAllNames() {
    try {
      Set<String> strings = new TreeSet<String>();
      for (IPerson person : loadAll()) {
        String name = StringUtils.safeTrim(person.getName());
        if (StringUtils.isSet(name))
          strings.add(name);
        else if (person.getEmail() != null)
          strings.add(person.getEmail().toString());
      }

      return strings;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }
}
