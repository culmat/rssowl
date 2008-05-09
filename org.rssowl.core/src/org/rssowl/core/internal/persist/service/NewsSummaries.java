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
import org.rssowl.core.persist.INews;

import java.util.HashMap;
import java.util.Map;

public class NewsSummaries {

  private final Map<Long, String> fNewsIdToFeedLinkMap;

  public NewsSummaries(int capacity) {
    fNewsIdToFeedLinkMap = new HashMap<Long, String>(capacity);
  }

  public synchronized boolean addIfAbsent(INews news) {
    assertNews(news);
    return fNewsIdToFeedLinkMap.put(news.getId(), news.getFeedLinkAsText()) == null;
  }

  private void assertNews(INews news) {
    Assert.isNotNull(news, "news");
    Assert.isLegal(news.getId() != null, "news id should be non-null");
  }

  public synchronized void remove(INews news) {
    assertNews(news);
    fNewsIdToFeedLinkMap.remove(news.getId());
  }

  public synchronized  boolean contains(long newsId) {
    return fNewsIdToFeedLinkMap.containsKey(newsId);
  }

  public synchronized long countAll() {
    return fNewsIdToFeedLinkMap.size();
  }
}
