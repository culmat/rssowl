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

package org.rssowl.ui.internal.filter;

import org.rssowl.core.INewsAction;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.notifier.NotificationService;

import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link INewsAction} to show the news in a notifier.
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class ShowNotifierNewsAction implements INewsAction {

  /*
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(final List<INews> news, final Object data) {
    NotificationService notificationService = Controller.getDefault().getNotificationService();
    if (data != null && data instanceof String)
      notificationService.show(news, OwlUI.getRGB((String) data));
    else
      notificationService.show(news, null);

    return Collections.emptyList();
  }

  /*
   * @see org.rssowl.core.INewsAction#isConflicting(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return false;
  }
}