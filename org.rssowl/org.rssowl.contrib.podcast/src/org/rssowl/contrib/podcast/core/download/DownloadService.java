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

package org.rssowl.contrib.podcast.core.download;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.events.BookMarkAdapter;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.preferences.IPreferencesScope;
import org.rssowl.core.model.types.IBookMark;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.JobQueue;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * A Service managing automatic reload of Feeds in RSSOwl based on the user
 * preferences.
 * <p>
 * TODO Re-Think the current strategy to ignore reloads on startup from Feeds
 * that are not set to update in a certain interval.
 * </p>
 * 
 * @author bpasero
 */
public class DownloadService {

	/* Max. number of concurrent running reload Jobs */
	private static final int MAX_CONCURRENT_DOWNLOAD_JOBS = 5;

	/* The delay-threshold in millis (5 Minutes) */
	private static final int DELAY_THRESHOLD = 5 * 60 * 1000;

	/* The delay-value in millis (30 Seconds) */
	private static final int DELAY_VALUE = 30 * 1000;

	/* Listen to Bookmark Updates */
	private BookMarkAdapter fBookMarkListener;

	/* Map IBookMark to Update-Intervals */
	private Map<IBookMark, Long> fMapBookMarkToInterval;

	/* Queue for downloading from Feeds */
	private final JobQueue fDownloadQueue;

	/*
	 * This subclass of a Job is making sure to delay the operation for <code>WAKEUP_DELAY</code>
	 * millis in case it is detecting that the last run of the Job was some
	 * amount of time (<code>DELAY_THRESHOLD</code>) after it was meant to
	 * be run due to the given Update-Interval. This fixes a problem, where all
	 * Update-Jobs would immediately run after waking up from an OS hibernate
	 * (e.g. on Windows). Since all Jobs are scheduled based on a time-dif, once
	 * waking up from hibernate, the dif is usually telling the Jobs to schedule
	 * immediately, even before network interfaces had any chance to start.
	 * Thus, all BookMarks will show errors.
	 */
	private class DownloadJob extends Job {
		private IBookMark fBookMark;
		private long fLastRunInMillis;

		DownloadJob(IBookMark bookMark, String name) {
			super(name);
			fBookMark = bookMark;
			fLastRunInMillis = System.currentTimeMillis();
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			/* Update Interval of this BookMark */
			Long updateIntervalInSeconds = fMapBookMarkToInterval
					.get(fBookMark);

			/* Delay execution if required */
			if (delay(updateIntervalInSeconds) && !monitor.isCanceled()) {
				try {
					Thread.sleep(DELAY_VALUE);
				} catch (InterruptedException e) {
					/* Ignore */
				}
			}

			/* Update field */
			fLastRunInMillis = System.currentTimeMillis();

			/* Reload */
			if (!monitor.isCanceled()) {

				// This where the reload is executed.
				// Controller.getDefault().reloadQueued(fBookMark, null);
			}

			/* Re-Schedule */
			if (!monitor.isCanceled() && updateIntervalInSeconds != null)
				schedule(updateIntervalInSeconds * 1000);

			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family.equals(fBookMark)
					|| family.equals(DownloadService.this);
		}

		private boolean delay(Long updateIntervalInSeconds) {
			if (fLastRunInMillis == 0 || updateIntervalInSeconds == null)
				return false;

			long dif = System.currentTimeMillis() - fLastRunInMillis;
			return dif > ((updateIntervalInSeconds * 1000) + DELAY_THRESHOLD);
		}
	}

	/* Task to perform Reload-Operations */
	private class DownloadTask implements ITask {
		private final Long fId;
		private final IBookMark fBookMark;
		private final Shell fShell;
		private final Priority fPriority;

		DownloadTask(IBookMark bookmark, Shell shell, ITask.Priority priority) {
			Assert.isNotNull(bookmark);
			Assert.isNotNull(bookmark.getId());

			fBookMark = bookmark;
			fId = bookmark.getId();
			fShell = shell;
			fPriority = priority;
		}

