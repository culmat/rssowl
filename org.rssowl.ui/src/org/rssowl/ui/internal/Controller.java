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

package org.rssowl.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.IApplicationService;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.connection.UnknownFeedException;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.IConditionalGetDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.JobQueue;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.dialogs.LoginDialog;
import org.rssowl.ui.internal.dialogs.properties.EntityPropertyPageWrapper;
import org.rssowl.ui.internal.editors.feed.NewsGrouping;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Note: The Controller should not be responsible for handling Exceptions, e.g.
 * showing a Dialog when Authentication is required. Its only responsible for
 * calling the appropiate methods on a complex operation like loading a Feed.
 * </p>
 * <p>
 * Note: As required by the UI, the controller should be filled with more
 * methods.
 * </p>
 *
 * @author bpasero
 */
public class Controller {

  /* Extension-Points */
  private static final String ENTITY_PROPERTY_PAGE_EXTENSION_POINT = "org.rssowl.ui.EntityPropertyPage"; //$NON-NLS-1$

  /* The Singleton Instance */
  private static Controller fInstance;

  /* Token to ask the DB if this is the first start of RSSOwl */
  private static final String FIRST_START_TOKEN = "org.rssowl.ui.FirstStartToken"; //$NON-NLS-1$

  /* Max. number of concurrent running reload Jobs */
  private static final int MAX_CONCURRENT_RELOAD_JOBS = 10;

  /* Max. number of concurrent Jobs for saving a Feed */
  private static final int MAX_CONCURRENT_SAVE_JOBS = 1;

  /* Max number of jobs in the queue used for saving feeds before it blocks */
  private static final int MAX_SAVE_QUEUE_SIZE = 1;

  /* Connection Timeouts in MS */
  private static final int FEED_CON_TIMEOUT = 30000;

  /* Flag indicating if RSSOwl is starting up for the first time */
  static boolean fgFirstStartup;

  /* Queue for reloading Feeds */
  private final JobQueue fReloadFeedQueue;

  /* Queue for saving Feeds */
  private final JobQueue fSaveFeedQueue;

  /* News-Service */
  private NewsService fNewsService;

  /* Notification Service */
  private NotificationService fNotificationService;

  /* Saved Search Service */
  private SavedSearchService fSavedSearchService;

  /* Feed-Reload Service */
  private FeedReloadService fFeedReloadService;

  /* Contributed Entity-Property-Pages */
  final List<EntityPropertyPageWrapper> fEntityPropertyPages;

  /* Flag is set to TRUE when shutting down the application */
  private boolean fShuttingDown;

  /* Service to access some cached Entities */
  private CacheService fCacheService;

  /* Service to manage Contexts */
  private ContextService fContextService;

  /* Misc. */
  private IApplicationService fAppService;
  private IBookMarkDAO fBookMarkDAO;
  private IFolderDAO fFolderDAO;
  private IConditionalGetDAO fConditionalGetDAO;
  private IPreferenceDAO fPrefsDAO;
  private ILabelDAO fLabelDao;
  private IModelFactory fFactory;
  private Lock fLoginDialogLock = new ReentrantLock();

  /* Task to perform Reload-Operations */
  private class ReloadTask implements ITask {
    private final Long fId;
    private final IBookMark fBookMark;
    private final Shell fShell;
    private final Priority fPriority;

    ReloadTask(IBookMark bookmark, Shell shell, ITask.Priority priority) {
      Assert.isNotNull(bookmark);
      Assert.isNotNull(bookmark.getId());

      fBookMark = bookmark;
      fId = bookmark.getId();
      fShell = shell;
      fPriority = priority;
    }

    public IStatus run(IProgressMonitor monitor) {
      IStatus status = reload(fBookMark, fShell, monitor);
      return status;
    }

    public String getName() {
      return fBookMark.getName();
    }

    public Priority getPriority() {
      return fPriority;
    }

    @Override
    public int hashCode() {
      return fId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;

      if (obj == null)
        return false;

      if (getClass() != obj.getClass())
        return false;

      final ReloadTask other = (ReloadTask) obj;
      return fId.equals(other.fId);
    }
  }

