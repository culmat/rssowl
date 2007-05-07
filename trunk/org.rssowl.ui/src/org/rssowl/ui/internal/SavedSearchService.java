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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.IndexListener;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.SearchHit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The <code>SavedSearchService</code> is responsible to listen for updates to
 * the search-index and updating all <code>ISearchMark</code>s as a result to
 * that event in order to reflect changing search results in the UI.
 *
 * @author bpasero
 */
public class SavedSearchService {

  /* Time in millies before updating the saved searches (long) */
  private static final int BATCH_INTERVAL_LONG = 2000;

  /* Time in millies before updating the saved searches (short) */
  private static final int BATCH_INTERVAL_SHORT = 200;

  /* Number of updated documents before using the long batch interval */
  private static final int SHORT_THRESHOLD = 1;

  private final Job fBatchJob;
  private final IndexListener fIndexListener;
  private final AtomicBoolean fBatchInProcess = new AtomicBoolean(false);
  private final AtomicBoolean fUpdatedOnce = new AtomicBoolean(false);

  /** Creates and Starts this Service */
  public SavedSearchService() {
    fBatchJob = createBatchJob();
    fIndexListener = registerListeners();
  }

  private Job createBatchJob() {
    Job job = new Job("Batch Job") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        fBatchInProcess.set(false);

        /* Update all saved searches */
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            updateSavedSearches(true);
          }
        });

        return Status.OK_STATUS;
      }
    };

    job.setSystem(true);
    job.setUser(false);

    return job;
  }

  private IndexListener registerListeners() {
    IndexListener listener = new IndexListener() {
      public void indexUpdated(int docCount) {
        onIndexUpdated(docCount);
      }
    };

    Owl.getPersistenceService().getModelSearch().addIndexListener(listener);
    return listener;
  }

  private void unregisterListeners() {
    Owl.getPersistenceService().getModelSearch().removeIndexListener(fIndexListener);
  }

  private void onIndexUpdated(int docCount) {

    /* Batch is in process - return */
    if (fBatchInProcess.get())
      return;

    /* Start a new Batch */
    fBatchInProcess.set(true);
    fBatchJob.schedule(docCount > SHORT_THRESHOLD ? BATCH_INTERVAL_LONG : BATCH_INTERVAL_SHORT);
  }

  /**
   * Update the results of all <code>ISearchMark</code>s stored in RSSOwl.
   *
   * @param force If set to <code>TRUE</code>, update saved searches even if
   * done before.
   */
  public void updateSavedSearches(boolean force) {
    if (!force && fUpdatedOnce.get())
      return;

    Set<ISearchMark> searchMarks = Controller.getDefault().getCacheService().getSearchMarks();
    updateSavedSearches(searchMarks);
  }

  /**
   * @param searchMarks The Set of <code>ISearchMark</code> to update the
   * results in.
   */
  public void updateSavedSearches(Set<ISearchMark> searchMarks) {
    fUpdatedOnce.set(true);
    IModelSearch modelSearch = Owl.getPersistenceService().getModelSearch();
    Set<SearchMarkEvent> events = new HashSet<SearchMarkEvent>(searchMarks.size());

    /* For each Search Mark */
    for (ISearchMark searchMark : searchMarks) {

      /* Execute the search */
      List<SearchHit<NewsReference>> results = modelSearch.searchNews(searchMark.getSearchConditions(), searchMark.matchAllConditions());

      /* Fill Result into Buckets */
      List<NewsReference> readNews = null;
      List<NewsReference> unreadNews = null;
      List<NewsReference> newNews = null;

      for (SearchHit<NewsReference> searchHit : results) {
        INews.State state = (State) searchHit.getData(INews.STATE);

        /* Read News */
        if (state == INews.State.READ) {
          if (readNews == null)
            readNews = new ArrayList<NewsReference>(results.size() / 3);

          readNews.add(searchHit.getResult());
        }

        /* Unread or Updated News */
        else if (state == INews.State.UNREAD || state == INews.State.UPDATED) {
          if (unreadNews == null)
            unreadNews = new ArrayList<NewsReference>(results.size() / 3);

          unreadNews.add(searchHit.getResult());
        }

        /* New News */
        else if (state == INews.State.NEW) {
          if (newNews == null)
            newNews = new ArrayList<NewsReference>(results.size() / 3);

          newNews.add(searchHit.getResult());
        }
      }

      /* Set result to SearchMark */
      boolean changed = false;

      if (searchMark.setResult(readNews, INews.State.READ))
        changed = true;

      if (searchMark.setResult(unreadNews, INews.State.UNREAD))
        changed = true;

      if (searchMark.setResult(newNews, INews.State.NEW))
        changed = true;

      /* Create Event to indicate changed results if any */
      if (changed)
        events.add(new SearchMarkEvent(searchMark, null, true));
    }

    /* Notify Listeners (TODO Optimize if nothing changed!) */
    DynamicDAO.getDAO(ISearchMarkDAO.class).fireResultsChanged(events);
  }

  /** Stops this service and unregisters any listeners added. */
  public void stopService() {
    unregisterListeners();
  }
}