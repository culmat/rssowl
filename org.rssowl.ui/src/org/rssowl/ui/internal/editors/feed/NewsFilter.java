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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.util.StringMatcher;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author bpasero
 */
public class NewsFilter extends ViewerFilter {

  /** Possible Filter Values */
  public enum Type {

    /** Show all News */
    SHOW_ALL("Show All News"),

    /** Show New News */
    SHOW_NEW("Show New News"),

    /** Show Unread News */
    SHOW_UNREAD("Show Unread News"),

    /** Show Recent News */
    SHOW_RECENT("Show Recent News"),

    /** Show Sticky News */
    SHOW_STICKY("Show Sticky News");

    String fName;

    Type(String name) {
      fName = name;
    }

    /**
     * Returns a human-readable Name of this enum-value.
     * 
     * @return A human-readable Name of this enum-value.
     */
    public String getName() {
      return fName;
    }
  }

  /** Possible Search Targets */
  public enum SearchTarget {

    /** Search Headlines */
    HEADLINE("Headline"),

    /** Search Entire News */
    ALL("Entire News"),

    /** Search Author */
    AUTHOR("Author"),

    /** Search Category */
    CATEGORY("Category"),

    /** Search Source */
    SOURCE("Source"),

    /** Search Attachments */
    ATTACHMENTS("Attachments");

    String fName;

    SearchTarget(String name) {
      fName = name;
    }

    /**
     * Returns a human-readable Name of this enum-value.
     * 
     * @return A human-readable Name of this enum-value.
     */
    public String getName() {
      return fName;
    }
  }

  /* The string pattern matcher used for this pattern filter */
  private StringMatcher fMatcher;

  /* Current Filter Value */
  private Type fType = Type.SHOW_ALL;

  /* Current Search Target */
  private SearchTarget fSearchTarget = SearchTarget.HEADLINE;

  /*
   * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  @Override
  public final boolean select(Viewer viewer, Object parentElement, Object element) {

    /* Filter not Active */
    if (fMatcher == null && fType == Type.SHOW_ALL)
      return true;

