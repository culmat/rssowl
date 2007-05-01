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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
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
import org.eclipse.ui.progress.UIJob;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.OwlUI.OSTheme;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 * <ul>
 * <li>Consider batching new News in NotificationService for a couple of
 * seconds before showing the popup to avoid size growing</li>
 * <li>Consider an option per Mark/Folder to enable/disable Notification</li>
 * <li>Implement animation (take from SystemTrayAlert)</li>
 * <li>Add preferences (respect use animation, number of news)</li>
 * <li>Enrich Popup toolbar with "Make Sticky" and a dropdown with "Options",
 * "Mark Read" etc...</li>
 * <li>Consider 2 modes: Stacked (3/10 with arrows and showing description) and
 * Grouped (N Headlines at once)</li>
 * </ul>
 *
 * @author bpasero
 */
public class NotificationPopup extends PopupDialog {

  /* Max. Number of News being displayed in the Popup */
  private static final int MAX_NEWS = 5;

  /* Default Width of the Popup */
  private static final int DEFAULT_WIDTH = 400;

  /* Time after the popup is closed automatically */
  private static final int AUTO_CLOSE_TIME = 8000;

  /* Singleton instance */
  private static NotificationPopup fgInstance;

  private List<INews> fRecentNews = new ArrayList<INews>();
  private ResourceManager fResources;
  private Map<FeedLinkReference, IBookMark> fMapFeedToBookmark;
  private Color fPopupBorderColor;
  private Color fPopupOuterCircleColor;
  private Image fCloseImageNormal;
  private Image fCloseImageActive;
  private Image fCloseImagePressed;
  private CLabel fTitleCircleLabel;
  private Composite fInnerContentCircle;
  private Composite fOuterContentCircle;
  private Font fBoldTextFont;
  private int fNewsCounter;
  private UIJob fAutoCloser;
  private MouseTrackListener fMouseTrackListner;
  private IPreferenceScope fGlobalScope;
  private int fVisibleNewsCount;

  /**
   * Opens the <code>NotificationPopup</code> if not yet opened and shows the
   * given List of News.
   *
   * @param news The <code>List</code> of news that should be shown in the
   * popup.
   */
  public static synchronized void showNews(List<INews> news) {

    /* Not yet opened */
    if (fgInstance == null) {
      fgInstance = new NotificationPopup(news.size());
      fgInstance.open();
    }

    /* Show News */
    fgInstance.makeVisible(news);
  }

