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
package org.rssowl.core.tests.persist;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.rssowl.core.internal.persist.DefaultModelFactory;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Unit tests for INews.
 */
public class INewsTest {

  private IModelFactory fFactory = new DefaultModelFactory();

  /**
   * See bug #558 : Consider not using GUID if isPermaLink is false.
   *
   * <p>
   * Tests that we consider a Guid#isPermaLink == false in the same way we
   * consider a null Guid when calling {@link INews#isEquivalent(INews)}.
   *
   * <p>
   * Note that this should happen no matter what happens in the comparison of
   * Guid#getValue for both News.
   * </p>
   * @throws URISyntaxException
   */
  @Test
  public void testIsEquivalentWithGuidIsPermaLinkFalse() throws URISyntaxException    {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com"));

    INews news1 = fFactory.createNews(null, feed, new Date());
    String link = "www.link.com";
    news1.setGuid(fFactory.createGuid(news1, link));
    news1.getGuid().setPermaLink(false);
    news1.setLink(new URI(link));

    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setGuid(fFactory.createGuid(news2, link));
    news2.getGuid().setPermaLink(false);
    news2.setLink(new URI(link));

    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setGuid(fFactory.createGuid(news3, "http://www.anotherlink.com"));
    news3.getGuid().setPermaLink(false);
    news3.setLink(new URI(link));

    INews news4 = fFactory.createNews(null, feed, new Date());
    news4.setGuid(fFactory.createGuid(news4, link));
    news4.getGuid().setPermaLink(false);
    news4.setLink(new URI("www.anotherlink2.com"));

    assertTrue(news1.isEquivalent(news2));
    assertTrue(news1.isEquivalent(news3));
    assertFalse(news1.isEquivalent(news4));

    assertTrue(news2.isEquivalent(news3));
    assertFalse(news2.isEquivalent(news4));

    assertFalse(news3.isEquivalent(news4));
  }
}
