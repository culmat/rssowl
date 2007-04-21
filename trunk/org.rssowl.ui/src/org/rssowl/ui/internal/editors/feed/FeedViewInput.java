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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.ui.internal.OwlUI;

/**
 * @author bpasero
 */
public class FeedViewInput implements IEditorInput {
  private IMark fMark;
  private boolean fIsDeleted;
  private boolean fIsBookMark;
  private PerformAfterInputSet fPerformOnInputSet;

  /**
   * @param mark
   */
  public FeedViewInput(IMark mark) {
    this(mark, null);
  }

  /**
   * @param mark
   * @param performOnInputSet
   */
  public FeedViewInput(IMark mark, PerformAfterInputSet performOnInputSet) {
    Assert.isNotNull(mark);
    fMark = mark;
    fIsBookMark = mark instanceof IBookMark;
    fPerformOnInputSet = performOnInputSet;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#exists()
   */
  public boolean exists() {
    return !fIsDeleted;
  }

  /** Marks this Input as Deleted (exists = false) */
  public void setDeleted() {
    fIsDeleted = true;
  }

  /**
   * @return Returns the action that is to be done automatically once the input
   * has been set.
   */
  public PerformAfterInputSet getPerformOnInputSet() {
    return fPerformOnInputSet;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    if (fIsBookMark) {
      IBookMark bookmark = (IBookMark) fMark;
      ImageDescriptor favicon = OwlUI.getFavicon(bookmark);
      if (favicon != null)
        return favicon;
      return OwlUI.BOOKMARK;
    } else if (fMark instanceof ISearchMark)
      return OwlUI.SEARCHMARK;

    return OwlUI.UNKNOWN;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getName()
   */
  public String getName() {
    return fMark.getName();
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getPersistable()
   */
  public IPersistableElement getPersistable() {
    return null;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getToolTipText()
   */
  public String getToolTipText() {
    return fMark.getName();
  }

  /*
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return Platform.getAdapterManager().getAdapter(this, adapter);
  }

  /**
   * @return Returns the mark.
   */
  public IMark getMark() {
    return fMark;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + fMark.hashCode();
    return result;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    FeedViewInput type = (FeedViewInput) obj;
    return fMark.equals(type.fMark);
  }
}