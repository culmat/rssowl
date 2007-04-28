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

package org.rssowl.core.persist;


public interface IPreference extends IEntity {

  public enum Type { BOOLEAN, INTEGER, LONG, STRING };
  
  String getKey();
  
  Type getType();

  Boolean getBoolean();

  boolean[] getBooleans();

  Integer getInteger();

  int[] getIntegers();

  Long getLong();

  long[] getLongs();

  String getString();

  String[] getStrings();
  
  void putStrings(String ... strings);

  void putLongs(long ... longs);

  void putIntegers(int ... integers);

  void putBooleans(boolean ... booleans);

  void clear();
}
