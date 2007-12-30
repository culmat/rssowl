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

package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link ISearchMark}.
 *
 * @author bpasero
 * @see ISearchMark
 */
//TODO Methods not thread-safe
public class SearchMark extends Mark implements ISearchMark {
  private List<ISearchCondition> fSearchConditions;
  private boolean fMatchAllConditions;

  private transient long[] fMatchingReadNews = new long[0];
  private transient long[] fMatchingUnreadUpdatedNews = new long[0];
  private transient long[] fMatchingNewNews = new long[0];

  /**
   * Creates a new Element of the type SearchMark. A SearchMark is only visually
   * represented in case it was added to a Folder. Make sure to add it to a
   * Folder using <code>Folder#addMark(Mark)</code>
   *
   * @param id The unique ID of this SearchMark.
   * @param folder The Folder this SearchMark belongs to.
   * @param name The Name of this SearchMark.
   */
  public SearchMark(Long id, IFolder folder, String name) {
    super(id, folder, name);
    fSearchConditions = new ArrayList<ISearchCondition>(5);
  }

  /**
   * Default constructor for deserialization
   */
  protected SearchMark() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.persist.ISearchMark#setResult(java.util.List)
   */
  public Pair<Boolean, Boolean> setResult(Map<INews.State, List<NewsReference>> results) {
    Assert.isNotNull(results, "results");

    boolean changed = false;
    boolean isNewNewsAdded = false;

    /* For each Result */
    for (Map.Entry<INews.State, List<NewsReference>> result : results.entrySet()) {
      List<NewsReference> news = result.getValue();
      INews.State state = result.getKey();

      Assert.isNotNull(news, "news");
      Assert.isNotNull(state, "state");

      long[] bucket = new long[news.size()];

      /* Fill Bucket */
      for (int i = 0; i < news.size(); i++) {
        bucket[i] = news.get(i).getId();
      }

      switch (state) {

        /* Read News */
        case READ:
          if (!changed)
            changed = !Arrays.equals(fMatchingReadNews, bucket);

          fMatchingReadNews = bucket;
          break;

        /* Unread or Updated News */
        case UNREAD:
        case UPDATED:
          if (!changed)
            changed = !Arrays.equals(fMatchingUnreadUpdatedNews, bucket);

          fMatchingUnreadUpdatedNews = bucket;
          break;

        /* New News */
        case NEW:

          /* Check for added *new* News */
          for (long lVal : bucket) {
            if (Arrays.binarySearch(fMatchingNewNews, lVal) < 0) {
              isNewNewsAdded = true;
              break;
            }
          }

          /* Also use for changed-flag */
          if (isNewNewsAdded)
            changed = true;

          /*
           * We need to sort the array to be able to do the binary search
           * in future iterations. However, since we assign the array to
           * fMatchingNewNews, we also need to sort the array in case we do
           * Arrays.equals.
           */
          Arrays.sort(bucket);
          if (!changed)
            changed = !Arrays.equals(fMatchingNewNews, bucket);

          fMatchingNewNews = bucket;
          break;
      }
    }

    return Pair.create(changed, isNewNewsAdded);
  }

  /*
   * @see org.rssowl.core.persist.ISearchMark#getMatchingNews()
   */
  public List<NewsReference> getResult() {
    return getResult(INews.State.getVisible());
  }

  /*
   * @see org.rssowl.core.persist.ISearchMark#getMatchingNews(java.util.EnumSet)
   */
  public List<NewsReference> getResult(Set<INews.State> states) {
    List<NewsReference> result = new ArrayList<NewsReference>(getResultCount(states));

    /* Add Read News */
    if (states.contains(INews.State.READ)) {
      for (long id : fMatchingReadNews)
        result.add(new NewsReference(id));
    }

    /* Add Unread News */
    if (states.contains(INews.State.UNREAD) || states.contains(INews.State.UPDATED)) {
      for (long id : fMatchingUnreadUpdatedNews)
        result.add(new NewsReference(id));
    }

    /* Add New News */
    if (states.contains(INews.State.NEW)) {
      for (long id : fMatchingNewNews)
        result.add(new NewsReference(id));
    }

    return result;
  }

  /*
   * @see org.rssowl.core.persist.ISearchMark#getMatchingNewsCount(java.util.Set)
   */
  public int getResultCount(Set<INews.State> states) {
    Assert.isNotNull(states, "states");
    int count = 0;

    /* Read News */
    if (states.contains(INews.State.READ))
      count += fMatchingReadNews.length;

    /* Unread or Updated News */
    if (states.contains(INews.State.UNREAD) || states.contains(INews.State.UPDATED))
      count += fMatchingUnreadUpdatedNews.length;

    /* New News */
    if (states.contains(INews.State.NEW))
      count += fMatchingNewNews.length;

    return count;
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

    return getId() == s.getId() && (getParent() == null ? s.getParent() == null : getParent().equals(s.getParent())) && (fSearchConditions == null ? s.fSearchConditions == null : fSearchConditions.equals(s.fSearchConditions)) && (getLastVisitDate() == null ? s.getLastVisitDate() == null : getLastVisitDate().equals(s.getLastVisitDate())) && getPopularity() == s.getPopularity() && fMatchAllConditions == s.matchAllConditions() && (getProperties() == null ? s.getProperties() == null : getProperties().equals(s.getProperties()));
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