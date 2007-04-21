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

package org.rssowl.contrib.internal.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.search.IModelSearch;
import org.rssowl.core.persist.search.ISearchCondition;
import org.rssowl.core.persist.search.ISearchHit;
import org.rssowl.core.persist.search.ISearchValueType;
import org.rssowl.core.persist.search.SearchSpecifier;
import org.rssowl.core.persist.service.PersistenceException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * The central interface for searching types from the persistence layer. The
 * implementation is contributable via extension-point mechanism.
 * <p>
 * TODO Consider using the <code>HitCollector</code> for collecting results
 * from a search because Lucene will re-run the search for any result > 100.
 * </p>
 *
 * @author ijuma
 * @author bpasero
 */
public class ModelSearchImpl implements IModelSearch {

  /* Maximum Date as String */
  private static final String MAX_DATE = DateTools.dateToString(new Date(Long.MAX_VALUE), Resolution.DAY);

  /* Minimum Date as String */
  private static final String MIN_DATE = DateTools.dateToString(new Date(0), Resolution.DAY);

  /* Maximum Number as String */
  private static final String MAX_NUMBER = NumberTools.longToString(Long.MAX_VALUE);

  /* Minimum Number as String */
  private static final String MIN_NUMBER = NumberTools.longToString(Long.MIN_VALUE);

  /* One Day in Millis */
  private static final Long DAY = 1000 * 3600 * 24L;

  private IndexSearcher fSearcher;
  private Indexer fIndexer;
  private Directory fDirectory;

  public ModelSearchImpl() {}

