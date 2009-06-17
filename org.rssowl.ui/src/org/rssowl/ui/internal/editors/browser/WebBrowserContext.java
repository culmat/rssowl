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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;

/**
 * The context from which the webbrowser was created.
 *
 * @author bpasero
 */
public class WebBrowserContext {
  private NewsReference fNewsReference;
  private ModelReference fNewsMarkReference;

  private WebBrowserContext() {}

  /**
   * @param news the news that is opened in the browser.
   * @param mark the news mark where the news is from.
   * @return a new instance of {@link WebBrowserContext} from the given input.
   */
  public static WebBrowserContext createFrom(INews news, INewsMark mark) {
    WebBrowserContext context = new WebBrowserContext();
    if (news == null || mark == null)
      return context;

    context.fNewsReference = news.toReference();

    if (mark instanceof FolderNewsMark)
      context.fNewsMarkReference = new FolderReference(mark.getId());
    else
      context.fNewsMarkReference = mark.toReference();

    return context;
  }

  /**
   * @param selection the selected news of the given feedview.
   * @param feedview the feedview from which the browser is opened.
   * @return a new instance of {@link WebBrowserContext} from the given input.
   */
  public static WebBrowserContext createFrom(ISelection selection, FeedView feedview) {
    WebBrowserContext context = new WebBrowserContext();
    if (selection == null || feedview == null)
      return context;

    context.fNewsReference = getNewsReference(selection);

    INewsMark mark = ((FeedViewInput) feedview.getEditorInput()).getMark();
    if (mark instanceof FolderNewsMark)
      context.fNewsMarkReference = new FolderReference(mark.getId());
    else
      context.fNewsMarkReference = mark.toReference();

    return context;
  }

  private static NewsReference getNewsReference(ISelection selection) {
    if (selection instanceof StructuredSelection) {
      Object element = ((StructuredSelection) selection).getFirstElement();
      if (element instanceof INews)
        return ((INews) element).toReference();
    }

    return null;
  }

  /**
   * @return news the news that is opened in the browser or <code>null</code> if
   * none.
   */
  public NewsReference getNewsReference() {
    return fNewsReference;
  }

  /**
   * @return the news mark where the news is from or <code>null</code> if none.
   */
  public INewsMark getNewsMark() {
    if (fNewsMarkReference == null)
      return null;

    if (fNewsMarkReference instanceof BookMarkReference)
      return (IBookMark) fNewsMarkReference.resolve();

    if (fNewsMarkReference instanceof SearchMarkReference)
      return (ISearchMark) fNewsMarkReference.resolve();

    if (fNewsMarkReference instanceof NewsBinReference)
      return (INewsBin) fNewsMarkReference.resolve();

    if (fNewsMarkReference instanceof FolderReference)
      return new FolderNewsMark((IFolder) fNewsMarkReference.resolve());

    return null;
  }
}