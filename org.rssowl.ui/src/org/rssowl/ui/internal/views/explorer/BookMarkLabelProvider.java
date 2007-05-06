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

package org.rssowl.ui.internal.views.explorer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.NewsService;
import org.rssowl.ui.internal.OwlUI;

import java.util.EnumSet;
import java.util.List;

/**
 * @author bpasero
 */
public class BookMarkLabelProvider extends CellLabelProvider {

  /* Resource Manager */
  private LocalResourceManager fResources;

  /* News Service */
  private NewsService fNewsService;

  /* Define whether to indicate *new* News in Bookmarks */
  private static final boolean INDICATE_NEW = true;

  /* Commonly used Resources */
  private Image fFolderIcon;
  private Image fFolderNewIcon;
  private Image fBookMarkErrorIcon;
  private Image fBookMarkIcon;
  private Image fSearchMarkIcon;
  private Image fGroupIcon;
  private Image fBookmarkSetIcon;
  private Color fStickyBgColor;
  private Color fGroupFgColor;
  private Font fBoldFont;
  private Font fDefaultFont;

  /** */
  public BookMarkLabelProvider() {
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fNewsService = Controller.getDefault().getNewsService();
    createResources();
  }

  private void createResources() {

    /* Images */
    fBookmarkSetIcon = OwlUI.getImage(fResources, OwlUI.BOOKMARK_SET);
    fGroupIcon = OwlUI.getImage(fResources, OwlUI.GROUP);
    fFolderIcon = OwlUI.getImage(fResources, OwlUI.FOLDER);
    fFolderNewIcon = OwlUI.getImage(fResources, OwlUI.FOLDER_NEW);
    fBookMarkErrorIcon = OwlUI.getImage(fResources, OwlUI.BOOKMARK_ERROR);
    fBookMarkIcon = OwlUI.getImage(fResources, OwlUI.BOOKMARK);
    fSearchMarkIcon = OwlUI.getImage(fResources, OwlUI.SEARCHMARK);

    /* Fonts */
    fBoldFont = OwlUI.getThemeFont(OwlUI.BKMRK_EXPLORER_FONT_ID, SWT.BOLD);
    fDefaultFont = OwlUI.getThemeFont(OwlUI.BKMRK_EXPLORER_FONT_ID, SWT.NORMAL);

    /* Colors */
    fStickyBgColor = OwlUI.getThemeColor(OwlUI.STICKY_BG_COLOR_ID, fResources, new RGB(255, 255, 128));
    fGroupFgColor = OwlUI.getColor(fResources, OwlUI.GROUP_FG_COLOR);
  }

