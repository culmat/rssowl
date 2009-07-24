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

package org.rssowl.ui.internal.services;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.JobQueue;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service to download files in a {@link JobQueue} with proper progress
 * reporting.
 *
 * @author bpasero
 */
public class DownloadService {

  /* Max. number of concurrent Jobs for downloading files */
  private static final int MAX_CONCURRENT_DOWNLOAD_JOBS = 3;

  /* Connection Timeouts in MS */
  private static final int DEFAULT_CON_TIMEOUT = 30000;

  private JobQueue fDownloadQueue;
  private Map<OutputStream, OutputStream> fOutputStreamMap = new ConcurrentHashMap<OutputStream, OutputStream>();

  /* Task for a Download */
  private class DownloadTask implements ITask {
    private URI fFile;
    private File fFolder;

    private DownloadTask(URI file, File folder) {
      fFile = file;
      fFolder = folder;
    }

    public IStatus run(IProgressMonitor monitor) {
      return internalDownload(fFile, fFolder, monitor);
    }

    public String getName() {
      return "Downloading " + fFile.toString();
    }

    public Priority getPriority() {
      return Priority.DEFAULT;
    }

    @Override
    public int hashCode() {
      return fFile.hashCode();
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
      return fFile.equals(other.fFile);
    }
  }

  /** Default Constructor to create a Download Queue */
  public DownloadService() {
    fDownloadQueue = new JobQueue("Downloading Attachments...", MAX_CONCURRENT_DOWNLOAD_JOBS, Integer.MAX_VALUE, true, 0);
    fDownloadQueue.setUnknownProgress(true);
  }

  /**
   * @param file the file to download as {@link URI}.
   * @param folder the folder to download to as {@link File}.
   */
  public void download(URI file, File folder) {
    DownloadTask task = new DownloadTask(file, folder);
    if (!fDownloadQueue.isQueued(task))
      fDownloadQueue.schedule(task);
  }

  private IStatus internalDownload(URI link, File folder, IProgressMonitor monitor) {
    try {
      IProtocolHandler handler = Owl.getConnectionService().getHandler(link);
      if (handler != null) {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(IConnectionPropertyConstants.CON_TIMEOUT, DEFAULT_CON_TIMEOUT);

        /* Check for Cancellation and Shutdown */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
          return Status.CANCEL_STATUS;

        /* First Download to a temporary File */
        byte[] buffer = new byte[8192];
        InputStream in = handler.openStream(link, monitor, properties);
        FileOutputStream out = null;
        String downloadFileName = URIUtils.getFile(link);
        File downloadFile = new File(folder, downloadFileName);
        File partFile = new File(folder, downloadFileName + ".part");
        try {
          partFile.createNewFile();
          partFile.deleteOnExit();

          out = new FileOutputStream(partFile);
          fOutputStreamMap.put(out, out);
          while (true) {

            /* Check for Cancellation and Shutdown */
            if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
              return Status.CANCEL_STATUS;

            /* Read from Stream */
            int read = in.read(buffer);
            if (read == -1)
              break;

            out.write(buffer, 0, read);
          }
        } catch (FileNotFoundException e) {
          return Activator.getDefault().createErrorStatus(e.getMessage(), e);
        } catch (IOException e) {
          return Activator.getDefault().createErrorStatus(e.getMessage(), e);
        } finally {
          monitor.done();

          if (in != null) {
            try {
              in.close();
            } catch (IOException e) {
              return Activator.getDefault().createErrorStatus(e.getMessage(), e);
            }
          }

          if (out != null) {
            try {
              out.close();
              fOutputStreamMap.remove(out);
            } catch (IOException e) {
              return Activator.getDefault().createErrorStatus(e.getMessage(), e);
            }
          }
        }

        /* Check for Cancellation and Shutdown */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
          return Status.CANCEL_STATUS;

        /* Now copy over the part file to the actual file in an atomic operation */
        if (!partFile.renameTo(downloadFile)) {
          downloadFile.delete();
          partFile.renameTo(downloadFile);
        }
      }
    } catch (ConnectionException e) {
      return Activator.getDefault().createErrorStatus(e.getMessage(), e);
    }

    return Status.OK_STATUS;
  }

  /**
   * Stops this Service and cancels all pending downloads.
   */
  public void stopService() {
    fDownloadQueue.cancel(false);

    /* Need to properly close yet opened Streams */
    Set<OutputStream> openStreams = fOutputStreamMap.keySet();
    for (OutputStream out : openStreams) {
      try {
        out.close();
      } catch (IOException e) {
        /* Ignore */}
    }
  }
}