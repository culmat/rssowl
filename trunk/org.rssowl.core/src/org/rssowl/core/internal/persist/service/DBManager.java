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
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.migration.Migrations;
import org.rssowl.core.persist.NewsCounter;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {
  private static final String FORMAT_FILE_NAME = "format";
  private static DBManager fInstance;
  private ObjectContainer fObjectContainer;
  private final ReadWriteLock fLock = new ReentrantReadWriteLock();
  private final ListenerList fEntityStoreListeners = new ListenerList();

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
    fEntityStoreListeners.add(listener);
  }

  private void fireDatabaseEvent(DatabaseEvent event, boolean storeOpened) {
    for (Object l : fEntityStoreListeners.getListeners()) {
      DatabaseListener listener = (DatabaseListener) l;
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

  private String getDBFilePath() {
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
    boolean reindexRequired = false;

    SubMonitor subMonitor = null;
    try {
      if (workspaceVersion != getCurrentFormatVersion()) {
        progressMonitor.beginLongOperation();
        subMonitor = SubMonitor.convert(progressMonitor, "Please wait while RSSOwl migrates data to the new version", 100);
        reindexRequired = migrate(workspaceVersion, getCurrentFormatVersion(), subMonitor.newChild(10));
      }

      defragmentIfNecessary();

      fObjectContainer = createObjectContainer(config);

      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), true);

      /*
       * If reindexRequired is true, subMonitor is guaranteed to be non-null,
       * but we have the check anyway to
       */
      if (subMonitor != null && reindexRequired) {
        IModelSearch modelSearch = InternalOwl.getDefault().getPersistenceService().getModelSearch();
        modelSearch.startup();
        modelSearch.reindexAll(subMonitor.newChild(90));
      }
    } finally {
      /*
       * If we perform the migration, the subMonitor is not null. Otherwise we
       * don't show progress.
       */
      if (subMonitor != null)
        progressMonitor.done();
    }
  }

  private boolean migrate(int workspaceFormat, int currentFormat, IProgressMonitor progressMonitor) {
    ConfigurationFactory configFactory = new ConfigurationFactory() {
      public Configuration createConfiguration() {
        return DBManager.this.createConfiguration();
      }
    };
    Migration migration = new Migrations().getMigration(workspaceFormat, currentFormat);
    if (migration == null) {
      throw new PersistenceException("No migration found for originFormat: " + workspaceFormat + ", and destinationFormat: " + currentFormat);
    }

    /* Create a copy of the db file to use for the migration */
    File dbFile = new File(getDBFilePath());
    String migDbFileName = getDBFilePath() + ".mig";
    File migDbFile = new File(migDbFileName);
    copyFile(dbFile, migDbFile);

    /* Migrate the copy */
    boolean reindexRequired = migration.migrate(configFactory, migDbFileName, progressMonitor);

    /*
     * Copy the db file to a permanent back where the file name includes the
     * workspaceFormat number.
     */
    File backupDbFile = new File(getDBFilePath() + ".bak." + workspaceFormat);
    copyFile(dbFile, backupDbFile);

    File dbFormatFile = getDBFormatFile();
    File migFormatFile = new File(dbFormatFile.getAbsolutePath() + ".mig");
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

    return reindexRequired;
  }

  private void copyFile(File originFile, File destinationFile) {
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

  private int getWorkspaceFormatVersion() {
    boolean dbFileExists = new File(getDBFilePath()).exists();
    File formatFile = getDBFormatFile();
    boolean formatFileExists = formatFile.exists();
    if (dbFileExists) {
      /* Assume that it's M5a if no format file exists, but a db file exists */
      if (!formatFileExists)
        return 0;

      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(formatFile));
        String versionText = reader.readLine();
        try {
          int version = Integer.parseInt(versionText);
          return version;
        } catch (NumberFormatException e) {
          throw new PersistenceException("Format file does not contain a number as the version", e);
        }
      } catch (IOException e) {
        throw new PersistenceException(e);
      } finally {
        closeCloseable(reader);
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

  private void closeCloseable(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      } catch (IOException e) {
        Activator.getDefault().logError("Failed to close stream.", e);
      }
  }

  private void setFormatVersion(File formatFile) {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(formatFile));
      String s = String.valueOf(getCurrentFormatVersion());
      writer.write(s);
      writer.flush();
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeCloseable(writer);
    }
  }

  private int getCurrentFormatVersion() {
    return 1;
  }

  private void defragmentIfNecessary() {
    File defragmentFile = getDefragmentFile();
    if (!defragmentFile.exists()) {
      return;
    }

    File file = new File(getDBFilePath());
    File backupFile = getDBBackupFile();
    if (!file.renameTo(backupFile)) {
      throw new PersistenceException("Failed to rename file: " + file + " to: "
          + backupFile);
    }
    copyDatabase(backupFile, file);
    if (!defragmentFile.delete()) {
      Activator.getDefault().logError("Failed to delete defragment file", null);
    }
  }

  @SuppressWarnings("unused")
  private void db4oDefrag(Configuration config, File defragmentFile) {
    DefragmentConfig defragConfig = new DefragmentConfig(getDBFilePath(),
        getDBBackupFile().getAbsolutePath());
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

  private File getDBBackupFile() {
    String dbFilePath = getDBFilePath();
    String backupFilePath = dbFilePath + ".backup";
    File backupFile = new File(backupFilePath);
    int index = 1;
    while (backupFile.exists()) {
      backupFile = new File(backupFile + "." + index++);
    }
    return backupFile;
  }

  /**
   * Creates a copy of the database that has all essential data structures.
   * At the moment, this means not copying NewsCounter and
   * IConditionalGets since they will be re-populated eventually.
   *
   */
  @SuppressWarnings("unused")
  private static void copyDatabase(File source, File destination) {
      ObjectContainer sourceDb = Db4o.openFile(createConfiguration(),
          source.getAbsolutePath());
      ObjectContainer destinationDb = Db4o.openFile(createConfiguration(),
          destination.getAbsolutePath());

      /*
       * Keep labels in memory to avoid duplicate copies when cascading feed.
       */
      List<Label> labels = new ArrayList<Label>();
      for (Label label : sourceDb.query(Label.class)) {
        labels.add(label);
        sourceDb.activate(label, Integer.MAX_VALUE);
        destinationDb.ext().set(label, Integer.MAX_VALUE);
      }
      for (Folder type : sourceDb.query(Folder.class))  {
        sourceDb.activate(type, Integer.MAX_VALUE);
        if (type.getParent() == null) {
          destinationDb.ext().set(type, Integer.MAX_VALUE);
        }
      }
      for (Feed feed : sourceDb.query(Feed.class)) {
        sourceDb.activate(feed, Integer.MAX_VALUE);
        destinationDb.ext().set(feed, Integer.MAX_VALUE);
      }
      for (Preference pref : sourceDb.query(Preference.class)) {
        sourceDb.activate(pref, Integer.MAX_VALUE);
        destinationDb.ext().set(pref, Integer.MAX_VALUE);
      }
      List<Counter> counterSet = sourceDb.query(Counter.class);
      Counter counter = counterSet.iterator().next();
      sourceDb.activate(counter, Integer.MAX_VALUE);
      destinationDb.ext().set(counter, Integer.MAX_VALUE);

      destinationDb.commit();
      destinationDb.close();
  }

  private static Configuration createConfiguration() {
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

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$

    /* Indexes */
    oc.objectField("fLinkText").indexed(true); //$NON-NLS-1$
    oc.objectField("fGuidValue").indexed(true); //$NON-NLS-1$
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
