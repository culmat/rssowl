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

package org.rssowl.ui.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.keys.IBindingService;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.actions.ExportAction;
import org.rssowl.ui.internal.actions.ImportAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.NewBookMarkAction;
import org.rssowl.ui.internal.actions.NewFolderAction;
import org.rssowl.ui.internal.actions.NewNewsBinAction;
import org.rssowl.ui.internal.actions.NewSearchMarkAction;
import org.rssowl.ui.internal.actions.NewTypeDropdownAction;
import org.rssowl.ui.internal.actions.RedoAction;
import org.rssowl.ui.internal.actions.ReloadAllAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.SearchFeedsAction;
import org.rssowl.ui.internal.actions.SearchNewsAction;
import org.rssowl.ui.internal.actions.ShowActivityAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.actions.UndoAction;
import org.rssowl.ui.internal.actions.NavigationActionFactory.Actions;
import org.rssowl.ui.internal.actions.NavigationActionFactory.NavigationAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link CoolBarAdvisor} is responsibe to fill the application tool bar
 * with items. It also provides a UI to configure the items.
 *
 * @author bpasero
 */
public class CoolBarAdvisor {

  /* ID of a Separator */
  private static final String SEPARATOR_ID = "org.rssowl.ui.CoolBarSeparator";

  /* ID of a Separator */
  private static final String SPACER_ID = "org.rssowl.ui.CoolBarSpacer";

  private final IWorkbenchWindow fWindow;
  private final ICoolBarManager fManager;
  private IPreferenceScope fPreferences;
  private Map<String, Item> fMapIdToItem;
  private IBindingService fBindingService = (IBindingService) PlatformUI.getWorkbench().getService(IBindingService.class);

  /** A List of Possible Items */
  public enum Item {

    /** Separator */
    SEPARATOR(SEPARATOR_ID, "--- Separator ---", null, OwlUI.getImageDescriptor("icons/obj16/separator.gif"), 0),

    /** Spacer */
    SPACER(SPACER_ID, "Blank Space", null, OwlUI.getImageDescriptor("icons/etool16/spacer.gif"), 0),

    /** New */
    NEW("org.rssowl.ui.NewDropDown", "New", null, OwlUI.getImageDescriptor("icons/etool16/add.gif"), true, false, 1),

    /** Import */
    IMPORT(ImportAction.ID, "Import", null, OwlUI.getImageDescriptor("icons/etool16/import.gif"), 1),

    /** Export */
    EXPORT(ExportAction.ID, "Export", null, OwlUI.getImageDescriptor("icons/etool16/export.gif"), 1),

    /** Undo */
    UNDO(UndoAction.ID, "Undo", null, OwlUI.getImageDescriptor("icons/elcl16/undo_edit.gif"), 2),

    /** Redo */
    REDO(RedoAction.ID, "Redo", null, OwlUI.getImageDescriptor("icons/elcl16/redo_edit.gif"), 2),

    /** Update */
    UPDATE(ReloadTypesAction.ID, "Update", null, OwlUI.getImageDescriptor("icons/elcl16/reload.gif"), 3),

    /** Update All */
    UPDATE_ALL(ReloadAllAction.ID, "Update All", null, OwlUI.getImageDescriptor("icons/elcl16/reload_all.gif"), 3),

    /** Stop */
    STOP("org.rssowl.ui.StopUpdate", "Stop", "Stop Updates", OwlUI.getImageDescriptor("icons/etool16/cancel.gif"), false, false, 3),

    /** Search */
    SEARCH(SearchNewsAction.ID, "Search", null, OwlUI.SEARCHMARK, 4),

    /** Mark Read */
    MARK_READ(ToggleReadStateAction.ID, "Mark Read", null, OwlUI.getImageDescriptor("icons/elcl16/mark_read.gif"), 5),

    /** Mark All Read */
    MARK_ALL_READ(MarkAllNewsReadAction.ID, "Mark All Read", null, OwlUI.getImageDescriptor("icons/elcl16/mark_all_read.gif"), 5),

    /** Label */
    LABEL("org.rssowl.ui.Label", "Label", "Label News", OwlUI.getImageDescriptor("icons/elcl16/labels.gif"), true, false, 6),

