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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
public class NewsTableLabelProvider extends OwnerDrawLabelProvider {
  private LocalResourceManager fResources;

  /* Date Formatter for News */
  private DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  /* Pre-Cache some Colors being used */
  private Color fStickyBgColor;
  private Color fGradientFgColor;
  private Color fGradientBgColor;
  private Color fGradientEndColor;
  private Color fGroupFgColor;
  private Color fGroupBgColor;

  /* Pre-Cache some Images being used */
  private Image fNewsUnreadIcon;
  private Image fNewsNewIcon;
  private Image fNewsUpdatedIcon;
  private Image fNewsReadIcon;
  private Image fNewsStickyIcon;
  private Image fNewsNonStickyIcon;
  private Image fGroupIcon;

  /* Pre-Cache some Fonts being used */
  private Font fBoldFont;

  /** Creates a new instance of this LabelProvider */
  public NewsTableLabelProvider() {
    fResources = new LocalResourceManager(JFaceResources.getResources());
    createResources();
  }

  private void createResources() {

    /* Colors */
    fStickyBgColor = RSSOwlUI.getColor(fResources, RSSOwlUI.STICKY_BG_COLOR);
    fGradientFgColor = RSSOwlUI.getColor(fResources, RSSOwlUI.GROUP_GRADIENT_FG_COLOR);
    fGradientBgColor = RSSOwlUI.getColor(fResources, RSSOwlUI.GROUP_GRADIENT_BG_COLOR);
    fGradientEndColor = RSSOwlUI.getColor(fResources, RSSOwlUI.GROUP_GRADIENT_END_COLOR);
    fGroupFgColor = RSSOwlUI.getColor(fResources, RSSOwlUI.GROUP_FG_COLOR);
    fGroupBgColor = RSSOwlUI.getColor(fResources, RSSOwlUI.GROUP_BG_COLOR);

    /* Icons */
    fNewsUnreadIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_STATE_UNREAD);
    fNewsNewIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_STATE_NEW);
    fNewsUpdatedIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_STATE_UPDATED);
    fNewsReadIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_STATE_READ);
    fNewsStickyIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_PINNED);
    fNewsNonStickyIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.NEWS_PIN);
    fGroupIcon = RSSOwlUI.getImage(fResources, RSSOwlUI.GROUP);

    /* Fonts */
    fBoldFont = RSSOwlUI.getThemeFont(RSSOwlUI.HEADLINES_FONT_ID, SWT.BOLD);
  }

  @Override
  public void update(ViewerCell cell) {

    /* Text */
    cell.setText(getColumnText(cell.getElement(), cell.getColumnIndex()));

    /* Image */
    cell.setImage(getColumnImage(cell.getElement(), cell.getColumnIndex()));

    /* Font */
    cell.setFont(getFont(cell.getElement(), cell.getColumnIndex()));

    /* Foreground */
    Color foreground = getForeground(cell.getElement(), cell.getColumnIndex());

    /* TODO This is required to invalidate + redraw the entire TableItem! */
    if (NewsTableControl.USE_CUSTOM_OWNER_DRAWN) {
      Item item = (Item)cell.getItem();
      if (item instanceof TreeItem)
        ((TreeItem) cell.getItem()).setForeground(foreground);
      else if (item instanceof TableItem)
        ((TableItem) cell.getItem()).setForeground(foreground);
    } else
      cell.setForeground(foreground);

    /* Background */
    cell.setBackground(getBackground(cell.getElement(), cell.getColumnIndex()));
  }

  private String getColumnText(Object element, int columnIndex) {
    String text = null;

    /* Handle News */
    if (element instanceof INews) {
      INews news = (INews) element;

      switch (columnIndex) {
        case NewsTableControl.COL_TITLE:
          text = ModelUtils.getHeadline(news);
          break;

        case NewsTableControl.COL_PUBDATE:
          Date date = DateUtils.getRecentDate(news);
          text = fDateFormat.format(date);
          break;

        case NewsTableControl.COL_AUTHOR:
          IPerson author = news.getAuthor();
          if (author != null) {
            if (author.getName() != null)
              text = author.getName();
            else if (author.getEmail() != null)
              text = author.getEmail().toString();
          }
          break;

        case NewsTableControl.COL_CATEGORY:
          List<ICategory> categories = news.getCategories();
          if (categories.size() > 0) {
            ICategory category = categories.get(0);
            if (category.getName() != null)
              text = category.getName();
            else if (category.getDomain() != null)
              text = category.getDomain();
          }
          break;
      }
    }

    /* Handle EntityGroup */
    else if (element instanceof EntityGroup && columnIndex == NewsTableControl.COL_TITLE)
      text = ((EntityGroup) element).getName();

    /* Make sure to normalize the Text for the Table */
    return text != null ? StringUtils.normalizeString(text) : null;
  }

  private Image getColumnImage(Object element, int columnIndex) {

    /* News */
    if (element instanceof INews) {
      INews news = (INews) element;

      /* News Icon */
      if (columnIndex == NewsTableControl.COL_TITLE) {
        if (news.getState() == INews.State.UNREAD)
          return fNewsUnreadIcon;
        else if (news.getState() == INews.State.NEW)
          return fNewsNewIcon;
        else if (news.getState() == INews.State.UPDATED)
          return fNewsUpdatedIcon;
        else if (news.getState() == INews.State.READ)
          return fNewsReadIcon;
      }

      /* Sticky State */
      else if (columnIndex == NewsTableControl.COL_STICKY) {
        if (news.isFlagged())
          return fNewsStickyIcon;

        return fNewsNonStickyIcon;
      }
    }

    /* EntityGroup Image */
    else if (element instanceof EntityGroup && columnIndex == NewsTableControl.COL_TITLE)
      return fGroupIcon;

    return null;
  }

  private Font getFont(Object element, @SuppressWarnings("unused")
  int columnIndex) {

    /* Use a Bold Font for Unread News */
    if (element instanceof INews) {
      INews news = (INews) element;
      INews.State state = news.getState();
      if (state == null)
        return null;

      /* Bold for New, Updated and Unread News */
      if (state == INews.State.NEW || state == INews.State.UPDATED || state == INews.State.UNREAD)
        return fBoldFont;
    }

    /* Use Bold Font for EntityGroup */
    if (element instanceof EntityGroup)
      return fBoldFont;

    return null;
  }

  private Color getBackground(Object element, @SuppressWarnings("unused")
  int columnIndex) {

    /* Handle INews */
    if (element instanceof INews && ((INews) element).isFlagged())
      return fStickyBgColor;

    /* Handle EntityGroup */
    else if (element instanceof EntityGroup)
      return fGroupBgColor;

    return null;
  }

  private Color getForeground(Object element, @SuppressWarnings("unused")
  int columnIndex) {

    /* Handle EntityGroup */
    if (element instanceof EntityGroup)
      return fGroupFgColor;

    /* Handle INews */
    else if (element instanceof INews) {
      ILabel label = ((INews) element).getLabel();
      if (label != null)
        return RSSOwlUI.getColor(fResources, label);
    }

    return null;
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

  /*
   * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#erase(org.eclipse.swt.widgets.Event,
   * java.lang.Object)
   */
  @Override
  protected void erase(Event event, Object element) {

    /* Erase News */
    if (element instanceof INews)
      eraseNews(event, (INews) element);

    /* Erase Group */
    else if (element instanceof EntityGroup)
      eraseGroup(event, (EntityGroup) element);
  }

  private void eraseNews(Event event, INews news) {
    final Scrollable scrollable = (Scrollable) event.widget;
    GC gc = event.gc;

    /* Handle selected News */
    if ((event.detail & SWT.SELECTED) != 0) {

      /* Some conditions under which we don't override the selection color */
      if (news.getLabel() == null || !scrollable.isFocusControl())
        return;

      Rectangle clArea = scrollable.getClientArea();
      Rectangle itemRect = event.getBounds();

      /* Paint the selection beyond the end of last column */
      expandRegion(event, scrollable, gc, clArea);

      /* Draw Rectangle */
      Color oldBackground = gc.getBackground();
      gc.setBackground(RSSOwlUI.getColor(fResources, news.getLabel()));
      gc.fillRectangle(0, itemRect.y, clArea.width, itemRect.height);
      gc.setBackground(oldBackground);

      /* Mark as Selected being handled */
      event.detail &= ~SWT.SELECTED;
    }

    /* Handle Non-Selected flagged News */
    else if (news.isFlagged()) {
      Rectangle clArea = scrollable.getClientArea();
      Rectangle itemRect = event.getBounds();

      /* Paint the selection beyond the end of last column */
      expandRegion(event, scrollable, gc, clArea);

      /* Draw Rectangle */
      Color oldBackground = gc.getBackground();
      gc.setBackground(fStickyBgColor);
      gc.fillRectangle(0, itemRect.y, clArea.width, itemRect.height);
      gc.setBackground(oldBackground);

      /* Mark as Background being handled */
      event.detail &= ~SWT.BACKGROUND;
    }
  }

  private void eraseGroup(Event event, @SuppressWarnings("unused")
  EntityGroup group) {
    Scrollable scrollable = (Scrollable) event.widget;
    GC gc = event.gc;

    Rectangle area = scrollable.getClientArea();
    Rectangle rect = event.getBounds();

    /* Paint the selection beyond the end of last column */
    expandRegion(event, scrollable, gc, area);

    /* Draw Gradient Rectangle */
    Color oldForeground = gc.getForeground();
    Color oldBackground = gc.getBackground();

    /* Gradient */
    gc.setForeground(fGradientFgColor);
    gc.setBackground(fGradientBgColor);
    gc.fillGradientRectangle(0, rect.y, area.width, rect.height, true);

    /* Bottom Line */
    gc.setForeground(fGradientEndColor);
    gc.drawLine(0, rect.y + rect.height - 1, area.width, rect.y + rect.height - 1);

    gc.setForeground(oldForeground);
    gc.setBackground(oldBackground);

    /* Mark as Background being handled */
    event.detail &= ~SWT.BACKGROUND;
  }

  private void expandRegion(Event event, Scrollable scrollable, GC gc, Rectangle area) {
    int columnCount;
    if (scrollable instanceof Table)
      columnCount = ((Table) scrollable).getColumnCount();
    else
      columnCount = ((Tree) scrollable).getColumnCount();

    if (event.index == columnCount - 1 || columnCount == 0) {
      int width = area.x + area.width - event.x;
      if (width > 0) {
        Region region = new Region();
        gc.getClipping(region);
        region.add(event.x, event.y, width, event.height);
        gc.setClipping(region);
        region.dispose();
      }
    }
  }

  /*
   * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#measure(org.eclipse.swt.widgets.Event,
   * java.lang.Object)
   */
  @Override
  protected void measure(Event event, Object element) {
  /* Ignore */
  }

  /*
   * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#paint(org.eclipse.swt.widgets.Event,
   * java.lang.Object)
   */
  @Override
  protected void paint(Event event, Object element) {
  /* Ignore */
  }
}