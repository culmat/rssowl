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

import java.io.File;

import org.rssowl.contrib.podcast.model.IPersonalAttachment;


/**
 * A download type, maintaining the model and status of a download.
 *
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @since 1.0
 * @version 1.1
 **/
public class Download {
	
	// Changed to Attachement Object. 
	protected IPersonalAttachment mAttachment;
	
    /**
     * The status of this download.
     */
    protected int mState;

    /**
     * The previous number of downloaded bytes.
     */
    protected int mPrevious = 0;

    /**
     * The starting offset of the download in bytes.
     */
    protected int mStart = 0;    
    //****  MEMBERS OF A GENERIC TASK, to be used in progress reporters.
    
    /**
     * The current offset of the download in bytes.
     */
    protected int mCurrent = 0;

    /**
     * The length of the download in bytes.
     */
    protected int mLength = 0;

    
    /**
     * The current message of the download object.
     * i.e. Can be used to provide progress of the 
     * download.
     */
    protected String mMessage = "";
    
    /**
     * The elapsed download time in miliseconds.
     */
    protected long timeElapsed = 0;

    /**
     * The speed in bytes per second.
     */
    protected float bytespersecond = 0;

    private int retryCounter = 0;
    
    private long mStateTime = 0;
    
    protected File mTempFile = null;
    
    public Download(IPersonalAttachment pAttachement) {
        mAttachment = pAttachement;
    }

    /**
     * Get the enclosure assiociated with this downloads.
     * 
     * @return Enclosure
     */
    public IPersonalAttachment getAttachment() {
        return mAttachment;
    }

    /**
     * Cancel the task.
     */
    public void stop() {
        mState = DownloadLogic.CANCELLED;
    }

    /**
     * Called to find out if the task has completed.
     * 
     * @return boolean
     */
    public boolean isDone() {
        return mState == DownloadLogic.COMPLETED;
    }

    /**
     * Calculate the downloadspeed. This is an average speed measured over
     * the total download time. For resumed download the offset position is
     * subtracted from the position.
     */
    public void calculateSpeed() {
        // The average speed measured over the total download time.
//        bytespersecond = ((float) mCurrent - (float) start) / (float) timeElapsed;
    }

    /**
     * @return Returns the retryCounter.
     */
    public int getRetryCounter() {
        return retryCounter;
    }

    /**
     * @param retryCounter
     *            The retryCounter to set.
     */
    public void setRetryCounter(int retryCounter) {
        this.retryCounter = retryCounter;
    }

    /**
     * @return Returns the state.
     */
    public int getState() {
        return mState;
    }
    
    public long getTimeElapsed(){
       return timeElapsed;
    }
    
    /**
     * @param pState
     *            The state to set.
     */
    public void setState(int pState) {
        mState = pState;
        mStateTime = 0; // reset the status period
    }
    
    public void setStart(int pStart){
        mStart = pStart;
    }
    
    
    public void incrementStateTime(long lTime){
        mStateTime += lTime;
    }
    
    public long getStateTime(){
        return mStateTime;
    }

    public float getBytesPerSecond() {
        return bytespersecond;
    }
    
    /**
     * Get the temporary storage file.
     * @return
     */
    public File getTempFile(){
        return mTempFile;
    }

	public int getCurrent() {
		return mCurrent;
	}

	public void setCurrent(int current) {
		mCurrent = current;
	}

	public int getLength() {
		return mLength;
	}

	public void setLength(int length) {
		mLength = length;
	}

	public String getMessage() {
		return mMessage;
	}

	public void setMessage(String message) {
		mMessage = message;
	}
}