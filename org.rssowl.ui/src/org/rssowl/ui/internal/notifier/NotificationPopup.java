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

package org.rssowl.ui.internal.notifier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.ApplicationWorkbenchAdvisor;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CCLabel;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO
 * <ul>
 * <li>Implement animation (and transparency)</li>
 * <li>Add preferences (respect use animation, number of news)</li>
 * <li>Enrich Popup toolbar with "Make Sticky" and a dropdown with "Options",
 * "Mark Read" etc...</li>
 * <li>Consider 2 modes: Stacked (3/10 with arrows and showing description) and
 * Grouped (N Headlines at once)</li>
 * <li>SearchNotificationItems are not aggregated if the max. number of items
 * is already showing</li>
 * </ul>
 *
 * @author bpasero
 */
public class NotificationPopup extends PopupDialog {

  /* Height of a CLabel with 16px Icon inside */
  private static final int CLABEL_HEIGHT = 22;

  /* Max. Number of Items being displayed in the Popup */
  private static final int MAX_ITEMS = 30;

  /* Default Width of the Popup */
  private static final int DEFAULT_WIDTH = 400;

  private Shell fShell;
  private List<NotificationItem> fRecentItems = new ArrayList<NotificationItem>();
  private ResourceManager fResources;
  private Image fCloseImageNormal;
  private Image fCloseImageActive;
  private Image fCloseImagePressed;
  private CLabel fTitleCircleLabel;
  private Composite fInnerContentCircle;
  private Composite fOuterContentCircle;
  private Font fBoldTextFont;
  private int fItemCounter;
  private UIJob fAutoCloser;
  private MouseTrackListener fMouseTrackListner;
  private IPreferenceScope fGlobalScope;
  private int fVisibleItemsCount;
  private int fItemLimit;
  private NotifierColors fNotifierColors;
  private Region fLastUsedRegion;
  private Image fItemStickyIcon;
  private Image fItemNonStickyIcon;
  private Image fItemNonStickyDisabledIcon;
  private Color fStickyBgColor;

  NotificationPopup(int visibleItemCount) {
    super(new Shell(PlatformUI.getWorkbench().getDisplay()), PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, false, false, false, false, null, null);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fBoldTextFont = OwlUI.getThemeFont(OwlUI.NOTIFICATION_POPUP_FONT_ID, SWT.BOLD);
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();

    fItemLimit = fGlobalScope.getInteger(DefaultPreferences.LIMIT_NOTIFICATION_SIZE);
    if (fItemLimit <= 0)
      fItemLimit = MAX_ITEMS;

    fVisibleItemsCount = (visibleItemCount > fItemLimit) ? fItemLimit : visibleItemCount;
    createAutoCloser();
    createMouseTrackListener();

    initResources();
  }

  private void addRegion(Shell shell) {
    Region region = new Region();
    Point s = shell.getSize();

    /* Add entire Shell */
    region.add(0, 0, s.x, s.y);

    /* Subtract Top-Left Corner */
    region.subtract(0, 0, 5, 1);
    region.subtract(0, 1, 3, 1);
    region.subtract(0, 2, 2, 1);
    region.subtract(0, 3, 1, 1);
    region.subtract(0, 4, 1, 1);

    /* Subtract Top-Right Corner */
    region.subtract(s.x - 5, 0, 5, 1);
    region.subtract(s.x - 3, 1, 3, 1);
    region.subtract(s.x - 2, 2, 2, 1);
    region.subtract(s.x - 1, 3, 1, 1);
    region.subtract(s.x - 1, 4, 1, 1);

    /* Dispose old first */
    if (shell.getRegion() != null)
      shell.getRegion().dispose();

    /* Apply Region */
    shell.setRegion(region);

    /* Remember to dispose later */
    fLastUsedRegion = region;
  }

  private void createAutoCloser() {
    fAutoCloser = new UIJob(PlatformUI.getWorkbench().getDisplay(), "") {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (getShell() != null && !getShell().isDisposed())
          close();

        return Status.OK_STATUS;
      }
    };

