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
package org.rssowl.core.model;

import org.rssowl.core.model.internal.types.Persistable;
import org.rssowl.core.model.persist.IPersistable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class NewsCounter extends Persistable implements IPersistable    {

  private Map<String, NewsCounterItem> countersMap;
  
  public NewsCounter() {
  }
  
  public synchronized void put(URI feedLink, NewsCounterItem item) {
    if (countersMap == null)
      countersMap = new HashMap<String, NewsCounterItem>();
    
    countersMap.put(feedLink.toString(), item);
  }
  
  public synchronized NewsCounterItem get(URI feedLink) {
    if (countersMap == null)
      return null;
    
    return countersMap.get(feedLink.toString());
  }

  public synchronized NewsCounterItem remove(URI feedLink) {
    if (countersMap == null)
      return null;
    
    return countersMap.remove(feedLink.toString());
  }
}
