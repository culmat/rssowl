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

import org.rssowl.core.model.events.SearchConditionEvent;
import org.rssowl.core.model.events.SearchConditionListener;
import org.rssowl.core.model.internal.persist.search.SearchCondition;
import org.rssowl.core.model.persist.dao.ISearchConditionDAO;
import org.rssowl.core.model.persist.search.ISearchCondition;

public final class SearchConditionDAOImpl extends AbstractEntityDAO<ISearchCondition,
    SearchConditionListener, SearchConditionEvent> implements ISearchConditionDAO  {

  public SearchConditionDAOImpl() {
    super(SearchCondition.class, true);
  }
  
  @Override
  protected final SearchConditionEvent createDeleteEventTemplate(ISearchCondition entity) {
    return createSaveEventTemplate(entity);
  }

  @Override
  protected final SearchConditionEvent createSaveEventTemplate(ISearchCondition entity) {
    return new SearchConditionEvent(entity, true);
  }
}
