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
package org.rssowl.core.internal.persist.dao.xstream;

import org.rssowl.core.internal.persist.service.Counter;
import org.rssowl.core.internal.persist.service.XStreamHelper;
import org.rssowl.core.persist.service.IDGenerator;

import com.thoughtworks.xstream.XStream;

import java.io.File;

public class XStreamIDGenerator implements IDGenerator  {
  private final static int BATCH_SIZE = 5000;

  private XStream fXStream;
  private File fBaseDir;
  private Counter fCounter;

  private long fCurrent = -1;
  private long fMax;

  /**
   * Creates an instance of this class.
   */
  public XStreamIDGenerator() {
    super();
  }

  public synchronized void startup(XStream xStream, File baseDir) {
    fXStream = xStream;
    fBaseDir = baseDir;
    if (getCounterFile().exists())
      fCounter = XStreamHelper.fromXML(fXStream, Counter.class, getCounterFile());
    else
      fCounter = new Counter(0L);

    fCurrent = fCounter.getValue();
    fMax = increaseMax();
  }

  public synchronized long getNext() {
    checkCurrent();
    ++fCurrent;
    if(fCurrent > fMax) {
      fMax = increaseMax();
    }
    return fCurrent;
  }

  private void checkCurrent() {
    if (fCurrent == -1) {
      throw new IllegalStateException("current has not been initialised yet."); //$NON-NLS-1$
    }
  }

  private long increaseMax() {
    fCounter.increment(BATCH_SIZE);
    save();
    return fCounter.getValue();
  }

  private void save() {
    XStreamHelper.toXML(fXStream, fCounter, getCounterFile(), true);
  }

  private File getCounterFile() {
    return new File(fBaseDir, "counter.xml");
  }

  public synchronized void shutdown() {
    fMax = fCurrent;
    fCounter.setValue(fCurrent + 1);
    save();
  }
}
