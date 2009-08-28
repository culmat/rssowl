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

package org.rssowl.ui.internal.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.util.List;

/**
 * The <code>NavigationActionFactory</code> is providing a list of common
 * Actions to navigate to News-Items or Feeds.
 *
 * @author bpasero
 */
public class NavigationActionFactory implements IExecutableExtensionFactory, IExecutableExtension {
  private String fId;

  /** Actual Action for the Navigation */
  public static class NavigationAction implements IWorkbenchWindowActionDelegate {
    private final NavigationActionType fType;

    /**
     * @param type the type of Navigation.
     */
    public NavigationAction(NavigationActionType type) {
      fType = type;
    }

    public void dispose() {}

    public void init(IWorkbenchWindow window) {}

    public void run(IAction action) {

      /* Tab Navigation */
      if (fType == NavigationActionType.NEXT_TAB || fType == NavigationActionType.PREVIOUS_TAB) {
        navigateInTabs();
      }

      /* News/Feed Navigation */
      else {

        /* 1.) Navigate in opened FeedView */
        if (navigateOnActiveFeedView())
          return;

        /* 2.) Navigate in opened Explorer */
        if (navigateOnOpenExplorer())
          return;

        /* 3.) Navigate on entire Model */
        if (navigateOnModel())
          return;
      }
    }

    private void navigateInTabs() {

      /* Current Active Editor */
      IEditorPart activeEditor = OwlUI.getActiveEditor();
      if (activeEditor == null)
        return;

      List<IEditorReference> editors = OwlUI.getEditorReferences();

      int index = -1;
      for (int i = 0; i < editors.size(); i++) {
        try {
          if (activeEditor.getEditorInput().equals(editors.get(i).getEditorInput())) {
            index = i;
            break;
          }
        } catch (PartInitException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }

      if (index < 0)
        return;

      IEditorPart tab = null;

      /* Next Tab */
      if (fType == NavigationActionType.NEXT_TAB)
        tab = editors.get(index + 1 < editors.size() ? index + 1 : 0).getEditor(true);

      /* Previous Tab */
      else if (fType == NavigationActionType.PREVIOUS_TAB)
        tab = editors.get(index - 1 >= 0 ? index - 1 : editors.size() - 1).getEditor(true);

      /* Activate */
      if (tab != null) {
        IWorkbenchPage page = tab.getSite().getPage();
        page.activate(tab.getSite().getPart());
        page.activate(tab);
      }
    }

    private boolean navigateOnActiveFeedView() {

      /* Get active FeedView if any */
      FeedView activeFeedView = OwlUI.getActiveFeedView();

      /* Run on active FeedView if any */
      if (fType.isNewsScoped() && activeFeedView != null) {
        boolean success = activeFeedView.navigate(true, false, fType.isNext(), fType.isUnread());

        /* For unread news, consider all news of the active feed (see Bug 1064) */
        if (!success && fType.isNewsScoped() && fType.isUnread())
          success = activeFeedView.navigate(false, false, fType.isNext(), fType.isUnread());

        if (success) {
          IWorkbenchPage page = activeFeedView.getSite().getPage();
          page.activate(activeFeedView.getSite().getPart());
          page.activate(activeFeedView);

          return true;
        }
      }

      return false;
    }

    private boolean navigateOnOpenExplorer() {

      /* Try finding the open Explorer for BookMarks */
      BookMarkExplorer bookmarkExplorer = OwlUI.getOpenedBookMarkExplorer();
      if (bookmarkExplorer == null)
        return false;

      /* Navigate on Explorer */
      if (bookmarkExplorer.navigate(fType.isNewsScoped(), fType.isNext(), fType.isUnread()))
        return true;

      return false;
    }

    private boolean navigateOnModel() {
      // TODO Implement this!
      return false;
    }

    public void selectionChanged(IAction action, ISelection selection) {}
  }

  /** Enumeration with all possible types of NavigationAction */
  public enum NavigationActionType {

    /** Action: Go to the next News */
    NEXT_NEWS("nextNews", "org.rssowl.ui.NextNews", true, true, false),

    /** Action: Go to the next unread News */
    NEXT_UNREAD_NEWS("nextUnreadNews", "org.rssowl.ui.NextUnreadNews", true, true, true),

