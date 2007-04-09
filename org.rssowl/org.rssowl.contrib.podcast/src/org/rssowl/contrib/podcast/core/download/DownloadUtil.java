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

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier</a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @since 1.0
 * @version 1.1
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Logger;

/**
 * This handler contains several functions: 
 * <ul>
 * <li>Resolves the location folder of the application</li>
 * <li>Contains a data handling mechanism for loading and saving data</li>
 * <li>File handling functions
 * 	<table >
 * 		<tr><td>Create a valid folder name from a Feed title<td></tr>
 * 		<tr><td>Create a file name from a URL<td></tr>
 * 		<tr><td>Maintain the temporary storage of an RSS file.<td></tr>
 * 		<tr><td>Maintain the main podcast storage folder. (Ask user when not known)<td></tr>
 * 		<tr><td>Copying from one file to another.<td></tr>
 *		<tr><td>Deleting a folder.<td></tr>
 * </table>
 * </li>
 * </ul>
 */
public class DownloadUtil {

	private static final int FILE_NOT_FOUND = -1;

	/**
	 * The podcast folder.
	 */
	private static String sPodcastFolderName;

	public static File sID3AllFile;

	public static File sApplicationDirectory;

	public static File sBinDirectory;

	public static File sImageDirectory;

	public static File sDocDirectory;

	public static File sLibDirectory;

	public static File sPluginDirectory;

	public static File sDefaultDirectory;


	static Logger sLog = Logger.getLogger(DownloadUtil.class.getName());


	private static DownloadUtil sSelf;


	public static DownloadUtil getInstance() {
		if (sSelf == null) {
			sSelf = new DownloadUtil();
		}
		return sSelf;
	}

	public boolean initialize(String pUserFolder) {
		// CB TODO, adapt log.
//		sLog.info("initialize(), current CL: "
//				+ DownloadUtil.class.getClassLoader());

		return true; // Initialization successful.
	}

	/**
	 * Read a file content
	 * 
	 * @param src
	 * @param dest
	 * @return boolean <code>true</code> if copying was successfull.
	 * @throws Exception
	 */
	public static String readFileContent(File pFile) throws DownloadException {
		StringWriter lOutput = new StringWriter();
		FileReader lInput = null;
		try {
			lInput = new FileReader(pFile);
			int lLength = 0;
			char[] lBuffer = new char[1024];
			while ((lLength = lInput.read(lBuffer)) > 0) {
				lOutput.write(lBuffer, 0, lLength);
			}
		} catch (FileNotFoundException fnfe) {
			throw new DownloadException(FILE_NOT_FOUND, fnfe);
		} catch (IOException ioe) {
			throw new DownloadException(ioe.getMessage(), ioe);
		} finally {
			if (lInput != null) {
				try {
					lInput.close();
				} catch (Exception e) {
				}
			}
		}
		return lOutput.toString();
	}

	/**
	 * Copy a file to another File object.
	 * 
	 * @param src
	 * @param dest
	 * @return boolean <code>true</code> if copying was successfull.
	 * @throws Exception
	 */
	public static boolean copyFile(File src, File dest) throws DownloadException {
		try {
			FileInputStream r = new FileInputStream(src);
			FileOutputStream w = new FileOutputStream(dest);
			int read;
			byte[] buffer = new byte[8192];
			while ((read = r.read(buffer)) > 0) {
				w.write(buffer, 0, read);
			}
			return true;
		} catch (IOException e) {
			throw new DownloadException(e.getMessage(), e);
		}
	}

