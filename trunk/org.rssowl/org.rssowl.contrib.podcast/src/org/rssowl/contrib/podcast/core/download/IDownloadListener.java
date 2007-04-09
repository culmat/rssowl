package org.rssowl.contrib.podcast.core.download;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer </a>
 * @since 1.0
 * @version 1.1
 */
public interface IDownloadListener {
	public void modelChanged(DownloadEvent pEvent);

}
