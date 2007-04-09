package org.rssowl.contrib.podcast.content;


/**
 * This class defines a generic mimetype.
 * http://www.iana.org/assignments/media-types/text/
 * Please note, non registered media types also exist.
 */
public interface IContent {

    /**
     * Comment for <code>MIME_MPEG</code>
     */
    public static final String MIME_MPEG = "audio/mpeg";
    public static final String EXTENSION_MPEG = "mpeg";
    
    /**
     * Comment for <code>MIME_MPG</code>
     */
    public static final String MIME_MPG = "audio/mpg";
    public static final String EXTENSION_MPG = "mpg";

    /**
     * Comment for <code>MIME_MPEG_X3</code>
     */
    public static final String MIME_MPEG_X3 = "audio/x-mpeg-3";

    /**
     * Comment for <code>MIME_MPEG_X</code>
     */
    public static final String MIME_MPEG_X = "audio/x-mpeg";

    /**
     * Comment for <code>MIME_MP3</code>
     */
    public static final String MIME_MP3 = "audio/mp3";
    public static final String EXTENSION_MP3 = "mp3";
    
    /**
     * Comment for <code>MIME_WMA</code>
     */
    public static final String MIME_WMA = "audio/wma";
    public static final String EXTENSION_WMA = "wma";
    
    /**
     * Comment for <code>MIME_BT</code>
     */
    public static final String MIME_BT = "application/x-bittorent";
    public static final String EXTENSION_BT = "torrent";
    /**
     * Comment for <code>MIME_MOV</code>
     */
    public static final String MIME_MOV = "video/quicktime";
    public static final String EXTENSION_MOV = "mov";

    /**
     * Comment for <code>MIME_AAC</code>
     */
    public static final String MIME_AAC = "audio/mp4";
    public static final String EXTENSION_AAC = "mp4";

    /**
     * Comment for <code>MIME_AAC</code>
     */
    public static final String MIME_M4A = "audio/x-m4a";
    public static final String EXTENSION_M4A = "m4a";
    
    /**
     * Comment for <code>MIME_MPEG_4 Video</code>
     */
    public static final String MIME_MP4V = "video/x-m4v";
    public static final String EXTENSION_MP4V = "m4v";
    
    
    /**
     * Comment for <code>MIME_TEXT_HTML</code>
     */
    public static final String MIME_TEXT_HTML = "text/html";
    public static final String EXTENSION_HTML = "htm";    
    
    
    public static final String[] EXTENSIONS = {EXTENSION_MPEG, 
        EXTENSION_MPG, EXTENSION_MP3, EXTENSION_WMA, EXTENSION_BT, 
        EXTENSION_MOV, EXTENSION_AAC, EXTENSION_MP4V
    };
    
    public static final String[] MIME_TYPES = { MIME_BT, MIME_MPEG, MIME_MPEG_X3,
            MIME_MPG, MIME_WMA, MIME_MOV, MIME_AAC, MIME_M4A, MIME_TEXT_HTML };

    public static final String[] BASE_TYPES = { MIME_BT, MIME_MPEG, MIME_WMA,
            MIME_AAC };
    
    public String getName();

    public String getContent();
    
    
    public String getExtension();
}