/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFeed;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FeedSummaries {

  private final Set<Long> fIds;
  private final Map<String, Long> fLinkToIdMap;

  public FeedSummaries(int capacity) {
    fLinkToIdMap = new HashMap<String, Long>(capacity);
    fIds = new HashSet<Long>(capacity);
  }

  /**
   * @return {@code true} if object changed.
   */
  public synchronized boolean addIfAbsent(IFeed feed) {
    assertFeed(feed);
    boolean added = fIds.add(feed.getId());
    if (added)
      fLinkToIdMap.put(feed.getLink().toString(), feed.getId());
    return added;
  }

  private void assertFeed(IFeed feed) {
    Assert.isNotNull(feed, "feed");
    Assert.isLegal(feed.getId() != null, "feed id should be non-null");
  }

  public synchronized void remove(IFeed feed) {
    assertFeed(feed);
    fLinkToIdMap.remove(feed.getLink().toString());
    fIds.remove(feed.getId());
  }

  public synchronized Set<Long> getIds() {
    return fIds;
  }

  public synchronized Long getId(URI feedLink) {
    Assert.isNotNull(feedLink, "feedLink");
    return fLinkToIdMap.get(feedLink.toString());
  }

  public synchronized boolean contains(long feedId) {
    return fIds.contains(feedId);
  }

  public synchronized long countAll() {
    return fIds.size();
  }
}
