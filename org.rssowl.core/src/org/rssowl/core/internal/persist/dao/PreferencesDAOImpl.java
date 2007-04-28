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

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.Preference;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.pref.PreferenceEvent;
import org.rssowl.core.persist.pref.PreferenceListener;
import org.rssowl.core.persist.service.PersistenceException;

import com.db4o.ObjectSet;
import com.db4o.query.Query;

/**
 * Default implementation of {@link IPreferenceDAO}.
 *
 * {@inheritDoc}
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class PreferencesDAOImpl extends AbstractEntityDAO<IPreference, PreferenceListener, PreferenceEvent> implements IPreferenceDAO  {

  /**
   * Creates an instance of this class.
   */
  public PreferencesDAOImpl() {
    super(Preference.class, true);
  }

  @Override
  protected void doSave(IPreference entity) {
    //TODO Add UniqueConstraintViolation exception and use it from
    //saveFeed and saveNewsCounter
    IPreference pref = load(entity.getKey());
    if (pref != null && pref != entity)
        throw new IllegalArgumentException("preference with the provided key already exists");
    super.doSave(entity);
  }

  public boolean delete(String key) throws PersistenceException {
    IPreference pref = load(key);
    if (pref == null)
      return false;
    
    delete(pref);
    return true;
  }

  @Override
  protected PreferenceEvent createDeleteEventTemplate(IPreference entity) {
    return null;
  }

  @Override
  protected PreferenceEvent createSaveEventTemplate(IPreference entity) {
    return null;
  }

  public IPreference load(String key) throws PersistenceException {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fKey").constrain(key);
    ObjectSet<IPreference> prefs = getObjectSet(query);
    activateAll(prefs);
    if (prefs.hasNext()) {
      return prefs.next();
    }
    return null;
  }

  public IPreference loadOrCreate(String key) throws PersistenceException {
    IPreference pref = load(key);
    if (pref == null)
      return Owl.getModelFactory().createPreference(key);
    
    return pref;
  }
}