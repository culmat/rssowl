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

package org.rssowl.contrib.podcast.core.net;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @since 1.0
 * @version 1.1
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.rssowl.contrib.podcast.core.download.Download;
import org.rssowl.contrib.podcast.core.download.DownloadLogic;
import org.rssowl.contrib.podcast.model.IPersonalAttachment;
import org.rssowl.core.model.types.IFeed;

// import org.apache.commons.net.ftp.FTPClient;
// import org.apache.commons.net.ftp.FTPReply;
// import org.apache.log4j.Logger;

/**
 * Class is holder for various static net related tasks. It also holds a
 * download a runnable download class which fire events on success or error.
 * <p>
 * An instance of a download task can be retrieved with
 * <code>getDownloadTask()</code>
 * <p>
 * Add a listner to this class, if notification of downloads is needed.
 * 
 */
public class NetTask implements RejectedExecutionHandler {

	// private static Logger sLog = Logger.getLogger(NetTask.class.getName());

	private static NetTask mSelf;

	private final ArrayList<INetTaskListener> mListenerList = new ArrayList<INetTaskListener>();

	private static ExecutorService mExecService = null;

	public NetTask() {
		// CB TODO Migrate thread pool usage to eclipse jobs.

		// System.getProperties().setProperty("httpclient.useragent",
		// "mozilla/4.0");
		// System.getProperties().setProperty("httpclient.useragent",
		// Main.APP_TITLE + " " + Main.APP_RELEASE);

		// Uncomment to show the HTTP wire log.
		// NetDebug.initialize();
		// initTreadPool();
	}

	public void initTreadPool() {

		mExecService = Executors.newFixedThreadPool(5);

		if (mExecService instanceof ThreadPoolExecutor) {
			ThreadPoolExecutor lExec = (ThreadPoolExecutor) mExecService;
			// lExec.setCorePoolSize(5);
			// lExec.setMaximumPoolSize(5);
			// lExec.setKeepAliveTime(5, TimeUnit.SECONDS);
			lExec.setRejectedExecutionHandler(this);
			BlockingQueue lQueue = lExec.getQueue();

			long lTime = lExec
					.getKeepAliveTime(java.util.concurrent.TimeUnit.MILLISECONDS);

			// sLog.info("Download threads will timeout when idle in: " + lTime
			// + " mili sec.");
		}
	}

