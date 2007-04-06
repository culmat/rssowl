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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.rssowl.ui.internal.RSSOwlUI;

/**
 * The <code>WebBrowserInput</code> is used as Input to the
 * <code>WebBrowserView</code> in order to display a website.
 *
 * @author bpasero
 */
public class WebBrowserInput implements IEditorInput {
  private final String fUrl;

  /**
   * @param url
   */
  public WebBrowserInput(String url) {
    fUrl = url;
  }

  /**
   * @return The URL that is to to open as <code>String</code>.
   */
  public String getUrl() {
    return fUrl;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#exists()
   */
  public boolean exists() {
    return false;
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
   */
  public ImageDescriptor getImageDescriptor() {
    return RSSOwlUI.getImageDescriptor("icons/eview16/webbrowser.gif");
  }

  /*
   * @see org.eclipse.ui.IEditorInput#getName()
   */
  public String getName() {
    return fUrl;
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
    return "";
  }

  /*
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return null;
  }
}