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
package org.rssowl.core.model.internal.db4o;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.ReparentInfo;
import org.rssowl.core.model.RetentionStrategy;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.internal.types.BookMark;
import org.rssowl.core.model.internal.types.Category;
import org.rssowl.core.model.internal.types.MergeResult;
import org.rssowl.core.model.internal.types.News;
import org.rssowl.core.model.internal.types.Person;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.types.IAttachment;
import org.rssowl.core.model.types.IBookMark;
import org.rssowl.core.model.types.ICategory;
import org.rssowl.core.model.types.IConditionalGet;
import org.rssowl.core.model.types.IFeed;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.core.model.types.ILabel;
import org.rssowl.core.model.types.IMark;
import org.rssowl.core.model.types.INews;
import org.rssowl.core.model.types.IPerson;
import org.rssowl.core.model.types.ISearchMark;
import org.rssowl.core.model.types.INews.State;
import org.rssowl.core.util.StringUtils;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * db4o implementation of IApplicationLayer
 */
public class ApplicationLayerImpl implements IApplicationLayer {

  private ObjectContainer fDb = DBManager.getDefault().getObjectContainer();
  private ReadWriteLock fLock;
  private Lock fWriteLock;

  /**
   * Creates an instance of this class.
   */
  public ApplicationLayerImpl() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
        fLock = event.getLock();
        fWriteLock = fLock.writeLock();
      }
      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }

  public final void handleFeedReload(IBookMark bookMark, IFeed emptyFeed,
      IConditionalGet conditionalGet, boolean deleteConditionalGet) {
    fWriteLock.lock();
    try {
      /* Resolve reloaded Feed */
      IFeed feed = bookMark.getFeedLinkReference().resolve();

      /* Copy over Properties to reloaded Feed to keep them */
      Map<String, ? > feedProperties = feed.getProperties();
      if (feedProperties != null) {
        feedProperties.entrySet();
        for (Map.Entry<String, ? > entry : feedProperties.entrySet())
          emptyFeed.setProperty(entry.getKey(), entry.getValue());
      }

      /* Merge with existing (remember number of added new news) */
      List<INews> newNewsBeforeMerge = feed.getNewsByStates(EnumSet.of(INews.State.NEW));
      MergeResult mergeResult = feed.mergeAndCleanUp(emptyFeed);
      List<INews> newNewsAdded = getNewNewsAdded(feed, newNewsBeforeMerge);
      updateStateOfUnsavedNewNews(newNewsAdded);

      /* Retention Policy */
      List<INews> deletedNews = RetentionStrategy.process(bookMark, feed, newNewsAdded.size());

      for (INews news : deletedNews)
        mergeResult.addUpdatedObject(news);

      saveFeed(mergeResult);

      /* Update Conditional GET */
      if (conditionalGet != null) {
        if (deleteConditionalGet)
          fDb.delete(conditionalGet);
        else
          fDb.ext().set(conditionalGet, 1);
      }
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  private List<INews> getNewNewsAdded(IFeed feed, List<INews> newNewsBeforeMerge) {
    List<INews> newNewsAdded = feed.getNewsByStates(EnumSet.of(INews.State.NEW));
    newNewsAdded.removeAll(newNewsBeforeMerge);
    return newNewsAdded;
  }

  @SuppressWarnings("unchecked")
  public final List<IBookMark> loadBookMarks(FeedLinkReference feedRef) {
    try {
      Query query = fDb.query();
      query.constrain(BookMark.class);
      query.descend("fFeedLink").constrain(feedRef.getLink().toString()); //$NON-NLS-1$
      ObjectSet<IBookMark> marks = query.execute();
      activateAll(marks);
      return new ArrayList<IBookMark>(marks);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public final List<IFolder> loadRootFolders() {
    try {
      Query query = fDb.query();
      query.constrain(IFolder.class);
      query.descend("fParent").constrain(null); //$NON-NLS-1$
      ObjectSet<IFolder> folders = query.execute();
      activateAll(folders);
      return new ArrayList<IFolder>(folders);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  private <T> List<T> activateAll(List<T> list) {
    for (T o : list)
      fDb.ext().activate(o, Integer.MAX_VALUE);

    return list;
  }

  public void reparent(List<ReparentInfo<IFolder, IFolder>> folderInfos,
      List<ReparentInfo<IMark, IFolder>> markInfos) {

    Assert.isLegal(folderInfos != null || markInfos != null, "Either folderInfos or markInfos must be non-null"); //$NON-NLS-1$
    fWriteLock.lock();
    try {
      List<FolderEvent> folderEvents = createFolderEvents(folderInfos);

      List<BookMarkEvent> bookMarkEvents = Collections.emptyList();
      List<SearchMarkEvent> searchMarkEvents = Collections.emptyList();
      if (markInfos != null) {
        bookMarkEvents = new ArrayList<BookMarkEvent>(markInfos.size() * (2 / 3));
        searchMarkEvents = new ArrayList<SearchMarkEvent>(markInfos.size() / 3);
        fillMarkEvents(markInfos, bookMarkEvents, searchMarkEvents);
      }

      for (FolderEvent event : folderEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getParent();
        if (newParent == null)
          fDb.set(event.getEntity());
        else
          fDb.set(newParent);
      }

      for (BookMarkEvent event : bookMarkEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getFolder();
        fDb.set(newParent);
      }

      for (SearchMarkEvent event : searchMarkEvents) {
        fDb.set(event.getOldParent());
        IFolder newParent = event.getEntity().getFolder();
        fDb.set(newParent);
      }

      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();

  }

  private List<FolderEvent> createFolderEvents(List<ReparentInfo<IFolder, IFolder>> folderInfos) {
    if (folderInfos == null)
      return Collections.emptyList();

    List<FolderEvent> folderEvents = new ArrayList<FolderEvent>(folderInfos.size());
    for (ReparentInfo<IFolder, IFolder> folderInfo : folderInfos) {
      IFolder folder = folderInfo.getObject();
      IFolder newParent = folderInfo.getNewParent();
      IFolder oldParent = folder.getParent();
      IFolder newPosition = folderInfo.getNewPosition();
      synchronized (folder) {
        removeFolderFromParent(folder);
        addFolder(newParent, folder);
        if (newPosition != null) {
          List<IFolder> folderList = new ArrayList<IFolder>(1);
          folderList.add(folder);
          newParent.reorderFolders(folderList, newPosition, folderInfo.isAfter().booleanValue());
        }
      }
      FolderEvent eventTemplate = new FolderEvent(folder, oldParent, true);
      folderEvents.add(eventTemplate);
      DBHelper.putEventTemplate(eventTemplate);
    }
    return folderEvents;
  }

  private void addFolder(IFolder parent, IFolder child) {
    child.setParent(parent);
    /* The new parent may be null. It becomes a root folder */
    if (parent != null)
      parent.addFolder(child);
  }

  private IFolder removeFolderFromParent(IFolder folder) {
    IFolder oldParent = folder.getParent();
    oldParent.removeFolder(folder);
    return oldParent;
  }

  private IFolder removeMarkFromParent(IMark mark) {
    IFolder oldParent = mark.getFolder();
    oldParent.removeMark(mark);
    return oldParent;
  }

  private void addMarkToFolder(IFolder parent, IMark child) {
    child.setFolder(parent);
    parent.addMark(child);
  }

  private void fillMarkEvents(List<ReparentInfo<IMark, IFolder>> markInfos,
      List<BookMarkEvent> bookMarkEvents, List<SearchMarkEvent> searchMarkEvents) {

    for (ReparentInfo<IMark, IFolder> markInfo : markInfos) {
      IMark mark = markInfo.getObject();
      IFolder newParent = markInfo.getNewParent();
      IFolder oldParent = mark.getFolder();
      IMark newPosition = markInfo.getNewPosition();
      synchronized (mark) {
        removeMarkFromParent(mark);
        addMarkToFolder(newParent, mark);
        if (newPosition != null) {
          List<IMark> markList = new ArrayList<IMark>(1);
          markList.add(mark);
          newParent.reorderMarks(markList, newPosition, markInfo.isAfter().booleanValue());
        }
      }
      if (mark instanceof IBookMark) {
        BookMarkEvent event = new BookMarkEvent((IBookMark) mark, oldParent, true);
        bookMarkEvents.add(event);
        DBHelper.putEventTemplate(event);
      }
      else if (mark instanceof ISearchMark) {
        SearchMarkEvent event = new SearchMarkEvent((ISearchMark) mark, oldParent, true);
        searchMarkEvents.add(event);
        DBHelper.putEventTemplate(event);
      }
      else
        throw new IllegalArgumentException("Uknown mark subclass found: " + mark.getClass()); //$NON-NLS-1$

    }
  }

  public final IFeed loadFeed(URI link) {
    return DBHelper.loadFeed(fDb, link, Integer.MAX_VALUE);
  }

  // FIXME Not sure if this makes sense anymore. If we decide to keep it, try
  // to make it more efficient
  public final FeedReference loadFeedReference(URI link) {
    IFeed feed = DBHelper.loadFeed(fDb, link, null);
    if (feed == null) {
      return null;
    }
    return new FeedReference(feed.getId());
  }

  private void updateStateOfUnsavedNewNews(List<INews> news) {
    for (INews newsItem : news) {
      List<INews> equivalentNews = Collections.emptyList();
      if (newsItem.getGuid() != null)
        equivalentNews = getNewsFromGuid(newsItem);
      else if (newsItem.getLink() != null)
        equivalentNews = getNewsFromLink(newsItem);

      if (!equivalentNews.isEmpty())
        newsItem.setState(equivalentNews.get(0).getState());
    }
  }

  public final void setNewsState(List<INews> news, State state,
      boolean affectEquivalentNews, boolean force) {
    if (news.isEmpty())
      return;
    fWriteLock.lock();
    try {
      Set<INews> changedNews;

      if (affectEquivalentNews) {
        /*
         * Give extra 25% size to take into account news that have same guid or
         * link.
         */
        int capacity = news.size() + (news.size() / 4);
        changedNews = new HashSet<INews>(capacity);
        for (INews newsItem : news) {
          if (newsItem.getId() == null)
            throw new IllegalArgumentException("newsItem was never saved to the database"); //$NON-NLS-1$

          List<INews> equivalentNews;

          if (newsItem.getGuid() != null) {
            equivalentNews = getNewsFromGuid(newsItem);
            if (equivalentNews.isEmpty()) {
              throwIllegalStateException("No news were found with guid: " + //$NON-NLS-1$
                  newsItem.getGuid().getValue(), newsItem);
            }
          }
          else if (newsItem.getLink() != null) {
            equivalentNews = getNewsFromLink(newsItem);
            if (equivalentNews.isEmpty()) {
              throwIllegalStateException("No news were found with link: " + //$NON-NLS-1$
                  newsItem.getLink().toString(), newsItem);
            }
          }
          else
            equivalentNews = Collections.singletonList(newsItem);

          changedNews.addAll(setState(equivalentNews, state, force));
        }
      } else {
        changedNews = setState(news, state, force);
      }
      saveNews(changedNews, 1, false);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  private void throwIllegalStateException(String message, INews newsItem) {
    News dbNews = (News) fDb.ext().peekPersisted(newsItem, 2, true);
    if (dbNews == null)
      throw new IllegalArgumentException("The news has been deleted from the persistence layer: " + newsItem);

    throw new IllegalStateException(message + ". This news in the db looks like: "  //$NON-NLS-1$
        + dbNews.toLongString());
  }

  @SuppressWarnings("unchecked")
  private List<INews> getNewsFromLink(INews newsItem) {
    Query query = fDb.query();
    query.constrain(News.class);
    query.descend("fLinkText").constrain(newsItem.getLink().toString()); //$NON-NLS-1$
    return activateAll(query.execute());
  }

  private Set<INews> setState(Collection<INews> news, State state, boolean force) {
    Set<INews> changedNews = new HashSet<INews>(news.size());
    for (INews newsItem : news) {
      if (newsItem.getState() != state || force) {
        newsItem.setState(state);
        changedNews.add(newsItem);
      }
    }
    return changedNews;
  }

  @SuppressWarnings("unchecked")
  private List<INews> getNewsFromGuid(INews newsItem) {
    Query query = fDb.query();
    query.constrain(News.class);
    query.descend("fGuidValue").constrain(newsItem.getGuid().getValue()); //$NON-NLS-1$
    return activateAll(query.execute());
  }

  public final List<ILabel> loadLabels() {
    try {
      ObjectSet<ILabel> labels = fDb.ext().query(ILabel.class);
      activateAll(labels);
      return new ArrayList<ILabel>(labels);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public final List<INews> saveNews(List<INews> newsList) {
    List<INews> news = saveNews(newsList, null, true);
    DBHelper.cleanUpAndFireEvents();
    return news;
  }

  private <T extends Collection<INews>> T saveNews(T newsList, Integer depth, boolean lock) {
    if (lock)
      fWriteLock.lock();

    try {

      if (depth == null)
        DBHelper.saveAndCascadeAllNews(fDb, newsList, true);
      else {
        for (INews news : newsList) {
          INews oldNews = fDb.ext().peekPersisted(news, 2, true);
          ModelEvent newsEventTemplate = new NewsEvent(oldNews, news, true);
          DBHelper.putEventTemplate(newsEventTemplate);
          fDb.ext().set(news, depth.intValue());
        }
      }
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      if (lock)
        fWriteLock.unlock();
    }
    return newsList;
  }

  private void saveFeed(MergeResult mergeResult) {
    for (Object o : mergeResult.getRemovedObjects()) {
      /* We know that in these cases, the parent entity will be updated */
      if (o instanceof INews)
        EventManager.getInstance().addItemBeingDeleted(((INews) o).getFeedReference());
      else if (o instanceof IAttachment)
        EventManager.getInstance().addItemBeingDeleted(((IAttachment) o).getNews());

      fDb.delete(o);
    }

    List<Object> updatedEntities = new ArrayList<Object>(mergeResult.getUpdatedObjects());
    for (Object o : updatedEntities) {
      if (o instanceof INews)
       DBHelper.saveNews(fDb, (INews) o);
      else if (o instanceof IFeed)
        fDb.ext().set(o, 2);
      else
        fDb.ext().set(o, 1);
    }
  }

  public List<IBookMark> loadAllBookMarks(boolean activateFully) {
    try {
      ObjectSet<IBookMark> marks = fDb.ext().query(IBookMark.class);
      if (activateFully)
        activateAll(marks);

      return new ArrayList<IBookMark>(marks);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public List<IFeed> loadAllFeeds() {
    try {
      ObjectSet<IFeed> feeds = fDb.ext().query(IFeed.class);
      activateAll(feeds);

      return new ArrayList<IFeed>(feeds);
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public void deleteFolders(List<IFolder> folders) {
    fWriteLock.lock();
    try {
      for (IFolder folder : folders) {
        FolderEvent event = new FolderEvent(folder, null, true);
        DBHelper.putEventTemplate(event);
      }
      for (IFolder folder : folders)
        fDb.delete(folder);

      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  public Set<String> loadAuthors() {
    try {
      ObjectSet<Person> persons = fDb.ext().query(Person.class);

      Set<String> strings = new TreeSet<String>();
      for (IPerson person : persons) {
        String name = person.getName();
        name = (name != null) ? name.trim() : null;
        if (StringUtils.isSet(name))
          strings.add(name);
        else if (person.getEmail() != null)
          strings.add(person.getEmail().toString());
      }

      return strings;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  public Set<String> loadCategories() {
    try {
      ObjectSet<Category> categories = fDb.ext().query(Category.class);

      Set<String> strings = new TreeSet<String>();
      for (ICategory category : categories) {
        String name = category.getName();
        name = (name != null) ? name.trim() : null;
        if (StringUtils.isSet(name))
          strings.add(name);
      }

      return strings;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }
}