    return isElementVisible(viewer, element);
  }

  /**
   * Answers whether the given element in the given viewer matches the filter
   * pattern. This is a default implementation that will show a leaf element in
   * the tree based on whether the provided filter text matches the text of the
   * given element's text, or that of it's children (if the element has any).
   * Subclasses may override this method.
   * 
   * @param viewer the tree viewer in which the element resides
   * @param element the element in the tree to check for a match
   * @return true if the element matches the filter pattern
   */
  boolean isElementVisible(Viewer viewer, Object element) {
    return isParentMatch(viewer, element) || isLeafMatch(viewer, element);
  }

  /**
   * Answers whether the given element is a valid selection in the filtered
   * tree. For example, if a tree has items that are categorized, the category
   * itself may not be a valid selection since it is used merely to organize the
   * elements.
   * 
   * @param element
   * @return true if this element is eligible for automatic selection
   */
  boolean isElementSelectable(Object element) {
    return element != null;
  }

  /**
   * Check if the parent (category) is a match to the filter text. The default
   * behavior returns true if the element has at least one child element that is
   * a match with the filter text. Subclasses may override this method.
   * 
   * @param viewer the viewer that contains the element
   * @param element the tree element to check
   * @return true if the given element has children that matches the filter text
   */
  private boolean isParentMatch(Viewer viewer, Object element) {
    if (viewer instanceof AbstractTreeViewer) {
      ITreeContentProvider provider = (ITreeContentProvider) ((AbstractTreeViewer) viewer).getContentProvider();
      Object[] children = provider.getChildren(element);

      if ((children != null) && (children.length > 0))
        return filter(viewer, element, children).length > 0;
    }

    return false;
  }

  /**
   * Check if the current (leaf) element is a match with the filter text. The
   * default behavior checks that the label of the element is a match.
   * Subclasses should override this method.
   * 
   * @param viewer the viewer that contains the element
   * @param element the tree element to check
   * @return true if the given element's label matches the filter text
   */
  private boolean isLeafMatch(@SuppressWarnings("unused")
  Viewer viewer, Object element) {

    /* Filter not Active */
    if (fMatcher == null && fType == Type.SHOW_ALL)
      return true;

    /* Element is a News */
    if (element instanceof INews) {
      INews news = (INews) element;

      /* First check the Pattern */
      if (fMatcher != null && !wordMatches(news)) {
        return false;
      }

      /* Show: All */
      if (fType == Type.SHOW_ALL)
        return true;

      /* Show New News */
      else if (fType == Type.SHOW_NEW) {
        INews.State state = news.getState();
        return state == INews.State.NEW;
      }

      /* Show Unread News */
      else if (fType == Type.SHOW_UNREAD) {
        INews.State state = news.getState();
        return state == INews.State.UNREAD || state == INews.State.NEW || state == INews.State.UPDATED;
      }

      /* Show Sticky News */
      else if (fType == Type.SHOW_STICKY) {
        return news.isFlagged();
      }

      /* Show Recent News (max 24h old) */
      else if (fType == Type.SHOW_RECENT) {
        Date date = DateUtils.getRecentDate(news);
        return date.getTime() > (System.currentTimeMillis() - DateUtils.DAY);
      }
    }

    return false;
  }

  @Override
  public boolean isFilterProperty(Object element, String property) {
    return false; // This is handled in needsRefresh() already
  }

  /**
   * Set the Type of this Filter. The Type is describing which elements are
   * filtered.
   * 
   * @param type The Type of this Filter as described in the <code>Type</code>
   * enumeration.
   */
  public void setType(Type type) {
    if (fType != type)
      fType = type;
  }

  /**
   * Get the Type of this Filter. The Type is describing which elements are
   * filtered.
   * 
   * @return Returns the Type of this Filter as described in the
   * <code>Type</code> enumeration.
   */
  Type getType() {
    return fType;
  }

  /**
   * Get the Target of the Search. The Target is describing which elements to
   * search when a Text-Search is performed.
   * 
   * @return Returns the SearchTarget of the Search as described in the
   * <code>SearchTarget</code> enumeration.
   */
  SearchTarget getSearchTarget() {
    return fSearchTarget;
  }

  /**
   * Set the Target of the Search. The Target is describing which elements to
   * search when a Text-Search is performed.
   * 
   * @param searchTarget The SearchTarget of the Search as described in the
   * <code>SearchTarget</code> enumeration.
   */
  public void setSearchTarget(SearchTarget searchTarget) {
    fSearchTarget = searchTarget;
  }

  /**
   * The pattern string for which this filter should select elements in the
   * viewer.
   * 
   * @param patternString
   */
  public void setPattern(String patternString) {
    if (patternString == null || patternString.equals("")) //$NON-NLS-1$
      fMatcher = null;
    else
      fMatcher = new StringMatcher(patternString + "*", true, false); //$NON-NLS-1$
  }

  /**
   * @return <code>TRUE</code> in case a Pattern is set and <code>FALSE</code>
   * otherwise.
   */
  boolean isPatternSet() {
    return fMatcher != null;
  }

  /**
   * Answers whether the given String matches the pattern.
   * 
   * @param string the String to test
   * @return whether the string matches the pattern
   */
  private boolean match(String string) {
    if (fMatcher == null)
      return true;

    return fMatcher.match(string);
  }

  /**
   * Take the given filter text and break it down into words using a
   * BreakIterator.
   * 
   * @param text
   * @return an array of words
   */
  private String[] getWords(String text) {
    List<String> words = new ArrayList<String>();

    /*
     * Break the text up into words, separating based on whitespace and common
     * punctuation. Previously used String.split(..., "\\W"), where "\W" is a
     * regular expression (see the Javadoc for class Pattern). Need to avoid
     * both String.split and regular expressions, in order to compile against
     * JCL Foundation (bug 80053). Also need to do this in an NL-sensitive way.
     * The use of BreakIterator was suggested in bug 90579.
     */
    BreakIterator iter = BreakIterator.getWordInstance();
    iter.setText(text);
    int i = iter.first();
    while (i != java.text.BreakIterator.DONE && i < text.length()) {
      int j = iter.following(i);
      if (j == java.text.BreakIterator.DONE)
        j = text.length();

      /* match the word */
      if (Character.isLetterOrDigit(text.charAt(i))) {
        String word = text.substring(i, j);
        words.add(word);
      }
      i = j;
    }
    return words.toArray(new String[words.size()]);
  }

  private boolean wordMatches(INews news) {

    /* Custom List that converts Objects to Strings and checks for NULL */
    List<Object> words = new ArrayList<Object>() {
      @Override
      public boolean add(Object o) {
        if (o instanceof String)
          return super.add(o);

        else if (o != null)
          return super.add(o.toString());

        return false;
      }
    };

    /* Search Headline */
    if ((fSearchTarget == SearchTarget.HEADLINE || fSearchTarget == SearchTarget.ALL))
      words.add(ModelUtils.getHeadline(news));

    /* Search Author */
    if (fSearchTarget == SearchTarget.AUTHOR || fSearchTarget == SearchTarget.ALL) {
      IPerson author = news.getAuthor();
      if (author != null) {
        words.add(author.getName());
        words.add(author.getEmail());
        words.add(author.getUri());
      }
    }

    /* Search Category */
    if (fSearchTarget == SearchTarget.CATEGORY || fSearchTarget == SearchTarget.ALL) {
      List<ICategory> categories = news.getCategories();
      for (ICategory category : categories) {
        words.add(category.getName());
        words.add(category.getDomain());
      }
    }

    /* Search Source */
    if (fSearchTarget == SearchTarget.SOURCE || fSearchTarget == SearchTarget.ALL) {
      ISource source = news.getSource();
      if (source != null) {
        words.add(source.getName());
        words.add(source.getLink());
      }
    }

    /* Search Attachments */
    if (fSearchTarget == SearchTarget.ATTACHMENTS || fSearchTarget == SearchTarget.ALL) {
      List<IAttachment> attachments = news.getAttachments();
      for (IAttachment attachment : attachments) {
        words.add(attachment.getType());
        words.add(attachment.getLink());
      }
    }

    /* Search All */
    if (fSearchTarget == SearchTarget.ALL) {
      words.add(news.getDescription());
      words.add(news.getComments());
      words.add(news.getLink());
    }

    StringBuilder str = new StringBuilder();
    for (Object object : words)
      str.append(object).append(' ');

    return str.length() > 0 && wordMatches(str.toString());
  }

  /**
   * Return whether or not if any of the words in text satisfy the match
   * critera.
   * 
   * @param text the text to match
   * @return boolean <code>true</code> if one of the words in text satisifes
   * the match criteria.
   */
  private boolean wordMatches(String text) {
    if (text == null)
      return false;

    /* If the whole text matches we are all set */
    if (match(text))
      return true;

    /* Otherwise check if any of the words of the text matches */
    String[] words = getWords(text);
    for (String word : words) {
      if (match(word))
        return true;
    }

    return false;
  }
}