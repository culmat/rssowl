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
package org.rssowl.core.internal.persist.service;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsGetter;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.IConditionalGetDAO;
import org.rssowl.core.persist.event.AttachmentEvent;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.CategoryEvent;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.PersonEvent;
import org.rssowl.core.persist.event.SearchConditionEvent;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.search.IModelSearch;
import org.rssowl.core.persist.search.ISearchCondition;
import org.rssowl.core.persist.search.ISearchHit;
import org.rssowl.core.persist.service.IDGenerator;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.events.Event4;
import com.db4o.events.EventArgs;
import com.db4o.events.EventListener4;
import com.db4o.events.EventRegistry;
import com.db4o.events.EventRegistryFactory;
import com.db4o.events.ObjectEventArgs;
import com.db4o.query.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class EventManager {

  /**
   * Iterator implementation that iterates from the end of the list. Useful
   * if items need to be removed from the list during iteration and specific
   * method needs to be called for removal.
   */
  private final static class ReverseIterator<T> implements Iterable<T>, Iterator<T> {
    private final List<T> fList;
    private int index;

    static <T>ReverseIterator<T> createInstance(List<T> list) {
      return new ReverseIterator<T>(list);
    }

    private ReverseIterator(List<T> list)    {
      fList = list;
      index = list.size() - 1;
    }

    public final Iterator<T> iterator() {
      return this;
    }

    public final boolean hasNext() {
      return index > -1;
    }

    public final T next() {
      return fList.get(index--);
    }

    public final void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private final ThreadLocal<Set<Object>> fItemsBeingDeleted = new ThreadLocal<Set<Object>>();
  private static final String PARENT_DELETED_KEY = "rssowl.db4o.EventManager.parentDeleted"; //$NON-NLS-1$
  private final static EventManager INSTANCE = new EventManager();
  private ObjectContainer fDb;
  private IConditionalGetDAO fConditionalGetDAO;
  private IDGenerator fIDGenerator;

  private EventManager() {
    initEntityStoreListener();
  }

  private IDGenerator getIDGenerator() {
    if (fIDGenerator == null)
      fIDGenerator = Owl.getPersistenceService().getIDGenerator();

    return fIDGenerator;
  }

  private IConditionalGetDAO getConditionalGetDAO() {
    if (fConditionalGetDAO == null)
      fConditionalGetDAO = Owl.getPersistenceService().getDAOService().getConditionalGetDAO();

    return fConditionalGetDAO;
  }

  private void initEventRegistry() {
    EventRegistry eventRegistry = EventRegistryFactory.forObjectContainer(fDb);

    EventListener4 updatedListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processUpdatedEvent(args);
      }
    };
    EventListener4 creatingListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processCreatingEvent(args);
      }
    };
    EventListener4 createdListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processCreatedEvent(args);
      }
    };

    EventListener4 deletingListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processDeletingEvent(args);
      }
    };

    EventListener4 deletedListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processDeletedEvent(args);
      }
    };

    EventListener4 activatedListener = new EventListener4() {
      public void onEvent(Event4 e, EventArgs args) {
        processActivatedEvent(args);
      }
    };

    eventRegistry.created().addListener(createdListener);
    eventRegistry.creating().addListener(creatingListener);
    eventRegistry.updated().addListener(updatedListener);
    eventRegistry.deleting().addListener(deletingListener);
    eventRegistry.deleted().addListener(deletedListener);
    eventRegistry.activated().addListener(activatedListener);
  }

  private void processActivatedEvent(EventArgs args) {
    IEntity entity = getEntity(args);
    if (entity == null)
      return;

    setSearchMarkGetter(entity);
  }

  private INewsGetter createNewsGetter(final ISearchMark mark) {
    INewsGetter newsGetter = new INewsGetter() {
      public List<ISearchHit<INews>> getNews() {
        List<ISearchCondition> searchConditions = mark.getSearchConditions();
        IModelSearch modelSearch = Owl.getPersistenceService().getModelSearch();
        List<ISearchHit<NewsReference>> newsRefs = modelSearch.searchNews(searchConditions, mark.matchAllConditions());
        List<ISearchHit<INews>> newsList = new ArrayList<ISearchHit<INews>>(newsRefs.size());
        for (ISearchHit<NewsReference> newsRefHit : newsRefs) {
          INews news = newsRefHit.getResult().resolve();
          if (news != null) { //TODO Remove once Bug 173 is fixed
            ISearchHit<INews> searchHit = modelSearch.createSearchHit(news, newsRefHit.getRelevance());
            newsList.add(searchHit);
          }
        }
        return newsList;
      }
    };
    return newsGetter;
  }

  private void processUpdatedEvent(EventArgs args) {
    IEntity entity = getEntity(args);
    if (entity == null)
      return;

    ModelEvent event = createModelEvent(entity);
    if (event != null)
      EventsMap.getInstance().putUpdateEvent(event);

  }

  private void processCreatingEvent(EventArgs args) {
    IEntity entity = getEntity(args);

    if (entity != null)
      setId(entity);

    setSearchMarkGetter(entity);

  }

  private void setSearchMarkGetter(IEntity entity) {
    if (entity instanceof ISearchMark) {
      ISearchMark mark = (ISearchMark) entity;
      mark.setNewsGetter(createNewsGetter(mark));
    }
  }

  private void processCreatedEvent(EventArgs args) {
    IEntity entity = getEntity(args);
    if (entity == null)
      return;

    ModelEvent event = createModelEvent(entity);
    if (event != null)
      EventsMap.getInstance().putPersistEvent(event);
  }

  private void processDeletingEvent(EventArgs args) {
    IEntity entity = getEntity(args);
    if (entity == null)
      return;

    if (entity instanceof INews)
      cascadeNewsDeletion((INews) entity);
    else if (entity instanceof IFeed)
      cascadeFeedDeletion((IFeed) entity);
    else if (entity instanceof IMark)
      cascadeMarkDeletion((IMark) entity);
    else if (entity instanceof IFolder)
      removeFromParentFolderAndCascade((IFolder) entity);
    else if (entity instanceof IAttachment)
      removeFromParentNews((IAttachment) entity);
    else if (entity instanceof ISearchCondition)
      cascadeSearchConditionDeletion((ISearchCondition) entity);
  }

  private void cascadeSearchConditionDeletion(ISearchCondition searchCondition) {
    ISearchMark searchMark = getSearchMark(searchCondition);
    if (!itemsBeingDeletedContains(searchMark)) {
      if (searchMark.removeSearchCondition(searchCondition))
        fDb.ext().set(searchMark, 2);
    }
    fDb.delete(searchCondition.getField());
  }

  @SuppressWarnings("unchecked")
  private ISearchMark getSearchMark(ISearchCondition searchCondition) {
    Query query = fDb.query();
    query.constrain(ISearchMark.class);
    query.descend("fSearchConditions").constrain(searchCondition);
    ObjectSet<ISearchMark> set = query.execute();
    if (set.size() != 1)
      throw new IllegalStateException("searchCondition has less than or more than 1 parent ISearchMark");

    return set.get(0);
  }

  private void cascadeNewsDeletion(INews news) {
    addItemBeingDeleted(news);
    removeFromParentFeed(news);

    fDb.delete(news.getGuid());
    fDb.delete(news.getSource());
    fDb.delete(news.getAuthor());

    for (ICategory category : ReverseIterator.createInstance(news.getCategories())) {
      fDb.delete(category);
    }
    for (IAttachment attachment : ReverseIterator.createInstance(news.getAttachments())) {
      fDb.delete(attachment);
    }
  }

  private void cascadeMarkDeletion(IMark mark) {
    removeFromParentFolder(mark);
    if (mark instanceof IBookMark)
      deleteFeedIfNecessary((IBookMark) mark);
    else if (mark instanceof ISearchMark)
      cascadeSearchMarkDeletion((ISearchMark) mark);
  }

  private void cascadeSearchMarkDeletion(ISearchMark mark) {
    addItemBeingDeleted(mark);
    for (ISearchCondition condition : mark.getSearchConditions())   {
      fDb.delete(condition);
    }
  }

  private void cascadeFeedDeletion(IFeed feed) {
    addItemBeingDeleted(new FeedLinkReference(feed.getLink()));
    fDb.delete(feed.getImage());
    fDb.delete(feed.getAuthor());
    for (ICategory category : ReverseIterator.createInstance(feed.getCategories())) {
      fDb.delete(category);
    }
    for (INews news : ReverseIterator.createInstance(feed.getNews())) {
      fDb.delete(news);
    }
    IConditionalGet conditionalGet = getConditionalGetDAO().load(feed.getLink());
    if (conditionalGet != null)
      fDb.delete(conditionalGet);

    removeFromItemsBeingDeleted(feed);
  }

  private void removeFromParentNews(IAttachment attachment) {
    INews news = attachment.getNews();
    if (itemsBeingDeletedContains(news))
      return;

    news.removeAttachment(attachment);
    fDb.set(news);
  }

  private void removeFromParentFolderAndCascade(IFolder folder) {
    IFolder parentFolder = folder.getParent();
    if (parentFolder != null) {
      parentFolder.removeFolder(folder);
      fDb.set(parentFolder);
    }
    for (IFolder child : ReverseIterator.createInstance(folder.getFolders())) {
      cascadeFolderDeletion(child);
    }

    for (IMark mark : ReverseIterator.createInstance(folder.getMarks())) {
      mark.setProperty(PARENT_DELETED_KEY, true);
      fDb.delete(mark);
    }
  }

  private void cascadeFolderDeletion(IFolder folder) {

    for (IFolder child : ReverseIterator.createInstance(folder.getFolders())) {
      cascadeFolderDeletion(child);
    }

    for (IMark mark : ReverseIterator.createInstance(folder.getMarks())) {
      mark.setProperty(PARENT_DELETED_KEY, true);
      fDb.delete(mark);
    }

    folder.setParent(null);

    fDb.delete(folder);
  }

  private void removeFromParentFolder(IMark mark) {
    IFolder parentFolder = mark.getFolder();
    parentFolder.removeMark(mark);
    if (mark.getProperty(PARENT_DELETED_KEY) == null)
      fDb.set(parentFolder);
    else {
      mark.removeProperty(PARENT_DELETED_KEY);
    }
  }

  private void removeFromParentFeed(INews news) {
    FeedLinkReference feedRef = news.getFeedReference();
    if (itemsBeingDeletedContains(feedRef))
      return;

    IFeed feed = feedRef.resolve();
    /* If the news was still within parent, update parent */
    if (feed.removeNews(news))
      fDb.ext().set(feed, 2);
 }

  private boolean removeFromItemsBeingDeleted(Object entity) {
    Set<Object> entities = fItemsBeingDeleted.get();
    if (entities == null)
      return false;

    return entities.remove(entity);
  }

  private boolean itemsBeingDeletedContains(Object entity) {
    Set<Object> entities = fItemsBeingDeleted.get();
    if (entities == null)
      return false;

    return entities.contains(entity);
  }

  private void deleteFeedIfNecessary(IBookMark mark) {
    //TODO Share logic with IApplicationLayer method that does the same
    Query query = fDb.query();
    query.constrain(Feed.class);
    query.descend("fLinkText").constrain(mark.getFeedLinkReference().getLink().toString()); //$NON-NLS-1$
    @SuppressWarnings("unchecked")
    ObjectSet<IFeed> feeds = query.execute();
    for (IFeed feed : feeds) {
      if(onlyBookMarkReference(feed)) {
        fDb.delete(feed);
      }
    }
  }

  private boolean onlyBookMarkReference(IFeed feed) {
    //TODO Share logic with IApplicationLayer method that does the same
    Query query = fDb.query();
    query.constrain(BookMark.class);
    query.descend("fFeedLink").constrain(feed.getLink().toString()); //$NON-NLS-1$

    @SuppressWarnings("unchecked")
    ObjectSet<IBookMark> marks = query.execute();

    if (marks.size() == 1) {
      return true;
    }
    return false;
  }

  private void processDeletedEvent(EventArgs args) {
    IEntity entity = getEntity(args);
    if (entity == null)
      return;

    ModelEvent event = createModelEvent(entity);
    if (event != null)
      EventsMap.getInstance().putRemoveEvent(event);
  }

  private IEntity getEntity(EventArgs args) {
    ObjectEventArgs queryArgs = ((ObjectEventArgs) args);
    Object o = queryArgs.object();
    if (o instanceof IEntity) {
      IEntity entity = (IEntity) o;
      return entity;
    }
    return null;
  }

  private ModelEvent createModelEvent(IEntity entity) {
    ModelEvent modelEvent = null;
    Map<Integer, ModelEvent> templatesMap = EventsMap.getInstance().getEventTemplatesMap();
    ModelEvent template = templatesMap.get(System.identityHashCode(entity));
    //TODO In some cases, the template is complete. We can save some object allocation
    //by reusing it.

    boolean root = isRoot(template);
    if (entity instanceof INews) {
      modelEvent = createNewsEvent((INews) entity, template, root);
    }
    else if (entity instanceof IAttachment) {
      IAttachment attachment = (IAttachment) entity;
      modelEvent = new AttachmentEvent(attachment, root);
    }
    else if (entity instanceof ICategory) {
      ICategory category = (ICategory) entity;
      modelEvent = new CategoryEvent(category, root);
    }
    else if (entity instanceof IFeed) {
      IFeed feed = (IFeed) entity;
      modelEvent = new FeedEvent(feed, root);
    }
    else if (entity instanceof IPerson) {
      IPerson person = (IPerson) entity;
      modelEvent = new PersonEvent(person, root);
    }
    else if (entity instanceof IBookMark) {
      IBookMark mark = (IBookMark) entity;
      BookMarkEvent eventTemplate = (BookMarkEvent) template;
      IFolder oldParent = eventTemplate == null ? null : eventTemplate.getOldParent();
      modelEvent = new BookMarkEvent(mark, oldParent, root);
    }
    else if (entity instanceof ISearchMark) {
      ISearchMark mark = (ISearchMark) entity;
      SearchMarkEvent eventTemplate = (SearchMarkEvent) template;
      IFolder oldParent = eventTemplate == null ? null : eventTemplate.getOldParent();
      modelEvent = new SearchMarkEvent(mark, oldParent, root);
    }
    else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      FolderEvent eventTemplate = (FolderEvent) template;
      IFolder oldParent = eventTemplate == null ? null : eventTemplate.getOldParent();
      modelEvent = new FolderEvent(folder, oldParent, root);
    }
    else if (entity instanceof ILabel) {
      ILabel label = (ILabel) entity;
      modelEvent = new LabelEvent(label, root);
    }
    else if (entity instanceof ISearchCondition) {
      ISearchCondition searchCond = (ISearchCondition) entity;
      modelEvent = new SearchConditionEvent(searchCond, root);
    }
    return modelEvent;
  }

  private ModelEvent createNewsEvent(INews news, ModelEvent template, boolean root) {
    ModelEvent modelEvent;
    NewsEvent newsTemplate = (NewsEvent) template;
    INews oldNews = newsTemplate == null ? null : newsTemplate.getOldNews();

    modelEvent = new NewsEvent(oldNews, news, root);
    return modelEvent;
  }

  private boolean isRoot(ModelEvent template) {
    if (template == null)
      return false;

    return template.isRoot();
  }

  private void setId(IEntity entity) {
    if (entity.getId() == null) {
      long id;

      IDGenerator idGenerator = getIDGenerator();
      if (idGenerator instanceof DB4OIDGenerator)
        id = ((DB4OIDGenerator) idGenerator).getNext(false);
      else
        id = idGenerator.getNext();

      entity.setId(id);
    }
  }

  private void initEntityStoreListener() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
        initEventRegistry();
      }
      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }

  //TODO Change this name to better reflect what it does. It just says that the
  //given object will be updated or deleted during the transaction
  public final void addItemBeingDeleted(Object entity) {
    Set<Object> entities = fItemsBeingDeleted.get();
    if (entities == null) {
      entities = new HashSet<Object>(3);
      fItemsBeingDeleted.set(entities);
    }
    entities.add(entity);
  }

  /**
   * Clears any temporary storage used by the EventManager for the thread-bound
   * transaction.
   */
  public void clear() {
    fItemsBeingDeleted.set(null);
  }

  /**
   * @return singleton instance
   */
  public final static EventManager getInstance() {
    return INSTANCE;
  }

}