  @Override
  public void update(ViewerCell cell) {
    Object element = cell.getElement();
    int unreadNewsCount = 0;
    int newNewsCount = 0;

    /* Create Label for a Folder */
    if (element instanceof IFolder) {
      IFolder folder = (IFolder) element;
      unreadNewsCount = getNewsCount(folder, true);
      newNewsCount = getNewsCount(folder, false);

      /* Image */
      if (folder.getParent() == null)
        cell.setImage(fBookmarkSetIcon);
      else if (newNewsCount == 0)
        cell.setImage(fFolderIcon);
      else
        cell.setImage(fFolderNewIcon);

      /* Font */
      if (unreadNewsCount > 0)
        cell.setFont(fBoldFont);
      else
        cell.setFont(fDefaultFont);

      /* Text */
      StringBuilder str = new StringBuilder(folder.getName());
      if (unreadNewsCount > 0)
        str.append(" (").append(unreadNewsCount).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
      cell.setText(str.toString());

      /* Reset Foreground */
      cell.setForeground(null);

      /* Reset Background */
      cell.setBackground(null);
    }

    /* Create Label for a BookMark */
    else if (element instanceof IBookMark) {
      IBookMark bookmark = (IBookMark) element;
      FeedLinkReference feedLinkRef = bookmark.getFeedLinkReference();
      unreadNewsCount = getUnreadNewsCount(feedLinkRef);
      int stickyNewsCount = getStickyNewsCount(feedLinkRef);
      boolean hasNew = getNewNewsCount(bookmark.getFeedLinkReference()) != 0;

      /* Font */
      if (unreadNewsCount > 0)
        cell.setFont(fBoldFont);
      else
        cell.setFont(fDefaultFont);

      /* Text */
      StringBuilder str = new StringBuilder(bookmark.getName());
      if (unreadNewsCount > 0)
        str.append(" (").append(unreadNewsCount).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
      cell.setText(str.toString());

      /* Background Color */
      if (stickyNewsCount > 0)
        cell.setBackground(fStickyBgColor);
      else
        cell.setBackground(null);

      /* Reset Foreground */
      cell.setForeground(null);

      /* Load the FavIcon */
      ImageDescriptor favicon = OwlUI.getFavicon(bookmark);

      /* Indicate Error */
      if (bookmark.isErrorLoading()) {

        /* Overlay with Error Icon if required */
        if (favicon != null) {
          Image faviconImg = OwlUI.getImage(fResources, favicon);
          DecorationOverlayIcon overlay = new DecorationOverlayIcon(faviconImg, OwlUI.getImageDescriptor("icons/ovr16/error.gif"), IDecoration.BOTTOM_RIGHT);
          cell.setImage(OwlUI.getImage(fResources, overlay));
        }

        /* Default Error Icon */
        else {
          cell.setImage(fBookMarkErrorIcon);
        }
      }

      /* Use normal Icon */
      else {
        Image icon = favicon != null ? OwlUI.getImage(fResources, favicon) : fBookMarkIcon;

        /* Overlay if News are *new* */
        if (hasNew && INDICATE_NEW) {
          DecorationOverlayIcon overlay = new DecorationOverlayIcon(icon, OwlUI.getImageDescriptor("icons/ovr16/new.gif"), IDecoration.BOTTOM_RIGHT);
          cell.setImage(OwlUI.getImage(fResources, overlay));
        }

        /* Don't overlay */
        else {
          cell.setImage(icon);
        }
      }
    }

    /* Create Label for a SearchMark */
    else if (element instanceof ISearchMark) {
      ISearchMark searchmark = (ISearchMark) element;
      unreadNewsCount = searchmark.getResultCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));
      boolean hasNew = searchmark.getResultCount(EnumSet.of(INews.State.NEW)) != 0;

      /* Image */
      Image icon = fSearchMarkIcon;

      /* Overlay if News are *new* */
      if (hasNew && INDICATE_NEW) {
        DecorationOverlayIcon overlay = new DecorationOverlayIcon(icon, OwlUI.getImageDescriptor("icons/ovr16/new.gif"), IDecoration.BOTTOM_RIGHT);
        cell.setImage(OwlUI.getImage(fResources, overlay));
      }

      /* Don't overlay */
      else {
        cell.setImage(icon);
      }

      /* Font */
      if (unreadNewsCount > 0)
        cell.setFont(fBoldFont);
      else
        cell.setFont(fDefaultFont);

      /* Text */
      StringBuilder str = new StringBuilder(searchmark.getName());
      if (unreadNewsCount > 0)
        str.append(" (").append(unreadNewsCount).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
      cell.setText(str.toString());

      /* Reset Foreground */
      cell.setForeground(null);

      /* Reset Background */
      cell.setBackground(null);
    }

    /* Create Label for EntityGroup */
    else if (element instanceof EntityGroup) {
      EntityGroup group = (EntityGroup) element;

      /* Text */
      cell.setText(group.getName());

      /* Image */
      cell.setImage(fGroupIcon);

      /* Foreground */
      cell.setForeground(fGroupFgColor);

      /* Reset Background */
      cell.setBackground(null);

      /* Font */
      cell.setFont(fBoldFont);
    }
  }

  /*
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    fResources.dispose();
  }

  /*
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
   * java.lang.String)
   */
  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  private int getNewsCount(IFolder folder, boolean unread) {
    int count = 0;

    /* Go through all Folders and Marks */
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {
      if (child instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) child;

        if (unread)
          count += getUnreadNewsCount(bookmark.getFeedLinkReference());
        else
          count += getNewNewsCount(bookmark.getFeedLinkReference());
      } else if (child instanceof IFolder)
        count += getNewsCount((IFolder) child, unread);
    }

    return count;
  }

  private int getUnreadNewsCount(FeedLinkReference feedLinkReference) {
    return fNewsService.getUnreadCount(feedLinkReference);
  }

  private int getNewNewsCount(FeedLinkReference feedLinkReference) {
    return fNewsService.getNewCount(feedLinkReference);
  }

  private int getStickyNewsCount(FeedLinkReference feedRef) {
    return fNewsService.getStickyCount(feedRef);
  }
}