  private NotificationPopup(int visibleNewsCount) {
    super(new Shell(PlatformUI.getWorkbench().getDisplay()), PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, false, false, false, false, null, null);
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fMapFeedToBookmark = new HashMap<FeedLinkReference, IBookMark>();
    fBoldTextFont = OwlUI.getThemeFont(OwlUI.NOTIFICATION_POPUP_FONT_ID, SWT.BOLD);
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
    fVisibleNewsCount = (visibleNewsCount > MAX_NEWS) ? MAX_NEWS : visibleNewsCount;
    createAutoCloser();
    createMouseTrackListener();

    initResources();
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
          fAutoCloser.schedule(AUTO_CLOSE_TIME);
      }
    };
  }

  private void makeVisible(List<INews> newsList) {

    /* Cancel Auto Closer and reschedule */
    if (!fGlobalScope.getBoolean(DefaultPreferences.STICKY_NOTIFICATION_POPUP)) {
      fAutoCloser.cancel();
      fAutoCloser.schedule(AUTO_CLOSE_TIME);
    }

    /* Remember count of News */
    fNewsCounter += newsList.size();

    /* Update Title Label */
    fTitleCircleLabel.setText("RSSOwl - " + fNewsCounter + " incoming News");

    /* Never show more than MAX_NEWS news */
    if (fRecentNews.size() >= MAX_NEWS)
      return;

    /* Add to recent News List */
    fRecentNews.addAll(newsList);

    /* Sort by Date */
    Collections.sort(fRecentNews, new Comparator<INews>() {
      public int compare(INews news1, INews news2) {
        Date date1 = DateUtils.getRecentDate(news1);
        Date date2 = DateUtils.getRecentDate(news2);

        return date2.compareTo(date1);
      }
    });

    /* Dispose old News first */
    Control[] children = fInnerContentCircle.getChildren();
    for (Control child : children) {
      child.dispose();
    }

    /* Show News */
    int oldVisibleNewsCount = fVisibleNewsCount;
    fVisibleNewsCount = 0;
    for (int i = 0; i < MAX_NEWS && i < fRecentNews.size(); i++) {
      renderNews(fRecentNews.get(i));
      fVisibleNewsCount++;
    }

    /* Layout */
    fOuterContentCircle.layout(true, true);

    /* Update Shell Bounds */
    Point oldSize = getShell().getSize();
    int labelHeight = fTitleCircleLabel.computeSize(DEFAULT_WIDTH, SWT.DEFAULT).y;
    int newHeight = oldSize.y + (fVisibleNewsCount - oldVisibleNewsCount) * labelHeight;

    Point newSize = new Point(oldSize.x, newHeight);
    Point newLocation = getInitialLocation(newSize);
    getShell().setBounds(newLocation.x, newLocation.y, newSize.x, newSize.y);
  }

  private void renderNews(final INews news) {
    FeedLinkReference feedRef = news.getFeedReference();

    /* Retrieve Bookmark */
    final IBookMark bookmark;
    if (fMapFeedToBookmark.containsKey(feedRef))
      bookmark = fMapFeedToBookmark.get(feedRef);
    else {
      Collection<IBookMark> bookmarks = Owl.getPersistenceService().getDAOService().getBookMarkDAO().loadAll(feedRef);
      bookmark = bookmarks.iterator().next();
      fMapFeedToBookmark.put(feedRef, bookmark);
    }

    /* Use a CCLabel per News */
    final CCLabel newsLabel = new CCLabel(fInnerContentCircle, SWT.NONE);
    newsLabel.setImage(OwlUI.getImage(fResources, OwlUI.getFavicon(bookmark)));
    newsLabel.setBackground(fInnerContentCircle.getBackground());
    newsLabel.setCursor(newsLabel.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
    newsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    newsLabel.setText(ModelUtils.getHeadline(news));
    newsLabel.setFont(fBoldTextFont);
    newsLabel.addMouseTrackListener(fMouseTrackListner);

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

    /* Restore RSSOwl label is clicked */
    newsLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        onOpen(bookmark, news, e);
      }
    });
  }

  private void onOpen(IBookMark bookmark, INews news, MouseEvent e) {

    /* Open Link in Browser if Modifier Key is pressed */
    if ((e.stateMask & SWT.MOD1) != 0) {
      new OpenInBrowserAction(new StructuredSelection(news)).run();
      close();
      return;
    }

    /* Otherwise open Feedview and select the News */
    IWorkbenchPage page = OwlUI.getPage();
    if (page != null) {
      Shell shell = page.getWorkbenchWindow().getShell();

      /* Restore from Tray or Minimization if required */
      if (ApplicationWorkbenchAdvisor.fPrimaryApplicationWorkbenchWindowAdvisor.isMinimizedToTray())
        ApplicationWorkbenchAdvisor.fPrimaryApplicationWorkbenchWindowAdvisor.restoreFromTray(shell);
      else if (shell.getMinimized()) {
        shell.setMinimized(false);
        shell.forceActive();
      }

      /* First try if the Bookmark is already visible */
      IEditorReference editorRef = EditorUtils.findEditor(page.getEditorReferences(), bookmark);
      if (editorRef != null) {
        IEditorPart editor = editorRef.getEditor(false);
        if (editor instanceof FeedView) {
          ((FeedView) editor).setSelection(new StructuredSelection(news));
          page.activate(editor);
        }
      }

      /* Otherwise Open */
      else {
        boolean activateEditor = OpenStrategy.activateOnOpen();
        FeedViewInput input = new FeedViewInput(bookmark, PerformAfterInputSet.selectNews(new NewsReference(news.getId())));
        try {
          OwlUI.getPage().openEditor(input, FeedView.ID, activateEditor);
        } catch (PartInitException ex) {
          Activator.getDefault().getLog().log(ex.getStatus());
        }
      }
    }

    /* Close Popup */
    close();
  }

  private void initResources() {

    /* Colors */
    OSTheme osTheme = OwlUI.getOSTheme(getParentShell().getDisplay());
    switch (osTheme) {
      case WINDOWS_BLUE:
        fPopupBorderColor = OwlUI.getColor(fResources, new RGB(125, 177, 251));
        fPopupOuterCircleColor = OwlUI.getColor(fResources, new RGB(73, 135, 234));
        break;

      case WINDOWS_SILVER:
        fPopupBorderColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        fPopupOuterCircleColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
        break;

      case WINDOWS_OLIVE:
        fPopupBorderColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
        fPopupOuterCircleColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
        break;

      case WINDOWS_CLASSIC:
        fPopupBorderColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        fPopupOuterCircleColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT);
        break;

      default:
        fPopupBorderColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
        fPopupOuterCircleColor = getParentShell().getDisplay().getSystemColor(SWT.COLOR_TITLE_BACKGROUND);
    }

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
    titleCircle.addMouseTrackListener(fMouseTrackListner);

    /* Title Label displaying RSSOwl */
    fTitleCircleLabel = new CLabel(titleCircle, SWT.NO_FOCUS);
    fTitleCircleLabel.setImage(OwlUI.getImage(fResources, "icons/product/16x16.gif"));
    fTitleCircleLabel.setText("RSSOwl");
    fTitleCircleLabel.setFont(fBoldTextFont);
    fTitleCircleLabel.setBackground(titleCircle.getBackground());
    fTitleCircleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fTitleCircleLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
    fTitleCircleLabel.addMouseTrackListener(fMouseTrackListner);

    /* CLabel to display a cross to close the popup */
    final CLabel closeButton = new CLabel(titleCircle, SWT.NO_FOCUS);
    closeButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    closeButton.setBackground(titleCircle.getBackground());
    closeButton.setImage(fCloseImageNormal);
    closeButton.setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_HAND));
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
    fInnerContentCircle.addMouseTrackListener(fMouseTrackListner);

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
    int initialHeight = getShell().computeSize(DEFAULT_WIDTH, SWT.DEFAULT).y;
    int labelHeight = fTitleCircleLabel.computeSize(DEFAULT_WIDTH, SWT.DEFAULT).y;

    return new Point(DEFAULT_WIDTH, initialHeight + fVisibleNewsCount * labelHeight);
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