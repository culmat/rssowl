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

import static org.rssowl.core.internal.interpreter.OPMLConstants.RSSOWL_NS;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.interpreter.OPMLConstants.Attributes;
import org.rssowl.core.internal.interpreter.OPMLConstants.Tags;
import org.rssowl.core.interpreter.ITypeExporter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Implementation of {@link ITypeExporter} for the OPML XML format.
 *
 * @author bpasero
 */
public class OPMLExporter implements ITypeExporter {

  /* Default Encoding */
  private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$

  /*
   * @see org.rssowl.core.interpreter.ITypeExporter#exportTo(java.io.File,
   * java.util.Collection, java.util.EnumSet)
   */
  public void exportTo(File destination, Collection<? extends IFolderChild> elements, EnumSet<Options> options) throws InterpreterException {
    Format format = Format.getPrettyFormat();
    format.setEncoding(UTF_8);
    XMLOutputter output = new XMLOutputter(format);
    DateFormat dateFormat = DateFormat.getDateInstance();

    Document document = new Document();
    Element root = new Element(Tags.OPML.get());
    root.setAttribute(Attributes.VERSION.get(), "1.1");
    root.addNamespaceDeclaration(RSSOWL_NS);
    document.setRootElement(root);

    Element body = new Element(Tags.BODY.get());
    root.addContent(body);

    boolean exportPreferences = (options != null && options.contains(Options.EXPORT_PREFERENCES));

    /* Export Folder Childs */
    if (elements != null && !elements.isEmpty())
      exportFolderChilds(body, elements, exportPreferences, dateFormat);

    /* Export Labels */
    if (options != null && options.contains(Options.EXPORT_LABELS))
      exportLabels(body);

    /* Export Filters */
    if (options != null && options.contains(Options.EXPORT_FILTERS))
      exportFilters(body, dateFormat);

    /* Export Preferences */
    if (exportPreferences)
      exportPreferences(body);

    /* Write to File */
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(destination);
      output.output(document, out);
      out.close();
    } catch (FileNotFoundException e) {
      throw new InterpreterException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } catch (IOException e) {
      throw new InterpreterException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          throw new InterpreterException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
        }
      }
    }
  }

  private void exportFolderChilds(Element parent, Collection<? extends IFolderChild> childs, boolean exportPreferences, DateFormat df) {
    for (IFolderChild child : childs) {

      /* Export Folder */
      if (child instanceof IFolder)
        exportFolder(parent, (IFolder) child, exportPreferences, df);

      /* Export Bookmark, Search or Bin */
      else if (child instanceof IMark)
        exportMark(parent, (IMark) child, exportPreferences, df);
    }
  }

  private void exportFolder(Element parent, IFolder folder, boolean exportPreferences, DateFormat df) {
    String name = folder.getName();

    Element folderElement = new Element(Tags.OUTLINE.get());
    folderElement.setAttribute(Attributes.TEXT.get(), name);
    folderElement.setAttribute(Attributes.IS_SET.get(), String.valueOf(folder.getParent() == null), RSSOWL_NS);
    folderElement.setAttribute(Attributes.ID.get(), String.valueOf(folder.getId()), RSSOWL_NS);
    parent.addContent(folderElement);

    /* Export Preferences if set */
    if (exportPreferences)
      exportPreferences(folderElement, folder);

    exportFolderChilds(folderElement, folder.getChildren(), exportPreferences, df);
  }

  private void exportPreferences(Element parent, IFolderChild child) {
    Map<String, Serializable> properties = child.getProperties();
    if (properties != null) {
      Set<Entry<String, Serializable>> entries = properties.entrySet();
      for (Entry<String, Serializable> entry : entries) {
        if (StringUtils.isSet(entry.getKey()) && entry.getValue() != null) {
          Element prefElement = new Element(Tags.PREFERENCE.get(), RSSOWL_NS);
          prefElement.setAttribute(Attributes.ID.get(), entry.getKey());
          prefElement.setAttribute(Attributes.VALUE.get(), entry.getValue().toString());
          parent.addContent(prefElement);
        }
      }
    }
  }

  private void exportMark(Element parent, IMark mark, boolean exportPreferences, DateFormat df) {
    String name = mark.getName();
    Element element = null;

    /* Export BookMark */
    if (mark instanceof IBookMark) {
      String link = ((IBookMark) mark).getFeedLinkReference().getLinkAsText();

      element = new Element(Tags.OUTLINE.get());
      element.setAttribute(Attributes.TEXT.get(), name);
      element.setAttribute(Attributes.XML_URL.get(), link);
      element.setAttribute(Attributes.ID.get(), String.valueOf(mark.getId()), RSSOWL_NS);
      parent.addContent(element);
    }

    /* Export SearchMark */
    else if (mark instanceof ISearchMark) {
      ISearchMark searchMark = (ISearchMark) mark;
      List<ISearchCondition> conditions = searchMark.getSearchConditions();

      element = new Element(Tags.SAVED_SEARCH.get(), RSSOWL_NS);
      element.setAttribute(Attributes.NAME.get(), name);
      element.setAttribute(Attributes.MATCH_ALL_CONDITIONS.get(), String.valueOf(searchMark.matchAllConditions()));
      element.setAttribute(Attributes.ID.get(), String.valueOf(mark.getId()), RSSOWL_NS);
      parent.addContent(element);

      for (ISearchCondition condition : conditions) {
        Element conditionElement = new Element(Tags.SEARCH_CONDITION.get(), RSSOWL_NS);
        element.addContent(conditionElement);

        fillElement(conditionElement, condition, df);
      }
    }

    /* Export Newsbin */
    else if (mark instanceof INewsBin) {
      element = new Element(Tags.BIN.get(), RSSOWL_NS);
      element.setAttribute(Attributes.NAME.get(), name);
      element.setAttribute(Attributes.ID.get(), String.valueOf(mark.getId()), RSSOWL_NS);
      parent.addContent(element);
    }

    /* Export Preferences if set */
    if (element != null && exportPreferences)
      exportPreferences(element, mark);
  }

  private void exportFilters(Element body, DateFormat df) {
    Collection<ISearchFilter> filters = DynamicDAO.loadAll(ISearchFilter.class);
    for (ISearchFilter filter : filters) {
      String name = filter.getName();
      int order = filter.getOrder();
      boolean isEnabled = filter.isEnabled();
      boolean matchAllNews = filter.matchAllNews();

      Element filterElement = new Element(Tags.FILTER.get(), RSSOWL_NS);
      filterElement.setAttribute(Attributes.NAME.get(), name);
      filterElement.setAttribute(Attributes.ORDER.get(), String.valueOf(order));
      filterElement.setAttribute(Attributes.ENABLED.get(), String.valueOf(isEnabled));
      filterElement.setAttribute(Attributes.MATCH_ALL_NEWS.get(), String.valueOf(matchAllNews));
      body.addContent(filterElement);

      /* Export Search if provided */
      ISearch search = filter.getSearch();
      if (search != null) {
        List<ISearchCondition> conditions = search.getSearchConditions();

        Element searchElement = new Element(Tags.SEARCH.get(), RSSOWL_NS);
        searchElement.setAttribute(Attributes.MATCH_ALL_CONDITIONS.get(), String.valueOf(search.matchAllConditions()));
        filterElement.addContent(searchElement);

        for (ISearchCondition condition : conditions) {
          Element conditionElement = new Element(Tags.SEARCH_CONDITION.get(), RSSOWL_NS);
          searchElement.addContent(conditionElement);

          fillElement(conditionElement, condition, df);
        }
      }

      /* Export Actions */
      List<IFilterAction> actions = filter.getActions();
      for (IFilterAction action : actions) {
        String actionId = action.getActionId();
        String data = toString(action.getData());

        Element actionElement = new Element(Tags.ACTION.get(), RSSOWL_NS);
        actionElement.setAttribute(Attributes.ID.get(), actionId);
        if (data != null)
          actionElement.setAttribute(Attributes.DATA.get(), data);

        filterElement.addContent(actionElement);
      }
    }
  }

  private void fillElement(Element conditionElement, ISearchCondition condition, DateFormat df) {

    /* Search Specifier */
    Element searchSpecifier = new Element(Tags.SPECIFIER.get(), RSSOWL_NS);
    searchSpecifier.setAttribute(Attributes.ID.get(), String.valueOf(condition.getSpecifier().ordinal()));
    conditionElement.addContent(searchSpecifier);

    /* Search Condition: Location */
    if (condition.getValue() instanceof Long[][]) {
      List<IFolderChild> locations = CoreUtils.toEntities((Long[][]) condition.getValue());

      Element searchValue = new Element(Tags.SEARCH_VALUE.get(), RSSOWL_NS);
      searchValue.setAttribute(Attributes.TYPE.get(), String.valueOf(condition.getField().getSearchValueType().getId()));
      conditionElement.addContent(searchValue);

      for (IFolderChild child : locations) {
        boolean isFolder = (child instanceof IFolder);
        boolean isNewsbin = (child instanceof INewsBin);

        Element location = new Element(Tags.LOCATION.get(), RSSOWL_NS);
        location.setAttribute(Attributes.IS_BIN.get(), String.valueOf(isNewsbin));
        location.setAttribute(Attributes.IS_FOLDER.get(), String.valueOf(isFolder));
        location.setAttribute(Attributes.VALUE.get(), String.valueOf(child.getId()));
        searchValue.addContent(location);
      }
    }

    /* Single Value */
    else if (!EnumSet.class.isAssignableFrom(condition.getValue().getClass())) {
      Element searchValue = new Element(Tags.SEARCH_VALUE.get(), RSSOWL_NS);
      searchValue.setAttribute(Attributes.TYPE.get(), String.valueOf(condition.getField().getSearchValueType().getId()));
      searchValue.setAttribute(Attributes.VALUE.get(), getValueString(df, condition));
      conditionElement.addContent(searchValue);
    }

    /* Multiple Values */
    else {
      EnumSet<?> values = ((EnumSet<?>) condition.getValue());

      Element searchValue = new Element(Tags.SEARCH_VALUE.get(), RSSOWL_NS);
      searchValue.setAttribute(Attributes.TYPE.get(), String.valueOf(condition.getField().getSearchValueType().getId()));
      conditionElement.addContent(searchValue);

      for (Enum<?> enumValue : values) {
        Element state = new Element(Tags.STATE.get(), RSSOWL_NS);
        state.setAttribute(Attributes.VALUE.get(), String.valueOf(enumValue.ordinal()));
        searchValue.addContent(state);
      }
    }

    /* Search Field */
    Element field = new Element(Tags.SEARCH_FIELD.get(), RSSOWL_NS);
    field.setAttribute(Attributes.NAME.get(), getSearchFieldName(condition.getField().getId()));
    field.setAttribute(Attributes.ENTITY.get(), condition.getField().getEntityName());
    conditionElement.addContent(field);
  }

  private String getSearchFieldName(int fieldId) {
    switch (fieldId) {
      case (IEntity.ALL_FIELDS):
        return "allFields";
      case (INews.TITLE):
        return "title";
      case (INews.LINK):
        return "link";
      case (INews.DESCRIPTION):
        return "description";
      case (INews.PUBLISH_DATE):
        return "publishDate";
      case (INews.MODIFIED_DATE):
        return "modifiedDate";
      case (INews.RECEIVE_DATE):
        return "receiveDate";
      case (INews.AUTHOR):
        return "author";
      case (INews.COMMENTS):
        return "comments";
      case (INews.GUID):
        return "guid";
      case (INews.SOURCE):
        return "source";
      case (INews.HAS_ATTACHMENTS):
        return "hasAttachments";
      case (INews.ATTACHMENTS_CONTENT):
        return "attachments";
      case (INews.CATEGORIES):
        return "categories";
      case (INews.IS_FLAGGED):
        return "isFlagged";
      case (INews.STATE):
        return "state";
      case (INews.LABEL):
        return "label";
      case (INews.RATING):
        return "rating";
      case (INews.FEED):
        return "feed";
      case (INews.AGE_IN_DAYS):
        return "ageInDays";
      case (INews.LOCATION):
        return "location";
      default:
        return "allFields";
    }
  }

  private String toString(Object data) {
    if (data == null)
      return null;

    if (data instanceof String)
      return (String) data;

    if (data instanceof Long)
      return String.valueOf(data);

    if (data instanceof Long[]) {
      Long[] value = (Long[]) data;
      StringBuilder builder = new StringBuilder();
      for (Long val : value) {
        builder.append(val).append(",");
      }

      if (value.length > 0)
        builder.delete(builder.length() - 1, builder.length());

      return builder.toString();
    }

    return null;
  }

  private String getValueString(DateFormat df, ISearchCondition condition) {
    if (condition.getValue() instanceof Date)
      return df.format((Date) condition.getValue());

    return condition.getValue().toString();
  }

  private void exportLabels(Element body) {
    Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
    for (ILabel label : labels) {
      Long id = label.getId();
      String name = label.getName();
      String color = label.getColor();
      int order = label.getOrder();

      Element labelElement = new Element(Tags.LABEL.get(), RSSOWL_NS);
      labelElement.setAttribute(Attributes.ID.get(), String.valueOf(id));
      labelElement.setAttribute(Attributes.NAME.get(), name);
      labelElement.setAttribute(Attributes.ORDER.get(), String.valueOf(order));
      labelElement.setAttribute(Attributes.COLOR.get(), color);

      body.addContent(labelElement);
    }
  }

  private void exportPreferences(@SuppressWarnings("unused") Element body) {
  //TODO Implement export of all Eclipse and Global preferences.
  }
}