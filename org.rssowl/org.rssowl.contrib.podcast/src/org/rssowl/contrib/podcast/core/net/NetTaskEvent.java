package org.rssowl.contrib.podcast.core.net;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier</a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @version 1.0
 */

/**
 * A download complete event. In a multithreading downloading system, external
 * listeners need to be notified of download completions.
 */
public class NetTaskEvent extends java.util.EventObject {

    /**
	 * 
	 */
	private static final long serialVersionUID = -5398619032847277466L;

	public static final short DOWNLOAD_SUCCESS = 300;

    public static final short DOWNLOAD_FAILED = 301;
    
    public static final short DOWNLOAD_STATUS_CHANGED = 302;
    
    public static final short HEAD_SUCCESS = 303;

    public static final short HEAD_FAILED = 304;

    private short mNetEvent;

    private Exception exception;

    /**
     * Constructor without exception argument.
     * @param source
     * @param pNetEvent
     */
    public NetTaskEvent(Object source, short pNetEvent) {
        this(source, pNetEvent, null);
    }

    public NetTaskEvent(Object source, short pNetEvent, Exception e) {
        super(source);
        this.mNetEvent = pNetEvent;
        this.exception = e;
    }

    /**
     * Return the result of the download completion. This can be a
     * DOWNLOAD_SUCCESS or DOWNLOAD_FAILED.

     * @return short
     */
    public short getNetEvent() {
        return mNetEvent;
    }

    /**
     * Return the failure exception. This value can be <code>null</code>
     * 
     * @return Returns the exception.
     */
    public Exception getException() {
        return exception;
    }
}