  private Controller() {
    fReloadFeedQueue = new JobQueue("Updating Feeds", MAX_CONCURRENT_RELOAD_JOBS, Integer.MAX_VALUE, true, 0);
    fSaveFeedQueue = new JobQueue("Updating Feeds", MAX_CONCURRENT_SAVE_JOBS, MAX_SAVE_QUEUE_SIZE, true, 0);
    fSaveFeedQueue.setUnknownProgress(true);
    fEntityPropertyPages = loadEntityPropertyPages();
    fBookMarkDAO = DynamicDAO.getDAO(IBookMarkDAO.class);
    fConditionalGetDAO = DynamicDAO.getDAO(IConditionalGetDAO.class);
    fFolderDAO = DynamicDAO.getDAO(IFolderDAO.class);
    fLabelDao = DynamicDAO.getDAO(ILabelDAO.class);
    fPrefsDAO = Owl.getPersistenceService().getDAOService().getPreferencesDAO();
    fAppService = Owl.getApplicationService();
    fFactory = Owl.getModelFactory();
  }

  private List<EntityPropertyPageWrapper> loadEntityPropertyPages() {
    List<EntityPropertyPageWrapper> pages = new ArrayList<EntityPropertyPageWrapper>();

    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(ENTITY_PROPERTY_PAGE_EXTENSION_POINT);

    /* For each contributed property Page */
    for (IConfigurationElement element : elements) {
      try {
        String id = element.getAttribute("id");
        String name = element.getAttribute("name");
        int order = Integer.valueOf(element.getAttribute("order"));
        boolean handlesMultipleEntities = Boolean.valueOf(element.getAttribute("handlesMultipleEntities"));

        List<Class<?>> targetEntities = new ArrayList<Class<?>>();
        IConfigurationElement[] entityTargets = element.getChildren("targetEntity");
        for (IConfigurationElement entityTarget : entityTargets)
          targetEntities.add(Class.forName(entityTarget.getAttribute("class")));

        pages.add(new EntityPropertyPageWrapper(id, element, targetEntities, name, order, handlesMultipleEntities));
      } catch (ClassNotFoundException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return pages;
  }

  /**
   * @return The Singleton Instance.
   */
  public static Controller getDefault() {
    if (fInstance == null)
      fInstance = new Controller();

    return fInstance;
  }

  /**
   * @param entities
   * @return The EntityPropertyPageWrappers for the given Entity.
   */
  public Set<EntityPropertyPageWrapper> getEntityPropertyPagesFor(List<IEntity> entities) {
    Set<EntityPropertyPageWrapper> pages = new HashSet<EntityPropertyPageWrapper>();

    /* Retrieve Class-Objects from Entities */
    Set<Class<? extends IEntity>> entityClasses = new HashSet<Class<? extends IEntity>>();
    for (IEntity entity : entities)
      entityClasses.add(entity.getClass());

    /* For each contributed Entity Property-Page */
    for (EntityPropertyPageWrapper pageWrapper : fEntityPropertyPages) {

      /* Ignore Pages that dont handle Multi-Selection */
      if (!pageWrapper.isHandlingMultipleEntities() && entities.size() > 1)
        continue;

      /* Check if Page is handling all of the given Entity-Classes */
      if (pageWrapper.handles(entityClasses))
        pages.add(pageWrapper);
    }

    return pages;
  }

  /**
   * @return Returns the newsService.
   */
  public NewsService getNewsService() {
    return fNewsService;
  }

  /**
   * @return Returns the savedSearchService.
   */
  public SavedSearchService getSavedSearchService() {
    return fSavedSearchService;
  }

  /**
   * @return Returns the contextService.
   */
  public ContextService getContextService() {

    /* Create the Context Service if not yet done */
    if (fContextService == null)
      fContextService = new ContextService();

    return fContextService;
  }

  /**
   * @return Returns the cacheService.
   */
  public CacheService getCacheService() {
    return fCacheService;
  }

  /**
   * @return Returns the JobFamily all reload-jobs belong to.
   */
  public Object getReloadFamily() {
    return fReloadFeedQueue;
  }

  /**
   * @return Returns the reload-service.
   */
  public FeedReloadService getReloadService() {
    return fFeedReloadService;
  }

  /**
   * Reload the given List of BookMarks. The BookMarks are processed in a queue
   * that stores all Tasks of this kind and guarantees that a certain amount of
   * Jobs process the Task concurrently.
   *
   * @param bookmarks The BookMarks to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary.
   */
  public void reloadQueued(Set<IBookMark> bookmarks, final Shell shell) {

    /* Decide wether this is a high prio Job */
    boolean highPrio = bookmarks.size() == 1;

    /* Create a Task for each Feed to Reload */
    List<ITask> tasks = new ArrayList<ITask>();
    for (final IBookMark bookmark : bookmarks) {
      ReloadTask task = new ReloadTask(bookmark, shell, highPrio ? ITask.Priority.SHORT : ITask.Priority.DEFAULT);

      /* Check if Task is not yet Queued already */
      if (!fReloadFeedQueue.isQueued(task))
        tasks.add(task);
    }

    fReloadFeedQueue.schedule(tasks);
  }

  /**
   * Reload the given BookMark. The BookMark is processed in a queue that stores
   * all Tasks of this kind and guarantees that a certain amount of Jobs process
   * the Task concurrently.
   *
   * @param bookmark The BookMark to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary.
   */
  public void reloadQueued(IBookMark bookmark, final Shell shell) {

    /* Create a Task for the Bookmark to Reload */
    ReloadTask task = new ReloadTask(bookmark, shell, ITask.Priority.DEFAULT);

    /* Check if Task is not yet Queued already */
    if (!fReloadFeedQueue.isQueued(task))
      fReloadFeedQueue.schedule(task);
  }

  /**
   * Reload the given BookMark.
   *
   * @param bookmark The BookMark to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary, or <code>NULL</code> if no Shell is available.
   * @param monitor A monitor to report progress and respond to cancelation. Use
   * a <code>NullProgressMonitor</code> if no progress is to be reported.
   * @return Returns the Status of the Operation.
   */
  public IStatus reload(final IBookMark bookmark, final Shell shell, final IProgressMonitor monitor) {
    Assert.isNotNull(bookmark);
    CoreException ex = null;

    /* Keep URL of Feed as final var */
    final URI feedLink = bookmark.getFeedLinkReference().getLink();

    try {

      /* Return on Cancelation or shutdown */
      if (monitor.isCanceled() || fShuttingDown)
        return Status.CANCEL_STATUS;

      /* Load Conditional Get for the URL */
      IConditionalGet conditionalGet = fConditionalGetDAO.load(feedLink);

      /* Define Properties for Connection */
      Map<Object, Object> properties = new HashMap<Object, Object>();
      properties.put(IConnectionPropertyConstants.CON_TIMEOUT, FEED_CON_TIMEOUT);

      /* Add Conditional GET Headers if present */
      if (conditionalGet != null) {
        String ifModifiedSince = conditionalGet.getIfModifiedSince();
        if (ifModifiedSince != null)
          properties.put(IConnectionPropertyConstants.IF_MODIFIED_SINCE, ifModifiedSince);

        String ifNoneMatch = conditionalGet.getIfNoneMatch();
        if (ifNoneMatch != null)
          properties.put(IConnectionPropertyConstants.IF_NONE_MATCH, ifNoneMatch);
      }

      /* Load the Feed */
      final Pair<IFeed, IConditionalGet> pairResult = Owl.getConnectionService().reload(feedLink, monitor, properties);

      /* Return on Cancelation or Shutdown */
      if (monitor.isCanceled() || fShuttingDown)
        return Status.CANCEL_STATUS;

      /* Update ConditionalGet Entity */
      boolean conditionalGetIsNull = (conditionalGet == null);
      conditionalGet = updateConditionalGet(feedLink, conditionalGet, pairResult.getSecond());
      boolean deleteConditionalGet = (!conditionalGetIsNull && conditionalGet == null);

      /* Return on Cancelation or Shutdown */
      if (monitor.isCanceled() || fShuttingDown)
        return Status.CANCEL_STATUS;

      /* Load the Favicon directly afterwards if required */
      if (!InternalOwl.PERF_TESTING && OwlUI.getFavicon(bookmark) == null) {
        try {
          byte[] faviconBytes = null;

          /* First try using the Homepage of the Feed */
          URI homepage = pairResult.getFirst().getHomepage();
          if (homepage != null && StringUtils.isSet(homepage.toString()))
            faviconBytes = Owl.getConnectionService().getFeedIcon(homepage);

          /* Then try with Feed address itself */
          if (faviconBytes == null)
            faviconBytes = Owl.getConnectionService().getFeedIcon(feedLink);

          /* Store locally */
          if (!monitor.isCanceled() && !fShuttingDown)
            OwlUI.storeImage(bookmark.getId(), faviconBytes, OwlUI.BOOKMARK);
        } catch (UnknownFeedException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }

      /* Return on Cancelation or Shutdown */
      if (monitor.isCanceled() || fShuttingDown)
        return Status.CANCEL_STATUS;

      /* Merge and Save Feed */
      if (!InternalOwl.TESTING) {
        final IConditionalGet finalConditionalGet = conditionalGet;
        final boolean finalDeleteConditionalGet = deleteConditionalGet;
        fSaveFeedQueue.schedule(new TaskAdapter() {
          public IStatus run(IProgressMonitor monitor) {

            /* Return on Cancelation or Shutdown */
            if (monitor.isCanceled() || fShuttingDown)
              return Status.CANCEL_STATUS;

            /* Handle Feed Reload */
            fAppService.handleFeedReload(bookmark, pairResult.getFirst(), finalConditionalGet, finalDeleteConditionalGet);
            return Status.OK_STATUS;
          }

          @Override
          public String getName() {
            return "Updating Feeds";
          }
        });
      } else {
        fAppService.handleFeedReload(bookmark, pairResult.getFirst(), conditionalGet, deleteConditionalGet);
      }
    }

    /* Error while reloading */
    catch (CoreException e) {
      ex = e;

      /* Authentication Required */
      if (e instanceof AuthenticationRequiredException && shell != null && !shell.isDisposed() && !fShuttingDown) {

        /* Only one Login Dialog at the same time */
        fLoginDialogLock.lock();
        try {
          final AuthenticationRequiredException authEx = (AuthenticationRequiredException) e;
          JobRunner.runSyncedInUIThread(shell, new Runnable() {
            public void run() {

              /* Check for shutdown flag and return if required */
              if (fShuttingDown || monitor.isCanceled())
                return;

              /* Credentials might have been provided meanwhile in another dialog */
              try {
                URI normalizedUri = URIUtils.normalizeUri(feedLink, true);
                if (!fShuttingDown && Owl.getConnectionService().getAuthCredentials(normalizedUri, authEx.getRealm()) != null) {
                  reloadQueued(bookmark, shell);
                  return;
                }
              } catch (CredentialsException exe) {
                Activator.getDefault().getLog().log(exe.getStatus());
              }

              /* Show Login Dialog */
              LoginDialog login = new LoginDialog(shell, feedLink, authEx.getRealm());
              if (login.open() == Window.OK && !fShuttingDown)
                reloadQueued(bookmark, shell);

              /* Update Error Flag if user hit Cancel */
              else if (!fShuttingDown && !monitor.isCanceled() && !bookmark.isErrorLoading()) {
                bookmark.setErrorLoading(true);
                fBookMarkDAO.save(bookmark);
              }
            }
          });

          return Status.OK_STATUS;
        } finally {
          fLoginDialogLock.unlock();
        }
      }

      /* Feed's Content has not modified since */
      else if (e instanceof NotModifiedException) {
        return Status.OK_STATUS;
      }

      /* Load the Favicon directly afterwards if required */
      else if ((e instanceof InterpreterException || e instanceof ParserException) && OwlUI.getFavicon(bookmark) == null && !fShuttingDown) {
        try {
          byte[] faviconBytes = Owl.getConnectionService().getFeedIcon(feedLink);
          OwlUI.storeImage(bookmark.getId(), faviconBytes, OwlUI.BOOKMARK);
        } catch (ConnectionException exe) {
          Activator.getDefault().getLog().log(exe.getStatus());
        }
      }

      return createWarningStatus(e.getStatus(), bookmark, feedLink);
    }

    /* Save Error State to the Bookmark if present */
    finally {
      updateErrorIndicator(bookmark, monitor, ex);
    }

    return Status.OK_STATUS;
  }

  private void updateErrorIndicator(final IBookMark bookmark, final IProgressMonitor monitor, CoreException ex) {

    /* Return on Cancelation or Shutdown */
    if (monitor.isCanceled() || fShuttingDown)
      return;

    /* Reset Error-Loading flag if necessary */
    if (bookmark.isErrorLoading() && (ex == null || ex instanceof NotModifiedException)) {
      bookmark.setErrorLoading(false);
      fBookMarkDAO.save(bookmark);
    }

    /* Set Error-Loading flag if necessary */
    else if (!bookmark.isErrorLoading() && ex != null && !(ex instanceof NotModifiedException) && !(ex instanceof AuthenticationRequiredException)) {
      bookmark.setErrorLoading(true);
      fBookMarkDAO.save(bookmark);
    }
  }

  /*
   * Note that this does not save the conditional get, it just updates the its
   * values.
   */
  private IConditionalGet updateConditionalGet(final URI feedLink, IConditionalGet oldConditionalGet, IConditionalGet newConditionalGet) {

    /* Conditional Get not provided, return */
    if (newConditionalGet == null)
      return null;

    String ifModifiedSince = newConditionalGet.getIfModifiedSince();
    String ifNoneMatch = newConditionalGet.getIfNoneMatch();
    if (ifModifiedSince != null || ifNoneMatch != null) {

      /* Create new */
      if (oldConditionalGet == null)
        return fFactory.createConditionalGet(ifModifiedSince, feedLink, ifNoneMatch);

      /* Else: Update old */
      oldConditionalGet.setHeaders(ifModifiedSince, ifNoneMatch);
      return oldConditionalGet;
    }

    return null;
  }

  /**
   * Tells the Controller to start. This method is called automatically from
   * osgi as soon as the org.rssowl.ui bundle gets activated.
   */
  public void startup() {

    /* Create Relations and Import Default Feeds if required */
    if (!InternalOwl.TESTING) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {

          /* First check wether this action is required */
          IPreference firstStartToken = fPrefsDAO.load(FIRST_START_TOKEN);
          if (firstStartToken != null)
            return;

          onFirstStartup();

          /* Mark this as the first start */
          fPrefsDAO.save(fFactory.createPreference(FIRST_START_TOKEN));
        }
      });
    }

    /* Create the Cache-Service */
    if (!InternalOwl.TESTING) {
      fCacheService = new CacheService();
      fCacheService.cacheRootFolders();
    }

    /* Create the News-Service */
    fNewsService = new NewsService();

    /* Create the Notification Service */
    if (!InternalOwl.TESTING)
      fNotificationService = new NotificationService();

    /* Create the Saved Search Service */
    if (!InternalOwl.TESTING)
      fSavedSearchService = new SavedSearchService();
  }

