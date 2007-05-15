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

package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A usual bookmark as seen in Firefox or other Browsers. The Bookmark is used
 * to define a position for a <code>Feed</code> inside the hierarchy of
 * Folders. The user may define some properties, e.g. how often to reload the
 * related Feed.
 *
 * @author bpasero
 */
public class BookMark extends Mark implements IBookMark {
  private String fFeedLink;
  private transient FeedLinkReference fFeedLinkReference;
  private boolean fIsErrorLoading;

  /**
   * Creates a new Element of the type BookMark. A BookMark is only visually
   * represented in case it was added to a Folder. Make sure to add it to a
   * Folder using <code>Folder#addMark(Mark)</code>
   *
   * @param id The unique ID of this type.
   * @param folder The Folder this BookMark belongs to.
   * @param feedRef The reference to the feed this BookMark is related to.
   * @param name The Name of this BookMark.
   */
  public BookMark(Long id, IFolder folder, FeedLinkReference feedRef, String name) {
    super(id, folder, name);
    Assert.isNotNull(feedRef, "feedRef cannot be null"); //$NON-NLS-1$
    fFeedLinkReference = feedRef;
    fFeedLink = feedRef.getLink().toString();
  }

  /**
   * Default constructor for deserialization
   */
  protected BookMark() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#isErrorLoading()
   */
  public synchronized boolean isErrorLoading() {
    return fIsErrorLoading;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#setErrorLoading(boolean)
   */
  public synchronized void setErrorLoading(boolean isErrorLoading) {
    fIsErrorLoading = isErrorLoading;
  }

  /*
   * @see org.rssowl.core.model.types.IBookMark#getFeedLinkReference()
   */
  public synchronized FeedLinkReference getFeedLinkReference() {
    if (fFeedLinkReference == null) {
      try {
        fFeedLinkReference = new FeedLinkReference(new URI(fFeedLink));
      } catch (URISyntaxException e) {
        throw new IllegalStateException("Malformed URI was stored somehow: " + fFeedLink); //$NON-NLS-1$
      }
    }
    return fFeedLinkReference;
  }

  /*
   * @see org.rssowl.core.model.types.IBookMark#setFeedLinkReference(org.rssowl.core.model.reference.FeedLinkReference)
   */
  public synchronized void setFeedLinkReference(FeedLinkReference feedLinkRef) {
    Assert.isNotNull(feedLinkRef, "link cannot be null"); //$NON-NLS-1$
    fFeedLinkReference = feedLinkRef;
    fFeedLink = feedLinkRef.getLink().toString();
  }

  /**
   * Compare the given type with this type for identity.
   *
   * @param bookMark to be compared.
   * @return whether this object and <code>bookMark</code> are identical. It
   * compares all the fields.
   */
  public synchronized boolean isIdentical(IBookMark bookMark) {
    if (this == bookMark)
      return true;

    if (!(bookMark instanceof BookMark))
      return false;

    synchronized (bookMark) {
      BookMark b = (BookMark) bookMark;

      return getId() == b.getId() &&
          (getParent() == null ? b.getParent() == null : getParent().equals(b.getParent())) &&
          (getCreationDate() == null ? b.getCreationDate() == null : getCreationDate().equals(b.getCreationDate())) &&
          (getName() == null ? b.getName() == null : getName().equals(b.getName())) &&
          (getLastVisitDate() == null ? b.getLastVisitDate() == null : getLastVisitDate().equals(b.getLastVisitDate())) &&
          getPopularity() == b.getPopularity() &&
          fIsErrorLoading == b.fIsErrorLoading &&
          (getProperties() == null ? b.getProperties() == null : getProperties().equals(b.getProperties()));
    }
  }

  @SuppressWarnings("nls")
  @Override
  public synchronized String toLongString() {
    return super.toString() + ", Is Error Loading: " + fIsErrorLoading + ", Belongs " + "to Feed = " + fFeedLink + ")";
  }

  @Override
  @SuppressWarnings("nls")
  public synchronized String toString() {
    return super.toString() + "Belongs to Feed = " + fFeedLink + ")";
  }
}