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

package org.rssowl.ui.internal;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 * <ul>
 * <li>Can not use PopupDialog because Shell needs Display as parent</li>
 * <li>Find solution for showing > 8 News</li>
 * <li>Implement Service that manages lifecycle of Popup and is able to batch
 * incoming News</li>
 * <li>Consider an option per Mark/Folder to enable/disable Notification</li>
 * <li>Implement animation (take from SystemTrayAlert)</li>
 * <li>Add preferences (respect use animation, sticky until close, number of
 * news)</li>
 * <li>Enrich Popup toolbar with "Make Sticky" and a dropdown with "Options",
 * "Mark Read" etc...</li>
 * <li>Consider having buttons right to a link to open external (like Wiki!) Or
 * respect Mod Click!</li>
 * <li>Do not couple Notification with System-Tray?!</li>
 * <li>Use OS background color</li>
 * <li>Restore Shell from Tray / force active</li>
 * <li>Sort news by date to show most recent on top (at least initially)</li>
 * <li>"Feed-Title has N new News" (in FD, very leightweight, shows scrollbar
 * after a while if too many items are present)</li>
 * <li>Don't move away when mouse is over tooltip</li>
 * <li>Consider 2 modes: Stacked (3/10 with arrows and showing description) and
 * Grouped (N Headlines at once)</li>
 * <li>Make sure that the popup is always having the same horizontal width</li>
 * <li>Provide own font for alert (default font size)</li>
 * </ul>
 *
 * @author bpasero
 */
public class NotificationPopup extends PopupDialog {

  /* Max. Number of News being displayed in the Popup */
  //private static final int MAX_NEWS = 8;

  /* Max. Title Length per News */
  private static final int MAX_TITLE_LENGTH = 50;

  /* Singleton instance */
  private static NotificationPopup fgInstance;

  private List<INews> fNews = new ArrayList<INews>();
  private ResourceManager fResources;
  private Map<FeedLinkReference, IBookMark> fMapFeedToBookmark;
  private Color fPopupBorderColor;
  private Color fPopupOuterCircleColor;
  private Color fPopupInnerCircleColor;
  private Image fCloseImageNormal;
  private Image fCloseImageActive;
  private Image fCloseImagePressed;
  private CLabel fTitleCircleLabel;
  private Composite fInnerContentCircle;
  private Composite fOuterContentCircle;

  /**
   * @param news
   */
  public static synchronized void showNews(List<INews> news) {

    /* Not yet opened */
    if (fgInstance == null) {
      fgInstance = new NotificationPopup();
      fgInstance.open();
    }

    /* Show News */
    fgInstance.makeVisible(news);
  }

  private void makeVisible(List<INews> newsList) {
    fNews.addAll(newsList);

    /* Update Title Label */
    fTitleCircleLabel.setText("RSSOwl - " + fNews.size() + " incoming News");

    /* Show News */
    for (INews news : newsList) {
      renderNews(news);
    }

    fOuterContentCircle.layout(true, true);
  }

