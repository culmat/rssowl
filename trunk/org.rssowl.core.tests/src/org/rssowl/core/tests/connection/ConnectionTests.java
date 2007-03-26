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

package org.rssowl.core.tests.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionManager;
import org.rssowl.core.connection.IConditionalGetCompatible;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.connection.auth.ICredentials;
import org.rssowl.core.connection.auth.ICredentialsProvider;
import org.rssowl.core.connection.auth.IProxyCredentials;
import org.rssowl.core.interpreter.Interpreter;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.internal.types.Feed;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.types.IConditionalGet;
import org.rssowl.core.model.types.IFeed;
import org.rssowl.core.util.Pair;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This TestCase covers use-cases for the Connection Plugin.
 *
 * @author bpasero
 */
public class ConnectionTests {

  /**
   * Test contribution of Credentials Provider.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testProxyCredentialProvider() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);

    IProxyCredentials proxyCredentials = conManager.getProxyCredentials(feed.getLink());

    assertEquals("", proxyCredentials.getDomain());
    assertEquals("bpasero", proxyCredentials.getUsername());
    assertEquals("admin", proxyCredentials.getPassword());
    assertEquals("127.0.0.1", proxyCredentials.getHost());
    assertEquals(0, proxyCredentials.getPort());
  }

  /**
   * Test a protected Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testProtectedFeed() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rss.xml");
    ICredentialsProvider credProvider = conManager.getCredentialsProvider(feedUrl);

    IFeed feed = new Feed(feedUrl);
    AuthenticationRequiredException e = null;

    try {
      conManager.openHTTPStream(feed.getLink(), null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }

    assertNotNull(e);

    ICredentials credentials = new ICredentials() {
      public String getDomain() {
        return null;
      }

      public String getPassword() {
        return "admin";
      }

      public String getUsername() {
        return "bpasero";
      }
    };

    credProvider.setAuthCredentials(credentials, feedUrl);

    InputStream inS = conManager.openHTTPStream(feed.getLink(), null);
    assertNotNull(inS);

    Interpreter.getDefault().interpret(inS, feed);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testHTTPFeed() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/rss_2_0.xml");
    IFeed feed = new Feed(feedUrl);

    InputStream inS = conManager.openHTTPStream(feed.getLink(), null);
    assertNotNull(inS);

    Interpreter.getDefault().interpret(inS, feed);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testHTTPSFeed() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("https://sourceforge.net/export/rss2_projnews.php?group_id=141424&rss_fulltext=1");
    IFeed feed = new Feed(feedUrl);

    InputStream inS = conManager.openHTTPStream(feed.getLink(), null);
    assertNotNull(inS);

    Interpreter.getDefault().interpret(inS, feed);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via FILE Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFILEFeed() throws Exception {
    URL pluginLocation = FileLocator.toFileURL(Platform.getBundle("org.rssowl.core.tests").getEntry("/"));
    ConnectionManager conManager = ConnectionManager.getDefault();
    URL feedUrl = pluginLocation.toURI().resolve("data/interpreter/feed_rss.xml").toURL();
    IFeed feed = new Feed(feedUrl.toURI());

    Pair<IFeed, IConditionalGet> result = conManager.reload(feed.getLink(), null, null);

    assertEquals("RSS 2.0", result.getFirst().getFormat());
  }

  /**
   * Test Conditional GET with a compatible Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testConditionalGet() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://rss.slashdot.org/Slashdot/slashdot/to");
    IFeed feed = new Feed(feedUrl);
    NotModifiedException e = null;

    InputStream inS = conManager.openHTTPStream(feed.getLink(), null);
    assertNotNull(inS);

    String ifModifiedSince = null;
    String ifNoneMatch = null;
    if (inS instanceof IConditionalGetCompatible) {
      ifModifiedSince = ((IConditionalGetCompatible) inS).getIfModifiedSince();
      ifNoneMatch = ((IConditionalGetCompatible) inS).getIfNoneMatch();
    }
    IConditionalGet conditionalGet = NewsModel.getDefault().getTypesFactory().createConditionalGet(ifModifiedSince, feedUrl, ifNoneMatch);

    Map<Object, Object> conProperties = new HashMap<Object, Object>();
    ifModifiedSince = conditionalGet.getIfModifiedSince();
    if (ifModifiedSince != null)
      conProperties.put(IConnectionPropertyConstants.IF_MODIFIED_SINCE, ifModifiedSince);

    ifNoneMatch = conditionalGet.getIfNoneMatch();
    if (ifNoneMatch != null)
      conProperties.put(IConnectionPropertyConstants.IF_NONE_MATCH, ifNoneMatch);

    try {
      conManager.openHTTPStream(feed.getLink(), conProperties);
    } catch (NotModifiedException e1) {
      e = e1;
    }

    assertNotNull(e);
  }

  /**
   * Test contribution of Credentials Provider.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testAuthCredentialProviderContribution() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);
    AuthenticationRequiredException e = null;

    try {
      conManager.getCredentialsProvider(feedUrl).deleteProxyCredentials(feedUrl); //Disable Proxy
      conManager.openHTTPStream(feed.getLink(), null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }

    assertNull(e);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCredentialsDeleted() throws Exception {
    ConnectionManager conManager = ConnectionManager.getDefault();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);

    IModelDAO dao = NewsModel.getDefault().getPersistenceLayer().getModelDAO();
    feed = dao.saveFeed(feed);

    ICredentials authCreds = new ICredentials() {
      public String getDomain() {
        return null;
      }

      public String getPassword() {
        return null;
      }

      public String getUsername() {
        return null;
      }
    };

    conManager.getCredentialsProvider(feedUrl).setAuthCredentials(authCreds, feedUrl);

    assertNotNull(conManager.getAuthCredentials(feedUrl));

    dao.deleteFeed(new FeedLinkReference(feedUrl));

    assertNull(conManager.getAuthCredentials(feedUrl));
  }
}