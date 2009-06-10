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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.OwlUI;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
public class NewsTableLabelProvider extends OwnerDrawLabelProvider {

  /** News Column Model to use */
  protected NewsColumnViewModel fColumnModel;

  /* Some Colors of a Label */
  private static final String LABEL_COLOR_BLACK = "0,0,0";
  private static final String LABEL_COLOR_WHITE = "255,255,255";

  /** Resource Manager to use */
  protected LocalResourceManager fResources;

  /* Date Formatter for News */
  private final DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

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

  /**
   * Creates a new instance of this LabelProvider
   *
   * @param model the column model.
   */
  public NewsTableLabelProvider(NewsColumnViewModel model) {
    fColumnModel = model;
    fResources = new LocalResourceManager(JFaceResources.getResources());
    createResources();
  }

  /*
   * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#initialize(org.eclipse.jface.viewers.ColumnViewer, org.eclipse.jface.viewers.ViewerColumn)
   */
  @Override
  protected void initialize(ColumnViewer viewer, ViewerColumn column) {
    super.initialize(viewer, column, false); //Disable Custom Ownerdrawn
  }

  /**
   * @param model
   */
  public void init(NewsColumnViewModel model) {
    fColumnModel = model;
  }

  void updateResources() {

    /* Sticky Color */
    fStickyBgColor = OwlUI.getThemeColor(OwlUI.STICKY_BG_COLOR_ID, fResources, new RGB(255, 255, 128));
  }