	/**
	 * Make the name (playlist) compatible with the file system. (Remove
	 * forbidden characters).
	 * 
	 * @param name
	 *            String
	 * @return String
	 */
	public static String makeFSName(String name) {
		String subdir = name;
		subdir = subdir.replace('\\', ' ');
		subdir = subdir.replace('*', ' ');
		subdir = subdir.replace(':', ' ');
		subdir = subdir.replace(';', ' ');
		subdir = subdir.replace('?', ' ');
		subdir = subdir.replace('\"', ' ');
		subdir = subdir.replace('<', ' ');
		subdir = subdir.replace('>', ' ');
		subdir = subdir.replace('|', ' ');
		subdir = subdir.replace('\\', ' ');
		subdir = subdir.replace('/', ' ');
		// subdir = subdir.replace('.', ' ');
		// subdir = subdir.replace('\'', ' ');
		// subdir = subdir.replace(',', ' ');
		subdir = subdir.trim();
		// if( subdir.endsWith(".")){
		// subdir = subdir.substring(0,subdir.length()-1);
		// }
		return subdir;
	}

	
// CB TODO How to handle preference? 	
	/**
	 * We are interrested in the download folder as the FileHandler is holding
	 * the current folder to generate feed folders or file paths. A non valid
	 * folder or unknow folder, will result in the user being asked to specify a
	 * folder. <p/> The folder recommendation from this method is to use the
	 * Java System propery [user.name] for non-Windows systems. For Windows
	 * systems we add: \My Doucments\My Received Podcasts <p/> It is not
	 * possible to escape the folder selection. User intervention is needed. A
	 * selection dialog is presented showing the default folder and a browse
	 * button to select another folder.
	 * 
	 * @see com.jpodder.data.configuration.IConfigurationListener#configurationChanged(com.jpodder.data.configuration.ConfigurationEvent)
	 */
//	public void configurationChanged(ConfigurationEvent event) {
//
//		if (event.getSource() instanceof DownloadUtil) {
//			return;
//		}
//
//		String lName = event.getPropertyName();
//		if (lName.length() > 0 && !lName.equals(Configuration.CONFIG_FOLDER)) {
//			return;
//		}
//
//		String lFolder = Configuration.getInstance().getFolder();
//		boolean lDone = false;
//		while (!lDone) {
//			if (lFolder == null || lFolder.length() == 0
//					|| (lFolder != null && !new File(lFolder).exists())) {
//				String lDefaultFolder;
//				if (OS.isWindows()) {
//					lDefaultFolder = System.getProperty("user.home")
//							+ File.separator + "My Documents" + File.separator
//							+ "My Received PodCasts";
//				} else {
//					lDefaultFolder = System.getProperty("user.home")
//							+ File.separator + "My Received PodCasts";
//				}
//				if (!new File(lDefaultFolder).exists()) {
//					new File(lDefaultFolder).mkdir();
//				}
//
//				DirectoryDialog dialog = new DirectoryDialog(UILauncher
//						.getInstance().getShell(), SWT.OPEN);
//				dialog.setFilterPath(lDefaultFolder);
//				dialog
//						.setText(Messages
//								.getString("filehandler.dialog.configuation.folder.title"));
//				String lFileName;
//				if ((lFileName = dialog.open()) != null) {
//					lFolder = lFileName;
//					lDone = true;
//				}
//			} else {
//				lDone = true;
//			}
//		}
//		
//		if(sPodcastFolderName == null || !sPodcastFolderName.equals(lFolder)){
//			sPodcastFolderName = lFolder;
//			Configuration.getInstance().setFolder(lFolder);
//			
//			ConfigurationLogic.getInstance().fireConfigurationChanged(
//					new ConfigurationEvent(this, Configuration.CONFIG_FOLDER));			
//		}
//	}

	/**
	 * Get the podcast folder from the properties.
	 * 
	 * @return String
	 */
	public static String getPodcastFolder() {
		return sPodcastFolderName;
	}

	/**
	 * Get the feed folder based on the feed Title..
	 * 
	 * @param feedTitle
	 *            String The feed title, used as the sub-folder name.
	 * @return String
	 */
	public static String getFeedFolder(String feedTitle) {
		String subFolder = makeFSName(feedTitle);
		String feedFolder = sPodcastFolderName + File.separator + subFolder;
		return feedFolder;
	}

	/**
	 * Create the enclosure File object.
	 * 
	 * @param url
	 *            URL
	 * @param pFolder
	 *            String
	 * @param pCreateOnDisk
	 *            Create the file on the storage as well.
	 * @return File
	 * @throws Exception
	 */
	public static File getLocalEnclosureFile(String pFileName, String pFolder) {
		String trackPath = pFolder + File.separator + pFileName;
		File lTrackFile = new File(trackPath);
		return lTrackFile;
	}

	public static File createEnclosureFile(String pFileName, String pFolder,
			boolean pCreateOnDisk) throws DownloadException {
		String trackPath = pFolder + File.separator + pFileName;
		File trackFile = new File(trackPath);
		if (pCreateOnDisk) {
			if (!trackFile.exists()) {
				try {
					if (!trackFile.createNewFile()) {
						throw new DownloadException(trackFile.getName());
					}
				} catch (IOException e) {
					throw new DownloadException(e.getMessage());
				}
			}
		}
		return trackFile;
	}

	/**
	 * Get the file name from a URL. This is under some conditions. 
	 * </br><b>1)</b>
	 * The file could be a path, so we take the last segment of the path.
	 * </br><b>2)</b>
	 * If the URL contains a query, than the URL is useless and we throw an
	 * exception. 
	 * @param pUrl
	 * @return A valid string, which can be used to create a local file.
	 */
	public static String getUrlFileName(URL pUrl) throws DownloadException {
		if (pUrl == null) {
			return "";
		}
		String lFileName = pUrl.getFile();

		int lSlashIndex = lFileName.lastIndexOf("/") + 1;
		lFileName = lFileName.substring(lSlashIndex);

		// int lEqualIndex = lFileName.indexOf("=");
		// lFileName = lFileName.substring(Math.max(lSlashIndex, lEqualIndex) +
		// 1,
		// lFileName.length());

		String lQueryName = pUrl.getQuery();
		if (lQueryName != null) {
			// The URL contains a query, and we can impossibly create a filename
			// from
			// this. We therefor fire an exception on this.
			throw new DownloadException(
					"File from URL path is not possible (Query included)");
		}

		try {
			lFileName = URLDecoder.decode(lFileName, "UTF-8");
		} catch (java.io.UnsupportedEncodingException uue) {
			// Will not happen UTF-8 is supported.
		}
		return lFileName;
	}

