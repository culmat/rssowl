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

package org.rssowl.contrib.internal.nntp;

import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.IProxyCredentials;
import org.rssowl.core.internal.connection.DefaultCredentialsProvider;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Subclass of <code>DefaultCredentialsProvider</code> normalizing any
 * <code>URI</code> to its scheme and host. Avoids having to add credentials
 * for any group of the server that the user wants to subscribe.
 * <p>
 * TODO The delete-methods are not implemented, because deleting the normalized
 * link would mean that all subscriptions to other groups of the same server
 * would loose their credentials too.
 * </p>
 *
 * @author bpasero
 */
public class NewsGroupCredentialsProvider extends DefaultCredentialsProvider {

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#deleteAuthCredentials(java.net.URI)
   */
  @Override
  public void deleteAuthCredentials(URI link) {}

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#deleteProxyCredentials(java.net.URI)
   */
  @Override
  public void deleteProxyCredentials(URI link) {}

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#getAuthCredentials(java.net.URI)
   */
  @Override
  public ICredentials getAuthCredentials(URI link) {
    return super.getAuthCredentials(normalizeUri(link));
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#getProxyCredentials(java.net.URI)
   */
  @Override
  public IProxyCredentials getProxyCredentials(URI link) {
    return super.getProxyCredentials(normalizeUri(link));
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#setAuthCredentials(org.rssowl.core.connection.auth.ICredentials,
   * java.net.URI)
   */
  @Override
  public void setAuthCredentials(ICredentials credentials, URI link) throws CredentialsException {
    super.setAuthCredentials(credentials, normalizeUri(link));
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#setProxyCredentials(org.rssowl.core.connection.auth.IProxyCredentials,
   * java.net.URI)
   */
  @Override
  public void setProxyCredentials(IProxyCredentials credentials, URI link) {
    super.setProxyCredentials(credentials, normalizeUri(link));
  }

  private URI normalizeUri(URI link) {
    try {
      return new URI(link.getScheme(), link.getHost(), null, null);
    } catch (URISyntaxException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    return link;
  }
}