  /**
   * Tells the Controller to stop. This method is called automatically from osgi
   * as soon as the org.rssowl.ui bundle gets stopped.
   *
   * @param emergency If set to <code>TRUE</code>, this method is called from
   * a shutdown hook that got triggered from a non-normal shutdown (e.g. System
   * Shutdown).
   */
  public void shutdown(boolean emergency) {
    fShuttingDown = true;

    /* Stop the Feed Reload Service */
    if (!InternalOwl.TESTING && !emergency)
      fFeedReloadService.stopService();

    /* Cancel the reload queue */
    if (!emergency)
      fReloadFeedQueue.cancel(false);

    /* Stop the Cache-Service */
    if (!InternalOwl.TESTING && !emergency)
      fCacheService.stopService();

    /* Cancel the feed-save queue (join) */
    if (!emergency)
      fSaveFeedQueue.cancel(true);

    /* Stop the News-Service */
    fNewsService.stopService();

    /* Stop the Notification Service */
    if (!InternalOwl.TESTING && !emergency)
      fNotificationService.stopService();

    /* Stop the Saved Search Service */
    if (!InternalOwl.TESTING && !emergency)
      fSavedSearchService.stopService();

    /* Shutdown ApplicationServer */
    if (!emergency)
      ApplicationServer.getDefault().shutdown();
  }