  private void renderNews(final INews news) {
    FeedLinkReference feedRef = news.getFeedReference();

    final IBookMark bookmark;
    if (fMapFeedToBookmark.containsKey(feedRef))
      bookmark = fMapFeedToBookmark.get(feedRef);
    else {
      Collection<IBookMark> bookmarks = Owl.getPersistenceService().getDAOService().getBookMarkDAO().loadAll(feedRef);
      //      bookmark = null;
      bookmark = bookmarks.iterator().next();
      fMapFeedToBookmark.put(feedRef, bookmark);
    }

    final CLabel newsLabel = new CLabel(fInnerContentCircle, SWT.NONE);
    newsLabel.setImage(OwlUI.getImage(fResources, OwlUI.BOOKMARK));
    newsLabel.setBackground(fInnerContentCircle.getBackground());
    newsLabel.setCursor(newsLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    newsLabel.setText(StringUtils.smartTrim(ModelUtils.getHeadline(news), MAX_TITLE_LENGTH));
    newsLabel.setFont(OwlUI.getThemeFont(OwlUI.NEWS_TEXT_FONT_ID, SWT.BOLD));

    /* Paint text blue on mouse-enter */
    newsLabel.addMouseTrackListener(new MouseTrackAdapter() {

      @Override
      public void mouseEnter(MouseEvent e) {
        newsLabel.setForeground(newsLabel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
      }

      @Override
      public void mouseExit(MouseEvent e) {
        newsLabel.setForeground(newsLabel.getDisplay().getSystemColor(SWT.COLOR_BLACK));
      }
    });

    /* Restore RSSOwl when content-text is clicked */
    newsLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        IWorkbenchPage page = OwlUI.getPage();
        if (page != null) {

          /* First try if the Bookmark is already visible */
          IEditorReference editorRef = EditorUtils.findEditor(page.getEditorReferences(), bookmark);
          if (editorRef != null) {
            IEditorPart editor = editorRef.getEditor(false);
            if (editor instanceof FeedView) {
              ((FeedView) editor).setSelection(new StructuredSelection(news));
              close();
              page.activate(editor);
              return;
            }
          }

          /* Otherwise Open */
          boolean activateEditor = OpenStrategy.activateOnOpen();
          FeedViewInput input = new FeedViewInput(bookmark, PerformAfterInputSet.selectNews(new NewsReference(news.getId())));
          try {
            OwlUI.getPage().openEditor(input, FeedView.ID, activateEditor);
          } catch (PartInitException ex) {
            Activator.getDefault().getLog().log(ex.getStatus());
          }
        }

        /* Close Popup */
        close();
      }
    });
  }

  /** */
  protected NotificationPopup() {
    super(new Shell(PlatformUI.getWorkbench().getDisplay()), PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, false, false, false, false, null, null);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fMapFeedToBookmark = new HashMap<FeedLinkReference, IBookMark>();

    initResources();
  }

  private void initResources() {

    /* Colors */
    fPopupBorderColor = OwlUI.getColor(fResources, new RGB(125, 177, 251));
    fPopupOuterCircleColor = OwlUI.getColor(fResources, new RGB(73, 135, 234));
    fPopupInnerCircleColor = OwlUI.getColor(fResources, new RGB(241, 240, 234));

    /* Icons */
    fCloseImageNormal = OwlUI.getImage(fResources, "icons/etool16/close_normal.gif");
    fCloseImageActive = OwlUI.getImage(fResources, "icons/etool16/close_active.gif");
    fCloseImagePressed = OwlUI.getImage(fResources, "icons/etool16/close_pressed.gif");
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    getShell().setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY));
    return createDialogArea(parent);
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    parent.setBackground(fPopupBorderColor);
    ((GridLayout) parent.getLayout()).marginWidth = 1;
    ((GridLayout) parent.getLayout()).marginHeight = 1;

    /* Outer Compositing holding the controlls */
    Composite outerCircle = new Composite(parent, SWT.NO_FOCUS);
    outerCircle.setBackground(fPopupOuterCircleColor);
    outerCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    outerCircle.setLayout(LayoutUtils.createGridLayout(1, 0, 3, 3));

    /* Title area containing label and close button */
    Composite titleCircle = new Composite(outerCircle, SWT.NO_FOCUS);
    titleCircle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    titleCircle.setBackground(outerCircle.getBackground());
    titleCircle.setLayout(LayoutUtils.createGridLayout(2, 0, 0));

    /* Title Label displaying RSSOwl */
    fTitleCircleLabel = new CLabel(titleCircle, SWT.NO_FOCUS);
    fTitleCircleLabel.setImage(OwlUI.getImage(fResources, "icons/product/16x16.gif"));
    fTitleCircleLabel.setText("RSSOwl");
    fTitleCircleLabel.setFont(OwlUI.getBold("default"));
    fTitleCircleLabel.setBackground(titleCircle.getBackground());
    fTitleCircleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fTitleCircleLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* CLabel to display a cross to close the popup */
    final CLabel closeButton = new CLabel(titleCircle, SWT.NO_FOCUS);
    closeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    closeButton.setBackground(titleCircle.getBackground());
    closeButton.setImage(fCloseImageNormal);
    closeButton.setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_HAND));
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
    fOuterContentCircle.setLayout(LayoutUtils.createGridLayout(1, 3, 0));
    fOuterContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fOuterContentCircle.setBackground(outerCircle.getBackground());

    /* Middle composite to show a 1px black line around the content controlls */
    Composite middleContentCircle = new Composite(fOuterContentCircle, SWT.NO_FOCUS);
    middleContentCircle.setLayout(LayoutUtils.createGridLayout(1, 1, 1));
    middleContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    middleContentCircle.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));

    /* Inner composite containing the content controlls */
    fInnerContentCircle = new Composite(middleContentCircle, SWT.NO_FOCUS);
    fInnerContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fInnerContentCircle.setLayout(LayoutUtils.createGridLayout(1, 5, 5, 0));
    fInnerContentCircle.setBackground(fPopupInnerCircleColor);

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
    return new Point(500, 200); //TODO
  }

  /**
   * Get the Client Area of the primary Monitor.
   *
   * @return Returns the Client Area of the primary Monitor.
   */
  private Rectangle getPrimaryClientArea() {
    Monitor primaryMonitor = getShell().getDisplay().getPrimaryMonitor();
    return (primaryMonitor != null) ? primaryMonitor.getClientArea() : getShell().getDisplay().getClientArea();
  }

  /*
   * @see org.eclipse.jface.dialogs.PopupDialog#close()
   */
  @Override
  public boolean close() {
    fgInstance = null;
    fResources.dispose();
    return super.close();
  }
}