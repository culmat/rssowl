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

package org.rssowl.ui.internal.dialogs.cleanup;

/**
 * A container of all supported clean up operations in RSSOwl.
 *
 * @author bpasero
 */
class CleanUpOperations {

  /* Feed Operations */
  private int fLastVisitInDays;
  private int fLastUpdateInDays;
  private boolean fDeleteFeedsByConError;

  /* News Operations */
  private int fMaxNewsCountPerFeed;
  private int fMaxNewsAge;
  private boolean fDeleteReadNews;
  private boolean fKeepUnreadNews;

  CleanUpOperations(int lastVisitInDays, int lastUpdateInDays, boolean deleteFeedsByConError, int maxNewsCountPerFeed, int maxNewsAge, boolean deleteReadNews, boolean keepUnreadNews) {
    fLastVisitInDays = lastVisitInDays;
    fLastUpdateInDays = lastUpdateInDays;
    fDeleteFeedsByConError = deleteFeedsByConError;
    fMaxNewsCountPerFeed = maxNewsCountPerFeed;
    fMaxNewsAge = maxNewsAge;
    fDeleteReadNews = deleteReadNews;
    fKeepUnreadNews = keepUnreadNews;
  }

  boolean deleteFeedByLastVisit() {
    return fLastVisitInDays > 0;
  }

  int getLastVisitDays() {
    return fLastVisitInDays;
  }

  boolean deleteFeedByLastUpdate() {
    return fLastUpdateInDays > 0;
  }

  int getLastUpdateDays() {
    return fLastUpdateInDays;
  }

  boolean deleteFeedsByConError() {
    return fDeleteFeedsByConError;
  }

  boolean deleteNewsByCount() {
    return fMaxNewsCountPerFeed >= 0;
  }

  int getMaxNewsCountPerFeed() {
    return fMaxNewsCountPerFeed;
  }

  boolean deleteNewsByAge() {
    return fMaxNewsAge > 0;
  }

  int getMaxNewsAge() {
    return fMaxNewsAge;
  }

  boolean deleteReadNews() {
    return fDeleteReadNews;
  }

  boolean keepUnreadNews() {
    return fKeepUnreadNews;
  }
}