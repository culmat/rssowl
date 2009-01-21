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

package org.rssowl.core.util;

import org.rssowl.core.internal.newsaction.CopyNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.ISearchFilter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper class for various Model operations.
 *
 * @author bpasero
 */
public class CoreUtils {

  /* Special case structural actions that need to run as last action */
  private static List<String> STRUCTURAL_ACTIONS = Arrays.asList(new String[] { MoveNewsAction.ID, CopyNewsAction.ID });

  /* This utility class constructor is hidden */
  private CoreUtils() {
  // Protect default constructor
  }

  /**
   * @param filter an instance of {@link ISearchFilter} to obtain a collection
   * of {@link IFilterAction}.
   * @return a collection of {@link IFilterAction}. the collection is sorted
   * such as structural actions are moved to the end of the list.
   */
  public static Collection<IFilterAction> getActions(ISearchFilter filter) {
    Set<IFilterAction> actions = new TreeSet<IFilterAction>(new Comparator<IFilterAction>() {
      public int compare(IFilterAction o1, IFilterAction o2) {
        if (STRUCTURAL_ACTIONS.contains(o1.getActionId()))
          return 1;

        if (STRUCTURAL_ACTIONS.contains(o2.getActionId()))
          return -1;

        return 1;
      }
    });

    actions.addAll(filter.getActions());
    return actions;
  }
}