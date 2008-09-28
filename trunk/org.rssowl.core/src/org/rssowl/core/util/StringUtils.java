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

package org.rssowl.core.util;

import org.apache.lucene.analysis.StopAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility Class for working with <code>Strings</code>.
 *
 * @author bpasero
 */
public class StringUtils {
  private static final String EMPTY_STRING = ""; //$NON-NLS-1$

  /**
   * An array containing some common English words that are not usually useful
   * for searching.
   */
  public static final String[] ENGLISH_STOP_WORDS = StopAnalyzer.ENGLISH_STOP_WORDS;

  /* This utility class constructor is hidden */
  private StringUtils() {
  // Protect default constructor
  }

  /**
   * If <code>string</code> is <code>null</code>, returns <code>null</code>.
   * Otherwise, returns the result of {@link String#trim()}.
   *
   * @param string String to be trimmed or <code>null</code>.
   * @return <code>null</code> or trimmed String.
   */
  public static final String safeTrim(String string) {
    return string == null ? null : string.trim();
  }

  /**
   * Returns TRUE in case the given String has a value that is not "" or
   * <code>NULL</code>.
   *
   * @param str The String to check
   * @return boolean TRUE in case the String has an value not "" or
   * <code>NULL</code>.
   */
  public static boolean isSet(String str) {
    return (str != null && str.length() > 0);
  }

  /**
   * This returns a new string with all surrounding whitespace removed and
   * internal whitespace normalized to a single space. If only whitespace
   * exists, the empty string is returned.
   * <p>
   * Per XML 1.0 Production 3 whitespace includes: #x20, #x9, #xD, #xA
   * </p>
   * <p>
   * See <code>org.jdom.Text</code>
   * </p>
   *
   * @param str string to be normalized.
   * @return normalized string or empty string
   */
  public static String normalizeString(String str) {
    if (str == null)
      return EMPTY_STRING;

    char[] n = new char[str.length()];
    boolean white = true;
    int pos = 0;
    for (int i = 0, c = str.length(); i < c; ++i) {
      char element = str.charAt(i);
      if (" \t\n\r".indexOf(element) != -1) { //$NON-NLS-1$
        if (!white) {
          n[pos++] = ' ';
          white = true;
        }
      } else {
        n[pos++] = element;
        white = false;
      }
    }
    if (white && pos > 0)
      pos--;

    return new String(n, 0, pos);
  }

  /**
   * Trim the given String to the given Limit. Make it human readable, such as
   * it is tried to trim the text after a whitespace, in order to keep entire
   * words.
   *
   * @param str The String to Trim
   * @param limit The max. number of characters
   * @return String The human readable trimmed String
   */
  public static String smartTrim(String str, int limit) {

    /* String does not contain a whitespace or is small */
    if (str.indexOf(' ') == -1 || str.length() < limit)
      return str;

    /* Substring to Limit */
    str = str.substring(0, limit);

    /* Cut after a whitespace */
    for (int a = limit - 1; a >= 0; a--)
      if (str.charAt(a) == ' ')
        return str.substring(0, a) + "..."; //$NON-NLS-1$

    return str;
  }

  /**
   * Remove HTML tags from the given String and replace Entities with their
   * corresponding values.
   *
   * @param str The String to remove the Tags from
   * @return Returns a String that is no longer containing any HTML or Entities.
   */
  public static String stripTags(String str) {

    /* Check String first */
    if (!StringUtils.isSet(str))
      return str;

    int length = 0;
    char[] result = new char[str.length()];
    HTMLStripReader stripReader = new HTMLStripReader(new StringReader(str));
    try {

      length += stripReader.read(result);

      if (stripReader.read() != -1)
        throw new IllegalStateException("stripReader blocks before filling the result array");

    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        stripReader.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return length > 0 ? new String(result, 0, length) : "";
  }

  /**
   * Checks whether the given String is of the Format "R,G,B" with each of the
   * components being an parseable Integer.
   *
   * @param rgb The String to check for a Valid RGB Value.
   * @return <code>TRUE</code> if the given String is a valid RGB Value.
   */
  public static boolean isValidRGB(String rgb) {
    if (rgb == null)
      return true;

    String split[] = rgb.split(","); //$NON-NLS-1$
    if (split.length != 3)
      return false;

    try {
      Integer.parseInt(split[0]);
      Integer.parseInt(split[1]);
      Integer.parseInt(split[2]);
    } catch (NumberFormatException e) {
      return false;
    }

    return true;
  }

  /**
   * This method does exactly the same as String.replaceAll() with the
   * difference that no regular expressions are used to perform the replacement.
   *
   * @param str The source String to search and replace
   * @param search The search term that should get replaced
   * @param replace The value that replaces the search term
   * @return String The new String with all replaced search terms
   */
  public static String replaceAll(String str, String search, String replace) {
    int start = 0;
    int pos;
    StringBuilder result = null;

    while ((pos = str.indexOf(search, start)) >= 0) {
      if (result == null)
        result = new StringBuilder(str.length());
      result.append(str.substring(start, pos));
      result.append(replace);
      start = pos + search.length();
    }

    if (result != null)
      result.append(str.substring(start));

    return result != null ? result.toString() : str;
  }

  /**
   * Convert a String to int and return <code>-1</code> in case the input String
   * is not a number.
   *
   * @param str The String to convert.
   * @return int The converted integer or <code>-1</code> in case the input
   * String is not a number.
   */
  public static int stringToInt(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Tokenizes the given String at a whitespace character, but keeps phrases
   * surrounded by quotes together.
   *
   * @param str the String to tokenize.
   * @return A list of tokens, including phrases surrounded by quotes if any.
   */
  public static List<String> tokenizePhraseAware(String str) {
    if (!StringUtils.isSet(str))
      return Collections.emptyList();

    str = normalizeString(str);

    boolean inQuotes = false;
    List<String> tokens = new ArrayList<String>(1);
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);

      /* Opening Quote */
      if (c == '"' && !inQuotes) {
        inQuotes = true;
        builder.append(c);
      }

      /* Closing Quote */
      else if (c == '"' && inQuotes) {
        inQuotes = false;
        builder.append(c);
      }

      /* Whitespace outside Quotes */
      else if (c == ' ' && !inQuotes) {
        tokens.add(builder.toString());
        builder.setLength(0);
      }

      /* Whitespace inside Quotes */
      else if (c == ' ' && inQuotes) {
        builder.append(c);
      }

      /* Any other Character */
      else {
        builder.append(c);
      }
    }

    if (builder.length() > 0)
      tokens.add(builder.toString());

    return tokens;
  }
}