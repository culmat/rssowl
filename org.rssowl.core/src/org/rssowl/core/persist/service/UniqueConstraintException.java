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
package org.rssowl.core.persist.service;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IPersistable;

/**
 * This exception should be thrown in situations where saving a
 * IPersistable would break a unique field invariant.
 */
public final class UniqueConstraintException extends PersistenceException {

  private final String fPropertyName;
  private final IPersistable fPersistable;

  /**
   * 
   * @param propertyName
   * @param persistable 
   */
  public UniqueConstraintException(String propertyName,
      IPersistable persistable) {
    Assert.isNotNull(persistable, "persistable");
    Assert.isNotNull(propertyName, "propertyName");
    fPersistable = persistable;
    fPropertyName = propertyName;
  }
  
  public final String getPropertyName() {
    return fPropertyName;
  }
  
  public final IPersistable getPersistable() {
    return fPersistable;
  }
  
  //TODO Override getMessage
}
