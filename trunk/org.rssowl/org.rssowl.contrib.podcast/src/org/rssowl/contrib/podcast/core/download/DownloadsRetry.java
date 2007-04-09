package org.rssowl.contrib.podcast.core.download;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier </a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @version 1.1
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * Downloads which fail due to time outs go into automatic retry mode. A
 * download attempt which is still in the download list because of failure can
 * be retried. Retry mode is initiated after a failed download. The download
 * object contains a retry counter.
 * 
 * The retry count is reported to the download table.
 */
public class DownloadsRetry {

//    private Logger mLog = Logger.getLogger(getClass().getName());

    public static long CALMDOWN_PERIOD = 10000;
    public static int RETRY_LIMIT = 3;

    /**
     * Retry a download after a timer expires. Note: This methods could be
     * invoked from within the download thread.
     * 
     * @param pDownload
     */
    public void retryLater(final Download pDownload) {

        int retries = pDownload.getRetryCounter();
        if (retries <= RETRY_LIMIT) {
            pDownload.setState(DownloadLogic.RETRYING);
//            mLog.info("Retry timer activated, retries=" + retries);
            Timer t = new Timer((int) CALMDOWN_PERIOD, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        pDownload
                                .setRetryCounter(pDownload.getRetryCounter() + 1);
//                        mLog.info("Retrying"
//                                + pDownload.getAttachment());
//                        DownloadLogic.getInstance().download(pDownload);

                    } catch (Exception e1) {
//                        mLog.info("Retry failed"
//                                + pDownload.getAttachment());
                    }
                }
            });
            t.setInitialDelay((int) CALMDOWN_PERIOD);
            t.setRepeats(false);
            t.start();

        } else {
//            mLog.info("No more retries allowed for"
//                    + pDownload.getAttachment());
//            pDownload.setState(DownloadLogic.ERROR);
//            pDownload.setMessage("");
        }
    }
}