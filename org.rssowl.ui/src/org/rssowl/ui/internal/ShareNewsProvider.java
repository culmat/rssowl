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

package org.rssowl.ui.internal;

import org.rssowl.core.persist.INews;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;

/**
 * Instances of {@link ShareNewsProvider} are used to allow for sharing of news
 * items with services. They can be contributed using the
 * <cod>ShareNewsProvider</code> extension point.
 *
 * @author bpasero
 */
public class ShareNewsProvider {
  private static final String URL_INPUT_TOKEN = "[L]";
  private static final String TITLE_INPUT_TOKEN = "[T]";

  private final String fId;
  private final String fName;
  private final String fIconPath;
  private final String fUrl;
  private final int fOrder;
  private final int fMaxTitleLength;

  /**
   * @param id the unique id of the contributed provider.
   * @param name the name of the provider.
   * @param iconPath the path to an icon of the provider.
   * @param url the templated URL to share with.
   * @param order the order of the provider.
   * @param maxTitleLength a limit for the title.
   */
  public ShareNewsProvider(String id, String name, String iconPath, String url, String order, String maxTitleLength) {
    fId = id;
    fName = name;
    fIconPath = iconPath;
    fUrl = url;

    if (order != null)
      fOrder = Integer.parseInt(order);
    else
      fOrder = Integer.MAX_VALUE;

    if (maxTitleLength != null)
      fMaxTitleLength = Integer.parseInt(maxTitleLength);
    else
      fMaxTitleLength = Integer.MAX_VALUE;
  }

  /**
   * @return the unique id of the contributed provider.
   */
  public String getId() {
    return fId;
  }

  /**
   * @return the name of the provider.
   */
  public String getName() {
    return fName;
  }

  /**
   * @return the path to an icon of the provider.
   */
  public String getIconPath() {
    return fIconPath;
  }

  /**
   * @return the order of the provider.
   */
  public int getOrder() {
    return fOrder;
  }

  /**
   * @param news the news to share.
   * @return a link that can be used to share the link with this provider.
   */
  public String toShareUrl(INews news) {
    String link = news.getLinkAsText();
    if (!StringUtils.isSet(link))
      link = "";

    String title = CoreUtils.getHeadline(news, true);
    if (!StringUtils.isSet(title))
      title = "";

    if (title.length() > fMaxTitleLength)
      title = StringUtils.smartTrim(title, fMaxTitleLength);

    link = URIUtils.urlEncode(link);
    title = URIUtils.urlEncode(title);

    String shareUrl = fUrl;

    int linkIndex = fUrl.indexOf(URL_INPUT_TOKEN);
    int titleIndex = fUrl.indexOf(TITLE_INPUT_TOKEN);

    if (linkIndex >= 0)
      shareUrl = StringUtils.replaceAll(shareUrl, URL_INPUT_TOKEN, link);

    if (titleIndex >= 0)
      shareUrl = StringUtils.replaceAll(shareUrl, TITLE_INPUT_TOKEN, title);

    return shareUrl;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fId == null) ? 0 : fId.hashCode());
    return result;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if (obj == null)
      return false;

    if (getClass() != obj.getClass())
      return false;

    ShareNewsProvider other = (ShareNewsProvider) obj;
    if (fId == null) {
      if (other.fId != null)
        return false;
    } else if (!fId.equals(other.fId))
      return false;

    return true;
  }
}