  /*
   * @see org.rssowl.core.model.search.IModelSearch#startup()
   */
  public synchronized void startup() throws PersistenceException {
    try {
      if (fDirectory == null) {
        String path = Activator.getDefault().getStateLocation().toOSString();
        LockFactory lockFactory = new NativeFSLockFactory(path);
        fDirectory = FSDirectory.getDirectory(path, lockFactory);
      }

      if (fIndexer == null)
        fIndexer = new Indexer(fDirectory);
      else
        fIndexer.initIfNecessary();

      if (fSearcher == null)
        fSearcher = new IndexSearcher(fDirectory);
    } catch (IOException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#shutdown()
   */
  public synchronized void shutdown() throws PersistenceException {
    try {
      disposeSearcher();
      fIndexer.shutdown();
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#searchNews(java.util.List,
   * boolean)
   */
  public List<ISearchHit<NewsReference>> searchNews(List<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException {

    /* Make sure the searcher is in sync */
    isSearcherCurrent();

    /* Perform the search */
    try {
      BooleanQuery bQuery = new BooleanQuery();

      /* Handle State-Field separately (group) */
      BooleanQuery stateQuery = null;
      for (ISearchCondition condition : conditions) {
        if (requiresStateGrouping(condition)) {

          /* Create and add new BooleanQuery for State */
          if (stateQuery == null) {
            stateQuery = new BooleanQuery();
            bQuery.add(stateQuery, Occur.MUST);
          }

          stateQuery.add(createBooleanClause(condition, false));
        }
      }

      /* Create a Query for each condition */
      BooleanQuery fieldQuery = null;
      for (ISearchCondition condition : conditions) {

        /* State Queries already handled */
        if (requiresStateGrouping(condition))
          continue;

        /* Create and add new BooleanQuery for other Fields */
        if (fieldQuery == null) {
          fieldQuery = new BooleanQuery();
          bQuery.add(fieldQuery, Occur.MUST);
        }

        /* Create the Clause */
        BooleanClause clause = null;
        if (condition.getField().getId() == IEntity.ALL_FIELDS)
          clause = createAllNewsFieldsClause(condition, matchAllConditions);
        else
          clause = createBooleanClause(condition, matchAllConditions);

        /*
         * Specially treat this case where the specifier is a negation but any
         * of the supplied conditions should match in the result set.
         */
        if (condition.getSpecifier().isNegation() && !matchAllConditions) {
          BooleanQuery nestedquery = new BooleanQuery();
          nestedquery.add(clause);
          nestedquery.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
          fieldQuery.add(new BooleanClause(nestedquery, Occur.SHOULD));
        }

        /* Normal Case */
        else {
          fieldQuery.add(clause);
        }
      }

      /* Add the MatchAllDocsQuery (MUST_NOT is used, All Conditions match) */
      if (fieldQuery != null && matchAllConditions) {
        boolean requireAllDocsQuery = true;
        BooleanClause[] clauses = fieldQuery.getClauses();
        for (BooleanClause clause : clauses) {
          if (clause.getOccur() != Occur.MUST_NOT) {
            requireAllDocsQuery = false;
            break;
          }
        }

        /* Add if required */
        if (requireAllDocsQuery)
          fieldQuery.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
      }

      /* Perform the Search */
      Hits hits;
      synchronized (this) {
        hits = fSearcher.search(bQuery);
      }

      /* Build Result */
      List<ISearchHit<NewsReference>> resultList = new ArrayList<ISearchHit<NewsReference>>(hits.length());
      for (Iterator< ? > it = hits.iterator(); it.hasNext();) {
        Hit hit = (Hit) it.next();
        String idText = hit.get(SearchDocument.ENTITY_ID_TEXT);
        long id = Long.valueOf(idText);

        /* Add to List */
        resultList.add(new SearchHit<NewsReference>(new NewsReference(id), hit.getScore()));
      }

      return resultList;
    } catch (IOException e) {
      throw new PersistenceException("Error searching news", e);
    }
  }

  private boolean requiresStateGrouping(ISearchCondition condition) {
    return condition.getField().getId() == INews.STATE && condition.getSpecifier() == SearchSpecifier.IS;
  }

  //TODO Think about a better performing solution here!
  private BooleanClause createAllNewsFieldsClause(ISearchCondition condition, boolean matchAllConditions) throws IOException {
    BooleanQuery allFieldsQuery = new BooleanQuery();
    String value = String.valueOf(condition.getValue());

    LowercaseWhitespaceAnalyzer analyzer = new LowercaseWhitespaceAnalyzer();
    TokenStream tokenStream = analyzer.tokenStream(String.valueOf(IEntity.ALL_FIELDS), new StringReader(value));
    Token token = null;
    while ((token = tokenStream.next()) != null) {

      /* Contained in Title */
      WildcardQuery titleQuery = new WildcardQuery(new Term(String.valueOf(INews.TITLE), token.termText()));
      allFieldsQuery.add(new BooleanClause(titleQuery, Occur.SHOULD));

      /* Contained in Description */
      WildcardQuery descriptionQuery = new WildcardQuery(new Term(String.valueOf(INews.DESCRIPTION), token.termText()));
      allFieldsQuery.add(new BooleanClause(descriptionQuery, Occur.SHOULD));

      /* Contained in Attachment */
      WildcardQuery attachmentQuery = new WildcardQuery(new Term(String.valueOf(INews.ATTACHMENTS_CONTENT), token.termText()));
      allFieldsQuery.add(new BooleanClause(attachmentQuery, Occur.SHOULD));

      /* Matches Author */
      WildcardQuery authorQuery = new WildcardQuery(new Term(String.valueOf(INews.AUTHOR), token.termText()));
      allFieldsQuery.add(new BooleanClause(authorQuery, Occur.SHOULD));

      /* Matches Category */
      WildcardQuery categoryQuery = new WildcardQuery(new Term(String.valueOf(INews.CATEGORIES), token.termText()));
      allFieldsQuery.add(new BooleanClause(categoryQuery, Occur.SHOULD));
    }

    /* Determine Occur (MUST, SHOULD, MUST NOT) */
    Occur occur = getOccur(condition.getSpecifier(), matchAllConditions);
    return new BooleanClause(allFieldsQuery, occur);
  }

  private BooleanClause createBooleanClause(ISearchCondition condition, boolean matchAllConditions) throws IOException {
    Query query = null;

    /* Separately handle this dynamic Query */
    if (condition.getField().getId() == INews.AGE_IN_DAYS)
      query = createAgeClause(condition);

    /* Other Fields */
    else {
      try {
        switch (condition.getField().getSearchValueType().getId()) {

          /* Boolean: Simple Term-Query */
          case ISearchValueType.BOOLEAN:
            query = createTermQuery(condition);
            break;

          /* String / Link / Enum: String Query */
          case ISearchValueType.ENUM:
          case ISearchValueType.STRING:
          case ISearchValueType.LINK:
            query = createStringQuery(condition);
            break;

          /* Date / Time / DateTime: Date Query (Ranged) */
          case ISearchValueType.DATE:
          case ISearchValueType.TIME:
          case ISearchValueType.DATETIME:
            query = createDateQuery(condition);
            break;

          /* Number / Integer: Number Query (Ranged) */
          case ISearchValueType.NUMBER:
          case ISearchValueType.INTEGER:
            query = createNumberQuery(condition);
        }
      } catch (ParseException e) {
        Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }
    }

    /* In case of the Query not being created, fallback to Term-Query */
    if (query == null) {
      query = createTermQuery(condition);
    }

    /* Determine Occur (MUST, SHOULD, MUST NOT) */
    Occur occur = getOccur(condition.getSpecifier(), matchAllConditions);
    return new BooleanClause(query, occur);
  }

  /* This Clause needs to be generated dynamically */
  private Query createAgeClause(ISearchCondition condition) {
    Integer age = (Integer) condition.getValue();
    String fieldname = String.valueOf(condition.getField().getId());

    /* Calculate Desired Date */
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis() - age * DAY);
    String value = DateTools.dateToString(cal.getTime(), Resolution.DAY);

    switch (condition.getSpecifier()) {
      case IS: {
        return new TermQuery(new Term(fieldname, value));
      }

      case IS_GREATER_THAN: {
        Term lowerBound = new Term(fieldname, MIN_DATE);
        Term upperBound = new Term(fieldname, value);

        return new RangeQuery(lowerBound, upperBound, false);
      }

      case IS_LESS_THAN: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_DATE);

        return new RangeQuery(lowerBound, upperBound, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Age Query");
  }

  private Query createStringQuery(ISearchCondition condition) throws ParseException, IOException {
    SearchSpecifier specifier = condition.getSpecifier();
    String fieldname = String.valueOf(condition.getField().getId());

    /* Retrieve Value */
    String value;
    if (condition.getValue() instanceof Enum)
      value = String.valueOf(((Enum< ? >) condition.getValue()).ordinal());
    else
      value = String.valueOf(condition.getValue());

    switch (specifier) {

      /* Create Wildcard-Query */
      case IS:
      case IS_NOT: {
        Term term = new Term(fieldname, value.toLowerCase());
        return new WildcardQuery(term);
      }

        /* Let Query-Parser handle this */
      case CONTAINS:
      case CONTAINS_NOT: {
        Analyzer analyzer;
        synchronized (this) {
          analyzer = fIndexer.createAnalyzer();
        }
        QueryParser parser = new QueryParser(fieldname, analyzer);
        parser.setAllowLeadingWildcard(true);

        /* Prepare the value for parsing */
        value = prepareForParsing(value);

        /* Parse */
        return parser.parse(value);
      }

        /* Wildcard-Query with trailing '*' */
      case BEGINS_WITH: {
        value = new StringBuilder(value.toLowerCase()).append("*").toString();
        Term term = new Term(fieldname, value);
        WildcardQuery query = new WildcardQuery(term);
        return query;
      }

        /* Wildcard-Query with leading '*' */
      case ENDS_WITH: {
        value = new StringBuilder("*").append(value.toLowerCase()).toString();
        Term term = new Term(fieldname, value);
        return new WildcardQuery(term);
      }

        /* Fuzzy Query */
      case SIMILIAR_TO: {
        BooleanQuery similarityQuery = new BooleanQuery();

        LowercaseWhitespaceAnalyzer analyzer = new LowercaseWhitespaceAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(String.valueOf(IEntity.ALL_FIELDS), new StringReader(value));
        Token token = null;
        while ((token = tokenStream.next()) != null) {
          Term term = new Term(fieldname, token.termText());
          similarityQuery.add(new BooleanClause(new FuzzyQuery(term), Occur.MUST));
        }

        return similarityQuery;
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Parsed Queries");
  }

  private Query createTermQuery(ISearchCondition condition) {
    String value;
    if (condition.getValue() instanceof Enum)
      value = String.valueOf(((Enum< ? >) condition.getValue()).ordinal());
    else
      value = String.valueOf(condition.getValue());

    String fieldname = String.valueOf(condition.getField().getId());

    Term term = new Term(fieldname, value);
    return new TermQuery(term);
  }

  private Query createDateQuery(ISearchCondition condition) {
    SearchSpecifier specifier = condition.getSpecifier();
    String value = DateTools.dateToString((Date) condition.getValue(), Resolution.DAY);
    String fieldname = String.valueOf(condition.getField().getId());

    switch (specifier) {
      case IS:
      case IS_NOT:
        return new TermQuery(new Term(fieldname, value));

      case IS_AFTER: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_DATE);

        return new RangeQuery(lowerBound, upperBound, false);
      }

      case IS_BEFORE: {
        Term lowerBound = new Term(fieldname, MIN_DATE);
        Term upperBound = new Term(fieldname, value);

        return new RangeQuery(lowerBound, upperBound, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Date/Time Queries");
  }

  private Query createNumberQuery(ISearchCondition condition) {
    SearchSpecifier specifier = condition.getSpecifier();
    String value = NumberTools.longToString((Integer) condition.getValue());
    String fieldname = String.valueOf(condition.getField().getId());

    switch (specifier) {
      case IS:
      case IS_NOT:
        return new TermQuery(new Term(fieldname, value));

      case IS_GREATER_THAN: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_NUMBER);

        return new RangeQuery(lowerBound, upperBound, false);
      }

      case IS_LESS_THAN: {
        Term lowerBound = new Term(fieldname, MIN_NUMBER);
        Term upperBound = new Term(fieldname, value);

        return new RangeQuery(lowerBound, upperBound, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Number Queries");
  }

  private synchronized void isSearcherCurrent() throws PersistenceException {
    try {
      fIndexer.flushIfNecessary();

      /* Create searcher if not yet done */
      if (fSearcher == null)
        fSearcher = new IndexSearcher(fDirectory);

      /* Re-Create searcher if no longer current */
      else if (!fSearcher.getIndexReader().isCurrent()) {
        fSearcher.close();
        fSearcher = new IndexSearcher(fDirectory);
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private Occur getOccur(SearchSpecifier specifier, boolean matchAllConditions) {
    switch (specifier) {
      case IS_NOT:
      case CONTAINS_NOT:
        return Occur.MUST_NOT;

      default:
        return matchAllConditions ? Occur.MUST : Occur.SHOULD;
    }
  }

  private void disposeSearcher() throws IOException {
    if (fSearcher != null)
      fSearcher.close();

    fSearcher = null;
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#clearIndex()
   */
  public synchronized void clearIndex() throws PersistenceException {
    try {
      disposeSearcher();
      fIndexer.clearIndex();
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#createSearchCondition(java.lang.Object,
   * float)
   */
  public <T> ISearchHit<T> createSearchHit(T result, float relevance) {
    return new SearchHit<T>(result, relevance);
  }

  private static String prepareForParsing(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      /* Escape Special Characters being used in Lucene */
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~')
        sb.append('\\');

      sb.append(c);
    }
    return sb.toString();
  }
}