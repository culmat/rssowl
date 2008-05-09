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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.Attachment;
import org.rssowl.core.internal.persist.Category;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.Person;
import org.rssowl.core.internal.persist.service.Counter;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.DBManager;
import org.rssowl.core.internal.persist.service.FeedSummaries;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.internal.persist.service.NewsSummaries;
import org.rssowl.core.persist.service.AbstractPersistenceService;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.LongOperationMonitor;

import com.thoughtworks.xstream.XStream;

import java.io.File;

public class XStreamPersistenceService extends AbstractPersistenceService  {

  private volatile IStatus fStartupStatus;
  private volatile XStream fXStream;
  private volatile File fBaseDir;
  private volatile ManualEventManager fEventManager;

  @Override
  public void startup(LongOperationMonitor monitor) throws PersistenceException {
    super.startup(monitor);
    doStartup();
    ((XStreamIDGenerator) getIDGenerator()).startup(fXStream, fBaseDir);
    //FIXME How do we handle exceptions on start-up?
    ((XStreamDAOService) getDAOService()).startup(fBaseDir, fXStream, getIDGenerator(), fEventManager);
    ((XStreamApplicationService) InternalOwl.getDefault().getApplicationService()).startup(DBHelper.getDescriptionDAO(), (XStreamFeedDAO) getDAOService().getFeedDAO(), getDAOService().getBookMarkDAO(), getDAOService().getNewsCounterDAO(), fEventManager);
    DBManager.getDefault().startup(monitor);
    getModelSearch().startup();
    fStartupStatus = Status.OK_STATUS;
  }

  private void doStartup() {
    fEventManager = new ManualEventManager();
    fBaseDir = new File(Activator.getDefault().getStateLocation().toFile(), "xstream");
    if (!fBaseDir.exists())
      fBaseDir.mkdirs();

    fXStream = createXStream();
  }

  private XStream createXStream() {
    XStream xStream = new XStream();
    xStream.alias("feed", Feed.class);
    xStream.alias("person", Person.class);
    xStream.alias("news", News.class);
    xStream.alias("category", Category.class);
    xStream.alias("label", Label.class);
    xStream.alias("attachment", Attachment.class);
    xStream.alias("counter", Counter.class);
    xStream.alias("feedSummaries", FeedSummaries.class);
    xStream.alias("description", Description.class);
    xStream.alias("newsSummaries", NewsSummaries.class);
    return xStream;
  }

  public IStatus getStartupStatus() {
    return fStartupStatus;
  }

  public void optimizeOnNextStartup() throws PersistenceException {
    /* Do nothing */
  }

  public void recreateSchema() throws PersistenceException {
    // TODO Auto-generated method stub
  }

  public void shutdown(boolean emergency) throws PersistenceException {
    if (!emergency)
      getIDGenerator().shutdown();

    if (!emergency) {
      getModelSearch().shutdown(emergency);
    } else {
      getModelSearch().shutdown(emergency);
    }
  }
}
