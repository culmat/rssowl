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

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.model.types.IAttachment;
import org.rssowl.core.model.types.ICategory;
import org.rssowl.core.model.types.ILabel;
import org.rssowl.core.model.types.INews;
import org.rssowl.core.model.types.IPerson;
import org.rssowl.core.model.types.ISource;
import org.rssowl.core.model.types.INews.State;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;

/**
 * @author bpasero
 */
public class NewsBrowserLabelProvider extends LabelProvider {

  /* Date Formatter for News */
  private DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);

  private String fNewsFontFamily;
  private String normalFontCSS;
  private String smallFontCSS;
  private String biggerFontCSS;
  private String biggestFontCSS;
  private IPropertyChangeListener fPropertyChangeListener;

  /** Creates a new Browser LabelProvider for News */
  public NewsBrowserLabelProvider() {
    createFonts();
    registerListeners();
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
    FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
    if (fontRegistry != null) {
      fPropertyChangeListener = new IPropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
          String property = event.getProperty();
          if (RSSOwlUI.NEWS_TEXT_FONT_ID.equals(property))
            createFonts();
        }
      };

      fontRegistry.addListener(fPropertyChangeListener);
    }
  }

  private void unregisterListeners() {
    FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
    if (fontRegistry != null && fPropertyChangeListener != null)
      fontRegistry.removeListener(fPropertyChangeListener);
  }

  /* Init the Theme Font (from UI Thread) */
  private void createFonts() {
    int fontHeight = 10;
    Font newsFont = RSSOwlUI.getThemeFont(RSSOwlUI.NEWS_TEXT_FONT_ID, SWT.NORMAL);
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
    normalFontCSS = "font-size: " + normal + fontUnit + ";";
    smallFontCSS = "font-size: " + small + fontUnit + ";";
    biggerFontCSS = "font-size: " + bigger + fontUnit + ";";
    biggestFontCSS = "font-size: " + biggest + fontUnit + ";";
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  @SuppressWarnings("nls")
  public String getText(Object element) {

    /* Return HTML for a Group */
    if (element instanceof EntityGroup)
      return getLabel((EntityGroup) element);

    /* Return HTML for a News */
    else if (element instanceof INews)
      return getLabel((INews) element);

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

    /* Group */
    writer.append("div.group { color: #678; ").append(biggestFontCSS).append(" font-weight: bold; padding: 0 15px 5px 15px; }\n");

    /* Main DIV per Item */
    writer.write("div.newsitem { margin: 10px 10px 30px 10px; border: dotted 1px silver; }\n");

    /* Main DIV Item Areas */
    writer.write("div.header { padding: 10px; background-color: #eee; }\n");
    writer.write("div.content { \n");
    writer.write("   padding: 15px 10px 15px 10px; border-top: dotted 1px silver; \n");
    writer.append("  background-color: #fff; clear: both; ").append(normalFontCSS).append("\n");
    writer.write("}\n");
    writer.write("div.footer { background-color: rgb(248,248,248); padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; }\n");

    /* Restrict the style of embedded Paragraphs */
    writer.write("div.content p { margin-top: 0; padding-top: 0; margin-left: 0; padding-left: 0; }\n");

    /* Title */
    writer.append("div.title { padding-bottom: 6px; ").append(biggerFontCSS).append(" }\n");

    writer.write("div.title a { color: #009; text-decoration: none; }\n");
    writer.write("div.title a.unread { font-weight: bold; }\n");
    writer.write("div.title a.readsticky { background-color: rgb(255,255,128); }\n");
    writer.write("div.title a.unreadsticky { font-weight: bold; background-color: rgb(255,255,128); }\n");
    writer.write("div.title a:hover { color: #009; text-decoration: underline; }\n");
    writer.write("div.title a:visited { color: #009; text-decoration: underline; }\n");

    writer.write("div.title span.unread { font-weight: bold; }\n");
    writer.write("div.title span.readsticky { background-color: rgb(255,255,128); }\n");
    writer.write("div.title span.unreadsticky { font-weight: bold; background-color: rgb(255,255,128); }\n");

    /* Date */
    writer.append("div.date { float: left; ").append(smallFontCSS).append(" }\n");

    /* Author */
    writer.append("div.author { text-align: right; ").append(smallFontCSS).append(" }\n");

    /* Attachments */
    writer.append("div.attachments { clear: both; ").append(smallFontCSS).append(" }\n");
    writer.write("div.attachments span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.attachments a { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:hover { text-decoration: underline; }\n");

    /* Label */
    writer.append("div.label { clear: both; ").append(smallFontCSS).append(" }\n");
    writer.write("div.label span.label {float: left; padding-right: 5px; }\n");

    /* Categories */
    writer.append("div.categories { clear: both; ").append(smallFontCSS).append(" }\n");
    writer.write("div.categories span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.categories a { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:hover { text-decoration: underline; }\n");

    /* Source */
    writer.append("div.source { clear: both; ").append(smallFontCSS).append(" }\n");
    writer.write("div.source span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.source a { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:hover { text-decoration: underline; }\n");

    /* Comments */
    writer.append("div.comments { clear: both; ").append(smallFontCSS).append(" }\n");
    writer.write("div.comments span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.comments a { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:hover { text-decoration: underline; }\n");

    /* Search Related */
    writer.append("div.searchrelated { clear: both; ").append(smallFontCSS).append(" }\n");
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

    builder.append(group.getName());

    /* Close: Group */
    close(builder, "div");

    return builder.toString();
  }

  private StringBuilder getBuilder(INews news) {
    int capacity = 0;

    if (news.getTitle() != null)
      capacity += news.getTitle().length();

    if (news.getDescription() != null)
      capacity += news.getDescription().length();

    return new StringBuilder(capacity);
  }

  private String getLabel(INews news) {
    StringBuilder builder = getBuilder(news);
    StringBuilder search = new StringBuilder();

    String newsTitle = ModelUtils.getHeadline(news);
    boolean hasLink = news.getLink() != null;
    State state = news.getState();
    boolean isUnread = (state == State.NEW || state == State.UPDATED || state == State.UNREAD);
    String color = (news.getLabel() != null) ? news.getLabel().getColor() : null;

    /* DIV: NewsItem */
    div(builder, "newsitem");

    /* DIV: NewsItem/Header */
    div(builder, "header");

    /* News Title */
    {

      /* DIV: NewsItem/Header/Title */
      div(builder, "title");

      String cssClass = isUnread ? "unread" : "read";
      if (news.isFlagged())
        cssClass = cssClass + "sticky";

      /* Link */
      if (hasLink)
        link(builder, news.getLink().toASCIIString(), newsTitle, cssClass, color);

      /* Normal */
      else
        span(builder, newsTitle, cssClass, color);

      /* Close: NewsItem/Header/Title */
      close(builder, "div");
    }

    /* News Date */
    {

      /* DIV: NewsItem/Header/Date */
      div(builder, "date");

      builder.append(fDateFormat.format(DateUtils.getRecentDate(news)));

      /* Close: NewsItem/Header/Date */
      close(builder, "div");
    }

    /* News Author */
    {
      IPerson author = news.getAuthor();

      /* DIV: NewsItem/Header/Author */
      div(builder, "author");

      if (author != null) {
        String name = author.getName();
        String email = (author.getEmail() != null) ? author.getEmail().toASCIIString() : null;
        if (email != null && !email.contains("mail:"))
          email = "mailto:" + email;

        /* Use name as email if valid */
        if (email == null && name.contains("@") && !name.contains(" "))
          email = name;

        if (StringUtils.isSet(name) && email != null)
          link(builder, email, name, "author");
        else if (StringUtils.isSet(name))
          builder.append(name);
        else if (email != null)
          link(builder, email, email, "author");
        else
          builder.append("&nbsp;");

        /* Add to Search */
        String value = StringUtils.isSet(name) ? name : email;
        if (StringUtils.isSet(value)) {
          String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.AUTHOR_HANDLER_ID + "?" + URIUtils.urlEncode(value);
          link(search, link, value, "searchrelated");
          search.append(", ");
        }
      } else
        builder.append("&nbsp;");

      /* Close: NewsItem/Header/Author */
      close(builder, "div");
    }

    /* Close: NewsItem/Header */
    close(builder, "div");

    /* News Content */
    {

      /* DIV: NewsItem/Content */
      div(builder, "content");

      builder.append(StringUtils.isSet(news.getDescription()) ? news.getDescription() : "This article does not provide any content.");

      /* Close: NewsItem/Content */
      close(builder, "div");
    }

    /* News Footer */
    {
      StringBuilder footer = new StringBuilder();
      boolean hasFooter = false;

      /* DIV: NewsItem/Footer */
      div(footer, "footer");

      /* Attachments */
      if (news.getAttachments().size() != 0) {
        hasFooter = true;

        /* DIV: NewsItem/Footer/Attachments */
        div(footer, "attachments");

        /* Label */
        span(footer, news.getAttachments().size() == 1 ? "Attachment:" : "Attachments:", "label");

        /* For each Attachment */
        boolean strip = false;
        for (IAttachment attachment : news.getAttachments()) {
          if (attachment.getLink() != null) {
            strip = true;
            URI link = attachment.getLink();
            String name = URIUtils.getFile(link);
            if (!StringUtils.isSet(name))
              name = link.toASCIIString();

            //TODO Consider Attachment length and type
            link(footer, link.toASCIIString(), name, "attachment");
            footer.append(", ");
          }
        }

        if (strip)
          footer.delete(footer.length() - 2, footer.length());

        /* Close: NewsItem/Footer/Attachments */
        close(footer, "div");
      }

      /* Label */
      ILabel label = news.getLabel();
      if (news.getLabel() != null) {
        hasFooter = true;
        String name = label.getName();

        /* DIV: NewsItem/Footer/Label */
        div(footer, "label");

        /* Label */
        span(footer, "Label:", "label");

        /* Append to Footer */
        span(footer, name, "label", color);

        /* Close: NewsItem/Footer/Label */
        close(footer, "div");

        /* Add to Search */
        if (StringUtils.isSet(name)) {
          String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.LABEL_HANDLER_ID + "?" + URIUtils.urlEncode(name);
          link(search, link, name, "searchrelated");
          search.append(", ");
        }
      }

      /* Categories */
      if (news.getCategories().size() > 0) {
        StringBuilder categories = new StringBuilder();
        boolean hasCategories = false;

        /* DIV: NewsItem/Footer/Categories */
        div(categories, "categories");

        /* Label */
        span(categories, news.getCategories().size() == 1 ? "Category:" : "Categories:", "label");

        /* For each Category */
        for (ICategory category : news.getCategories()) {
          String name = category.getName();
          String domain = category.getDomain();

          /* As Link */
          if (URIUtils.looksLikeLink(domain) && StringUtils.isSet(name)) {
            link(categories, domain, name, "category");
            hasCategories = true;
          }

          /* As Text */
          else if (StringUtils.isSet(name)) {
            categories.append(name);
            hasCategories = true;
          }

          /* Separate with colon */
          categories.append(", ");

          /* Add to Search */
          if (StringUtils.isSet(name)) {
            String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.CATEGORY_HANDLER_ID + "?" + URIUtils.urlEncode(name);
            link(search, link, name, "searchrelated");
            search.append(", ");
          }
        }

        if (hasCategories)
          categories.delete(categories.length() - 2, categories.length());

        /* Close: NewsItem/Footer/Categories */
        close(categories, "div");

        /* Append categories if provided */
        if (hasCategories) {
          hasFooter = true;
          footer.append(categories);
        }
      }

      /* Source */
      ISource source = news.getSource();
      if (source != null) {
        hasFooter = true;
        String link = (source.getLink() != null) ? source.getLink().toASCIIString() : null;
        String name = source.getName();

        /* DIV: NewsItem/Footer/Source */
        div(footer, "source");

        /* Label */
        span(footer, "Source:", "label");

        if (StringUtils.isSet(name) && link != null)
          link(footer, link, name, "source");
        else if (link != null)
          link(footer, link, link, "source");
        else if (StringUtils.isSet(name))
          footer.append(name);

        /* Close: NewsItem/Footer/Source */
        close(footer, "div");
      }

      /* Comments */
      if (StringUtils.isSet(news.getComments()) && news.getComments().trim().length() > 0) {
        hasFooter = true;
        String comments = news.getComments();

        /* DIV: NewsItem/Footer/Comments */
        div(footer, "comments");

        /* Label */
        span(footer, "Comments:", "label");

        if (URIUtils.looksLikeLink(comments))
          link(footer, comments, "Read", "comments");
        else
          footer.append(comments);

        /* Close: NewsItem/Footer/Comments */
        close(footer, "div");
      }

      /* Find related News */
      if (search.length() > 0) {
        hasFooter = true;
        search.delete(search.length() - 2, search.length());

        /* DIV: NewsItem/Footer/SearchRelated */
        div(footer, "searchrelated");

        /* Label */
        span(footer, "Search related News:", "label");

        /* Append to Footer */
        footer.append(search);

        /* Close: NewsItem/Footer/SearchRelated */
        close(footer, "div");
      }

      /* Close: NewsItem/Footer */
      close(footer, "div");

      /* Append if provided */
      if (hasFooter)
        builder.append(footer);
    }

    /* Close: NewsItem */
    close(builder, "div");

    return builder.toString();
  }

  private void div(StringBuilder builder, String cssClass) {
    builder.append("<div class=\"").append(cssClass).append("\">\n");
  }

  private void close(StringBuilder builder, String tag) {
    builder.append("</").append(tag).append(">\n");
  }

  private void link(StringBuilder builder, String link, String content, String cssClass) {
    link(builder, link, content, cssClass, null);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String color) {
    builder.append("<a href=\"").append(link).append("\" class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    builder.append(">").append(content).append("</a>");
  }

  private void span(StringBuilder builder, String content, String cssClass) {
    span(builder, content, cssClass, null);
  }

  private void span(StringBuilder builder, String content, String cssClass, String color) {
    builder.append("<span class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    builder.append(">").append(content).append("</span>\n");
  }
}