	public static File saveTempFeedFile(InputStream pStream) {
		// Save a byte stream instead.

		BufferedInputStream lInputStream = new BufferedInputStream(pStream);
		BufferedOutputStream lOutputStream = null;
		File tempFile = null;
		try {
			tempFile = File.createTempFile("jpodder", ".xml");
			lOutputStream = new BufferedOutputStream(new FileOutputStream(
					tempFile));
		} catch (IOException e1) {
		}
		int read;
		byte[] buffer = new byte[8192];
		try {
			while ((read = lInputStream.read(buffer)) > 0) {
				lOutputStream.write(buffer, 0, read);
			}
			pStream.close();
			lInputStream.close();
			lOutputStream.close();
		} catch (IOException e) {
		}
		return tempFile;
	}

	public static File saveTempFeedFile(Reader reader) {
		// Empty the stream (Write it to a temporary file).

		BufferedReader r = new BufferedReader(reader);
		BufferedWriter w = null;
		File tempFile = null;
		try {
			tempFile = File.createTempFile("jpodder", ".xml");
			w = new BufferedWriter(new FileWriter(tempFile));
		} catch (IOException e1) {
		}

		String line;
		boolean lFirstLine = true;
		try {
			while ((line = r.readLine()) != null) {
				if (lFirstLine) {
					int i = 0;
					for (; i < line.length(); i++) {
						char lChar = line.toCharArray()[i];
						Character lCharacter = new Character(lChar);
						if (lCharacter.equals(new Character('<'))) {
							break;
						}
					}
					w.write(line.substring(i));
					w.newLine();
					lFirstLine = false;
				} else {
					w.write(line);
					w.newLine();
				}
			}
		} catch (IOException e) {
			// Error reading/writing the feed to disk.
		}

		try {
			reader.close();
			r.close();
			w.close();
		} catch (IOException e2) {
		}

		return tempFile;
	}

	// CB TODO Migrate, would need to access the model.
//	public static void removeUnusedFeedFiles(XPersonalFeedList pFeedList) {
//		String lTempDir = System.getProperty("java.io.tmpdir");
//		File lTempDirFile = new File(lTempDir);
//		if (lTempDirFile.exists()) {
//			sLog.info("Cleaning temp files from: " + lTempDir);
//			File[] lTempFiles = lTempDirFile.listFiles(new FileFilter() {
//				public boolean accept(File pathname) {
//					if (pathname.getName().startsWith("jpodder")) {
//						return true;
//					} else {
//						return false;
//					}
//				}
//			});
//
//			for (int j = 0; j < lTempFiles.length; j++) {
//				boolean lMatch = false;
//				Iterator lIt = pFeedList.getFeedIterator();
//				for (; lIt.hasNext();) {
//					File lFile = ((IXPersonalFeed) lIt.next()).getFile();
//					if (lFile != null) {
//						String lRSSFileName = lFile.getName();
//						if (lTempFiles[j].getName().equals(lRSSFileName)) {
//							lMatch = true;
//							break;
//						}
//					}
//				}
//				if (!lMatch) {
//					sLog.info("Clean: " + lTempFiles[j].getName());
//					lTempFiles[j].deleteOnExit();
//				}
//			}
//		}
//	}
	
	
	
	
	
	/**
	 * Create a subfolder in the podcast folder. If the folder exists, the
	 * method returns. If not it will be created. If it can't be created,
	 * podcasts will be created in the default podcastfolder.
	 * <p>
	 * 
	 * @param feedFolder
	 *            String
	 * @throws Exception
	 */
	public static void createFeedFolder(String feedFolder)
			throws DownloadException {

		File feedFolderFile = new File(feedFolder);
		if (!feedFolderFile.exists()) {
			if (feedFolderFile.mkdir()) {
//				sLog.info("...folder created: " + feedFolder);
			} else {
//				sLog.error("...can not create: " + feedFolder);
				throw new DownloadException("...can not create: " + feedFolder);
			}
		}
	}


	public static void deleteDirectory(File pDirectory) {
		if (pDirectory != null && pDirectory.exists()
				&& pDirectory.isDirectory()) {
			File[] lFiles = pDirectory.listFiles();
			for (int i = 0; i < lFiles.length; i++) {
				if (lFiles[i].isDirectory()) {
					deleteDirectory(lFiles[i]);
				}
				lFiles[i].delete();
			}
		}
	}

}