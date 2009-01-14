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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.ui.filter.INewsActionPresentation;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * An implementation of {@link INewsActionPresentation} to select a color to be
 * used for the news in the notifier.
 *
 * @author bpasero
 */
public class ShowNotifierNewsActionPresentation implements INewsActionPresentation {
  private Composite fContainer;
  private ToolItem fColorItem;
  private RGB fSelectedColor;
  private LocalResourceManager fResources;
  private Image fColorImage;

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#create(org.eclipse.swt.widgets.Composite, java.lang.Object)
   */
  public void create(Composite parent, Object data) {
    fResources = new LocalResourceManager(JFaceResources.getResources());

    if (data != null && data instanceof String)
      fSelectedColor = OwlUI.getRGB((String) data);
    else
      fSelectedColor = new RGB(0, 0, 0);

    fContainer = new Composite(parent, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    ((GridLayout) fContainer.getLayout()).marginLeft = 5;
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    Label nameLabel = new Label(fContainer, SWT.NONE);
    nameLabel.setText("Select a Color: ");
    nameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    ToolBar colorBar = new ToolBar(fContainer, SWT.FLAT);
    colorBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    colorBar.setBackground(fContainer.getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

    fColorItem = new ToolItem(colorBar, SWT.PUSH);
    fColorItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelectColor();
      }

    });

    updateColorItem();
  }

  private void onSelectColor() {
    ColorDialog dialog = new ColorDialog(fContainer.getShell());
    dialog.setRGB(fSelectedColor);
    RGB color = dialog.open();
    if (color != null) {
      fSelectedColor = color;
      updateColorItem();
    }
  }

  private void updateColorItem() {
    Color color = OwlUI.getColor(fResources, fSelectedColor);

    /* Dispose old first */
    if (fColorImage != null)
      fColorImage.dispose();

    fColorImage = new Image(fContainer.getShell().getDisplay(), 32, 10);
    GC gc = new GC(fColorImage);

    gc.setBackground(color);
    gc.fillRectangle(0, 0, 32, 10);

    gc.setForeground(fContainer.getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));
    gc.drawRectangle(0, 0, 31, 9);

    gc.dispose();

    fColorItem.setImage(fColorImage);
  }

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#dispose()
   */
  public void dispose() {
    if (fColorImage != null)
      fColorImage.dispose();
    fContainer.dispose();
  }

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#getData()
   */
  public Object getData() {
    return OwlUI.toString(fSelectedColor);
  }
}