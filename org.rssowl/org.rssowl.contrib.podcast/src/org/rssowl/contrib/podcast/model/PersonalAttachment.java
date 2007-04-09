package org.rssowl.contrib.podcast.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.rssowl.contrib.podcast.content.Content;
import org.rssowl.contrib.podcast.content.ContentException;
import org.rssowl.contrib.podcast.core.download.DownloadException;
import org.rssowl.contrib.podcast.core.download.DownloadUtil;
import org.rssowl.core.model.internal.types.Attachment;

public class PersonalAttachment extends Attachment implements
		IPersonalAttachment {

	// CB TODO, We can store the status in a single byte. (8 bits).
	private int mStatus = 0x00;

	static final byte MARKED = 0x01;

	static final byte LOCAL = 0x02;

	static final byte PLAYER = 0x04;

	static final byte CACHED = 0x08;

	static final byte TORRENT = 0x10;

	static final byte INSPECTED = 0x20;

	static final byte COMPLETED = 0x40;

	static final byte CANDIDATE = (byte) 0x80;

	private String mFileName;

	private File mTorrentFile;

	private long mContentDate;
	private int mContentSize;

	public boolean isMarked() {
		return ((mStatus & MARKED) == 0) ? false : true;
	}

	public void setMarked(boolean pMarked) {
		mStatus = (pMarked) ? mStatus | MARKED : mStatus & ~MARKED;
	}

	public boolean isCached() {
		return ((mStatus & CACHED) == 0) ? false : true;
	}

	public void setCached(boolean pCached) {
		mStatus = (pCached) ? mStatus | CACHED : mStatus & ~CACHED;
	}

	public boolean getInPlayer() {
		return ((mStatus & PLAYER) == 0) ? false : true;
	}

	public void setInPlayer(boolean pPlayer) {
		mStatus = (pPlayer) ? mStatus | PLAYER : mStatus & ~PLAYER;
	}

	public boolean isLocal() {
		return ((mStatus & LOCAL) == 0) ? false : true;
	}

	public void setLocal(boolean pLocal) {
		mStatus = (pLocal) ? mStatus | LOCAL : mStatus & ~LOCAL;
	}

	public void setCandidate(boolean pCandidate) {
		mStatus = (pCandidate) ? mStatus | CANDIDATE : mStatus & ~CANDIDATE;
	}

	public boolean isCandidate() {
		return ((mStatus & CANDIDATE) == 0) ? false : true;
	}

	/**
	 * Set that the enclosure is now inspected.
	 */
	public void setInspected() {
		if (isInspected()) {
			throw new IllegalArgumentException();
		} else {
			mStatus |= INSPECTED;
		}
	}

	public boolean isInspected() {
		return ((mStatus & INSPECTED) == 0) ? false : true;
	}

	public void setDownloadCompleted(boolean pCompleted) {
		mStatus = (pCompleted) ? mStatus | COMPLETED : mStatus & ~COMPLETED;
	}

	public boolean isDownloadCompleted() {
		return ((mStatus & COMPLETED) == 0) ? false : true;
	}

	/**
	 * Get if this enclosure is a torrent file.
	 * 
	 * @return boolean
	 */
	public boolean isTorrent() {
		return ((mStatus & TORRENT) == 0) ? false : true;
	}

	/**
	 * Set this enclosure as torrentFile.
	 */
	public void setTorrent() {
		mStatus |= TORRENT;
	}

	public Date getDate() {
		return new Date(getContentDate());
	}

	public int getFileLength() {
		return mContentSize;
	}

	public File getFile() {
		return getFile(false);
	}

	/**
	 * Overrides the XFile method.
	 */
	public File getFile(boolean pRefresh) {
		String mFile = mFileName;
		// String lFolder = getFeed().getFolder();
		if (mFile == null || mFile.length() == 0 || mFile.equals("null")
				|| pRefresh) {

			mFile = getName();
			if (!mFileName.equals(mFile)) {
				mFileName = mFile;
			}
			if (mFile == null) {
				return null;
			}
		}
		return new File(mFile);
		// CB TODO, The Feed folder is not resolved yet.
		// return DownloadUtil.getLocalEnclosureFile(mFile, lFolder);
	}

	/**
	 * Return the URL decoded file name or RSS decoded (From news Item title).
	 * 
	 * @return String
	 */
	public String getName() {

		boolean lUrlProblem = false;
		try {
			URL lUrl = getLink().toURL();
			return DownloadUtil.getUrlFileName(lUrl);
		} catch (DownloadException e) {
			lUrlProblem = true;
		} catch (MalformedURLException e) {
		}

		if (lUrlProblem) { // get a file name from the content.
			Content lContent = null;
			try {
				lContent = new Content(getType());
				String lTitle = getNews().getTitle();
				lTitle = DownloadUtil.makeFSName(lTitle);
				if (lContent != null) {
					lTitle += "." + lContent.getExtension();
				}
				return lTitle;
			} catch (ContentException e1) {
				// invalid content. no name returned.
			}
		}
		return new Long(System.currentTimeMillis()).toString() + ".tmp";
	}

	public void setFile(File pFile) {
		// Should not be called, satisfy interface only.
		throw new IllegalArgumentException();
	}

	public void setFileName(String pFile) {
		mFileName = pFile;
	}

	public String getFileName() {
		return mFileName;
	}

	/**
	 * Get the file to which the torrent enclosure will be/has been downloaded.
	 * 
	 * @param torrentFile
	 *            File
	 */
	public void setTorrentFile(File pTorrentFile) {
		mTorrentFile = pTorrentFile;
	}

	/**
	 * Get the file to which the torrent enclosure will be/has been downloaded.
	 * 
	 * @return File the bitTorrent file.
	 */
	public File getTorrentFile() {
		return mTorrentFile;
	}

	/**
	 * Return if the file is incomplete. This method performs several checks on
	 * the file. It compares file length on the local storage with the length in
	 * the HTTP Header. An exact match of the size in bytes is needed. It also
	 * compares the file length with the length as provided in the RSS tag.
	 * 
	 * @throws Exception
	 * @return boolean
	 */
	public boolean isIncomplete() {

		if (getFile() != null) {

			long lFileLength = getFile().length();
			if (lFileLength == 0 || isDownloadCompleted()) {
				return false;
			}
			// We perform an additional check as the downloadCompleted flag
			// was implemented in a later release. Previous stored enclosures
			// will not have this flag set.

			// CB FIXME  Unfortunatly the HEAD information is not always
			// correct for certain feed. The difference is not "visible"
			// when rounding off with the size formatters.
			// We could set an incomplete flag, when we start the download
			// and then clear the flag wehn the download completes.
			// The incomplete flag, would need to be stored persistenly, in the
			// configuration.

			long lContentSize = getContentSize();

			// mLog.info( lFileLength + "=" + lHEADLength);
			if (getFile().length() != 0 && lContentSize != 0
					&& getFile().length() >= lContentSize) {
				return false;
				// Note that the file could be bigger (or smaller) than
				// indicated by
				// the server. ID3 rewriting could add some bytes like new
				// tags or padding to the file
			}

			// Another check using the RSS length of the file.
			long lRSSLength = getLength();
			if (getFile().length() != 0 && getFile().length() >= lRSSLength) {
				return false;
			}

			// we have to assume this file is not complete.
			return true;

		} else {
			return false;
		}
	}

	public int getContentSize() {
		return mContentSize;
	}

	public void setContentSize(int pSize) {
		mContentSize = pSize;
	}

	public void setContentDate(long pDate) {
		mContentDate = pDate;
	}

	public long getContentDate() {
		return mContentDate;
	}

}
