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
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.ui.IWorkbenchWindow;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.actions.ExportAction;
import org.rssowl.ui.internal.actions.ImportAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.NewTypeDropdownAction;
import org.rssowl.ui.internal.actions.RedoAction;
import org.rssowl.ui.internal.actions.ReloadAllAction;
import org.rssowl.ui.internal.actions.SearchNewsAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.actions.UndoAction;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link CoolBarAdvisor} is responsibe to fill the application tool bar
 * with items. It also provides a UI to configure the items.
 *
 * @author bpasero
 */
public class CoolBarAdvisor {

  /* ID of a Separator */
  private static final String SEPARATOR_ID = "org.rssowl.ui.internal.Separator";

  private final IWorkbenchWindow fWindow;
  private final ICoolBarManager fManager;
  private IPreferenceScope fPreferences;
  private Map<String, Item> fMapIdToItem;

  /** A List of Possible Items */
  public enum Item {

    /** Separator */
    SEPARATOR(SEPARATOR_ID, "--- Separator ---", OwlUI.getImageDescriptor("icons/obj16/separator.gif")),

    /** New */
    NEW("org.rssowl.ui.NewDropDown", "New", OwlUI.getImageDescriptor("icons/etool16/add.gif"), true, false),

    /** Import */
    IMPORT(ImportAction.ID, "Import", OwlUI.getImageDescriptor("icons/etool16/import.gif")),

    /** Export */
    EXPORT(ExportAction.ID, "Export", OwlUI.getImageDescriptor("icons/etool16/export.gif")),

    /** Undo */
    UNDO(UndoAction.ID, "Undo", OwlUI.getImageDescriptor("icons/elcl16/undo_edit.gif")),

    /** Redo */
    REDO(RedoAction.ID, "Redo", OwlUI.getImageDescriptor("icons/elcl16/redo_edit.gif")),

    /** Update All */
    UPDATE_ALL(ReloadAllAction.ID, "Update All", OwlUI.getImageDescriptor("icons/elcl16/reload_all.gif")),

    /** Stop */
    STOP("org.rssowl.ui.StopUpdate", "Stop", OwlUI.getImageDescriptor("icons/etool16/stop.gif"), false, false),

    /** Search */
    SEARCH(SearchNewsAction.ID, "Search", OwlUI.SEARCHMARK),

    /** Mark Read */
    MARK_READ(ToggleReadStateAction.ID, "Mark Read", OwlUI.getImageDescriptor("icons/elcl16/mark_read.gif")),

    /** Mark All Read */
    MARK_ALL_READ(MarkAllNewsReadAction.ID, "Mark All Read", OwlUI.getImageDescriptor("icons/elcl16/mark_all_read.gif"));

    /** Next (Drop Down with all from Next, remembers Selection with Radio) */
    //NEXT(null, null, null),

    /**
     * Previous (Drop Down with all from Previous, remembers Selection with
     * Radio)
     */
    //PREVIOUS(null, null, null);

    private final String fId;
    private final String fName;
    private final ImageDescriptor fImg;
    private final boolean fWithDropDownMenu;
    private final boolean fHasCommand;

    Item(String id, String name, ImageDescriptor img) {
      this(id, name, img, false, true);
    }

    Item(String id, String name, ImageDescriptor img, boolean withDropDownMenu, boolean hasCommand) {
      fId = id;
      fName = name;
      fImg = img;
      fWithDropDownMenu = withDropDownMenu;
      fHasCommand = hasCommand;
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

          /* Any other Item */
          else {
            ActionContributionItem contribItem = new ActionContributionItem(getAction(item, mode));
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

  private Action getAction(final Item item, final Mode mode) {
    return new Action(item.getName(), item.withDropDownMenu() ? IAction.AS_DROP_DOWN_MENU : IAction.AS_PUSH_BUTTON) {

      @Override
      public String getId() {
        return item.getId();
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
        CoolBarAdvisor.this.run(item);
      }

      @Override
      public IMenuCreator getMenuCreator() {
        return CoolBarAdvisor.this.getMenu(item);
      }
    };
  }

  private void run(Item item) {
    switch (item) {

      /* New */
      case NEW: {
        NewTypeDropdownAction action = new NewTypeDropdownAction();
        action.init(fWindow);
        IFolder folder = OwlUI.getBookMarkExplorerSelection();
        if (folder != null)
          action.selectionChanged(null, new StructuredSelection(folder));
        action.run(null);
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

        /* Stop */
      case STOP: {
        Controller.getDefault().stopUpdate();
        break;
      }

        /* Mark Read */
      case MARK_READ: {
        IStructuredSelection selection = OwlUI.getActiveFeedViewSelection();
        if (selection != null) {
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
    }
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
    };

    return null;
  }
}