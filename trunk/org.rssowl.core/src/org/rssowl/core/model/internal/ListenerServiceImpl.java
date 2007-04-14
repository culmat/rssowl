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

package org.rssowl.core.model.internal;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.model.IListenerService;
import org.rssowl.core.model.events.AttachmentEvent;
import org.rssowl.core.model.events.AttachmentListener;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.BookMarkListener;
import org.rssowl.core.model.events.CategoryEvent;
import org.rssowl.core.model.events.CategoryListener;
import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.FeedListener;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.events.LabelEvent;
import org.rssowl.core.model.events.LabelListener;
import org.rssowl.core.model.events.ModelEvent;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.events.PersonEvent;
import org.rssowl.core.model.events.PersonListener;
import org.rssowl.core.model.events.SearchConditionEvent;
import org.rssowl.core.model.events.SearchConditionListener;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.events.SearchMarkListener;
import org.rssowl.core.model.events.runnable.EventType;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.pref.PreferencesEvent;
import org.rssowl.core.model.persist.pref.PreferencesListener;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.util.Set;

/**
 * @author bpasero
 */
public class ListenerServiceImpl implements IListenerService {

  //TODO Need a better solution here
  private static final boolean DEBUG = false;

  /* Listener Lists for listening on Model Events */
  private ListenerList fAttachmentListeners = new ListenerList();
  private ListenerList fBookMarkListeners = new ListenerList();
  private ListenerList fCategoryListeners = new ListenerList();
  private ListenerList fFeedListeners = new ListenerList();
  private ListenerList fFolderListeners = new ListenerList();
  private ListenerList fLabelListeners = new ListenerList();
  private ListenerList fNewsListeners = new ListenerList();
  private ListenerList fPersonListeners = new ListenerList();
  private ListenerList fSearchMarkListeners = new ListenerList();
  private ListenerList fSearchConditionListeners = new ListenerList();
  private ListenerList fPreferencesListeners = new ListenerList();

