
package org.rssowl.core.internal.persist.search;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ijuma
 * @author bpasero
 */
public class NewsDocument extends SearchDocument<INews> {

  /* State ID as String */
  static final String STATE_ID_TEXT = String.valueOf(INews.STATE);

  /**
   * @param type
   */
  public NewsDocument(INews type) {
    super(type);
  }

  /*
   * @see org.rssowl.contrib.search.internal.SearchDocument#addFields()
   */
  @Override
  public boolean addFields() {
    INews news = getType();
    List<Field> fields = new ArrayList<Field>();

    /* Add ID */
    addField(fields, createDocumentIDField());

    /* Add textual content */
    addField(fields, createHTMLField(INews.TITLE, news.getTitle()));
    addField(fields, createHTMLField(INews.DESCRIPTION, news.getDescription()));

    /* Add URIs */
    addField(fields, createURIField(INews.LINK, news.getLink(), Store.NO, Index.UN_TOKENIZED));
    addField(fields, createURIField(INews.FEED, news.getFeedReference().getLink(), Store.NO, Index.UN_TOKENIZED));

    /* Add Dates */
    addField(fields, createDateField(INews.RECEIVE_DATE, news.getReceiveDate(), Store.NO));
    addField(fields, createDateField(INews.PUBLISH_DATE, news.getPublishDate(), Store.NO));
    addField(fields, createDateField(INews.MODIFIED_DATE, news.getModifiedDate(), Store.NO));
    addField(fields, createDateField(INews.AGE_IN_DAYS, DateUtils.getRecentDate(news), Store.NO));

    /* Add States (actually store INews.State in Index) */
    addField(fields, createEnumField(INews.STATE, news.getState(), Store.YES));
    addField(fields, createBooleanField(INews.IS_FLAGGED, news.isFlagged(), Store.NO));
    addField(fields, createLongField(INews.RATING, news.getRating(), Store.NO));

    /* Add Label */
    if (news.getLabel() != null)
      addField(fields, createStringField(INews.LABEL, news.getLabel().getName().toLowerCase(), Store.NO, Index.UN_TOKENIZED));

    /* Add Source */
    if (news.getSource() != null) {
      if (news.getSource().getLink() != null)
        addField(fields, createURIField(INews.SOURCE, news.getSource().getLink(), Store.NO, Index.UN_TOKENIZED));
      else if (news.getSource().getName() != null)
        addField(fields, createStringField(INews.SOURCE, news.getSource().getName(), Store.NO, Index.TOKENIZED));
    }

    /* Add Author */
    addField(fields, createPersonField(INews.AUTHOR, news.getAuthor(), Store.NO, Index.TOKENIZED));

    /* Add Categories */
    addField(fields, createCategoriesField(INews.CATEGORIES, news.getCategories(), Store.NO, Index.TOKENIZED));

    /* Add Attachments */
    addField(fields, createBooleanField(INews.HAS_ATTACHMENTS, !news.getAttachments().isEmpty(), Store.NO));
    addField(fields, createAttachmentsField(INews.ATTACHMENTS_CONTENT, news.getAttachments(), Store.NO, Index.TOKENIZED));

    return addFieldsToDocument(fields);
  }
}