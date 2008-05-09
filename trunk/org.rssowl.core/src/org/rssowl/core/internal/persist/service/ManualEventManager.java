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
package org.rssowl.core.internal.persist.service;

import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.event.AttachmentEvent;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.CategoryEvent;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.PersonEvent;
import org.rssowl.core.persist.event.PreferenceEvent;
import org.rssowl.core.persist.event.SearchConditionEvent;
import org.rssowl.core.persist.event.SearchEvent;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.event.runnable.EventType;

import java.util.List;
import java.util.Map;

public class ManualEventManager {

  public ManualEventManager() {
  }

//  private void cascadeNewsBinDeletion(INewsBin entity) {
//    Set<FeedLinkReference> removedFeedRefs = new HashSet<FeedLinkReference>();
//    DBHelper.removeNews(fDb, removedFeedRefs, entity.getNewsRefs());
//    DBHelper.removeFeedsAfterNewsBinUpdate(fDb, removedFeedRefs);
//    if (entity instanceof NewsBin)
//      fDb.delete(((NewsBin) entity).internalGetNewsContainer());
//
//  }

//  private void deleteFeedIfNecessary(IBookMark mark) {
//    Query query = fDb.query();
//    query.constrain(Feed.class);
//    query.descend("fLinkText").constrain(mark.getFeedLinkReference().getLink().toString()); //$NON-NLS-1$
//    @SuppressWarnings("unchecked")
//    List<IFeed> feeds = query.execute();
//    for (IFeed feed : feeds) {
//      FeedLinkReference feedRef = new FeedLinkReference(feed.getLink());
//      if (DBHelper.countBookMarkReference(fDb, feedRef) == 1) {
//        if (DBHelper.feedHasNewsWithCopies(fDb, feedRef)) {
//          List<INews> newsList = new ArrayList<INews>(feed.getNews());
//          for (INews news : newsList) {
//            feed.removeNews(news);
//            addItemBeingDeleted(feed);
//            fDb.delete(news);
//          }
//          fDb.ext().set(feed, 2);
//        }
//        else
//          fDb.delete(feed);
//      }
//    }
//  }

  //TODO need to figure out if it's update or persist somehow
  //TODO Need to add the one for remove
  public EventsMap2 createEventRunnables(Map<IEntity, ModelEvent> eventTemplates,
      List<EntityWithChildren<?>> entityWithChildrenList,
      EventType eventType, boolean cascade) {
    EventsMap2 eventsMap = new EventsMap2();
    for (EntityWithChildren<?> entityWithChildren : entityWithChildrenList) {
      eventsMap.putEvent(createModelEvent(eventTemplates, entityWithChildren.getEntity()), eventType);
      if (cascade) {
        for (IEntity entity : entityWithChildren.getChildEntities())
          eventsMap.putEvent(createModelEvent(eventTemplates, entity), eventType);
      }
    }
    return eventsMap;
  }

  public ModelEvent createModelEvent(Map<IEntity, ModelEvent> eventTemplates, IEntity entity) {
    ModelEvent modelEvent = null;
    ModelEvent template = eventTemplates.get(entity);
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
    else if (entity instanceof INewsBin) {
      INewsBin newsBin = (INewsBin) entity;
      NewsBinEvent eventTemplate = (NewsBinEvent) template;
      IFolder oldParent = eventTemplate == null ? null : eventTemplate.getOldParent();
      modelEvent = new NewsBinEvent(newsBin, oldParent, root);
    }
    else if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;
      FolderEvent eventTemplate = (FolderEvent) template;
      IFolder oldParent = eventTemplate == null ? null : eventTemplate.getOldParent();
      modelEvent = new FolderEvent(folder, oldParent, root);
    }
    else if (entity instanceof ILabel) {
      ILabel label = (ILabel) entity;
      LabelEvent eventTemplate = (LabelEvent) template;
      ILabel oldLabel = eventTemplate == null ? null : eventTemplate.getOldLabel();
      modelEvent = new LabelEvent(oldLabel, label, root);
    }
    else if (entity instanceof ISearchCondition) {
      ISearchCondition searchCond = (ISearchCondition) entity;
      modelEvent = new SearchConditionEvent(searchCond, root);
    }
    else if (entity instanceof IPreference) {
      IPreference pref = (IPreference) entity;
      modelEvent = new PreferenceEvent(pref);
    }
    else if (entity instanceof ISearch) {
      ISearch search = (ISearch) entity;
      modelEvent = new SearchEvent(search, root);
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

  public void databaseOpened(DatabaseEvent event) {
  }
  public void databaseClosed(DatabaseEvent event) {
  }
}