    /** Sticky */
    STICKY("org.rssowl.ui.actions.MarkSticky", "Sticky", "Mark a News Sticky", OwlUI.NEWS_PINNED, 6),

    /** Next Unread News */
    NEXT("org.rssowl.ui.Next", "Next", null, OwlUI.getImageDescriptor("icons/etool16/next.gif"), true, false, 7),

    /** Previous Unread News */
    PREVIOUS("org.rssowl.ui.Previous", "Previous", null, OwlUI.getImageDescriptor("icons/etool16/previous.gif"), true, false, 7),

    /** Move To */
    MOVE("org.rssowl.ui.Move", "Move", "Move News", OwlUI.getImageDescriptor("icons/etool16/move_to.gif"), true, false, 8),

    /** Copy To */
    COPY("org.rssowl.ui.Copy", "Copy", "Copy News", OwlUI.getImageDescriptor("icons/etool16/copy_to.gif"), true, false, 8),

    /** Share */
    SHARE("org.rssowl.ui.Share", "Share", "Share News", OwlUI.getImageDescriptor("icons/elcl16/share.gif"), true, false, 8),

    /** Save As */
    SAVE_AS("org.eclipse.ui.file.saveAs", "Save", "Save News", OwlUI.getImageDescriptor("icons/etool16/save_as.gif"), 8),

    /** Print */
    PRINT("org.eclipse.ui.file.print", "Print", "Print News", OwlUI.getImageDescriptor("icons/etool16/print.gif"), 8),

    /** Find More Feeds */
    FIND_MORE_FEEDS("org.rssowl.ui.SearchFeedsAction", "Find Feeds", null, OwlUI.getImageDescriptor("icons/etool16/new_bkmrk.gif"), 9),

    /** New Bookmark */
    NEW_BOOKMARK("org.rssowl.ui.actions.NewBookMark", "Bookmark", "New Bookmark", OwlUI.BOOKMARK, 9),

    /** New News Bin */
    NEW_BIN("org.rssowl.ui.actions.NewNewsBin", "News Bin", "New News Bin", OwlUI.NEWSBIN, 9),

    /** New Saved Search */
    NEW_SAVED_SEARCH("org.rssowl.ui.actions.NewSearchMark", "Saved Search", "New Saved Search", OwlUI.SEARCHMARK, 9),

    /** New Folder */
    NEW_FOLDER("org.rssowl.ui.actions.NewFolder", "Folder", "New Folder", OwlUI.FOLDER, 9),

    /** Close Tab */
    CLOSE("org.eclipse.ui.file.close", "Close", null, OwlUI.getImageDescriptor("icons/etool16/close_tab.gif"), 10),

    /** Close Others */
    CLOSE_OTHERS("org.eclipse.ui.file.closeOthers", "Close Others", null, OwlUI.getImageDescriptor("icons/etool16/close_other_tabs.gif"), 10),

    /** Close All Tabs */
    CLOSE_ALL("org.eclipse.ui.file.closeAll", "Close All", null, OwlUI.getImageDescriptor("icons/etool16/close_all_tabs.gif"), 10),

    /** Bookmarks */
    BOOKMARKS("org.rssowl.ui.Bookmarks", "Bookmarks", null, OwlUI.getImageDescriptor("icons/etool16/subscriptions.gif"), true, false, 11),

    /** History */
    HISTORY("org.rssowl.ui.History", "History", "Recently Visited Feeds", OwlUI.getImageDescriptor("icons/etool16/history.gif"), true, false, 11),

    /** Bookmark View */
    BOOKMARK_VIEW("org.rssowl.ui.ToggleBookmarksCommand", "Bookmark View", null, OwlUI.getImageDescriptor("icons/eview16/bkmrk_explorer.gif"), 11),

    /** Downloads and Activity */
    ACTIVITIES("org.rssowl.ui.ShowActivityAction", "Activity", "Downloads && Activity", OwlUI.getImageDescriptor("icons/elcl16/activity.gif"), 12),

    /** Preferences */
    PREFERENCES("org.rssowl.ui.ShowPreferences", "Preferences", null, OwlUI.getImageDescriptor("icons/elcl16/preferences.gif"), false, false, 12),

    /** Fullscreen */
    FULLSCREEN("org.rssowl.ui.FullScreenCommand", "Full Screen", "Toggle Full Screen", OwlUI.getImageDescriptor("icons/etool16/fullscreen.gif"), 12);

