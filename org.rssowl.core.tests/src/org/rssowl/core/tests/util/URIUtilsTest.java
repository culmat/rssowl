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

package org.rssowl.core.tests.util;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.rssowl.core.util.URIUtils;

import java.net.URI;

/**
 * Tests methods in {@link URIUtils}.
 *
 * @author bpasero
 */
public class URIUtilsTest {

  /**
   * @throws Exception
   */
  @Test
  public void testLooksLikeFeedLink() throws Exception {
    assertFalse(URIUtils.looksLikeFeedLink(""));
    assertFalse(URIUtils.looksLikeFeedLink(" "));
    assertFalse(URIUtils.looksLikeFeedLink("foo bar"));
    assertFalse(URIUtils.looksLikeFeedLink("www.domain.org"));
    assertFalse(URIUtils.looksLikeFeedLink("http://www.domain.org"));

    assertTrue(URIUtils.looksLikeFeedLink("http://www.domain.de/foobar.rss"));
    assertTrue(URIUtils.looksLikeFeedLink("http://www.domain.de/foobar.rdf"));
    assertTrue(URIUtils.looksLikeFeedLink("http://www.domain.de/foobar.xml"));
    assertTrue(URIUtils.looksLikeFeedLink("http://www.domain.de/foobar.atom"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLooksLikeLink() throws Exception {
    assertFalse(URIUtils.looksLikeLink(""));
    assertFalse(URIUtils.looksLikeLink(" "));
    assertFalse(URIUtils.looksLikeLink("foo bar"));

    assertTrue(URIUtils.looksLikeLink("www.domain.org"));
    assertTrue(URIUtils.looksLikeLink("http://www.domain.org"));
    assertTrue(URIUtils.looksLikeLink("http://www.domain.de/foobar.rss"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testToFaviconUri() throws Exception {
    assertEquals("http://www.domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://www.domain.de/path/index.html"), false).toString());
    assertEquals("http://www.domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://www.domain.de/path/"), false).toString());
    assertEquals("http://www.domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://www.domain.de"), false).toString());
    assertEquals("http://domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://test.domain.de/path/index.html"), true).toString());
    assertEquals("http://domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://test.domain.de/path/"), true).toString());
    assertEquals("http://domain.de/favicon.ico", URIUtils.toFaviconUrl(new URI("http://test.domain.de"), true).toString());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetFile() throws Exception {
    assertEquals("foo", URIUtils.getFile(new URI("foo")));
    assertEquals("foo.bar", URIUtils.getFile(new URI("foo.bar")));
    assertEquals("bar", URIUtils.getFile(new URI("foo/bar")));
    assertEquals("bar", URIUtils.getFile(new URI("/foo/bar")));
    assertEquals("bar.txt", URIUtils.getFile(new URI("/foo/bar.txt")));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNormalize() throws Exception {
    assertEquals("http://www.rssowl.org", URIUtils.normalizeUri(new URI("http://www.rssowl.org/path"), false).toString());
    assertEquals("http://www.rssowl.org", URIUtils.normalizeUri(new URI("http://www.rssowl.org/path"), true).toString());
    assertEquals("http://www.rssowl.org:80", URIUtils.normalizeUri(new URI("http://www.rssowl.org:80/path"), true).toString());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testResolve() throws Exception {
    URI baseWithTrailingSlash = new URI("http://www.rssowl.org/");
    URI baseWithoutTrailingSlash = new URI("http://www.rssowl.org");

    URI base2WithTrailingSlash = new URI("http://www.rssowl.org/other/");
    URI base2WithoutTrailingSlash = new URI("http://www.rssowl.org/other");

    URI relativeWithLeadingSlash = new URI("/path/download.mp3");
    URI relativeWithoutLeadingSlash = new URI("path/download.mp3");

    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(baseWithTrailingSlash, relativeWithLeadingSlash).toString());
    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(baseWithTrailingSlash, relativeWithoutLeadingSlash).toString());

    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(baseWithoutTrailingSlash, relativeWithLeadingSlash).toString());
    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(baseWithoutTrailingSlash, relativeWithoutLeadingSlash).toString());

    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(base2WithTrailingSlash, relativeWithLeadingSlash).toString());
    assertEquals("http://www.rssowl.org/other/path/download.mp3", URIUtils.resolve(base2WithTrailingSlash, relativeWithoutLeadingSlash).toString());

    assertEquals("http://www.rssowl.org/path/download.mp3", URIUtils.resolve(base2WithoutTrailingSlash, relativeWithLeadingSlash).toString());
    assertEquals("http://www.rssowl.org/other/path/download.mp3", URIUtils.resolve(base2WithoutTrailingSlash, relativeWithoutLeadingSlash).toString());
  }
}