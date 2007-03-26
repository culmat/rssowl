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

import org.rssowl.core.model.dao.IDGenerator;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

/**
 * db4o implementation of IDGenerator.
 */
public class DB4OIDGenerator implements IDGenerator {

  private final static int BATCH_SIZE = 100;
  
  private long fCurrent = -1;
  private long fMax;
  
  private ObjectContainer fDb;
  
  private Counter fCounter;
  
  /**
   * Creates an instance of this class.
   */
  public DB4OIDGenerator() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseClosed(DatabaseEvent event) {
        setObjectContainer(null);
      }
      public void databaseOpened(DatabaseEvent event) {
        setObjectContainer(event.getObjectContainer());
      }
    });
  }
  
  private synchronized void setObjectContainer(ObjectContainer db) {
    fDb = db;
    if (fDb == null) {
      fCurrent = -1;
      fCounter = null;
      fMax = 0;
    }
    else {
      fCounter = loadOrCreateCounter();
      fCurrent = fCounter.getValue();
      fMax = increaseMax(true);
    }
  }
  
  public long getNext() {
    return getNext(true);
  }

  /**
   * Implements the contract of {@link #getNext()} with additional control
   * over whether this method is allowed to commit a db4o transaction. This
   * should be set to <code>false</code> if this method is called from within
   * a db4o transaction. However, in the case the transaction is rolled back,
   * the ids provided during that transaction are invalid.
   * 
   * @param commit
   * @return a long value that has not been returned from this method before.
   */
  public synchronized long getNext(boolean commit) {
    checkCurrent();
    ++fCurrent;
    if(fCurrent > fMax) {
      fMax = increaseMax(commit);
    }
    return fCurrent;
  }

  private void checkCurrent() {
    if (fCurrent == -1) {
      throw new IllegalStateException("current has not been initialised yet."); //$NON-NLS-1$
    }
  }

  private long increaseMax(boolean commit) {
    fCounter.increment(BATCH_SIZE);
    fDb.set(fCounter);
    if (commit)
      fDb.commit();
    
    return fCounter.getValue();
  }
  
  public synchronized void shutdown() {
    fMax = fCurrent;
    fCounter.setValue(fCurrent + 1);
    fDb.set(fCounter);
    fDb.commit();
  }
  
  private Counter loadCounter() {
    ObjectSet<Counter> counterSet = fDb.ext().query(Counter.class);
    if (counterSet.hasNext())
      return counterSet.next();
      
    return null;
  }

  private Counter loadOrCreateCounter() {
    Counter counter = loadCounter();
    if (counter == null)
      counter = new Counter();

    return counter;
  }
}