		public IStatus run(IProgressMonitor monitor) {
			// CB TODO, code for reloading.
			// IStatus status = reload(fBookMark, fShell, monitor);
			// return status;
			return null;
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

			final DownloadTask other = (DownloadTask) obj;
			return fId.equals(other.fId);
		}
	}

	/**
	 * Initializes the download service.</br>
	 * <ul>
	 * <li>Initialize the job queue for handling downloads.</li>
	 * <li>Initiates a structure for storing the bookmark and interval.</li>
	 * <li>Registers listeners. </li>
	 * <li>In a separate thread.</li>
	 * </ul>
	 */
	public DownloadService() {

		fDownloadQueue = new JobQueue("Downloading podcasts",
				MAX_CONCURRENT_DOWNLOAD_JOBS, true, 0);

		// CB TODO Do we need a similar structure for bookmarks/intervals?
		fMapBookMarkToInterval = new HashMap<IBookMark, Long>();

		// CB TODO, specific listeners for updates,add and deletes of bookmarks.
		/* Register Listeners */
		registerListeners();

		/* Init from a Background Thread */
		JobRunner.runInBackgroundThread(new Runnable() {
			public void run() {
				init();
			}
		});
	}

	/** Unregister from Listeners and cancel all Jobs */
	public void stopService() {
		unregisterListeners();
		Job.getJobManager().cancel(this);
	}

