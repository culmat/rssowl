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
package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NewsContainer {

  /**
   * A long[] for each state, so fNews[news.getState().ordinal()] returns
   * the long[] of ids for the given state.
   */
  private long[][] fNews;

  /**
   * fNews[getState()] returns the current array size for the given state
   * in fNews.
   */
  private int[] fNewsSize;

  private boolean fSorted;

  public NewsContainer(boolean sorted) {
    fSorted = sorted;
    int length = INews.State.values().length;
    fNews = new long[length][0];
    fNewsSize = new int[length];
  }

  public void addNews(INews news) {
    int index = news.getState().ordinal();
    long[] fNewsState = fNews[index];
    fNews[index] = addNews(fNewsState, news, fNewsSize[index]);
    ++fNewsSize[index];
  }

  private long[] addNews(long[] newsArray, INews news, int currentArraySize) {
    long[] array = ArrayUtils.ensureCapacity(newsArray, currentArraySize + 1);
    long newsId = news.getId();
    if (fSorted) {
      int index = ArrayUtils.binarySearch(array, newsId, currentArraySize);
      Assert.isLegal(index < 0, "news already part of container: " + news);
      int insertionPoint = (-index) - 1;
      System.arraycopy(array, insertionPoint, array, insertionPoint + 1,
          currentArraySize - insertionPoint);
      array[index] = newsId;
    }
    else
      array[currentArraySize] = newsId;
    return array;
  }

  public void removeNews(INews news) {

  }

  public int getNewsCount(Set<INews.State> states) {
    Assert.isNotNull(states, "states");

    int count = 0;

    for (INews.State state : states) {
      count += fNewsSize[state.ordinal()];
    }

    return count;
  }

  public boolean containsNews(INews news) {
    for (int i = 0, c = fNews.length; i < c; ++i) {
      if (containsNews(fNews[i], news, fNewsSize[i]))
        return true;
    }
    return false;
  }

  private boolean containsNews(long[] newsArray, INews news, int endIndex) {
    if (fSorted)
      return ArrayUtils.binarySearch(newsArray, news.getId(), endIndex) >= 0;

    for (int i = 0, c = newsArray.length; i < c; ++i) {
      if (newsArray[i] == news.getId().longValue())
        return true;
    }
    return false;
  }

  public List<NewsReference> getNews(Set<INews.State> states)   {
    List<NewsReference> newsRefs = new ArrayList<NewsReference>(getNewsCount(states));

    for (INews.State state : states) {
      int index = state.ordinal();
      long[] newsIds = fNews[index];
      for (int i = 0, c = newsIds.length; i < c; ++i) {
        newsRefs.add(new NewsReference(newsIds[i]));
      }
    }

    return newsRefs;
  }

  public List<NewsReference> getNews() {
    return getNews(INews.State.getVisible());
  }

  void compact() {
    for (int i = 0, c = fNews.length; i < c; ++i) {
      long[] fStateNews = fNews[i];
      int size = fNewsSize[i];
      long[] compacted = new long[size];
      System.arraycopy(fStateNews, 0, compacted, 0, size);
    }
  }
}
