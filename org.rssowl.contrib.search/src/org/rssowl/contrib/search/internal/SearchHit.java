/*
 * Created on 16.05.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

package org.rssowl.contrib.search.internal;

import org.rssowl.core.model.persist.search.ISearchHit;

/**
 * TODO API is in Progress.
 *
 * @author ijuma
 * @author bpasero
 * @param <T>
 */
public class SearchHit<T> implements ISearchHit<T> {
  private T fResult;
  private float fRelevance;

  SearchHit(T result, float relevance) {
    fResult = result;
    fRelevance = relevance;
  }

  /*
   * @see org.rssowl.core.model.search.ISearchHit#getRelevance()
   */
  public float getRelevance() {
    return fRelevance;
  }

  /*
   * @see org.rssowl.core.model.search.ISearchHit#getResult()
   */
  public T getResult() {
    return fResult;
  }
}