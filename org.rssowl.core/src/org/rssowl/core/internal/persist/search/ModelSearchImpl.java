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

package org.rssowl.core.internal.persist.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.EntityIdsByEventType;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.IndexListener;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.SearchHit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The central interface for searching types from the persistence layer. The
 * implementation is contributable via extension-point mechanism.
 *
 * @author ijuma
 * @author bpasero
 */
public class ModelSearchImpl implements IModelSearch {

  /* Cached News States */
  private static final INews.State[] NEWS_STATES = INews.State.values();

  /* An increased clauses count to set in case of a MaxClouseCountException */
  private static final int MAX_CLAUSE_COUNT = 65536;

  private volatile IndexSearcher fSearcher;
  private volatile Indexer fIndexer;
  private volatile Directory fDirectory;
  private final List<IndexListener> fIndexListeners = new CopyOnWriteArrayList<IndexListener>();
  private final Map<IndexSearcher, AtomicInteger> fSearchers = new ConcurrentHashMap<IndexSearcher, AtomicInteger>(3, 0.75f, 1);

  /*
   * @see org.rssowl.core.model.search.IModelSearch#startup()
   */
  public void startup() throws PersistenceException {
    try {
      if (fDirectory == null) {
        String path = Activator.getDefault().getStateLocation().toOSString();
        LockFactory lockFactory = new NativeFSLockFactory(path);
        fDirectory = FSDirectory.getDirectory(path, lockFactory);
      }

      if (fIndexer == null)
        fIndexer = new Indexer(this, fDirectory);

      fIndexer.initIfNecessary();

      synchronized (this) {
        if (fSearcher == null)
          fSearcher = createIndexSearcher();
      }
    } catch (IOException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#shutdown()
   */
  public void shutdown(boolean emergency) throws PersistenceException {
    try {
      /*
       * Close fIndexer first because it's more important (reduces the chance of
       * a corrupt index). Can be null if exception thrown during start-up
       */
      if (fIndexer != null)
        fIndexer.shutdown(emergency);

      /*
       * We don't bother to close searchers if it's an emergency. They will be
       * released when the process exits.
       */
      if (emergency)
        return;

      synchronized (this) {
        /* We first close all the searchers whose refCount is 0 */
        for (Map.Entry<IndexSearcher, AtomicInteger> mapEntry : fSearchers.entrySet()) {
          if (mapEntry.getValue().get() == 0)
            dispose(mapEntry.getKey());
        }
        while (!fSearchers.isEmpty()) {
          try {
            /*
             * We sleep with a lock held because the Threads that we're waiting
             * to make progress don't acquire a lock
             */
            Thread.sleep(50);
          } catch (InterruptedException e) {
            /* If interrupted, we just leave the rest of the searchers open */
            return;
          }
          /* Try again for the ones that are left */
          for (Map.Entry<IndexSearcher, AtomicInteger> mapEntry : fSearchers.entrySet()) {
            if (mapEntry.getValue().get() == 0)
              dispose(mapEntry.getKey());
          }
        }
        fSearcher = null;
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private BooleanClause createIsCopyTermQuery(boolean copy) {
    String field = String.valueOf(INews.PARENT_ID);
    TermQuery termQuery = new TermQuery(new Term(field, NumberTools.longToString(0)));
    Occur occur = copy ? Occur.MUST_NOT : Occur.MUST;
    return new BooleanClause(termQuery, occur);
  }

  private static final class SimpleHitCollector extends HitCollector {

    private final IndexSearcher fSearcher;
    private final List<NewsReference> fResultList;

    SimpleHitCollector(IndexSearcher searcher, List<NewsReference> resultList) {
      fSearcher = searcher;
      fResultList = resultList;
    }

    @Override
    public void collect(int doc, float score) {
      try {
        Document document = fSearcher.doc(doc);

        /* Receive Stored Fields */
        long newsId = Long.parseLong(document.get(SearchDocument.ENTITY_ID_TEXT));

        /* Add to List */
        fResultList.add(new NewsReference(newsId));
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  /**
   * @param guids the List of {@link IGuid} to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search and
   * grouped by {@link IGuid}.
   */
  public Map<IGuid, List<NewsReference>> searchNewsByGuids(List<IGuid> guids, boolean copy) {
    Map<IGuid, List<NewsReference>> linkToRefs = new HashMap<IGuid, List<NewsReference>>(guids.size());
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      for (IGuid guid : guids) {
        BooleanQuery query = createGuidQuery(guid, copy);
        List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
        if (!newsRefs.isEmpty())
          linkToRefs.put(guid, newsRefs);
      }
      return linkToRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  /**
   * @param links The Links to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search and
   * grouped by the {@link URI}.
   */
  public Map<URI, List<NewsReference>> searchNewsByLinks(List<URI> links, boolean copy) {
    Map<URI, List<NewsReference>> linkToRefs = new HashMap<URI, List<NewsReference>>(links.size());
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      for (URI link : links) {
        BooleanQuery query = createNewsByLinkBooleanQuery(link, copy);
        List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
        if (!newsRefs.isEmpty())
          linkToRefs.put(link, newsRefs);
      }
      return linkToRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  /**
   * @param link The Link to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search.
   */
  public List<NewsReference> searchNewsByLink(URI link, boolean copy) {
    Assert.isNotNull(link, "link"); //$NON-NLS-1$
    BooleanQuery query = createNewsByLinkBooleanQuery(link, copy);
    return simpleSearch(query);
  }

  private BooleanQuery createNewsByLinkBooleanQuery(URI link, boolean copy) {
    BooleanQuery query = new BooleanQuery(true);
    query.add(new TermQuery(new Term(String.valueOf(INews.LINK), link.toString().toLowerCase())), Occur.MUST);
    query.add(createIsCopyTermQuery(copy));
    return query;
  }

  /**
   * @param guid the {@link IGuid} to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search.
   */
  public List<NewsReference> searchNewsByGuid(IGuid guid, boolean copy) {
    Assert.isNotNull(guid, "guid"); //$NON-NLS-1$
    BooleanQuery query = createGuidQuery(guid, copy);
    return simpleSearch(query);
  }

  private BooleanQuery createGuidQuery(IGuid guid, boolean copy) {
    BooleanQuery query = new BooleanQuery(true);
    query.add(new TermQuery(new Term(String.valueOf(INews.GUID), guid.getValue().toLowerCase())), Occur.MUST);
    query.add(createIsCopyTermQuery(copy));
    return query;
  }

  private List<NewsReference> simpleSearch(BooleanQuery query) {
    /* Make sure the searcher is in sync */
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
      return newsRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  private List<NewsReference> simpleSearch(IndexSearcher currentSearcher, BooleanQuery query) {
    List<NewsReference> resultList = new ArrayList<NewsReference>(2);

    try {
      /* Use custom hit collector for performance reasons */
      /* Perform the Search */
      currentSearcher.search(query, new SimpleHitCollector(currentSearcher, resultList));
      return resultList;
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  private void disposeIfNecessary(IndexSearcher currentSearcher) {
    AtomicInteger referenceCount = fSearchers.get(currentSearcher);
    if (referenceCount.decrementAndGet() == 0 && fSearcher != currentSearcher) {
      try {
        /*
         * May be called by getCurrentSearcher at the same time, but safe
         * because dispose is safe to be called many times for the same
         * searcher.
         */
        dispose(currentSearcher);
      } catch (IOException e) {
        throw new PersistenceException(e);
      }
    }
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#searchNews(org.rssowl.core
   * .persist.ISearch)
   */
  public List<SearchHit<NewsReference>> searchNews(ISearch search) throws PersistenceException {
    return searchNews(search.getSearchConditions(), search.matchAllConditions());
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#searchNews(java.util.List,
   * boolean)
   */
  public List<SearchHit<NewsReference>> searchNews(Collection<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException {
    try {
      return doSearchNews(conditions, matchAllConditions);
    }

    /* Too Many Clauses - Increase Clauses Limit */
    catch (TooManyClauses e) {

      /* Disable Clauses Limit */
      if (BooleanQuery.getMaxClauseCount() != Integer.MAX_VALUE) {
        BooleanQuery.setMaxClauseCount(MAX_CLAUSE_COUNT);
        return doSearchNews(conditions, matchAllConditions);
      }

      /* Maximum reached */
      throw new PersistenceException(Messages.ModelSearchImpl_ERROR_WILDCARDS, e);
    }
  }

  private List<SearchHit<NewsReference>> doSearchNews(Collection<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException {

    /* Perform the search */
    try {
      Query bQuery = ModelSearchQueries.createQuery(conditions, matchAllConditions);

      /* Make sure the searcher is in sync */
      final IndexSearcher currentSearcher = getCurrentSearcher();
      final List<SearchHit<NewsReference>> resultList = new ArrayList<SearchHit<NewsReference>>();

      /* Use custom hit collector for performance reasons */
      HitCollector collector = new HitCollector() {
        @Override
        public void collect(int doc, float score) {
          try {
            Document document = currentSearcher.doc(doc);

            /* Receive Stored Fields */
            long newsId = Long.parseLong(document.get(SearchDocument.ENTITY_ID_TEXT));
            INews.State newsState = NEWS_STATES[Integer.parseInt(document.get(NewsDocument.STATE_ID_TEXT))];

            Map<Integer, INews.State> data = new HashMap<Integer, INews.State>(1);
            data.put(INews.STATE, newsState);

            /* Add to List */
            resultList.add(new SearchHit<NewsReference>(new NewsReference(newsId), score, data));
          } catch (IOException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      };

      /* Perform the Search */
      try {
        currentSearcher.search(bQuery, collector);
        return resultList;
      } finally {
        disposeIfNecessary(currentSearcher);
      }
    } catch (IOException e) {
      throw new PersistenceException(Messages.ModelSearchImpl_ERROR_SEARCH, e);
    }
  }

  private IndexSearcher createIndexSearcher() throws CorruptIndexException, IOException {
    IndexSearcher searcher = new IndexSearcher(IndexReader.open(fDirectory));
    fSearchers.put(searcher, new AtomicInteger(0));
    return searcher;
  }

  private IndexSearcher getCurrentSearcher() throws PersistenceException {
    try {
      boolean flushed = fIndexer.flushIfNecessary();

      /* Get the current searcher before acquiring lock in case we block */
      IndexSearcher currentSearcher = fSearcher;

      synchronized (this) {
        /*
         * If there are changes and currentSearcher == fSearcher, it means we
         * won the race for the lock, so we reopen the searcher. If flushed is
         * true, but currentSearcher != fSearcher it means that another thread
         * has reopened the reader while we were blocked waiting for the lock.
         */
        if (flushed && currentSearcher == fSearcher) {
          IndexReader currentReader = fSearcher.getIndexReader();
          IndexReader newReader = currentReader.reopen();
          if (newReader != currentReader) {

            IndexSearcher newSearcher = new IndexSearcher(newReader);
            fSearchers.put(newSearcher, new AtomicInteger(1));

            /*
             * Assign to field before we check the referenceCount to ensure that
             * disposeIfNecessary will dispose the searcher if it has the last
             * reference, is yet to check if fSearcher has been changed (if this
             * was done after referenceCount.get() == 0, we could leak a
             * searcher).
             */
            fSearcher = newSearcher;

            AtomicInteger referenceCount = fSearchers.get(currentSearcher);
            if (referenceCount != null && referenceCount.get() == 0) {
              /*
               * May be called by disposeIfNecessary at the same time, but safe
               * because dispose is safe to be called many times for the same
               * searcher.
               */
              dispose(currentSearcher);
            }

            return fSearcher;
          }
        }
        fSearchers.get(fSearcher).incrementAndGet();
        return fSearcher;
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  /**
   * Can be called multiple times safely because: - close is safe to be called
   * many times in IndexReader and IndexSearcher - No IndexSearcher is ever
   * added again into the fSearchers map so calling remove two or more times is
   * harmless.
   */
  private void dispose(IndexSearcher searcher) throws IOException {
    fSearchers.remove(searcher);
    searcher.close();
    searcher.getIndexReader().close();
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#clearIndex()
   */
  public void clearIndex() throws PersistenceException {
    try {
      synchronized (this) {
        IndexSearcher currentSearcher = fSearcher;
        fIndexer.clearIndex();
        fSearcher = createIndexSearcher();

        /*
         * We block until the current reader has been closed or can be closed.
         * Most times we should be able to succeed without having to sleep.
         */
        while (true) {
          AtomicInteger refCount = fSearchers.get(currentSearcher);
          if (refCount == null)
            break;
          else if (refCount.get() == 0) {
            /*
             * This may be called at the same time from disposeIfNecessary, but
             * that's fine.
             */
            dispose(currentSearcher);
            break;
          } else {
            try {
              /*
               * We sleep with a lock held because the Threads that we're
               * waiting to make progress don't acquire a lock
               */
              Thread.sleep(100);
            } catch (InterruptedException e) {
              throw new PersistenceException("Failed to close IndexSearcher: " + fSearcher); //$NON-NLS-1$
            }
          }
        }
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#addIndexListener(org.rssowl
   * .core.persist.service.IndexListener)
   */
  public void addIndexListener(IndexListener listener) {
    fIndexListeners.add(listener);
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#removeIndexListener(org.rssowl
   * .core.persist.service.IndexListener)
   */
  public void removeIndexListener(IndexListener listener) {
    fIndexListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.persist.service.IModelSearch#optimize()
   */
  public void optimize() {
    try {
      fIndexer.optimize();
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  void notifyIndexUpdated(int docCount) {
    for (IndexListener listener : fIndexListeners) {
      listener.indexUpdated(docCount);
    }
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#reindexAll(org.eclipse.core
   * .runtime.IProgressMonitor)
   */
  public void reindexAll(IProgressMonitor monitor) throws PersistenceException {
    /* May be used before Owl is completely set-up */
    Collection<INews> newsList = InternalOwl.getDefault().getPersistenceService().getDAOService().getNewsDAO().loadAll();

    monitor.beginTask(Messages.ModelSearchImpl_RE_INDEXING_NEWS, newsList.size());

    EntityIdsByEventType entitiesToBeIndexed = DBHelper.getEntitiesToBeIndexedDAO().load();

    /* Ensure that we don't lose entities on dirty shutdown */
    synchronized (entitiesToBeIndexed) {
      for (INews news : newsList)
        entitiesToBeIndexed.addUpdatedEntity(news);
    }

    DBHelper.getEntitiesToBeIndexedDAO().save(entitiesToBeIndexed);
    /* Lock the indexer for the duration of the reindexing */
    synchronized (fIndexer) {
      /* Delete the Index first */
      clearIndex();

      /*
       * Re-Index all Entities: News. newsList is a LazyList so news are only
       * activated on retrieval
       */
      for (INews news : newsList) {
        if (monitor.isCanceled())
          break;

        /* We don't pass the whole list at once to be able to report progress. */
        List<INews> indexList = new ArrayList<INews>(1);
        indexList.add(news);
        fIndexer.index(indexList, false);
        monitor.worked(1);
      }
      /* Commit in order to avoid first search slowdown */
      fIndexer.flushIfNecessary();
    }

    /* Finished */
    monitor.done();
  }
}