  /*
   * @see org.rssowl.core.model.IListenerService#addAttachmentListener(org.rssowl.core.model.events.AttachmentListener)
   */
  public void addAttachmentListener(AttachmentListener listener) {
    fAttachmentListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeAttachmentListener(org.rssowl.core.model.events.AttachmentListener)
   */
  public void removeAttachmentListener(AttachmentListener listener) {
    fAttachmentListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyAttachmentAdded(java.util.Set)
   */
  public void notifyAttachmentAdded(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyAttachmentDeleted(java.util.Set)
   */
  public void notifyAttachmentDeleted(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyAttachmentUpdated(java.util.Set)
   */
  public void notifyAttachmentUpdated(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addBookMarkListener(org.rssowl.core.model.events.BookMarkListener)
   */
  public void addBookMarkListener(BookMarkListener listener) {
    fBookMarkListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeBookMarkListener(org.rssowl.core.model.events.BookMarkListener)
   */
  public void removeBookMarkListener(BookMarkListener listener) {
    fBookMarkListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyBookMarkAdded(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyBookMarkAdded(final Set<BookMarkEvent> events) {
    logEvents(events, EventType.PERSIST);

    /* Notify Listeners */
    Object listeners[] = fBookMarkListeners.getListeners();
    for (Object element : listeners) {
      final BookMarkListener listener = (BookMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyBookMarkDeleted(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyBookMarkDeleted(final Set<BookMarkEvent> events) {
    logEvents(events, EventType.REMOVE);

    /* Notify Listeners */
    Object listeners[] = fBookMarkListeners.getListeners();
    for (Object element : listeners) {
      final BookMarkListener listener = (BookMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyBookMarkUpdated(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyBookMarkUpdated(final Set<BookMarkEvent> events) {
    logEvents(events, EventType.UPDATE);

    /* Notify Listeners */
    Object listeners[] = fBookMarkListeners.getListeners();
    for (Object element : listeners) {
      final BookMarkListener listener = (BookMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addCategoryListener(org.rssowl.core.model.events.CategoryListener)
   */
  public void addCategoryListener(CategoryListener listener) {
    fCategoryListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeCategoryListener(org.rssowl.core.model.events.CategoryListener)
   */
  public void removeCategoryListener(CategoryListener listener) {
    fCategoryListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyCategoryAdded(java.util.Set)
   */
  public void notifyCategoryAdded(final Set<CategoryEvent> events) {
    Object listeners[] = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyCategoryDeleted(java.util.Set)
   */
  public void notifyCategoryDeleted(final Set<CategoryEvent> events) {
    Object listeners[] = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyCategoryUpdated(java.util.Set)
   */
  public void notifyCategoryUpdated(final Set<CategoryEvent> events) {
    Object[] listeners = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addFeedListener(org.rssowl.core.model.events.FeedListener)
   */
  public void addFeedListener(FeedListener listener) {
    fFeedListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeFeedListener(org.rssowl.core.model.events.FeedListener)
   */
  public void removeFeedListener(FeedListener listener) {
    fFeedListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFeedAdded(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFeedAdded(final Set<FeedEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object[] listeners = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFeedDeleted(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFeedDeleted(final Set<FeedEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFeedUpdated(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFeedUpdated(final Set<FeedEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addFolderListener(org.rssowl.core.model.events.FolderListener)
   */
  public void addFolderListener(FolderListener listener) {
    fFolderListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeFolderListener(org.rssowl.core.model.events.FolderListener)
   */
  public void removeFolderListener(FolderListener listener) {
    fFolderListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFolderAdded(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFolderAdded(final Set<FolderEvent> events) {
    logEvents(events, EventType.PERSIST);

    /* Notify Listeners */
    Object listeners[] = fFolderListeners.getListeners();
    for (Object element : listeners) {
      final FolderListener listener = (FolderListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFolderDeleted(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFolderDeleted(final Set<FolderEvent> events) {
    logEvents(events, EventType.REMOVE);

    /* Notify Listeners */
    Object listeners[] = fFolderListeners.getListeners();
    for (Object element : listeners) {
      final FolderListener listener = (FolderListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyFolderUpdated(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyFolderUpdated(final Set<FolderEvent> events) {
    logEvents(events, EventType.UPDATE);

    /* Notify Listeners */
    Object listeners[] = fFolderListeners.getListeners();
    for (Object element : listeners) {
      final FolderListener listener = (FolderListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addLabelListener(org.rssowl.core.model.events.LabelListener)
   */
  public void addLabelListener(LabelListener listener) {
    fLabelListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeLabelListener(org.rssowl.core.model.events.LabelListener)
   */
  public void removeLabelListener(LabelListener listener) {
    fLabelListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyLabelAdded(java.util.Set)
   */
  public void notifyLabelAdded(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyLabelDeleted(java.util.Set)
   */
  public void notifyLabelDeleted(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyLabelUpdated(java.util.Set)
   */
  public void notifyLabelUpdated(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addNewsListener(org.rssowl.core.model.events.NewsListener)
   */
  public void addNewsListener(NewsListener listener) {
    fNewsListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeNewsListener(org.rssowl.core.model.events.NewsListener)
   */
  public void removeNewsListener(NewsListener listener) {
    fNewsListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyentitiesAdded(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyNewsAdded(final Set<NewsEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyNewsDeleted(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyNewsDeleted(final Set<NewsEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyNewsUpdated(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifyNewsUpdated(final Set<NewsEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addPersonListener(org.rssowl.core.model.events.PersonListener)
   */
  public void addPersonListener(PersonListener listener) {
    fPersonListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removePersonListener(org.rssowl.core.model.events.PersonListener)
   */
  public void removePersonListener(PersonListener listener) {
    fPersonListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPersonAdded(java.util.Set)
   */
  public void notifyPersonAdded(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPersonDeleted(java.util.Set)
   */
  public void notifyPersonDeleted(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPersonUpdated(java.util.Set)
   */
  public void notifyPersonUpdated(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addSearchMarkListener(org.rssowl.core.model.events.SearchMarkListener)
   */
  public void addSearchMarkListener(SearchMarkListener listener) {
    fSearchMarkListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeSearchMarkListener(org.rssowl.core.model.events.SearchMarkListener)
   */
  public void removeSearchMarkListener(SearchMarkListener listener) {
    fSearchMarkListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchMarkAdded(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkAdded(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchMarkDeleted(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkDeleted(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchMarkUpdated(java.util.Set)
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkUpdated(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferenceAdded(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferenceAdded(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceAdded(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferencesDeleted(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferencesDeleted(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceDeleted(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifyPreferencesUpdated(org.rssowl.core.model.persist.pref.PreferencesEvent)
   */
  public void notifyPreferencesUpdated(final PreferencesEvent event) {
    Object listeners[] = fPreferencesListeners.getListeners();
    for (Object element : listeners) {
      final PreferencesListener listener = (PreferencesListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.preferenceUpdated(event);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addPreferencesListener(org.rssowl.core.model.persist.pref.PreferencesListener)
   */
  public void addPreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removePreferencesListener(org.rssowl.core.model.persist.pref.PreferencesListener)
   */
  public void removePreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchConditionAdded(java.util.Set)
   */
  public void notifySearchConditionAdded(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesAdded(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchConditionDeleted(java.util.Set)
   */
  public void notifySearchConditionDeleted(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesDeleted(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#notifySearchConditionUpdated(java.util.Set)
   */
  public void notifySearchConditionUpdated(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.entitiesUpdated(events);
        }
      });
    }
  }

  /*
   * @see org.rssowl.core.model.IListenerService#addSearchConditionListener(org.rssowl.core.model.events.SearchConditionListener)
   */
  public void addSearchConditionListener(SearchConditionListener listener) {
    fSearchConditionListeners.add(listener);
  }

  /*
   * @see org.rssowl.core.model.IListenerService#removeSearchConditionListener(org.rssowl.core.model.events.SearchConditionListener)
   */
  public void removeSearchConditionListener(SearchConditionListener listener) {
    fSearchConditionListeners.remove(listener);
  }

  @SuppressWarnings("nls")
  private void logEvents(Set< ? extends ModelEvent> events, EventType eventType) {
    if (DEBUG) {
      String eventTypeString = null;
      switch (eventType) {
        case PERSIST:
          eventTypeString = " Added ("; //$NON-NLS-1$
          break;
        case UPDATE:
          eventTypeString = " Updated ("; //$NON-NLS-1$
          break;
        case REMOVE:
          eventTypeString = " Removed ("; //$NON-NLS-1$
          break;
      }
      IPersistable type = null;
      ModelEvent event = events.iterator().next();
      if (eventType != EventType.REMOVE)
        type = event.getEntity();

      String typeName = type == null ? "" : type.getClass().getSimpleName();
      String typeString = type == null ? "" : type.toString();

      if (events.size() > 0 && typeName == "")
        typeName = events.iterator().next().getClass().getSimpleName();

      System.out.println(typeName + eventTypeString + typeString + ", events = " + events.size() + ", isRoot = " + event.isRoot() + ")");
    }
  }
}