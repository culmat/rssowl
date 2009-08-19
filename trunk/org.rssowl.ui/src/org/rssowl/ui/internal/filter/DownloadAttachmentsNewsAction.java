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

package org.rssowl.ui.internal.filter;

import org.rssowl.core.INewsAction;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link INewsAction} to download all attachments of the
 * news.
 *
 * @author bpasero
 */
public class DownloadAttachmentsNewsAction implements INewsAction {

  /** Unique ID of this Action */
  public static final String ID = "org.rssowl.ui.DownloadAttachmentsNewsAction";

  /*
   * @see org.rssowl.core.INewsAction#run(java.util.List, java.lang.Object)
   */
  public List<IEntity> run(List<INews> news, Object data) {
    if (data != null && data instanceof String) {
      File folder = new File((String) data);
      if (folder.exists()) {
        for (INews newsitem : news) {
          List<IAttachment> attachments = newsitem.getAttachments();
          for (IAttachment attachment : attachments) {
            URI link = attachment.getLink();
            if (link != null) {
              if (!link.isAbsolute()) {
                try {
                  link = URIUtils.resolve(newsitem.getFeedReference().getLink(), link);
                } catch (URISyntaxException e) {
                  Activator.getDefault().logError(e.getMessage(), e);
                  continue; //Proceed with other Attachments
                }
              }
            }

            if (link != null)
              Controller.getDefault().getDownloadService().download(attachment, link, folder, false);
          }
        }
      }
    }

    return Collections.emptyList();
  }

  /*
   * @see org.rssowl.core.INewsAction#conflictsWith(org.rssowl.core.INewsAction)
   */
  public boolean conflictsWith(INewsAction otherAction) {
    return otherAction instanceof DownloadAttachmentsNewsAction;
  }
}