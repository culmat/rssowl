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

package org.rssowl.ui.internal;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.rssowl.ui.internal.actions.ReloadAllAction;

/**
 * @author bpasero
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

  /** Name of the "Manage Extensions" SubMenu */
  public static final String M_MANAGE_EXTENSIONS = "manageExtensions"; //$NON-NLS-1$

  /** Name of the View Top Menu */
  public static final String M_VIEW = "view";

  /** Start of the View Top Menu */
  public static final String M_VIEW_START = "viewStart";

  /** End of the View Top Menu */
  public static final String M_VIEW_END = "viewEnd";

  private IContributionItem fOpenWindowsItem;
  private IContributionItem fShowViewMenu;
  private IContributionItem fReopenEditors;

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
    register(ActionFactory.CLOSE.create(window)); // TODO ActionSet?
    register(ActionFactory.CLOSE_ALL.create(window)); // TODO ActionSet?
    register(ActionFactory.SAVE_AS.create(window)); // TODO ActionSet?
    register(ActionFactory.PRINT.create(window)); // TODO ActionSet?
    register(ActionFactory.QUIT.create(window));

    fReopenEditors = ContributionItemFactory.REOPEN_EDITORS.create(window);

    /* Menu: Edit */
    register(ActionFactory.CUT.create(window));
    register(ActionFactory.COPY.create(window));
    register(ActionFactory.PASTE.create(window));
    register(ActionFactory.DELETE.create(window));
    register(ActionFactory.SELECT_ALL.create(window));
    register(ActionFactory.PROPERTIES.create(window));

    /* Menu: Tools */
    register(ActionFactory.PREFERENCES.create(window));

    /* Menu: Window */
    register(ActionFactory.OPEN_NEW_WINDOW.create(window));
    getAction(ActionFactory.OPEN_NEW_WINDOW.getId()).setText("&New Window");
    register(ActionFactory.TOGGLE_COOLBAR.create(window));
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
    //    register(ActionFactory.SHOW_OPEN_EDITORS.create(window));
    //    register(ActionFactory.SHOW_WORKBOOK_EDITORS.create(window));
    //    register(ActionFactory.SHOW_PART_PANE_MENU.create(window));
    //    register(ActionFactory.SHOW_VIEW_MENU.create(window));

    fOpenWindowsItem = ContributionItemFactory.OPEN_WINDOWS.create(window);
    fShowViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

    /* Menu: Help */
    // register(ActionFactory.INTRO.create(window)); TODO Enable
    register(ActionFactory.ABOUT.create(window));
    getAction(ActionFactory.ABOUT.getId()).setText("&About RSSOwl");

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
    MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
    menuBar.add(fileMenu);

    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.CLOSE.getId()));
    fileMenu.add(getAction(ActionFactory.CLOSE_ALL.getId()));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
    fileMenu.add(new Separator());
    fileMenu.add(getAction(ActionFactory.SAVE_AS.getId()));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.SAVE_EXT));
    fileMenu.add(new Separator());
    fileMenu.add(getAction(ActionFactory.PRINT.getId()));

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    fileMenu.add(fReopenEditors); // TODO Consider moving into a "Go" Menu!

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.QUIT.getId()));
  }

  /* Menu: Edit */
  private void createEditMenu(IMenuManager menuBar) {
    MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
    menuBar.add(editMenu);

    editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
    editMenu.add(new Separator());
    editMenu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
    editMenu.add(new Separator());

    editMenu.add(getAction(ActionFactory.CUT.getId()));
    editMenu.add(getAction(ActionFactory.COPY.getId()));
    editMenu.add(getAction(ActionFactory.PASTE.getId()));
    editMenu.add(new Separator());
    editMenu.add(getAction(ActionFactory.DELETE.getId()));
    editMenu.add(getAction(ActionFactory.SELECT_ALL.getId()));

    editMenu.add(new Separator());
    editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
    editMenu.add(new Separator());

    editMenu.add(getAction(ActionFactory.PROPERTIES.getId()));
  }

  /* Menu: View */
  private void createViewMenu(IMenuManager menuBar) {
    MenuManager viewMenu = new MenuManager("&View", M_VIEW);
    menuBar.add(viewMenu);

    viewMenu.add(getAction(ActionFactory.TOGGLE_COOLBAR.getId()));
    viewMenu.add(new GroupMarker(M_VIEW_START));
    viewMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    viewMenu.add(new GroupMarker(M_VIEW_START));
  }

  /* Menu: Go */
  private void createGoMenu(IMenuManager menuBar) {
    MenuManager viewMenu = new MenuManager("&Go", IWorkbenchActionConstants.M_NAVIGATE);
    menuBar.add(viewMenu);

    viewMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  /* Menu: Tools */
  private void createToolsMenu(IMenuManager menuBar) {
    MenuManager toolsMenu = new MenuManager("&Tools", OwlUI.M_TOOLS);
    menuBar.add(toolsMenu);

    toolsMenu.add(new Separator());
    toolsMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    toolsMenu.add(new Separator());

    toolsMenu.add(getAction(ActionFactory.PREFERENCES.getId()));
  }

  /* Menu: Window */
  private void createWindowMenu(IMenuManager menuBar) {
    MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
    menuBar.add(windowMenu);

    windowMenu.add(getAction(ActionFactory.OPEN_NEW_WINDOW.getId()));
    windowMenu.add(new Separator());

    MenuManager showViewMenu = new MenuManager("&Show View");
    windowMenu.add(showViewMenu);
    showViewMenu.add(fShowViewMenu);
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

    windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    windowMenu.add(fOpenWindowsItem);
  }

  /* Menu: Help */
  private void createHelpMenu(IMenuManager menuBar) {
    MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
    menuBar.add(helpMenu);

    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
    helpMenu.add(new Separator());

    // helpMenu.add(getAction(ActionFactory.INTRO.getId())); TODO Enable
    helpMenu.add(new Separator());

    helpMenu.add(new Separator());
    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
    helpMenu.add(new Separator());

    helpMenu.add(getAction(ActionFactory.ABOUT.getId()));
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
   */
  protected void fillTrayItem(IMenuManager trayItem) {
    trayItem.add(new ReloadAllAction());
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
    coolBarContextMenuManager.add(getAction(ActionFactory.LOCK_TOOL_BAR.getId()));
    //    coolBarContextMenuManager.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
    coolBarContextMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
  }
}