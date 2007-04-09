package org.rssowl.contrib.podcast.core.download;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer </a>
 * @version 1.0
 */
public class DownloadEvent extends java.util.EventObject {
    /**
	 * 
	 */
	private static final long serialVersionUID = 6958212344345131993L;

	public DownloadEvent(Object pSource){
        super(pSource);
    }
}
