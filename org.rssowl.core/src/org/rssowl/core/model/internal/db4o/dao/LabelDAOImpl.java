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

import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.events.LabelEvent;
import org.rssowl.core.persist.events.LabelListener;

public final class LabelDAOImpl extends AbstractEntityDAO<ILabel, LabelListener,
    LabelEvent> implements ILabelDAO {

  public LabelDAOImpl() {
    super(Label.class, false);
  }

  @Override
  protected final LabelEvent createDeleteEventTemplate(ILabel entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final LabelEvent createSaveEventTemplate(ILabel entity) {
    return new LabelEvent(entity, true);
  }
}
