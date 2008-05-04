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

package org.rssowl.core.connection;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * The default implementation of the ICredentialsProvider retrieves
 * authentication Credentials from the Equinox Security Storage.
 *
 * @author bpasero
 */
public class PlatformCredentialsProvider implements ICredentialsProvider {

  /** Node for feed related security preferences */
  public static final String SECURE_FEED_NODE = "rssowl/feeds";

  /* Unique Key to store Usernames */
  private static final String USERNAME = "org.rssowl.core.connection.auth.Username"; //$NON-NLS-1$

  /* Unique Key to store Passwords */
  private static final String PASSWORD = "org.rssowl.core.connection.auth.Password"; //$NON-NLS-1$

  /* Unique Key to store Domains */
  private static final String DOMAIN = "org.rssowl.core.connection.auth.Domain"; //$NON-NLS-1$

  /* Default Realm being used to store credentials */
  private static final String REALM = ""; //$NON-NLS-1$

  /* A cache of non-protected Links */
  private final Set<String> fUnprotectedLinksCache = new HashSet<String>();

  private ISecurePreferences fSecurity;

  /** Default credentials provider using Equinox Security */
  public PlatformCredentialsProvider() {
    fSecurity = SecurePreferencesFactory.getDefault();
  }

  /* Simple POJO Implementation of ICredentials */
  private static class Credentials implements ICredentials {
    private String fUsername;
    private String fPassword;
    private String fDomain;

    Credentials(String username, String password, String domain) {
      fUsername = username;
      fPassword = password;
      fDomain = domain;
    }

    public String getUsername() {
      return fUsername;
    }

    public String getPassword() {
      return fPassword;
    }

    public String getDomain() {
      return fDomain;
    }
  }

  /*
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#getAuthCredentials(java
   * .net.URI, java.lang.String)
   */
  public ICredentials getAuthCredentials(URI link, String realm) throws CredentialsException {

    /* Check Cache first */
    if (fUnprotectedLinksCache.contains(link.toString()))
      return null;

    /* Retrieve Credentials */
    ICredentials authorizationInfo = getAuthorizationInfo(link, realm);

    /* Credentials Provided */
    if (authorizationInfo != null)
      return authorizationInfo;

    /* Cache as unprotected */
    if (!fUnprotectedLinksCache.contains(link.toString()))
      fUnprotectedLinksCache.add(link.toString());

    /* Credentials not provided */
    return null;
  }

  private ICredentials getAuthorizationInfo(URI link, String realm) throws CredentialsException {

    /* Return from Equinox Security Storage */
    if (fSecurity.nodeExists(SECURE_FEED_NODE)) { // Global Feed Node
      ISecurePreferences allFeedsPreferences = fSecurity.node(SECURE_FEED_NODE);
      if (allFeedsPreferences.nodeExists(EncodingUtils.encodeSlashes(link.toString()))) { // Feed Node
        ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
        if (feedPreferences.nodeExists(EncodingUtils.encodeSlashes(realm != null ? realm : REALM))) { // Realm Node
          ISecurePreferences realmPreferences = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));

          try {
            String username = realmPreferences.get(USERNAME, null);
            String password = realmPreferences.get(PASSWORD, null);
            String domain = realmPreferences.get(DOMAIN, null);

            if (username != null && password != null)
              return new Credentials(username, password, domain);
          } catch (StorageException e) {
            throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
      }
    }

    return null;
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#getProxyCredentials
   * (java.net.URI)
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
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#setAuthCredentials(org.
   * rssowl.core.connection.ICredentials, java.net.URI, java.lang.String)
   */
  public void setAuthCredentials(ICredentials credentials, URI link, String realm) throws CredentialsException {

    /* Store in Equinox Security Storage */
    ISecurePreferences allFeedsPreferences = fSecurity.node(SECURE_FEED_NODE);
    ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
    ISecurePreferences realmPreference = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));

    IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();
    boolean useMasterPassword = globalScope.getBoolean(DefaultPreferences.USE_MASTER_PASSWORD);
    try {
      if (credentials.getUsername() != null)
        realmPreference.put(USERNAME, credentials.getUsername(), useMasterPassword);

      if (credentials.getPassword() != null)
        realmPreference.put(PASSWORD, credentials.getPassword(), useMasterPassword);

      if (credentials.getDomain() != null)
        realmPreference.put(DOMAIN, credentials.getDomain(), useMasterPassword);

      realmPreference.flush(); // Flush to disk early
    } catch (StorageException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } catch (IOException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* Uncache */
    fUnprotectedLinksCache.remove(link.toString());
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#setProxyCredentials
   * (org.rssowl.core.connection.auth.IProxyCredentials, java.net.URI)
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
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#deleteAuthCredentials(java
   * .net.URI, java.lang.String)
   */
  public void deleteAuthCredentials(URI link, String realm) throws CredentialsException {

    /* Remove from Equinox Security Storage */
    if (fSecurity.nodeExists(SECURE_FEED_NODE)) { // Global Feed Node
      ISecurePreferences allFeedsPreferences = fSecurity.node(SECURE_FEED_NODE);
      if (allFeedsPreferences.nodeExists(EncodingUtils.encodeSlashes(link.toString()))) { // Feed Node
        ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
        if (feedPreferences.nodeExists(EncodingUtils.encodeSlashes(realm != null ? realm : REALM))) { // Realm Node
          ISecurePreferences realmPreferences = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));
          realmPreferences.clear();
          realmPreferences.removeNode();
          try {
            feedPreferences.flush();
          } catch (IOException e) {
            throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
      }
    }

    /* Delete from Cache */
    fUnprotectedLinksCache.remove(link.toString());
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#deleteProxyCredentials
   * (java.net.URI)
   */
  public void deleteProxyCredentials(URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();
    proxyService.setProxiesEnabled(false);
    //TODO System Properties are still set?
  }
}