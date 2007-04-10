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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.persist.pref.IPreferenceScope;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.NewsReference;
import org.rssowl.core.model.reference.SearchMarkReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;

/**
 * Part of the FeedView to display News in a BrowserViewer.
 *
 * @author bpasero
 */
public class NewsBrowserControl implements IFeedViewPart {
  private IEditorSite fEditorSite;
  private NewsBrowserViewer fViewer;
  private ISelectionListener fSelectionListener;
  private Object fInitialInput;
  private IPreferenceScope fInputPreferences;
  private IPropertyChangeListener fFontPropertyChangeListener;

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#init(org.eclipse.ui.IEditorSite)
   */
  public void init(IEditorSite editorSite) {
    fEditorSite = editorSite;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#onInputChanged(org.rssowl.ui.internal.editors.feed.FeedViewInput)
   */
  public void onInputChanged(FeedViewInput input) {
    fInputPreferences = Owl.getPreferenceService().getEntityScope(input.getMark());
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#createViewer(org.eclipse.swt.widgets.Composite)
   */
  public NewsBrowserViewer createViewer(Composite parent) {
    fViewer = new NewsBrowserViewer(parent, SWT.NONE);
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    return fViewer;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#getViewer()
   */
  public NewsBrowserViewer getViewer() {
    return fViewer;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#initViewer(org.eclipse.jface.viewers.IStructuredContentProvider,
   * org.eclipse.jface.viewers.ViewerFilter)
   */
  public void initViewer(IStructuredContentProvider contentProvider, ViewerFilter filter) {

    /* Apply ContentProvider */
    fViewer.setContentProvider(contentProvider);

    /* Create LabelProvider */
    fViewer.setLabelProvider(new NewsBrowserLabelProvider());

    /* Create Comparator */
    fViewer.setComparator(new NewsComparator());

    /* Add ViewerFilter */
    fViewer.addFilter(filter);

    /* Register Listeners */
    registerListener();
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setInput(java.lang.Object)
   */
  public void setPartInput(Object input) {
    fViewer.setInput(getInput(input));

    /* Remember as initial Input */
    fInitialInput = fViewer.getInput();
  }

  private Object getInput(Object obj) {

    /* Bookmark: Return BookMarkReference */
    if (obj instanceof IBookMark)
      return new BookMarkReference(((IBookMark) obj).getId());

    /* Searchmark: Return SearchMarkReference */
    else if (obj instanceof ISearchMark)
      return new SearchMarkReference(((ISearchMark) obj).getId());

    /* News: Handle special dependant on settings */
    else if (obj instanceof INews)
      return getInput((INews) obj);

    /* NewsReference: Resolve and special handle */
    else if (obj instanceof NewsReference)
      return getInput(((NewsReference) obj).resolve());

    return obj;
  }

  private Object getInput(INews news) {
    if (fInputPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS))
      return news.getLink().toString();

    boolean openEmptyNews = Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS);
    if (openEmptyNews && !StringUtils.isSet(news.getDescription()))
      return news.getLink().toString();

    return news;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#dispose()
   */
  public void dispose() {
    unregisterListeners();
  }

  private void registerListener() {

    /* Listen on selection-changes */
    fSelectionListener = new ISelectionListener() {
      public void selectionChanged(IWorkbenchPart part, ISelection sel) {

        /* Only Track selections from the HeadlineControl */
        if (!part.equals(fEditorSite.getPart()))
          return;

        IStructuredSelection selection = (IStructuredSelection) sel;

        /* Restore Initial Input if selection is empty */
        if (selection.isEmpty())
          fViewer.setInput(fInitialInput);

        /* Set Elements as Input if 1 Item is selected */
        else if (selection.size() == 1)
          setPartInput(selection.getFirstElement());
      }
    };
    fEditorSite.getPage().addPostSelectionListener(fSelectionListener);

    /* Send Browser-Status to Workbench-Status */
    ((Browser) fViewer.getControl()).addStatusTextListener(new StatusTextListener() {
      public void changed(StatusTextEvent event) {

        /* Don't show Status for the Handler Protocol */
        if (event.text != null && !event.text.contains(ILinkHandler.HANDLER_PROTOCOL))
          fEditorSite.getActionBars().getStatusLineManager().setMessage(event.text);
      }
    });

    /* Control Browser's visibility based on the location */
    ((Browser) fViewer.getControl()).addLocationListener(new LocationAdapter() {
      @Override
      public void changing(LocationEvent event) {
        if (event.doit) {
          String loc = event.location;
          boolean visible = fViewer.getControl().getVisible();

          /* Make Browser visible now */
          if (!visible && StringUtils.isSet(loc) && !URIUtils.ABOUT_BLANK.equals(loc))
            fViewer.getControl().setVisible(true);
        }
      }
    });

    /* Refresh Browser when Font Changes */
    fFontPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (!fViewer.getControl().isDisposed() && OwlUI.NEWS_TEXT_FONT_ID.equals(event.getProperty()))
          ((Browser) fViewer.getControl()).refresh();
      }
    };
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fFontPropertyChangeListener);
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removePostSelectionListener(fSelectionListener);
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fFontPropertyChangeListener);
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setFocus()
   */
  public void setFocus() {
    fViewer.getControl().setFocus();
  }
}