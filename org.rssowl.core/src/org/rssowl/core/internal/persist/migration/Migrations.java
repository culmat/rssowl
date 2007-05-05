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
package org.rssowl.core.internal.persist.migration;

import org.rssowl.core.internal.persist.service.Migration;

import java.util.ArrayList;
import java.util.List;

public final class Migrations {

  private final List<Migration> fMigrations;
  
  public Migrations() {
    fMigrations = new ArrayList<Migration>(0);
    // No migrations included by default
  }

  public final Migration getMigration(int originFormat, int destinationFormat) {
    for (Migration migration : fMigrations) {
      if (migration.getOriginFormat() == originFormat && migration.getDestinationFormat() == destinationFormat) {
        return migration;
      }
    }
    return null;
  }
}