    private final String fId;
    private final String fName;
    private final String fTooltip;
    private final ImageDescriptor fImg;
    private final boolean fWithDropDownMenu;
    private final boolean fHasCommand;
    private final int fGroup;

    Item(String id, String name, String tooltip, ImageDescriptor img, int group) {
      this(id, name, tooltip, img, false, true, group);
    }

    Item(String id, String name, String tooltip, ImageDescriptor img, boolean withDropDownMenu, boolean hasCommand, int group) {
      fId = id;
      fName = name;
      fTooltip = tooltip;
      fImg = img;
      fWithDropDownMenu = withDropDownMenu;
      fHasCommand = hasCommand;
      fGroup = group;
    }

    /**
     * @return the unique identifier of this item.
     */
    public String getId() {
      return fId;
    }

    /**
     * @return the Name to show for this Item or <code>null</code> if none.
     */
    public String getName() {
      return fName;
    }

    /**
     * @return the Tooltip to show for this Item or <code>null</code> if none.
     */
    public String getTooltip() {
      return fTooltip;
    }

    /**
     * @return an integer describing the group of an item. Can be used for
     * grouping of items that have the same group number.
     */
    public int getGroup() {
      return fGroup;
    }

    /**
     * @return the Image to show for this Item or <code>null</code> if none.
     */
    public ImageDescriptor getImg() {
      return fImg;
    }

    boolean withDropDownMenu() {
      return fWithDropDownMenu;
    }

    boolean hasCommand() {
      return fHasCommand;
    }
  }

  /** Toolbar Mode */
  public enum Mode {

    /** Image and Text */
    IMAGE_TEXT,

    /** Only Image */
    IMAGE,

    /** Only Text */
    TEXT
  }

  /* Selection Listener for Navigation Actions */
  private class NavigationSelectionListener extends SelectionAdapter {
    private boolean fIsNext;

    NavigationSelectionListener(boolean isNext) {
      fIsNext = isNext;
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
      Object data = e.widget.getData();
      if (data instanceof Actions) {
        Actions actionType = (Actions) data;
        NavigationAction action = new NavigationAction(actionType);
        action.init(fWindow);
        action.run(null);

        fPreferences.putInteger(fIsNext ? DefaultPreferences.DEFAULT_NEXT_ACTION : DefaultPreferences.DEFAULT_PREVIOUS_ACTION, actionType.ordinal());
      }
    }
  }

  /**
   * @param manager
   * @param window
   */
  public CoolBarAdvisor(ICoolBarManager manager, IWorkbenchWindow window) {
    fManager = manager;
    fWindow = window;
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fMapIdToItem = new HashMap<String, Item>();

    Item[] items = Item.values();
    for (Item item : items) {
      fMapIdToItem.put(item.getId(), item);
    }
  }

  /** Fill the Coolbar */
  public void advise() {
    advise(false);
  }

  /**
   * Fill the Coolbar
   *
   * @param fromUpdate if <code>true</code> this method will ensure to re-layout
   * and update the coolbar.
   */
  public void advise(boolean fromUpdate) {

    /* Retrieve Control if available */
    CoolBar barControl = null;
    if (fManager instanceof CoolBarManager)
      barControl = ((CoolBarManager) fManager).getControl();

    /* Disable Redraw to avoid Flicker */
    if (barControl != null && fromUpdate)
      barControl.getShell().setRedraw(false);

    try {

      /* First Remove All */
      fManager.removeAll();

      /* Load Toolbar Mode */
      Mode mode = Mode.values()[fPreferences.getInteger(DefaultPreferences.TOOLBAR_MODE)];

      /* Load and Add Items */
      String[] items = fPreferences.getStrings(DefaultPreferences.TOOLBAR_ITEMS);
      ToolBarManager currentToolBar = new ToolBarManager(SWT.FLAT);
      for (String id : items) {
        Item item = fMapIdToItem.get(id);
        if (item != null) {

          /* Separator: Start a new Toolbar */
          if (item == Item.SEPARATOR) {
            fManager.add(currentToolBar);
            currentToolBar = new ToolBarManager(SWT.FLAT);
          }

          /* Spacer */
          else if (item == Item.SPACER) {
            ActionContributionItem contribItem = new ActionContributionItem(new Action("") {
              @Override
              public boolean isEnabled() {
                return false;
              }
            });
            currentToolBar.add(contribItem);
          }

          /* Any other Item */
          else {
            ActionContributionItem contribItem = new ActionContributionItem(getAction(item, mode, currentToolBar));
            contribItem.setId(id);
            if (mode == Mode.IMAGE_TEXT)
              contribItem.setMode(ActionContributionItem.MODE_FORCE_TEXT);
            currentToolBar.add(contribItem);
          }
        }
      }

      /* Add latest Toolbar Manager to Coolbar too */
      fManager.add(currentToolBar);

      /* Ensure Updates are properly Propagated */
      if (fromUpdate) {
        fManager.update(true);
        if (barControl != null) {
          boolean isLocked = barControl.getLocked();
          barControl.setLocked(!isLocked);
          barControl.setLocked(isLocked);
        }
      }
    } finally {
      if (barControl != null && fromUpdate)
        barControl.getShell().setRedraw(true);
    }
  }