  /**
   * This method is called just after the windows have been opened.
   */
  public void postUIStartup() {

    /* Create the Feed-Reload Service */
    if (!InternalOwl.TESTING)
      fFeedReloadService = new FeedReloadService();
  }

  /**
   * Returns wether the application is in process of shutting down.
   *
   * @return <code>TRUE</code> if the application has been closed, and
   * <code>FALSE</code> otherwise.
   */
  public boolean isShuttingDown() {
    return fShuttingDown;
  }

  /**
   * This method is called immediately prior to workbench shutdown before any
   * windows have been closed.
   *
   * @return <code>true</code> to allow the workbench to proceed with
   * shutdown, <code>false</code> to veto a non-forced shutdown
   */
  public boolean preUIShutdown() {
    fShuttingDown = true;

    return true;
  }

  private void onFirstStartup() throws PersistenceException, InterpreterException, ParserException {

    /* Add Default Labels */
    List<ILabel> labels = addDefaultLabels();

    /* Import Default Marks */
    importDefaults(labels);

    /* Remember this */
    fgFirstStartup = true;
  }

  private List<ILabel> addDefaultLabels() throws PersistenceException {
    List<ILabel> labels = new ArrayList<ILabel>();

    ILabel label = fFactory.createLabel(null, "Important");
    label.setColor("159,63,63");
    labels.add(label);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, "Work");
    label.setColor("255,153,0");
    labels.add(label);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, "Personal");
    label.setColor("0,153,0");
    labels.add(label);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, "To Do");
    label.setColor("51,51,255");
    labels.add(label);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, "Later");
    label.setColor("151,53,151");
    labels.add(label);
    fLabelDao.save(label);

    return labels;
  }

  private void importDefaults(List<ILabel> labels) throws PersistenceException, InterpreterException, ParserException {

    /* Import Default Feeds */
    InputStream inS = getClass().getResourceAsStream("/default_feeds.xml"); //$NON-NLS-1$;
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(inS);
    IFolder imported = (IFolder) types.get(0);
    imported.setName("Default"); //$NON-NLS-1$

    /* Create Default SearchMarks */
    String newsEntityName = INews.class.getName();

    /* SearchCondition: New and Updated News */
    {
      ISearchMark mark = fFactory.createSearchMark(null, imported, "New and Updated News");

      ISearchField field1 = fFactory.createSearchField(INews.STATE, newsEntityName);
      fFactory.createSearchCondition(null, mark, field1, SearchSpecifier.IS, EnumSet.of(INews.State.NEW, INews.State.UPDATED));
    }

    /* SearchCondition: Recent News */
    {
      ISearchMark mark = fFactory.createSearchMark(null, imported, "Recent News");

      ISearchField field1 = fFactory.createSearchField(INews.AGE_IN_DAYS, newsEntityName);
      fFactory.createSearchCondition(null, mark, field1, SearchSpecifier.IS_LESS_THAN, 2);
    }

    /* SearchCondition: News with Attachments */
    {
      ISearchMark mark = fFactory.createSearchMark(null, imported, "News with Attachments");

      ISearchField field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, newsEntityName);
      fFactory.createSearchCondition(null, mark, field, SearchSpecifier.IS, true);
    }

    /* SearchCondition: Sticky News */
    {
      ISearchMark mark = fFactory.createSearchMark(null, imported, "Sticky News");

      ISearchField field = fFactory.createSearchField(INews.IS_FLAGGED, newsEntityName);
      fFactory.createSearchCondition(null, mark, field, SearchSpecifier.IS, true);
    }

    /* SearchCondition: News is Labeld */
    {
      ISearchMark mark = fFactory.createSearchMark(null, imported, "Labeled News");
      IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(mark);
      preferences.putInteger(DefaultPreferences.BM_NEWS_GROUPING, NewsGrouping.Type.GROUP_BY_LABEL.ordinal());

      for (ILabel label : labels) {
        ISearchField field = fFactory.createSearchField(INews.LABEL, newsEntityName);
        fFactory.createSearchCondition(null, mark, field, SearchSpecifier.IS, label.getName());
      }
    }

    fFolderDAO.save(imported);
  }

  /**
   * TODO Temporary
   *
   * @param fileName
   * @throws FileNotFoundException In case of an error.
   * @throws ParserException In case of an error.
   * @throws InterpreterException In case of an error.
   */
  public void importFeeds(String fileName) throws FileNotFoundException, InterpreterException, ParserException {
    List<IEntity> entitiesToReload = new ArrayList<IEntity>();

    /* Import from File */
    File file = new File(fileName);
    InputStream inS = new FileInputStream(file);
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(inS);
    IFolder defaultContainer = (IFolder) types.get(0);

    /* Load the current selected Set */
    String selectedBookMarkSetPref = BookMarkExplorer.getSelectedBookMarkSetPref(OwlUI.getWindow());
    Long selectedFolderID = fPrefsDAO.load(selectedBookMarkSetPref).getLong();
    IFolder selectedRootFolder = fFolderDAO.load(selectedFolderID);

    /* Load all Root Folders */
    Set<IFolder> rootFolders = fCacheService.getRootFolders();

    /* 1.) Handle Folders and Marks from default Container */
    {
      reparentChildren(defaultContainer, selectedRootFolder);
      entitiesToReload.addAll(defaultContainer.getChildren());
    }

    /* 2.) Handle other Sets */
    for (int i = 1; i < types.size(); i++) {
      IFolder setFolder = (IFolder) types.get(i);
      IFolder existingSetFolder = null;

      /* Check if set already exists */
      for (IFolder rootFolder : rootFolders) {
        if (rootFolder.getName().equals(setFolder.getName())) {
          existingSetFolder = rootFolder;
          break;
        }
      }

      /* Reparent into Existing Set */
      if (existingSetFolder != null) {
        reparentChildren(setFolder, existingSetFolder);
        entitiesToReload.addAll(existingSetFolder.getChildren());
      }

      /* Otherwise save as new Set */
      else {
        fFolderDAO.save(setFolder);
        entitiesToReload.add(setFolder);
      }
    }

    /* Reload imported Feeds */
    new ReloadTypesAction(new StructuredSelection(entitiesToReload), OwlUI.getPrimaryShell()).run();
  }

  private void reparentChildren(IFolder from, IFolder to) {
    boolean changed = false;

    /* Reparent all imported folders into selected Set */
    List<IFolder> folders = from.getFolders();
    for (IFolder folder : folders) {
      folder.setParent(to);
      to.addFolder(folder, null, null);
      changed = true;
    }

    /* Reparent all imported marks into selected Set */
    List<IMark> marks = from.getMarks();
    for (IMark mark : marks) {
      mark.setParent(to);
      to.addMark(mark, null, null);
      changed = true;
    }

    /* Save Set */
    if (changed)
      fFolderDAO.save(to);
  }

  private IStatus createWarningStatus(IStatus status, IBookMark bookmark, URI feedLink) {
    StringBuilder msg = new StringBuilder();
    msg.append("Error loading '").append(bookmark.getName()).append("'");

    if (StringUtils.isSet(status.getMessage()))
      msg.append("\nProblem: ").append(status.getMessage());

    msg.append("\nLink: ").append(feedLink);

    return new Status(IStatus.WARNING, status.getPlugin(), status.getCode(), msg.toString(), status.getException());
  }
}