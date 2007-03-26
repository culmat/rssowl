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

package org.rssowl.core.interpreter.internal;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.Interpreter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.model.types.IBookMark;
import org.rssowl.core.model.types.IEntity;
import org.rssowl.core.model.types.IExtendableType;
import org.rssowl.core.model.types.IFeed;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.core.util.URIUtils;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Importer for the popular OPML Format. Will create a new Folder that contains
 * all Folders and Feeds of the OPML.
 * 
 * @author bpasero
 */
public class OPMLImporter implements ITypeImporter {

  /*
   * @see org.rssowl.core.interpreter.ITypeImporter#importFrom(org.jdom.Document)
   */
  @SuppressWarnings("unused")
  public List< ? extends IEntity> importFrom(Document document) throws InterpreterException {
    Element root = document.getRootElement();

    /* Interpret Children */
    List< ? > feedChildren = root.getChildren();
    for (Iterator< ? > iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Body */
      if ("body".equals(name)) //$NON-NLS-1$
        return processBody(child);
    }

    return null;
  }

  private List<IFolder> processBody(Element body) {
    IFolder folder = Interpreter.getDefault().getTypesFactory().createFolder(null, "Imported from OPML");

    /* Interpret Children */
    List< ? > feedChildren = body.getChildren();
    for (Iterator< ? > iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, folder);
    }

    return Collections.singletonList(folder);
  }

  private void processOutline(Element outline, IExtendableType parent) {
    IExtendableType type = null;
    String title = null;
    String link = null;
    String homepage = null;
    String description = null;

    /* Interpret Attributes */
    List< ? > attributes = outline.getAttributes();
    for (Iterator< ? > iter = attributes.iterator(); iter.hasNext();) {
      Attribute attribute = (Attribute) iter.next();
      String name = attribute.getName();

      /* Link */
      if (name.toLowerCase().equals("xmlurl")) //$NON-NLS-1$
        link = attribute.getValue();

      /* Title */
      else if (name.toLowerCase().equals("title")) //$NON-NLS-1$
        title = attribute.getValue();

      /* Text */
      else if (title == null && name.toLowerCase().equals("text")) //$NON-NLS-1$
        title = attribute.getValue();

      /* Homepage */
      else if (name.toLowerCase().equals("htmlurl")) //$NON-NLS-1$
        homepage = attribute.getValue();

      /* Description */
      else if (name.toLowerCase().equals("description")) //$NON-NLS-1$
        description = attribute.getValue();
    }

    /* Outline is a Category */
    if (link == null && title != null) {
      type = Interpreter.getDefault().getTypesFactory().createFolder((IFolder) parent, title);
    }

    /* Outline is a BookMark */
    else {
      IApplicationLayer applicationLayer = NewsModel.getDefault().getPersistenceLayer().getApplicationLayer();
      URI uri = link != null ? URIUtils.createURI(link) : null;
      if (uri != null) {

        /* Check if a Feed with the URL already exists */
        FeedReference feedRef = applicationLayer.loadFeedReference(uri);

        /* Create a new Feed then */
        if (feedRef == null) {
          IFeed feed = Interpreter.getDefault().getTypesFactory().createFeed(uri);
          feed.setHomepage(homepage != null ? URIUtils.createURI(homepage) : null);
          feed.setDescription(description);
          feed = NewsModel.getDefault().getPersistenceLayer().getModelDAO().saveFeed(feed);
        }

        /* Create the BookMark */
        FeedLinkReference feedLinkRef = new FeedLinkReference(uri);
        type = Interpreter.getDefault().getTypesFactory().createBookMark((IFolder) parent, feedLinkRef, title != null ? title : link);
      }
    }

    /* In case this Outline Element did not represent a Category */
    if (type == null || type instanceof IBookMark)
      return;

    /* Recursivley Interpret Children */
    List< ? > feedChildren = outline.getChildren();
    for (Iterator< ? > iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, type);
    }
  }
}