    /** Action: Go to the next Feed */
    NEXT_FEED("nextFeed", "org.rssowl.ui.NextFeed", false, true, false),

    /** Action: Go to the next unread Feed */
    NEXT_UNREAD_FEED("nextUnreadFeed", "org.rssowl.ui.NextUnreadFeed", false, true, true),

    /** Action: Go to the previous News */
    PREVIOUS_NEWS("previousNews", "org.rssowl.ui.PreviousNews", true, false, false),

    /** Action: Go to the previous unread News */
    PREVIOUS_UNREAD_NEWS("previousUnreadNews", "org.rssowl.ui.PreviousUnreadNews", true, false, true),

    /** Action: Go to the previous Feed */
    PREVIOUS_FEED("previousFeed", "org.rssowl.ui.PreviousFeed", false, false, false),

    /** Action: Go to the previous unread Feed */
    PREVIOUS_UNREAD_FEED("previousUnreadFeed", "org.rssowl.ui.PreviousUnreadFeed", false, false, true),

    /** Action: Go to next Tab */
    NEXT_TAB("nextTab", "org.rssowl.ui.NextTab", false, false, false),

    /** Action: Go to previous Tab */
    PREVIOUS_TAB("previousTab", "org.rssowl.ui.PreviousTab", false, false, false);

    String fId;
    boolean fIsNewsScoped;
    boolean fIsNext;
    boolean fIsUnread;
    String fCommandId;

    NavigationActionType(String id, String commandId, boolean isNewsScoped, boolean isNext, boolean isUnread) {
      fId = id;
      fCommandId = commandId;
      fIsNewsScoped = isNewsScoped;
      fIsNext = isNext;
      fIsUnread = isUnread;
    }

    /**
     * @return the id of this navigation action.
     */
    public String getId() {
      return fId;
    }

    /**
     * @return the id of the command for this navigation action.
     */
    public String getCommandId() {
      return fCommandId;
    }

    boolean isNewsScoped() {
      return fIsNewsScoped;
    }

    boolean isUnread() {
      return fIsUnread;
    }

    boolean isNext() {
      return fIsNext;
    }
  };

  /** Keep for reflection */
  public NavigationActionFactory() {}

  /*
   * @see org.eclipse.core.runtime.IExecutableExtensionFactory#create()
   */
  public Object create() {
    if (NavigationActionType.NEXT_NEWS.getId().equals(fId))
      return new NavigationAction(NavigationActionType.NEXT_NEWS);

    if (NavigationActionType.NEXT_UNREAD_NEWS.getId().equals(fId))
      return new NavigationAction(NavigationActionType.NEXT_UNREAD_NEWS);

    if (NavigationActionType.NEXT_FEED.getId().equals(fId))
      return new NavigationAction(NavigationActionType.NEXT_FEED);

    if (NavigationActionType.NEXT_UNREAD_FEED.getId().equals(fId))
      return new NavigationAction(NavigationActionType.NEXT_UNREAD_FEED);

    if (NavigationActionType.PREVIOUS_NEWS.getId().equals(fId))
      return new NavigationAction(NavigationActionType.PREVIOUS_NEWS);

    if (NavigationActionType.PREVIOUS_UNREAD_NEWS.getId().equals(fId))
      return new NavigationAction(NavigationActionType.PREVIOUS_UNREAD_NEWS);

    if (NavigationActionType.PREVIOUS_FEED.getId().equals(fId))
      return new NavigationAction(NavigationActionType.PREVIOUS_FEED);

    if (NavigationActionType.PREVIOUS_UNREAD_FEED.getId().equals(fId))
      return new NavigationAction(NavigationActionType.PREVIOUS_UNREAD_FEED);

    if (NavigationActionType.NEXT_TAB.getId().equals(fId))
      return new NavigationAction(NavigationActionType.NEXT_TAB);

    if (NavigationActionType.PREVIOUS_TAB.getId().equals(fId))
      return new NavigationAction(NavigationActionType.PREVIOUS_TAB);

    return null;
  }

  /*
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
   * java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
    if (data instanceof String)
      fId = (String) data;
    else
      throw new CoreException(Activator.getDefault().createErrorStatus("Data argument must be a String for " + getClass(), null));
  }
}