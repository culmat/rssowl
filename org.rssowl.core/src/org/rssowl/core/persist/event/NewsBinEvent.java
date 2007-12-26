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
package org.rssowl.core.persist.event;

import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.event.runnable.NewsBinEventRunnable;

public final class NewsBinEvent extends ModelEvent    {

  private final IFolder fOldParent;

  public NewsBinEvent(INewsBin newsBin, IFolder oldParent, boolean isRoot) {
    super(newsBin, isRoot);
    fOldParent = oldParent;
  }

  @Override
  public NewsBinEventRunnable createEventRunnable() {
    return new NewsBinEventRunnable();
  }

  public IFolder getOldParent() {
    return fOldParent;
  }
}
