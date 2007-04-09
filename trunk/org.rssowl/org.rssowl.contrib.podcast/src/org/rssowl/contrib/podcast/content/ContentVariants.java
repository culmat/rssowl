package org.rssowl.contrib.podcast.content;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * MIME Types are unfortunatly expressed in various ways. This class creates a
 * map of a base MIME type and it's associated variants.
 */
public class ContentVariants {

	public static Hashtable variantMap = new Hashtable();

	public static String[] MPEG_VARIANTS = { IContent.MIME_MPG,
			IContent.MIME_MPEG_X3, IContent.MIME_MP3 };

	public static String[] MP4_VARIANTS = { IContent.MIME_AAC,
			IContent.EXTENSION_M4A };

	/**
	 * Map the base MIME types to their variants. if the variants = null, we
	 * return true;
	 */
	static {
		variantMap.put(IContent.MIME_MPEG, MPEG_VARIANTS);
		variantMap.put(IContent.MIME_AAC, MP4_VARIANTS);
	}

	/**
	 * Query if this MIME type could be a variant type of the provided base type
	 * in String format.
	 * 
	 * @param base
	 * @param suspect
	 * @return
	 */
	public static boolean isVariant(String base, String suspect) {
		try {
			return isVariant(new Content(base), new Content(suspect));
		} catch (ContentException e) {
			return false;
		}
	}

	/**
	 * Returns <code>true</code> if the provided suspect is a variant.
	 * 
	 * @param base
	 * @param suspect
	 * @return boolean
	 */
	public static boolean isVariant(IContent base, IContent suspect) {
		boolean match = false;
		String[] variants = (String[]) variantMap.get(base.getName());
		for (int i = 0; i < variants.length; i++) {
			if (variants[i].equals(suspect.getName())) {
				match = true;
				break;
			}
		}
		return match;
	}

	/**
	 * Get if this content type has any variants registered.
	 * 
	 * @param pContent
	 * @return boolean
	 */
	public static boolean hasVariants(IContent pContent) {
		return variantMap.contains(pContent.getContent());
	}

	/**
	 * Get if this a base type.
	 * 
	 * @param suspect
	 * @return boolean
	 */
	public static boolean isBaseType(IContent suspect) {
		boolean match = false;
		Object o;
		if ((o = variantMap.get(suspect.getContent())) != null) {
			match = true;
		}
		return match;
	}

	/**
	 * Get the base
	 * 
	 * @param suspect
	 * @return MIMEType
	 */
	public static IContent getBaseType(IContent suspect) {
		String base = null;

		Enumeration lEnum = variantMap.keys();
		while (lEnum.hasMoreElements()) {
			String type = (String) lEnum.nextElement();
			String[] variants = (String[]) variantMap.get(type);
			for (int i = 0; i < variants.length; i++) {
				if (variants[i].equals(suspect.getName())) {
					base = type;
				}
			}
		}
		if (base != null) {
			try {
				return new Content(base);
			} catch (ContentException ce) {
				// Can not happen for base types.
			}
		}
		return null;
	}

	/**
	 * Get the String array of variants.
	 * 
	 * @param base
	 * @return String[]
	 */
	public static String[] getVariants(IContent base) {
		return (String[]) variantMap.get(base.getName());
	}
}
