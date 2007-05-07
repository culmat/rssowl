
package org.rssowl.core.internal.persist.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.JobQueue;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * @author ijuma
 * @author bpasero
 */
public class Indexer {

  /* Delay in millis before showing Progress of Indexing */
  private static final int INDEX_JOB_PROGRESS_DELAY = 800;

  /* Lucene only allows 1 Indexer to run at the same time */
  private static final int MAX_INDEX_JOBS_COUNT = 1;

  /* The directory to the lucene index */
  private final Directory fIndexDirectory;

  /* The IndexWriter to add/update/delete Documents */
  private IndexWriter fIndexWriter;

  private final JobQueue fJobQueue;
  private NewsListener fNewsListener;
  private boolean fFlushRequired;
  private final ModelSearchImpl fSearch;

  /* The Default Analyzer */
  private static class DefaultAnalyzer extends KeywordAnalyzer {

    /*
     * @see org.apache.lucene.analysis.KeywordAnalyzer#tokenStream(java.lang.String,
     * java.io.Reader)
     */
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = super.tokenStream(fieldName, reader);
      result = new LowerCaseFilter(result);

      return result;
    }
  }

  /**
   * @param search
   * @param directory
   * @throws PersistenceException
   */
  Indexer(ModelSearchImpl search, Directory directory) throws PersistenceException {
    fSearch = search;
    fIndexDirectory = directory;
    fJobQueue = new JobQueue("Updating Saved Searches", MAX_INDEX_JOBS_COUNT, true, INDEX_JOB_PROGRESS_DELAY);

    init();
  }

  /**
   * TODO Provide generic method that can deal with any entity!
   *
   * @param entities
   * @param isUpdate
   */
  synchronized void index(List<INews> entities, boolean isUpdate) {
    int docCount = 0;

    /* For each Event */
    for (ListIterator<INews> it = entities.listIterator(entities.size()); it.hasPrevious();) {
      INews news = it.previous();
      it.remove();
      NewsDocument newsDoc = new NewsDocument(news);
      try {
        if (newsDoc.addFields()) {
          docCount++;

          /* Update Event */
          if (isUpdate) {
            Term term = createTerm(news);
            fIndexWriter.updateDocument(term, newsDoc.getDocument());
          }

          /* Added Event */
          else
            fIndexWriter.addDocument(newsDoc.getDocument());

          /* Mark as in need for a flush */
          fFlushRequired = true;
        }
      } catch (IOException e) {
        Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }
    }

    /* Notify Listeners */
    if (fFlushRequired)
      fSearch.notifyIndexUpdated(docCount);
  }

  /**
   * TODO Provide generic method that can deal with any Event!
   *
   * @param entities
   * @throws IOException
   */
  synchronized void removeFromIndex(List<INews> entities) throws IOException {
    int docCount = 0;

    /* For each entity */
    for (ListIterator<INews> it = entities.listIterator(entities.size()); it.hasPrevious();) {
      INews news = it.previous();
      it.remove();
      Term term = createTerm(news);
      fIndexWriter.deleteDocuments(term);
      docCount++;
    }

    /* Mark as in need for a flush */
    fFlushRequired = true;

    /* Notify Listeners */
    fSearch.notifyIndexUpdated(docCount);
  }

  /**
   * @throws IOException
   */
  synchronized void flushIfNecessary() throws IOException {
    if (fFlushRequired)
      fIndexWriter.flush();

    fFlushRequired = false;
  }

  /**
   * @throws IOException
   */
  synchronized void shutdown() throws IOException {
    unregisterListeners();
    dispose();
  }

  /**
   * Deletes all the information that is stored in the search index. This must
   * be called if the information stored in the persistence layer has been
   * cleared with a method that does not issue events for the elements that are
   * removed.
   *
   * @throws IOException
   */
  synchronized void clearIndex() throws IOException {
    dispose();
    if (IndexReader.indexExists(fIndexDirectory))
      fIndexWriter = createIndexWriter(fIndexDirectory, true);
  }

  /**
   * Creates the <code>Analyzer</code> that is used for all analyzation of
   * Fields and Queries.
   *
   * @return Returns the <code>Analyzer</code> that is used for all
   * analyzation of Fields and Queries.
   */
  Analyzer createAnalyzer() {
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new DefaultAnalyzer());

    /* Standard (Lowercase, Letter, Stop,...) */
    StandardAnalyzer stdAnalyzer = new StandardAnalyzer();
    analyzer.addAnalyzer(String.valueOf(INews.TITLE), stdAnalyzer);
    analyzer.addAnalyzer(String.valueOf(INews.DESCRIPTION), stdAnalyzer);
    analyzer.addAnalyzer(String.valueOf(INews.ATTACHMENTS_CONTENT), stdAnalyzer);

    /* Simple (Lowercase, Whitespace Tokzenizer) */
    LowercaseWhitespaceAnalyzer simpleAnalyzer = new LowercaseWhitespaceAnalyzer();
    analyzer.addAnalyzer(String.valueOf(INews.AUTHOR), simpleAnalyzer);
    analyzer.addAnalyzer(String.valueOf(INews.LABEL), simpleAnalyzer);

    /* Simple (Lowercase, Delim Tokenizer) */
    analyzer.addAnalyzer(String.valueOf(INews.CATEGORIES), new LowercaseDelimiterAnalyzer('\n'));

    return analyzer;
  }

  private void init() throws PersistenceException {

    /* Create Index Writer */
    createIndexWriter();

    /* Listen to Model Events */
    registerListeners();
  }

  synchronized void initIfNecessary() {
    if (fIndexWriter == null)
      init();
  }

  private void createIndexWriter() {

    /* Create the Index if required */
    try {
      fIndexWriter = createIndexWriter(fIndexDirectory, !IndexReader.indexExists(fIndexDirectory));
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private void registerListeners() {

    /* Listener already registered */
    if (fNewsListener != null)
      return;

    /* Listen to News-Events */
    fNewsListener = new NewsListener() {
      public void entitiesAdded(Set<NewsEvent> events) {
        if (!Owl.TESTING)
          fJobQueue.schedule(new IndexingTask(Indexer.this, events, EventType.PERSIST));
        else
          new IndexingTask(Indexer.this, events, EventType.PERSIST).run(new NullProgressMonitor());
      }

      public void entitiesUpdated(Set<NewsEvent> events) {
        if (!Owl.TESTING)
          fJobQueue.schedule(new IndexingTask(Indexer.this, events, EventType.UPDATE));
        else
          new IndexingTask(Indexer.this, events, EventType.UPDATE).run(new NullProgressMonitor());
      }

      public void entitiesDeleted(Set<NewsEvent> events) {
        if (!Owl.TESTING)
          fJobQueue.schedule(new IndexingTask(Indexer.this, events, EventType.REMOVE));
        else
          new IndexingTask(Indexer.this, events, EventType.REMOVE).run(new NullProgressMonitor());
      }
    };

    /* We register listeners as part of initialisation, we must use InternalOwl */
    InternalOwl.getDefault().getPersistenceService().getDAOService().getNewsDAO().addEntityListener(fNewsListener);
  }

  private void unregisterListeners() {
    if (fNewsListener != null)
      Owl.getPersistenceService().getDAOService().getNewsDAO().removeEntityListener(fNewsListener);

    fNewsListener = null;
  }

  private IndexWriter createIndexWriter(Directory directory, boolean create) throws IOException {
    IndexWriter indexWriter = new IndexWriter(directory, createAnalyzer(), create);
    fFlushRequired = false;
    return indexWriter;
  }

  private Term createTerm(IEntity entity) {
    String value = String.valueOf(entity.getId());
    return new Term(SearchDocument.ENTITY_ID_TEXT, value);
  }

  private void dispose() throws IOException {
    if (fIndexWriter == null)
      return;

    fIndexWriter.close();
    fIndexWriter = null;
    fFlushRequired = false;
  }
}