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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.ui.internal.OwlUI;

/**
 * The <code>WebBrowserInput</code> is used as Input to the
 * <code>WebBrowserView</code> in order to display a website.
 *
 * @author bpasero
 */
public class WebBrowserInput implements IEditorInput {
  private final String fUrl;
  private final INewsMark fContext;

  /**
   * @param url
   */
  public WebBrowserInput(String url) {
    this(url, null);
  }

  /**
   * @param url
   * @param context
   */
  public WebBrowserInput(String url, INewsMark context) {
    fUrl = url;
    fContext = context;
  }

  /**
   * @return The URL that is to to open as <code>String</code>.
   */
  public String getUrl() {
    return fUrl;
  }

  /**
   * @return the context from which this web browser input was created from or
   * <code>null</code> if none.
   */
  public INewsMark getContext() {
    return fContext;
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
    return OwlUI.getImageDescriptor("icons/eview16/webbrowser.gif");
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