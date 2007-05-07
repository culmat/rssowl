
package org.rssowl.core.internal.persist.search;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.util.HTMLStripReader;

import java.io.StringReader;
import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * TODO Consider adding the same Field multiple times (see
 * createCategoriesField()) instead of appending the string to a single Field.
 * </p>
 *
 * @author ijuma
 * @author bpasero
 * @param <T>
 */
public abstract class SearchDocument<T extends IEntity> {

  /** The value of an Entitie's ID as integer */
  protected static final int ENTITY_ID = -2;

  /** The value of an Entitie's ID as String */
  protected static final String ENTITY_ID_TEXT = String.valueOf(ENTITY_ID);

  private final Document fDocument;
  private final T fType;

  public SearchDocument(T type) {
    Assert.isLegal(type != null);
    fType = type;
    fDocument = new Document();
  }

  public final T getType() {
    return fType;
  }

  public final Document getDocument() {
    return fDocument;
  }

  public abstract boolean addFields();

  protected boolean addFieldsToDocument(List<Field> fields) {
    if (fields.size() == 0)
      return false;

    for (Field field : fields) {
      getDocument().add(field);
    }

    return true;
  }

  protected boolean addField(List<Field> fields, Field field) {
    if (field == null)
      return false;

    fields.add(field);
    return true;
  }

  /**
   * Creates a new <code>Field</code> representing the entities ID this
   * Document wraps around.
   *
   * @return
   */
  protected Field createDocumentIDField() {
    return new Field(ENTITY_ID_TEXT, String.valueOf(getType().getId()), Store.YES, Index.UN_TOKENIZED);
  }

  /**
   * Creates a new <code>Field</code> from the given ID value.
   *
   * @return
   */
  protected Field createIDField(int fieldConstant, IEntity entity, Store store) {
    return new Field(String.valueOf(fieldConstant), String.valueOf(entity.getId()), store, Index.UN_TOKENIZED);
  }

  /**
   * Creates a new <code>Field</code> from the given long value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @return
   */
  protected Field createLongField(int fieldConstant, long value, Store store) {
    String valueText = NumberTools.longToString(value);
    return new Field(String.valueOf(fieldConstant), valueText, store, Index.UN_TOKENIZED);
  }

  /**
   * Creates a new <code>Field</code> from the given boolean value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @return
   */
  protected Field createBooleanField(int fieldConstant, boolean value, Store store) {
    return new Field(String.valueOf(fieldConstant), String.valueOf(value), store, Index.UN_TOKENIZED);
  }

  /**
   * Creates a new <code>Field</code> from the given Date value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @return
   */
  protected Field createDateField(int fieldConstant, Date value, Store store) {
    if (value == null)
      return null;

    String valueText = DateTools.dateToString(value, Resolution.DAY);
    return new Field(String.valueOf(fieldConstant), valueText, store, Index.UN_TOKENIZED);
  }

  /**
   * Creates a new <code>Field</code> from the given URI value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @param index
   * @return
   */
  protected Field createURIField(int fieldConstant, URI value, Store store, Index index) {
    if (value == null)
      return null;

    return new Field(String.valueOf(fieldConstant), value.toString(), store, index);
  }

  /**
   * Creates a new <code>Field</code> from the given String value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @param index
   * @return
   */
  protected Field createStringField(int fieldConstant, String value, Store store, Index index) {
    if (value == null)
      return null;

    return new Field(String.valueOf(fieldConstant), value, store, index);
  }

  /**
   * Creates a new <code>Field</code> from the given String value containing
   * HTML. The HTML will be stripped from the field and entities replaced. The
   * value is <em>not stored</em>and <em>tokenized indexed</em>.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @param index
   * @return
   */
  protected Field createHTMLField(int fieldConstant, String value) {
    if (value == null)
      return null;

    return new Field(String.valueOf(fieldConstant), new HTMLStripReader(new StringReader(value)));
  }

  /**
   * Creates a new <code>Field</code> from the given enum value.
   *
   * @param fieldConstant
   * @param value
   * @param store
   * @return
   */
  protected Field createEnumField(int fieldConstant, Enum< ? > value, Store store) {
    if (value == null)
      return null;

    return new Field(String.valueOf(fieldConstant), String.valueOf(value.ordinal()), store, Index.UN_TOKENIZED);
  }

  /**
   * @param fieldConstant
   * @param person
   * @param store
   * @param index
   * @return
   */
  protected Field createPersonField(int fieldConstant, IPerson person, Store store, Index index) {
    if (person == null)
      return null;

    /* Add Name if present */
    if (person.getName() != null)
      return createStringField(fieldConstant, person.getName(), store, index);

    /* Add EMail if present */
    else if (person.getEmail() != null)
      return createURIField(fieldConstant, person.getEmail(), store, Index.UN_TOKENIZED);

    return null;
  }

  protected Field createCategoriesField(int fieldConstant, List<ICategory> categories, Store store, Index index) {
    if (categories == null || categories.size() == 0)
      return null;

    /* Convert categories to a single String */
    StringBuilder categoryValues = new StringBuilder();
    for (ICategory category : categories) {
      if (category.getName() != null)
        categoryValues.append(category.getName()).append('\n');
    }

    if (categoryValues.length() > 0)
      return createStringField(fieldConstant, categoryValues.toString(), store, index);

    return null;
  }

  protected Field createAttachmentsField(int fieldConstant, List<IAttachment> attachments, Store store, Index index) {
    if (attachments == null || attachments.size() == 0)
      return null;

    /* Convert attachments to a single String */
    StringBuilder attachmentValues = new StringBuilder();
    for (IAttachment attachment : attachments) {
      if (attachment.getLink() != null)
        attachmentValues.append(attachment.getLink()).append(" ");

      if (attachment.getType() != null)
        attachmentValues.append(attachment.getType()).append(" ");
    }

    if (attachmentValues.length() > 0)
      return createStringField(fieldConstant, attachmentValues.toString(), store, index);

    return null;
  }
}