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

package org.rssowl.core.model;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.model.dao.PersistenceLayer;
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
import org.rssowl.core.model.internal.persist.DefaultModelTypesFactory;
import org.rssowl.core.model.internal.persist.pref.DefaultScope;
import org.rssowl.core.model.internal.persist.pref.EntityScope;
import org.rssowl.core.model.internal.persist.pref.GlobalScope;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IModelTypesFactory;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.pref.IPreferencesInitializer;
import org.rssowl.core.model.persist.pref.IPreferencesScope;
import org.rssowl.core.model.persist.pref.PreferencesEvent;
import org.rssowl.core.model.persist.pref.PreferencesListener;
import org.rssowl.core.util.ExtensionUtils;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.util.Set;

/**
 * The central facade to the model of RSSOwl, serving access to underlying
 * services like persistence-layer-access. Also allows to add listeners for most
 * entities to be informed on any changes.
 * 
 * @author bpasero
 */
public class NewsModel {

  /** Flag indicating wether the Controller is accessed from a Test */
  public static boolean TESTING = false;

  /* Extension Point: Factory for Model Types */
  private static final String MODEL_TYPESFACTORY_EXTENSION_POINT = "org.rssowl.core.ModelTypesFactory"; //$NON-NLS-1$

  /* Extension Point: Persistence Layer */
  private static final String PERSISTANCE_LAYER_EXTENSION_POINT = "org.rssowl.core.PersistanceLayer"; //$NON-NLS-1$

  /* Extension Point: Preferences Initializer */
  private static final String PREFERENCES_INITIALIZER_EXTENSION_POINT = "org.rssowl.core.PreferencesInitializer"; //$NON-NLS-1$

  /* The Singleton Instance */
  private static NewsModel fInstance;

  private IModelTypesFactory fTypesFactory;
  private PersistenceLayer fPersistanceLayer;

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

  /* Scoped Preferences */
  private IPreferencesScope fDefaultScope;
  private IPreferencesScope fGlobalScope;

  // TODO Need a better solution here
  private static final boolean DEBUG = false;

  private NewsModel() {
    fTypesFactory = loadTypesFactory();
    fPersistanceLayer = (PersistenceLayer) ExtensionUtils.loadSingletonExecutableExtension(PERSISTANCE_LAYER_EXTENSION_POINT);
    Assert.isNotNull(fTypesFactory);
    Assert.isNotNull(fPersistanceLayer);
  }

  private void startup() {
    initScopedPreferences();
  }

