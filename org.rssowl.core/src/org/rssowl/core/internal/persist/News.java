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
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.MergeUtils;

import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A News is a single entry inside a Feed. The attributes IsRead, IsNew and
 * IsDeleted describe the life-cycle of a News:
 * <ul>
 * <li>IsRead: The News has been marked read</li>
 * <li>IsNew: The News has not been read and was not yet looked at</li>
 * <li>IsDeleted: The News has been deleted by the user or system</li>
 * </ul>
 *
 * @author bpasero
 */
public class News extends AbstractEntity implements INews {

  static final class Lock {

    private final transient ReadWriteLock fLock = new ReentrantReadWriteLock();
    private volatile transient Thread fReadLockThread;

    void acquireWriteLock() {
      if (fReadLockThread == Thread.currentThread()) {
        throw new IllegalStateException("Cannot acquire the write lock from the " +
        		"same thread as the read lock.");
      }
      fLock.writeLock().lock();
    }

    void releaseWriteLock() {
      fLock.writeLock().unlock();
    }

    void acquireReadLock() {
      fLock.readLock().lock();
    }

    void acquireReadLockSpecial() {
      fLock.readLock().lock();
      fReadLockThread = Thread.currentThread();
    }

    void releaseReadLock() {
      fLock.readLock().unlock();
    }

    void releaseReadLockSpecial() {
      fReadLockThread = null;
      fLock.readLock().unlock();
    }

  }

  private String fTitle;

  private transient URI fLink;
  private String fLinkText;

  private String fBaseUri;

  private Date fReceiveDate;
  private Date fPublishDate;
  private Date fModifiedDate;
  private String fDescription;
  private String fComments;
  private String fInReplyTo;
  private boolean fIsFlagged;
  private int fRating;

  private int fStateOrdinal = INews.State.NEW.ordinal();

  private String fGuidValue;
  private boolean fGuidIsPermaLink;
  private transient IGuid fGuid;

  private ISource fSource;

  private ILabel fLabel;

  private String fFeedLink;
  private transient FeedLinkReference fFeedLinkReference;

  private IPerson fAuthor;

  private List<IAttachment> fAttachments;
  private List<ICategory> fCategories;

  private transient final Lock fLock = new Lock();

  /**
   * Constructor used by <code>DefaultModelFactory</code>
   *
   * @param feed The Feed this News is belonging to.
   */
  public News(IFeed feed) {
    super(null);
    Assert.isNotNull(feed, "The type News requires a Feed that is not NULL"); //$NON-NLS-1$
    fFeedLink = feed.getLink().toString();
    fReceiveDate = new Date();
    init();
  }

  /**
   * Creates a new Element of the Type News
   * <p>
   * TODO Consider whether feed can be null
   * </p>
   *
   * @param id The unique id of the News.
   * @param feed The Feed this News belongs to.
   * @param receiveDate The Date this News was received.
   */
  public News(Long id, IFeed feed, Date receiveDate) {
    super(id);
    Assert.isNotNull(feed, "The type News requires a Feed that is not NULL"); //$NON-NLS-1$
    fFeedLink = feed.getLink().toString();
    Assert.isNotNull(receiveDate, "The type News requires a ReceiveDate that is not NULL"); //$NON-NLS-1$
    fReceiveDate = receiveDate;
    init();
  }

  /**
   * Default constructor for deserialization
   */
  protected News() {
  // As per javadoc
  }

