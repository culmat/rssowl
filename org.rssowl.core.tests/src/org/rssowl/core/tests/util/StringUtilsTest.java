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

package org.rssowl.core.tests.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.rssowl.core.util.StringUtils;

/**
 * Tests methods in StringUtils.
 */
public class StringUtilsTest {

  /**
   * @throws Exception
   */
  @Test
  public void testStripTags() throws Exception {
    assertEquals("Foo Bar", StringUtils.stripTags("Foo Bar"));
    assertEquals("Foo  Bar", StringUtils.stripTags("Foo <br> Bar"));
    assertEquals("Foo Bar", StringUtils.stripTags("Foo Bar<br>"));
    assertEquals("Foo Bar Foo Bar", StringUtils.stripTags("Foo Bar<br> Foo Bar<br>"));
    assertEquals("Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar", StringUtils.stripTags("Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br>"));
    assertEquals("T-Systems übernimmt DVB-H-Sendebetrieb", StringUtils.stripTags("<h3 class=\"anriss\"><a href=\"/newsticker/meldung/97406\">T-Systems übernimmt DVB-H-Sendebetrieb</a></h3>"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNormalize() throws Exception {
    assertEquals("Foo Bar", StringUtils.normalizeString("Foo Bar"));
    assertEquals("FooBar", StringUtils.normalizeString("FooBar"));
    assertEquals("FooBar", StringUtils.normalizeString(" FooBar"));
    assertEquals("FooBar", StringUtils.normalizeString(" FooBar "));
    assertEquals("FooBar", StringUtils.normalizeString("  FooBar "));
    assertEquals("FooBar", StringUtils.normalizeString("  FooBar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo Bar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo\nBar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo\n\t Bar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo Bar\n  "));
  }
}