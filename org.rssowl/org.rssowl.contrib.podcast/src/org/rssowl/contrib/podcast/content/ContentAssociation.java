package org.rssowl.contrib.podcast.content;

import org.eclipse.swt.program.Program;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @version 1.1
 */
public class ContentAssociation {

	protected String mExecString;

	protected IContent mMIMEType;

	protected String mExtension;

	public ContentAssociation(String pMIMEType, String pExecString,
			String pExtension) {
		mExecString = pExecString;
		try {
			mMIMEType = new Content(pMIMEType);
		} catch (ContentException ce) {

		}
		mExtension = pExtension;
	}

	/**
	 * @return Returns the mExecString.
	 */
	public String getExecString() {
		return mExecString;
	}

	/**
	 * @param execString
	 *            The mExecString to set.
	 */
	public void setExecString(String execString) {
		mExecString = execString;
	}

	/**
	 * @return Returns the mExtension.
	 */
	public String getExtension() {
		return mExtension;
	}

	/**
	 * @param extension
	 *            The mExtension to set.
	 */
	public void setExtension(String extension) {
		mExtension = extension;
	}

	/**
	 * @return Returns the mMIMEType.
	 */
	public IContent getMIMEType() {
		return mMIMEType;
	}

	/**
	 * @param type
	 *            The mMIMEType to set.
	 */
	public void setMIMEType(IContent type) {
		mMIMEType = type;
	}

	/**
	 * @param pExtension
	 * @return
	 */
	public static Program getOSAssociation(String pExtension) {
		Program p = Program.findProgram(pExtension);
		return p;
	}

	/**
	 * @param pExtension
	 * @return
	 */
	public static void openProgram(String pExtension, String pFile) {
		Program p = getOSAssociation(pExtension);
		if (p != null) {
			p.execute(pFile);
		}
	}
}