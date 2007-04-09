package org.rssowl.contrib.podcast.core.net;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * version 1.0
 */
import java.text.ParseException;
import java.util.Date;

import org.rssowl.contrib.podcast.util.Util;

/**
 * HTTP HEAD Information class. This is a data holder for simple HTTP Connection
 * mime/type.
 */
public class NetHEADInfo {

//	private static Logger sLog = Logger.getLogger(NetHEADInfo.class.getName());

	public int length;

	public String contentType;

	public String encoding;

	private long mModified = -1;

	private String mDateString = "";

	private String mExpiredString;

	private String mModifiedString = "";

	/**
	 * Empty Constructor. An instance of this class can be constructed with
	 * empty class variables, which can be set at later stage.
	 */
	public NetHEADInfo() {
	}

	/**
	 * Constructor.
	 * 
	 * @param length
	 *            int
	 * @param contentType
	 *            String
	 * @param encoding
	 *            String
	 * @param date
	 *            long
	 * @param expired
	 *            long
	 * @param modified
	 *            long
	 */
	public NetHEADInfo(int length, String contentType, String encoding,
			long date, long expired, long modified) {
		this.length = length;
		this.contentType = contentType;
		this.encoding = encoding;
		// mDate = date;
		// mExpired = expired;
		// mModified = modified;
	}

	/**
	 * An attempt is made to parse the RFC 822 strings, to retrieve a UTC value
	 * for the time/date.
	 * 
	 * @param pLength
	 * @param pContentType
	 * @param pEncoding
	 * @param dateString
	 * @param expiredString
	 * @param modifiedString
	 */
	public NetHEADInfo(int pLength, String pContentType, String pEncoding,
			String dateString, String expiredString, String modifiedString) {
		length = pLength;
		contentType = pContentType;
		encoding = pEncoding;
		mDateString = dateString;
		mExpiredString = expiredString;
		mModifiedString = modifiedString;
		if (mModifiedString == null || mModifiedString.length() == 0
				&& (mDateString != null && mDateString.length() > 0)) {
			mModifiedString = mDateString;
		}
		try {
			Date lDate = Util.resolvedDateRFC822(mModifiedString);
			if (lDate != null) {
				mModified = lDate.getTime();
			}
		} catch (ParseException e) {
//			sLog.warn("HTTP HEAD Information: " + e.getMessage());
		} catch (IllegalArgumentException iae) {
//			sLog.warn("HTTP HEAD Information: RFC822 date missing");
		}
	}
	
	/**
	 * Return the size in bytes.
	 * 
	 * @return String
	 */
	public String getSize() {
		return new Integer(length).toString();
	}

	public long getModifiedLong() {
		return mModified;
	}

	public String getModifiedString() {
		return mModifiedString;
	}

	public String getDateString() {
		return mDateString;
	}

	public String getExpiredString() {
		return mExpiredString;
	}
}