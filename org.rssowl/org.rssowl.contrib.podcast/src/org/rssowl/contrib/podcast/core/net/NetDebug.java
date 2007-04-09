package org.rssowl.contrib.podcast.core.net;

/**
 * Turn on HTTP Client wire logging 
 */
public class NetDebug {

    public NetDebug(){
    }
    
    public static void initialize(){
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");

        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");    
    }
}
