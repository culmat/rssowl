/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.keys.IBindingService;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.actions.AssignLabelsAction;
import org.rssowl.ui.internal.actions.AutomateFilterAction;
import org.rssowl.ui.internal.actions.CopyLinkAction;
import org.rssowl.ui.internal.actions.CreateFilterAction;
import org.rssowl.ui.internal.actions.FindAction;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.RedoAction;
import org.rssowl.ui.internal.actions.ReloadAllAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.SearchNewsAction;
import org.rssowl.ui.internal.actions.SendLinkAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.actions.UndoAction;
import org.rssowl.ui.internal.actions.CreateFilterAction.PresetAction;
import org.rssowl.ui.internal.dialogs.CustomizeToolbarDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog.DialogMode;
import org.rssowl.ui.internal.dialogs.preferences.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.dialogs.preferences.NotifierPreferencesPage;
import org.rssowl.ui.internal.dialogs.preferences.OverviewPreferencesPage;
import org.rssowl.ui.internal.dialogs.preferences.SharingPreferencesPage;
import org.rssowl.ui.internal.dialogs.welcome.TutorialWizard;
import org.rssowl.ui.internal.editors.browser.WebBrowserContext;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.handler.TutorialHandler;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;
import org.rssowl.ui.internal.views.explorer.BookMarkFilter;
import org.rssowl.ui.internal.views.explorer.BookMarkFilter.Type;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

  /** Name of the "Manage Extensions" SubMenu */
  public static final String M_MANAGE_EXTENSIONS = "manageExtensions"; //$NON-NLS-1$

  /** Name of the View Top Menu */
  public static final String M_VIEW = "view"; //$NON-NLS-1$

  /** Start of the View Top Menu */
  public static final String M_VIEW_START = "viewStart"; //$NON-NLS-1$

  /** End of the View Top Menu */
  public static final String M_VIEW_END = "viewEnd"; //$NON-NLS-1$

  /* Local Resource Manager (lives across entire application life) */
  private static ResourceManager fgResources = new LocalResourceManager(JFaceResources.getResources());

  private CoolBarAdvisor fCoolBarAdvisor;
  private IContributionItem fOpenWindowsItem;
  private IContributionItem fReopenEditors;
  private FindAction fFindAction;

  /**
   * @param configurer
   */
  public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
    super(configurer);
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#makeActions(org.eclipse.ui.IWorkbenchWindow)
   */
  @Override
  protected void makeActions(IWorkbenchWindow window) {

    /* Menu: File */
    register(ActionFactory.SAVE_AS.create(window));
    register(ActionFactory.CLOSE.create(window));
    register(ActionFactory.CLOSE_ALL.create(window));
    register(ActionFactory.PRINT.create(window));
    register(ActionFactory.QUIT.create(window));

    fReopenEditors = ContributionItemFactory.REOPEN_EDITORS.create(window);

    /* Menu: Edit */
    register(ActionFactory.CUT.create(window));
    register(ActionFactory.COPY.create(window));
    register(ActionFactory.PASTE.create(window));
    register(ActionFactory.DELETE.create(window));
    register(ActionFactory.SELECT_ALL.create(window));
    register(ActionFactory.PROPERTIES.create(window));

    fFindAction = new FindAction();
    register(fFindAction);

    /* Menu: Tools */
    register(ActionFactory.PREFERENCES.create(window));

    /* Menu: Window */
    register(ActionFactory.OPEN_NEW_WINDOW.create(window));
    getAction(ActionFactory.OPEN_NEW_WINDOW.getId()).setText(Messages.ApplicationActionBarAdvisor_NEW_WINDOW);
    fOpenWindowsItem = ContributionItemFactory.OPEN_WINDOWS.create(window);

    //    register(ActionFactory.TOGGLE_COOLBAR.create(window));
    //    register(ActionFactory.RESET_PERSPECTIVE.create(window));
    //    register(ActionFactory.EDIT_ACTION_SETS.create(window));
    //    register(ActionFactory.ACTIVATE_EDITOR.create(window));
    //    register(ActionFactory.MAXIMIZE.create(window));
    //    register(ActionFactory.MINIMIZE.create(window));
    //    register(ActionFactory.NEXT_EDITOR.create(window));
    //    register(ActionFactory.PREVIOUS_EDITOR.create(window));
    //    register(ActionFactory.PREVIOUS_PART.create(window));
    //    register(ActionFactory.NEXT_PART.create(window));
    //    register(ActionFactory.SHOW_EDITOR.create(window));

    /* Menu: Help */
    // register(ActionFactory.INTRO.create(window));
    register(ActionFactory.ABOUT.create(window));
    getAction(ActionFactory.ABOUT.getId()).setText(Messages.ApplicationActionBarAdvisor_ABOUT_RSSOWL);

    /* CoolBar: Contextual Menu */
    register(ActionFactory.LOCK_TOOL_BAR.create(window));
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
   */
  @Override
  protected void fillMenuBar(IMenuManager menuBar) {

    /* File Menu */
    createFileMenu(menuBar);

    /* Edit Menu */
    createEditMenu(menuBar);

    /* View Menu */
    createViewMenu(menuBar);

    /* Go Menu */
    createGoMenu(menuBar);

    /* Bookmarks Menu */
    createBookMarksMenu(menuBar);

    /* News Menu */
    createNewsMenu(menuBar);

    /* Allow Top-Level Menu Contributions here */
    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    /* Menu: Tools */
    createToolsMenu(menuBar);

    /* Window Menu */
    createWindowMenu(menuBar);

    /* Menu: Help */
    createHelpMenu(menuBar);
  }

  /* Menu: File */
  private void createFileMenu(IMenuManager menuBar) {
    MenuManager fileMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_FILE, IWorkbenchActionConstants.M_FILE);
    menuBar.add(fileMenu);

    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.CLOSE.getId()));
    fileMenu.add(getAction(ActionFactory.CLOSE_ALL.getId()));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
    fileMenu.add(new Separator());
    fileMenu.add(getAction(ActionFactory.SAVE_AS.getId()));
    fileMenu.add(new Separator());
    fileMenu.add(getAction(ActionFactory.PRINT.getId()));

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.QUIT.getId()));
  }

  /* Menu: Edit */
  private void createEditMenu(IMenuManager menuBar) {
    MenuManager editMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_EDIT, IWorkbenchActionConstants.M_EDIT);
    editMenu.add(getAction(ActionFactory.COPY.getId())); //Dummy action
    menuBar.add(editMenu);

    editMenu.setRemoveAllWhenShown(true);
    editMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager editMenu) {
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
        editMenu.add(new Separator());

        editMenu.add(new UndoAction());
        editMenu.add(new RedoAction());
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
        editMenu.add(new Separator());

        editMenu.add(getAction(ActionFactory.CUT.getId()));
        editMenu.add(getAction(ActionFactory.COPY.getId()));
        editMenu.add(new CopyLinkAction());
        editMenu.add(getAction(ActionFactory.PASTE.getId()));
        editMenu.add(new Separator());
        editMenu.add(getAction(ActionFactory.DELETE.getId()));
        editMenu.add(getAction(ActionFactory.SELECT_ALL.getId()));

        editMenu.add(new Separator());

        editMenu.add(new SearchNewsAction(OwlUI.getWindow()));
        editMenu.add(fFindAction);

        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
        editMenu.add(new Separator());

        editMenu.add(getAction(ActionFactory.PROPERTIES.getId()));
      }
    });
  }

  /* Menu: View */
  private void createViewMenu(IMenuManager menuBar) {
    final IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();
    final IPreferenceScope eclipsePrefs = Owl.getPreferenceService().getEclipseScope();

    MenuManager viewMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_VIEW, M_VIEW);
    viewMenu.setRemoveAllWhenShown(true);
    menuBar.add(viewMenu);

    /* Add dummy action to show the top level menu */
    viewMenu.add(new Action("") { //$NON-NLS-1$
          @Override
          public void run() {}
        });

    /* Build Menu dynamically */
    viewMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        manager.add(new GroupMarker(M_VIEW_START));

        /* Layout */
        MenuManager layoutMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_LAYOUT);
        layoutMenu.add(new Action(Messages.ApplicationActionBarAdvisor_CLASSIC_VIEW, IAction.AS_RADIO_BUTTON) {
          @Override
          public void run() {
            preferences.putBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC, true);
            preferences.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, false);
            List<FeedView> feedViews = OwlUI.getFeedViews();
            for (FeedView feedView : feedViews) {
              feedView.updateLayout();
            }
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC) && !preferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
          }
        });

        layoutMenu.add(new Action(Messages.ApplicationActionBarAdvisor_VERTICAL_VIEW, IAction.AS_RADIO_BUTTON) {
          @Override
          public void run() {
            preferences.putBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC, false);
            preferences.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, false);
            List<FeedView> feedViews = OwlUI.getFeedViews();
            for (FeedView feedView : feedViews) {
              feedView.updateLayout();
            }
          }

          @Override
          public boolean isChecked() {
            return !preferences.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC) && !preferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
          }
        });

        layoutMenu.add(new Action(Messages.ApplicationActionBarAdvisor_NEWSPAPER_VIEW, IAction.AS_RADIO_BUTTON) {
          @Override
          public void run() {
            preferences.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, true);
            List<FeedView> feedViews = OwlUI.getFeedViews();
            for (FeedView feedView : feedViews) {
              feedView.updateLayout();
            }
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
          }
        });

        layoutMenu.add(new Separator());
        layoutMenu.add(new Action(Messages.ApplicationActionBarAdvisor_TABBED_BROWSING, IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            boolean tabbedBrowsingEnabled = isChecked();

            /* Disable Tabbed Browsing */
            if (tabbedBrowsingEnabled) {

              /* Close other Tabs if necessary */
              boolean doit = true;
              IWorkbenchPage page = OwlUI.getPage();
              if (page != null) {
                IEditorReference[] editorReferences = page.getEditorReferences();
                if (editorReferences.length > 1) {
                  MessageBox confirmDialog = new MessageBox(page.getWorkbenchWindow().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                  confirmDialog.setText(Messages.ApplicationActionBarAdvisor_DISABLE_TABBED_BROWSING);
                  confirmDialog.setMessage(NLS.bind(Messages.ApplicationActionBarAdvisor_TABS_MESSAGE, editorReferences.length));
                  if (confirmDialog.open() == SWT.YES)
                    OwlUI.closeOtherEditors();
                  else
                    doit = false;
                }
              }

              /* Update Preferences */
              if (doit) {
                eclipsePrefs.putBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS, false);
                eclipsePrefs.putBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS, true);
                eclipsePrefs.putInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, 1);
              }
            }

            /* Enable Tabbed Browsing */
            else {
              eclipsePrefs.putBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS, true);
              eclipsePrefs.putBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS, false);
              eclipsePrefs.putInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, 5);
            }
          }

          @Override
          public boolean isChecked() {
            boolean autoCloseTabs = eclipsePrefs.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS);
            int autoCloseTabsThreshold = eclipsePrefs.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD);
            return !autoCloseTabs || autoCloseTabsThreshold > 1;
          }
        });

        manager.add(layoutMenu);
        manager.add(new Separator());

        /* Toggle State of Toolbar Visibility */
        manager.add(new Action(Messages.ApplicationActionBarAdvisor_TOOLBAR, IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;

            boolean isToolBarVisible = preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR);
            configurer.setToolBarVisible(!isToolBarVisible);
            preferences.putBoolean(DefaultPreferences.SHOW_TOOLBAR, !isToolBarVisible);
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR);
          }
        });

        /* Toggle State of Status Bar Visibility */
        manager.add(new Action(Messages.ApplicationActionBarAdvisor_STATUS, IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;

            boolean isStatusVisible = preferences.getBoolean(DefaultPreferences.SHOW_STATUS);
            configurer.setStatusVisible(!isStatusVisible, true);
            preferences.putBoolean(DefaultPreferences.SHOW_STATUS, !isStatusVisible);
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.SHOW_STATUS);
          }
        });

        /* Toggle State of Bookmarks Visibility */
        manager.add(new Action(Messages.ApplicationActionBarAdvisor_BOOKMARKS, IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            OwlUI.toggleBookmarks();
          }

          @Override
          public String getActionDefinitionId() {
            return "org.rssowl.ui.ToggleBookmarksCommand"; //$NON-NLS-1$
          }

          @Override
          public String getId() {
            return "org.rssowl.ui.ToggleBookmarksCommand"; //$NON-NLS-1$
          }

          @Override
          public boolean isChecked() {
            IWorkbenchPage page = OwlUI.getPage();
            if (page != null)
              return page.findView(BookMarkExplorer.VIEW_ID) != null;

            return false;
          }
        });

        /* Customize Toolbar */
        manager.add(new Separator());
        manager.add(new Action(Messages.ApplicationActionBarAdvisor_CUSTOMIZE_TOOLBAR) {
          @Override
          public void run() {

            /* Unhide Toolbar if hidden */
            ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;

            boolean isToolBarVisible = preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR);
            if (!isToolBarVisible) {
              configurer.setToolBarVisible(true);
              preferences.putBoolean(DefaultPreferences.SHOW_TOOLBAR, true);
            }

            /* Open Dialog to Customize Toolbar */
            CustomizeToolbarDialog dialog = new CustomizeToolbarDialog(getActionBarConfigurer().getWindowConfigurer().getWindow().getShell());
            if (dialog.open() == IDialogConstants.OK_ID)
              fCoolBarAdvisor.advise(true);
          }
        });

        /* Fullscreen Mode */
        manager.add(new Separator());
        manager.add(new Action(Messages.ApplicationActionBarAdvisor_FULL_SCREEN, IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            OwlUI.toggleFullScreen();
          }

          @Override
          public String getActionDefinitionId() {
            return "org.rssowl.ui.FullScreenCommand"; //$NON-NLS-1$
          }

          @Override
          public String getId() {
            return "org.rssowl.ui.FullScreenCommand"; //$NON-NLS-1$
          }

          @Override
          public boolean isChecked() {
            Shell shell = OwlUI.getActiveShell();
            if (shell != null)
              return shell.getFullScreen();

            return super.isChecked();
          }
        });

        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new GroupMarker(M_VIEW_START));
      }
    });
  }

  /* Menu: Go */
  private void createGoMenu(IMenuManager menuBar) {
    MenuManager goMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_GO, IWorkbenchActionConstants.M_NAVIGATE);
    menuBar.add(goMenu);

    goMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    goMenu.add(fReopenEditors);
  }

  /* Menu: Bookmarks */
  private void createBookMarksMenu(IMenuManager menuBar) {
    MenuManager bmMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_BOOKMARKS, "bookmarks"); //$NON-NLS-1$
    bmMenu.setRemoveAllWhenShown(true);
    bmMenu.add(new Action("") {}); //Dummy Action //$NON-NLS-1$
    bmMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        fillBookMarksMenu(manager, getActionBarConfigurer().getWindowConfigurer().getWindow());
        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });

    menuBar.add(bmMenu);
  }

  /* Menu News */
  private void createNewsMenu(IMenuManager menuBar) {
    final IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();

    final MenuManager newsMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_NEWS, "news"); //$NON-NLS-1$
    menuBar.add(newsMenu);
    newsMenu.setRemoveAllWhenShown(true);

    newsMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        final IStructuredSelection selection;

        FeedView activeFeedView = OwlUI.getActiveFeedView();
        FeedViewInput activeInput = null;
        if (activeFeedView != null) {
          selection = (IStructuredSelection) activeFeedView.getSite().getSelectionProvider().getSelection();
          activeInput = (FeedViewInput) activeFeedView.getEditorInput();
        } else
          selection = StructuredSelection.EMPTY;

        /* Open */
        {
          manager.add(new Separator("open")); //$NON-NLS-1$

          /* Open News in Browser */
          manager.add(new OpenInBrowserAction(selection, WebBrowserContext.createFrom(selection, activeFeedView)) {
            @Override
            public boolean isEnabled() {
              return !selection.isEmpty();
            }
          });

          /* Open Externally - Show only when internal browser is used */
          if (!selection.isEmpty() && !preferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER) && !preferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
            manager.add(new OpenInExternalBrowserAction(selection));
        }

        /* Attachments */
        {
          fillAttachmentsMenu(manager, selection, getActionBarConfigurer().getWindowConfigurer().getWindow(), false);
        }

        /* Mark / Label */
        {
          manager.add(new Separator("mark")); //$NON-NLS-1$

          /* Mark */
          {
            MenuManager markMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_MARK, "mark"); //$NON-NLS-1$
            manager.add(markMenu);

            /* Mark as Read */
            IAction action = new ToggleReadStateAction(selection);
            action.setEnabled(!selection.isEmpty());
            markMenu.add(action);

            /* Mark All Read */
            action = new MarkAllNewsReadAction();
            action.setEnabled(activeFeedView != null);
            markMenu.add(action);

            /* Sticky */
            markMenu.add(new Separator());
            action = new MakeNewsStickyAction(selection);
            action.setEnabled(!selection.isEmpty());
            markMenu.add(action);
          }

          /* Label */
          fillLabelMenu(manager, selection, getActionBarConfigurer().getWindowConfigurer().getWindow(), false);
        }

        /* Move To / Copy To */
        if (!selection.isEmpty()) {
          manager.add(new Separator("movecopy")); //$NON-NLS-1$

          /* Load all news bins and sort by name */
          List<INewsBin> newsbins = new ArrayList<INewsBin>(DynamicDAO.loadAll(INewsBin.class));

          Comparator<INewsBin> comparator = new Comparator<INewsBin>() {
            public int compare(INewsBin o1, INewsBin o2) {
              return o1.getName().compareTo(o2.getName());
            };
          };

          Collections.sort(newsbins, comparator);

          /* Move To */
          MenuManager moveMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_MOVE_TO, "moveto"); //$NON-NLS-1$
          manager.add(moveMenu);

          for (INewsBin bin : newsbins) {
            if (activeInput != null && activeInput.getMark().equals(bin))
              continue;

            moveMenu.add(new MoveCopyNewsToBinAction(selection, bin, true));
          }

          moveMenu.add(new Separator("movetonewbin")); //$NON-NLS-1$
          moveMenu.add(new MoveCopyNewsToBinAction(selection, null, true));
          moveMenu.add(new AutomateFilterAction(PresetAction.MOVE, selection));

          /* Copy To */
          MenuManager copyMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_COPY_TO, "copyto"); //$NON-NLS-1$
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            if (activeInput != null && activeInput.getMark().equals(bin))
              continue;

            copyMenu.add(new MoveCopyNewsToBinAction(selection, bin, false));
          }

          copyMenu.add(new Separator("copytonewbin")); //$NON-NLS-1$
          copyMenu.add(new MoveCopyNewsToBinAction(selection, null, false));
          copyMenu.add(new AutomateFilterAction(PresetAction.COPY, selection));
        }

        /* Share */
        fillShareMenu(manager, selection, getActionBarConfigurer().getWindowConfigurer().getWindow(), false);

        /* Filter */
        if (!selection.isEmpty()) {
          manager.add(new Separator("filter")); //$NON-NLS-1$

          /* Create Filter */
          manager.add(new Action(Messages.ApplicationActionBarAdvisor_CREATE_FILTER) {
            @Override
            public void run() {
              CreateFilterAction action = new CreateFilterAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
              return OwlUI.FILTER;
            }
          });
        }

        /* Update */
        {
          manager.add(new Separator("reload")); //$NON-NLS-1$

          /* Update */
          manager.add(new Action(Messages.ApplicationActionBarAdvisor_UPDATE) {
            @Override
            public void run() {
              IActionDelegate action = new ReloadTypesAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
              return OwlUI.getImageDescriptor("icons/elcl16/reload.gif"); //$NON-NLS-1$
            }

            @Override
            public ImageDescriptor getDisabledImageDescriptor() {
              return OwlUI.getImageDescriptor("icons/dlcl16/reload.gif"); //$NON-NLS-1$
            }

            @Override
            public boolean isEnabled() {
              return !selection.isEmpty() || OwlUI.getActiveFeedView() != null;
            }

            @Override
            public String getActionDefinitionId() {
              return ReloadTypesAction.ID;
            }

            @Override
            public String getId() {
              return ReloadTypesAction.ID;
            }
          });

          /* Update All */
          manager.add(new ReloadAllAction());
        }

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });
  }

  /* Menu: Tools */
  private void createToolsMenu(IMenuManager menuBar) {
    MenuManager toolsMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_TOOLS, OwlUI.M_TOOLS);
    menuBar.add(toolsMenu);

    /* Contributions */
    toolsMenu.add(new GroupMarker("begin")); //$NON-NLS-1$
    toolsMenu.add(new Separator());
    toolsMenu.add(new GroupMarker("middle")); //$NON-NLS-1$
    toolsMenu.add(new Separator());
    toolsMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    toolsMenu.add(new Separator());
    toolsMenu.add(new GroupMarker("end")); //$NON-NLS-1$
    toolsMenu.add(new Separator());

    /* Preferences */
    IAction preferences = getAction(ActionFactory.PREFERENCES.getId());
    preferences.setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/preferences.gif")); //$NON-NLS-1$
    toolsMenu.add(preferences);
    if (Application.IS_MAC) {
      IContributionItem item = toolsMenu.find(ActionFactory.PREFERENCES.getId());
      if (item != null)
        item.setVisible(false);
    }
  }

  /* Menu: Window */
  private void createWindowMenu(IMenuManager menuBar) {
    MenuManager windowMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_WINDOW, IWorkbenchActionConstants.M_WINDOW);
    menuBar.add(windowMenu);

    IAction openNewWindowAction = getAction(ActionFactory.OPEN_NEW_WINDOW.getId());
    openNewWindowAction.setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/newwindow.gif")); //$NON-NLS-1$
    windowMenu.add(openNewWindowAction);

    windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    windowMenu.add(fOpenWindowsItem);

    //    MenuManager showViewMenu = new MenuManager("&Show View");
    //    windowMenu.add(showViewMenu);
    //    showViewMenu.add(fShowViewMenu);
    //    windowMenu.add(new Separator());

    //    windowMenu.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
    //    windowMenu.add(getAction(ActionFactory.RESET_PERSPECTIVE.getId()));
    //    windowMenu.add(new Separator());
    //
    //    MenuManager navigationMenu = new MenuManager("&Navigation");
    //    windowMenu.add(navigationMenu);
    //
    //    navigationMenu.add(getAction(ActionFactory.SHOW_PART_PANE_MENU.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_VIEW_MENU.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.MAXIMIZE.getId()));
    //    navigationMenu.add(getAction(ActionFactory.MINIMIZE.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.ACTIVATE_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.NEXT_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.PREVIOUS_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_OPEN_EDITORS.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_WORKBOOK_EDITORS.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.NEXT_PART.getId()));
    //    navigationMenu.add(getAction(ActionFactory.PREVIOUS_PART.getId()));
  }

  /* Menu: Help */
  private void createHelpMenu(IMenuManager menuBar) {
    MenuManager helpMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_HELP, IWorkbenchActionConstants.M_HELP);
    menuBar.add(helpMenu);

    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));

    /* Tutorial Wizard */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_TUTORIAL) {
      @Override
      public void run() {
        TutorialWizard wizard = new TutorialWizard();
        OwlUI.openWizard(getActionBarConfigurer().getWindowConfigurer().getWindow().getShell(), wizard, false, false, null);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/help.gif"); //$NON-NLS-1$
      }

      @Override
      public String getId() {
        return TutorialHandler.ID;
      }

      @Override
      public String getActionDefinitionId() {
        return TutorialHandler.ID;
      }
    });

    /* Link to boreal.rssowl.org */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_FAQ) {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://boreal.rssowl.org/#faq"); //$NON-NLS-1$
      }
    });

    /* Show Key Bindings */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_SHOW_KEY_BINDINGS) {
      @Override
      public void run() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IBindingService bindingService = (IBindingService) workbench.getService(IBindingService.class);
        bindingService.openKeyAssistDialog();
      }
    });

    helpMenu.add(new Separator());

    /* Report Bugs */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_REPORT_PROBLEMS) {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://dev.rssowl.org"); //$NON-NLS-1$
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/bug.gif"); //$NON-NLS-1$
      }
    });

    /* Export Log to File */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_EXPORT_LOGFILE) {
      @Override
      public void run() {
        FileDialog dialog = new FileDialog(getActionBarConfigurer().getWindowConfigurer().getWindow().getShell(), SWT.SAVE);
        dialog.setText(Messages.ApplicationActionBarAdvisor_EXPORT_LOGFILE_DIALOG);
        dialog.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
        dialog.setFileName("rssowl.log"); //$NON-NLS-1$
        dialog.setOverwrite(true);

        String file = dialog.open();
        if (StringUtils.isSet(file)) {
          try {
            File logFile = Platform.getLogFileLocation().toFile();
            InputStream inS;
            if (logFile.exists())
              inS = new FileInputStream(logFile);
            else
              inS = new ByteArrayInputStream(new byte[0]);
            FileOutputStream outS = new FileOutputStream(new File(file));
            CoreUtils.copy(inS, outS);
          } catch (FileNotFoundException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      }
    });

    helpMenu.add(new Separator());

    /* Homepage */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_HOMEPAGE) {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://www.rssowl.org"); //$NON-NLS-1$
      }
    });

    /* License */
    helpMenu.add(new Action(Messages.ApplicationActionBarAdvisor_LICENSE) {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://www.rssowl.org/legal/epl-v10.html"); //$NON-NLS-1$
      }
    });

    // helpMenu.add(getAction(ActionFactory.INTRO.getId()));
    helpMenu.add(new Separator());

    helpMenu.add(new Separator());
    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
    helpMenu.add(new Separator());

    helpMenu.add(getAction(ActionFactory.ABOUT.getId()));
    if (Application.IS_MAC) {
      IContributionItem item = helpMenu.find(ActionFactory.ABOUT.getId());
      if (item != null)
        item.setVisible(false);
    }
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillStatusLine(org.eclipse.jface.action.IStatusLineManager)
   */
  @Override
  protected void fillStatusLine(IStatusLineManager statusLine) {
    super.fillStatusLine(statusLine);
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillActionBars(int)
   */
  @Override
  public void fillActionBars(int flags) {
    super.fillActionBars(flags);
  }

  /**
   * @param trayItem
   * @param shell
   * @param advisor
   */
  protected void fillTrayItem(IMenuManager trayItem, final Shell shell, final ApplicationWorkbenchWindowAdvisor advisor) {
    trayItem.add(new ReloadAllAction(false));
    trayItem.add(new Separator());

    trayItem.add(new Action(Messages.ApplicationActionBarAdvisor_CONFIGURE_NOTIFICATIONS) {
      @Override
      public void run() {
        advisor.restoreFromTray(shell);
        PreferencesUtil.createPreferenceDialogOn(shell, NotifierPreferencesPage.ID, null, null).open();
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/notification.gif"); //$NON-NLS-1$
      }
    });

    trayItem.add(new Action(Messages.ApplicationActionBarAdvisor_PREFERENCES) {
      @Override
      public void run() {
        advisor.restoreFromTray(shell);
        PreferencesUtil.createPreferenceDialogOn(shell, OverviewPreferencesPage.ID, null, null).open();
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/preferences.gif"); //$NON-NLS-1$
      }
    });

    trayItem.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    trayItem.add(getAction(ActionFactory.QUIT.getId()));
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillCoolBar(org.eclipse.jface.action.ICoolBarManager)
   */
  @Override
  protected void fillCoolBar(ICoolBarManager coolBar) {

    /* CoolBar Context Menu */
    MenuManager coolBarContextMenuManager = new MenuManager(null, "org.rssowl.ui.CoolBarContextMenu"); //$NON-NLS-1$
    coolBar.setContextMenuManager(coolBarContextMenuManager);

    /* Customize Coolbar */
    coolBarContextMenuManager.add(new Action(Messages.ApplicationActionBarAdvisor_CUSTOMIZE_TOOLBAR) {
      @Override
      public void run() {
        CustomizeToolbarDialog dialog = new CustomizeToolbarDialog(getActionBarConfigurer().getWindowConfigurer().getWindow().getShell());
        if (dialog.open() == IDialogConstants.OK_ID)
          fCoolBarAdvisor.advise(true);
      }
    });

    /* Lock Coolbar  */
    coolBarContextMenuManager.add(new Separator());
    IAction lockToolbarAction = getAction(ActionFactory.LOCK_TOOL_BAR.getId());
    lockToolbarAction.setText(Messages.ApplicationActionBarAdvisor_LOCK_TOOLBAR);
    coolBarContextMenuManager.add(lockToolbarAction);

    /* Toggle State of Toolbar Visibility */
    coolBarContextMenuManager.add(new Action(Messages.ApplicationActionBarAdvisor_HIDE_TOOLBAR) {
      @Override
      public void run() {
        ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
        configurer.setToolBarVisible(false);
        Owl.getPreferenceService().getGlobalScope().putBoolean(DefaultPreferences.SHOW_TOOLBAR, false);
      }
    });

    /* Support for more Contributions */
    coolBarContextMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    /* Coolbar Advisor */
    fCoolBarAdvisor = new CoolBarAdvisor(coolBar, getActionBarConfigurer().getWindowConfigurer().getWindow());
    fCoolBarAdvisor.advise();
  }

  /**
   * @param manager the {@link IMenuManager} to fill this menu into.
   * @param selection the current {@link IStructuredSelection} of {@link INews}.
   * @param shellProvider a {@link IShellProvider} for dialogs.
   * @param directMenu if <code>true</code> directly fill all items to the menu,
   * otherwise create a sub menu.
   */
  public static void fillAttachmentsMenu(IMenuManager manager, final IStructuredSelection selection, final IShellProvider shellProvider, boolean directMenu) {
    final List<Pair<IAttachment, URI>> attachments = ModelUtils.getAttachmentLinks(selection);
    if (!attachments.isEmpty()) {
      manager.add(new Separator("attachments")); //$NON-NLS-1$

      /* Either as direct Menu or Submenu */
      IMenuManager attachmentMenu;
      if (directMenu)
        attachmentMenu = manager;
      else {
        attachmentMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_ATTACHMENTS, "attachments"); //$NON-NLS-1$
        manager.add(attachmentMenu);
      }

      /* Offer to Download All */
      if (attachments.size() > 1) {
        int sumBytes = 0;
        for (Pair<IAttachment, URI> attachment : attachments) {
          if (attachment.getFirst().getLength() > 0) {
            sumBytes += attachment.getFirst().getLength();
          } else {
            sumBytes = 0;
            break;
          }
        }
        String sumSize = OwlUI.getSize(sumBytes);

        attachmentMenu.add(new Action(sumSize != null ? (NLS.bind(Messages.ApplicationActionBarAdvisor_DOWNLOAD_ALL_WITH_SIZE, sumSize)) : (Messages.ApplicationActionBarAdvisor_DOWNLOAD_ALL)) {
          @Override
          public void run() {
            DirectoryDialog dialog = new DirectoryDialog(shellProvider.getShell(), SWT.None);
            dialog.setText(Messages.ApplicationActionBarAdvisor_SELECT_FOLDER_FOR_DOWNLOADS);
            String folder = dialog.open();
            if (StringUtils.isSet(folder)) {
              for (Pair<IAttachment, URI> attachment : attachments) {
                Controller.getDefault().getDownloadService().download(attachment.getFirst(), attachment.getSecond(), new File(folder), true);
              }
            }
          }
        });
        attachmentMenu.add(new Separator());
      }

      /* Offer Download Action for each */
      for (final Pair<IAttachment, URI> attachmentPair : attachments) {
        IAttachment attachment = attachmentPair.getFirst();
        String fileName = URIUtils.getFile(attachmentPair.getSecond());
        String size = OwlUI.getSize(attachment.getLength());

        Action action = new Action(size != null ? (NLS.bind(Messages.ApplicationActionBarAdvisor_FILE_SIZE, fileName, size)) : (fileName)) {
          @Override
          public void run() {
            DirectoryDialog dialog = new DirectoryDialog(shellProvider.getShell(), SWT.None);
            dialog.setText(Messages.ApplicationActionBarAdvisor_SELECT_FOLDER_FOR_DOWNLOAD);
            String folder = dialog.open();
            if (StringUtils.isSet(folder))
              Controller.getDefault().getDownloadService().download(attachmentPair.getFirst(), attachmentPair.getSecond(), new File(folder), true);
          }
        };

        action.setImageDescriptor(OwlUI.getAttachmentImage(fileName, attachmentPair.getFirst().getType()));
        attachmentMenu.add(action);
      }

      /* Offer to Automize Downloading */
      attachmentMenu.add(new Separator());
      attachmentMenu.add(new AutomateFilterAction(PresetAction.DOWNLOAD, selection));
    }
  }

  /**
   * @param manager the {@link IMenuManager} to fill this menu into.
   * @param selection the current {@link IStructuredSelection} of {@link INews}.
   * @param shellProvider a {@link IShellProvider} for dialogs.
   * @param directMenu if <code>true</code> directly fill all items to the menu,
   * otherwise create a sub menu.
   */
  public static void fillShareMenu(IMenuManager manager, final IStructuredSelection selection, final IShellProvider shellProvider, boolean directMenu) {
    manager.add(new Separator("share")); //$NON-NLS-1$

    /* Either as direct Menu or Submenu */
    IMenuManager shareMenu;
    if (directMenu)
      shareMenu = manager;
    else {
      shareMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_SHARE_NEWS, OwlUI.SHARE, "sharenews"); //$NON-NLS-1$
      manager.add(shareMenu);
    }

    /* List all selected Share Providers  */
    List<ShareProvider> providers = Controller.getDefault().getShareProviders();
    for (final ShareProvider provider : providers) {
      if (provider.isEnabled()) {
        shareMenu.add(new Action(provider.getName()) {
          @Override
          public void run() {

            /* Special Case "Send E-Mail" action */
            if (SendLinkAction.ID.equals(provider.getId())) {
              IActionDelegate action = new SendLinkAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            /* Other Action */
            else {
              Object obj = selection.getFirstElement();
              if (obj != null && obj instanceof INews) {
                String shareLink = provider.toShareUrl((INews) obj);
                new OpenInBrowserAction(new StructuredSelection(shareLink)).run();
              }
            }
          };

          @Override
          public ImageDescriptor getImageDescriptor() {
            if (StringUtils.isSet(provider.getIconPath()))
              return OwlUI.getImageDescriptor(provider.getPluginId(), provider.getIconPath());

            return super.getImageDescriptor();
          };

          @Override
          public boolean isEnabled() {
            return !selection.isEmpty();
          }

          @Override
          public String getActionDefinitionId() {
            return SendLinkAction.ID.equals(provider.getId()) ? SendLinkAction.ID : super.getActionDefinitionId();
          }

          @Override
          public String getId() {
            return SendLinkAction.ID.equals(provider.getId()) ? SendLinkAction.ID : super.getId();
          }
        });
      }
    }

    /* Allow to Configure Providers */
    shareMenu.add(new Separator());
    shareMenu.add(new Action(Messages.ApplicationActionBarAdvisor_CONFIGURE) {
      @Override
      public void run() {
        PreferencesUtil.createPreferenceDialogOn(shellProvider.getShell(), SharingPreferencesPage.ID, null, null).open();
      };
    });
  }

  /**
   * @param manager the {@link IMenuManager} to fill this menu into.
   * @param selection the current {@link IStructuredSelection} of {@link INews}.
   * @param shellProvider a {@link IShellProvider} for dialogs.
   * @param directMenu if <code>true</code> directly fill all items to the menu,
   * otherwise create a sub menu.
   */
  public static void fillLabelMenu(IMenuManager manager, final IStructuredSelection selection, final IShellProvider shellProvider, boolean directMenu) {

    /* Either as direct Menu or Submenu */
    IMenuManager labelMenu;
    if (directMenu)
      labelMenu = manager;
    else {
      labelMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_LABEL);
      manager.add(labelMenu);
    }

    /* Assign  Labels */
    labelMenu.add(new AssignLabelsAction(shellProvider.getShell(), selection));

    /* Organize Labels */
    labelMenu.add(new Action(Messages.ApplicationActionBarAdvisor_ORGANIZE_LABELS) {
      @Override
      public void run() {
        PreferencesUtil.createPreferenceDialogOn(shellProvider.getShell(), ManageLabelsPreferencePage.ID, null, null).open();
      }
    });

    /* Load Labels */
    final Collection<ILabel> labels = CoreUtils.loadSortedLabels();

    /* Retrieve Labels that all selected News contain */
    labelMenu.add(new Separator());
    Set<ILabel> selectedLabels = ModelUtils.getLabelsForAll(selection);
    for (final ILabel label : labels) {
      LabelAction labelAction = new LabelAction(label, selection);
      labelAction.setChecked(selectedLabels.contains(label));
      labelMenu.add(labelAction);
    }

    /* New Label */
    labelMenu.add(new Action(Messages.ApplicationActionBarAdvisor_NEW_LABEL) {
      @Override
      public void run() {
        LabelDialog dialog = new LabelDialog(shellProvider.getShell(), DialogMode.ADD, null);
        if (dialog.open() == IDialogConstants.OK_ID) {
          String name = dialog.getName();
          RGB color = dialog.getColor();

          ILabel newLabel = Owl.getModelFactory().createLabel(null, name);
          newLabel.setColor(OwlUI.toString(color));
          newLabel.setOrder(labels.size());
          DynamicDAO.save(newLabel);

          LabelAction labelAction = new LabelAction(newLabel, selection);
          labelAction.run();
        }
      }

      @Override
      public boolean isEnabled() {
        return !selection.isEmpty();
      }
    });

    /* Remove All Labels */
    labelMenu.add(new Separator());
    LabelAction removeAllLabels = new LabelAction(null, selection);
    removeAllLabels.setEnabled(!selection.isEmpty() && !labels.isEmpty());
    labelMenu.add(removeAllLabels);
  }

  /**
   * @param menu the {@link IMenuManager} to fill.
   * @param window the {@link IWorkbenchWindow} where the menu is living.
   */
  public static void fillBookMarksMenu(IMenuManager menu, IWorkbenchWindow window) {
    Set<IFolder> roots = CoreUtils.loadRootFolders();

    /* Filter Options */
    final IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();
    Type[] allFilters = BookMarkFilter.Type.values();
    Type selectedFilter = allFilters[preferences.getInteger(DefaultPreferences.BM_MENU_FILTER)];
    List<Type> displayedFilters = Arrays.asList(new Type[] { Type.SHOW_ALL, Type.SHOW_NEW, Type.SHOW_UNREAD, Type.SHOW_STICKY });

    MenuManager optionsMenu = new MenuManager(Messages.ApplicationActionBarAdvisor_FILTER_ELEMENTS, (selectedFilter == Type.SHOW_ALL) ? OwlUI.FILTER : OwlUI.getImageDescriptor("icons/etool16/filter_active.gif"), null); //$NON-NLS-1$
    for (final Type filter : displayedFilters) {
      String name = Messages.ApplicationActionBarAdvisor_SHOW_ALL;
      switch (filter) {
        case SHOW_NEW:
          name = Messages.ApplicationActionBarAdvisor_SHOW_NEW;
          break;
        case SHOW_UNREAD:
          name = Messages.ApplicationActionBarAdvisor_SHOW_UNREAD;
          break;
        case SHOW_STICKY:
          name = Messages.ApplicationActionBarAdvisor_SHOW_STICKY;
          break;
      }

      Action action = new Action(name, IAction.AS_RADIO_BUTTON) {
        @Override
        public void run() {
          preferences.putInteger(DefaultPreferences.BM_MENU_FILTER, filter.ordinal());
        }
      };
      action.setChecked(filter == selectedFilter);
      optionsMenu.add(action);
      if (filter == Type.SHOW_ALL)
        optionsMenu.add(new Separator());
    }
    menu.add(optionsMenu);
    menu.add(new Separator());

    /* Single Bookmark Set */
    if (roots.size() == 1) {
      fillBookMarksMenu(window, menu, roots.iterator().next().getChildren(), selectedFilter);
    }

    /* More than one Bookmark Set */
    else {
      for (IFolder root : roots) {
        if (shouldShow(root, selectedFilter)) {
          MenuManager rootItem = new MenuManager(root.getName(), OwlUI.BOOKMARK_SET, null);
          menu.add(rootItem);

          fillBookMarksMenu(window, rootItem, root.getChildren(), selectedFilter);
        }
      }
    }

    /* Indicate that no Items are Showing */
    if (menu.getItems().length == 2 && selectedFilter != Type.SHOW_ALL) {
      boolean hasBookMarks = false;
      for (IFolder root : roots) {
        if (!root.getChildren().isEmpty()) {
          hasBookMarks = true;
          break;
        }
      }

      if (hasBookMarks) {
        menu.add(new Action(Messages.ApplicationActionBarAdvisor_SOME_ELEMENTS_FILTERED) {
          @Override
          public boolean isEnabled() {
            return false;
          }
        });
      }
    }
  }

  private static boolean shouldShow(IFolderChild child, Type filter) {
    switch (filter) {
      case SHOW_ALL:
        return true;

      case SHOW_NEW:
        return hasNewsWithState(child, EnumSet.of(INews.State.NEW));

      case SHOW_UNREAD:
        return hasNewsWithState(child, EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));

      case SHOW_STICKY:
        return hasStickyNews(child);

      default:
        return true;
    }
  }

  private static void fillBookMarksMenu(final IWorkbenchWindow window, IMenuManager parent, List<IFolderChild> childs, final Type filter) {
    for (final IFolderChild child : childs) {

      /* Check if a Filter applies */
      if (!shouldShow(child, filter))
        continue;

      /* News Mark or Empty Folder */
      if (child instanceof INewsMark || (child instanceof IFolder && ((IFolder) child).getChildren().isEmpty())) {
        String name = child.getName();
        if (child instanceof INewsMark) {
          int unreadNewsCount = (((INewsMark) child).getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
          if (unreadNewsCount > 0)
            name = NLS.bind(Messages.ApplicationActionBarAdvisor_MARK_UNREAD_COUNT, name, unreadNewsCount);
        }

        Action action = new Action(name) {
          @Override
          public void run() {
            if (child instanceof INewsMark)
              OwlUI.openInFeedView(window.getActivePage(), new StructuredSelection(child));
          }
        };
        action.setImageDescriptor(getImageDescriptor(child));
        parent.add(action);
      }

      /* Folder with Children */
      else if (child instanceof IFolder) {
        final IFolder folder = (IFolder) child;

        final MenuManager folderMenu = new MenuManager(folder.getName(), getImageDescriptor(folder), null);
        parent.add(folderMenu);
        folderMenu.add(new Action("") {}); //Dummy Action //$NON-NLS-1$
        folderMenu.setRemoveAllWhenShown(true);
        folderMenu.addMenuListener(new IMenuListener() {
          public void menuAboutToShow(IMenuManager manager) {
            fillBookMarksMenu(window, folderMenu, folder.getChildren(), filter);
          }
        });
      }
    }
  }

  private static ImageDescriptor getImageDescriptor(IFolderChild child) {
    boolean hasNewNews = hasNewsWithState(child, EnumSet.of(INews.State.NEW));

    /* Bookmark */
    if (child instanceof IBookMark) {
      ImageDescriptor favicon = OwlUI.getFavicon((IBookMark) child);
      if (!hasNewNews)
        return (favicon != null) ? favicon : OwlUI.BOOKMARK;

      /* Overlay if News are *new* */
      Image base = (favicon != null) ? OwlUI.getImage(fgResources, favicon) : OwlUI.getImage(fgResources, OwlUI.BOOKMARK);
      DecorationOverlayIcon overlay = new DecorationOverlayIcon(base, OwlUI.getImageDescriptor("icons/ovr16/new.gif"), IDecoration.BOTTOM_RIGHT); //$NON-NLS-1$
      return overlay;
    }

    /* Saved Search */
    else if (child instanceof ISearchMark) {
      if (hasNewNews)
        return OwlUI.SEARCHMARK_NEW;
      else if (((INewsMark) child).getNewsCount(INews.State.getVisible()) != 0)
        return OwlUI.SEARCHMARK;

      return OwlUI.SEARCHMARK_EMPTY;
    }

    /* News Bin */
    else if (child instanceof INewsBin) {
      if (hasNewNews)
        return OwlUI.NEWSBIN_NEW;
      else if (((INewsMark) child).getNewsCount(INews.State.getVisible()) != 0)
        return OwlUI.NEWSBIN;

      return OwlUI.NEWSBIN_EMPTY;
    }

    /* Folder */
    else if (child instanceof IFolder)
      return hasNewNews ? OwlUI.FOLDER_NEW : OwlUI.FOLDER;

    return null;
  }

  private static boolean hasNewsWithState(IFolderChild child, EnumSet<INews.State> states) {
    if (child instanceof IFolder)
      return hasNewsWithStates((IFolder) child, states);

    return ((INewsMark) child).getNewsCount(states) != 0;
  }

  private static boolean hasNewsWithStates(IFolder folder, EnumSet<INews.State> states) {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof INewsMark && ((INewsMark) mark).getNewsCount(states) != 0)
        return true;
    }

    List<IFolder> folders = folder.getFolders();
    for (IFolder child : folders) {
      if (hasNewsWithStates(child, states))
        return true;
    }

    return false;
  }

  private static boolean hasStickyNews(IFolderChild child) {
    if (child instanceof IFolder)
      return hasStickyNews((IFolder) child);

    if (child instanceof IBookMark)
      return ((IBookMark) child).getStickyNewsCount() != 0;

    return false;
  }

  private static boolean hasStickyNews(IFolder folder) {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark && ((IBookMark) mark).getStickyNewsCount() != 0)
        return true;
    }

    List<IFolder> folders = folder.getFolders();
    for (IFolder child : folders) {
      if (hasStickyNews(child))
        return true;
    }

    return false;
  }
}