	private void init() {

		/* Query Update Intervals and reload/open state */

		// CB TODO, here the bookmarks are fetched from a cache service.
		// For now create a local cache service, which knows about folder and 
		// bookmarks.
		CacheService fCacheService = new CacheService();
		fCacheService.cacheRootFolders();

		Set<IBookMark> bookmarks = fCacheService.getBookMarks();

		final List<IBookMark> bookmarksToOpenOnStartup = new ArrayList<IBookMark>();
		for (IBookMark bookMark : bookmarks) {

			IPreferencesScope entityPreferences = NewsModel.getDefault()
					.getEntityScope(bookMark);

			/* BookMark is to reload in a certain Interval */
			if (entityPreferences
					.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE)) {
				long updateInterval = entityPreferences
						.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
				fMapBookMarkToInterval.put(bookMark, updateInterval);

				/* BookMark is to reload on startup */
				if (entityPreferences
						.getBoolean(DefaultPreferences.BM_RELOAD_ON_STARTUP)) {
					downloadQueued(bookMark, null);
				}
			}

			/* BookMark is to open on startup */
			if (entityPreferences
					.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP))
				bookmarksToOpenOnStartup.add(bookMark);
		}

		/* Initialize the Jobs that manages Updates */
		Set<Entry<IBookMark, Long>> entries = fMapBookMarkToInterval.entrySet();
		for (Entry<IBookMark, Long> entry : entries) {
			IBookMark bookMark = entry.getKey();
			scheduleUpdate(bookMark, entry.getValue());
		}

		/* Open BookMarks which are to open on startup */
		// if (!bookmarksToOpenOnStartup.isEmpty()) {
		// JobRunner.runInUIThread(null, new Runnable() {
		// public void run() {
		// boolean activateEditor = OpenStrategy.activateOnOpen();
		// int openEditorLimit = EditorUtils.getOpenEditorLimit();
		// IWorkbenchWindow wWindow =
		// PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		// IWorkbenchPage page = wWindow != null ? wWindow.getActivePage() :
		// null;
		//
		// if (page != null) {
		// for (int i = 0; i < bookmarksToOpenOnStartup.size() && i <
		// openEditorLimit; i++) {
		// try {
		// IBookMark bookMarkToOpen = bookmarksToOpenOnStartup.get(i);
		// page.openEditor(new FeedViewInput(bookMarkToOpen), FeedView.ID,
		// activateEditor);
		// } catch (PartInitException e) {
		// Activator.getDefault().getLog().log(e.getStatus());
		// }
		// }
		// }
		// }
		// });
		// }
	}

	/**
	 * Download attachments from the given BookMark. The BookMark is processed
	 * in a queue that stores all Tasks of this kind and guarantees that a
	 * certain amount of Jobs process the Task concurrently.
	 * 
	 * @param bookmark
	 *            The BookMark to download attachments from.
	 * @param shell
	 *            The Shell this operation is running in, used to open Dialogs
	 *            if necessary.
	 */
	public void downloadQueued(IBookMark bookmark, final Shell shell) {

		/* Create a Task for the Bookmark to Reload */
		DownloadTask task = new DownloadTask(bookmark, shell,
				ITask.Priority.DEFAULT);

		/* Check if Task is not yet Queued already */
		if (!fDownloadQueue.isQueued(task))
			fDownloadQueue.schedule(task);
	}

	private void scheduleUpdate(final IBookMark bookMark, Long intervalInSeconds) {
		Job downloadJob = new DownloadJob(bookMark, "Auto-Update Service");
		downloadJob.setSystem(true);
		downloadJob.schedule(intervalInSeconds * 1000);
	}

	private void registerListeners() {
		fBookMarkListener = new BookMarkAdapter() {

			@Override
			public void bookMarkAdded(Set<BookMarkEvent> events) {
				onBookMarksAdded(events);
			}

			@Override
			public void bookMarkUpdated(Set<BookMarkEvent> events) {
				onBookMarksUpdated(events);
			}

			@Override
			public void bookMarkDeleted(Set<BookMarkEvent> events) {
				onBookMarksDeleted(events);
			}
		};

		NewsModel.getDefault().addBookMarkListener(fBookMarkListener);
	}

	private void unregisterListeners() {
		NewsModel.getDefault().removeBookMarkListener(fBookMarkListener);
	}

	private void onBookMarksAdded(Set<BookMarkEvent> events) {
		for (BookMarkEvent event : events) {
			IBookMark addedBookMark = event.getEntity();
			IPreferencesScope entityPreferences = NewsModel.getDefault()
					.getEntityScope(addedBookMark);

			Long interval = entityPreferences
					.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
			boolean autoUpdateState = entityPreferences
					.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE);

			/* BookMark wants to Auto-Update */
			if (autoUpdateState)
				addUpdate(event.getEntity(), interval);
		}
	}

	private void onBookMarksUpdated(Set<BookMarkEvent> events) {
		for (BookMarkEvent event : events) {
			IBookMark updatedBookMark = event.getEntity();
			sync(updatedBookMark);
		}
	}

	private void onBookMarksDeleted(Set<BookMarkEvent> events) {
		for (BookMarkEvent event : events) {
			removeUpdate(event.getEntity());
		}
	}

	/**
	 * Synchronizes the reload-service on the given BookMark. Performs no
	 * operation in case the given bookmarks update-interval is matching the
	 * stored one.
	 * 
	 * @param updatedBookmark
	 *            The Bookmark to synchronize with the reload-service.
	 */
	public void sync(IBookMark updatedBookmark) {
		IPreferencesScope entityPreferences = NewsModel.getDefault()
				.getEntityScope(updatedBookmark);

		Long oldInterval = fMapBookMarkToInterval.get(updatedBookmark);
		Long newInterval = entityPreferences
				.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);

		boolean autoUpdateState = entityPreferences
				.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE);

		/* BookMark known to the Service */
		if (oldInterval != null) {

			/* BookMark no longer Auto-Updating */
			if (!autoUpdateState)
				removeUpdate(updatedBookmark);

			/* New Interval different to Old Interval */
			else if (!newInterval.equals(oldInterval)) {
				Job.getJobManager().cancel(updatedBookmark);
				fMapBookMarkToInterval.put(updatedBookmark, newInterval);
				scheduleUpdate(updatedBookmark, newInterval);
			}
		}

		/* BookMark not yet known to the Service and wants to Auto-Update */
		else if (autoUpdateState) {
			addUpdate(updatedBookmark, newInterval);
		}
	}

	private void removeUpdate(IBookMark bookmark) {
		fMapBookMarkToInterval.remove(bookmark);
		Job.getJobManager().cancel(bookmark);
	}

	private void addUpdate(IBookMark bookmark, Long intervalInSeconds) {
		fMapBookMarkToInterval.put(bookmark, intervalInSeconds);
		scheduleUpdate(bookmark, intervalInSeconds);
	}
}