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

package org.rssowl.contrib.internal.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.util.ITask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * TODO Make this generic instead of being for news events only
 * </p>
 *
 * @author ijuma
 * @author bpasero
 */
public final class IndexingTask implements ITask {
  private final Indexer fIndexer;
  private final Type fTaskType;
  private final List<INews> fNews;

  /* Indexing Task Types */
  enum Type {
    ADD, UPDATE, DELETE
  }

  IndexingTask(Indexer indexer, Set<NewsEvent> events, Type taskType) {
    fIndexer = indexer;
    fNews = new ArrayList<INews>(events.size());
    for (NewsEvent event : events)
      fNews.add(event.getEntity());
    
    fTaskType = taskType;
  }

  /*
   * @see org.rssowl.core.util.ITask#getName()
   */
  public final String getName() {
    return "Indexing Feed";
  }

  /*
   * @see org.rssowl.core.util.ITask#getPriority()
   */
  public final Priority getPriority() {
    return Priority.DEFAULT;
  }

  /*
   * @see org.rssowl.core.util.ITask#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public final IStatus run(IProgressMonitor monitor) {
    switch (fTaskType) {

      /* Add the Entities of the Events to the Index */
      case ADD:
        addToIndex();
        break;

      /* Update the Entities of the Events in the Index */
      case UPDATE:
        updateIndex();
        break;

      /* Delete the Entities of the Events from the Index */
      case DELETE:
        deleteFromIndex();
        break;
    }

    return Status.OK_STATUS;
  }

  private void addToIndex() {
    fIndexer.index(fNews, false);
  }

  private void updateIndex() {
    fIndexer.index(fNews, true);
  }

  private void deleteFromIndex() {
    try {
      fIndexer.removeFromIndex(fNews);
    } catch (IOException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }
}