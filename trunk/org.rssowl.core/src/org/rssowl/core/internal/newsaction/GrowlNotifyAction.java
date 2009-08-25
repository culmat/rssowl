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
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StreamGobbler;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link INewsAction} to show matching news in Growl. This
 * is only supported on Mac.
 *
 * @author bpasero
 */
public class GrowlNotifyAction implements INewsAction {
  private static final String APPLICATION_NAME = "RSSOwl";

  /**
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(List<INews> news, Object data) {

    /* Launch if file exists */
    if (data instanceof String && new File((String) data).exists()) {
      try {
        //TODO Rather aggregate N news into single growl call?
        for (INews item : news) {
          executeCommand(toGrowlnotify(item, (String) data));
        }
      }

      /* Log any error message */
      catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return Collections.emptyList();
  }

  private void executeCommand(String cmd) throws IOException {
    Process proc = Runtime.getRuntime().exec(cmd);

    /* Let StreamGobbler handle error message */
    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

    /* Let StreamGobbler handle output */
    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

    /* Flush both error and output streams */
    errorGobbler.schedule();
    outputGobbler.schedule();
  }

  private String toGrowlnotify(INews news, String pathToGrowlnotify) {
    StringBuilder cmd = new StringBuilder();

    cmd.append(pathToGrowlnotify);

    /* Application Name */
    cmd.append(" -name ").append(APPLICATION_NAME);

    /* Title */
    cmd.append(" -t ").append(CoreUtils.getHeadline(news, true));

    /* Message */
    //cmd.append(" -m ").append(); //TODO

    /* Image */
    //cmd.append(" --image ").append(); //TODO

    /* Application */
    //cmd.append(" -a ").append(APPLICATION_NAME); //TODO

    return cmd.toString();
  }

  /*
   * @see org.rssowl.core.INewsAction#conflictsWith(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return false;
  }
}