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
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;
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
import com.db4o.defragment.Defragment;
import com.db4o.defragment.DefragmentConfig;
import com.db4o.query.Query;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {
  private static final int MAX_BACKUPS_COUNT = 3;
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
    fObjectContainer = Db4o.openFile(config, getDBFilePath());
    return fObjectContainer;
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

      if (!defragmentIfNecessary(progressMonitor, subMonitor)) {
        if (migrationResult.isDefragmentDatabase())
          defragment(progressMonitor, subMonitor);
        /*
         * We only run the time-based back-up if a defragment has not taken
         * place because we always back-up during defragment.
         */
        else
          backUpIfNecessary();
      }

      fObjectContainer = createObjectContainer(config);

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

  private void backUpIfNecessary() {
    File lastBackUpFile = getDBLastBackUpFile();
    if (!lastBackUpFile.exists()) {
      finishDBBackUp();
      return;
    }

    try {
      long lastBackUpDate = Long.parseLong(readFirstLineFromFile(lastBackUpFile));
      long now = System.currentTimeMillis();
      if (now - lastBackUpDate > (1000 * 60 * 60 * 24 * 7)) {
        File dbFile = new File(getDBFilePath());
        File backUpFile = prepareDBBackupFile();
        copyFile(dbFile, backUpFile);
        finishDBBackUp();
      }
    } catch (NumberFormatException e) {
      throw new PersistenceException("lastbackup file does not contain a number for the date as expected", e);
    }
  }

  private void finishDBBackUp() {
    File lastBackUpFile = getDBLastBackUpFile();
    if (!lastBackUpFile.exists()) {
      try {
        lastBackUpFile.createNewFile();
      } catch (IOException e) {
        throw new PersistenceException("Failed to create new file", e);
      }
    }
    writeToFile(lastBackUpFile, String.valueOf(System.currentTimeMillis()));
  }

  private String readFirstLineFromFile(File file) {
    BufferedReader reader = null;
    try {
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        reader = new BufferedReader(new FileReader(file));
      }
      String text = reader.readLine();
      return text;
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeCloseable(reader);
    }
  }

  public File getDBLastBackUpFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File lastBackUpFile = new File(dir, "lastbackup");
    return lastBackUpFile;
  }

  private MigrationResult migrate(int workspaceFormat, int currentFormat, IProgressMonitor progressMonitor) {
    ConfigurationFactory configFactory = new ConfigurationFactory() {
      public Configuration createConfiguration() {
        return DBManager.createConfiguration();
      }
    };
    Migration migration = new Migrations().getMigration(workspaceFormat, currentFormat);
    if (migration == null) {
      throw new PersistenceException("No migration found for originFormat: " + workspaceFormat + ", and destinationFormat: " + currentFormat);
    }

    /* Create a copy of the db file to use for the migration */
    File dbFile = new File(getDBFilePath());
    String migDbFileName = getDBFilePath() + ".mig.temp";
    File migDbFile = new File(migDbFileName);
    copyFile(dbFile, migDbFile);

    /* Migrate the copy */
    MigrationResult migrationResult = migration.migrate(configFactory, migDbFileName, progressMonitor);

    /*
     * Copy the db file to a permanent back-up where the file name includes the
     * workspaceFormat number.
     */
    File backupDbFile = createMigrationBackUpFile(workspaceFormat);
    copyFile(dbFile, backupDbFile);

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
      throw new PersistenceException("Error creating database", ioe); //$NON-NLS-1$
    }
    setFormatVersion(migFormatFile);

    /* If rename fails, fall-back to delete and rename */
    if (!migFormatFile.renameTo(dbFormatFile)) {
      dbFormatFile.delete();
      if (!migFormatFile.renameTo(dbFormatFile)) {
        throw new PersistenceException("Failed to migrate data.");
      }
    }

    /* Finally, rename the actual db file */
    /* If rename fails, fall-back to delete and rename */
    if (!migDbFile.renameTo(dbFile)) {
      dbFile.delete();
      if (!migDbFile.renameTo(dbFile)) {
        throw new PersistenceException("Failed to migrate data.");
      }
    }

    /* Delete old migration back-ups */
    for (int i = workspaceFormat - 1; i >= 0; --i) {
      File file = createMigrationBackUpFile(workspaceFormat);
      if (file.exists())
        file.delete();
    }

    return migrationResult;
  }

  private File createMigrationBackUpFile(int workspaceFormat) {
    return new File(getDBFilePath() + ".mig." + workspaceFormat);
  }

  /**
   * Internal method. Exposed for testing.
   * @param originFile
   * @param destinationFile
   */
  public static final void copyFile(File originFile, File destinationFile) {
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;
    try {
      inputStream = new FileInputStream(originFile);
      FileChannel srcChannel = inputStream.getChannel();

      if (!destinationFile.exists())
        destinationFile.createNewFile();

      outputStream = new FileOutputStream(destinationFile);
      FileChannel dstChannel = outputStream.getChannel();

      dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeCloseable(inputStream);
      closeCloseable(outputStream);
    }
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
        writeToFile(formatFile, text);
        formatFileExists = true;
      } catch (IOException e) {
        throw new PersistenceException(e);
      } finally {
        closeCloseable(reader);
      }
      getOldDBFormatFile().delete();
    }

    if (dbFileExists) {
      /* Assume that it's M5a if no format file exists, but a db file exists */
      if (!formatFileExists)
        return 0;

      String versionText = readFirstLineFromFile(formatFile);
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

  private static void closeCloseable(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      } catch (IOException e) {
        Activator.getDefault().logError("Failed to close stream.", e);
      }
  }

  private void writeToFile(File file, String text) {
    BufferedWriter writer = null;
    try {
      try {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        writer = new BufferedWriter(new FileWriter(file));
      }
      writer.write(text);
      writer.flush();
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeCloseable(writer);
    }
  }

  private void setFormatVersion(File formatFile) {
    writeToFile(formatFile, String.valueOf(getCurrentFormatVersion()));
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

    File file = new File(getDBFilePath());
    File backupFile = prepareDBBackupFile();
    if (!file.renameTo(backupFile)) {
      throw new PersistenceException("Failed to rename file: " + file + " to: "
          + backupFile);
    }
    finishDBBackUp();
    copyDatabase(backupFile, file, monitor);
  }

  @SuppressWarnings("unused")
  private void db4oDefrag(Configuration config, File defragmentFile) {
    DefragmentConfig defragConfig = new DefragmentConfig(getDBFilePath(),
        prepareDBBackupFile().getAbsolutePath());
    defragConfig.db4oConfig(config);
    try {
      Defragment.defrag(defragConfig);
      defragmentFile.delete();
    } catch (IOException e) {
      //FIXME We should rename the original file back, continue start-up
      //and notify the user about the failure to defragment. Also delete
      //defragment file
      throw new PersistenceException("A defragment was requested, but there was " +
      		"an error performing it", e);
    }
  }

  /**
   * @return the File for the most recent back-up file and deletes and renames
   * the other back-up files as appropriate.
   */
  private File prepareDBBackupFile() {
    String dbFilePath = getDBFilePath();
    final String backupFilePath = dbFilePath + ".backup";
    final File backupFile = new File(backupFilePath);
    int index = 0;
    List<File> backupFiles = new ArrayList<File>(3);
    File tempBackUpFile = backupFile;
    while (tempBackUpFile.exists()) {
      backupFiles.add(tempBackUpFile);
      tempBackUpFile = new File(backupFilePath + "." + index++);
    }

    /* We're creating a new back-up, so must leave one space available */
    while (backupFiles.size() > (MAX_BACKUPS_COUNT - 1)) {
      File fileToDelete = backupFiles.remove(backupFiles.size() - 1);
      if (!fileToDelete.delete()) {
        throw new PersistenceException("Failed to delete file: " + fileToDelete);
      }
    }

    while (backupFiles.size() > 0) {
      index = backupFiles.size() - 1;
      File fileToRename = backupFiles.remove(index);
      File newFile = new File(backupFilePath + "." + index);
      if (!fileToRename.renameTo(newFile)) {
        throw new PersistenceException("Failed to rename file from " + fileToRename + " to " + newFile);
      }
    }

    if (backupFile.exists()) {
      if (!backupFile.delete()) {
        throw new PersistenceException("Failed to delete file: " + backupFile);
      }
    }

    return backupFile;
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