  private Action getAction(final Item item, final Mode mode, final ToolBarManager manager) {
    return new Action(item.getName(), item.withDropDownMenu() ? IAction.AS_DROP_DOWN_MENU : IAction.AS_PUSH_BUTTON) {

      @Override
      public String getId() {
        return item.getId();
      }

      @Override
      public String getToolTipText() {
        return item.getTooltip();
      }

      @Override
      public String getActionDefinitionId() {
        return item.hasCommand() ? item.getId() : null;
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return mode == Mode.TEXT ? null : item.getImg();
      }

      @Override
      public void run() {
        CoolBarAdvisor.this.run(this, item, manager);
      }

      @Override
      public IMenuCreator getMenuCreator() {
        return CoolBarAdvisor.this.getMenu(item);
      }
    };
  }

  private void run(Action wrappingAction, Item item, ToolBarManager manager) {
    switch (item) {

      /* New */
      case NEW: {
        NewTypeDropdownAction action = new NewTypeDropdownAction();
        initWithExplorerSelectionAndRunAction(action);
        break;
      }

        /* Import */
      case IMPORT: {
        ImportAction action = new ImportAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Export */
      case EXPORT: {
        ExportAction action = new ExportAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Undo */
      case UNDO: {
        UndoAction action = new UndoAction();
        action.run();
        break;
      }

        /* Redo */
      case REDO: {
        RedoAction action = new RedoAction();
        action.run();
        break;
      }

        /* Search */
      case SEARCH: {
        SearchNewsAction action = new SearchNewsAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Update All */
      case UPDATE_ALL: {
        ReloadAllAction action = new ReloadAllAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Update */
      case UPDATE: {
        IStructuredSelection activeSelection = OwlUI.getActiveSelection();
        if (activeSelection != null && !activeSelection.isEmpty()) {
          ReloadTypesAction action = new ReloadTypesAction(activeSelection, fWindow.getShell());
          action.run(null);
        }

        break;
      }

        /* Stop */
      case STOP: {
        Controller.getDefault().stopUpdate();
        break;
      }

        /* Mark Read */
      case MARK_READ: {
        IStructuredSelection selection = OwlUI.getActiveFeedViewSelection();
        if (selection != null && !selection.isEmpty()) {
          ToggleReadStateAction action = new ToggleReadStateAction(selection);
          action.init(fWindow);
          action.run(null);
        }
        break;
      }

        /* Mark All Read */
      case MARK_ALL_READ: {
        MarkAllNewsReadAction action = new MarkAllNewsReadAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Next */
      case NEXT: {
        Actions defaultAction = Actions.values()[fPreferences.getInteger(DefaultPreferences.DEFAULT_NEXT_ACTION)];
        NavigationAction action = new NavigationAction(defaultAction);
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Previous */
      case PREVIOUS: {
        Actions defaultAction = Actions.values()[fPreferences.getInteger(DefaultPreferences.DEFAULT_PREVIOUS_ACTION)];
        NavigationAction action = new NavigationAction(defaultAction);
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* New Bookmark */
      case NEW_BOOKMARK: {
        NewBookMarkAction action = new NewBookMarkAction();
        initWithExplorerSelectionAndRunAction(action);
        break;
      }

        /* New News Bin */
      case NEW_BIN: {
        NewNewsBinAction action = new NewNewsBinAction();
        initWithExplorerSelectionAndRunAction(action);
        break;
      }

        /* New Saved Search */
      case NEW_SAVED_SEARCH: {
        NewSearchMarkAction action = new NewSearchMarkAction();
        initWithExplorerSelectionAndRunAction(action);
        break;
      }

        /* New Folder */
      case NEW_FOLDER: {
        NewFolderAction action = new NewFolderAction();
        initWithExplorerSelectionAndRunAction(action);
        break;
      }

        /* Close */
      case CLOSE: {
        IWorkbenchAction action = ActionFactory.CLOSE.create(fWindow);
        action.run();
        break;
      }

        /* Close Others */
      case CLOSE_OTHERS: {
        IWorkbenchPage page = fWindow.getActivePage();
        if (page != null) {
          IEditorReference[] refArray = page.getEditorReferences();
          if (refArray != null && refArray.length > 1) {
            IEditorReference[] otherEditors = new IEditorReference[refArray.length - 1];
            IEditorReference activeEditor = (IEditorReference) page.getReference(page.getActiveEditor());
            for (int i = 0; i < refArray.length; i++) {
              if (refArray[i] != activeEditor)
                continue;
              System.arraycopy(refArray, 0, otherEditors, 0, i);
              System.arraycopy(refArray, i + 1, otherEditors, i, refArray.length - 1 - i);
              break;
            }
            page.closeEditors(otherEditors, true);
          }
        }
        break;
      }

        /* Close All */
      case CLOSE_ALL: {
        IWorkbenchAction action = ActionFactory.CLOSE_ALL.create(fWindow);
        action.run();
        break;
      }

        /* Save As */
      case SAVE_AS: {
        FeedView activeFeedView = OwlUI.getActiveFeedView();
        if (activeFeedView != null)
          activeFeedView.doSaveAs();
        break;
      }

        /* Print */
      case PRINT: {
        FeedView activeFeedView = OwlUI.getActiveFeedView();
        if (activeFeedView != null)
          activeFeedView.print();
        break;
      }

        /* Fullscreen */
      case FULLSCREEN: {
        OwlUI.toggleFullScreen();
        break;
      }

        /* Toggle Bookmarks View */
      case BOOKMARK_VIEW: {
        OwlUI.toggleBookmarks();
        break;
      }

        /* Sticky */
      case STICKY: {
        IStructuredSelection selection = OwlUI.getActiveFeedViewSelection();
        if (selection != null && !selection.isEmpty())
          new MakeNewsStickyAction(selection).run();
        break;
      }

        /* Find more Feeds */
      case FIND_MORE_FEEDS: {
        SearchFeedsAction action = new SearchFeedsAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Downloads & Activity */
      case ACTIVITIES: {
        ShowActivityAction action = new ShowActivityAction();
        action.init(fWindow);
        action.run(null);
        break;
      }

        /* Preferences */
      case PREFERENCES: {
        IWorkbenchAction action = ActionFactory.PREFERENCES.create(fWindow);
        action.run();
        break;
      }

        /* History */
      case HISTORY: {
        showMenu(wrappingAction, manager);
        break;
      }

        /* Label */
      case LABEL: {
        showMenu(wrappingAction, manager);
        break;
      }

        /* Move */
      case MOVE: {
        showMenu(wrappingAction, manager);
        break;
      }

        /* Copy */
      case COPY: {
        showMenu(wrappingAction, manager);
        break;
      }

        /* Share */
      case SHARE: {
        showMenu(wrappingAction, manager);
        break;
      }

        /* Bookmarks */
      case BOOKMARKS: {
        showMenu(wrappingAction, manager);
        break;
      }
    }
  }

  private void showMenu(Action wrappingAction, ToolBarManager manager) {
    Menu menu = wrappingAction.getMenuCreator().getMenu(manager.getControl());
    if (menu != null) {

      /* Adjust Location */
      IContributionItem contributionItem = manager.find(wrappingAction.getId());
      if (contributionItem != null && contributionItem instanceof ActionContributionItem) {
        Widget widget = ((ActionContributionItem) contributionItem).getWidget();
        if (widget != null && widget instanceof ToolItem) {
          ToolItem item = (ToolItem) widget;
          Rectangle rect = item.getBounds();
          Point pt = new Point(rect.x, rect.y + rect.height);
          pt = manager.getControl().toDisplay(pt);
          menu.setLocation(pt.x, pt.y);
        }
      }

      /* Set Visible */
      menu.setVisible(true);
    }
  }

  private void initWithExplorerSelectionAndRunAction(IWorkbenchWindowActionDelegate action) {

    /* Workbench Window */
    action.init(fWindow);

    /* Explorer Selection */
    IFolder folder = OwlUI.getBookMarkExplorerSelection();
    if (folder != null)
      action.selectionChanged(null, new StructuredSelection(folder));

    /* Run */
    action.run(null);
  }

  private IMenuCreator getMenu(Item item) {
    if (!item.withDropDownMenu())
      return null;

    switch (item) {

      /* New Bookmark | Saved Search | News Bin | Folder */
      case NEW: {
        NewTypeDropdownAction action = new NewTypeDropdownAction();
        action.init(fWindow);
        IFolder folder = OwlUI.getBookMarkExplorerSelection();
        if (folder != null)
          action.selectionChanged(null, new StructuredSelection(folder));
        return action;
      }

        /* Next News | Next Unread News || Next Feed | Next Unread Feed || Next Tab */
      case NEXT: {
        return new IMenuCreator() {

          public Menu getMenu(Control parent) {
            Menu menu = new Menu(parent);
            Actions defaultAction = Actions.values()[fPreferences.getInteger(DefaultPreferences.DEFAULT_NEXT_ACTION)];

            MenuItem item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.NEXT_NEWS.getCommandId(), "Next News"));
            item.setData(Actions.NEXT_NEWS);
            item.addSelectionListener(new NavigationSelectionListener(true));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.NEXT_UNREAD_NEWS.getCommandId(), "Next Unread News"));
            item.setData(Actions.NEXT_UNREAD_NEWS);
            item.addSelectionListener(new NavigationSelectionListener(true));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            new MenuItem(menu, SWT.SEPARATOR);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.NEXT_FEED.getCommandId(), "Next Feed"));
            item.setData(Actions.NEXT_FEED);
            item.addSelectionListener(new NavigationSelectionListener(true));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.NEXT_UNREAD_FEED.getCommandId(), "Next Unread Feed"));
            item.setData(Actions.NEXT_UNREAD_FEED);
            item.addSelectionListener(new NavigationSelectionListener(true));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            new MenuItem(menu, SWT.SEPARATOR);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.NEXT_TAB.getCommandId(), "Next Tab"));
            item.setData(Actions.NEXT_TAB);
            item.addSelectionListener(new NavigationSelectionListener(true));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            return menu;
          }

          public void dispose() {}

          public Menu getMenu(Menu parent) {
            return null;
          }
        };
      }

