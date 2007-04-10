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

package org.rssowl.core.model.reference;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.persist.IFeed;

import java.net.URI;

/**
 * A <code>FeedLinkReference</code> is a lightweight representation of IFeed.
 * The actual IFeed can be retrieved by calling the resolve() method.
 */
public final class FeedLinkReference {
  private final URI fLink;

  /**
   * Creates an instance of this object for a Feed with link <code>link</code>.
   * 
   * @param link The link of the Feed that this object references. This cannot
   * be null.
   */
  public FeedLinkReference(URI link) {
    Assert.isNotNull(link, "link"); //$NON-NLS-1$
    this.fLink = link;
  }

  /**
   * @return the link of the feed this object references.
   */
  public final URI getLink() {
    return fLink;
  }

  /**
   * Loads the Feed that this reference points to from the persistence layer and
   * returns it. It may return <code>null</code> if the feed has been deleted
   * from the persistence layer.
   * 
   * @return the IFeed this object references.
   * @throws PersistenceException In case an error occurs while accessing the
   * persistence layer.
   */
  public final IFeed resolve() throws PersistenceException {
    return Owl.getPersistenceService().getApplicationLayer().loadFeed(fLink);
  }

  /**
   * Returns <code>true</code> if calling {@link #resolve()} on this reference
   * will return an entity equal to <code>feed</code>.
   * 
   * @param feed The IFeed to compare to.
   * @return <code>true</code> if this object references <code>feed</code>
   * or <code>false</code> otherwise.
   */
  public boolean references(IFeed feed) {
    Assert.isNotNull(feed);

    URI entityLink = feed.getLink();
    return entityLink == null ? false : fLink.equals(entityLink);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    FeedLinkReference other = (FeedLinkReference) obj;
    return fLink.toString().equals(other.fLink.toString());
  }

  @Override
  public int hashCode() {
    return fLink.toString().hashCode();
  }

  @Override
  @SuppressWarnings("nls")
  public String toString() {
    String name = super.toString();
    int index = name.lastIndexOf('.');
    if (index != -1)
      name = name.substring(index + 1, name.length());

    return name + " (Link = " + fLink + ")";
  }
}
