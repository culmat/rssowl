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
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
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
  private transient State fState;

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

  private final transient ReadWriteLock fReadWriteLock = new ReentrantReadWriteLock();
  private final transient Lock fWriteLock = fReadWriteLock.writeLock();
  private final transient Lock fReadLock = fReadWriteLock.readLock();

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
  }

  /**
   * Default constructor for deserialization
   */
  protected News() {
  // As per javadoc
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
  public final Lock getReadLock() {
    return fReadLock;
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
    fReadLock.lock();
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
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#addAttachment(org.rssowl.core.model.types.IAttachment)
   */
  public void addAttachment(IAttachment attachment) {
    fWriteLock.lock();
    try {
      if (fAttachments == null)
        fAttachments = new ArrayList<IAttachment>();

      Assert.isNotNull(attachment, "Exception adding NULL as Attachment into News"); //$NON-NLS-1$

      /* Rule: Child needs to know about its new parent already! */
      Assert.isTrue(equals(attachment.getNews()), "The Attachment has a different News set!"); //$NON-NLS-1$
      fAttachments.add(attachment);
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAttachments()
   */
  public List<IAttachment> getAttachments() {
    fReadLock.lock();
    try {
      if (fAttachments == null)
        return Collections.emptyList();
      return Collections.unmodifiableList(fAttachments);
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAuthor()
   */
  public IPerson getAuthor() {
    fReadLock.lock();
    try {
      return fAuthor;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setAuthor(org.rssowl.core.model.types.IPerson)
   */
  public void setAuthor(IPerson author) {
    fWriteLock.lock();
    try {
      fAuthor = author;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getDescription()
   */
  public String getDescription() {
    fReadLock.lock();
    try {
      return fDescription;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    fWriteLock.lock();
    try {
      fDescription = description;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getLink()
   */
  public URI getLink() {
    fReadLock.lock();
    try {
      if (fLink == null && fLinkText != null) {
        try {
          fLink = new URI(fLinkText);
        } catch (URISyntaxException e) {
          throw new IllegalStateException("Somehow an illegal URI was stored.", e); //$NON-NLS-1$
        }
      }
    } finally {
     fReadLock.unlock();
    }
    return fLink;
  }

  /*
   * @see org.rssowl.core.model.types.INews#setLink(java.lang.String)
   */
  public void setLink(URI link) {
    fWriteLock.lock();
    try {
      fLinkText = link == null ? null : link.toString();
      fLink = link;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getPublishDate()
   */
  public Date getPublishDate() {
    fReadLock.lock();
    try {
      return fPublishDate;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setPublishDate(java.util.Date)
   */
  public void setPublishDate(Date publishDate) {
    fWriteLock.lock();
    try {
      fPublishDate = publishDate;
    } finally	{
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getTitle()
   */
  public String getTitle() {
    fReadLock.lock();
    try {
      return fTitle;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    fWriteLock.lock();
    try {
      fTitle = title;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getFeed()
   */
  public FeedLinkReference getFeedReference() {
    fReadLock.lock();
    try {
      if (fFeedLinkReference == null) {
        try {
          fFeedLinkReference = new FeedLinkReference(new URI(fFeedLink));
        } catch (URISyntaxException e) {
          throw new IllegalStateException("A Malformed URI was stored somehow", e); //$NON-NLS-1$
        }
      }
      return fFeedLinkReference;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setReceiveDate(java.util.Date)
   */
  public void setReceiveDate(Date receiveDate) {
    fWriteLock.lock();
    try {
      fReceiveDate = receiveDate;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getReceiveDate()
   */
  public Date getReceiveDate() {
    fReadLock.lock();
    try {
      return fReceiveDate;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setComments(java.lang.String)
   */
  public void setComments(String comments) {
    fWriteLock.lock();
    try {
      fComments = comments;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setGuid(org.rssowl.core.model.types.IGuid)
   */
  public void setGuid(IGuid guid) {
    fWriteLock.lock();
    try {
      fGuid = guid;
      fGuidValue = (guid == null ? null : guid.getValue());
      fGuidIsPermaLink = (guid == null ? false : guid.isPermaLink());
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setSource(org.rssowl.core.model.types.ISource)
   */
  public void setSource(ISource source) {
    fWriteLock.lock();
    try {
      fSource = source;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setInReplyTo(java.lang.String)
   */
  public void setInReplyTo(String guid) {
    fWriteLock.lock();
    try {
      fInReplyTo = guid;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setModifiedDate(java.util.Date)
   */
  public void setModifiedDate(Date modifiedDate) {
    fWriteLock.lock();
    try {
      fModifiedDate = modifiedDate;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getModifiedDate()
   */
  public Date getModifiedDate() {
    fReadLock.lock();
    try {
      return fModifiedDate;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#addCategory(org.rssowl.core.model.types.ICategory)
   */
  public void addCategory(ICategory category) {
    fWriteLock.lock();
    try {
      if (fCategories == null)
        fCategories = new ArrayList<ICategory>();
      fCategories.add(category);
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getComments()
   */
  public String getComments() {
    fReadLock.lock();
    try {
      return fComments;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#isFlagged()
   */
  public boolean isFlagged() {
    fReadLock.lock();
    try {
      return fIsFlagged;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setFlagged(boolean)
   */
  public void setFlagged(boolean isFlagged) {
    fWriteLock.lock();
    try {
      fIsFlagged = isFlagged;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getLabel()
   */
  public ILabel getLabel() {
    fReadLock.lock();
    try {
      return fLabel;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setLabel(org.rssowl.core.model.types.impl.Label)
   */
  public void setLabel(ILabel label) {
    fWriteLock.lock();
    try {
      fLabel = label;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getGuid()
   */
  public IGuid getGuid() {
    fReadLock.lock();
    try {
      if (fGuid == null && fGuidValue != null) {
        fGuid = new Guid(fGuidValue);
        fGuid.setPermaLink(fGuidIsPermaLink);
      }
      return fGuid;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setBase(java.net.URI)
   */
  public void setBase(URI baseUri) {
    fWriteLock.lock();
    try {
      fBaseUri = getURIText(baseUri);
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getBase()
   */
  public URI getBase() {
    fReadLock.lock();
    try {
      return createURI(fBaseUri);
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getCategories()
   */
  public List<ICategory> getCategories() {
    fReadLock.lock();
    try {
      if (fCategories == null)
        return Collections.emptyList();
      return Collections.unmodifiableList(fCategories);
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setState(org.rssowl.core.model.types.INews.State)
   */
  public void setState(State state) {
    Assert.isNotNull(state, "state cannot be null"); //$NON-NLS-1$
    fWriteLock.lock();
    try {
      fStateOrdinal = state.ordinal();
      fState = state;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getState()
   */
  public State getState() {
    fReadLock.lock();
    try {
      if (fState == null)
        fState = INews.State.values()[fStateOrdinal];

      return fState;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setRating(int)
   */
  public void setRating(int rating) {
    fWriteLock.lock();
    try {
      fRating = rating;
    } finally {
      fWriteLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getRating()
   */
  public int getRating() {
    fReadLock.lock();
    try {
      return fRating;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getSource()
   */
  public ISource getSource() {
    fReadLock.lock();
    try {
      return fSource;
    } finally {
      fReadLock.unlock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getInReplyTo()
   */
  public String getInReplyTo() {
    fReadLock.lock();
    try {
      return fInReplyTo;
    } finally {
      fReadLock.unlock();
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
    fReadLock.lock();
    try {
      str.append("\nNews ID: ").append(getId());
      if (getTitle() != null)
        str.append("\nTitle: ").append(getTitle());
      if (getLink() != null)
        str.append("\nLink: ").append(getLink());
    } finally {
      fReadLock.unlock();
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
    fReadLock.lock();
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
      fReadLock.unlock();
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
    fReadLock.lock();
    n.getReadLock();
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
      fReadLock.unlock();
      n.getReadLock().unlock();
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
    fWriteLock.lock();
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
      fWriteLock.unlock();
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
    fWriteLock.lock();
    try {
      this.fFeedLink = feed.getLink().toString();

      /*
       * Current value is not valid anymore, but don't create a new one until
       * getFeedReference is called.
       */
      this.fFeedLinkReference = null;
    } finally {
      fWriteLock.unlock();
    }
  }

  public void removeAttachment(IAttachment attachment) {
    fWriteLock.lock();
    try {
      if (fAttachments != null)
        fAttachments.remove(attachment);
    } finally {
      fWriteLock.unlock();
    }
  }
}