        /* Previous News | Previous Unread News || Previous Feed | Previous Unread Feed || Previous Tab */
      case PREVIOUS: {
        return new IMenuCreator() {

          public Menu getMenu(Control parent) {
            Menu menu = new Menu(parent);
            Actions defaultAction = Actions.values()[fPreferences.getInteger(DefaultPreferences.DEFAULT_PREVIOUS_ACTION)];

            MenuItem item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.PREVIOUS_NEWS.getCommandId(), "Previous News"));
            item.setData(Actions.PREVIOUS_NEWS);
            item.addSelectionListener(new NavigationSelectionListener(false));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.PREVIOUS_UNREAD_NEWS.getCommandId(), "Previous Unread News"));
            item.setData(Actions.PREVIOUS_UNREAD_NEWS);
            item.addSelectionListener(new NavigationSelectionListener(false));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            new MenuItem(menu, SWT.SEPARATOR);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.PREVIOUS_FEED.getCommandId(), "Previous Feed"));
            item.setData(Actions.PREVIOUS_FEED);
            item.addSelectionListener(new NavigationSelectionListener(false));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.PREVIOUS_UNREAD_FEED.getCommandId(), "Previous Unread Feed"));
            item.setData(Actions.PREVIOUS_UNREAD_FEED);
            item.addSelectionListener(new NavigationSelectionListener(false));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            new MenuItem(menu, SWT.SEPARATOR);

            item = new MenuItem(menu, SWT.RADIO);
            item.setText(getLabelWithBinding(Actions.PREVIOUS_TAB.getCommandId(), "Previous Tab"));
            item.setData(Actions.PREVIOUS_TAB);
            item.addSelectionListener(new NavigationSelectionListener(false));
            if (item.getData().equals(defaultAction))
              item.setSelection(true);

            return menu;
          }

          public void dispose() {}

          public Menu getMenu(Menu parent) {
            return null;
          }
        };
      }

        /* History */
      case HISTORY:
        return new IMenuCreator() {

          public Menu getMenu(Control parent) {
            Menu menu = new Menu(parent);
            ContributionItemFactory.REOPEN_EDITORS.create(fWindow).fill(menu, 0);
            MenuItem[] items = menu.getItems();
            if (items.length > 0 && (items[0].getStyle() & SWT.SEPARATOR) != 0)
              items[0].dispose();
            return menu;
          }

          public Menu getMenu(Menu parent) {
            return null;
          }

          public void dispose() {}
        };

        /* Label */
      case LABEL:
        return new IMenuCreator() {
          public Menu getMenu(Control parent) {
            MenuManager manager = new MenuManager();
            IStructuredSelection activeFeedViewSelection = OwlUI.getActiveFeedViewSelection();
            ApplicationActionBarAdvisor.fillLabelMenu(manager, activeFeedViewSelection, fWindow, true);
            return manager.createContextMenu(parent);
          }

          public Menu getMenu(Menu parent) {
            return null;
          }

          public void dispose() {}
        };

        /* Move */
      case MOVE:
        return getMoveCopyMenu(true);

        /* Copy */
      case COPY:
        return getMoveCopyMenu(false);

        /* Share */
      case SHARE:
        return new IMenuCreator() {
          public Menu getMenu(Control parent) {
            MenuManager manager = new MenuManager();
            IStructuredSelection activeFeedViewSelection = OwlUI.getActiveFeedViewSelection();
            ApplicationActionBarAdvisor.fillShareMenu(manager, activeFeedViewSelection, fWindow, true);
            return manager.createContextMenu(parent);
          }

          public Menu getMenu(Menu parent) {
            return null;
          }

          public void dispose() {}
        };

        /* Bookmarks */
      case BOOKMARKS:
        return new IMenuCreator() {
          public Menu getMenu(Control parent) {
            MenuManager manager = new MenuManager();
            ApplicationActionBarAdvisor.fillBookMarksMenu(manager, fWindow);
            return manager.createContextMenu(parent);
          }

          public Menu getMenu(Menu parent) {
            return null;
          }

          public void dispose() {}
        };
    };

    return null;
  }

  private IMenuCreator getMoveCopyMenu(final boolean isMove) {
    return new IMenuCreator() {
      public Menu getMenu(Control parent) {
        IStructuredSelection selection = OwlUI.getActiveFeedViewSelection();
        if (selection == null || selection.isEmpty())
          return null;

        MenuManager manager = new MenuManager();

        /* Load all news bins and sort by name */
        List<INewsBin> newsbins = new ArrayList<INewsBin>(DynamicDAO.loadAll(INewsBin.class));

        Comparator<INewsBin> comparator = new Comparator<INewsBin>() {
          public int compare(INewsBin o1, INewsBin o2) {
            return o1.getName().compareTo(o2.getName());
          };
        };

        Collections.sort(newsbins, comparator);

        IEditorPart activeEditor = OwlUI.getActiveEditor();
        IEditorInput activeInput = (activeEditor != null) ? activeEditor.getEditorInput() : null;
        for (INewsBin bin : newsbins) {
          if (activeInput != null && activeInput instanceof FeedViewInput && ((FeedViewInput) activeInput).getMark().equals(bin))
            continue;

          manager.add(new MoveCopyNewsToBinAction(selection, bin, isMove));
        }

        manager.add(new Separator("movetonewbin"));
        manager.add(new MoveCopyNewsToBinAction(selection, null, isMove));

        return manager.createContextMenu(parent);
      }

      public Menu getMenu(Menu parent) {
        return null;
      }

      public void dispose() {}
    };
  }

  private String getLabelWithBinding(String id, String label) {
    TriggerSequence binding = fBindingService.getBestActiveBindingFor(id);
    if (binding != null)
      return label + "\t" + binding.format();

    return label;
  }
}