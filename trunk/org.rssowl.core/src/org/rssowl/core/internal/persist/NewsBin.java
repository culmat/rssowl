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

import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;

import java.util.List;
import java.util.Set;

public class NewsBin extends Mark implements INewsBin   {

  private NewsContainer newsContainer;

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
    newsContainer.addNews(news);
  }

  public boolean containsNews(INews news) {
    return newsContainer.containsNews(news);
  }

  public List<NewsReference> getNews() {
    return newsContainer.getNews();
  }

  public int getNewsCount(Set<State> states) {
    return newsContainer.getNewsCount(states);
  }

  public void removeNews(INews news) {
    newsContainer.removeNews(news);
  }
}