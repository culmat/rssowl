package org.rssowl.contrib.podcast.content;

import java.util.TreeMap;

/**
 * A Content type implementation.
 */
public class Content implements IContent {

	public static final TreeMap lContentToExtensionMap = new TreeMap();
    
    static{
    	lContentToExtensionMap.put(IContent.MIME_AAC, IContent.EXTENSION_AAC);
    	lContentToExtensionMap.put(IContent.MIME_MP3, IContent.EXTENSION_MP3);
    	lContentToExtensionMap.put(IContent.MIME_MPEG, IContent.EXTENSION_MP3);
    	lContentToExtensionMap.put(IContent.MIME_MPG, IContent.EXTENSION_MP3);
    	lContentToExtensionMap.put(IContent.MIME_MPEG_X, IContent.EXTENSION_MP3);
    	lContentToExtensionMap.put(IContent.MIME_MPEG_X3, IContent.EXTENSION_MP3);
    	lContentToExtensionMap.put(IContent.MIME_WMA, IContent.EXTENSION_WMA);
    	lContentToExtensionMap.put(IContent.MIME_BT, IContent.EXTENSION_BT);
    	lContentToExtensionMap.put(IContent.MIME_MP4V, IContent.EXTENSION_MP4V);
    	lContentToExtensionMap.put(IContent.MIME_MOV, IContent.EXTENSION_MOV);
    }    

	
    /**
     * The basic MIME_TYPE.
     * Comment for <code>mimeType</code>
     */
    private String mContent;
    
    /**
     * @param pMimeType
     */
    public Content(String pMimeType) throws ContentException{
        
    	if(assertObject(pMimeType)){
    		mContent = pMimeType;	
    	}else{
    		throw new ContentException("Mime type not recognized");
    	}
    	
    
    }

    /**
     * Get the mimetype.
     * 
     * @see org.rssowl.contrib.podcast.content.IContent#getName()
     */
    public String getName() {
        return mContent;
    }
    
    /**
     * @return Returns the mimeType.
     */
    public String getContent() {
        return mContent;
    }
    
    public boolean equals(Content pContent){
        return pContent.getContent().equals(mContent);
    }

    public String getExtension() {
    	return (String)lContentToExtensionMap.get(mContent);
    }
    
    private boolean assertObject(String pMimeType){
    	Object lExtension = lContentToExtensionMap.get(pMimeType);
    	if(lExtension != null && ( lExtension instanceof String)){
    		return true;
    	}else{
    		return false;
    	}
    }
}