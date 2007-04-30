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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.SearchMark;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.util.LoggingSafeRunnable;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.config.Configuration;
import com.db4o.config.ObjectClass;
import com.db4o.config.ObjectField;
import com.db4o.config.QueryEvaluationMode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {

  private static final String DATABASE_EXISTS_FILE_NAME = "databaseExists"; //$NON-NLS-1$
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
   *
   * @throws DBException In case of an error while initializing and loading the
   * contributed DataBase.
   */
  public void startup() throws DBException {
    /* Initialise */
    EventManager.getInstance();

    createDatabase();
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
    fObjectContainer.ext().configure().queries().evaluationMode(QueryEvaluationMode.IMMEDIATE);
    return fObjectContainer;
  }

  private final String getDBFilePath() {
    String filePath = Activator.getDefault().getStateLocation().toOSString() + "/rssowl.db"; //$NON-NLS-1$
    return filePath;
  }

  private File getDBExistsFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File existsFile = new File(dir + DATABASE_EXISTS_FILE_NAME);
    return existsFile;
  }

  public void removeEntityStoreListener(DatabaseListener listener) {
    fEntityStoreListeners.remove(listener);
  }

  public void createDatabase() throws DBException {
//    ConfigurationFactory configFactory = new ConfigurationFactory() {
//      public Configuration createConfiguration() {
//        return DBManager.this.createConfiguration();
//      }
//    };
//    new Migration20M5M6(configFactory, getDBFilePath());
    
    Configuration config = createConfiguration();
    File dbExistsFile = getDBExistsFile();
    boolean dbExists = dbExistsFile.exists();
    if (!dbExists) {
      try {
        dbExistsFile.createNewFile();
      } catch (IOException ioe) {
        throw new DBException(Activator.getDefault().createErrorStatus(
            "Error creating database", ioe)); //$NON-NLS-1$
      }
    }

    fObjectContainer = createObjectContainer(config);

    fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), true);
//    copyDatabase();
  }

  /**
   * Creates a copy of the database that has all essential data structures.
   * At the moment, this means not copying NewsCounter and
   * IConditionalGets since they will be repopulated eventually.
   *
   * TODO Allow new db file name to be set
   * TODO Allow replacing old db with copy
   * TODO Provide a full copy mode (that includes IConditionalGet an NewsCounter)
   */
  @SuppressWarnings("unused")
  private void copyDatabase() {
      ObjectContainer db = Db4o.openFile(getDBFilePath() + "50");
      db.ext().configure().queries().evaluationMode(QueryEvaluationMode.IMMEDIATE);
      for (Folder type : fObjectContainer.query(Folder.class))  {
        fObjectContainer.activate(type, Integer.MAX_VALUE);
        if (type.getParent() == null) {
          db.ext().set(type, Integer.MAX_VALUE);
        }
      }
      for (Feed feed : fObjectContainer.query(Feed.class)) {
        fObjectContainer.activate(feed, Integer.MAX_VALUE);
        db.ext().set(feed, Integer.MAX_VALUE);
      }
      for (Preference pref : fObjectContainer.query(Preference.class)) {
        fObjectContainer.activate(pref, Integer.MAX_VALUE);
        db.ext().set(pref, Integer.MAX_VALUE);
      }
      List<Counter> counterSet = fObjectContainer.query(Counter.class);
      Counter counter = counterSet.iterator().next();
      fObjectContainer.activate(counter, Integer.MAX_VALUE);
      db.ext().set(counter, Integer.MAX_VALUE);

      db.commit();
  }

  private Configuration createConfiguration() {
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
    configureSearchMark(config);
    configureFolder(config);
    config.objectClass(NewsCounter.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).objectField("fKey").indexed(true); //$NON-NLS-1$
    return config;
  }

  private void configureAbstractEntity(Configuration config) {
    ObjectClass abstractEntityClass = config.objectClass(AbstractEntity.class);
    ObjectField idField = abstractEntityClass.objectField("fId");
    idField.indexed(true); //$NON-NLS-1$
    idField.cascadeOnActivate(true); //$NON-NLS-1$
    abstractEntityClass.objectField("fProperties").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private void configureSearchMark(Configuration config) {
    ObjectClass oc = config.objectClass(SearchMark.class);
    oc.objectField("fSearchConditions").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private void configureFolder(Configuration config) {
    ObjectClass oc = config.objectClass(Folder.class);
    oc.objectField("fFolders").cascadeOnUpdate(true); //$NON-NLS-1$
    oc.objectField("fMarks").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private void configureNews(Configuration config) {
    ObjectClass oc = config.objectClass(News.class);

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$

    /* Indexes */
    oc.objectField("fLinkText").indexed(true); //$NON-NLS-1$
    oc.objectField("fGuidValue").indexed(true); //$NON-NLS-1$
  }

  private void configureFeed(Configuration config)  {
    ObjectClass oc = config.objectClass(Feed.class);

    ObjectField linkText = oc.objectField("fLinkText"); //$NON-NLS-1$
    linkText.indexed(true);
    linkText.cascadeOnActivate(true);

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$
  }

  /**
   * Shutdown the contributed Database.
   *
   * @throws DBException In case of an error while shutting down the contributed
   * DataBase.
   */
  public void shutdown() throws DBException {
    fLock.writeLock().lock();
    try {
      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), false);
      if (fObjectContainer != null)
        while (!fObjectContainer.close());
    } finally {
      fLock.writeLock().unlock();
    }
  }

  public void dropDatabase() throws DBException {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        shutdown();
        if (!new File(getDBFilePath()).delete()) {
          Activator.getDefault().logError("Failed to delete db file", null); //$NON-NLS-1$
        }
        if (!getDBExistsFile().delete()) {
          Activator.getDefault().logError("Failed to delete dbExistsFile", null); //$NON-NLS-1$
        }
      }
    });
  }

  public final ObjectContainer getObjectContainer() {
    return fObjectContainer;
  }
}
