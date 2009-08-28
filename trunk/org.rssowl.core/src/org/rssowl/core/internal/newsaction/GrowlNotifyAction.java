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

package org.rssowl.core.internal.newsaction;

import org.rssowl.core.INewsAction;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.BatchedBuffer;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StreamGobbler;
import org.rssowl.core.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link INewsAction} to show matching news in Growl. This
 * is only supported on Mac.
 *
 * @author bpasero
 */
public class GrowlNotifyAction implements INewsAction {

  /* Batch News-Events for every 5 seconds */
  private static final int BATCH_INTERVAL = 5000;

  private static final String APPLICATION_NAME = "RSSOwl";
  private static final String SEPARATOR = System.getProperty("line.separator");

  private BatchedBuffer<INews> fBatchedBuffer;
  private String fPathToGrowlNotify;

  /** Initialize a Batched Buffer for Growl Notifications */
  public GrowlNotifyAction() {
    BatchedBuffer.Receiver<INews> receiver = new BatchedBuffer.Receiver<INews>() {
      public void receive(Set<INews> items) {
        try {
          executeCommand(fPathToGrowlNotify, items);
        }

        /* Log any error message */
        catch (IOException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    };

    fBatchedBuffer = new BatchedBuffer<INews>(receiver, BATCH_INTERVAL);
  }

  /**
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(List<INews> news, Object data) {

    /* Launch if file exists */
    if (data instanceof String && new File((String) data).exists()) {
      fPathToGrowlNotify = (String) data;
      fBatchedBuffer.addAll(news);
    }

    return Collections.emptyList();
  }

  private void executeCommand(String pathToGrowlnotify, Collection<INews> news) throws IOException {
    if (StringUtils.isSet(pathToGrowlnotify)) {
      List<String> commands = new ArrayList<String>();
      commands.add(pathToGrowlnotify);
      commands.add("--name");
      commands.add(APPLICATION_NAME);
      commands.add("-a");
      commands.add(APPLICATION_NAME);
      commands.add("-m");

      StringBuilder message = new StringBuilder();
      for (INews item : news) {
        message.append(CoreUtils.getHeadline(item, true)).append(SEPARATOR);
      }

      commands.add(message.toString());

      /* Execute */
      Process proc = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));

      /* Write Message to Growl */
      OutputStream outputStream = proc.getOutputStream();
      outputStream.write(message.toString().getBytes());
      outputStream.close();

      /* Let StreamGobbler handle error message */
      StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

      /* Let StreamGobbler handle output */
      StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

      /* Flush both error and output streams */
      errorGobbler.schedule();
      outputGobbler.schedule();
    }
  }

  /*
   * @see org.rssowl.core.INewsAction#conflictsWith(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return false;
  }
}