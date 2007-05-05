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
package org.rssowl.core.internal.persist.service;

/**
 * Implementors of this interface are able to migrate the database data from
 * one format version to another. 
 */
public interface Migration {
  /**
   * @return the format version that the implementor can migrate from.
   */
  int getOriginFormat();
  
  /**
   * @return the format version that the implementor can migrate to.
   */
  int getDestinationFormat();
  
  /**
   * Perform the migration. Implementors are responsible for making sure
   * that all object containers are closed at the end of the script.
   * 
   * @param configFactory
   * @param dbFileName
   */
  void migrate(ConfigurationFactory configFactory, String dbFileName);
}
