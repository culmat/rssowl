/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal;

import org.rssowl.core.internal.persist.Mark;
import org.rssowl.core.internal.persist.NewsContainer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.CoreUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An internal subclass of {@link Mark} that implements {@link INewsMark} to
 * provide the news of all bookmarks, bins and saved searches inside a folder.
 * The {@link FolderNewsMark} is created dynamically whenever a folder is opened
 * in the feedview and is never persisted to the DB.
 * <p>
 * TODO This class is not very good in terms of performance because it has to
 * load all news of the folder on the fly to produce the news container. When
 * then opened from the feedview, these news get resolved again and thereby
 * twice.
 * </p>
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class FolderNewsMark extends Mark implements INewsMark {
  private final NewsContainer fNewsContainer;
  private final IFolder fFolder;
  private final AtomicBoolean fIsResolved = new AtomicBoolean(false);

  /**
   * Internal implementation of the <code>ModelReference</code> for the internal
   * Type <code>FolderNewsMark</code>.
   *
   * @author bpasero
   */
  public static final class FolderNewsMarkReference extends ModelReference {

    /**
     * @param id
     */
    public FolderNewsMarkReference(long id) {
      super(id, FolderNewsMark.class);
    }

    @Override
    public IFolder resolve() throws PersistenceException {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * @param folder the {@link IFolder} that makes the contents of this
   * {@link INewsMark}.
   */
  public FolderNewsMark(IFolder folder) {
    super(folder.getId(), folder.getParent(), folder.getName());
    fFolder = folder;
    fNewsContainer = new NewsContainer(Collections.<INews.State, Boolean> emptyMap());
  }

  /**
   * @param news the {@link List} of {@link INews} to add into this
   * {@link INewsMark}.
   */
  public void add(List<INews> news) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    synchronized (this) {
      for (INews item : news) {
        if (item != null && item.getId() != null)
          fNewsContainer.addNews(item);
      }
    }
  }

  /**
   * @param events the {@link Set} of {@link NewsEvent} that provide details
   * about the changes. For each event, find the news to update from its old
   * news state. This is necessary because the {@link NewsContainer} stores news
   * by state id.
   */
  public void update(Set<NewsEvent> events) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    synchronized (this) {
      for (NewsEvent event : events) {
        if (event.getOldNews() != null && CoreUtils.isStateChange(event)) { //Only update for state change since NewsContainer uses states
          INews item = event.getEntity();
          if (item != null && item.getId() != null) {
            if (fNewsContainer.removeNews(event.getOldNews())) //Use old news to pick up old state
              fNewsContainer.addNews(item);
          }
        }
      }
    }
  }

  /**
   * @param events the {@link Set} of {@link NewsEvent} that provide details
   * about the changes. For each event, find the news to delete from its old
   * news state. This is necessary because the {@link NewsContainer} stores news
   * by state id.
   */
  public void remove(Set<NewsEvent> events) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    synchronized (this) {
      for (NewsEvent event : events) {
        if (event.getOldNews() != null) {
          INews item = event.getOldNews(); //Need to use old news to pick up old state
          if (item != null && item.getId() != null)
            fNewsContainer.removeNews(item);
        }
      }
    }
  }

  /**
   * Resolves all news of this news mark that match the given state.
   *
   * @param states the {@link org.rssowl.core.persist.INews.State} of news that
   * should be resolved in this news mark.
   */
  public void resolve(Set<INews.State> states) {
    if (!fIsResolved.get())
      internalResolve(states);
  }

  private void resolveIfNecessary() {
    if (!fIsResolved.get())
      internalResolve(INews.State.getVisible());
  }

  private void internalResolve(Set<State> states) {
    if (!fIsResolved.get()) {
      synchronized (this) {
        if (!fIsResolved.getAndSet(true))
          fillNews(fFolder, states);
      }
    }
  }

  private void fillNews(IFolder folder, Set<State> states) {
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {
      if (child instanceof INewsMark) {
        INewsMark newsmark = (INewsMark) child;
        List<INews> news = newsmark.getNews(states);
        for (INews newsitem : news) {
          if (newsitem != null && newsitem.getId() != null)
            fNewsContainer.addNews(newsitem);
        }
      }

      /* Recursively treat Folders */
      if (child instanceof IFolder)
        fillNews((IFolder) child, states);
    }
  }

  /**
   * @return the {@link IFolder} that serves as input to this {@link INewsMark}.
   */
  public IFolder getFolder() {
    return fFolder;
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#setProperty(java.lang.String, java.io.Serializable)
   */
  @Override
  public synchronized void setProperty(String key, Serializable value) {
    fFolder.setProperty(key, value);
  }

  /*
   * @see org.rssowl.core.internal.persist.Mark#getName()
   */
  @Override
  public synchronized String getName() {
    return fFolder.getName();
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperties()
   */
  @Override
  public synchronized Map<String, Serializable> getProperties() {
    return fFolder.getProperties();
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperty(java.lang.String)
   */
  @Override
  public synchronized Object getProperty(String key) {
    return fFolder.getProperty(key);
  }

  /*
   * @see org.rssowl.core.internal.persist.Mark#getParent()
   */
  /*
   * @see org.rssowl.core.internal.persist.Mark#getParent()
   */
  @Override
  public synchronized IFolder getParent() {
    return fFolder.getParent();
  }

  /**
   * @param news the news to find out if this mark is related to.
   * @return <code>true</code> if the given News belongs to any
   * {@link IBookMark} or {@link INewsBin} of the given {@link IFolder}.
   */
  public boolean isRelatedTo(INews news) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    FeedLinkReference feedRef = news.getFeedReference();
    return isRelatedTo(fFolder, news, feedRef);
  }

  private boolean isRelatedTo(IFolder folder, INews news, FeedLinkReference ref) {
    List<IFolderChild> children = folder.getChildren();

    for (IFolderChild child : children) {

      /* Check contained in Folder */
      if (child instanceof IFolder && isRelatedTo((IFolder) child, news, ref))
        return true;

      /* News could be part of the Feed (but is no copy) */
      else if (news.getParentId() == 0 && child instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) child;
        if (bookmark.getFeedLinkReference().equals(ref))
          return true;
      }

      /* News could be part of Bin (and is a copy) */
      else if (news.getParentId() != 0 && child instanceof INewsBin) {
        INewsBin bin = (INewsBin) child;
        if (bin.getId() == news.getParentId())
          return true;
      }
    }

    return false;
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#containsNews(org.rssowl.core.persist.INews)
   */
  public synchronized boolean containsNews(INews news) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    return fNewsContainer.containsNews(news);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews()
   */
  public synchronized List<INews> getNews() {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    return getNews(EnumSet.allOf(INews.State.class));
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews(java.util.Set)
   */
  public List<INews> getNews(Set<State> states) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    List<NewsReference> newsRefs;
    synchronized (this) {
      newsRefs = fNewsContainer.getNews(states);
    }

    return getNews(newsRefs);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsCount(java.util.Set)
   */
  public synchronized int getNewsCount(Set<State> states) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    return fNewsContainer.getNewsCount(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs()
   */
  public synchronized List<NewsReference> getNewsRefs() {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    return fNewsContainer.getNews();
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs(java.util.Set)
   */
  public synchronized List<NewsReference> getNewsRefs(Set<State> states) {

    /* Resolve Lazily if necessary */
    resolveIfNecessary();

    return fNewsContainer.getNews(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#isGetNewsRefsEfficient()
   */
  public boolean isGetNewsRefsEfficient() {
    return true;
  }

  /*
   * @see org.rssowl.core.persist.IEntity#toReference()
   */
  public ModelReference toReference() {
    return new FolderNewsMarkReference(getId());
  }
}