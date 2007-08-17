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

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.rssowl.core.internal.Activator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of the ICredentialsProvider retrieves
 * authentication Credentials from the Platform.
 *
 * @author bpasero
 */
public class PlatformCredentialsProvider implements ICredentialsProvider {

  /* Unique Key to store Usernames */
  private static final String USERNAME = "org.rssowl.core.connection.auth.Username"; //$NON-NLS-1$

  /* Unique Key to store Passwords */
  private static final String PASSWORD = "org.rssowl.core.connection.auth.Password"; //$NON-NLS-1$

  /* Unique Key to store Domains */
  private static final String DOMAIN = "org.rssowl.core.connection.auth.Domain"; //$NON-NLS-1$

  /* Default Realm being used to store credentials */
  private static final String REALM = ""; //$NON-NLS-1$

  /* Default Scheme being used to store credentials */
  private static final String SCHEME = ""; //$NON-NLS-1$

  /* A cache of non-protected Links */
  private final Set<String> fUnprotectedLinksCache = new HashSet<String>();

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#getAuthCredentials(java.net.URI)
   */
  public ICredentials getAuthCredentials(URI link) {

    /* Retrieve Credentials */
    final Map< ? , ? > authorizationInfo = getAuthorizationInfo(link);

    /* Credentials Provided */
    if (authorizationInfo != null) {
      return new ICredentials() {
        public String getUsername() {
          return (String) authorizationInfo.get(USERNAME);
        }

        public String getPassword() {
          return (String) authorizationInfo.get(PASSWORD);
        }

        public String getDomain() {
          return (String) authorizationInfo.get(DOMAIN);
        }
      };
    }

    /* Cache as unprotected */
    if (!fUnprotectedLinksCache.contains(link.toString()))
      fUnprotectedLinksCache.add(link.toString());

    /* Credentials not provided */
    return null;
  }

  private Map< ? , ? > getAuthorizationInfo(URI link) {

    /* Check Cache first */
    if (fUnprotectedLinksCache.contains(link.toString()))
      return null;

    /* Return from Platform */
    try {
      URL urlLink = link.toURL();
      return Platform.getAuthorizationInfo(urlLink, REALM, SCHEME);
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#getProxyCredentials(java.net.URI)
   */
  public IProxyCredentials getProxyCredentials(URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();

    /* Check if Proxy is enabled */
    if (!proxyService.isProxiesEnabled())
      return null;

    String host = link.getHost();
    boolean isSSL = "https".equals(link.getScheme());

    /* Retrieve Proxy Data */
    final IProxyData proxyData = proxyService.getProxyDataForHost(host, isSSL ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE);
    if (proxyData != null) {
      return new IProxyCredentials() {
        public String getHost() {
          return proxyData.getHost();
        }

        public int getPort() {
          return proxyData.getPort();
        }

        public String getDomain() {
          return null;
        }

        public String getPassword() {
          return proxyData.getPassword();
        }

        public String getUsername() {
          return proxyData.getUserId();
        }
      };
    }

    /* Feed does not require Proxy or Credentials not supplied */
    return null;
  }

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#setAuthCredentials(org.rssowl.core.connection.auth.ICredentials,
   * java.net.URI)
   */
  public void setAuthCredentials(ICredentials credentials, URI link) throws CredentialsException {

    /* Create Credentials Map */
    Map<String, String> credMap = new HashMap<String, String>();

    if (credentials.getUsername() != null)
      credMap.put(USERNAME, credentials.getUsername());

    if (credentials.getPassword() != null)
      credMap.put(PASSWORD, credentials.getPassword());

    if (credentials.getDomain() != null)
      credMap.put(DOMAIN, credentials.getDomain());

    /* Store in Platform */
    try {
      URL urlLink = link.toURL();
      Platform.addAuthorizationInfo(urlLink, REALM, SCHEME, credMap);
    } catch (CoreException e) {
      throw new CredentialsException(e.getStatus());
    } catch (MalformedURLException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* Uncache */
    fUnprotectedLinksCache.remove(link.toString());
  }

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#setProxyCredentials(org.rssowl.core.connection.auth.IProxyCredentials,
   * java.net.URI)
   */
  public void setProxyCredentials(IProxyCredentials credentials, URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();
    proxyService.setProxiesEnabled(true);
    boolean isSSL = "https".equals(link.getScheme());

    /* Retrieve Proxy Data */
    final IProxyData proxyData = proxyService.getProxyData(isSSL ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE);
    if (proxyData != null) { //TODO What if Data is NULL?
      proxyData.setHost(credentials.getHost());
      proxyData.setPort(credentials.getPort());
      proxyData.setUserid(credentials.getUsername());
      proxyData.setPassword(credentials.getPassword());
    }
  }

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#deleteAuthCredentials(java.net.URI)
   */
  public void deleteAuthCredentials(URI link) throws CredentialsException {

    /* Remove from Platform */
    try {
      URL urlLink = link.toURL();
      Platform.flushAuthorizationInfo(urlLink, REALM, SCHEME);
    } catch (CoreException e) {
      throw new CredentialsException(e.getStatus());
    } catch (MalformedURLException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* Delete from Cache */
    fUnprotectedLinksCache.remove(link.toString());
  }

  /*
   * @see org.rssowl.core.connection.auth.ICredentialsProvider#deleteProxyCredentials(java.net.URI)
   */
  public void deleteProxyCredentials(URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();
    proxyService.setProxiesEnabled(false);
    //TODO System Properties are still set?
  }
}