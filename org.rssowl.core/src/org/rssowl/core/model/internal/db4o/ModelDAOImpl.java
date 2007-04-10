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
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.model.events.AttachmentEvent;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.CategoryEvent;
import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.LabelEvent;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.PersonEvent;
import org.rssowl.core.model.events.SearchConditionEvent;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.internal.persist.ConditionalGet;
import org.rssowl.core.model.persist.IAttachment;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.ICloud;
import org.rssowl.core.model.persist.IConditionalGet;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.ITextInput;
import org.rssowl.core.model.persist.NewsCounter;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.reference.AttachmentReference;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.CategoryReference;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.reference.FolderReference;
import org.rssowl.core.model.reference.LabelReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.model.reference.PersonReference;
import org.rssowl.core.model.reference.SearchConditionReference;
import org.rssowl.core.model.reference.SearchMarkReference;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.net.URI;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * An implementation of IModelDAO that simply delegates all the functionality
 * to DBManager.<p>
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class ModelDAOImpl implements IModelDAO {
  private ReadWriteLock fLock;
  private Lock fWriteLock;
  private ObjectContainer fDb;
  /**
   * Creates an instance of this class.
   */
  public ModelDAOImpl() {
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

  private void deleteEntityAndFireEvents(ModelEvent event) {
    DBHelper.putEventTemplate(event);
    deleteObject(event.getEntity());
    DBHelper.cleanUpAndFireEvents();
  }

  private void deleteObject(Object object) {
    fWriteLock.lock();
    try {
      fDb.delete(object);
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
  }

  public final void deleteConditionalGet(IConditionalGet conditionalGet) {
    deleteObject(conditionalGet);
  }

  public final void deleteNewsCounter() {
    NewsCounter newsCounter = loadNewsCounter();
    deleteObject(newsCounter);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteBookMark(org.rssowl.core.model.reference.BookMarkReference)
   */
  public final void deleteBookMark(BookMarkReference reference) throws PersistenceException {
    IBookMark mark = loadBookMark(reference.getId());
    BookMarkEvent event = new BookMarkEvent(mark, null, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteCategory(org.rssowl.core.model.reference.CategoryReference)
   */
  public final void deleteCategory(CategoryReference reference) throws PersistenceException {
    ICategory category = loadCategory(reference.getId());
    CategoryEvent event = new CategoryEvent(category, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteFeed(org.rssowl.core.model.reference.FeedReference)
   */
  public final void deleteFeed(FeedReference reference) throws PersistenceException {
    IFeed feed = loadFeed(reference.getId());
    FeedEvent event = new FeedEvent(feed, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteFeed(org.rssowl.core.model.reference.FeedLinkReference)
   */
  public final void deleteFeed(FeedLinkReference reference) throws PersistenceException {
    IFeed feed = Owl.getPersistenceService().getApplicationLayer().loadFeed(reference.getLink());
    FeedEvent event = new FeedEvent(feed, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteFolder(org.rssowl.core.model.reference.FolderReference)
   */
  public final void deleteFolder(FolderReference reference) throws PersistenceException {
    IFolder folder = loadFolder(reference.getId());
    FolderEvent event = new FolderEvent(folder, null, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteLabel(org.rssowl.core.model.reference.LabelReference)
   */
  public final void deleteLabel(LabelReference reference) throws PersistenceException {
    ILabel label = loadLabel(reference.getId());
    LabelEvent event = new LabelEvent(label, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteNews(org.rssowl.core.model.reference.NewsReference)
   */
  public final void deleteNews(NewsReference reference) throws PersistenceException {
    INews news = loadNews(reference.getId());
    NewsEvent event = new NewsEvent(null, news, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteSearchMark(org.rssowl.core.model.reference.SearchMarkReference)
   */
  public final void deleteSearchMark(SearchMarkReference reference) throws PersistenceException {
    ISearchMark mark = loadSearchMark(reference.getId());
    SearchMarkEvent event = new SearchMarkEvent(mark, null, true);
    deleteEntityAndFireEvents(event);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deletePerson(org.rssowl.core.model.reference.PersonReference)
   */
  public void deletePerson(PersonReference reference) throws PersistenceException {
    IPerson person = loadPerson(reference.getId());
    PersonEvent event = new PersonEvent(person, true);
    deleteEntityAndFireEvents(event);
  }

  public void deleteSearchCondition(SearchConditionReference reference) throws PersistenceException {
    ISearchCondition searchCond = loadSearchCondition(reference.getId());
    SearchConditionEvent event = new SearchConditionEvent(searchCond, true);
    deleteEntityAndFireEvents(event);
  }
  /*
   * @see org.rssowl.core.model.dao.IModelDAO#deleteAttachment(org.rssowl.core.model.reference.AttachmentReference)
   */
  public void deleteAttachment(AttachmentReference reference) throws PersistenceException {
    IAttachment attachment = loadAttachment(reference.getId());
    AttachmentEvent event = new AttachmentEvent(attachment, true);
    //TODO Not sure about this, but let's do it for now to help us track a bug
    //in NewsService where never having a newsUpdated with a null oldNews is
    //helpful
    INews news = attachment.getNews();
    INews oldNews = fDb.ext().peekPersisted(news, 2, true);
    NewsEvent newsEvent = new NewsEvent(oldNews, news, false);
    DBHelper.putEventTemplate(newsEvent);
    deleteEntityAndFireEvents(event);
  }

  public final IConditionalGet loadConditionalGet(URI link) throws PersistenceException  {
    Assert.isNotNull(link, "link cannot be null"); //$NON-NLS-1$
    try {
      Query query = fDb.query();
      query.constrain(ConditionalGet.class);
      query.descend("fLink").constrain(link.toString()); //$NON-NLS-1$

      @SuppressWarnings("unchecked")
      ObjectSet<IConditionalGet> set = query.execute();
      for (IConditionalGet entity : set) {
        fDb.activate(entity, Integer.MAX_VALUE);
        return entity;
      }
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
    return null;
  }

  public final NewsCounter loadNewsCounter() throws PersistenceException    {
    try {
      Query query = fDb.query();
      query.constrain(NewsCounter.class);

      @SuppressWarnings("unchecked")
      ObjectSet<NewsCounter> set = query.execute();
      if (set.isEmpty())
        return null;

      if (set.size() > 1)
        throw new IllegalStateException("Only one NewsCounter should exist, but " +
                "there are: " + set.size());

      NewsCounter counter = set.next();
      fDb.activate(counter, Integer.MAX_VALUE);
      return counter;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadAttachment(long)
   */
  public final IAttachment loadAttachment(long id) throws PersistenceException {
    return loadEntity(IAttachment.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadBookMark(long)
   */
  public IBookMark loadBookMark(long id) throws PersistenceException {
    return loadEntity(IBookMark.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadCategory(long)
   */
  public ICategory loadCategory(long id) throws PersistenceException {
    return loadEntity(ICategory.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadCloud(long)
   */
  public ICloud loadCloud(long id) throws PersistenceException {
//  TODO If we don't implement this, what do we do? Return null or throw
    // NotSupportedOperation?
    return null;
  }

  public IFeed loadFeed(long id) throws PersistenceException   {
    IFeed feed = loadEntity(IFeed.class,id);
    return feed;
  }

  private <T extends IEntity>T loadEntity(Class<T> klass, long id)  {
    try {
      Query query = fDb.query();
      query.constrain(klass);
      query.descend("fId").constrain(Long.valueOf(id)); //$NON-NLS-1$

      @SuppressWarnings("unchecked")
      ObjectSet<T> set = query.execute();
      for (T entity : set) {
        // TODO Activate completely by default for now. Must decide how to deal
        // with this.
        fDb.activate(entity, Integer.MAX_VALUE);
        return entity;
      }
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
    return null;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadFolder(long)
   */
  public IFolder loadFolder(long id) throws PersistenceException {
    return loadEntity(IFolder.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadLabel(long)
   */
  public ILabel loadLabel(long id) throws PersistenceException {
    return loadEntity(ILabel.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadNews(long)
   */
  public INews loadNews(long id) throws PersistenceException {
    return loadEntity(INews.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadPerson(long)
   */
  public IPerson loadPerson(long id) throws PersistenceException {
    return loadEntity(IPerson.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadSearchMark(long)
   */
  public ISearchMark loadSearchMark(long id) throws PersistenceException {
    return loadEntity(ISearchMark.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadSearchCondition(long)
   */
  public ISearchCondition loadSearchCondition(long id) throws PersistenceException {
    return loadEntity(ISearchCondition.class, id);
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#loadTextInput(long)
   */
  public ITextInput loadTextInput(long id) throws PersistenceException {
//  TODO If we don't implement this, what do we do? Return null or throw
    // NotSupportedOperation?
    return null;
  }

  private void saveAndCommit(ModelEvent event, boolean saveFully)  {
    saveAndCommit(event.getEntity(), saveFully);
  }

  private void saveAndCommit(Object entity, boolean saveFully) {
    fWriteLock.lock();
    try {
      if (saveFully)
        fDb.ext().set(entity, Integer.MAX_VALUE);
      else
        fDb.set(entity);

      fDb.commit();
    } catch (Db4oException e)   {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
  }

  private void saveCommitAndFireEvents(ModelEvent event, boolean updateFully)  {
    DBHelper.putEventTemplate(event);
    saveAndCommit(event, updateFully);
    DBHelper.cleanUpAndFireEvents();
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveBookMark(org.rssowl.core.model.types.IBookMark)
   */
  public IBookMark saveBookMark(IBookMark bookMark) throws PersistenceException  {
    ModelEvent event = new BookMarkEvent(bookMark, null, true);
    saveCommitAndFireEvents(event, false);
    return bookMark;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveCategory(org.rssowl.core.model.types.ICategory)
   */
  public ICategory saveCategory(ICategory category) throws PersistenceException {
    ModelEvent event = new CategoryEvent(category, true);
    saveCommitAndFireEvents(event, false);
    return category;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveFeed(org.rssowl.core.model.types.IFeed)
   */
  public IFeed saveFeed(IFeed feed) throws PersistenceException {
    fWriteLock.lock();
    try {
      DBHelper.saveFeed(fDb, feed);
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
    return feed;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveFolder(org.rssowl.core.model.types.IFolder)
   */
  public IFolder saveFolder(IFolder folder) throws PersistenceException {
    FolderEvent event = new FolderEvent(folder, null, true);
    saveCommitAndFireEvents(event, false);
    return folder;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveLabel(org.rssowl.core.model.types.ILabel)
   */
  public ILabel saveLabel(ILabel label) throws PersistenceException {
    LabelEvent event = new LabelEvent(label, true);
    saveCommitAndFireEvents(event, false);
    return label;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveNews(org.rssowl.core.model.types.INews)
   */
  public INews saveNews(INews news) throws PersistenceException {
    fWriteLock.lock();
    try {
      DBHelper.saveAndCascadeNews(fDb, news, true);
      fDb.commit();
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
    return news;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveSearchMark(org.rssowl.core.model.types.ISearchMark)
   */
  public ISearchMark saveSearchMark(ISearchMark mark) throws PersistenceException {
    SearchMarkEvent event = new SearchMarkEvent(mark, null, true);
    saveCommitAndFireEvents(event, false);
    return mark;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveCloud(org.rssowl.core.model.types.ICloud)
   */
  public ICloud saveCloud(ICloud cloud) throws PersistenceException {
    // TODO If we don't implement this, what do we do? Return null or throw
    // NotSupportedOperation?
    return null;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveTextInput(org.rssowl.core.model.types.ITextInput)
   */
  public ITextInput saveTextInput(ITextInput textInput) throws PersistenceException {
    // TODO If we don't implement this, what do we do? Return null or throw
    // NotSupportedOperation?
    return null;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveSearchCondition(org.rssowl.core.model.search.ISearchCondition)
   */
  public ISearchCondition saveSearchCondition(ISearchCondition condition) throws PersistenceException {
    SearchConditionEvent event = new SearchConditionEvent(condition, true);
    saveCommitAndFireEvents(event, true);
    return condition;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#saveAttachment(org.rssowl.core.model.types.IAttachment)
   */
  public IAttachment saveAttachment(IAttachment attachment) throws PersistenceException   {
    AttachmentEvent event = new AttachmentEvent(attachment, true);
    saveCommitAndFireEvents(event, false);
    return attachment;
  }

  /*
   * @see org.rssowl.core.model.dao.IModelDAO#savePerson(org.rssowl.core.model.types.IPerson)
   */
  public IPerson savePerson(IPerson person) throws PersistenceException {
    PersonEvent event = new PersonEvent(person, true);
    saveCommitAndFireEvents(event, false);
    return person;
  }

  public IConditionalGet saveConditionalGet(IConditionalGet conditionalGet) throws PersistenceException {
    saveAndCommit(conditionalGet, true);
    return conditionalGet;
  }

  public NewsCounter saveNewsCounter(NewsCounter newsCounter) throws PersistenceException   {
    if (!fDb.ext().isStored(newsCounter) && (loadNewsCounter() != null))
      throw new IllegalArgumentException("Only a single newsCounter can be stored");

    saveAndCommit(newsCounter, true);
    return newsCounter;
  }
}
