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
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//TODO What happens to hidden and deleted news?
public class NewsBin extends Mark implements INewsBin   {

  private long[] fReadNews = new long[0];
  private int fReadNewsSize = 0;

  private long[] fUnreadUpdatedNews = new long[0];
  private int fUnreadUpdatedNewsSize = 0;

  private long[] fNewNews = new long[0];
  private int fNewNewsSize = 0;

  /**
   * Creates a new Element of the type SearchMark. A SearchMark is only visually
   * represented in case it was added to a Folder. Make sure to add it to a
   * Folder using <code>Folder#addMark(Mark)</code>
   *
   * @param id The unique ID of this SearchMark.
   * @param folder The Folder this SearchMark belongs to.
   * @param name The Name of this SearchMark.
   */
  public NewsBin(Long id, IFolder folder, String name) {
    super(id, folder, name);
  }

  /**
   * Default constructor for deserialization
   */
  protected NewsBin() {
    super();
  }

  public void addNews(INews news) {
    switch (news.getState()) {
      case NEW:
        fNewNews = addNews(fNewNews, news, fNewNewsSize);
        ++fNewNewsSize;
        break;
      case READ:
        fReadNews = addNews(fReadNews, news, fReadNewsSize);
        ++fReadNewsSize;
        break;
      case UNREAD:
      case UPDATED:
        fUnreadUpdatedNews = addNews(fUnreadUpdatedNews, news, fUnreadUpdatedNewsSize);
        ++fUnreadUpdatedNewsSize;
        break;
      default:
        throw new IllegalArgumentException("news cannot be added with state: " + news.getState());
    }
  }

  private long[] addNews(long[] newsArray, INews news, int currentArraySize) {
    long[] array = ArrayUtils.ensureCapacity(newsArray, currentArraySize + 1);
    array[currentArraySize] = news.getId();
    return array;
  }

  public void removeNews(INews news) {

  }

  public int getNewsCount(Set<INews.State> states) {
    Assert.isNotNull(states, "states");
    int count = 0;

    /* Read News */
    if (states.contains(INews.State.READ))
      count += fReadNewsSize;

    //FIXME This is copied from SearchMark and it seems buggy to me. Ask ben
    //what the motivation was
    /* Unread or Updated News */
    if (states.contains(INews.State.UNREAD) || states.contains(INews.State.UPDATED))
      count += fUnreadUpdatedNewsSize;

    /* New News */
    if (states.contains(INews.State.NEW))
      count += fNewNewsSize;

    return count;
  }

  public boolean containsNews(INews news) {
    if (containsNews(fNewNews, news, fNewNewsSize)) {
      return true;
    } if (containsNews(fUnreadUpdatedNews, news, fUnreadUpdatedNewsSize)) {
      return true;
    }
    return containsNews(fReadNews, news, fReadNewsSize);
  }

  private static boolean containsNews(long[] newsArray, INews news, int endIndex) {
    return ArrayUtils.binarySearch(newsArray, news.getId(), endIndex) >= 0;
  }

  public List<NewsReference> getNews() {
    Set<State> states = INews.State.getVisible();
    List<NewsReference> newsRefs = new ArrayList<NewsReference>(getNewsCount(states));

    /* Add Read News */
    if (states.contains(INews.State.READ)) {
      for (int i = 0; i < fReadNewsSize; ++i)
        newsRefs.add(new NewsReference(fReadNews[i]));
    }

    /* Add Unread News */
    if (states.contains(INews.State.UNREAD) || states.contains(INews.State.UPDATED)) {
      for (int i = 0; i < fUnreadUpdatedNewsSize; ++i)
        newsRefs.add(new NewsReference(fUnreadUpdatedNews[i]));
    }

    /* Add New News */
    if (states.contains(INews.State.NEW)) {
      for (int i = 0; i < fNewNewsSize; ++i)
        newsRefs.add(new NewsReference(fNewNews[i]));
    }

    return newsRefs;
  }
}