  private void initScopedPreferences() {
    fDefaultScope = new DefaultScope();
    fGlobalScope = new GlobalScope(fDefaultScope);

    /* Pass Service through Initializers */
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(PREFERENCES_INITIALIZER_EXTENSION_POINT);
    for (IConfigurationElement element : elements) {
      try {
        IPreferencesInitializer initializer = (IPreferencesInitializer) element.createExecutableExtension("class"); //$NON-NLS-1$
        initializer.initialize(fDefaultScope);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  /**
   * @return Returns the root of all Model Objects.
   */
  public static NewsModel getDefault() {
    if (fInstance == null) {
      fInstance = new NewsModel();
      fInstance.startup();
    }
    return fInstance;
  }

  /**
   * The default scope can be used to intialize default preferences. It is the
   * most-outer Scope with no parent scope at all. None of the values stored in
   * the default scope is persisted.
   * 
   * @return The Default Scope for Preferences.
   */
  public IPreferencesScope getDefaultScope() {
    return fDefaultScope;
  }

  /**
   * The global scope stores global preferences. Most entity-scopes will be
   * initialized with the values of the global scope.
   * 
   * @return The Global Scope for Preferences.
   */
  public IPreferencesScope getGlobalScope() {
    return fGlobalScope;
  }

  /**
   * The entity scope stores preferences in the given entity itself.
   * 
   * @param entity The Entity to be used for the Scope.
   * @return The Entity Scope for Preferences as defined by the given Entity.
   */
  public IPreferencesScope getEntityScope(IEntity entity) {
    return new EntityScope(entity, fGlobalScope);
  }

  /**
   * @return Returns the types factory used to instantiate the object model.
   */
  public IModelTypesFactory getTypesFactory() {
    return fTypesFactory;
  }

  /* Load Model Types Factory contribution */
  private IModelTypesFactory loadTypesFactory() {
    IModelTypesFactory defaultFactory = new DefaultModelTypesFactory();
    return (IModelTypesFactory) ExtensionUtils.loadSingletonExecutableExtension(MODEL_TYPESFACTORY_EXTENSION_POINT, defaultFactory);
  }

  /**
   * @return The Persistance Layer of the Application.
   */
  public PersistenceLayer getPersistenceLayer() {
    return fPersistanceLayer;
  }

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * an attachment is added, deleted or updated, by calling one of the methods
   * defined in the <code>AttachmentListener</code> interface.
   * 
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addAttachmentListener(AttachmentListener listener) {
    fAttachmentListeners.add(listener);
  }

  /**
   * Removes the listener from the collection of listeners who will be notified
   * when an attachment is added, deleted or updated.
   * 
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeAttachmentListener(AttachmentListener listener) {
    fAttachmentListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyAttachmentAdded(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.attachmentAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyAttachmentDeleted(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.attachmentDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyAttachmentUpdated(final Set<AttachmentEvent> events) {
    Object listeners[] = fAttachmentListeners.getListeners();
    for (Object element : listeners) {
      final AttachmentListener listener = (AttachmentListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.attachmentUpdated(events);
        }
      });
    }
  }

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * a bookmark is added, deleted or updated, by calling one of the methods
   * defined in the <code>BookMarkListener</code> interface.
   * 
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addBookMarkListener(BookMarkListener listener) {
    fBookMarkListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeBookMarkListener(BookMarkListener listener) {
    fBookMarkListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.bookMarkAdded(events);
        }
      });
    }
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

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.bookMarkDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.bookMarkUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addCategoryListener(CategoryListener listener) {
    fCategoryListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeCategoryListener(CategoryListener listener) {
    fCategoryListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyCategoryAdded(final Set<CategoryEvent> events) {
    Object listeners[] = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.categoryAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyCategoryDeleted(final Set<CategoryEvent> events) {
    Object listeners[] = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.categoryDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyCategoryUpdated(final Set<CategoryEvent> events) {
    Object[] listeners = fCategoryListeners.getListeners();
    for (Object element : listeners) {
      final CategoryListener listener = (CategoryListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.categoryUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addFeedListener(FeedListener listener) {
    fFeedListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeFeedListener(FeedListener listener) {
    fFeedListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyFeedAdded(final Set<FeedEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object[] listeners = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.feedAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyFeedDeleted(final Set<FeedEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.feedDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyFeedUpdated(final Set<FeedEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fFeedListeners.getListeners();
    for (Object element : listeners) {
      final FeedListener listener = (FeedListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.feedUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addFolderListener(FolderListener listener) {
    fFolderListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeFolderListener(FolderListener listener) {
    fFolderListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.folderAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.folderDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
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
          listener.folderUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addLabelListener(LabelListener listener) {
    fLabelListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeLabelListener(LabelListener listener) {
    fLabelListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyLabelAdded(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.labelAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyLabelDeleted(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.labelDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyLabelUpdated(final Set<LabelEvent> events) {
    Object listeners[] = fLabelListeners.getListeners();
    for (Object element : listeners) {
      final LabelListener listener = (LabelListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.labelUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addNewsListener(NewsListener listener) {
    fNewsListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeNewsListener(NewsListener listener) {
    fNewsListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyNewsAdded(final Set<NewsEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.newsAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyNewsDeleted(final Set<NewsEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.newsDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifyNewsUpdated(final Set<NewsEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fNewsListeners.getListeners();
    for (Object element : listeners) {
      final NewsListener listener = (NewsListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.newsUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addPersonListener(PersonListener listener) {
    fPersonListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removePersonListener(PersonListener listener) {
    fPersonListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyPersonAdded(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.personAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyPersonDeleted(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.personDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifyPersonUpdated(final Set<PersonEvent> events) {
    Object listeners[] = fPersonListeners.getListeners();
    for (Object element : listeners) {
      final PersonListener listener = (PersonListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.personUpdated(events);
        }
      });
    }
  }

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  public void addSearchMarkListener(SearchMarkListener listener) {
    fSearchMarkListeners.add(listener);
  }

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  public void removeSearchMarkListener(SearchMarkListener listener) {
    fSearchMarkListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkAdded(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.PERSIST);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchMarkAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkDeleted(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.REMOVE);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchMarkDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  public void notifySearchMarkUpdated(final Set<SearchMarkEvent> events) {
    logEvents(events, EventType.UPDATE);

    Object listeners[] = fSearchMarkListeners.getListeners();
    for (Object element : listeners) {
      final SearchMarkListener listener = (SearchMarkListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchMarkUpdated(events);
        }
      });
    }
  }

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
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

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
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

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
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

  /**
   * @param listener the PreferencesListener to add to the list of listeners.
   */
  public void addPreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.add(listener);
  }

  /**
   * @param listener the PreferencesListener to remove to the list of listeners.
   */
  public void removePreferencesListener(PreferencesListener listener) {
    fPreferencesListeners.remove(listener);
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifySearchConditionAdded(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchConditionAdded(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifySearchConditionDeleted(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchConditionDeleted(events);
        }
      });
    }
  }

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  public void notifySearchConditionUpdated(final Set<SearchConditionEvent> events) {
    Object listeners[] = fSearchConditionListeners.getListeners();
    for (Object element : listeners) {
      final SearchConditionListener listener = (SearchConditionListener) element;
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.searchConditionUpdated(events);
        }
      });
    }
  }

  /**
   * <p>
   * Note: The default implementation of RSSOwl will <em>never</em> issue any
   * of Event of this kind. But it might be used in 3d-Party contributions.
   * </p>
   * 
   * @param listener the SearchConditionListener to add to the list of
   * listeners.
   */
  public void addSearchConditionListener(SearchConditionListener listener) {
    fSearchConditionListeners.add(listener);
  }

  /**
   * @param listener the SearchConditionListener to remove to the list of
   * listeners.
   */
  public void removeSearchConditionListener(SearchConditionListener listener) {
    fSearchConditionListeners.remove(listener);
  }
}