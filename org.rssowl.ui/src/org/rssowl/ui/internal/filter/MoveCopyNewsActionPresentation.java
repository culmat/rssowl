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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.ui.filter.INewsActionPresentation;
import org.rssowl.ui.internal.search.LocationControl;
import org.rssowl.ui.internal.search.LocationControl.Mode;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

/**
 * An implementation of {@link INewsActionPresentation} for moving or copying
 * news to news bins.
 *
 * @author bpasero
 */
public class MoveCopyNewsActionPresentation implements INewsActionPresentation {
  private Composite fContainer;
  private LocationControl fLocationControl;

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#create(org.eclipse.swt.widgets.Composite, java.lang.Object)
   */
  public void create(Composite parent, Object data) {
    fContainer = new Composite(parent, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    ((GridLayout) fContainer.getLayout()).marginLeft = 5;
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    Label label = new Label(fContainer, SWT.None);
    label.setText("to News Bins: ");
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    fLocationControl = new LocationControl(fContainer, SWT.None, Mode.SELECT_BIN);
    fLocationControl.setLayout(LayoutUtils.createGridLayout(1, 0, 1));
    fLocationControl.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    if (data != null && data instanceof Long[]) {
      Long[][] locationData = new Long[3][];
      locationData[ModelUtils.NEWSBIN] = (Long[]) data;
      fLocationControl.select(locationData);
    }
  }

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#dispose()
   */
  public void dispose() {
    fContainer.dispose();
  }

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#getData()
   */
  public Long[] getData() {
    Long[][] selection = fLocationControl.getSelection();
    if (selection != null) {
      return selection[ModelUtils.NEWSBIN];
    }

    return null;
  }
}