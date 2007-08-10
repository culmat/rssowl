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

package org.rssowl.core.internal.interpreter;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.util.URIUtils;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Importer for the popular OPML Format. Will create a new Folder that contains
 * all Folders and Feeds of the OPML.
 *
 * @author bpasero
 */
public class OPMLImporter implements ITypeImporter {
  private DateFormat fDateFormat = DateFormat.getDateInstance();

  /*
   * @see org.rssowl.core.interpreter.ITypeImporter#importFrom(org.jdom.Document)
   */
  @SuppressWarnings("unused")
  public List<? extends IEntity> importFrom(Document document) throws InterpreterException {
    Element root = document.getRootElement();

    /* Interpret Children */
    List<?> feedChildren = root.getChildren();
    for (Iterator<?> iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Body */
      if ("body".equals(name)) //$NON-NLS-1$
        return processBody(child);
    }

    return null;
  }

  private List<IFolder> processBody(Element body) {
    IFolder folder = Owl.getModelFactory().createFolder(null, null, "Imported from OPML");

    /* Interpret Children */
    List<?> feedChildren = body.getChildren();
    for (Iterator<?> iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, folder);

      else if ("savedsearch".equals(name))
        processSavedSearch(child, folder);
    }

    return Collections.singletonList(folder);
  }

  private void processSavedSearch(Element savedSearchElement, IFolder folder) {
    Namespace namespace = Namespace.getNamespace("rssowl", "http://www.rssowl.org");

    String name = savedSearchElement.getAttributeValue("name");
    boolean matchAllConditions = Boolean.parseBoolean(savedSearchElement.getAttributeValue("matchAllConditions"));

    ISearchMark searchmark = Owl.getModelFactory().createSearchMark(null, folder, name);
    searchmark.setMatchAllConditions(matchAllConditions);

    List<?> conditions = savedSearchElement.getChildren("searchcondition", namespace);
    for (int i = 0; i < conditions.size(); i++) {
      try {
        Element condition = (Element) conditions.get(i);

        /* Search Specifier */
        Element specifierElement = condition.getChild("searchspecifier", namespace);
        SearchSpecifier searchSpecifier = SearchSpecifier.values()[Integer.parseInt(specifierElement.getAttributeValue("id"))];

        /* Search Value */
        Element valueElement = condition.getChild("searchvalue", namespace);
        Object value = getValue(valueElement, namespace);

        /* Search Field */
        Element fieldElement = condition.getChild("searchfield", namespace);
        String fieldName = fieldElement.getAttributeValue("name");
        String entityName = fieldElement.getAttributeValue("entity");
        ISearchField searchField = Owl.getModelFactory().createSearchField(getFieldID(fieldName), entityName);

        searchmark.addSearchCondition(Owl.getModelFactory().createSearchCondition(searchField, searchSpecifier, value));
      } catch (NumberFormatException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (ParseException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private int getFieldID(String fieldName) {
    if ("allFields".equals(fieldName))
      return IEntity.ALL_FIELDS;

    if ("title".equals(fieldName))
      return INews.TITLE;

    if ("link".equals(fieldName))
      return INews.LINK;

    if ("description".equals(fieldName))
      return INews.DESCRIPTION;

    if ("publishDate".equals(fieldName))
      return INews.PUBLISH_DATE;

    if ("modifiedDate".equals(fieldName))
      return INews.MODIFIED_DATE;

    if ("receiveDate".equals(fieldName))
      return INews.RECEIVE_DATE;

    if ("author".equals(fieldName))
      return INews.AUTHOR;

    if ("comments".equals(fieldName))
      return INews.COMMENTS;

    if ("guid".equals(fieldName))
      return INews.GUID;

    if ("source".equals(fieldName))
      return INews.SOURCE;

    if ("hasAttachments".equals(fieldName))
      return INews.HAS_ATTACHMENTS;

    if ("attachments".equals(fieldName))
      return INews.ATTACHMENTS_CONTENT;

    if ("categories".equals(fieldName))
      return INews.CATEGORIES;

    if ("isFlagged".equals(fieldName))
      return INews.IS_FLAGGED;

    if ("state".equals(fieldName))
      return INews.STATE;

    if ("label".equals(fieldName))
      return INews.LABEL;

    if ("rating".equals(fieldName))
      return INews.RATING;

    if ("feed".equals(fieldName))
      return INews.FEED;

    if ("ageInDays".equals(fieldName))
      return INews.AGE_IN_DAYS;

    if ("location".equals(fieldName))
      return INews.LOCATION;

    return IEntity.ALL_FIELDS;
  }

  private Object getValue(Element valueElement, Namespace namespace) throws ParseException {
    Object value = null;
    int valueType = Integer.parseInt(valueElement.getAttributeValue("type"));

    /* Treat set of News States separately */
    List<?> newsStateElements = valueElement.getChildren("newsstate", namespace);
    if (newsStateElements.size() > 0) {
      List<INews.State> states = new ArrayList<INews.State>(newsStateElements.size());
      for (int i = 0; i < newsStateElements.size(); i++) {
        Element newsStateElement = (Element) newsStateElements.get(i);
        int ordinal = Integer.parseInt(newsStateElement.getAttributeValue("value"));
        states.add(INews.State.values()[ordinal]);
      }

      value = EnumSet.copyOf(states);
    }

    /* Any other Value */
    else {
      String valueAsString = valueElement.getAttributeValue("value");

      switch (valueType) {
        case ISearchValueType.BOOLEAN:
          value = Boolean.parseBoolean(valueAsString);
          break;

        case ISearchValueType.STRING:
          value = valueAsString;
          break;

        case ISearchValueType.LINK:
          value = valueAsString;
          break;

        case ISearchValueType.INTEGER:
          value = Integer.parseInt(valueAsString);
          break;

        case ISearchValueType.NUMBER:
          value = Integer.parseInt(valueAsString);
          break;

        case ISearchValueType.DATE:
          value = fDateFormat.parse(valueAsString);
          break;

        case ISearchValueType.DATETIME:
          value = fDateFormat.parse(valueAsString);
          break;

        case ISearchValueType.TIME:
          value = fDateFormat.parse(valueAsString);
          break;

        case ISearchValueType.ENUM:
          value = valueAsString;
          break;
      }
    }

    return value;
  }

  private void processOutline(Element outline, IPersistable parent) {
    IPersistable type = null;
    String title = null;
    String link = null;
    String homepage = null;
    String description = null;

    /* Interpret Attributes */
    List<?> attributes = outline.getAttributes();
    for (Iterator<?> iter = attributes.iterator(); iter.hasNext();) {
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

    /* Outline is a Folder */
    if (link == null && title != null) {
      type = Owl.getModelFactory().createFolder(null, (IFolder) parent, title);
    }

    /* Outline is a BookMark */
    else {
      URI uri = link != null ? URIUtils.createURI(link) : null;
      if (uri != null) {

        /* Check if a Feed with the URL already exists */
        IFeedDAO feedDao = Owl.getPersistenceService().getDAOService().getFeedDAO();
        FeedReference feedRef = feedDao.loadReference(uri);

        /* Create a new Feed then */
        if (feedRef == null) {
          IFeed feed = Owl.getModelFactory().createFeed(null, uri);
          feed.setHomepage(homepage != null ? URIUtils.createURI(homepage) : null);
          feed.setDescription(description);
          feed = DynamicDAO.save(feed);
        }

        /* Create the BookMark */
        FeedLinkReference feedLinkRef = new FeedLinkReference(uri);
        type = Owl.getModelFactory().createBookMark(null, (IFolder) parent, feedLinkRef, title != null ? title : link);
      }
    }

    /* In case this Outline Element did not represent a Category */
    if (type == null || type instanceof IBookMark)
      return;

    /* Recursivley Interpret Children */
    List<?> feedChildren = outline.getChildren();
    for (Iterator<?> iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, type);
    }
  }
}