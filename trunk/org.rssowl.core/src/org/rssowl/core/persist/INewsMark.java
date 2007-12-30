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

import org.rssowl.core.persist.reference.NewsReference;

import java.util.List;
import java.util.Set;

public interface INewsMark extends IMark    {
  //TODO Do we need to provide getNews and getNewsRefs? This could be useful
  //for optimization. e.g. if a Mark needs to resolve the INews and the caller
  //needs the resolved INews, it would be wasteful to do getNewsRefs followed
  //by a resolve for each news.
  List<INews> getNews();

  //TODO Need to decide how to deal with cases where the subclass doesn't
  //hold news in a certain state. For example, a given SM search may include
  //INews in HIDDEN or DELETED state, but we don't bother storing those since neither
  //the UI or other users need those INews atm. On the other hand, INewsBin
  //will store HIDDEN news. Seems like we need to document that subclasses
  //will define the exact details of what states are returned.
  List<INews> getNews(Set<INews.State> states);

  List<NewsReference> getNewsRefs();
  List<NewsReference> getNewsRefs(Set<INews.State> states);

  //TODO Unclear if all subclasses can do this efficiently. There are some
  //problems with the idea of having IBookMark storing long[] or delegating
  //to a IFeed that does this
  int getNewsCount(Set<INews.State> states);
}