	public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
		// sLog.error("Execution rejected for:" + arg0);
	}

	public static NetTask getInstance() {
		if (mSelf == null) {
			mSelf = new NetTask();
		}
		return mSelf;
	}

	/**
	 * Add a Listener to the list.
	 * <p>
	 * 
	 * @param pListener
	 *            NetActionListener. The listener to be added.
	 */
	public final void addNetActionListener(INetTaskListener pListener) {
		if (pListener != null && !mListenerList.contains(pListener)) {
			mListenerList.add(pListener);
		}
	}

	/**
	 * Remove a listener from the list.
	 * <p>
	 * 
	 * @param pListener
	 *            The listener to be removed.
	 */
	public final void removeNetActionListener(INetTaskListener pListener) {
		if (pListener != null && mListenerList.contains(pListener)) {
			mListenerList.remove(pListener);
		}
	}

	/**
	 * Fire a download complete event.
	 * 
	 * @param event
	 *            DownloadCompleteEvent
	 */
	public void fireNetActionPerformed(NetTaskEvent event) {
		Iterator<INetTaskListener> it = mListenerList.iterator();
		while (it.hasNext()) {
			INetTaskListener listener = it.next();
			listener.netActionPerformed(event);
		}
	}

	/**
	 * Download from an URL to a file.
	 * <p>
	 * 
	 * @param pDownload
	 *            Downloads.Download
	 * @throws Exception
	 */
	public static void download(Download pDownload) throws Exception {
		NetDownloadTask lTask = getDownloadTask(pDownload);
		mExecService.execute(lTask);
	}

	public static NetDownloadTask getDownloadTask(Download pDownload) {
		if (mSelf == null) {
			getInstance();
		}
		return mSelf.new NetDownloadTask(pDownload);
	}

	/**
	 * Get a reader. The connection is left open and should be closed after
	 * having processed the response.
	 * 
	 * @param feed
	 * @param url
	 * @param head
	 *            NetHEADInfo
	 * @throws Exception
	 * @return Reader
	 */
	public static NetHEADInfo getHeadInfoWithGet(IFeed feed, URL url)
			throws NetworkException {

		HttpClient hClient = new HttpClient();
		HostConfiguration hc = hClient.getHostConfiguration();
		hc = setProxySetttings(hClient, hc);

		GetMethod gMethod = new GetMethod(url.toString());
		gMethod.setRequestHeader("cache-control", "no-cache");
		try {
			int status = hClient.executeMethod(gMethod);
			if (status != HttpStatus.SC_OK) {
				// Check for redirection.
				if (status == HttpStatus.SC_MOVED_PERMANENTLY
						|| status == HttpStatus.SC_MOVED_TEMPORARILY
						|| status == HttpStatus.SC_SEE_OTHER
						|| status == HttpStatus.SC_TEMPORARY_REDIRECT) {
					String redirectLocation;
					Header locationHeader = gMethod
							.getResponseHeader("location");
					if (locationHeader != null) {
						redirectLocation = locationHeader.getValue();
						gMethod = new GetMethod(redirectLocation);
						status = hClient.executeMethod(gMethod);
						if (status != HttpStatus.SC_OK) {
							throw new NetworkException(gMethod.getStatusLine()
									.getReasonPhrase());
						}
					} else {
						// The response is invalid and did not provide the
						// new
						// location for
						// the resource. Report an error or possibly handle
						// the
						// response
						// like a 404 Not Found error.
					}
				} else {
					if (status == HttpStatus.SC_UNAUTHORIZED) {
						// Retry with password.
						hc = hClient.getHostConfiguration();
						hc = setProxySetttings(hClient, hc);

						hClient.getState()
								.setCredentials(
										new AuthScope(AuthScope.ANY_HOST,
												AuthScope.ANY_PORT,
												AuthScope.ANY_REALM),
										getCredentials(feed));

						gMethod = new GetMethod(url.toString());
						gMethod.setDoAuthentication(true);
						status = hClient.executeMethod(gMethod);
						if (status != HttpStatus.SC_OK) {
							throw new NetworkException(gMethod.getStatusLine()
									.getReasonPhrase());
						}
					} else {
						throw new NetworkException(gMethod.getStatusLine()
								.getReasonPhrase());
					}
				}
			}

			return getHeadInfo(gMethod);
		} catch (HttpException he) {
			throw new NetworkException(he.getMessage());
		} catch (IOException ioe) {
			throw new NetworkException(ioe.getMessage());
		}
	}

	public static Reader getReader(URL url) throws NetworkException {
		return getReader(null, url, null);
	}

	public static Reader getReader(IFeed pFeed, URL url, NetHEADInfo head)
			throws NetworkException {
		InputStream lStream = getStream(pFeed, url, head);
		InputStreamReader reader = new InputStreamReader(lStream);
		return reader;
	}

	public static InputStream getStream(IFeed pFeed, URL url, NetHEADInfo head)
			throws NetworkException {

		InputStream lInputStream;
		// sLog.debug(Messages.getMessage("net.feed.reader", new String[] {
		// url + "", (url != null ? url.getProtocol() : "no-protocol") }));
		// Check if HTTP protocol otherwise assume we have a local file
		if ("http".equals(url.getProtocol())) {

			HttpClient hClient = new HttpClient();
			hClient.getParams().setSoTimeout(NetPropertiesHandler.timeoutValue);
			HostConfiguration hc = hClient.getHostConfiguration();
			hc = setProxySetttings(hClient, hc);

			GetMethod gMethod = new GetMethod(url.toString());
			gMethod.setFollowRedirects(false);
			gMethod.setRequestHeader("cache-control", "no-cache");
			try {
				int status = hClient.executeMethod(gMethod);
				if (status != HttpStatus.SC_OK) {
					// Check for redirection.
					if (status == HttpStatus.SC_MOVED_PERMANENTLY
							|| status == HttpStatus.SC_MOVED_TEMPORARILY
							|| status == HttpStatus.SC_SEE_OTHER
							|| status == HttpStatus.SC_TEMPORARY_REDIRECT) {
						String redirectLocation;
						Header locationHeader = gMethod
								.getResponseHeader("location");
						if (locationHeader != null) {
							redirectLocation = locationHeader.getValue();

							gMethod = new GetMethod(redirectLocation);
							status = hClient.executeMethod(gMethod);
							if (status != HttpStatus.SC_OK) {
								throw new NetworkException(gMethod
										.getStatusLine().getReasonPhrase());
							}
						} else {
							// The response is invalid and did not provide
							// the
							// new
							// location for
							// the resource. Report an error or possibly
							// handle
							// the
							// response
							// like a 404 Not Found error.
						}
					} else {
						if (status == HttpStatus.SC_UNAUTHORIZED) {
							hc = hClient.getHostConfiguration();
							hc = setProxySetttings(hClient, hc);
							hClient.getState().setCredentials(
									new AuthScope(AuthScope.ANY_HOST,
											AuthScope.ANY_PORT,
											AuthScope.ANY_REALM),
									getCredentials(pFeed));
							gMethod = new GetMethod(url.toString());
							gMethod.setDoAuthentication(true);
							status = hClient.executeMethod(gMethod);
							if (status != HttpStatus.SC_OK) {
								throw new NetworkException(gMethod
										.getStatusLine().getReasonPhrase());
							}
						} else {
							throw new NetworkException(gMethod.getStatusLine()
									.getReasonPhrase());
						}

					}
				}

				head = getHeadInfo(gMethod);
				lInputStream = gMethod.getResponseBodyAsStream();

			} catch (HttpException he) {
				throw new NetworkException(he.getMessage());
			} catch (IOException ioe) {
				throw new NetworkException(ioe.getMessage());
			}
		} else {
			try {
				lInputStream = url.openStream();
			} catch (IOException ioe) {
				throw new NetworkException(ioe.getMessage());
			}
		}
		return lInputStream;
	}

	/**
	 * Get the HTTP HEAD information. A static method, which gets the HEAD from
	 * an HTTP Connection.
	 * 
	 * @param feed
	 * @param url
	 * @throws Exception
	 * @return NetHEADInfo
	 */
	public static NetHEADInfo getHeadInfo(IFeed feed, URL url)
			throws NetworkException {
		NetHEADInfo head = null;
		HttpClient hClient = new HttpClient();
		hClient.getParams().setSoTimeout(25000);
		// hClient.setTimeout(5000); deprecated in 3.0 client
		HostConfiguration hc = hClient.getHostConfiguration();
		hc = setProxySetttings(hClient, hc);
		HeadMethod hMethod = null;
		try {
			hMethod = new HeadMethod(url.toString());
			hMethod.setRequestHeader("cache-control", "no-cache");
			int status = hClient.executeMethod(hMethod);

			if (status != HttpStatus.SC_OK) {
				// Check for redirection.
				if (status == HttpStatus.SC_MOVED_PERMANENTLY
						|| status == HttpStatus.SC_MOVED_TEMPORARILY
						|| status == HttpStatus.SC_SEE_OTHER
						|| status == HttpStatus.SC_TEMPORARY_REDIRECT) {
					String redirectLocation;
					Header locationHeader = hMethod
							.getResponseHeader("location");
					if (locationHeader != null) {
						redirectLocation = locationHeader.getValue();
						hMethod = new HeadMethod(redirectLocation);
						status = hClient.executeMethod(hMethod);
						if (status != HttpStatus.SC_OK) {
							throw new NetworkException(hMethod.getStatusLine()
									.getReasonPhrase());
						}
					} else {
						// The response is invalid and did not provide the
						// new
						// location for
						// the resource. Report an error or possibly handle
						// the
						// response
						// like a 404 Not Found error.
					}
				} else {
					if (status == HttpStatus.SC_UNAUTHORIZED) {
						// Retry with password.
						hc = hClient.getHostConfiguration();
						try {

							hClient.getState().setCredentials(
									new AuthScope(AuthScope.ANY_HOST,
											AuthScope.ANY_PORT,
											AuthScope.ANY_REALM),
									getCredentials(feed)

							);
						} catch (Exception e) {
							throw new NetworkException(e.getMessage());
						}
						hMethod = new HeadMethod(url.toString());
						hMethod.setDoAuthentication(true);
						status = hClient.executeMethod(hMethod);
						if (status != HttpStatus.SC_OK) {
							throw new NetworkException(hMethod.getStatusLine()
									.getReasonPhrase());
						}
					} else {
						throw new NetworkException(hMethod.getStatusLine()
								.getReasonPhrase());
					}
				}
			}
			head = getHeadInfo(hMethod);
		} catch (HttpException he) {
			// Received Illegal redirect exception on some URLs
			throw new NetworkException(he.getMessage(), he);
		} catch (IOException ioe) {
			throw new NetworkException(ioe.getMessage(), ioe);
		} catch (java.lang.IllegalArgumentException iae) {
			throw new NetworkException(iae.getMessage(), iae);
		} finally {
			if (hMethod != null) {
				hMethod.releaseConnection();
			}
		}
		return head;
	}

	/**
	 * Set the http proxy server for a host configuration.
	 * 
	 * @param hClient
	 * 
	 * @param hConf
	 *            HostConfiguration
	 * @return HostConfiguration The configuration of the host which will be
	 *         addressed
	 */
	public static HostConfiguration setProxySetttings(HttpClient hClient,
			HostConfiguration hConf) {
		if (NetPropertiesHandler.proxyServer != null) {
			hConf.setProxy(NetPropertiesHandler.proxyServer.getHost(),
					NetPropertiesHandler.proxyServer.getPort());
		}
		// Set the proxy Authentication if the proxy is checked.
		String userName = NetPropertiesHandler.userName;
		String password = NetPropertiesHandler.password;
		if (NetPropertiesHandler.proxyOn
				&& (userName != null && userName.length() > 0)
				&& (password != null && password.length() > 0)) {
			hClient.getState().setProxyCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(userName, password));
		}
		return hConf;
	}

	/**
	 * Get HTTP HEAD information.
	 * 
	 * @param method
	 *            HttpMethodBase
	 * @return NetHEADInfo
	 */
	private static NetHEADInfo getHeadInfo(HttpMethodBase method) {
		Header header;

		String lastModifiedDate = null;
		String contentType = null;
		String contentLength = "0";
		String date = null;

		Header[] lHeaders = method.getResponseHeaders();
		for (int i = 0; i < lHeaders.length; i++) {
			Header lHeader = lHeaders[i];
			String lValue = lHeader.getValue();
			// if (Debug.WITH_DEV_DEBUG && lValue != null && lValue.length() >
			// 0) {
			// sLog.debug(lHeader.getName() + ":" + lValue);
			// }
		}

		if ((header = method.getResponseHeader("Date")) != null) {
			date = header.getValue();
		}
		if ((header = method.getResponseHeader("Last-Modified")) != null) {
			lastModifiedDate = header.getValue();
		}

		if ((header = method.getResponseHeader("Content-Type")) != null) {
			contentType = header.getValue();
		}

		if ((header = method.getResponseHeader("Content-Length")) != null) {
			contentLength = header.getValue();
		}

		// headers = gMethod.getResponseFooters();
		return new NetHEADInfo(contentLength != null ? new Integer(
				contentLength).intValue() : 0, contentType, null, date, null,
				lastModifiedDate);
	}

	/**
	 * Perform network actions in a runnable class. The actions are: 1) Get a
	 * reader from an HTTP Connection. 2) Get the HTTP HEAD information. 3)
	 * Download a HTTP URL to a file on the file system.
	 * <p>
	 * The action is determined by the <code>actionType</code> argument in the
	 * constructor. you have to call the <code>setDownload()</code> method in
	 * case the action is a DOWNLOAD. Various results are reported with a
	 * network action.
	 */
	public class NetDownloadTask implements Runnable {

		final static short DOWNLOAD = 400;

		final static short HEAD = 401;

		final static short STREAM = 402;

		private Download mDownload;

		/**
		 * Constructor.
		 * 
		 * @param actionHandler
		 *            NetActionHandler
		 * @param actionType
		 *            short
		 */
		public NetDownloadTask(Download pDownload) {

			mDownload = pDownload;
		}

		/**
		 * Runnable class. Executes the net action, as provided by the
		 * actionType parameter.
		 */
		public void run() {
			try {
				downLoad(mDownload);
			} catch (Exception ie) {
				// sLog.debug(Messages.getMessage("net.download.failed",
				// new String[] { mDownload + "" }), ie);
				// if (mDownload != null) {
				fireNetActionPerformed(new NetTaskEvent(mDownload,
						NetTaskEvent.DOWNLOAD_FAILED, ie));
			}
		}
	}

	/**
	 * Download using the apache httpClient.
	 * 
	 * @param d
	 *            Downloads.Download
	 * @throws Exception
	 */
	public void downLoad(Download pDownload) throws NetworkException {

		int lState = pDownload.getState();
		if (lState != DownloadLogic.QUEUED) {
			return;
		}

		IPersonalAttachment pEncl = pDownload.getAttachment();

		URI lLink = pEncl.getLink();
		URL lUrl;
		try {
			lUrl = lLink.toURL();
		} catch (MalformedURLException e) {
			// mmmhh bad link. let's exist.
			throw new NetworkException(e.getMessage());
		}

		// sLog.info("Starting download: "
		// + pDownload.getEnclosure().getName());
		// sLog.debug(Messages.getMessage("net.download.start", new Object[] {
		// pDownload, pEncl, pUrl }));

		GetMethod gMethod = null;
		FileOutputStream lOutputStream = null;
		InputStream lInputStream = null;
		int lCount = 0; // Downloaded bytes count

		// STATUS CHANGE CONNECTING
		pDownload.setState(DownloadLogic.CONNECTING);
		fireNetActionPerformed(new NetTaskEvent(pDownload,
				NetTaskEvent.DOWNLOAD_STATUS_CHANGED));
		boolean lRedirect = false;

		try {
			boolean append = false;

			if ("http".equals(lUrl.getProtocol())) {
				HttpClient hClient = new HttpClient();
				hClient.getParams().setSoTimeout(
						NetPropertiesHandler.timeoutValue);
				HostConfiguration hc = hClient.getHostConfiguration();
				hc = setProxySetttings(hClient, hc);
				gMethod = new GetMethod(lUrl.toExternalForm());

				// Add a byte range header asking for partial content.
				if (pDownload.getAttachment().isIncomplete()) {
					append = true;
					addByteRangeHeader(pDownload.getAttachment(), gMethod);
					gMethod.setFollowRedirects(true);
				} else {
					gMethod.setFollowRedirects(false);
				}
				
				// Need to do redirects manually to get the filename for
				// storage. We follow the redirections until a 200 OK.
				// we break in case of error.

				boolean lContinue = true;
				while (lContinue) {

					int status = hClient.executeMethod(gMethod);
					switch (status) { // Switch the result.
					case HttpStatus.SC_OK: {
						lContinue = false;
					}
						break;
					case HttpStatus.SC_MOVED_PERMANENTLY:
					case HttpStatus.SC_MOVED_TEMPORARILY:
					case HttpStatus.SC_SEE_OTHER:
					case HttpStatus.SC_TEMPORARY_REDIRECT: {
						lRedirect = true;
						// The redirection code fails for
						// localhost, use IP address as a workaround.
						String redirectLocation;
						Header locationHeader = gMethod
								.getResponseHeader("location");

						if (locationHeader != null) {
							redirectLocation = locationHeader.getValue();
							gMethod.setFollowRedirects(true);
							lUrl = new URL(redirectLocation);
						} else {

						}
					}
						break;
					case HttpStatus.SC_PARTIAL_CONTENT:
						// sLog.info("(1) Partial download granted for: "
						// + pUrl.toExternalForm());
						// sLog.info("(2) Start at byte: "
						// + gMethod.getRequestHeader("Range")
						// .getValue());
						lContinue = false;
						break;
					case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE: {
						// 28-07-2006, Duh? OK let's try again without
						// Range.
						// sLog.warn("(1) Partial download denied for: "
						// + pUrl.toExternalForm());

						Header lRHeader = gMethod
								.getResponseHeader("Content-Range");
						if (lRHeader != null) {
							// sLog.warn("(2) The content-range is: "
							// + lRHeader.getValue());
						}

						// sLog.warn(gMethod.getResponseBodyAsString());

						Header h = gMethod.getRequestHeader("Range");
						gMethod.removeRequestHeader(h);
						append = false;
					}
						break;
					case HttpStatus.SC_UNAUTHORIZED: {
						// Retry with password.
						hc = hClient.getHostConfiguration();
						hc = setProxySetttings(hClient, hc);
						hClient.getState()
								.setCredentials(
										new AuthScope(AuthScope.ANY_HOST,
												AuthScope.ANY_PORT,
												AuthScope.ANY_REALM),
										getCredentials(pDownload
												.getAttachment().getNews()
												.getFeedReference().resolve()));

						gMethod = new GetMethod(lUrl.toString());
						gMethod.setDoAuthentication(true);
					}
						break;
					case HttpStatus.SC_METHOD_NOT_ALLOWED: {
						// This sometimes happens, we exit with an error
						// message.
						// sLog.warn(gMethod.getStatusLine().getReasonPhrase()
						// + " to URL " + pUrl.toExternalForm());
					}
						break;
					default: {
						pDownload.setState(DownloadLogic.ERROR);
						pDownload.setMessage(gMethod.getStatusLine()
								.getReasonPhrase());

						throw new NetworkException(pDownload.getMessage());
					}
					}
				}
				NetHEADInfo lInfo = getHeadInfo(gMethod);
				if (!append) {
					try {
						pDownload.getAttachment().setContentSize(lInfo.length);
					} catch (IllegalArgumentException iae) {
						// Likely parsing of the HEAD Date failed.
					}
				} else {
					pDownload.getAttachment().setContentSize(
							(int) pDownload.getAttachment().getLength());
				}
				pDownload.getAttachment().setContentDate(
						lInfo.getModifiedLong());

				pDownload.setLength(pDownload.getAttachment().getContentSize());
				lInputStream = gMethod.getResponseBodyAsStream();
			} else {
				pDownload.setLength(0);
				lInputStream = lUrl.openStream();
			}

			// REDIRECT Handling.
			// In case of re-direct we re-create a download file.
			// We don't store the re-direct URL. (It is volatile).
			// The volatile redirect URL is used to generate the file name.
			// if it can't be generated, we create a filename from the
			// the RSS entry and set it in the enclosure object. Before we
			// redownload
			// the file, we also want to
			// check if the file is by chance an already existing file in
			// the folder, cache or player. If so we cancel the download.
			// We fire a change event.

			boolean lInspect = false;
			if (lRedirect) {
				File lFile = pEncl.getFile(true);
				pEncl.setFileName(lFile.getName());
			}

			File lF = pEncl.getFile();
			if (!lF.exists()) {
				lF.createNewFile();
				lInspect = false;
			}

			// --- File Inspection.
			// CB FIXME Do we have to inspect only after redirect?
			// We don't inspect a file with a random number.
			if (lInspect) {
				// If we re-inspect and remark, we ignore the
				// candidate restriction has we are already
				// in the download phase. The enclosure was either
				// a candidate before or downloaded manually.
				pEncl.setCandidate(false);

				// CB TODO, migrate inspections.
				// XFeedLogic.getInstance().inspectSingleFile(pEncl);
				// XFeedLogic.getInstance().markSingleEnclosure(pEncl);

				if (pEncl.isIncomplete()) {
					// CB FIXME This is a problem, as we
					// need to execute again to get the byte range.
					// Rethink the whole redirect handling.
				}

				// CB TODO, this is very jPodder specific.
				// updating the candidate, can likely be done
				// in the model.

				// pEncl.getFeed().updateSingleCandidate(pEncl,
				// Configuration.getInstance().getMarkMax());

				if (!pEncl.isMarked()) {
					pDownload.setState(DownloadLogic.CANCELLED);
					pDownload.setMessage("Redirect leads to existing file");
					throw new NetworkException(
							"Redirect leads to existing file");
				}
			}

			// THE ENCLOSURE FILE IS NEVER NULL, AND EXISTS FROM HERE!
			// WE ARE IN DEEP SHIT OTHERWISE.

			// sLog.info(Messages.getString("tasks.download.newDownload")
			// + pEncl.getFile());

			lOutputStream = new FileOutputStream(lF, append);

			if (append) {
				lCount = (int) pEncl.getFile().length();
			}
			if (pDownload.getLength() != 0) {
			} else {
				// Length of the file is unknown.
				// This not good. (only for HTTP)
			}

			pDownload.setStart(lCount);

			// STATUS CHANGE DOWNLOADING
			pDownload.setState(DownloadLogic.DOWNLOADING);
			fireNetActionPerformed(new NetTaskEvent(pDownload,
					NetTaskEvent.DOWNLOAD_STATUS_CHANGED));
			int read;
			byte[] buffer = new byte[8192];
			while ((read = lInputStream.read(buffer)) > 0) {
				lOutputStream.write(buffer, 0, read);
				lCount += read;
				pDownload.setCurrent(lCount);
				if (pDownload.getState() == DownloadLogic.CANCELLED
						|| pDownload.getState() == DownloadLogic.PAUZED) {
					fireNetActionPerformed(new NetTaskEvent(pDownload,
							NetTaskEvent.DOWNLOAD_STATUS_CHANGED));
					break;
				}
			}

			// sLog.info(Messages
			// .getMessage("net.download.start", new String[] {
			// lCount + "", pDownload.lengthOfTask + "" }));

		} catch (org.apache.commons.httpclient.HttpException he) {
			pDownload.setState(DownloadLogic.ERROR);
			pDownload.setMessage(he.getMessage());
		} catch (java.io.FileNotFoundException fnfe) {
			pDownload.setState(DownloadLogic.ERROR);
			pDownload.setMessage(fnfe.getMessage());
		} catch (java.io.IOException ioe) {
			pDownload.setState(DownloadLogic.ERROR);
			pDownload.setMessage(ioe.getMessage());
		} catch (Exception e) {
			pDownload.setState(DownloadLogic.ERROR);
			pDownload.setMessage(e.getMessage());
		}

		// catch (java.lang.IllegalArgumentException iae) {
		// pDownload.setState(DownloadLogic.ERROR);
		// pDownload.setMessage("Programatic Error");
		// }
		finally {

			lState = pDownload.getState();
			if (lState == DownloadLogic.DOWNLOADING) {
				// Incompleted downlads are checked.
				if (pDownload.getLength() != 0
						&& lCount != pDownload.getLength()) {
					// sLog.info("Partial download of : "
					// + pDownload.getEnclosure().getFile());
					pDownload.setState(DownloadLogic.ERROR);
					// pDownload.setMessage(Messages
					// .getString("nettask.incomplete"));
				} else {
					// STATUS CHANGE COMPLETE
					pDownload.setState(DownloadLogic.COMPLETED);
					fireNetActionPerformed(new NetTaskEvent(pDownload,
							NetTaskEvent.DOWNLOAD_STATUS_CHANGED));
					fireNetActionPerformed(new NetTaskEvent(pDownload,
							NetTaskEvent.DOWNLOAD_SUCCESS));
				}
			}

			if (lState == DownloadLogic.ERROR) {
				fireNetActionPerformed(new NetTaskEvent(pDownload,
						NetTaskEvent.DOWNLOAD_STATUS_CHANGED));
				throw new NetworkException(pDownload.getMessage());
			}

			if (gMethod != null) {
				gMethod.abort();
				gMethod.releaseConnection();
			}
			try {
				if (lOutputStream != null)
					lOutputStream.close();
				if (lInputStream != null) {
					lInputStream.close();
				}
			} catch (IOException ioe) {
				throw new NetworkException(ioe.getMessage());
			}

		}
	}

	private void addByteRangeHeader(IPersonalAttachment encl, GetMethod gMethod) {

		int fileLen = new Long(encl.getFile().length()).intValue();
		String contentRange = "bytes=" + new Integer((fileLen)).toString()
				+ "-";

		Header head = new Header();
		head.setName("Range");
		head.setValue(contentRange);
		gMethod.addRequestHeader(head);
	}

	// CB TODO.

	// public class AuthDialog implements CredentialsProvider {
	//
	// public AuthDialog() {
	// super();
	// }
	//
	// public Credentials getCredentials(final AuthScheme authscheme,
	// final String host, int port, boolean proxy)
	// throws CredentialsNotAvailableException {
	// if (authscheme == null) {
	// return null;
	// }
	// try {
	// if (authscheme instanceof NTLMScheme) {
	// // System.out.println(host + ":" + port + " requires
	// // Windows
	// // authentication");
	// // System.out.print("Enter domain: ");
	// // String domain = readConsole();
	// // System.out.print("Enter username: ");
	// // String user = readConsole();
	// // System.out.print("Enter password: ");
	// // String password = readConsole();
	// // return new NTCredentials(user, password, host,
	// // domain);
	// throw new CredentialsNotAvailableException(
	// "Unsupported authentication scheme: "
	// + authscheme.getSchemeName());
	// } else if (authscheme instanceof RFC2617Scheme) {
	// System.out.println(host + ":" + port
	// + " requires authentication with the realm '"
	// + authscheme.getRealm() + "'");
	// int result = CredentialDialog.showDialog();
	// if (result == JOptionPane.OK_OPTION) {
	// String username = CredentialDialog.getNameField()
	// .getText();
	// String password = CredentialDialog.getPassField()
	// .getText();
	// return new UsernamePasswordCredentials(username,
	// password);
	// } else {
	// // User pressed cancel.
	// throw new CredentialsNotAvailableException(
	// "Unsupported authentication scheme: "
	// + authscheme.getSchemeName());
	// }
	//
	// } else {
	// throw new CredentialsNotAvailableException(
	// "Unsupported authentication scheme: "
	// + authscheme.getSchemeName());
	// }
	// } catch (IOException e) {
	// throw new CredentialsNotAvailableException(e.getMessage(),
	// e);
	// }
	// }
	// }
	// }

	public static UsernamePasswordCredentials getCredentials(IFeed feed)
			throws NetworkException {

		// FeedCredentials fc = feed.getCredentials();

		// UsernamePasswordCredentials credentials = null;
		// if (fc != null) {
		// credentials = new UsernamePasswordCredentials(fc.getName(), fc
		// .getPassword());
		// } else {

		// Credentials are unknown, show a popup dialog.
		// CB TODO Handover the UI portion to some other part
		// of the code.

		// int result = CredentialDialog.showDialog();
		// if (result == JOptionPane.OK_OPTION) {
		//
		// // FIXME Migrate Authentication support.
		// String username = ""; // CredentialDialog.getNameField().getText();
		// String password = ""; // CredentialDialog.getPassField().getText();
		//
		// credentials = new UsernamePasswordCredentials(username,
		// password);
		// FeedCredentials lCredentials = ((XPersonalFeed) feed).new
		// FeedCredentials(
		// username, password);
		// feed.setCredentials(lCredentials);
		// } else {
		// // User cancelled, throw exception, can not authenticate.
		// throw new NetworkException("Authentication aborted.");
		// }
		// }
		// return credentials;
		return null;
	}
}