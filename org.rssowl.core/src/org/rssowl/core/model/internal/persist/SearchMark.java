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

package org.rssowl.core.model.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.INewsGetter;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.search.ISearchCondition;
import org.rssowl.core.model.persist.search.ISearchHit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The SearchMark is acting like virtual folders in Thunderbird. The user is
 * defining some criterias, e.g. "mozilla" as part of a news-title, and all News
 * that matches this criteria will be related to this SearchMark.
 *
 * @author bpasero
 */
public class SearchMark extends Mark implements ISearchMark {
  private transient INewsGetter fNewsGetter;

  private List<ISearchCondition> fSearchConditions;
  private boolean fMatchAllConditions;

  /**
   * Creates a new Element of the type SearchMark. A SearchMark is only visually
   * representated in case it was added to a Folder. Make sure to add it to a
   * Folder using <code>Folder#addMark(Mark)</code>
   *
   * @param id The unique ID of this SearchMark.
   * @param folder The Folder this SearchMark belongs to.
   * @param name The Name of this SearchMark.
   */
  public SearchMark(Long id, IFolder folder, String name) {
    super(id, folder, name);
    fSearchConditions = new ArrayList<ISearchCondition>();
  }

  /**
   * Default constructor for deserialization
   */
  protected SearchMark() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#setNewsGetter(org.rssowl.core.model.types.INewsGetter)
   */
  public void setNewsGetter(INewsGetter newsGetter) {
    fNewsGetter = newsGetter;
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#getMatchingNews()
   */
  public List<ISearchHit<INews>> getMatchingNews() {
    //TODO Consider some sort of caching
    return fNewsGetter.getNews();
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#addSearchCondition(org.rssowl.core.model.reference.SearchConditionReference)
   */
  public void addSearchCondition(ISearchCondition searchCondition) {
    Assert.isNotNull(searchCondition, "Exception adding NULL as Search Condition into SearchMark"); //$NON-NLS-1$
    fSearchConditions.add(searchCondition);
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#removeSearchCondition(org.rssowl.core.model.search.ISearchCondition)
   */
  public boolean removeSearchCondition(ISearchCondition searchCondition) {
    return fSearchConditions.remove(searchCondition);
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#getSearchConditions()
   */
  public List<ISearchCondition> getSearchConditions() {
    return Collections.unmodifiableList(fSearchConditions);
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#requiresAllConditions()
   */
  public boolean matchAllConditions() {
    return fMatchAllConditions;
  }

  /*
   * @see org.rssowl.core.model.types.ISearchMark#setRequireAllConditions(boolean)
   */
  public void setMatchAllConditions(boolean requiresAllConditions) {
    fMatchAllConditions = requiresAllConditions;
  }

  /**
   * Compare the given type with this type for identity.
   *
   * @param searchMark to be compared.
   * @return whether this object and <code>searchMark</code> are identical. It
   * compares all the fields.
   */
  public boolean isIdentical(ISearchMark searchMark) {
    if (this == searchMark)
      return true;

    if (searchMark instanceof SearchMark == false)
      return false;

    SearchMark s = (SearchMark) searchMark;

    return getId() == s.getId() && (getFolder() == null ? s.getFolder() == null : getFolder().equals(s.getFolder())) &&
      (fSearchConditions == null ? s.fSearchConditions == null : fSearchConditions.equals(s.fSearchConditions)) &&
      (getLastVisitDate() == null ? s.getLastVisitDate() == null : getLastVisitDate().equals(s.getLastVisitDate())) &&
      getPopularity() == s.getPopularity() && fMatchAllConditions == s.matchAllConditions() &&
      (getProperties() == null ? s.getProperties() == null : getProperties().equals(s.getProperties()));
  }


  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  @Override
  @SuppressWarnings("nls")
  public String toLongString() {
    return super.toString() + "Search Conditions = " + fSearchConditions.toString() + ")";
  }
}