    fAutoCloser.setSystem(true);
  }

  /* Listener to control Auto-Close Job */
  private void createMouseTrackListener() {
    fMouseTrackListner = new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        if (!fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP))
          fAutoCloser.cancel();
      }

      @Override
      public void mouseExit(MouseEvent e) {
        if (!fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP))
          fAutoCloser.schedule(fGlobalScope.getInteger(DefaultPreferences.AUTOCLOSE_NOTIFICATION_VALUE) * 1000);
      }
    };
  }

  void makeVisible(Collection<NotificationItem> items) {

    /* Cancel Auto Closer and reschedule */
    if (!fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP)) {
      fAutoCloser.cancel();
      fAutoCloser.schedule(fGlobalScope.getInteger(DefaultPreferences.AUTOCLOSE_NOTIFICATION_VALUE) * 1000);
    }

    /* Remember count of Items */
    fItemCounter += getNewsCount(items);

    /* Update Title Label */
    if (fItemCounter > 0)
      fTitleCircleLabel.setText("RSSOwl - " + fItemCounter + " incoming News");
    else
      fTitleCircleLabel.setText("RSSOwl - Incoming News");

    /* Never show more than MAX_ITEMS items */
    if (fRecentItems.size() >= fItemLimit)
      return;

    /* Remove any old duplicate first */
    for (NotificationItem item : items)
      fRecentItems.remove(item);

    /* Add to recent Items List */
    fRecentItems.addAll(items);

    /* Sort Items */
    Collections.sort(fRecentItems);

    /* Dispose old Items first */
    Control[] children = fInnerContentCircle.getChildren();
    for (Control child : children) {
      child.dispose();
    }

    /* Show Items */
    fVisibleItemsCount = 0;
    for (int i = 0; i < fItemLimit && i < fRecentItems.size(); i++) {
      renderItem(fRecentItems.get(i));
      fVisibleItemsCount++;
    }

    /* Layout */
    fOuterContentCircle.layout(true, true);

    /* Update Shell Bounds */
    Point oldSize = fShell.getSize();
    int newHeight = fShell.computeSize(DEFAULT_WIDTH, SWT.DEFAULT).y;

    Point newSize = new Point(oldSize.x, newHeight);
    Point newLocation = getInitialLocation(newSize);
    fShell.setBounds(newLocation.x, newLocation.y, newSize.x, newSize.y);

    /* Add Region to Shell */
    addRegion(fShell);
  }

  private int getNewsCount(Collection<NotificationItem> items) {
    int count = 0;

    for (NotificationItem item : items) {
      if (item instanceof NewsNotificationItem)
        count++;
    }

    return count;
  }

  private void renderItem(final NotificationItem item) {

    /* Use a CCLabel per Item */
    final CCLabel itemLabel = new CCLabel(fInnerContentCircle, SWT.NONE);
    itemLabel.setBackground(fInnerContentCircle.getBackground());
    itemLabel.setCursor(itemLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    itemLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    /* Offer Label to mark item sticky */
    final CLabel markStickyLabel = new CLabel(fInnerContentCircle, SWT.NONE);
    markStickyLabel.setImage(item.supportsSticky() ? fItemNonStickyIcon : fItemNonStickyDisabledIcon);
    markStickyLabel.setBackground(fInnerContentCircle.getBackground());
    markStickyLabel.setEnabled(item.supportsSticky());
    markStickyLabel.setCursor(fShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    markStickyLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent e) {
        boolean newStateSticky = !item.isSticky();

        /* Update Background Color */
        itemLabel.setBackground(newStateSticky ? fStickyBgColor : fInnerContentCircle.getBackground());
        markStickyLabel.setBackground(newStateSticky ? fStickyBgColor : fInnerContentCircle.getBackground());

        /* Update Image */
        markStickyLabel.setImage(newStateSticky ? fItemStickyIcon : fItemNonStickyIcon);

        /* Apply state */
        item.setSticky(newStateSticky);
      }
    });

    itemLabel.setText(item.getText());
    itemLabel.setFont(fBoldTextFont);
    itemLabel.addMouseTrackListener(fMouseTrackListner);
    itemLabel.setImage(OwlUI.getImage(fResources, item.getImage()));

    /* Paint text blue on mouse-enter */
    itemLabel.addMouseTrackListener(new MouseTrackAdapter() {

      @Override
      public void mouseEnter(MouseEvent e) {
        itemLabel.setForeground(itemLabel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
      }

      @Override
      public void mouseExit(MouseEvent e) {
        itemLabel.setForeground(itemLabel.getDisplay().getSystemColor(SWT.COLOR_BLACK));
      }
    });

    /* Restore RSSOwl label is clicked */
    itemLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {

        /* Open Item */
        item.open(e);

        /* Close Popup */
        close();
      }
    });
  }

  private void restoreWindow(IWorkbenchPage page) {
    Shell applicationShell = page.getWorkbenchWindow().getShell();

    /* Restore from Tray or Minimization if required */
    ApplicationWorkbenchWindowAdvisor advisor = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
    if (advisor != null && advisor.isMinimizedToTray())
      advisor.restoreFromTray(applicationShell);
    else if (applicationShell.getMinimized()) {
      applicationShell.setMinimized(false);
      applicationShell.forceActive();
    }
  }

  private void initResources() {

    /* Colors */
    fNotifierColors = new NotifierColors(getParentShell().getDisplay(), fResources);
    fStickyBgColor = OwlUI.getThemeColor(OwlUI.STICKY_BG_COLOR_ID, fResources, new RGB(255, 255, 128));

    /* Icons */
    fCloseImageNormal = OwlUI.getImage(fResources, "icons/etool16/close_normal.gif");
    fCloseImageActive = OwlUI.getImage(fResources, "icons/etool16/close_active.gif");
    fCloseImagePressed = OwlUI.getImage(fResources, "icons/etool16/close_pressed.gif");
    fItemStickyIcon = OwlUI.getImage(fResources, OwlUI.NEWS_PINNED);
    fItemNonStickyIcon = OwlUI.getImage(fResources, OwlUI.NEWS_PIN);
    fItemNonStickyDisabledIcon = OwlUI.getImage(fResources, "icons/obj16/news_pin_disabled.gif");
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    fShell = parent.getShell();
    parent.setBackground(fNotifierColors.getBorder());

    return createDialogArea(parent);
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    ((GridLayout) parent.getLayout()).marginWidth = 1;
    ((GridLayout) parent.getLayout()).marginHeight = 1;

    /* Outer Compositing holding the controlls */
    final Composite outerCircle = new Composite(parent, SWT.NO_FOCUS);
    outerCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    outerCircle.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0));

    /* Title area containing label and close button */
    final Composite titleCircle = new Composite(outerCircle, SWT.NO_FOCUS);
    titleCircle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    titleCircle.setBackgroundMode(SWT.INHERIT_FORCE);
    titleCircle.setLayout(LayoutUtils.createGridLayout(2, 3, 0));
    titleCircle.addMouseTrackListener(fMouseTrackListner);
    titleCircle.addControlListener(new ControlAdapter() {
      @Override
      public void controlResized(ControlEvent e) {
        Rectangle clArea = titleCircle.getClientArea();
        Image newBGImage = new Image(titleCircle.getDisplay(), clArea.width, clArea.height);
        GC gc = new GC(newBGImage);

        /* Gradient */
        drawGradient(gc, clArea);

        /* Fix Region Shape */
        fixRegion(gc, clArea);

        gc.dispose();

        Image oldBGImage = titleCircle.getBackgroundImage();
        titleCircle.setBackgroundImage(newBGImage);

        if (oldBGImage != null)
          oldBGImage.dispose();
      }

      private void drawGradient(GC gc, Rectangle clArea) {
        gc.setForeground(fNotifierColors.getGradientBegin());
        gc.setBackground(fNotifierColors.getGradientEnd());
        gc.fillGradientRectangle(clArea.x, clArea.y, clArea.width, clArea.height, true);
      }

      private void fixRegion(GC gc, Rectangle clArea) {
        gc.setForeground(fNotifierColors.getBorder());

        /* Fill Top Left */
        gc.drawPoint(2, 0);
        gc.drawPoint(3, 0);
        gc.drawPoint(1, 1);
        gc.drawPoint(0, 2);
        gc.drawPoint(0, 3);

        /* Fill Top Right */
        gc.drawPoint(clArea.width - 4, 0);
        gc.drawPoint(clArea.width - 3, 0);
        gc.drawPoint(clArea.width - 2, 1);
        gc.drawPoint(clArea.width - 1, 2);
        gc.drawPoint(clArea.width - 1, 3);
      }
    });

    /* Title Label displaying RSSOwl */
    fTitleCircleLabel = new CLabel(titleCircle, SWT.NO_FOCUS);
    fTitleCircleLabel.setImage(OwlUI.getImage(fResources, "icons/product/24x24.png"));
    fTitleCircleLabel.setText("RSSOwl");
    fTitleCircleLabel.setFont(fBoldTextFont);
    fTitleCircleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fTitleCircleLabel.addMouseTrackListener(fMouseTrackListner);
    fTitleCircleLabel.setCursor(fShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    fTitleCircleLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        IWorkbenchPage page = OwlUI.getPage();
        if (page != null) {

          /* Restore Window */
          restoreWindow(page);

          /* Close Notifier */
          close();
        }
      }
    });

    /* CLabel to display a cross to close the popup */
    final CLabel closeButton = new CLabel(titleCircle, SWT.NO_FOCUS);
    closeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    closeButton.setImage(fCloseImageNormal);
    closeButton.setCursor(fShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    closeButton.addMouseTrackListener(fMouseTrackListner);
    closeButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        close();
      }

      @Override
      public void mouseDown(MouseEvent e) {
        closeButton.setImage(fCloseImagePressed);
      }
    });

    closeButton.addMouseTrackListener(new MouseTrackAdapter() {

      /* Show hot image on mouse-enter */
      @Override
      public void mouseEnter(MouseEvent e) {
        closeButton.setImage(fCloseImageActive);
      }

      /* Show normal image on mouse-exit */
      @Override
      public void mouseExit(MouseEvent e) {
        closeButton.setImage(fCloseImageNormal);
      }
    });

    /* Outer composite to hold content controlls */
    fOuterContentCircle = new Composite(outerCircle, SWT.NONE);
    fOuterContentCircle.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    fOuterContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fOuterContentCircle.setBackground(outerCircle.getBackground());

    /* Middle composite to show a 1px black line around the content controlls */
    Composite middleContentCircle = new Composite(fOuterContentCircle, SWT.NO_FOCUS);
    middleContentCircle.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    ((GridLayout) middleContentCircle.getLayout()).marginTop = 1;
    middleContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    middleContentCircle.setBackground(fNotifierColors.getBorder());

    /* Inner composite containing the content controlls */
    fInnerContentCircle = new Composite(middleContentCircle, SWT.NO_FOCUS);
    fInnerContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fInnerContentCircle.setLayout(LayoutUtils.createGridLayout(2, 0, 5, 0, 0, false));
    ((GridLayout) fInnerContentCircle.getLayout()).marginLeft = 5;
    ((GridLayout) fInnerContentCircle.getLayout()).marginRight = 2;
    fInnerContentCircle.addMouseTrackListener(fMouseTrackListner);
    fInnerContentCircle.setBackground(fShell.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    return outerCircle;
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#getInitialLocation(org.eclipse.swt.graphics.Point)
   */
  @Override
  protected Point getInitialLocation(Point initialSize) {
    Rectangle clArea = getPrimaryClientArea();

    return new Point(clArea.width + clArea.x - initialSize.x, clArea.height + clArea.y - initialSize.y);
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#getInitialSize()
   */
  @Override
  protected Point getInitialSize() {
    int initialHeight = fShell.computeSize(DEFAULT_WIDTH, SWT.DEFAULT).y;

    return new Point(DEFAULT_WIDTH, initialHeight + fVisibleItemsCount * CLABEL_HEIGHT);
  }

  /**
   * Get the Client Area of the primary Monitor.
   *
   * @return Returns the Client Area of the primary Monitor.
   */
  private Rectangle getPrimaryClientArea() {
    Monitor primaryMonitor = fShell.getDisplay().getPrimaryMonitor();
    return (primaryMonitor != null) ? primaryMonitor.getClientArea() : fShell.getDisplay().getClientArea();
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#close()
   */
  @Override
  public boolean close() {
    fResources.dispose();
    if (fLastUsedRegion != null)
      fLastUsedRegion.dispose();

    return super.close();
  }
}