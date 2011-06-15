/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2011 RSSOwl Development Team                                  **
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

package org.rssowl.core.internal.connection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.osgi.service.url.URLStreamHandlerService;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.SyncConnectionException;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.interpreter.json.JSONException;
import org.rssowl.core.internal.interpreter.json.JSONObject;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.core.util.Triple;
import org.rssowl.core.util.URIUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Extends the {@link DefaultProtocolHandler} dealing with Google Reader
 * synchronization. The result from loading a feed is a JSON Object that is
 * passed on to the responsible JSON interpreter service.
 *
 * @author bpasero
 */
public class ReaderProtocolHandler extends DefaultProtocolHandler {

  /* Some Sync Constants */
  private static final String REQUEST_HEADER_USER_AGENT = "User-Agent"; //$NON-NLS-1$
  private static final String REQUEST_HEADER_ACCEPT_CHARSET = "Accept-Charset"; //$NON-NLS-1$
  private static final String REQUEST_HEADER_AUTHORIZATION = "Authorization"; //$NON-NLS-1$
  private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
  private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:2.0.1) Gecko/20100101 Firefox/4.0.1"; //$NON-NLS-1$
  private static final int DEFAULT_ITEM_LIMIT = 200;

  /*
   * @see
   * org.rssowl.core.internal.connection.DefaultProtocolHandler#reload(java.
   * net.URI, org.eclipse.core.runtime.IProgressMonitor, java.util.Map)
   */
  @Override
  public Triple<IFeed, IConditionalGet, URI> reload(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws CoreException {
    int itemLimit = DEFAULT_ITEM_LIMIT;
    if (properties != null && properties.containsKey(IConnectionPropertyConstants.ITEM_LIMIT)) {
      Object itemLimitObj = properties.get(IConnectionPropertyConstants.ITEM_LIMIT);
      if (itemLimitObj instanceof Integer)
        itemLimit = (Integer) itemLimitObj;
    }

    URI googleLink = readerToGoogle(link, itemLimit);
    InputStream inS = null;

    /* First Try: Use shared token */
    try {
      String authToken = handleAuthentication(false, monitor);
      inS = openGoogleConnection(authToken, googleLink, monitor, properties);
    } catch (ConnectionException e) {

      /* Rethrow if this exception is not about Authentication issues */
      if (!(e instanceof AuthenticationRequiredException) && !(e instanceof SyncConnectionException))
        throw e;

      /* Return on Cancelation or Shutdown */
      if (monitor.isCanceled()) {
        closeStream(inS, true);
        return null;
      }

      /* Second Try: Obtain fresh token (could be expired) */
      String authToken = handleAuthentication(true, monitor);
      inS = openGoogleConnection(authToken, googleLink, monitor, properties);
    }

    /* Return on Cancelation or Shutdown */
    if (monitor.isCanceled()) {
      closeStream(inS, true);
      return null;
    }

    /* Retrieve Conditional Get if present */
    IConditionalGet conditionalGet = getConditionalGet(googleLink, inS);

    /* Return on Cancelation or Shutdown */
    if (monitor.isCanceled()) {
      closeStream(inS, true);
      return null;
    }

    /* Read JSON Object from Response and parse */
    IModelFactory typesFactory = Owl.getModelFactory();
    IFeed feed = typesFactory.createFeed(null, link);
    feed.setBase(readerToHTTP(link));
    try {
      JSONObject obj = new JSONObject(StringUtils.readString(new InputStreamReader(inS, UTF_8)));
      Owl.getInterpreter().interpretJSONObject(obj, feed);
    } catch (JSONException e) {
      throw new ParserException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } catch (IOException e) {
      throw new ParserException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    return Triple.create(feed, conditionalGet, link);
  }

  private InputStream openGoogleConnection(String authToken, URI googleLink, IProgressMonitor monitor, Map<Object, Object> properties) throws ConnectionException {

    /* Return on Cancelation or Shutdown */
    if (monitor.isCanceled())
      return null;

    /* Fill necessary headers to retrieve feed from Google */
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(REQUEST_HEADER_AUTHORIZATION, SyncUtils.getGoogleAuthorizationHeader(authToken));
    headers.put(REQUEST_HEADER_ACCEPT_CHARSET, UTF_8.toLowerCase());
    headers.put(REQUEST_HEADER_USER_AGENT, BROWSER_USER_AGENT); //Necessary as otherwise the content is not sent over as gzip for some unknown reason
    properties.put(IConnectionPropertyConstants.HEADERS, headers);

    /* Add Monitor to support early cancelation */
    properties.put(IConnectionPropertyConstants.PROGRESS_MONITOR, monitor);

    return openStream(googleLink, properties);
  }

  private String handleAuthentication(boolean refresh, IProgressMonitor monitor) throws ConnectionException {

    /* Obtain Google Credentials */
    URI googleLoginUri = URI.create(SyncUtils.GOOGLE_LOGIN_URL);
    ICredentialsProvider provider = Owl.getConnectionService().getCredentialsProvider(googleLoginUri);
    ICredentials credentials = provider.getAuthCredentials(googleLoginUri, null);
    if (credentials == null)
      throw new AuthenticationRequiredException(null, Status.CANCEL_STATUS);

    /* Obtain Google Authentication Token */
    String token = SyncUtils.getGoogleAuthToken(credentials.getUsername(), credentials.getPassword(), refresh, monitor);
    if (token == null)
      throw new AuthenticationRequiredException(null, Status.CANCEL_STATUS);

    return token;
  }

  /**
   * Parameters:
   * <ul>
   * <li>ot=[unix timestamp] : The time from which you want to retrieve items.</li>
   * <li>r=[d|n|o] : Sort order of item results.</li>
   * <li>xt=[exclude target] : Used to exclude certain items from the feed.</li>
   * <li>n=[integer] : The maximum number of results to return.</li>
   * <li>ck=[unix timestamp] : Use the current Unix time here, helps Google with
   * caching.</li>
   * <li>client=[your client] : You can use the default Google client (scroll).</li>
   * </ul>
   */
  private URI readerToGoogle(URI uri, int itemLimit) throws ConnectionException {

    /* Handle Special Feeds */
    String linkVal = uri.toString();
    try {

      /* All Items */
      if (SyncUtils.GOOGLE_READER_ALL_ITEMS_FEED.equals(linkVal))
        return new URI(SyncUtils.GOOGLE_STREAM_CONTENTS_URL + "user/-/state/com.google/reading-list?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis()); //$NON-NLS-1$ //$NON-NLS-2$

      /* Starred Items */
      else if (SyncUtils.GOOGLE_READER_STARRED_FEED.equals(linkVal))
        return new URI(SyncUtils.GOOGLE_STREAM_CONTENTS_URL + "user/-/state/com.google/starred?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis()); //$NON-NLS-1$ //$NON-NLS-2$

      /* Shared Items */
      else if (SyncUtils.GOOGLE_READER_SHARED_ITEMS_FEED.equals(linkVal))
        return new URI(SyncUtils.GOOGLE_STREAM_CONTENTS_URL + "user/-/state/com.google/broadcast?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis()); //$NON-NLS-1$ //$NON-NLS-2$

      /* Recommended Items */
      else if (SyncUtils.GOOGLE_READER_RECOMMENDED_ITEMS_FEED.equals(linkVal)) {
        String language = Locale.getDefault().getLanguage();
        String exclude = "&xt=user/-/state/com.google/read&xt=user/-/state/com.google/dislike"; //$NON-NLS-1$
        return new URI(SyncUtils.GOOGLE_STREAM_CONTENTS_URL + "user/-/state/com.google/itemrecs/" + language + "?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis() + exclude); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      /* Notes */
      else if (SyncUtils.GOOGLE_READER_NOTES_FEED.equals(linkVal))
        return new URI(SyncUtils.GOOGLE_STREAM_CONTENTS_URL + "user/-/state/com.google/created?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis()); //$NON-NLS-1$ //$NON-NLS-2$
    } catch (URISyntaxException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* Normal Synchronized Feed */
    URI httpUri = readerToHTTP(uri);
    try {
      return new URI(SyncUtils.GOOGLE_FEED_URL + URIUtils.urlEncode(httpUri.toString()) + "?n=" + itemLimit + "&client=scroll&ck=" + System.currentTimeMillis()); //$NON-NLS-1$ //$NON-NLS-2$
    } catch (URISyntaxException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  /*
   * @see
   * org.rssowl.core.internal.connection.DefaultProtocolHandler#openStream(java
   * .net.URI, org.eclipse.core.runtime.IProgressMonitor, java.util.Map)
   */
  @Override
  public InputStream openStream(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws ConnectionException {
    return super.openStream(readerToHTTP(link), monitor, properties);
  }

  /*
   * @see
   * org.rssowl.core.internal.connection.DefaultProtocolHandler#getFeedIcon(
   * java.net.URI, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public byte[] getFeedIcon(URI link, IProgressMonitor monitor) {
    try {
      String linkVal = link.toString();

      /* Do not try to resolve special Google Reader feed icons */
      if (SyncUtils.GOOGLE_READER_ALL_ITEMS_FEED.equals(linkVal))
        return null;
      else if (SyncUtils.GOOGLE_READER_STARRED_FEED.equals(linkVal))
        return null;
      else if (SyncUtils.GOOGLE_READER_SHARED_ITEMS_FEED.equals(linkVal))
        return null;
      else if (SyncUtils.GOOGLE_READER_RECOMMENDED_ITEMS_FEED.equals(linkVal))
        return null;
      else if (SyncUtils.GOOGLE_READER_NOTES_FEED.equals(linkVal))
        return null;

      /* Otherwise proceed loading feed icon through HTTP */
      return super.getFeedIcon(readerToHTTP(link), monitor);
    } catch (ConnectionException e) {
      return null;
    }
  }

  /*
   * @see
   * org.rssowl.core.internal.connection.DefaultProtocolHandler#getLabel(java
   * .net.URI, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public String getLabel(URI link, IProgressMonitor monitor) throws ConnectionException {
    String linkVal = link.toString();

    /* Do not try to resolve special Google Reader feed labels */
    if (SyncUtils.GOOGLE_READER_ALL_ITEMS_FEED.equals(linkVal))
      return Messages.ReaderProtocolHandler_GR_ALL_ITEMS;
    else if (SyncUtils.GOOGLE_READER_STARRED_FEED.equals(linkVal))
      return Messages.ReaderProtocolHandler_GR_STARRED_ITEMS;
    else if (SyncUtils.GOOGLE_READER_SHARED_ITEMS_FEED.equals(linkVal))
      return Messages.ReaderProtocolHandler_GR_SHARED_ITEMS;
    else if (SyncUtils.GOOGLE_READER_RECOMMENDED_ITEMS_FEED.equals(linkVal))
      return Messages.ReaderProtocolHandler_GR_RECOMMENDED_ITEMS;
    else if (SyncUtils.GOOGLE_READER_NOTES_FEED.equals(linkVal))
      return Messages.ReaderProtocolHandler_GR_NOTES;

    /* Otherwise proceed loading feed label through HTTP */
    return super.getLabel(readerToHTTP(link), monitor);
  }

  /*
   * @see
   * org.rssowl.core.internal.connection.DefaultProtocolHandler#getFeed(java
   * .net.URI, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public URI getFeed(URI website, IProgressMonitor monitor) throws ConnectionException {
    return super.getFeed(readerToHTTP(website), monitor);
  }

  /**
   * Do not override default URLStreamHandler of HTTP/HTTPS and therefor return
   * NULL.
   *
   * @see org.rssowl.core.connection.IProtocolHandler#getURLStreamHandler()
   */
  @Override
  public URLStreamHandlerService getURLStreamHandler() {
    return null;
  }

  private URI readerToHTTP(URI uri) throws ConnectionException {
    try {
      String scheme = SyncUtils.READER_HTTPS_SCHEME.equals(uri.getScheme()) ? URIUtils.HTTPS_SCHEME : URIUtils.HTTP_SCHEME;
      return new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
    } catch (URISyntaxException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }
}