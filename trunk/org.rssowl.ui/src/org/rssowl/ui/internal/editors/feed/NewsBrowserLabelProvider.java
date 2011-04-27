/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

import static org.rssowl.ui.internal.ILinkHandler.HANDLER_PROTOCOL;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.ARCHIVE_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.ATTACHMENTS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.ATTACHMENT_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.COLLAPSE_GROUP_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.DELETE_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.EXPAND_NEWS_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.GROUP_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.LABELS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.RELATED_NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.SHARE_NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.TOGGLE_READ_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.TOGGLE_STICKY_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.TRANSFORM_HANDLER_ID;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.ExpandingReader;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.FolderNewsMark.FolderNewsMarkReference;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.CBrowser;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsBrowserLabelProvider extends LabelProvider {

  /* Date Formatter for News */
  private DateFormat fDateFormat = OwlUI.getLongDateFormat();

  /* Time Formatter for News */
  private DateFormat fTimeFormat = OwlUI.getShortTimeFormat();

  /* Potential Media Tags */
  private final Set<String> fMediaTags = new HashSet<String>(Arrays.asList(new String[] { "img", "applet", "embed", "area", "frame", "frameset", "iframe", "map", "object" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

  /* Windows only: Mark of the Web */
  private static final String IE_MOTW = "<!-- saved from url=(0014)about:internet -->"; //$NON-NLS-1$

  /* Dynamic HTML in Content */
  enum Dynamic {
    NEWS("newsitem"), //$NON-NLS-1$
    TITLE("title"), //$NON-NLS-1$
    SUBLINE("subline"), //$NON-NLS-1$
    TOGGLE_READ_LINK("toggleRead"), //$NON-NLS-1$
    TOGGLE_READ_IMG("toggleReadImg"), //$NON-NLS-1$
    TOGGLE_STICKY_LINK("toggleStickyLink"), //$NON-NLS-1$
    TOGGLE_STICKY_IMG("toggleStickyImg"), //$NON-NLS-1$
    TOGGLE_GROUP_LINK("toggleGroupLink"), //$NON-NLS-1$
    TOGGLE_GROUP_IMG("toggleGroupImg"), //$NON-NLS-1$
    TOGGLE_NEWS_LINK("toggleNewsLink"), //$NON-NLS-1$
    TOGGLE_NEWS_IMG("toggleNewsImg"), //$NON-NLS-1$
    LABELS_MENU_LINK("labelsMenuLink"), //$NON-NLS-1$
    ARCHIVE_LINK("archiveLink"), //$NON-NLS-1$
    SHARE_MENU_LINK("shareMenuLink"), //$NON-NLS-1$
    GROUP_MENU_LINK("groupMenuLink"), //$NON-NLS-1$
    NEWS_MENU_LINK("newsMenuLink"), //$NON-NLS-1$
    HEADER("header"), //$NON-NLS-1$
    CONTENT("content"), //$NON-NLS-1$
    FOOTER("footer"), //$NON-NLS-1$
    FIND_RELATED_MENU_LINK("findRelatedMenuLink"), //$NON-NLS-1$
    ATTACHMENTS_MENU_LINK("attachmentsMenuLink"), //$NON-NLS-1$
    ATTACHMENT_LINK("attachmentLink"), //$NON-NLS-1$
    FULL_CONTENT_LINK("fullContentLink"), //$NON-NLS-1$
    FULL_CONTENT_LINK_TEXT("fullContentLinkText"), //$NON-NLS-1$
    LABELS("labels"), //$NON-NLS-1$
    LABELS_SEPARATOR("labelsSeparator"); //$NON-NLS-1$

    private String fId;

    Dynamic(String id) {
      fId = id;
    }

    String getId(long id) {
      return fId + id;
    }

    String getId(EntityGroup group) {
      return fId + group.getId();
    }

    String getId(INews news) {
      return fId + news.getId();
    }

    String getId(NewsReference newsRef) {
      return fId + newsRef.getId();
    }
  }

  /* Colors and Fonts */
  private String fNewsFontFamily;
  private String fNormalFontCSS;
  private String fVerySmallFontCSS;
  private String fSmallFontCSS;
  private String fBiggerFontCSS;
  private String fBiggestFontCSS;
  private String fStickyBGColorCSS;
  private String fNewsListBGColorCSS;
  private String fLinkFGColorCSS;
  private boolean fIsNewsListBGColorDefined;
  private IPropertyChangeListener fPropertyChangeListener;

  /* Label Provider State */
  private final NewsBrowserViewer fViewer;
  private final boolean fIsIE;
  private boolean fStripMediaFromNews;
  private boolean fForceShowFeedInformation;
  private boolean fManageLinks;
  private boolean fShowFooter;
  private boolean fHeadlinesOnly;
  private final long fTodayInMillies;
  private final Map<String, String> fMapFeedLinkToName = new HashMap<String, String>();

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param browser
   */
  public NewsBrowserLabelProvider(CBrowser browser) {
    this(null, browser.isIE());
  }

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param viewer
   */
  public NewsBrowserLabelProvider(NewsBrowserViewer viewer) {
    this(viewer, viewer.getBrowser().isIE());
  }

  private NewsBrowserLabelProvider(NewsBrowserViewer viewer, boolean isIE) {
    fViewer = viewer;
    fIsIE = isIE;
    fManageLinks = OwlUI.useExternalBrowser();
    fShowFooter = true;
    fTodayInMillies = DateUtils.getToday().getTimeInMillis();

    createFonts();
    createColors();
    registerListeners();
  }

  /**
   * @param stripMediaFromNews <code>true</code> to strip images and other media
   * from the news and <code>false</code> otherwise.
   */
  public void setStripMediaFromNews(boolean stripMediaFromNews) {
    fStripMediaFromNews = stripMediaFromNews;
  }

  /* Removes Media Tags from the provided String if the provider is configured to do so */
  String stripMediaTagsIfNecessary(String str) {
    if (fStripMediaFromNews)
      return StringUtils.filterTags(str, fMediaTags, false);

    return str;
  }

  /**
   * @param forceShowFeedInformation if <code>true</code> will show the name of
   * a feed of a news when shown, <code>false</code> otherwise.
   */
  public void setForceShowFeedInformation(boolean forceShowFeedInformation) {
    fForceShowFeedInformation = forceShowFeedInformation;
  }

  /**
   * @param showFooter if <code>true</code> will show the footer for each news
   * and <code>false</code> otheriwse. Default is <code>true</code>.
   */
  public void setShowFooter(boolean showFooter) {
    fShowFooter = showFooter;
  }

  /**
   * @param headlinesOnly if set to <code>true</code> only show headlines of
   * news and not the content, <code>false</code> otherwise. Default is
   * <code>false</code>.
   */
  public void setHeadlinesOnly(boolean headlinesOnly) {
    fHeadlinesOnly = headlinesOnly;
  }

  /*
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    unregisterListeners();
    fMapFeedLinkToName.clear();
  }

  private void registerListeners() {

    /* Create Property Listener */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (OwlUI.NEWS_TEXT_FONT_ID.equals(property))
          createFonts();
        else if (OwlUI.STICKY_BG_COLOR_ID.equals(property) || OwlUI.LINK_FG_COLOR_ID.equals(property) || OwlUI.NEWS_LIST_BG_COLOR_ID.equals(property))
          createColors();
      }
    };

    /* Add it to listen to Theme Events */
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  private void unregisterListeners() {
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  /* Init the Theme Font (from UI Thread) */
  private void createFonts() {
    int fontHeight = 10;
    Font newsFont = OwlUI.getThemeFont(OwlUI.NEWS_TEXT_FONT_ID, SWT.NORMAL);
    FontData[] fontData = newsFont.getFontData();
    if (fontData.length > 0) {
      fNewsFontFamily = fontData[0].getName();
      fontHeight = fontData[0].getHeight();
    }

    int normal = fontHeight;
    int small = normal - 1;
    int verysmall = normal - 2;
    int bigger = normal + 1;
    int biggest = bigger + 2;

    String fontUnit = "pt"; //$NON-NLS-1$
    fNormalFontCSS = "font-size: " + normal + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fVerySmallFontCSS = "font-size: " + verysmall + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fSmallFontCSS = "font-size: " + small + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fBiggerFontCSS = "font-size: " + bigger + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fBiggestFontCSS = "font-size: " + biggest + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /* Init the Theme Color (from UI Thread) */
  private void createColors() {
    RGB stickyRgb = OwlUI.getThemeRGB(OwlUI.STICKY_BG_COLOR_ID, new RGB(255, 255, 180));
    fStickyBGColorCSS = "background-color: rgb(" + stickyRgb.red + "," + stickyRgb.green + "," + stickyRgb.blue + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    RGB linkRgb = OwlUI.getThemeRGB(OwlUI.LINK_FG_COLOR_ID, new RGB(0, 0, 153));
    fLinkFGColorCSS = "color: rgb(" + linkRgb.red + "," + linkRgb.green + "," + linkRgb.blue + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    RGB newsListRgb = OwlUI.getThemeRGB(OwlUI.NEWS_LIST_BG_COLOR_ID, new RGB(255, 255, 255));
    fNewsListBGColorCSS = "background-color: rgb(" + newsListRgb.red + "," + newsListRgb.green + "," + newsListRgb.blue + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    fIsNewsListBGColorDefined = !newsListRgb.equals(new RGB(255, 255, 255));
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  public String getText(Object element) {
    return getText(element, true, true, -1);
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @param withManagedLinks if set to <code>false</code>, the output will not
   * contain any managed links.
   * @param index the zero-based index of the element from top.
   * @return the HTML representation for the given element.
   */
  public String getText(Object element, boolean withInternalLinks, boolean withManagedLinks, int index) {

    /* Return HTML for a Group */
    if (element instanceof EntityGroup)
      return getLabel((EntityGroup) element, withInternalLinks);

    /* Return HTML for a News */
    else if (element instanceof INews)
      return getLabel((INews) element, withInternalLinks, withManagedLinks, index);

    return ""; //$NON-NLS-1$
  }

  private boolean isSingleNewsDisplayed() {
    Object input = fViewer != null ? fViewer.getInput() : null;
    return input instanceof INews;
  }

  private boolean isEntityGroupDisplayed() {
    Object input = fViewer != null ? fViewer.getInput() : null;
    return input instanceof EntityGroup;
  }

  private boolean isGroupingEnabled() {
    if (isEntityGroupDisplayed())
      return false; //Entity Group as input does not count here

    if (fViewer != null) {
      IContentProvider cp = fViewer.getContentProvider();
      if (cp instanceof NewsContentProvider)
        return ((NewsContentProvider) cp).isGroupingEnabled();
    }

    return false;
  }

  private boolean showFeedInformation() {
    if (fForceShowFeedInformation)
      return true;

    Object input = fViewer != null ? fViewer.getInput() : null;
    return input instanceof FolderNewsMarkReference || input instanceof SearchMarkReference || input instanceof NewsBinReference;
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer) throws IOException {
    writeCSS(writer, isSingleNewsDisplayed(), true);
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @param forSingleNews if <code>true</code>, the site contains a single news,
   * or <code>false</code> if it contains a collection of news.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer, boolean forSingleNews, boolean withInternalLinks) throws IOException {

    /* Open CSS */
    writer.write("<style type=\"text/css\">\n"); //$NON-NLS-1$

    /* General */
    writer.append("body { overflow: auto; margin: 0; font-family: ").append(fNewsFontFamily).append(",Verdanna,sans-serif; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("a { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("a:hover { ").append(fLinkFGColorCSS).append(" text-decoration: underline; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("a:visited { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("img { border: none; }\n"); //$NON-NLS-1$

    /* Group */
    writer.append("div.group { color: #678; ").append(fBiggestFontCSS).append(" font-weight: bold; margin: 10px 0px 5px 5px; padding-bottom: 3px; border-bottom: 2px solid #678; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.group a { color: #678; ").append(fBiggestFontCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.group a:hover { color: #678; ").append(fBiggestFontCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.group a:visited { color: #678; ").append(fBiggestFontCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("span.groupNote { ").append(fNormalFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* News Container (Single News) */
    if (forSingleNews) {
      writer.write("div.newsitemUnread { margin: 0; }\n"); //$NON-NLS-1$
      writer.write("div.newsitemRead { margin: 0; }\n"); //$NON-NLS-1$
    }

    /* News Container (Many News - Newspaper Layout) */
    else {
      if (isGroupingEnabled()) {
        writer.write("div.newsitemUnread { margin: 0px 0px 20px 10px; border-bottom: 1px solid white; }\n"); //$NON-NLS-1$
        writer.write("div.newsitemRead { margin: 0px 0px 20px 10px; border-bottom: 1px solid white; }\n"); //$NON-NLS-1$
      } else {
        writer.write("div.newsitemUnread { margin: 0px 0px 20px 0px; border-bottom: 1px solid white; }\n"); //$NON-NLS-1$
        writer.write("div.newsitemRead { margin: 0px 0px 20px 0px; border-bottom: 1px solid white; }\n"); //$NON-NLS-1$
      }
    }

    /* Header */
    if (forSingleNews) {
      writer.write("div.header { padding: 10px 10px 5px 10px; background-color: rgb(242,242,242); }\n"); //$NON-NLS-1$
      writer.append("div.headerSticky { padding: 10px 10px 5px 10px; ").append(fStickyBGColorCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    } else if (isGroupingEnabled()) {
      writer.write("div.header { padding: 10px 10px 5px 5px; border-top: 1px solid white; }\n"); //$NON-NLS-1$
      writer.append("div.headerSticky { padding: 10px 10px 5px 5px; ").append(fStickyBGColorCSS).append(" border-top: 1px dotted silver; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    } else {
      writer.write("div.header { padding: 10px 10px 5px 10px; border-top: 1px solid white; }\n"); //$NON-NLS-1$
      writer.append("div.headerSticky { padding: 10px 10px 5px 10px; ").append(fStickyBGColorCSS).append(" border-top: 1px dotted silver; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /* Content */
    writer.write("div.content { \n"); //$NON-NLS-1$
    writer.write("   padding: 15px 10px 15px 10px; border-top: dotted 1px silver; \n"); //$NON-NLS-1$
    writer.append("  background-color: #fff; clear: both; ").append(fNormalFontCSS).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("}\n"); //$NON-NLS-1$

    /* Restrict the style of embedded Paragraphs */
    writer.write("div.content p { margin-top: 0; padding-top: 0; margin-left: 0; padding-left: 0; }\n"); //$NON-NLS-1$

    /* Footer */
    if (forSingleNews) {
      writer.write("div.footer { background-color: rgb(248,248,248); padding: 3px 5px 3px 5px; line-height: 20px; border-top: dotted 1px silver; border-bottom: dotted 1px silver; clear: both; }\n"); //$NON-NLS-1$
      writer.append("div.footerSticky { ").append(fStickyBGColorCSS).append(" padding: 3px 5px 3px 5px; line-height: 20px; border-top: dotted 1px silver; border-bottom: dotted 1px silver; clear: both; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    } else {
      writer.write("div.footer { padding: 3px 5px 3px 5px; line-height: 20px; border-top: dotted 1px silver; border-bottom: 1px solid white; clear: both; }\n"); //$NON-NLS-1$
      writer.append("div.footerSticky { ").append(fStickyBGColorCSS).append(" padding: 3px 5px 3px 5px; line-height: 20px; border-top: dotted 1px silver; border-bottom: 1px dotted silver; clear: both; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
      writer.write("div.clearingFooter { clear: both; }\n"); //$NON-NLS-1$
    }

    /* Title */
    if (withInternalLinks) //Need to set width to avoid float drop bug of delete button on all OS (see Bug 1393)
      writer.append("div.title { width: 90%; float: left; padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    else
      writer.append("div.title { float: left; padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    writer.append("div.title a { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.title a.unread { font-weight: bold; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.append("div.title a:hover { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.title a:visited { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Author */
    writer.write("a.author { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:hover { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:active { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:visited { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$

    /* Comments */
    writer.write("a.comments { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:hover { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:active { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:visited { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$

    writer.write("div.title span.unread { font-weight: bold; }\n"); //$NON-NLS-1$

    /* Delete */
    writer.append("div.delete { text-align: right; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Subline */
    writer.append("div.subline { margin: 0; padding: 0; clear: left; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("table.subline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("tr.subline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("td.firstactionsubline { text-align: left; width: 20px; margin: 0; padding: 0; color: rgb(80, 80, 80); ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("td.firstactionsubline a { width: 20px; }\n"); //$NON-NLS-1$
    writer.append("td.otheractionsubline { text-align: center; width: 22px; margin: 0; padding: 0; color: rgb(80, 80, 80); ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("td.otheractionsubline a { width: 22px; }\n"); //$NON-NLS-1$
    writer.append("td.subline { margin: 0; padding: 0; color: rgb(80, 80, 80); padding-right: 5px; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("td.sublineseparator { margin: 0; padding: 0; color: rgb(140, 140, 140); padding-right: 5px; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("td.actionsublineseparator { margin: 0; padding: 0; color: rgb(140, 140, 140); padding-left: 4px; padding-right: 5px; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Date */
    writer.append("div.date { float: left; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Author */
    writer.append("div.author { text-align: right; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Footer Line */
    writer.append("div.footerline { clear: both; ").append(fVerySmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.footerline a { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.footerline a:visited { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("div.footerline a:hover { ").append(fLinkFGColorCSS).append(" text-decoration: none; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("table.footerline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("tr.footerline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("td.footerline { margin: 0; padding: 0; padding-right: 5px; ").append(fVerySmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("td.footerlineseparator { margin: 0; padding: 0; color: rgb(140, 140, 140); padding-right: 5px; ").append(fVerySmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Quotes */
    writer.write("span.quote_lvl1 { color: #660066; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl2 { color: #007777; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl3 { color: #3377ff; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl4 { color: #669966; }\n"); //$NON-NLS-1$

    writer.write("</style>\n"); //$NON-NLS-1$
  }

  private String getLabel(EntityGroup group, boolean withInternalLinks) {
    StringBuilder builder = new StringBuilder();

    String groupName = StringUtils.htmlEscape(group.getName());
    String groupColor = null;
    if (group.getColorHint() != null && !group.getColorHint().equals(new RGB(255, 255, 255)))
      groupColor = OwlUI.toString(group.getColorHint());

    /* DIV: Group */
    if (groupColor == null)
      div(builder, "group"); //$NON-NLS-1$
    else
      div(builder, "group", "border-bottom-color: rgb(" + groupColor + ");", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /* Let the group name be a link to invoke actions on all news inside and provide a toggle */
    if (withInternalLinks) {

      /* Toggle to Expand / Collapse */
      String link = HANDLER_PROTOCOL + COLLAPSE_GROUP_HANDLER_ID + "?" + group.getId(); //$NON-NLS-1$
      imageLink(builder, link, null, null, "/icons/elcl16/expanded.gif", "expanded.gif", Dynamic.TOGGLE_GROUP_LINK.getId(group), Dynamic.TOGGLE_GROUP_IMG.getId(group)); //$NON-NLS-1$ //$NON-NLS-2$

      /* Group Name as Link */
      link = HANDLER_PROTOCOL + GROUP_MENU_HANDLER_ID + "?" + group.getId(); //$NON-NLS-1$
      link(builder, link, groupName, null, Dynamic.GROUP_MENU_LINK.getId(group), groupColor);
    }

    /* Not using internal links */
    else {
      span(builder, groupName, null, groupColor);
    }

    /* Group Note (number of articles and filtered elements if any) */
    int sizeHint = group.getSizeHint();
    int sizeDiff = group.getEntities().size() - sizeHint;
    String groupNote;
    if (sizeHint == 1) {
      if (sizeDiff == 0)
        groupNote = Messages.NewsBrowserLabelProvider_ONE_ARTICLE;
      else
        groupNote = NLS.bind(Messages.NewsBrowserLabelProvider_ONE_ARTICLE_N_FILTERED, sizeDiff);
    } else {
      if (sizeDiff == 0)
        groupNote = NLS.bind(Messages.NewsBrowserLabelProvider_N_ARTICLES, sizeHint);
      else
        groupNote = NLS.bind(Messages.NewsBrowserLabelProvider_N_ARTICLES_M_FILTERED, sizeHint, sizeDiff);
    }

    span(builder, groupNote, "groupNote", null, groupColor); //$NON-NLS-1$

    /* Close: Group */
    close(builder, "div"); //$NON-NLS-1$

    return builder.toString();
  }

  private StringBuilder getBuilder(INews news, String description) {
    int capacity = 0;

    if (news.getTitle() != null)
      capacity += news.getTitle().length();

    if (description != null)
      capacity += description.length();

    return new StringBuilder(capacity);
  }

  private String getLabel(INews news, boolean withInternalLinks, boolean withManagedLinks, int index) {
    String description = null; //Fetch description lazily if only headlines shown
    if (!fHeadlinesOnly)
      description = stripMediaTagsIfNecessary(news.getDescription());

    StringBuilder builder = getBuilder(news, description);

    String newsTitle = CoreUtils.getHeadline(news, false);
    String newsLink = CoreUtils.getLink(news);
    boolean hasLink = newsLink != null;
    State state = news.getState();
    boolean isUnread = (state == State.NEW || state == State.UPDATED || state == State.UNREAD);
    Set<ILabel> labels = CoreUtils.getSortedLabels(news);
    String color = !labels.isEmpty() ? labels.iterator().next().getColor() : null;
    if ("0,0,0".equals(color) || "255,255,255".equals(color)) //Don't let black or white override link color //$NON-NLS-1$ //$NON-NLS-2$
      color = null;

    boolean hasAttachments = false;
    List<IAttachment> attachments = news.getAttachments();
    for (IAttachment attachment : attachments) {
      if (attachment.getLink() != null) {
        hasAttachments = true;
        break;
      }
    }

    /* DIV: NewsItem */
    if (index != 0 && fIsNewsListBGColorDefined && index % 2 != 0)
      div(builder, isUnread ? "newsitemUnread" : "newsitemRead", fNewsListBGColorCSS, Dynamic.NEWS.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
    else
      div(builder, isUnread ? "newsitemUnread" : "newsitemRead", Dynamic.NEWS.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

    /* DIV: NewsItem/Header */
    if (index == 0)
      div(builder, news.isFlagged() ? "headerSticky" : "header", "border-top: none;", Dynamic.HEADER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    else
      div(builder, news.isFlagged() ? "headerSticky" : "header", Dynamic.HEADER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

    /* News Title */
    {

      /* DIV: NewsItem/Header/Title */
      div(builder, "title"); //$NON-NLS-1$

      /* Triangle to expand/collapse News if headlines only */
      if (fHeadlinesOnly) {
        String link = HANDLER_PROTOCOL + EXPAND_NEWS_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
        imageLink(builder, link, null, null, "/icons/elcl16/collapsed.gif", "collapsed.gif", Dynamic.TOGGLE_NEWS_LINK.getId(news), Dynamic.TOGGLE_NEWS_IMG.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
      }

      String cssClass = isUnread ? "unread" : "read"; //$NON-NLS-1$ //$NON-NLS-2$

      /* Let click on Title expand news */
      if (fHeadlinesOnly) {
        String link = HANDLER_PROTOCOL + EXPAND_NEWS_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
        link(builder, link, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);
      }

      /* Otherwise treat normally */
      else {
        if (hasLink)
          link(builder, (fManageLinks && withManagedLinks) ? URIUtils.toManaged(newsLink) : newsLink, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);
        else
          span(builder, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);
      }

      /* Close: NewsItem/Header/Title */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* Delete */
    if (withInternalLinks) {

      /* DIV: NewsItem/Header/Delete */
      div(builder, "delete"); //$NON-NLS-1$

      String link = HANDLER_PROTOCOL + DELETE_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_DELETE, Messages.NewsBrowserLabelProvider_DELETE, "/icons/elcl16/remove_small.gif", "remove_small.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$

      /* DIV: NewsItem/Header/Delete */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* DIV: NewsItem/Header/Subline */
    if (fHeadlinesOnly) //Hidden initially in headlines mode
      div(builder, "subline", "display: none;", Dynamic.SUBLINE.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
    else
      div(builder, "subline", Dynamic.SUBLINE.getId(news)); //$NON-NLS-1$
    builder.append("<table class=\"subline\">"); //$NON-NLS-1$
    builder.append("<tr class=\"subline\">"); //$NON-NLS-1$

    /* Actions */
    if (withInternalLinks) {

      /* Toggle Read */
      builder.append("<td class=\"firstactionsubline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + TOGGLE_READ_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      String text = (news.getState() == INews.State.READ) ? Messages.NewsBrowserLabelProvider_MARK_UNREAD : Messages.NewsBrowserLabelProvider_MARK_READ;
      imageLink(builder, link, text, text, "/icons/elcl16/mark_read_light.gif", "mark_read_light.gif", Dynamic.TOGGLE_READ_LINK.getId(news), Dynamic.TOGGLE_READ_IMG.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Toggle Sticky */
      builder.append("<td class=\"otheractionsubline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + TOGGLE_STICKY_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_STICKY, Messages.NewsBrowserLabelProvider_STICKY, news.isFlagged() ? "/icons/obj16/news_pinned_light.gif" : "/icons/obj16/news_pin_light.gif", news.isFlagged() ? "news_pinned_light.gif" : "news_pin_light.gif", Dynamic.TOGGLE_STICKY_LINK.getId(news), Dynamic.TOGGLE_STICKY_IMG.getId(news)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
      builder.append("</td>"); //$NON-NLS-1$

      /* Assign Labels */
      builder.append("<td class=\"otheractionsubline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + LABELS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_ASSIGN_LABELS, Messages.NewsBrowserLabelProvider_LABEL, "/icons/elcl16/labels_light.gif", "labels_light.gif", Dynamic.LABELS_MENU_LINK.getId(news), null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Archive News */
      builder.append("<td class=\"otheractionsubline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + ARCHIVE_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_ARCHIVE_NEWS, Messages.NewsBrowserLabelProvider_ARCHIVE, "/icons/etool16/archive_light.gif", "archive_light.gif", Dynamic.ARCHIVE_LINK.getId(news), null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Share News Context Menu */
      builder.append("<td class=\"otheractionsubline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + SHARE_NEWS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_SHARE_NEWS, Messages.NewsBrowserLabelProvider_SHARE, "/icons/elcl16/share_light.gif", "share_light.gif", Dynamic.SHARE_MENU_LINK.getId(news), null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* News Context Menu */
      builder.append("<td class=\"otheractionsubline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + NEWS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_MENU, Messages.NewsBrowserLabelProvider_MENU, "/icons/obj16/menu_light.gif", "menu_light.gif", Dynamic.NEWS_MENU_LINK.getId(news), null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"actionsublineseparator\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Date */
    builder.append("<td class=\"subline\">"); //$NON-NLS-1$

    Date newsDate = DateUtils.getRecentDate(news);
    if (DateUtils.isAfterIncludingToday(newsDate, fTodayInMillies))
      builder.append(fTimeFormat.format(newsDate));
    else
      builder.append(fDateFormat.format(newsDate));

    builder.append("</td>"); //$NON-NLS-1$

    /* Author */
    IPerson author = news.getAuthor();
    if (author != null) {
      builder.append("<td class=\"sublineseparator\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String name = author.getName();
      String email = (author.getEmail() != null) ? author.getEmail().toASCIIString() : null;
      if (email != null && !email.contains("mail:")) //$NON-NLS-1$
        email = "mailto:" + email; //$NON-NLS-1$

      /* Use name as email if valid */
      if (email == null && name.contains("@") && !name.contains(" ")) //$NON-NLS-1$ //$NON-NLS-2$
        email = name;

      if (StringUtils.isSet(name) && email != null)
        link(builder, email, NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(name)), "author"); //$NON-NLS-1$
      else if (StringUtils.isSet(name))
        builder.append(NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(name)));
      else if (email != null)
        link(builder, email, NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(email)), "author"); //$NON-NLS-1$
      else
        builder.append(Messages.NewsBrowserLabelProvider_UNKNOWN);

      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Feed Information */
    if (showFeedInformation()) {

      /* Retrieve Name */
      String feedLinkAsText = news.getFeedLinkAsText();
      String feedName = fMapFeedLinkToName.get(feedLinkAsText);
      if (feedName == null) {
        IBookMark bm = CoreUtils.getBookMark(news.getFeedReference());
        if (bm != null) {
          feedName = StringUtils.htmlEscape(bm.getName());
          fMapFeedLinkToName.put(feedLinkAsText, feedName);
        }
      }

      /* Show Name if Provided */
      if (StringUtils.isSet(feedName)) {
        builder.append("<td class=\"sublineseparator\">"); //$NON-NLS-1$
        builder.append("|"); //$NON-NLS-1$
        builder.append("</td>"); //$NON-NLS-1$

        builder.append("<td class=\"subline\">"); //$NON-NLS-1$
        builder.append(feedName);
        builder.append("</td>"); //$NON-NLS-1$
      }
    }

    /* Comments */
    if (StringUtils.isSet(news.getComments()) && news.getComments().trim().length() > 0 && URIUtils.looksLikeLink(news.getComments())) {
      builder.append("<td class=\"sublineseparator\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$

      String comments = news.getComments();
      imageLink(builder, comments, Messages.NewsBrowserLabelProvider_READ_COMMENTS, Messages.NewsBrowserLabelProvider_COMMENTS, "/icons/obj16/comments_light.gif", "comments_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$

      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Attachments Menu */
    if (hasAttachments) {
      builder.append("<td class=\"sublineseparator\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + ATTACHMENTS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_ATTACHMENTS, Messages.NewsBrowserLabelProvider_ATTACHMENTS, "/icons/obj16/attachment_light.gif", "attachment_light.gif", Dynamic.ATTACHMENTS_MENU_LINK.getId(news), null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Labels Separator  */
    if (labels.isEmpty())
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"sublineseparator\" style=\"display: none;\">"); //$NON-NLS-1$ //$NON-NLS-2$
    else
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"sublineseparator\">"); //$NON-NLS-1$ //$NON-NLS-2$
    builder.append("|"); //$NON-NLS-1$
    builder.append("</td>"); //$NON-NLS-1$

    /* Labels */
    builder.append("<td id=\"").append(Dynamic.LABELS.getId(news)).append("\" class=\"subline\">"); //$NON-NLS-1$ //$NON-NLS-2$

    if (!labels.isEmpty())
      builder.append(Messages.NewsBrowserLabelProvider_LABELS).append(" "); //$NON-NLS-1$

    /* Append Labels to Header */
    int c = 0;
    for (ILabel label : labels) {
      c++;
      if (c < labels.size())
        span(builder, StringUtils.htmlEscape(label.getName()) + ", ", null, label.getColor()); //$NON-NLS-1$
      else
        span(builder, StringUtils.htmlEscape(label.getName()), null, label.getColor());
    }

    builder.append("</td>"); //$NON-NLS-1$

    /* Close: NewsItem/Header/Actions */
    builder.append("</tr>"); //$NON-NLS-1$
    builder.append("</table>"); //$NON-NLS-1$
    close(builder, "div"); //$NON-NLS-1$

    /* Close: NewsItem/Header */
    close(builder, "div"); //$NON-NLS-1$

    /* News Content */
    {

      /* DIV: NewsItem/Content */
      String extraCSS = ""; //$NON-NLS-1$
      if (index != 0 && fIsNewsListBGColorDefined && index % 2 != 0)
        extraCSS = fNewsListBGColorCSS;

      if (fHeadlinesOnly) //Hidden initially in headlines mode
        extraCSS += " display: none;"; //$NON-NLS-1$

      div(builder, "content", extraCSS, Dynamic.CONTENT.getId(news)); //$NON-NLS-1$

      /* Content is provided and should be displayed */
      if (!fHeadlinesOnly) {
        if (StringUtils.isSet(description) && description != null && !description.equals(news.getTitle()))
          builder.append(description);

        /* Content is not provided */
        else {

          /* Inform the user */
          builder.append(Messages.NewsBrowserLabelProvider_NO_CONTENT);

          /* Provide a link to attempt to download the news content and show it */
          if (withInternalLinks && hasLink) {
            builder.append(" "); //$NON-NLS-1$
            String link = HANDLER_PROTOCOL + TRANSFORM_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
            link(builder, link, Messages.NewsBrowserLabelProvider_ATTEMPT_DOWNLOAD_AND_DISPLAY, null);
          }
        }
      }

      /* Close: NewsItem/Content */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* News Footer */
    if (withInternalLinks && fShowFooter) {

      /* DIV: NewsItem/Footer */
      if (fHeadlinesOnly) //Hidden initially in headlines mode
        div(builder, news.isFlagged() ? "footerSticky" : "footer", "display: none;", Dynamic.FOOTER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      else
        div(builder, news.isFlagged() ? "footerSticky" : "footer", Dynamic.FOOTER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

      /* DIV: NewsItem/Footer/Footerline */
      div(builder, "footerline"); //$NON-NLS-1$
      builder.append("<table class=\"footerline\">"); //$NON-NLS-1$
      builder.append("<tr class=\"footerline\">"); //$NON-NLS-1$

      /* Related News Menu */
      builder.append("<td class=\"footerline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + RELATED_NEWS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_RELATED_NEWS, Messages.NewsBrowserLabelProvider_SEARCH_FOR_RELATED_NEWS, null, "/icons/obj16/mono_search.gif", "mono_search.gif", Dynamic.FIND_RELATED_MENU_LINK.getId(news), null, null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Transform News */
      if (hasLink) {
        builder.append("<td class=\"footerlineseparator\">"); //$NON-NLS-1$
        builder.append("|"); //$NON-NLS-1$
        builder.append("</td>"); //$NON-NLS-1$

        builder.append("<td class=\"footerline\">"); //$NON-NLS-1$
        link = HANDLER_PROTOCOL + TRANSFORM_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
        imageLink(builder, link, Messages.NewsBrowserLabelProvider_FULL_CONTENT, Messages.NewsBrowserLabelProvider_DISPLAY_FULL_CONTENT, null, "/icons/obj16/mono_transform.gif", "mono_transform.gif", Dynamic.FULL_CONTENT_LINK.getId(news), null, Dynamic.FULL_CONTENT_LINK_TEXT.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
        builder.append("</td>"); //$NON-NLS-1$
      }

      /* Attachments */
      if (attachments.size() != 0) {
        builder.append("<td class=\"footerlineseparator\">"); //$NON-NLS-1$
        builder.append("|"); //$NON-NLS-1$
        builder.append("</td>"); //$NON-NLS-1$

        for (IAttachment attachment : attachments) {
          if (attachment.getLink() != null) {
            URI attachmentLink = attachment.getLink();
            String name = URIUtils.getFile(attachmentLink, OwlUI.getExtensionForMime(attachment.getType()));
            if (!StringUtils.isSet(name))
              name = attachmentLink.toASCIIString();

            String size = OwlUI.getSize(attachment.getLength());
            if (size != null)
              name = NLS.bind(Messages.NewsBrowserLabelProvider_NAME_SIZE, StringUtils.htmlEscape(name), size);
            else
              name = StringUtils.htmlEscape(name);

            builder.append("<td class=\"footerline\">"); //$NON-NLS-1$
            link = HANDLER_PROTOCOL + ATTACHMENT_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
            imageLink(builder, link, name, null, null, "/icons/obj16/mono_attachment.gif", "mono_attachment.gif", Dynamic.ATTACHMENT_LINK.getId(news), null, null); //$NON-NLS-1$ //$NON-NLS-2$
            builder.append("</td>"); //$NON-NLS-1$
          }
        }
      }

      /* Source */
      ISource source = news.getSource();
      if (source != null && source.getLink() != null) {
        link = source.getLink().toASCIIString();
        String name = source.getName();
        if (StringUtils.isSet(link)) {
          builder.append("<td class=\"footerlineseparator\">"); //$NON-NLS-1$
          builder.append("|"); //$NON-NLS-1$
          builder.append("</td>"); //$NON-NLS-1$

          if (StringUtils.isSet(name))
            name = StringUtils.htmlEscape(name);
          else
            name = StringUtils.htmlEscape(link);

          builder.append("<td class=\"footerline\">"); //$NON-NLS-1$
          imageLink(builder, link, name, null, null, "/icons/obj16/mono_source.gif", "mono_source.gif", null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
          builder.append("</td>"); //$NON-NLS-1$
        }
      }

      /* Close: NewsItem/Footer/Footerline */
      builder.append("</tr>"); //$NON-NLS-1$
      builder.append("</table>"); //$NON-NLS-1$
      close(builder, "div"); //$NON-NLS-1$

      /* Close: NewsItem/Footer */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* Even though no footer wanted, need to have a div with clear:both to avoid hanging images from content */
    else {
      div(builder, "clearingFooter"); //$NON-NLS-1$
      close(builder, "div"); //$NON-NLS-1$
    }

    /* Close: NewsItem */
    close(builder, "div"); //$NON-NLS-1$

    String result = builder.toString();

    /* Highlight Support */
    if (fViewer != null) {
      Collection<String> wordsToHighlight = fViewer.getHighlightedWords();
      if (!wordsToHighlight.isEmpty()) {
        StringBuilder highlightedResult = new StringBuilder(result.length());

        RGB searchRGB = OwlUI.getThemeRGB(OwlUI.SEARCH_HIGHLIGHT_BG_COLOR_ID, new RGB(255, 255, 0));
        String preHighlight = "<span style=\"background-color:rgb(" + OwlUI.toString(searchRGB) + ");\">"; //$NON-NLS-1$ //$NON-NLS-2$
        String postHighlight = "</span>"; //$NON-NLS-1$

        ExpandingReader resultHighlightReader = new ExpandingReader(new StringReader(result), wordsToHighlight, preHighlight, postHighlight, true);

        int len = 0;
        char[] buf = new char[1000];
        try {
          while ((len = resultHighlightReader.read(buf)) != -1)
            highlightedResult.append(buf, 0, len);

          return highlightedResult.toString();
        } catch (IOException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    }

    return result;
  }

  private void div(StringBuilder builder, String cssClass) {
    builder.append("<div class=\"").append(cssClass).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void div(StringBuilder builder, String cssClass, String id) {
    builder.append("<div id=\"").append(id).append("\" class=\"").append(cssClass).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  private void div(StringBuilder builder, String cssClass, String extraCSS, String id) {
    builder.append("<div id=\"").append(id).append("\" class=\"").append(cssClass).append("\" style=\"").append(extraCSS).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  private void close(StringBuilder builder, String tag) {
    builder.append("</").append(tag).append(">\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void link(StringBuilder builder, String link, String content, String cssClass) {
    link(builder, link, content, cssClass, null);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String color) {
    link(builder, link, content, cssClass, null, color);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String id, String color) {
    builder.append("<a href=\"").append(link).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (cssClass != null)
      builder.append(" class=\"").append(cssClass).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (id != null)
      builder.append(" id=\"").append(id).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append(">").append(content).append("</a>"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void imageLink(StringBuilder builder, String link, String tooltip, String alt, String imgPath, String imgName, String linkId, String imageId) {
    imageLink(builder, link, null, tooltip, alt, imgPath, imgName, linkId, imageId, null);
  }

  private void imageLink(StringBuilder builder, String link, String text, String tooltip, String alt, String imgPath, String imgName, String linkId, String imageId, String textId) {
    builder.append("<a"); //$NON-NLS-1$

    if (linkId != null)
      builder.append(" id=\"").append(linkId).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (tooltip != null)
      builder.append(" title=\"").append(tooltip).append("\" href=\"").append(link).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    else
      builder.append(" href=\"").append(link).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append("<img"); //$NON-NLS-1$

    if (imageId != null)
      builder.append(" id=\"").append(imageId).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    String imageUri;
    if (fIsIE)
      imageUri = OwlUI.getImageUri(imgPath, imgName);
    else
      imageUri = ApplicationServer.getDefault().toResourceUrl(imgPath);

    if (alt == null)
      alt = text;

    if (alt != null)
      builder.append(" alt=\"").append(alt).append("\" border=\"0\" src=\"").append(imageUri).append("\" />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    else
      builder.append(" border=\"0\" src=\"").append(imageUri).append("\" />"); //$NON-NLS-1$ //$NON-NLS-2$

    if (text != null) {
      if (textId != null)
        builder.append("<span id=\"").append(textId).append("\" style=\"padding-left:3px;\">").append(text).append("</span>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      else
        builder.append("<span style=\"padding-left:3px;\">").append(text).append("</span>"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    builder.append("</a>"); //$NON-NLS-1$
  }

  private void span(StringBuilder builder, String content, String cssClass, String color) {
    span(builder, content, cssClass, null, color);
  }

  private void span(StringBuilder builder, String content, String cssClass, String id, String color) {
    if (cssClass != null)
      builder.append("<span class=\"").append(cssClass).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
    else
      builder.append("<span"); //$NON-NLS-1$

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (id != null)
      builder.append(" id=\"").append(id).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append(">").append(content).append("</span>\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Renders the provided list of elements in HTML for reading.
   *
   * @param elements the elements to render as HTML.
   * @param base a URI that should be used as base URI for the HTML document.
   * @param withManagedLinks if set to <code>false</code>, the output will not
   * contain any managed links.
   * @return a HTML document with the elements provided rendered properly for
   * reading.
   */
  public String render(Object[] elements, URI base, boolean withManagedLinks) {

    /* Store existing settings to restore later */
    boolean stripMediaFromNews = fStripMediaFromNews;
    boolean showFooter = fShowFooter;
    boolean headlinesOnly = fHeadlinesOnly;
    boolean showFeedInformation = fForceShowFeedInformation;

    fStripMediaFromNews = false;
    fShowFooter = false;
    fHeadlinesOnly = false;
    fForceShowFeedInformation = true;
    try {
      return internalRender(elements, base, withManagedLinks);
    } finally {
      fStripMediaFromNews = stripMediaFromNews;
      fShowFooter = showFooter;
      fHeadlinesOnly = headlinesOnly;
      fForceShowFeedInformation = showFeedInformation;
    }
  }

  private String internalRender(Object[] elements, URI base, boolean withManagedLinks) {

    /* Start HTML */
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"); //$NON-NLS-1$

    /* Windows only: Mark of the Web */
    if (Application.IS_WINDOWS) {
      html.append(IE_MOTW);
      html.append("\n"); //$NON-NLS-1$
    }

    /* Head */
    html.append("<html>\n  <head>\n"); //$NON-NLS-1$

    /* Append Base URI if available */
    if (base != null) {
      html.append("  <base href=\""); //$NON-NLS-1$
      html.append(base);
      html.append("\">"); //$NON-NLS-1$
    }

    /* Meta */
    html.append("\n  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"); //$NON-NLS-1$

    /* CSS */
    try {
      StringWriter writer = new StringWriter();
      writeCSS(writer, elements.length == 1, false);
      html.append(writer.toString());
    } catch (IOException e) {
      /* Will Never Happen */
    }

    /* Open Body */
    html.append("  </head>\n  <body id=\"owlbody\">\n"); //$NON-NLS-1$

    /* Write News */
    for (int i = 0; i < elements.length; i++) {
      if (elements[i] instanceof INews)
        html.append(getText(elements[i], false, withManagedLinks, i));
    }

    /* End HTML */
    html.append("\n  </body>\n</html>"); //$NON-NLS-1$

    return html.toString();
  }
}