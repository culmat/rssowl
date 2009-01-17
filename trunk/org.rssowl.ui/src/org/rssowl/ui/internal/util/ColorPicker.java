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

package org.rssowl.ui.internal.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.ui.internal.OwlUI;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ToolItem} drop down that allows to either select from some
 * predefined colors or to choose any color from the native color dialog.
 *
 * @author bpasero
 */
public class ColorPicker {

  private static final String[] COLOR_LABELS = new String[] { "Brick", "Bright Red", "Hot Pink", "Dark Orange", "Tangerine", "Anjou Pear", "Olive", "Light Green", "Denim", "Navy", "Dark Teal", "Plum", "Dark Pink", "Dark Gray", "Light Gray" };

  private static final RGB[] COLOR_VALUES = new RGB[] { new RGB(129, 24, 38), //Brick
      new RGB(177, 39, 52), //Bright Red
      new RGB(207, 63, 90), //Hot Pink
      new RGB(216, 107, 78), //Dark Orange
      new RGB(234, 152, 79), //Tangerine
      new RGB(235, 192, 123), //Anjou Pear
      new RGB(105, 130, 73), //Olive
      new RGB(151, 192, 136), //Light Green
      new RGB(67, 96, 138), //Denim
      new RGB(96, 106, 142), //Navy
      new RGB(113, 160, 168), //Dark Teal
      new RGB(113, 21, 88), //Plum
      new RGB(228, 158, 156), //Dark Pink
      new RGB(100, 100, 100), //Dark Gray
      new RGB(160, 160, 160) //Light Gray
  };

  private Menu fColorMenu;
  private final Composite fParent;
  private ToolBar fBar;
  private ToolItem fColorItem;
  private List<Image> fImagesToDispose = new ArrayList<Image>();
  private RGB fSelectedColor = new RGB(0, 0, 0);

  /**
   * @param parent
   * @param style
   */
  public ColorPicker(Composite parent, int style) {
    fParent = parent;
    initControl(style);
  }

  /**
   * @param color the color to show.
   */
  public void setColor(RGB color) {
    fSelectedColor = color;

    if (fColorItem.getImage() != null)
      fColorItem.getImage().dispose();

    fColorItem.setImage(createColorImage(color));
  }

  /**
   * @return the selected color
   */
  public RGB getColor() {
    return fSelectedColor;
  }

  private void initControl(int style) {
    fBar = new ToolBar(fParent, style);
    fBar.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        for (Image img : fImagesToDispose) {
          img.dispose();
        }
      }
    });

    createColorMenu();

    fColorItem = new ToolItem(fBar, SWT.DROP_DOWN);
    fColorItem.setImage(createColorImage(fSelectedColor));
    fColorItem.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        Rectangle rect = fColorItem.getBounds();
        Point pt = new Point(rect.x, rect.y + rect.height);
        pt = fBar.toDisplay(pt);
        fColorMenu.setLocation(pt.x, pt.y);
        fColorMenu.setVisible(true);
      }
    });
  }

  private Menu createColorMenu() {
    fColorMenu = new Menu(fParent.getShell(), SWT.POP_UP);

    /* Add some useful Colors */
    for (int i = 0; i < COLOR_LABELS.length; i++) {
      MenuItem item = new MenuItem(fColorMenu, SWT.PUSH);
      item.setText(COLOR_LABELS[i]);

      final RGB color = COLOR_VALUES[i];
      item.setImage(createColorImage(color));
      item.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          onColorSelected(color);
        }
      });
    }

    /* Add Item to open the native Color Picker */
    new MenuItem(fColorMenu, SWT.SEPARATOR);
    MenuItem moreColor = new MenuItem(fColorMenu, SWT.PUSH);
    moreColor.setText("Other...");
    moreColor.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelectOtherColor();
      }
    });

    return fColorMenu;
  }

  private void onColorSelected(RGB color) {
    fSelectedColor = color;

    if (fColorItem.getImage() != null)
      fColorItem.getImage().dispose();

    fColorItem.setImage(createColorImage(fSelectedColor));
  }

  private Image createColorImage(RGB color) {
    Image img = OwlUI.createColorImage(fParent.getDisplay(), color);
    fImagesToDispose.add(img);

    return img;
  }

  private void onSelectOtherColor() {
    ColorDialog dialog = new ColorDialog(fParent.getShell());
    dialog.setRGB(fSelectedColor);
    RGB color = dialog.open();
    if (color != null)
      onColorSelected(color);
  }
}