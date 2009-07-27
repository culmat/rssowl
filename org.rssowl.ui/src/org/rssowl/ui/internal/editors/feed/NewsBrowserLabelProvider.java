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

import static org.rssowl.ui.internal.ILinkHandler.HANDLER_PROTOCOL;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.DELETE_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.LABELS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.SHARE_NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.TOGGLE_READ_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.TOGGLE_STICKY_HANDLER_ID;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ExpandingReader;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsBrowserLabelProvider extends LabelProvider {

  /* Date Formatter for News */
  private DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);

  /* Potential Media Tags */
  private final Set<String> fMediaTags = new HashSet<String>(Arrays.asList(new String[] { "img", "applet", "embed", "area", "frame", "frameset", "iframe", "map", "object" }));

  /* Dynamic HTML in Content */
  enum Dynamic {
    NEWS("newsitem"), TITLE("title"), TOGGLE_READ("toggleRead"), HEADER("header"), FOOTER("footer"), TOGGLE_STICKY("toggleSticky"), LABELS("labels"), LABELS_SEPARATOR("labelsSeparator");

    private String fId;

    Dynamic(String id) {
      fId = id;
    }

    String getId(INews news) {
      return fId + news.getId();
    }
  }

  private String fNewsFontFamily;
  private String fNormalFontCSS;
  private String fSmallFontCSS;
  private String fBiggerFontCSS;
  private String fBiggestFontCSS;
  private String fStickyBGColorCSS;
  private IPropertyChangeListener fPropertyChangeListener;
  private final boolean fIsIE;
  private final NewsBrowserViewer fViewer;
  private boolean fStripMediaFromNews;

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param viewer
   */
  public NewsBrowserLabelProvider(NewsBrowserViewer viewer) {
    fViewer = viewer;
    fIsIE = fViewer.getBrowser().isIE();
    createFonts();
    createColors();
    registerListeners();
  }

  /**
   * @param stripMediaFromNews <code>true</code> to strip images and other media
   * from the news and <code>false</code> otherwise.
   */
  void setStripMediaFromNews(boolean stripMediaFromNews) {
    fStripMediaFromNews = stripMediaFromNews;
  }

  /*
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    unregisterListeners();
  }

  private void registerListeners() {

    /* Create Property Listener */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (OwlUI.NEWS_TEXT_FONT_ID.equals(property))
          createFonts();
        else if (OwlUI.STICKY_BG_COLOR_ID.equals(property))
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
    int bigger = normal + 1;
    int biggest = bigger + 6;

    String fontUnit = "pt";
    fNormalFontCSS = "font-size: " + normal + fontUnit + ";";
    fSmallFontCSS = "font-size: " + small + fontUnit + ";";
    fBiggerFontCSS = "font-size: " + bigger + fontUnit + ";";
    fBiggestFontCSS = "font-size: " + biggest + fontUnit + ";";
  }

  /* Init the Theme Color (from UI Thread) */
  private void createColors() {
    RGB stickyRgb = OwlUI.getThemeRGB(OwlUI.STICKY_BG_COLOR_ID, new RGB(255, 255, 180));
    fStickyBGColorCSS = "background-color: rgb(" + stickyRgb.red + "," + stickyRgb.green + "," + stickyRgb.blue + ");";
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  @SuppressWarnings("nls")
  public String getText(Object element) {
    return getText(element, true);
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @return the HTML representation for the given element.
   */
  @SuppressWarnings("nls")
  public String getText(Object element, boolean withInternalLinks) {

    /* Return HTML for a Group */
    if (element instanceof EntityGroup)
      return getLabel((EntityGroup) element);

    /* Return HTML for a News */
    else if (element instanceof INews)
      return getLabel((INews) element, withInternalLinks);

    return ""; //$NON-NLS-1$
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer) throws IOException {

    /* Open CSS */
    writer.write("<style type=\"text/css\">\n");

    /* General */
    writer.append("body { overflow: auto; margin: 0 0 10px 0; font-family: ").append(fNewsFontFamily).append(",Verdanna,sans-serif; }\n");
    writer.write("a { color: #009; text-decoration: none; }\n");
    writer.write("a:hover { color: #009; text-decoration: underline; }\n");
    writer.write("a:visited { color: #009; text-decoration: none; }\n");
    writer.write("img { border: none; }\n");
    writer.write("div.hidden { display: none; }\n");

    /* Group */
    writer.append("div.group { color: #678; ").append(fBiggestFontCSS).append(" font-weight: bold; padding: 5px 5px 0px 15px; }\n");

    /* Main DIV per Item */
    writer.write("div.newsitemUnread { margin: 10px 10px 30px 10px; border: dotted 1px silver; }\n");
    writer.write("div.newsitemRead { margin: 10px 10px 30px 10px; border: dotted 1px silver; }\n");

    /* Main DIV Item Areas */
    writer.write("div.header { padding: 10px 10px 5px 10px; background-color: #eee; }\n");
    writer.append("div.headerSticky { padding: 10px 10px 5px 10px; ").append(fStickyBGColorCSS).append(" }\n");
    writer.write("div.content { \n");
    writer.write("   padding: 15px 10px 15px 10px; border-top: dotted 1px silver; \n");
    writer.append("  background-color: #fff; clear: both; ").append(fNormalFontCSS).append("\n");
    writer.write("}\n");
    writer.write("div.footer { background-color: rgb(248,248,248); padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; }\n");
    writer.append("div.footerSticky { ").append(fStickyBGColorCSS).append(" padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; }\n");

    /* Restrict the style of embedded Paragraphs */
    writer.write("div.content p { margin-top: 0; padding-top: 0; margin-left: 0; padding-left: 0; }\n");

    /* Title */
    writer.append("div.title { float: left; padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n");

    writer.write("div.title a { color: #009; text-decoration: none; }\n");
    writer.write("div.title a.unread { font-weight: bold; text-decoration: none; }\n");
    writer.write("div.title a:hover { color: #009; text-decoration: none; }\n");
    writer.write("div.title a:visited { color: #009; text-decoration: none; }\n");

    writer.write("a.comments { color: rgb(80,80,80); text-decoration: none; }\n");
    writer.write("a.comments:hover { color: rgb(80,80,80); text-decoration: none; }\n");
    writer.write("a.comments:active { color: rgb(80,80,80); text-decoration: none; }\n");
    writer.write("a.comments:visited { color: rgb(80,80,80); text-decoration: none; }\n");

    writer.write("div.title span.unread { font-weight: bold; }\n");

    /* Delete */
    writer.append("div.delete { text-align: right; ").append(fSmallFontCSS).append(" }\n");

    /* Subline */
    writer.append("div.subline { margin: 0; padding: 0; clear: left; ").append(fSmallFontCSS).append(" }\n");
    writer.append("table.subline { margin: 0; padding: 0; }\n");
    writer.append("tr.subline { margin: 0; padding: 0; }\n");
    writer.append("td.subline { margin: 0; padding: 0; color: rgb(80, 80, 80); padding-right: 8px; ").append(fSmallFontCSS).append(" }\n");

    /* Date */
    writer.append("div.date { float: left; ").append(fSmallFontCSS).append(" }\n");

    /* Author */
    writer.append("div.author { text-align: right; ").append(fSmallFontCSS).append(" }\n");

    /* Attachments */
    writer.append("div.attachments { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.attachments span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.attachments a { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:hover { text-decoration: underline; }\n");

    /* Categories */
    writer.append("div.categories { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.categories span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.categories a { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:hover { text-decoration: underline; }\n");

    /* Source */
    writer.append("div.source { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.source span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.source a { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:hover { text-decoration: underline; }\n");

    /* Comments */
    writer.append("div.comments { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.comments span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.comments a { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:hover { text-decoration: underline; }\n");

    /* Search Related */
    writer.append("div.searchrelated { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.searchrelated span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.searchrelated a { color: #009; text-decoration: none; }\n");
    writer.write("div.searchrelated a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.searchrelated a:hover { text-decoration: underline; }\n");

    /* Quotes */
    writer.write("span.quote_lvl1 { color: #660066; }\n");
    writer.write("span.quote_lvl2 { color: #007777; }\n");
    writer.write("span.quote_lvl3 { color: #3377ff; }\n");
    writer.write("span.quote_lvl4 { color: #669966; }\n");

    writer.write("</style>\n");
  }

  private String getLabel(EntityGroup group) {
    StringBuilder builder = new StringBuilder();

    /* DIV: Group */
    div(builder, "group");

    builder.append(StringUtils.htmlEscape(group.getName()));

    /* Close: Group */
    close(builder, "div");

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

  private String getLabel(INews news, boolean withInternalLinks) {
    String description = news.getDescription();
    if (fStripMediaFromNews)
      description = StringUtils.filterTags(description, fMediaTags, false);
    StringBuilder builder = getBuilder(news, description);
    StringBuilder search = new StringBuilder();

    String newsTitle = CoreUtils.getHeadline(news, false);
    String newsLink = CoreUtils.getLink(news);
    boolean hasLink = newsLink != null;
    State state = news.getState();
    boolean isUnread = (state == State.NEW || state == State.UPDATED || state == State.UNREAD);
    Set<ILabel> labels = CoreUtils.getSortedLabels(news);
    String color = !labels.isEmpty() ? labels.iterator().next().getColor() : null;
    if ("0,0,0".equals(color)) //Don't let black override link color
      color = null;

    boolean hasAttachments = false;
    if (!news.getAttachments().isEmpty()) {
      URI attachmentLink = news.getAttachments().get(0).getLink();
      if (attachmentLink != null)
        hasAttachments = URIUtils.looksLikeLink(attachmentLink.toString());
    }

    /* DIV: NewsItem */
    div(builder, isUnread ? "newsitemUnread" : "newsitemRead", Dynamic.NEWS.getId(news));

    /* DIV: NewsItem/Header */
    div(builder, news.isFlagged() ? "headerSticky" : "header", Dynamic.HEADER.getId(news));

    /* News Title */
    {

      /* DIV: NewsItem/Header/Title */
      div(builder, "title");

      String cssClass = isUnread ? "unread" : "read";

      /* Link */
      if (hasLink)
        link(builder, newsLink, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);

      /* Normal */
      else
        span(builder, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);

      /* Close: NewsItem/Header/Title */
      close(builder, "div");
    }

    /* Delete */
    if (withInternalLinks) {

      /* DIV: NewsItem/Header/Delete */
      div(builder, "delete");

      String link = HANDLER_PROTOCOL + DELETE_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, "Delete", "Delete", "/icons/elcl16/remove_light.gif", "remove_light.gif", null, null);

      /* DIV: NewsItem/Header/Delete */
      close(builder, "div");
    }

    /* DIV: NewsItem/Header/Subline */
    div(builder, "subline");
    builder.append("<table class=\"subline\">");
    builder.append("<tr class=\"subline\">");

    /* Actions */
    if (withInternalLinks) {

      /* Toggle Read */
      builder.append("<td class=\"subline\">");
      String link = HANDLER_PROTOCOL + TOGGLE_READ_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, news.getState() == INews.State.READ ? "Mark Unread" : "Mark Read", "Toggle Read", "/icons/elcl16/mark_read_light.gif", "mark_read_light.gif", Dynamic.TOGGLE_READ.getId(news), null);
      builder.append("</td>");

      /* Toggle Sticky */
      builder.append("<td class=\"subline\">");
      link = HANDLER_PROTOCOL + TOGGLE_STICKY_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, "Sticky", "Sticky", news.isFlagged() ? "/icons/obj16/news_pinned_light.gif" : "/icons/obj16/news_pin_light.gif", news.isFlagged() ? "news_pinned_light.gif" : "news_pin_light.gif", null, Dynamic.TOGGLE_STICKY.getId(news));
      builder.append("</td>");

      /* Assign Labels */
      builder.append("<td class=\"subline\">");
      link = HANDLER_PROTOCOL + LABELS_MENU_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, "Assign Labels", "Label", "/icons/elcl16/labels_light.gif", "labels_light.gif", null, null);
      builder.append("</td>");

      /* Share News Context Menu */
      builder.append("<td class=\"subline\">");
      link = HANDLER_PROTOCOL + SHARE_NEWS_MENU_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, "Share News", "Share", "/icons/elcl16/share_light.gif", "share_light.gif", null, null);
      builder.append("</td>");

      /* News Context Menu */
      builder.append("<td class=\"subline\">");
      link = HANDLER_PROTOCOL + NEWS_MENU_HANDLER_ID + "?" + news.getId();
      imageLink(builder, link, "Menu", "Menu", "/icons/obj16/menu_light.gif", "menu_light.gif", null, null);
      builder.append("</td>");

      builder.append("<td class=\"subline\">");
      builder.append("|");
      builder.append("</td>");
    }

    /* Date */
    builder.append("<td class=\"subline\">");
    builder.append(fDateFormat.format(DateUtils.getRecentDate(news)));
    builder.append("</td>");

    /* Author */
    IPerson author = news.getAuthor();
    if (author != null) {
      builder.append("<td class=\"subline\">");
      builder.append("|");
      builder.append("</td>");

      builder.append("<td class=\"subline\">By ");
      String name = author.getName();
      String email = (author.getEmail() != null) ? author.getEmail().toASCIIString() : null;
      if (email != null && !email.contains("mail:"))
        email = "mailto:" + email;

      /* Use name as email if valid */
      if (email == null && name.contains("@") && !name.contains(" "))
        email = name;

      if (StringUtils.isSet(name) && email != null)
        link(builder, email, StringUtils.htmlEscape(name), "author");
      else if (StringUtils.isSet(name))
        builder.append(StringUtils.htmlEscape(name));
      else if (email != null)
        link(builder, email, StringUtils.htmlEscape(email), "author");
      else
        builder.append("Unknown");

      /* Add to Search */
      String value = StringUtils.isSet(name) ? name : email;
      if (StringUtils.isSet(value)) {
        String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.AUTHOR_HANDLER_ID + "?" + URIUtils.urlEncode(value);
        link(search, link, StringUtils.htmlEscape(value), "searchrelated");
        search.append(", ");
      }
      builder.append("</td>");
    }

    /* Comments */
    if (StringUtils.isSet(news.getComments()) && news.getComments().trim().length() > 0 && URIUtils.looksLikeLink(news.getComments())) {
      builder.append("<td class=\"subline\">");
      builder.append("|");
      builder.append("</td>");

      builder.append("<td class=\"subline\">");

      String comments = news.getComments();
      imageLink(builder, comments, "Read Comments", "Comments", "/icons/obj16/comments_light.gif", "comments_light.gif", null, null);

      builder.append("</td>");
    }

    /* Go to Attachments */
    if (hasAttachments) {
      builder.append("<td class=\"subline\">");
      builder.append("|");
      builder.append("</td>");

      builder.append("<td class=\"subline\">");
      IAttachment attachment = news.getAttachments().get(0);
      String link = attachment.getLink().toASCIIString();
      String name = URIUtils.getFile(attachment.getLink());
      imageLink(builder, link, StringUtils.isSet(name) ? StringUtils.htmlEscape(name) : "Attachment", "Attachment", "/icons/obj16/attachment_light.gif", "attachment_light.gif", null, null);
      builder.append("</td>");
    }

    /* Labels Separator  */
    if (labels.isEmpty())
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"subline\" style=\"display: none;\">");
    else
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"subline\">");
    builder.append("|");
    builder.append("</td>");

    /* Labels */
    builder.append("<td id=\"").append(Dynamic.LABELS.getId(news)).append("\" class=\"subline\">");

    if (!labels.isEmpty())
      builder.append("Labels: ");

    /* Append Labels to Footer */
    int c = 0;
    for (ILabel label : labels) {
      c++;
      if (c < labels.size())
        span(builder, StringUtils.htmlEscape(label.getName()) + ", ", null, label.getColor());
      else
        span(builder, StringUtils.htmlEscape(label.getName()), null, label.getColor());
    }

    builder.append("</td>");

    /* Add to Search */
    for (ILabel label : labels) {
      String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.LABEL_HANDLER_ID + "?" + URIUtils.urlEncode(label.getName());
      link(search, link, StringUtils.htmlEscape(label.getName()), "searchrelated");
      search.append(", ");
    }

    /* Close: NewsItem/Header/Actions */
    builder.append("</tr>");
    builder.append("</table>");
    close(builder, "div");

    /* Close: NewsItem/Header */
    close(builder, "div");

    /* News Content */
    {

      /* DIV: NewsItem/Content */
      div(builder, "content");

      if (StringUtils.isSet(description) && !description.equals(news.getTitle()))
        builder.append(description);
      else {
        builder.append("This article does not provide any content.");

        if (hasLink) {
          builder.append(" Click ");
          link(builder, newsLink, "here", null);
          builder.append(" to open the article in the browser.");
        }
      }

      /* Close: NewsItem/Content */
      close(builder, "div");
    }

    /* News Footer */
    {
      StringBuilder footer = new StringBuilder();

      /* DIV: NewsItem/Footer */
      div(footer, news.isFlagged() ? "footerSticky" : "footer", Dynamic.FOOTER.getId(news));

      /* Attachments */
      List<IAttachment> attachments = news.getAttachments();
      if (attachments.size() != 0) {

        /* DIV: NewsItem/Footer/Attachments */
        div(footer, "attachments");

        /* Label */
        span(footer, attachments.size() == 1 ? "Attachment:" : "Attachments:", "label");

        /* For each Attachment */
        boolean strip = false;
        for (IAttachment attachment : attachments) {
          if (attachment.getLink() != null) {
            strip = true;
            URI link = attachment.getLink();
            String name = URIUtils.getFile(link);
            if (!StringUtils.isSet(name))
              name = link.toASCIIString();

            //TODO Consider Attachment length and type
            link(footer, link.toASCIIString(), StringUtils.htmlEscape(name), "attachment");
            footer.append(", ");
          }
        }

        if (strip)
          footer.delete(footer.length() - 2, footer.length());

        /* Close: NewsItem/Footer/Attachments */
        close(footer, "div");
      }

      /* Categories */
      List<ICategory> categories = news.getCategories();
      if (categories.size() > 0) {
        StringBuilder categoriesText = new StringBuilder();
        boolean hasCategories = false;

        /* DIV: NewsItem/Footer/Categories */
        div(categoriesText, "categories");

        /* Label */
        span(categoriesText, categories.size() == 1 ? "Category:" : "Categories:", "label");

        /* For each Category */
        for (ICategory category : categories) {
          String name = category.getName();
          String domain = category.getDomain();

          /* As Link */
          if (URIUtils.looksLikeLink(domain) && StringUtils.isSet(name)) {
            link(categoriesText, domain, StringUtils.htmlEscape(name), "category");
            hasCategories = true;
          }

          /* As Text */
          else if (StringUtils.isSet(name)) {
            categoriesText.append(StringUtils.htmlEscape(name));
            hasCategories = true;
          }

          /* Separate with colon */
          categoriesText.append(", ");

          /* Add to Search */
          if (StringUtils.isSet(name)) {
            String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.CATEGORY_HANDLER_ID + "?" + URIUtils.urlEncode(name);
            link(search, link, StringUtils.htmlEscape(name), "searchrelated");
            search.append(", ");
          }
        }

        if (hasCategories)
          categoriesText.delete(categoriesText.length() - 2, categoriesText.length());

        /* Close: NewsItem/Footer/Categories */
        close(categoriesText, "div");

        /* Append categories if provided */
        if (hasCategories)
          footer.append(categoriesText);
      }

      /* Source */
      ISource source = news.getSource();
      if (source != null) {
        String link = (source.getLink() != null) ? source.getLink().toASCIIString() : null;
        String name = source.getName();

        /* DIV: NewsItem/Footer/Source */
        div(footer, "source");

        /* Label */
        span(footer, "Source:", "label");

        if (StringUtils.isSet(name) && link != null)
          link(footer, link, StringUtils.htmlEscape(name), "source");
        else if (link != null)
          link(footer, link, StringUtils.htmlEscape(link), "source");
        else if (StringUtils.isSet(name))
          footer.append(StringUtils.htmlEscape(name));

        /* Close: NewsItem/Footer/Source */
        close(footer, "div");
      }

      /* Find related News */
      if (search.length() > 0) {
        search.delete(search.length() - 2, search.length());

        /* DIV: NewsItem/Footer/SearchRelated */
        div(footer, "searchrelated");

        /* Label */
        if (withInternalLinks)
          span(footer, "Search related News:", "label");

        /* Append to Footer */
        if (withInternalLinks)
          footer.append(search);

        /* Close: NewsItem/Footer/SearchRelated */
        close(footer, "div");
      }

      /* Close: NewsItem/Footer */
      close(footer, "div");

      /* Append */
      builder.append(footer);
    }

    /* Close: NewsItem */
    close(builder, "div");

    String result = builder.toString();

    /* Highlight Support */
    Collection<String> wordsToHighlight = fViewer.getHighlightedWords();
    if (!wordsToHighlight.isEmpty()) {
      StringBuilder highlightedResult = new StringBuilder(result.length());

      RGB searchRGB = OwlUI.getThemeRGB(OwlUI.SEARCH_HIGHLIGHT_BG_COLOR_ID, new RGB(255, 255, 0));
      String preHighlight = "<span style=\"background-color:rgb(" + OwlUI.toString(searchRGB) + ");\">";
      String postHighlight = "</span>";

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

    return result;
  }

  private void div(StringBuilder builder, String cssClass) {
    builder.append("<div class=\"").append(cssClass).append("\">\n");
  }

  private void div(StringBuilder builder, String cssClass, String id) {
    builder.append("<div id=\"").append(id).append("\" class=\"").append(cssClass).append("\">\n");
  }

  private void close(StringBuilder builder, String tag) {
    builder.append("</").append(tag).append(">\n");
  }

  private void link(StringBuilder builder, String link, String content, String cssClass) {
    link(builder, link, content, cssClass, null);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String color) {
    link(builder, link, content, cssClass, null, color);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String id, String color) {
    builder.append("<a href=\"").append(link).append("\"");

    if (cssClass != null)
      builder.append(" class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    if (id != null)
      builder.append(" id=\"").append(id).append("\"");

    builder.append(">").append(content).append("</a>");
  }

  private void imageLink(StringBuilder builder, String link, String tooltip, String alt, String imgPath, String imgName, String linkId, String imageId) {
    builder.append("<a");

    if (linkId != null)
      builder.append(" id=\"").append(linkId).append("\"");

    builder.append(" title=\"").append(tooltip).append("\" href=\"").append(link).append("\">");
    builder.append("<img");

    if (imageId != null)
      builder.append(" id=\"").append(imageId).append("\"");

    String imageUri;
    if (fIsIE)
      imageUri = OwlUI.getImageUri(imgPath, imgName);
    else
      imageUri = ApplicationServer.getDefault().toResourceUrl(imgPath);

    builder.append(" alt=\"").append(alt).append("\" border=\"0\" src=\"").append(imageUri).append("\" />");
    builder.append("</a>");
  }

  private void span(StringBuilder builder, String content, String cssClass) {
    span(builder, content, cssClass, null);
  }

  private void span(StringBuilder builder, String content, String cssClass, String color) {
    span(builder, content, cssClass, null, color);
  }

  private void span(StringBuilder builder, String content, String cssClass, String id, String color) {
    builder.append("<span class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    if (id != null)
      builder.append(" id=\"").append(id).append("\"");

    builder.append(">").append(content).append("</span>\n");
  }
}