  private void createResources() {

    /* Colors */
    fStickyBgColor = OwlUI.getThemeColor(OwlUI.STICKY_BG_COLOR_ID, fResources, new RGB(255, 255, 128));
    fGradientFgColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_FG_COLOR);
    fGradientBgColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_BG_COLOR);
    fGradientEndColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_END_COLOR);
    fGroupFgColor = OwlUI.getColor(fResources, OwlUI.GROUP_FG_COLOR);
    fGroupBgColor = OwlUI.getColor(fResources, OwlUI.GROUP_BG_COLOR);

    /* Icons */
    fNewsUnreadIcon = OwlUI.getImage(fResources, OwlUI.NEWS_STATE_UNREAD);
    fNewsNewIcon = OwlUI.getImage(fResources, OwlUI.NEWS_STATE_NEW);
    fNewsUpdatedIcon = OwlUI.getImage(fResources, OwlUI.NEWS_STATE_UPDATED);
    fNewsReadIcon = OwlUI.getImage(fResources, OwlUI.NEWS_STATE_READ);
    fNewsStickyIcon = OwlUI.getImage(fResources, OwlUI.NEWS_PINNED);
    fNewsNonStickyIcon = OwlUI.getImage(fResources, OwlUI.NEWS_PIN);
    fGroupIcon = OwlUI.getImage(fResources, OwlUI.GROUP);

    /* Fonts */
    fBoldFont = OwlUI.getThemeFont(OwlUI.HEADLINES_FONT_ID, SWT.BOLD);
  }

  /*
   * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
   */
  @Override
  public void update(ViewerCell cell) {
    NewsColumn column = fColumnModel.getColumn(cell.getColumnIndex());

    /* Text */
    cell.setText(getColumnText(cell.getElement(), column, cell.getColumnIndex()));

    /* Image */
    cell.setImage(getColumnImage(cell.getElement(), column, cell.getColumnIndex()));

    /* Font */
    cell.setFont(getFont(cell.getElement(), cell.getColumnIndex()));

    /* Foreground */
    Color foreground = getForeground(cell.getElement(), cell.getColumnIndex());

    /* TODO This is required to invalidate + redraw the entire TableItem! */
    if (NewsTableControl.USE_CUSTOM_OWNER_DRAWN) {
      Item item = (Item) cell.getItem();
      if (item instanceof TreeItem)
        ((TreeItem) cell.getItem()).setForeground(foreground);
      else if (item instanceof TableItem)
        ((TableItem) cell.getItem()).setForeground(foreground);
    } else
      cell.setForeground(foreground);

    /* Background */
    cell.setBackground(getBackground(cell.getElement(), cell.getColumnIndex()));
  }

  /*
   * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
   */
  @Override
  public String getToolTipText(Object element) {
    INews news = (INews) element;
    FeedLinkReference feedRef = news.getFeedReference();
    IBookMark bookMark = CoreUtils.getBookMark(feedRef);

    String name = null;
    if (bookMark != null)
      name = bookMark.getName();
    else
      name = feedRef.getLinkAsText();

    if (news.getParentId() != 0) {
      INewsBin bin = DynamicDAO.load(INewsBin.class, news.getParentId());
      if (bin != null) {
        name = bin.getName() + ": " + name;
      }
    }

    return StringUtils.replaceAll(name, "&", "&&");
  }

  /**
   * @param element
   * @param column
   * @param colIndex
   * @return String
   */
  protected String getColumnText(Object element, NewsColumn column, int colIndex) {
    String text = null;

    /* Handle News */
    if (element instanceof INews) {
      INews news = (INews) element;

      switch (column) {
        case TITLE:
          text = CoreUtils.getHeadline(news);
          break;

        case DATE:
          Date date = DateUtils.getRecentDate(news);
          text = fDateFormat.format(date);
          break;

        case AUTHOR:
          IPerson author = news.getAuthor();
          if (author != null) {
            if (author.getName() != null)
              text = author.getName();
            else if (author.getEmail() != null)
              text = author.getEmail().toString();
          }
          break;

        case CATEGORY:
          List<ICategory> categories = news.getCategories();
          if (categories.size() > 0) {
            ICategory category = categories.get(0);
            if (category.getName() != null)
              text = category.getName();
            else if (category.getDomain() != null)
              text = category.getDomain();
          }
          break;

        case LABELS:
          Set<ILabel> labels = CoreUtils.getSortedLabels(news);
          if (!labels.isEmpty()) {
            StringBuilder str = new StringBuilder();
            for (ILabel label : labels) {
              str.append(label.getName()).append(", ");
            }

            str = str.delete(str.length() - 2, str.length());
            text = str.toString();
          }
          break;

        case STATUS:
          State state = news.getState();
          if (state == State.NEW)
            text = "New";
          else if (state == State.UNREAD)
            text = "Unread";
          else if (state == State.UPDATED)
            text = "Updated";
          else if (state == State.READ)
            text = "Read";

          break;
      }
    }

    /* Handle EntityGroup */
    else if (element instanceof EntityGroup && colIndex == 0)
      text = ((EntityGroup) element).getName();

    /* Make sure to normalize the Text for the Table */
    return text != null ? StringUtils.normalizeString(text) : null;
  }

  /**
   * @param element
   * @param newsColumn
   * @param colIndex
   * @return Image
   */
  protected Image getColumnImage(Object element, NewsColumn newsColumn, int colIndex) {

    /* News */
    if (element instanceof INews) {
      INews news = (INews) element;

      /* News Icon */
      if (newsColumn == NewsColumn.TITLE) {
        if (news.getState() == INews.State.UNREAD)
          return fNewsUnreadIcon;
        else if (news.getState() == INews.State.NEW)
          return fNewsNewIcon;
        else if (news.getState() == INews.State.UPDATED)
          return fNewsUpdatedIcon;
        else if (news.getState() == INews.State.READ)
          return fNewsReadIcon;
      }

      /* Feed Column */
      else if (newsColumn == NewsColumn.FEED) {
        FeedLinkReference feedRef = news.getFeedReference();
        IBookMark bookMark = CoreUtils.getBookMark(feedRef);
        if (bookMark != null) {
          ImageDescriptor favicon = OwlUI.getFavicon(bookMark);
          return OwlUI.getImage(fResources, favicon != null ? favicon : OwlUI.BOOKMARK);
        }

        return OwlUI.getImage(fResources, OwlUI.BOOKMARK);
      }

      /* Sticky State */
      else if (newsColumn == NewsColumn.STICKY) {
        if (news.isFlagged())
          return fNewsStickyIcon;

        return fNewsNonStickyIcon;
      }

      /* Attachment */
      else if (newsColumn == NewsColumn.ATTACHMENTS) {
        List<IAttachment> attachments = news.getAttachments();
        if (!attachments.isEmpty())
          return OwlUI.getImage(fResources, OwlUI.ATTACHMENT);
      }
    }

    /* EntityGroup Image */
    else if (element instanceof EntityGroup && colIndex == 0) {
      EntityGroup group = (EntityGroup) element;
      if (group.getImage() != null)
        return OwlUI.getImage(fResources, group.getImage());

      return fGroupIcon;
    }

    return null;
  }

  /**
   * @param element
   * @param columnIndex
   * @return Font
   */
  protected Font getFont(Object element, @SuppressWarnings("unused") int columnIndex) {

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

  /**
   * @param element
   * @param columnIndex
   * @return Color
   */
  protected Color getBackground(Object element, @SuppressWarnings("unused") int columnIndex) {

    /* Handle INews */
    if (element instanceof INews && ((INews) element).isFlagged())
      return fStickyBgColor;

    /* Handle EntityGroup */
    else if (element instanceof EntityGroup)
      return fGroupBgColor;

    return null;
  }

  /**
   * @param element
   * @param columnIndex
   * @return Color
   */
  protected Color getForeground(Object element, @SuppressWarnings("unused") int columnIndex) {

    /* Handle EntityGroup */
    if (element instanceof EntityGroup)
      return fGroupFgColor;

    /* Handle INews */
    else if (element instanceof INews) {
      Set<ILabel> labels = CoreUtils.getSortedLabels((INews) element);
      if (!labels.isEmpty())
        return OwlUI.getColor(fResources, labels.iterator().next());
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
  public void erase(Event event, Object element) {

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

    /* Handle selected News (Linux: Note Bug 444) */
    if (!news.isFlagged() && (event.detail & SWT.SELECTED) != 0) {

      Set<ILabel> labels = CoreUtils.getSortedLabels(news);
      /* Some conditions under which we don't override the selection color */
      if (labels.isEmpty() || !scrollable.isFocusControl())
        return;

      ILabel label = labels.iterator().next();
      if (isInvalidLabelColor(label))
        return;

      Rectangle clArea = scrollable.getClientArea();
      Rectangle itemRect = event.getBounds();

      /* Paint the selection beyond the end of last column */
      expandRegion(event, scrollable, gc, clArea);

      /* Draw Rectangle */
      Color oldBackground = gc.getBackground();
      gc.setBackground(OwlUI.getColor(fResources, label));
      gc.fillRectangle(0, itemRect.y, clArea.width, itemRect.height);
      gc.setBackground(oldBackground);
      gc.setForeground(scrollable.getDisplay().getSystemColor(SWT.COLOR_WHITE));

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

  private boolean isInvalidLabelColor(ILabel label) {
    return label.getColor().equals(LABEL_COLOR_BLACK) || label.getColor().equals(LABEL_COLOR_WHITE);
  }

  private void eraseGroup(Event event, @SuppressWarnings("unused") EntityGroup group) {
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