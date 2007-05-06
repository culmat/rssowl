/*
 * Created on 16.05.2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

package org.rssowl.contrib.internal.search;

import org.rssowl.core.util.ISearchHit;

import java.util.Map;

/**
 * Instances of <code>ISearchHit</code> are the result of running a query in
 * the <code>IModelSearch</code>. Every hit provides the result identified by
 * <code>T</code>, the relevance score and allows to receive additional data
 * in a generic way.
 *
 * @author ijuma
 * @author bpasero
 * @param <T> The type of Object this Hit provides.
 */
public class SearchHit<T> implements ISearchHit<T> {
  private final T fResult;
  private final float fRelevance;
  private final Map<?, ?> fData;

  SearchHit(T result, float relevance, Map<?, ?> data) {
    fResult = result;
    fRelevance = relevance;
    fData = data;
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

  /*
   * @see org.rssowl.core.util.ISearchHit#getData(java.lang.Object)
   */
  public Object getData(Object key) {
    return fData != null ? fData.get(key) : null;
  }
}