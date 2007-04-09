package org.rssowl.contrib.podcast.core.net;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier</a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @since 1.0
 * @version 1.1
 */

/**
 * An interface defining the completion of a download.
 */
public interface INetTaskListener
    extends java.util.EventListener {
  /**
   * Method is executed upon the completion of an network action.
   * @param event NetActionEvent
   * @see NetAction
   */
  public void netActionPerformed(NetTaskEvent event);
}
