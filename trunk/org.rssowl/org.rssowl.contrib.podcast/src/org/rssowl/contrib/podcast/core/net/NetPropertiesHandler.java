///////////////////////////
// Network properties, should be retrieved from the default eclipse preferences. 

package org.rssowl.contrib.podcast.core.net;

/**
 * @author <a href="mailto:christophe@kualasoft.com">Christophe Bouhier</a>
 * @author <a href="mailto:andreas.schaefer@madplanet.com">Andreas Schaefer</a>
 * @version 1.0
 */
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.httpclient.HostConfiguration;

//import com.jpodder.data.configuration.Configuration;
//import com.jpodder.data.configuration.ConfigurationEvent;
//import com.jpodder.data.configuration.IConfigurationListener;
//import com.jpodder.data.configuration.ConfigurationLogic;

/**
 * Network properties handler. Supports proxy server and HTTP timeout values.
 */

// CB TODO FIXME TODO
// CB Removed the property handling 
public class NetPropertiesHandler {

    //  private final Main app;
    public static boolean proxyOn = false;

    public static String userName = null;

    public static String password = null;

    public static URL proxyServer = null;

    public static int timeoutValue = 15000;

//    private static Logger sLog = Logger.getLogger(NetPropertiesHandler.class
//            .getName());

    private static NetPropertiesHandler sSelf;

    public static NetPropertiesHandler getInstance() {
        if (sSelf == null) {
            sSelf = new NetPropertiesHandler();
        }
        return sSelf;
    }

    /**
     * Constructor.
     */
    public NetPropertiesHandler() {

    }

//    /**
//     * Satisfy interface. Get the http Proxy server properties.
//     * 
//     * @param event
//     *            PropertyEvent
//     */
//    public void configurationChanged(ConfigurationEvent event) {
//        if(!event.getSource().equals(ConfigurationLogic.class)){
//            return;
//        }
//        Configuration.Connection lConfigurationConnection = Configuration
//                .getInstance().getConnection();
//        proxyOn = lConfigurationConnection.getProxyEnabled();
//        if (proxyOn) {
//            proxyServer = lConfigurationConnection.getProxy();
//            // Set the port in the URL.
//            String lHost = proxyServer.getHost();
//            String lProtocol = proxyServer.getProtocol();
//            try {
//                proxyServer = new URL(lProtocol, lHost,
//                        lConfigurationConnection.getProxyPort(), "");
//            } catch (MalformedURLException e) {
//                // CB Should never happen
//            }
//        } else {
//            proxyServer = null;
//        }
//        userName = lConfigurationConnection.getUserName();
//        password = lConfigurationConnection.getPassword();
//        setJVMProxySettings(proxyServer);
//        int lTimeOutValue = lConfigurationConnection.getTimeout();
//        if (lTimeOutValue == 0) {
//            // Inform UI about the default value.
//            Configuration.getInstance().getConnection().setTimeout(timeoutValue/1000);
//            ConfigurationLogic.getInstance().fireConfigurationChanged(new ConfigurationEvent(this,
//                    Configuration.CONNECTION_HTTP_TIMEOUT));
//        } else {
//            timeoutValue = lTimeOutValue * 1000;
//        }
//
//    }
    
    
    

    /**
     * Set the proxysettings for java.net.
     * 
     * @param url
     *            URL
     */
    private static void setJVMProxySettings(URL url) {
        if (url != null) {
            setJVMProxySettings(url.getHost(), url.getPort());
        } else {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
        }
    }

    /**
     * Set proxy server settings for java.net. This method sets the proxy
     * settings directly in the jvm property variables
     * <code>http.proxyHost</code> and <code>http.proxyPort</code>.
     * 
     * @param host
     *            String
     * @param port
     *            int
     */
    public static void setJVMProxySettings(String host, int port) {
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", new Integer(port).toString());
    }

    /**
     * Set the http proxy server for a host configuration.
     * 
     * @param hConf
     *            HostConfiguration
     * @return HostConfiguration The configuration of the host which will be
     *         addressed
     */
    public static HostConfiguration setProxySetttings(HostConfiguration hConf) {
        if (proxyServer != null) {
            hConf.setProxy(proxyServer.getHost(), proxyServer.getPort());
        }
        return hConf;
    }
}