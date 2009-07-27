/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
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
import org.rssowl.core.internal.newsaction.CopyNewsAction;
import org.rssowl.core.internal.newsaction.LabelNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.util.URIUtils;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
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
  private final DateFormat fDateFormat = DateFormat.getDateInstance();
  private final Namespace fRSSOwlNamespace = Namespace.getNamespace("rssowl", "http://www.rssowl.org");

  /*
   * @see
   * org.rssowl.core.interpreter.ITypeImporter#importFrom(org.jdom.Document)
   */
  public List<? extends IEntity> importFrom(Document document) {
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

  private List<IEntity> processBody(Element body) {
    IFolder defaultRootFolder = Owl.getModelFactory().createFolder(null, null, "My Bookmarks");
    List<IEntity> importedEntities = new ArrayList<IEntity>();
    importedEntities.add(defaultRootFolder);

    /* Interpret Children */
    List<?> feedChildren = body.getChildren();
    for (Iterator<?> iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, defaultRootFolder, importedEntities);

      /* Process Saved Search */
      else if ("savedsearch".equals(name))
        processSavedSearch(child, defaultRootFolder);

      /* Process News Bin */
      else if ("newsbin".equals(name))
        processNewsBin(child, defaultRootFolder);

      /* Process Label */
      else if ("label".equals(name))
        processLabel(child, importedEntities);

      /* Process News Filter */
      else if ("searchfilter".equals(name))
        processFilter(child, importedEntities);
    }

    return importedEntities;
  }

  private void processFilter(Element filterElement, List<IEntity> importedEntities) {
    IModelFactory factory = Owl.getModelFactory();

    String name = filterElement.getAttributeValue("name");
    int order = Integer.parseInt(filterElement.getAttributeValue("order"));
    boolean isEnabled = Boolean.parseBoolean(filterElement.getAttributeValue("enabled"));
    boolean matchAllNews = Boolean.parseBoolean(filterElement.getAttributeValue("matchAllNews"));

    /* Search if provided */
    ISearch search = null;
    Element searchElement = filterElement.getChild("search", fRSSOwlNamespace);
    if (searchElement != null) {
      search = factory.createSearch(null);
      search.setMatchAllConditions(Boolean.parseBoolean(searchElement.getAttributeValue("matchAllConditions")));

      /* Search Conditions */
      List<?> conditions = searchElement.getChildren("searchcondition", fRSSOwlNamespace);
      for (int i = 0; i < conditions.size(); i++) {
        try {
          Element condition = (Element) conditions.get(i);

          ISearchCondition searchCondition = processSearchCondition(condition);
          if (searchCondition != null)
            search.addSearchCondition(searchCondition);
        } catch (NumberFormatException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        } catch (ParseException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    }

    /* Filter */
    ISearchFilter filter = factory.createSearchFilter(null, search, name);
    filter.setEnabled(isEnabled);
    filter.setMatchAllNews(matchAllNews);
    filter.setOrder(order);

    /* Filter Actions */
    List<?> actions = filterElement.getChildren("filteraction", fRSSOwlNamespace);
    for (int i = 0; i < actions.size(); i++) {
      Element action = (Element) actions.get(i);
      String id = action.getAttributeValue("id");
      String data = action.getAttributeValue("data");

      IFilterAction filterAction = factory.createFilterAction(id);
      if (data != null) {

        /* Special case Label Action */
        if (LabelNewsAction.ID.equals(id)) {
          Long labelId = Long.parseLong(data);
          filterAction.setData(labelId);
        }

        /* Special case Move/Copy Action */
        else if (MoveNewsAction.ID.equals(id) || CopyNewsAction.ID.equals(id)) {
          String[] binIds = data.split(",");
          Long[] binIdsLong = new Long[binIds.length];
          for (int j = 0; j < binIds.length; j++) {
            binIdsLong[j] = Long.parseLong(binIds[j]);
          }

          filterAction.setData(binIdsLong);
        }

        /* Any other Action */
        else {
          filterAction.setData(data);
        }
      }

      filter.addAction(filterAction);
    }

    importedEntities.add(filter);
  }

  private void processSavedSearch(Element savedSearchElement, IFolder folder) {
    String name = savedSearchElement.getAttributeValue("name");
    boolean matchAllConditions = Boolean.parseBoolean(savedSearchElement.getAttributeValue("matchAllConditions"));

    ISearchMark searchmark = Owl.getModelFactory().createSearchMark(null, folder, name);
    searchmark.setMatchAllConditions(matchAllConditions);

    List<?> conditions = savedSearchElement.getChildren("searchcondition", fRSSOwlNamespace);
    for (int i = 0; i < conditions.size(); i++) {
      try {
        Element condition = (Element) conditions.get(i);

        ISearchCondition searchCondition = processSearchCondition(condition);
        if (searchCondition != null)
          searchmark.addSearchCondition(searchCondition);
      } catch (NumberFormatException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (ParseException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private ISearchCondition processSearchCondition(Element conditionElement) throws ParseException {

    /* Search Specifier */
    Element specifierElement = conditionElement.getChild("searchspecifier", fRSSOwlNamespace);
    SearchSpecifier searchSpecifier = SearchSpecifier.values()[Integer.parseInt(specifierElement.getAttributeValue("id"))];

    /* Search Value */
    Element valueElement = conditionElement.getChild("searchvalue", fRSSOwlNamespace);
    Object value = getValue(valueElement, fRSSOwlNamespace);

    /* Search Field */
    Element fieldElement = conditionElement.getChild("searchfield", fRSSOwlNamespace);
    String fieldName = fieldElement.getAttributeValue("name");
    String entityName = fieldElement.getAttributeValue("entity");
    ISearchField searchField = Owl.getModelFactory().createSearchField(getFieldID(fieldName), entityName);

    /*
     * Guard against null (Location Conditions may potentially lead to NULL if
     * they are stale since they are not updated when locations change)
     */
    if (value != null)
      return Owl.getModelFactory().createSearchCondition(searchField, searchSpecifier, value);

    return null;
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

    List<?> locationElements = valueElement.getChildren("location", namespace);
    List<?> newsStateElements = valueElement.getChildren("newsstate", namespace);

    /* Treat set of Locations separately */
    if (locationElements.size() > 0) {
      List<Long> folderIds = new ArrayList<Long>(locationElements.size());
      List<Long> bookmarkIds = new ArrayList<Long>(locationElements.size());
      List<Long> newsbinIds = new ArrayList<Long>(locationElements.size());

      for (int i = 0; i < locationElements.size(); i++) {
        Element locationElement = (Element) locationElements.get(i);
        Long id = Long.parseLong(locationElement.getAttributeValue("value"));
        boolean isFolder = Boolean.parseBoolean(locationElement.getAttributeValue("isFolder"));
        boolean isBin = Boolean.parseBoolean(locationElement.getAttributeValue("isBin"));

        if (isFolder)
          folderIds.add(id);
        else if (isBin)
          newsbinIds.add(id);
        else
          bookmarkIds.add(id);
      }

      Long[][] result = new Long[3][];
      result[0] = folderIds.toArray(new Long[folderIds.size()]);
      result[1] = bookmarkIds.toArray(new Long[bookmarkIds.size()]);
      result[2] = newsbinIds.toArray(new Long[bookmarkIds.size()]);

      return result;
    }

    /* Treat set of News States separately */
    else if (newsStateElements.size() > 0) {
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

  private void processOutline(Element outline, IPersistable parent, List<IEntity> importedEntities) {
    IEntity type = null;
    Long id = null;
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

    /* RSSOwl Namespace Attributes */
    Attribute idAttribute = outline.getAttribute("id", fRSSOwlNamespace);
    if (idAttribute != null)
      id = Long.valueOf(idAttribute.getValue());

    boolean isSet = Boolean.parseBoolean(outline.getAttributeValue("isSet", fRSSOwlNamespace));

    /* Outline is a Folder */
    if (link == null && title != null) {
      type = Owl.getModelFactory().createFolder(null, isSet ? null : (IFolder) parent, title);

      /* Assign old ID value */
      if (id != null)
        type.setProperty(ID_KEY, id);

      if (isSet)
        importedEntities.add(type);
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
          feed = feedDao.save(feed);
        }

        /* Create the BookMark */
        FeedLinkReference feedLinkRef = new FeedLinkReference(uri);
        type = Owl.getModelFactory().createBookMark(null, (IFolder) parent, feedLinkRef, title != null ? title : link);

        /* Assign old ID value */
        if (id != null)
          type.setProperty(ID_KEY, id);
      }
    }

    /* In case this Outline Element did not represent a Folder */
    if (type == null || type instanceof IMark)
      return;

    /* Recursively Interpret Children */
    List<?> feedChildren = outline.getChildren();
    for (Iterator<?> iter = feedChildren.iterator(); iter.hasNext();) {
      Element child = (Element) iter.next();
      String name = child.getName().toLowerCase();

      /* Process Outline */
      if ("outline".equals(name)) //$NON-NLS-1$
        processOutline(child, type, importedEntities);

      /* Process Saved Search */
      else if ("savedsearch".equals(name))
        processSavedSearch(child, (IFolder) type);

      /* Process News Bin */
      else if ("newsbin".equals(name))
        processNewsBin(child, (IFolder) type);
    }
  }

  private void processLabel(Element labelElement, List<IEntity> importedEntities) {
    String id = labelElement.getAttributeValue("id");
    String name = labelElement.getAttributeValue("name");
    String order = labelElement.getAttributeValue("order");
    String color = labelElement.getAttributeValue("color");

    ILabel label = Owl.getModelFactory().createLabel(null, name);
    label.setColor(color);
    label.setOrder(Integer.parseInt(order));
    label.setProperty(ID_KEY, Long.valueOf(id));

    importedEntities.add(label);
  }

  private void processNewsBin(Element newsBinElement, IFolder folder) {
    String name = newsBinElement.getAttributeValue("name");

    /* RSSOwl Namespace Attributes */
    Long id = null;
    Attribute idAttribute = newsBinElement.getAttribute("id", fRSSOwlNamespace);
    if (idAttribute != null)
      id = Long.valueOf(idAttribute.getValue());

    INewsBin newsbin = Owl.getModelFactory().createNewsBin(null, folder, name);

    /* Assign old ID value */
    if (id != null)
      newsbin.setProperty(ID_KEY, id);
  }
}