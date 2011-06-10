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

package org.rssowl.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Some tools to synchronize with online services like Google Reader.
 *
 * @author bpasero
 */
public class SyncUtils {

  /** Google Client Login Site */
  public static final String GOOGLE_LOGIN = "https://www.google.com/accounts/ClientLogin"; //$NON-NLS-1$

  /** Google Token Service */
  public static final String TOKEN_URL = "http://www.google.com/reader/api/0/token"; //$NON-NLS-1$

  /** Schemes to use for synced feeds */
  public static final String READER_HTTP_SCHEME = "reader"; //$NON-NLS-1$
  public static final String READER_HTTPS_SCHEME = "readers"; //$NON-NLS-1$

  /* Part of the identifier of synchronized news */
  private static final String SYNCED_NEWS_ID_PART = "tag:google.com"; //$NON-NLS-1$

  /* Google Auth Identifier */
  private static final String AUTH_IDENTIFIER = "Auth="; //$NON-NLS-1$

  /* Google Auth Header */
  private static final String GOOGLE_LOGIN_HEADER_VALUE = "GoogleLogin auth="; //$NON-NLS-1$

  /* Google Authentication Token can be shared during the session */
  private static String fgSharedAuthToken;

  /* Some special preferences a news can have after parsed from the JSONInterpreter */
  public static final String GOOGLE_MARKED_READ = "org.rssowl.pref.GoogleMarkedRead"; //$NON-NLS-1$
  public static final String GOOGLE_LABELS = "org.rssowl.pref.GoogleLabels"; //$NON-NLS-1$

  /**
   * Obtains the Google Auth Token to perform REST operations for Google
   * Services.
   *
   * @param email the user account for google
   * @param pw the password for the user account
   * @param refresh if <code>true</code> causes a fresh authentication token to
   * be obtained and a shared one to be picked up otherwise.
   * @param monitor an instance of {@link IProgressMonitor} that can be used to
   * cancel the operation and report progress.
   * @return the google Auth Token for the given account or <code>null</code> if
   * none.
   * @throws ConnectionException Checked Exception to be used in case of any
   * Exception.
   */
  public static String getGoogleAuthToken(String email, String pw, boolean refresh, IProgressMonitor monitor) throws ConnectionException {

    /*
     * Return the shared token if existing or even null if not willing to
     * refresh. Clients have to force refresh to get the token then.
     */
    if (!refresh)
      return fgSharedAuthToken;

    /* Clear Shared Token */
    fgSharedAuthToken = null;

    /* Return on cancellation */
    if (monitor.isCanceled())
      return null;

    /* Obtain a new token (only 1 Thread permitted) */
    synchronized (GOOGLE_LOGIN) {

      /* Another thread might have won the race */
      if (fgSharedAuthToken != null)
        return fgSharedAuthToken;

      /* Return on cancellation */
      if (monitor.isCanceled())
        return null;

      /* Now Connect to Google */
      try {
        fgSharedAuthToken = internalGetGoogleAuthToken(email, pw, monitor);
      } catch (URISyntaxException e) {
        throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
      } catch (IOException e) {
        throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }
    }

    return fgSharedAuthToken;
  }

  private static String internalGetGoogleAuthToken(String email, String pw, IProgressMonitor monitor) throws ConnectionException, URISyntaxException, IOException {
    URI uri = new URI(GOOGLE_LOGIN);
    IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);
    if (handler != null) {

      /* Google Specific Parameters */
      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put("accountType", "GOOGLE"); //$NON-NLS-1$ //$NON-NLS-2$
      parameters.put("Email", email); //$NON-NLS-1$
      parameters.put("Passwd", pw); //$NON-NLS-1$
      parameters.put("service", "reader"); //$NON-NLS-1$ //$NON-NLS-2$
      parameters.put("source", "RSSOwl.org-RSSOwl-" + Activator.getDefault().getVersion()); //$NON-NLS-1$ //$NON-NLS-2$

      Map<Object, Object> properties = new HashMap<Object, Object>();
      properties.put(IConnectionPropertyConstants.PARAMETERS, parameters);
      properties.put(IConnectionPropertyConstants.POST, Boolean.TRUE);

      BufferedReader reader = null;
      try {
        InputStream inS = handler.openStream(uri, monitor, properties);
        reader = new BufferedReader(new InputStreamReader(inS));
        String line;
        while (!monitor.isCanceled() && (line = reader.readLine()) != null) {
          if (line.startsWith(AUTH_IDENTIFIER))
            return line.substring(AUTH_IDENTIFIER.length());
        }
      } finally {
        if (reader != null)
          reader.close();
      }
    }

    return null;
  }

  /**
   * @param authToken the authorization token that can be obtained from
   * {@link SyncUtils#getGoogleAuthToken(String, String, boolean, IProgressMonitor)}
   * @return a header value that can be used inside <code>Authorization</code>
   * to get access to Google Services.
   */
  public static String getGoogleAuthorizationHeader(String authToken) {
    return GOOGLE_LOGIN_HEADER_VALUE + authToken;
  }

  /**
   * @param news the {@link INews} to check for synchronization.
   * @return <code>true</code> if the news is under synchronization control and
   * <code>false</code> otherwise.
   */
  public static boolean isSynchronized(INews news) {
    return news != null && news.getGuid() != null && news.getGuid().getValue().startsWith(SYNCED_NEWS_ID_PART) && StringUtils.isSet(news.getInReplyTo());
  }

  /**
   * @param bm the {@link IBookMark} to check for synchronization.
   * @return <code>true</code> if the bookmark is under synchronization control
   * and <code>false</code> otherwise.
   */
  public static boolean isSynchronized(IBookMark bm) {
    String link = bm.getFeedLinkReference().getLinkAsText();
    return link.startsWith(READER_HTTP_SCHEME);
  }
}