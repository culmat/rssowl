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

package org.rssowl.core.model.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.internal.persist.Persistable;

/**
 * <p>
 * TODO Consider contributing the NewsCounter from org.rssowl.ui if its only
 * used from that bundle.
 * </p>
 */
public final class NewsCounterItem extends Persistable implements IPersistable {
  private int newCounter;
  private int unreadCounter;
  private int stickyCounter;

  public NewsCounterItem() {

  }

  public final int getNewCounter() {
    return newCounter;
  }

  public final void incrementNewCounter() {
    ++newCounter;
  }

  public final void decrementNewCounter() {
    Assert.isTrue(newCounter > 0, "newCounter must not be negative"); //$NON-NLS-1$
    --newCounter;
  }

  public final int getUnreadCounter() {
    return unreadCounter;
  }

  public final void incrementUnreadCounter() {
    ++unreadCounter;
  }

  public final void decrementUnreadCounter() {
    Assert.isTrue(unreadCounter > 0, "unreadCounter must not be negative"); //$NON-NLS-1$
    --unreadCounter;
  }

  public final int getStickyCounter() {
    return stickyCounter;
  }

  public final void incrementStickyCounter() {
    ++stickyCounter;
  }

  public final void decrementStickyCounter() {
    Assert.isTrue(stickyCounter > 0, "stickyCounter must not be negative"); //$NON-NLS-1$
    --stickyCounter;
  }
}
