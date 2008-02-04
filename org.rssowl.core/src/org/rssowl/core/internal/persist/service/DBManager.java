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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.NewsBin;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.migration.MigrationResult;
import org.rssowl.core.internal.persist.migration.Migrations;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.LongOperationMonitor;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.config.Configuration;
import com.db4o.config.ObjectClass;
import com.db4o.config.ObjectField;
import com.db4o.config.QueryEvaluationMode;
import com.db4o.ext.DatabaseFileLockedException;
import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {
  private static final int MAX_BACKUPS_COUNT = 2;
  private static final String FORMAT_FILE_NAME = "format2";
  private static DBManager fInstance;
  private ObjectContainer fObjectContainer;
  private final ReadWriteLock fLock = new ReentrantReadWriteLock();
  private final List<DatabaseListener> fEntityStoreListeners = new CopyOnWriteArrayList<DatabaseListener>();

  /**
   * @return The Singleton Instance.
   */
  public static DBManager getDefault() {
    if (fInstance == null)
      fInstance = new DBManager();
    return fInstance;
  }

  /**
   * Load and initialize the contributed DataBase.
   * @param monitor
   *
   * @throws PersistenceException In case of an error while initializing and loading the
   * contributed DataBase.
   */
  public void startup(LongOperationMonitor monitor) throws PersistenceException {
    /* Initialise */
    EventManager.getInstance();

    createDatabase(monitor);
  }

  public void addEntityStoreListener(DatabaseListener listener) {
    if (listener instanceof EventManager)
      fEntityStoreListeners.add(0, listener);
    else if (listener instanceof DB4OIDGenerator) {
      if (fEntityStoreListeners.get(0) instanceof EventManager)
        fEntityStoreListeners.add(1, listener);
      else
        fEntityStoreListeners.add(0, listener);
    } else
      fEntityStoreListeners.add(listener);
  }

  private void fireDatabaseEvent(DatabaseEvent event, boolean storeOpened) {
    for (DatabaseListener listener : fEntityStoreListeners) {
      if (storeOpened) {
        listener.databaseOpened(event);
      } else {
        listener.databaseClosed(event);
      }
    }
  }

  private ObjectContainer createObjectContainer(Configuration config) {
    BackupService backupService = createOnlineBackupService();
    backupService.backup(true);
    try {
      fObjectContainer = Db4o.openFile(config, getDBFilePath());
    } catch (Exception e) {
      if (e instanceof DatabaseFileLockedException)
        throw (DatabaseFileLockedException) e;

      /*
       * Most recent back-up is the one we did before we opened the object
       * container, we look at the second most recent to try a recovery.
       */
      File backupFile = backupService.getBackupFile(1);

      /* There was no online back-up file. Should not happen. */
      if (backupFile == null)
        throw new PersistenceException("Database file corrupted and there is no online back-up", e);

      backupService.rotateFileToBackupAsCorrupted();
      DBHelper.rename(backupFile, backupService.getFileToBackup());
      try {
        fObjectContainer = Db4o.openFile(config, getDBFilePath());
      } catch (Db4oException e1) {
        Activator.getDefault().logError("Current and backup database files corrupted, logging exception for backup database and rethrowing exception for current database", e1);
        throw new PersistenceException(e);
      }
    }
    return fObjectContainer;
  }

  private BackupService createOnlineBackupService() {
    BackupService backupService = new BackupService(new File(getDBFilePath()), ".onlinebak", 2);
    return backupService;
  }

  /**
   * @return the File indicating whether defragment should be run or not.
   */
  public File getDefragmentFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    return new File(dir, "defragment");
  }

  /**
   * Internal method, exposed for tests only.
   * @return the path to the db file.
   */
  public static final String getDBFilePath() {
    String filePath = Activator.getDefault().getStateLocation().toOSString() + "/rssowl.db"; //$NON-NLS-1$
    return filePath;
  }

  private File getDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, FORMAT_FILE_NAME);
    return formatFile;
  }

  public void removeEntityStoreListener(DatabaseListener listener) {
    fEntityStoreListeners.remove(listener);
  }

  public void createDatabase(LongOperationMonitor progressMonitor) throws PersistenceException {
    Configuration config = createConfiguration();
    int workspaceVersion = getWorkspaceFormatVersion();
    MigrationResult migrationResult = new MigrationResult(false, false, false);

    SubMonitor subMonitor = null;
    try {
      if (workspaceVersion != getCurrentFormatVersion()) {
        progressMonitor.beginLongOperation();
        subMonitor = SubMonitor.convert(progressMonitor, "Please wait while RSSOwl migrates data to the new version", 100);
        //TODO Have a better way to allocate the ticks to the child. We need
        //to be able to do it dynamically based on whether a reindex is required or not.
        migrationResult = migrate(workspaceVersion, getCurrentFormatVersion(), subMonitor.newChild(70));
      }

      boolean scheduledBackupRequired = false;
      if (!defragmentIfNecessary(progressMonitor, subMonitor)) {
        if (migrationResult.isDefragmentDatabase())
          defragment(progressMonitor, subMonitor);
        /*
         * We only run the time-based back-up if a defragment has not taken
         * place because we always back-up during defragment. We perform it
         * after opening the object container because we can make the copy from
         * the online back-up async.
         */
        else
          scheduledBackupRequired = true;
      }

      fObjectContainer = createObjectContainer(config);

      if (scheduledBackupRequired)
        scheduledBackup();

      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), true);

      /*
       * If reindexRequired is true, subMonitor is guaranteed to be non-null
       */
      IModelSearch modelSearch = InternalOwl.getDefault().getPersistenceService().getModelSearch();
      if (migrationResult.isReindex() || migrationResult.isOptimizeIndex())
        modelSearch.startup();

      if (migrationResult.isReindex())
        modelSearch.reindexAll(subMonitor.newChild(20));

      if (migrationResult.isOptimizeIndex())
        modelSearch.optimize();

    } finally {
      /*
       * If we perform the migration, the subMonitor is not null. Otherwise we
       * don't show progress.
       */
      if (subMonitor != null)
        progressMonitor.done();
    }
  }

  private BackupService createBackupService(Long backupFrequency) {
    return new BackupService(new File(getDBFilePath()), ".backup", MAX_BACKUPS_COUNT, getDBLastBackUpFile(), backupFrequency);
  }

  private void scheduledBackup() {
    if (!new File(getDBFilePath()).exists())
      return;


    new Job("") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        long sevenDays = 1000 * 60 * 60 * 24 * 7;
        BackupService backupService = createBackupService(sevenDays);
        backupService.setFileToBackupAlias(createOnlineBackupService().getBackupFile());
        backupService.backup(false);
        return Status.OK_STATUS;
      }
    }.schedule(5000);
  }

  public File getDBLastBackUpFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File lastBackUpFile = new File(dir, "lastbackup");
    return lastBackUpFile;
  }

  private MigrationResult migrate(final int workspaceFormat, int currentFormat, IProgressMonitor progressMonitor) {
    ConfigurationFactory configFactory = new ConfigurationFactory() {
      public Configuration createConfiguration() {
        return DBManager.createConfiguration();
      }
    };
    Migration migration = new Migrations().getMigration(workspaceFormat, currentFormat);
    if (migration == null) {
      throw new PersistenceException("No migration found for originFormat: " + workspaceFormat + ", and destinationFormat: " + currentFormat);
    }

    final File dbFile = new File(getDBFilePath());
    final String backupFileSuffix = ".mig.";

    /*
     * Copy the db file to a permanent back-up where the file name includes the
     * workspaceFormat number. This will only be deleted after another migration.
     */
    final BackupService backupService = new BackupService(dbFile, backupFileSuffix + workspaceFormat, 1);
    backupService.setLayoutStrategy(new BackupService.BackupLayoutStrategy() {
      public List<File> findBackupFiles() {
        List<File> backupFiles = new ArrayList<File>(3);
        for (int i = workspaceFormat; i >= 0; --i) {
          File file = new File(dbFile.getAbsoluteFile() + backupFileSuffix + i);
          if (file.exists())
            backupFiles.add(file);
        }
        return backupFiles;
      }

      public void rotateBackups(List<File> backupFiles) {
        throw new UnsupportedOperationException("No rotation supported because maxBackupCount is 1");
      }
    });
    backupService.backup(true);

    /* Create a copy of the db file to use for the migration */
    File migDbFile = backupService.getTempBackupFile();
    DBHelper.copyFile(dbFile, migDbFile);

    /* Migrate the copy */
    MigrationResult migrationResult = migration.migrate(configFactory, migDbFile.getAbsolutePath(), progressMonitor);

    File dbFormatFile = getDBFormatFile();
    File migFormatFile = new File(dbFormatFile.getAbsolutePath() + ".mig.temp");
    try {
      if (!migFormatFile.exists()) {
        migFormatFile.createNewFile();
      }
      if (!dbFormatFile.exists()) {
        dbFormatFile.createNewFile();
      }
    } catch (IOException ioe) {
      throw new PersistenceException("Failed to migrate data", ioe); //$NON-NLS-1$
    }
    setFormatVersion(migFormatFile);

    DBHelper.rename(migFormatFile, dbFormatFile);

    /* Finally, rename the actual db file */
    DBHelper.rename(migDbFile, dbFile);

    return migrationResult;
  }

  private File getOldDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, "format");
    return formatFile;
  }

  private int getWorkspaceFormatVersion() {
    boolean dbFileExists = new File(getDBFilePath()).exists();
    File formatFile = getDBFormatFile();
    boolean formatFileExists = formatFile.exists();

    //TODO Remove this after M8 release
    if (!formatFileExists && getOldDBFormatFile().exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(getOldDBFormatFile()));
        String text = reader.readLine();
        DBHelper.writeToFile(formatFile, text);
        formatFileExists = true;
      } catch (IOException e) {
        throw new PersistenceException(e);
      } finally {
        DBHelper.closeQuietly(reader);
      }
      getOldDBFormatFile().delete();
    }

    if (dbFileExists) {
      /* Assume that it's M5a if no format file exists, but a db file exists */
      if (!formatFileExists)
        return 0;

      String versionText = DBHelper.readFirstLineFromFile(formatFile);
      try {
        int version = Integer.parseInt(versionText);
        return version;
      } catch (NumberFormatException e) {
        throw new PersistenceException("Format file does not contain a number as the version", e);
      }
    }
    /*
     * In case there is no database file, we just set the version as the current
     * version.
     */
    if (!formatFileExists) {
      try {
        formatFile.createNewFile();
      } catch (IOException ioe) {
        throw new PersistenceException("Error creating database", ioe); //$NON-NLS-1$
      }
    }
    setFormatVersion(formatFile);
    return getCurrentFormatVersion();
  }

  private void setFormatVersion(File formatFile) {
    DBHelper.writeToFile(formatFile, String.valueOf(getCurrentFormatVersion()));
  }

  private int getCurrentFormatVersion() {
    return 5;
  }

  private boolean defragmentIfNecessary(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    File defragmentFile = getDefragmentFile();
    if (!defragmentFile.exists()) {
      return false;
    }
    defragment(progressMonitor, subMonitor);
    if (!defragmentFile.delete()) {
      Activator.getDefault().logError("Failed to delete defragment file", null);
    }
    return true;
  }

  private void defragment(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    SubMonitor monitor;
    if (subMonitor == null) {
      progressMonitor.beginLongOperation();
      String monitorText = "Please wait while RSSOwl cleans up the database";
      subMonitor = SubMonitor.convert(progressMonitor, monitorText, 100);
      monitor = subMonitor.newChild(100);

      /*
       * This should not be needed, but things don't work properly when it's
       * not called.
       */
      monitor.beginTask(monitorText, 100);
    } else {
      monitor = subMonitor.newChild(10);
      monitor.setWorkRemaining(100);
    }

    BackupService backupService = createBackupService(null);
    File file = new File(getDBFilePath());
    File defragmentedFile = backupService.getTempBackupFile();
    copyDatabase(file, defragmentedFile, monitor);
    DBHelper.rename(defragmentedFile, file);
  }

  /**
   * Internal method. Made public for testing.
   *
   * Creates a copy of the database that has all essential data structures.
   * At the moment, this means not copying NewsCounter and
   * IConditionalGets since they will be re-populated eventually.
   * @param source
   * @param destination
   * @param monitor
   *
   */
  public final static void copyDatabase(File source, File destination, IProgressMonitor monitor) {
    ObjectContainer sourceDb = Db4o.openFile(createConfiguration(), source.getAbsolutePath());
    ObjectContainer destinationDb = Db4o.openFile(createConfiguration(), destination.getAbsolutePath());

    /*
     * Keep labels in memory to avoid duplicate copies when cascading feed.
     */
    List<Label> labels = new ArrayList<Label>();
    for (Label label : sourceDb.query(Label.class)) {
      labels.add(label);
      sourceDb.activate(label, Integer.MAX_VALUE);
      destinationDb.ext().set(label, Integer.MAX_VALUE);
    }
    monitor.worked(5);
    for (Folder type : sourceDb.query(Folder.class)) {
      sourceDb.activate(type, Integer.MAX_VALUE);
      if (type.getParent() == null) {
        destinationDb.ext().set(type, Integer.MAX_VALUE);
      }
    }
    monitor.worked(15);

    for (NewsBin newsBin : sourceDb.query(NewsBin.class)) {
      for (NewsReference newsRef : newsBin.getNewsRefs()) {
        Query query = sourceDb.query();
        query.constrain(News.class);
        query.descend("fId").constrain(newsRef.getId());
        Object news = query.execute().next();
        sourceDb.activate(news, Integer.MAX_VALUE);
        destinationDb.ext().set(news, Integer.MAX_VALUE);
      }
    }

    monitor.worked(25);

    NewsCounter newsCounter = new NewsCounter();
    for (Feed feed : sourceDb.query(Feed.class)) {
      sourceDb.activate(feed, Integer.MAX_VALUE);
      addNewsCounterItem(newsCounter, feed);
      destinationDb.ext().set(feed, Integer.MAX_VALUE);
    }
    destinationDb.ext().set(newsCounter, Integer.MAX_VALUE);
    monitor.worked(30);

    for (Description description : sourceDb.query(Description.class)) {
      sourceDb.activate(description, Integer.MAX_VALUE);
      destinationDb.ext().set(description, Integer.MAX_VALUE);
    }
    monitor.worked(10);

    for (Preference pref : sourceDb.query(Preference.class)) {
      sourceDb.activate(pref, Integer.MAX_VALUE);
      destinationDb.ext().set(pref, Integer.MAX_VALUE);
    }
    monitor.worked(5);
    List<Counter> counterSet = sourceDb.query(Counter.class);
    Counter counter = counterSet.iterator().next();
    sourceDb.activate(counter, Integer.MAX_VALUE);
    destinationDb.ext().set(counter, Integer.MAX_VALUE);

    sourceDb.close();
    destinationDb.commit();
    destinationDb.close();
    monitor.worked(10);
  }

  private static void addNewsCounterItem(NewsCounter newsCounter, Feed feed) {
    Map<State, Integer> stateToCountMap = feed.getNewsCount();
    int unreadCount = getCount(stateToCountMap, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));
    Integer newCount = stateToCountMap.get(INews.State.NEW);
    newsCounter.put(feed.getLink(), new NewsCounterItem(newCount, unreadCount, feed.getStickyCount()));
  }

  private static int getCount(Map<State, Integer> stateToCountMap, Set<State> states) {
    int count = 0;
    for (State state : states) {
      count += stateToCountMap.get(state);
    }
    return count;
  }

  /**
   * Internal method, exposed for tests only.
   *
   * @return
   */
  public static final Configuration createConfiguration() {
    Configuration config = Db4o.newConfiguration();
    //TODO We can use dbExists to configure our parameters for a more
    //efficient startup. For example, the following could be used. We'd have
    //to include a file when we need to evolve the schema or something similar
    //config.detectSchemaChanges(false)

//    config.blockSize(8);
//    config.bTreeCacheHeight(0);
//    config.bTreeNodeSize(100);
//    config.diagnostic().addListener(new DiagnosticListener() {
//      public void onDiagnostic(Diagnostic d) {
//        System.out.println(d);
//      }
//    });
//    config.messageLevel(3);

    config.lockDatabaseFile(false);
    config.queries().evaluationMode(QueryEvaluationMode.IMMEDIATE);
    config.automaticShutDown(false);
	config.callbacks(false);
    config.activationDepth(2);
    config.flushFileBuffers(false);
    config.callConstructors(true);
    config.exceptionsOnNotStorable(true);
    configureAbstractEntity(config);
    config.objectClass(BookMark.class).objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    config.objectClass(ConditionalGet.class).objectField("fLink").indexed(true); //$NON-NLS-1$
    configureFeed(config);
    configureNews(config);
    configureFolder(config);
    config.objectClass(Description.class).objectField("fNewsId").indexed(true);
    config.objectClass(NewsCounter.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).objectField("fKey").indexed(true); //$NON-NLS-1$
    return config;
  }

  private static void configureAbstractEntity(Configuration config) {
    ObjectClass abstractEntityClass = config.objectClass(AbstractEntity.class);
    ObjectField idField = abstractEntityClass.objectField("fId");
    idField.indexed(true);
    idField.cascadeOnActivate(true);
    abstractEntityClass.objectField("fProperties").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureFolder(Configuration config) {
    ObjectClass oc = config.objectClass(Folder.class);
    oc.objectField("fChildren").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureNews(Configuration config) {
    ObjectClass oc = config.objectClass(News.class);

    /* Indexes */
    oc.objectField("fParentId").indexed(true);
    oc.objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    oc.objectField("fStateOrdinal").indexed(true); //$NON-NLS-1$
  }

  private static void configureFeed(Configuration config)  {
    ObjectClass oc = config.objectClass(Feed.class);

    ObjectField linkText = oc.objectField("fLinkText"); //$NON-NLS-1$
    linkText.indexed(true);
    linkText.cascadeOnActivate(true);

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$
  }

  /**
   * Shutdown the contributed Database.
   *
   * @throws PersistenceException In case of an error while shutting down the contributed
   * DataBase.
   */
  public void shutdown() throws PersistenceException {
    fLock.writeLock().lock();
    try {
      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), false);
      if (fObjectContainer != null)
        while (!fObjectContainer.close());
    } finally {
      fLock.writeLock().unlock();
    }
  }

  public void dropDatabase() throws PersistenceException {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        shutdown();
        if (!new File(getDBFilePath()).delete()) {
          Activator.getDefault().logError("Failed to delete db file", null); //$NON-NLS-1$
        }
        if (!getDBFormatFile().delete()) {
          Activator.getDefault().logError("Failed to delete db format file", null); //$NON-NLS-1$
        }
      }
    });
  }

  public final ObjectContainer getObjectContainer() {
    return fObjectContainer;
  }
}
