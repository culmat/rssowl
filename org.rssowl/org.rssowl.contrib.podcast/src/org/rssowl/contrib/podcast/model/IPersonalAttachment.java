package org.rssowl.contrib.podcast.model;

import java.io.File;

import org.rssowl.core.model.types.IAttachment;

public interface IPersonalAttachment extends IAttachment {

	// ****************** HTTP HEAD Information.
	public abstract int getContentSize();

	public abstract void setContentSize(int pSize);

	public abstract void setContentDate(long pDate);

	public abstract long getContentDate();

	// ******************* STATUS Information.
	public abstract boolean isMarked();

	public abstract void setMarked(boolean pMarked);

	public abstract boolean isCached();

	public abstract void setCached(boolean pCached);

	public abstract boolean getInPlayer();

	public abstract void setInPlayer(boolean pPlayer);

	public abstract boolean isLocal();

	public abstract void setLocal(boolean pLocal);

	public abstract void setCandidate(boolean pCandidate);

	public abstract boolean isCandidate();

	public abstract void setInspected();

	public abstract boolean isInspected();

	// ********** File name information.
	public abstract String getFileName();

	public abstract File getFile();

	public abstract File getFile(boolean pRefresh);

	public abstract void setFileName(String pName)
			throws IllegalAccessException;

	public abstract void setDownloadCompleted(boolean pCompleted);

	public abstract boolean isDownloadCompleted();

	public abstract boolean isIncomplete();

}