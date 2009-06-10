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

package org.rssowl.ui.internal.editors.feed;

/**
 * Enumeration used for the {@link NewsTableControl} representing the available
 * columns for a feed to display.
 *
 * @author bpasero
 */
public enum NewsColumn {

  /** Title of the News */
  TITLE("Title", true, false, true, true, true, true, true),

  /** Date of the News */
  DATE("Date", true, false, false, true, true, true, true),

  /** Author of the News */
  AUTHOR("Author", true, false, true, true, true, true, true),

  /** Category of the News */
  CATEGORY("Category", true, false, true, true, true, true, true),

  /** Labels */
  LABELS("Label", true, false, true, true, true, true, true),

  /** Status */
  STATUS("Status", true, false, true, true, true, true, true),

  /** Sticky-State of the News */
  STICKY("Sticky", false, true, false, true, false, true, false),

  /** Attachments */
  ATTACHMENTS("Attachments", false, true, false, true, false, true, false),

  /** Feed of a News */
  FEED("Feed", false, true, false, true, false, true, false),

  /** Relevance of a News (not selectable) */
  RELEVANCE("Relevance", false, true, false, false, false, true, false);

  private final String fName;
  private final boolean fShowName;
  private final boolean fShowTooltip;
  private final boolean fPrefersAscending;
  private final boolean fSelectable;
  private final boolean fResizable;
  private final boolean fMoveable;
  private final boolean fShowSortIndicator;

  NewsColumn(String name, boolean showName, boolean showTooltip, boolean prefersAscending, boolean selectable, boolean resizable, boolean moveable, boolean showSortIndicator) {
    fName = name;
    fShowName = showName;
    fShowTooltip = showTooltip;
    fPrefersAscending = prefersAscending;
    fSelectable = selectable;
    fResizable = resizable;
    fMoveable = moveable;
    fShowSortIndicator = showSortIndicator;
  }

  /**
   * @return the name of the column or <code>null</code> if none.
   */
  public String getName() {
    return fName;
  }

  /**
   * @return <code>true</code> if the column should show its name or
   * <code>false</code> otherwise.
   */
  public boolean showName() {
    return fShowName;
  }

  /**
   * @return <code>true</code> if the column should show its tooltip or
   * <code>false</code> otherwise.
   */
  public boolean showTooltip() {
    return fShowTooltip;
  }

  /**
   * @return Returns <code>TRUE</code> if this Column prefers to be sorted
   * ascending and <code>FALSE</code> otherwise.
   */
  public boolean prefersAscending() {
    return fPrefersAscending;
  }

  /**
   * @return <code>true</code> if this column is selectable for the user and
   * <code>false</code> otherwise.
   */
  public boolean isSelectable() {
    return fSelectable;
  }

  /**
   * @return <code>true</code> if this column should indicate the sort direction
   * and <code>false</code> otherwise.
   */
  public boolean showSortIndicator() {
    return fShowSortIndicator;
  }

  /**
   * @return <code>true</code> if the column is resizable and <code>false</code>
   * otherwise.
   */
  public boolean isResizable() {
    return fResizable;
  }

  /**
   * @return <code>true</code> if the column is moveable and <code>false</code>
   * otherwise.
   */
  public boolean isMoveable() {
    return fMoveable;
  }
}