  /**
   * Initialises object after deserialization. Should not be used otherwise.
   */
  public final void init() {
    fLock.acquireWriteLock();
    try {
      if (fLink == null) {
        fLink = createURI(fLinkText);
        fFeedLinkReference = new FeedLinkReference(createURI(fFeedLink));
        if (fGuidValue != null) {
          fGuid = new Guid(fGuidValue);
          fGuid.setPermaLink(fGuidIsPermaLink);
        }
      }
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /**
   * Returns the Lock that is used by the public methods that do not
   * modify this object.
   *
   * This method should only be used in very specific circumstances. Avoid
   * if possible.
   *
   * @return Lock used during operations that do not modify this object.
   */
  public final void acquireReadLockSpecial() {
    fLock.acquireReadLockSpecial();
  }

  public final void releaseReadLockSpecial() {
    fLock.releaseReadLockSpecial();
  }

  private <T>Boolean isEquivalentCompare(T o1, T o2) {
    if ((o1 == null) && (o2 == null))
      return null;

    return Boolean.valueOf(equals(o1, o2));
  }

  private boolean equals(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  public boolean isEquivalent(INews other) {
    fLock.acquireReadLock();
    try {
      Assert.isNotNull(other, "other cannot be null"); //$NON-NLS-1$
      String guidValue = (getGuid() == null ? null : getGuid().getValue());

      String otherGuidValue = (other.getGuid() == null ? null : other.getGuid().getValue());

      Boolean guidMatch = isEquivalentCompare(guidValue, otherGuidValue);
      if (guidMatch != null) {
        if (guidMatch.equals(Boolean.TRUE))
          return true;

        return false;
      }

      URI newsItemLink = other.getLink();
      Boolean linkMatch = isEquivalentCompare(getLink(), newsItemLink);
      if (linkMatch != null) {
        if (linkMatch.equals(Boolean.TRUE))
          return true;

        return false;
      }
      if (!getFeedReference().equals(other.getFeedReference()))
        return false;

      Boolean titleMatch = isEquivalentCompare(getTitle(), other.getTitle());
      if (titleMatch != null && titleMatch.equals(Boolean.TRUE))
        return true;

      return false;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#addAttachment(org.rssowl.core.model.types.IAttachment)
   */
  public void addAttachment(IAttachment attachment) {
    fLock.acquireWriteLock();
    try {
      if (fAttachments == null)
        fAttachments = new ArrayList<IAttachment>();

      Assert.isNotNull(attachment, "Exception adding NULL as Attachment into News"); //$NON-NLS-1$

      /* Rule: Child needs to know about its new parent already! */
      Assert.isTrue(equals(attachment.getNews()), "The Attachment has a different News set!"); //$NON-NLS-1$
      fAttachments.add(attachment);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAttachments()
   */
  public List<IAttachment> getAttachments() {
    fLock.acquireReadLock();
    try {
      if (fAttachments == null)
        return Collections.emptyList();
      return Collections.unmodifiableList(fAttachments);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAuthor()
   */
  public IPerson getAuthor() {
    fLock.acquireReadLock();
    try {
      return fAuthor;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setAuthor(org.rssowl.core.model.types.IPerson)
   */
  public void setAuthor(IPerson author) {
    fLock.acquireWriteLock();
    try {
      fAuthor = author;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getDescription()
   */
  public String getDescription() {
    fLock.acquireReadLock();
    try {
      return fDescription;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    fLock.acquireWriteLock();
    try {
      fDescription = description;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getLink()
   */
  public URI getLink() {
    fLock.acquireReadLock();
    try {
      return fLink;
    } finally {
     fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setLink(java.lang.String)
   */
  public void setLink(URI link) {
    fLock.acquireWriteLock();
    try {
      fLinkText = link == null ? null : link.toString();
      fLink = link;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getPublishDate()
   */
  public Date getPublishDate() {
    fLock.acquireReadLock();
    try {
      return fPublishDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setPublishDate(java.util.Date)
   */
  public void setPublishDate(Date publishDate) {
    fLock.acquireWriteLock();
    try {
      fPublishDate = publishDate;
    } finally	{
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getTitle()
   */
  public String getTitle() {
    fLock.acquireReadLock();
    try {
      return fTitle;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    fLock.acquireWriteLock();
    try {
      fTitle = title;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getFeed()
   */
  public FeedLinkReference getFeedReference() {
    fLock.acquireReadLock();
    try {
      return fFeedLinkReference;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setReceiveDate(java.util.Date)
   */
  public void setReceiveDate(Date receiveDate) {
    fLock.acquireWriteLock();
    try {
      fReceiveDate = receiveDate;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getReceiveDate()
   */
  public Date getReceiveDate() {
    fLock.acquireReadLock();
    try {
      return fReceiveDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setComments(java.lang.String)
   */
  public void setComments(String comments) {
    fLock.acquireWriteLock();
    try {
      fComments = comments;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setGuid(org.rssowl.core.model.types.IGuid)
   */
  public void setGuid(IGuid guid) {
    fLock.acquireWriteLock();
    try {
      fGuid = guid;
      fGuidValue = (guid == null ? null : guid.getValue());
      fGuidIsPermaLink = (guid == null ? false : guid.isPermaLink());
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setSource(org.rssowl.core.model.types.ISource)
   */
  public void setSource(ISource source) {
    fLock.acquireWriteLock();
    try {
      fSource = source;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setInReplyTo(java.lang.String)
   */
  public void setInReplyTo(String guid) {
    fLock.acquireWriteLock();
    try {
      fInReplyTo = guid;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setModifiedDate(java.util.Date)
   */
  public void setModifiedDate(Date modifiedDate) {
    fLock.acquireWriteLock();
    try {
      fModifiedDate = modifiedDate;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getModifiedDate()
   */
  public Date getModifiedDate() {
    fLock.acquireReadLock();
    try {
      return fModifiedDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#addCategory(org.rssowl.core.model.types.ICategory)
   */
  public void addCategory(ICategory category) {
    fLock.acquireWriteLock();
    try {
      if (fCategories == null)
        fCategories = new ArrayList<ICategory>();
      fCategories.add(category);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getComments()
   */
  public String getComments() {
    fLock.acquireReadLock();
    try {
      return fComments;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#isFlagged()
   */
  public boolean isFlagged() {
    fLock.acquireReadLock();
    try {
      return fIsFlagged;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setFlagged(boolean)
   */
  public void setFlagged(boolean isFlagged) {
    fLock.acquireWriteLock();
    try {
      fIsFlagged = isFlagged;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getLabel()
   */
  public ILabel getLabel() {
    fLock.acquireReadLock();
    try {
      return fLabel;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setLabel(org.rssowl.core.model.types.impl.Label)
   */
  public void setLabel(ILabel label) {
    fLock.acquireWriteLock();
    try {
      fLabel = label;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getGuid()
   */
  public IGuid getGuid() {
    fLock.acquireReadLock();
    try {
      return fGuid;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setBase(java.net.URI)
   */
  public void setBase(URI baseUri) {
    fLock.acquireWriteLock();
    try {
      fBaseUri = getURIText(baseUri);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getBase()
   */
  public URI getBase() {
    fLock.acquireReadLock();
    try {
      return createURI(fBaseUri);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getCategories()
   */
  public List<ICategory> getCategories() {
    fLock.acquireReadLock();
    try {
      if (fCategories == null)
        return Collections.emptyList();
      return Collections.unmodifiableList(fCategories);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setState(org.rssowl.core.model.types.INews.State)
   */
  public void setState(State state) {
    Assert.isNotNull(state, "state cannot be null"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      fStateOrdinal = state.ordinal();
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getState()
   */
  public State getState() {
    fLock.acquireReadLock();
    try {
      return INews.State.getState(fStateOrdinal);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setRating(int)
   */
  public void setRating(int rating) {
    fLock.acquireWriteLock();
    try {
      fRating = rating;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getRating()
   */
  public int getRating() {
    fLock.acquireReadLock();
    try {
      return fRating;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getSource()
   */
  public ISource getSource() {
    fLock.acquireReadLock();
    try {
      return fSource;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getInReplyTo()
   */
  public String getInReplyTo() {
    fLock.acquireReadLock();
    try {
      return fInReplyTo;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#isVisible()
   */
  public boolean isVisible() {
    /* No need to lock since getState() has a read lock */
    INews.State state = getState();
    return State.getVisible().contains(state);
  }

  @SuppressWarnings("nls")
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\n\n****************************** News ******************************\n");
    fLock.acquireReadLock();
    try {
      str.append("\nNews ID: ").append(getId());
      if (getTitle() != null)
        str.append("\nTitle: ").append(getTitle());
      if (getLink() != null)
        str.append("\nLink: ").append(getLink());
    } finally {
      fLock.releaseReadLock();
    }
    return str.toString();
  }

  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  @SuppressWarnings("nls")
  public String toLongString() {
    StringBuilder str = new StringBuilder();

    str.append("\n\n****************************** News ******************************\n");
    fLock.acquireReadLock();
    try {
      str.append("\nNews ID: ").append(getId());
      if (fFeedLink != null)
        str.append("\nFeed Link: ").append(fFeedLink);
      str.append("\nState: ").append(getState());
      if (getTitle() != null)
        str.append("\nTitle: ").append(getTitle());
      if (getLink() != null)
        str.append("\nLink: ").append(getLink());
      if (getBase() != null)
        str.append("\nBase URI: ").append(getBase());
      if (getDescription() != null)
        str.append("\nDescription: ").append(getDescription());
      str.append("\nRating: ").append(getRating());
      if (getPublishDate() != null)
        str.append("\nPublish Date: ").append(DateFormat.getDateTimeInstance().format(getPublishDate()));
      if (getReceiveDate() != null)
        str.append("\nReceive Date: ").append(DateFormat.getDateTimeInstance().format(getReceiveDate()));
      if (getModifiedDate() != null)
        str.append("\nModified Date: ").append(DateFormat.getDateTimeInstance().format(getModifiedDate()));
      if (getAuthor() != null)
        str.append("\nAuthor: ").append(getAuthor());
      if (getComments() != null)
        str.append("\nComments: ").append(getComments());
      if (getGuid() != null)
        str.append("\nGUID: ").append(getGuid());
      if (getSource() != null)
        str.append("\nSource: ").append(getSource());
      if (getInReplyTo() != null)
        str.append("\nIn Reply To: ").append(getInReplyTo());
      if (getLabel() != null)
        str.append("\nLabel: ").append(getLabel());
      str.append("\nAttachments: ").append(getAttachments());
      str.append("\nCategories: ").append(getCategories());
      str.append("\nIs Flagged: ").append(fIsFlagged);
      str.append("\nProperties: ").append(getProperties());
    } finally {
      fLock.releaseReadLock();
    }
    return str.toString();
  }

  /**
   * @param news
   * @return whether <code>news</code> is identical to this object.
   */
  public boolean isIdentical(INews news) {
    if (this == news)
      return true;

    if (!(news instanceof News))
      return false;

    News n = (News) news;
    fLock.acquireReadLock();
    n.fLock.acquireReadLock();
    try {
      return getId().equals(n.getId()) &&
          fFeedLink.equals(n.fFeedLink) &&
          simpleFieldsEqual(news) &&
          (fReceiveDate == null ? n.fReceiveDate == null : fReceiveDate.equals(n.fReceiveDate)) &&
          (getGuid() == null ? n.getGuid() == null : getGuid().equals(n.getGuid())) &&
          (fSource == null ? n.fSource == null : fSource.equals(n.fSource)) &&
          (fInReplyTo == null ? n.fInReplyTo == null : fInReplyTo.equals(n.fInReplyTo)) &&
          (fLabel == null ? n.fLabel == null : fLabel.equals(n.fLabel)) &&
          (getAuthor() == null ? n.getAuthor() == null : getAuthor().equals(n.getAuthor())) &&
          getAttachments().equals(n.getAttachments()) &&
          getCategories().equals(n.getCategories()) &&
          getState() == n.getState() && fIsFlagged == n.fIsFlagged && fRating == n.fRating &&
          (getProperties() == null ? n.getProperties() == null : getProperties().equals(n.getProperties()));
    } finally {
      fLock.releaseReadLock();
      n.fLock.releaseReadLock();
    }

  }

  private boolean simpleFieldsEqual(INews news) {
    return MergeUtils.equals(getBase(), news.getBase()) &&
        MergeUtils.equals(fComments, news.getComments()) &&
        MergeUtils.equals(getDescription(), news.getDescription()) &&
        MergeUtils.equals(getLink(), news.getLink()) &&
        MergeUtils.equals(fModifiedDate, news.getModifiedDate()) &&
        MergeUtils.equals(fPublishDate, news.getPublishDate()) &&
        MergeUtils.equals(fInReplyTo, news.getInReplyTo()) &&
        MergeUtils.equals(fTitle, news.getTitle());
  }

  public MergeResult merge(INews news) {
    Assert.isNotNull(news, "news cannot be null"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      boolean updated = mergeState(news);

      MergeResult result = new MergeResult();
      /* Merge all items except for feed and id */
      updated |= processListMergeResult(result, mergeAttachments(news.getAttachments()));
      updated |= processListMergeResult(result, mergeCategories(news.getCategories()));
      updated |= processListMergeResult(result, mergeAuthor(news.getAuthor()));
      updated |= processListMergeResult(result, mergeGuid(news.getGuid()));
      updated |= processListMergeResult(result, mergeSource(news.getSource()));
      updated |= !simpleFieldsEqual(news);

      setBase(news.getBase());
      fComments = news.getComments();
      setDescription(news.getDescription());
      setLink(news.getLink());
      fModifiedDate = news.getModifiedDate();
      fPublishDate = news.getPublishDate();
      fTitle = news.getTitle();
      fInReplyTo = news.getInReplyTo();

      ComplexMergeResult<?> propertiesResult = MergeUtils.mergeProperties(this, news);
      if (updated || propertiesResult.isStructuralChange()) {
        result.addUpdatedObject(this);
        result.addAll(propertiesResult);
      }
      return result;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  private boolean mergeState(INews news) {
    State thisState = getState();
    State otherState = news.getState();
    if (thisState != otherState && otherState != State.NEW) {
      setState(otherState);
      return true;
    }
    if (isUpdated(news)) {
      setState(State.UPDATED);
      return true;
    }
    return false;
  }

  //FIXME Need to consolidate description comparison so that we only do it once
  //per merge.
  private boolean isUpdated(INews news) {
    State thisState = getState();
    if (thisState != State.READ && thisState != State.UNREAD)
      return false;

    String title = news.getTitle();
    if (!(fTitle == null ? title == null : fTitle.equals(title)))
      return true;

    return false;
  }

  private ComplexMergeResult<IGuid> mergeGuid(IGuid guid) {
    ComplexMergeResult<IGuid> mergeResult = MergeUtils.merge(getGuid(), guid);
    fGuid = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<ISource> mergeSource(ISource source) {
    ComplexMergeResult<ISource> mergeResult = MergeUtils.merge(getSource(), source);
    fSource = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<IPerson> mergeAuthor(IPerson author) {
    ComplexMergeResult<IPerson> mergeResult = MergeUtils.merge(getAuthor(), author);
    fAuthor = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<List<ICategory>> mergeCategories(List<ICategory> categories) {
    Comparator<ICategory> comparator = new Comparator<ICategory>() {

      public int compare(ICategory o1, ICategory o2) {
        if (o1.getName() == null ? o2.getName() == null : o1.getName().equals(o2.getName())) {
          return 0;
        }
        return -1;
      }

    };
    ComplexMergeResult<List<ICategory>> mergeResult = MergeUtils.merge(fCategories, categories, comparator, null);
    fCategories = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<List<IAttachment>> mergeAttachments(List<IAttachment> attachments) {
    Comparator<IAttachment> comparator = new Comparator<IAttachment>() {
      public int compare(IAttachment o1, IAttachment o2) {
        if (o1.getLink().equals(o2.getLink())) {
          return 0;
        }
        return -1;
      }
    };
    ComplexMergeResult<List<IAttachment>> mergeResult = MergeUtils.merge(fAttachments, attachments, comparator, this);
    fAttachments = mergeResult.getMergedObject();
    return mergeResult;
  }

  public void setParent(IFeed feed) {
    Assert.isNotNull(feed, "feed"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      this.fFeedLink = feed.getLink().toString();
      this.fFeedLinkReference = new FeedLinkReference(feed.getLink());
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public void removeAttachment(IAttachment attachment) {
    fLock.acquireWriteLock();
    try {
      if (fAttachments != null)
        fAttachments.remove(attachment);
    } finally {
      fLock.releaseWriteLock();
    }
  }
}