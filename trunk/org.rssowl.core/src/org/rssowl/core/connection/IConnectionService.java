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

package org.rssowl.core.connection;

import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.util.Pair;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * The ConnectionManager is the main class of the connection plugin. It is
 * responsible to retrieve the contents of a Feed by supplying an InputStream.
 *
 * @author bpasero
 */
public interface IConnectionService {

  /**
   * Notify the service about being shut down.
   */
  void shutdown();

  /**
   * Load the Contents of the given HTTP/HTTPS - URL by connecting to it. The
   * given <code>HashMap</code> may be used to define connection related
   * properties as defined in <code>IConnectionPropertyConstants</code> for
   * example.
   *
   * @param link The URL to Load.
   * @param properties Connection related properties as defined in
   * <code>IConnectionPropertyConstants</code> for example.
   * @return The Content of the URL as InputStream.
   * @throws ConnectionException In case of an error while loading the Feed or
   * in case no suitable ProtocolHandler is present.
   * @see AuthenticationRequiredException
   * @see NotModifiedException
   */
  InputStream openHTTPStream(URI link, Map<Object, Object> properties) throws ConnectionException;

  /**
   * Reloads a <code>IFeed</code> with its News from the given
   * <code>URL</code> and returns it.
   *
   * @param link The Link to the Feed as <code>URL</code>.
   * @param monitor The Progress-Monitor used from the callee.
   * @param properties A Map of properties that can be used to transport custom
   * information
   * @return Returns the <code>IFeed</code> from the given URL.
   * @throws CoreException In case of an Exception while loading the Feed from
   * the URL.
   * @see IConnectionPropertyConstants
   */
  Pair<IFeed, IConditionalGet> reload(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws CoreException;

  /**
   * Returns the Feed Icon for the given Link. For instance, this could be the
   * favicon associated with the host providing the Feed.
   *
   * @param link The Link to the Feed as <code>URI</code>.
   * @return Returns an Icon for the given Link as byte-array.
   * @throws UnknownFeedException In case of a missing Feed-Handler for the
   * given Link.
   */
  byte[] getFeedIcon(URI link) throws UnknownFeedException;

  /**
   * Returns the Credentials-Provider capable of returning Credentials for
   * protected URLs and Proxy-Server.
   *
   * @param link The Link for which to retrieve the Credentials-Provider.
   * @return The Credentials-Provider.
   */
  ICredentialsProvider getCredentialsProvider(URI link);

  /**
   * Returns the contributed or default Factory for Secure Socket Connections.
   *
   * @return the contributed or default Factory for Secure Socket Connections.
   */
  SecureProtocolSocketFactory getSecureProtocolSocketFactory();

  /**
   * Return the Authentication Credentials for the given Feed or NULL if none.
   *
   * @param link The Link to check present Authentication Credentials.
   * @return the Authentication Credentials for the given Feed or NULL if none.
   * @throws CredentialsException In case of an error while retrieving
   * Credentials for the Feed.
   */
  ICredentials getAuthCredentials(URI link) throws CredentialsException;

  /**
   * Return the Proxy Credentials for the given Feed or NULL if none.
   *
   * @param link The Link to check present Proxy Credentials.
   * @return the Proxy Credentials for the given Feed or NULL if none.
   * @throws CredentialsException In case of an error while retrieving Proxy
   * Credentials.
   */
  IProxyCredentials getProxyCredentials(URI link) throws CredentialsException;
}