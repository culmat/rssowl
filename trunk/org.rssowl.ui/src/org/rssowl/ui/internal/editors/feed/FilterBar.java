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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.persist.pref.IPreferenceScope;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.JobTracker;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * The FilterBar is the central control to filter News that are showing in the
 * FeedView. It supports filtering, grouping and a quick-search.
 *
 * @author bpasero
 */
public class FilterBar {

  /* Action to Filter News */
  private static final String FILTER_ACTION = "org.rssowl.ui.internal.editors.feed.FilterAction";

  /* Action to Group News */
  private static final String GROUP_ACTION = "org.rssowl.ui.internal.editors.feed.GroupAction";

  /* Toggle vertical/horizontal Layout */
  private static final String TOGGLE_LAYOUT_ACTION = "org.rssowl.ui.internal.editors.feed.ToggleLayoutAction";

  /* Maximize Browser */
  private static final String TOGGLE_MAXIMIZED_ACTION = "org.rssowl.ui.internal.editors.feed.ToggleMaximizedAction";

  private Composite fParent;
  private ToolBarManager fSecondToolBarManager;
  private ToolBarManager fFirstToolBarManager;
  private FeedView fFeedView;
  private JobTracker fQuickSearchTracker;
  private Text fSearchInput;
  private Label fFilterLabel;
  private boolean fLayoutVertical;
  private IPreferenceScope fGlobalPreferences;
  private boolean fMaximized;

  /**
   * @param feedView
   * @param parent
   */
  public FilterBar(FeedView feedView, Composite parent) {
    fFeedView = feedView;
    fParent = parent;
    fLayoutVertical = feedView.fInitialLayoutVertical;
    fQuickSearchTracker = new JobTracker(500, false, true, ITask.Priority.SHORT);
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fMaximized = fGlobalPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);;
    createControl();
  }

  /** Clear the Quick-Search */
  public void clearQuickSearch() {
    if (fSearchInput.getText().length() != 0)
      fSearchInput.setText(""); //$NON-NLS-1$
  }

  /** Give Focus to the Quicksearch Input */
  public void focusQuickSearch() {
    fSearchInput.setFocus();
  }

  private void createControl() {
    Composite container = new Composite(fParent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(6, 3, 0));
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Filter */
    fFirstToolBarManager = new ToolBarManager(SWT.FLAT);
    createFilterBar();
    fFirstToolBarManager.createControl(container);
    fFirstToolBarManager.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* Link to show and change Filter */
    fFilterLabel = new Label(container, SWT.NONE);
    fFilterLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fFilterLabel.setText(fFeedView.getFilter().getType().getName());

    /* Separator */
    Label sep = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
    sep.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) sep.getLayoutData()).heightHint = 18;

    fSecondToolBarManager = new ToolBarManager(SWT.FLAT);

    /* Group By */
    createGrouperBar();

    /* Toggle Layout */
    createLayoutBar();

    fSecondToolBarManager.createControl(container);
    fSecondToolBarManager.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* Separator */
    sep = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
    sep.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) sep.getLayoutData()).heightHint = 18;

    /* Quick Search */
    createQuickSearch(container);
  }

  /* Quick Search */
  private void createQuickSearch(Composite parent) {
    Composite searchContainer = new Composite(parent, SWT.NONE);
    searchContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    ((GridLayout) searchContainer.getLayout()).marginTop = 1;
    searchContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) searchContainer.getLayoutData()).widthHint = 180;

    final ToolBarManager manager = new ToolBarManager(SWT.FLAT);
    final NewsFilter filter = fFeedView.getFilter();

    IAction quickSearch = new Action("Quick Search", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {

        /* Show Menu */
        getMenuCreator().getMenu(manager.getControl()).setVisible(true);
      }
    };
    quickSearch.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/find.gif"));

    quickSearch.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Search on: Subject */
        final MenuItem searchHeadline = new MenuItem(menu, SWT.RADIO);
        searchHeadline.setText(NewsFilter.SearchTarget.HEADLINE.getName());
        searchHeadline.setSelection(NewsFilter.SearchTarget.HEADLINE == filter.getSearchTarget());
        searchHeadline.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchHeadline.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.HEADLINE)
              doSearch(NewsFilter.SearchTarget.HEADLINE);
          }
        });

        /* Search on: Entire News */
        final MenuItem searchEntireNews = new MenuItem(menu, SWT.RADIO);
        searchEntireNews.setText(NewsFilter.SearchTarget.ALL.getName());
        searchEntireNews.setSelection(NewsFilter.SearchTarget.ALL == filter.getSearchTarget());
        searchEntireNews.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchEntireNews.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.ALL)
              doSearch(NewsFilter.SearchTarget.ALL);
          }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        /* Search on: Author */
        final MenuItem searchAuthor = new MenuItem(menu, SWT.RADIO);
        searchAuthor.setText(NewsFilter.SearchTarget.AUTHOR.getName());
        searchAuthor.setSelection(NewsFilter.SearchTarget.AUTHOR == filter.getSearchTarget());
        searchAuthor.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchAuthor.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.AUTHOR)
              doSearch(NewsFilter.SearchTarget.AUTHOR);
          }
        });

        /* Search on: Category */
        final MenuItem searchCategory = new MenuItem(menu, SWT.RADIO);
        searchCategory.setText(NewsFilter.SearchTarget.CATEGORY.getName());
        searchCategory.setSelection(NewsFilter.SearchTarget.CATEGORY == filter.getSearchTarget());
        searchCategory.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchCategory.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.CATEGORY)
              doSearch(NewsFilter.SearchTarget.CATEGORY);
          }
        });

        /* Search on: Source */
        final MenuItem searchSource = new MenuItem(menu, SWT.RADIO);
        searchSource.setText(NewsFilter.SearchTarget.SOURCE.getName());
        searchSource.setSelection(NewsFilter.SearchTarget.SOURCE == filter.getSearchTarget());
        searchSource.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchSource.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.SOURCE)
              doSearch(NewsFilter.SearchTarget.SOURCE);
          }
        });

        /* Search on: Attachments */
        final MenuItem searchAttachments = new MenuItem(menu, SWT.RADIO);
        searchAttachments.setText(NewsFilter.SearchTarget.ATTACHMENTS.getName());
        searchAttachments.setSelection(NewsFilter.SearchTarget.ATTACHMENTS == filter.getSearchTarget());
        searchAttachments.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchAttachments.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.ATTACHMENTS)
              doSearch(NewsFilter.SearchTarget.ATTACHMENTS);
          }
        });

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });

    manager.add(quickSearch);
    manager.createControl(searchContainer);

    /* Input for the Search */
    fSearchInput = new Text(searchContainer, SWT.BORDER | SWT.SINGLE | SWT.SEARCH);
    fSearchInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fSearchInput.setMessage(fFeedView.getFilter().getSearchTarget().getName());

    /* Register this Input Field to Context Service */
    Controller.getDefault().getContextService().registerInputField(fSearchInput);

    /* Reset any Filter if set on ESC */
    fSearchInput.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.ESC)
          clearQuickSearch();
      }
    });

    /* Handle the CR Key Pressed */
    fSearchInput.addTraverseListener(new TraverseListener() {
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_RETURN || e.detail == SWT.TRAVERSE_PAGE_NEXT || e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
          e.doit = false;
          fFeedView.handleQuicksearchTraversalEvent(e.detail);
        }
      }
    });

    /* Run search when text is entered */
    fSearchInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {

        /* Hint is visible - ignore */
        if (fSearchInput.getData() != null)
          return;

        /* Clear Search immediately */
        if (fSearchInput.getText().length() == 0 && fFeedView.getFilter().isPatternSet()) {
          fFeedView.getFilter().setPattern(fSearchInput.getText());
          fFeedView.refresh(true, false);
        }

        /* Run Search in JobTracker */
        else if (fSearchInput.getText().length() > 0) {
          fQuickSearchTracker.run(new TaskAdapter() {
            public IStatus run(IProgressMonitor monitor) {
              fFeedView.getFilter().setPattern(fSearchInput.getText());
              fFeedView.refresh(true, false);
              return Status.OK_STATUS;
            }
          });
        }
      }
    });

    fSearchInput.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(true);
      }

      public void focusLost(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
      }
    });
  }

  /* Layout */
  private void createLayoutBar() {

    /* Toggle Layout */
    final ImageDescriptor horizontalImg = OwlUI.getImageDescriptor("icons/etool16/horizontal.gif");
    final ImageDescriptor verticalImg = OwlUI.getImageDescriptor("icons/etool16/vertical.gif");

    /* Toggle Layout */
    IAction toggleLayout = new Action("Toggle Layout", IAction.AS_PUSH_BUTTON) {

      @Override
      public void run() {
        fFeedView.toggleLayout();
        fLayoutVertical = !fLayoutVertical;
        fSecondToolBarManager.find(TOGGLE_LAYOUT_ACTION).update(IAction.IMAGE);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        if (fLayoutVertical)
          return horizontalImg;
        return verticalImg;
      }
    };
    toggleLayout.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/vertical.gif"));
    toggleLayout.setId(TOGGLE_LAYOUT_ACTION);
    fSecondToolBarManager.add(toggleLayout);

    /* Maximize / Minimize Browser */
    final ImageDescriptor img = OwlUI.getImageDescriptor("icons/etool16/browsermaximized.gif");

    IAction toggleMaximized = new Action("", IAction.AS_CHECK_BOX) {

      @Override
      public void run() {
        fFeedView.toggleNewsViewMaximized();
        fMaximized = !fMaximized;
        fSecondToolBarManager.find(TOGGLE_MAXIMIZED_ACTION).update(IAction.TOOL_TIP_TEXT);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return img;
      }

      @Override
      public String getToolTipText() {
        if (fMaximized)
          return "Show Headlines";

        return "Hide Headlines";
      }
    };
    toggleMaximized.setId(TOGGLE_MAXIMIZED_ACTION);
    toggleMaximized.setChecked(fMaximized);

    fSecondToolBarManager.add(toggleMaximized);
  }

  /* News Filter */
  private void createFilterBar() {
    final NewsFilter filter = fFeedView.getFilter();

    IAction newsFilterAction = new Action("Filter News", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {

        /* Restore Default */
        if (filter.getType() != NewsFilter.Type.SHOW_ALL)
          doFilter(NewsFilter.Type.SHOW_ALL, true, true);

        /* Show Menu */
        else
          getMenuCreator().getMenu(fFirstToolBarManager.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        if (filter.getType() == NewsFilter.Type.SHOW_ALL)
          return OwlUI.getImageDescriptor("icons/etool16/filter.gif"); //$NON-NLS-1$

        return OwlUI.getImageDescriptor("icons/etool16/filter_active.gif"); //$NON-NLS-1$
      }
    };
    newsFilterAction.setId(FILTER_ACTION);
    fFirstToolBarManager.add(newsFilterAction);

    newsFilterAction.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Filter: None */
        final MenuItem showAll = new MenuItem(menu, SWT.RADIO);
        showAll.setText(NewsFilter.Type.SHOW_ALL.getName());
        showAll.setSelection(NewsFilter.Type.SHOW_ALL == filter.getType());
        showAll.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showAll.getSelection() && filter.getType() != NewsFilter.Type.SHOW_ALL)
              doFilter(NewsFilter.Type.SHOW_ALL, true, true);
          }
        });
        menu.setDefaultItem(showAll);

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: New */
        final MenuItem showNew = new MenuItem(menu, SWT.RADIO);
        showNew.setText(NewsFilter.Type.SHOW_NEW.getName());
        showNew.setSelection(NewsFilter.Type.SHOW_NEW == filter.getType());
        showNew.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showNew.getSelection() && filter.getType() != NewsFilter.Type.SHOW_NEW)
              doFilter(NewsFilter.Type.SHOW_NEW, true, true);
          }
        });

        /* Filter: Unread */
        final MenuItem showUnread = new MenuItem(menu, SWT.RADIO);
        showUnread.setText(NewsFilter.Type.SHOW_UNREAD.getName());
        showUnread.setSelection(NewsFilter.Type.SHOW_UNREAD == filter.getType());
        showUnread.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showUnread.getSelection() && filter.getType() != NewsFilter.Type.SHOW_UNREAD)
              doFilter(NewsFilter.Type.SHOW_UNREAD, true, true);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: Recent News */
        final MenuItem showRecent = new MenuItem(menu, SWT.RADIO);
        showRecent.setText(NewsFilter.Type.SHOW_RECENT.getName());
        showRecent.setSelection(NewsFilter.Type.SHOW_RECENT == filter.getType());
        showRecent.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showRecent.getSelection() && filter.getType() != NewsFilter.Type.SHOW_RECENT)
              doFilter(NewsFilter.Type.SHOW_RECENT, true, true);
          }
        });

        /* Filter: Sticky */
        final MenuItem showSticky = new MenuItem(menu, SWT.RADIO);
        showSticky.setText(NewsFilter.Type.SHOW_STICKY.getName());
        showSticky.setSelection(NewsFilter.Type.SHOW_STICKY == filter.getType());
        showSticky.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showSticky.getSelection() && filter.getType() != NewsFilter.Type.SHOW_STICKY)
              doFilter(NewsFilter.Type.SHOW_STICKY, true, true);
          }
        });

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });
  }

  void doFilter(final NewsFilter.Type type, boolean refresh, boolean saveSettings) {
    fFeedView.getFilter().setType(type);
    fFilterLabel.setText(type.getName());
    fFilterLabel.getParent().layout();
    fFirstToolBarManager.find(FILTER_ACTION).update(IAction.IMAGE);

    /* Refresh if set */
    if (refresh)
      fFeedView.refresh(true, false);

    /* Update Settings */
    if (saveSettings) {
      JobRunner.runInBackgroundThread(new Runnable() {
        public void run() {
          fGlobalPreferences.putInteger(DefaultPreferences.FV_FILTER_TYPE, type.ordinal());
        }
      });
    }
  }

  private void doSearch(final NewsFilter.SearchTarget target) {
    fFeedView.getFilter().setSearchTarget(target);
    fSearchInput.setMessage(fFeedView.getFilter().getSearchTarget().getName());
    fSearchInput.setFocus();

    if (fSearchInput.getText().length() > 0)
      fFeedView.refresh(true, false);

    /* Update Settings */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        fGlobalPreferences.putInteger(DefaultPreferences.FV_SEARCH_TARGET, target.ordinal());
      }
    });
  }

  /* News Group */
  private void createGrouperBar() {
    final NewsGrouping grouping = fFeedView.getGrouper();

    final IAction newsGroup = new Action("Group News", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {

        /* Restore Default */
        if (fFeedView.getGrouper().getType() != NewsGrouping.Type.NO_GROUPING)
          doGrouping(NewsGrouping.Type.NO_GROUPING, true, true);

        /* Show Menu */
        else
          getMenuCreator().getMenu(fSecondToolBarManager.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        if (grouping.getType() == NewsGrouping.Type.NO_GROUPING)
          return OwlUI.getImageDescriptor("icons/etool16/group.gif"); //$NON-NLS-1$

        return OwlUI.getImageDescriptor("icons/etool16/group_active.gif"); //$NON-NLS-1$
      }
    };

    newsGroup.setId(GROUP_ACTION);

    newsGroup.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Group: None */
        final MenuItem noGrouping = new MenuItem(menu, SWT.RADIO);
        noGrouping.setText(NewsGrouping.Type.NO_GROUPING.getName());
        noGrouping.setSelection(grouping.getType() == NewsGrouping.Type.NO_GROUPING);
        noGrouping.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (noGrouping.getSelection() && grouping.getType() != NewsGrouping.Type.NO_GROUPING)
              doGrouping(NewsGrouping.Type.NO_GROUPING, true, true);
          }
        });
        menu.setDefaultItem(noGrouping);

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Group: By Date */
        final MenuItem groupByDate = new MenuItem(menu, SWT.RADIO);
        groupByDate.setText(NewsGrouping.Type.GROUP_BY_DATE.getName());
        groupByDate.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_DATE);
        groupByDate.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByDate.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_DATE)
              doGrouping(NewsGrouping.Type.GROUP_BY_DATE, true, true);
          }
        });

        /* Group: By Author */
        final MenuItem groupByAuthor = new MenuItem(menu, SWT.RADIO);
        groupByAuthor.setText(NewsGrouping.Type.GROUP_BY_AUTHOR.getName());
        groupByAuthor.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_AUTHOR);
        groupByAuthor.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByAuthor.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_AUTHOR)
              doGrouping(NewsGrouping.Type.GROUP_BY_AUTHOR, true, true);
          }
        });

        /* Group: By Category */
        final MenuItem groupByCategory = new MenuItem(menu, SWT.RADIO);
        groupByCategory.setText(NewsGrouping.Type.GROUP_BY_CATEGORY.getName());
        groupByCategory.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_CATEGORY);
        groupByCategory.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByCategory.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_CATEGORY)
              doGrouping(NewsGrouping.Type.GROUP_BY_CATEGORY, true, true);
          }
        });

        /* Group: By Topic */
        final MenuItem groupByTopic = new MenuItem(menu, SWT.RADIO);
        groupByTopic.setText(NewsGrouping.Type.GROUP_BY_TOPIC.getName());
        groupByTopic.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_TOPIC);
        groupByTopic.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByTopic.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_TOPIC)
              doGrouping(NewsGrouping.Type.GROUP_BY_TOPIC, true, true);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Group: By Other */
        final MenuItem groupByOther = new MenuItem(menu, SWT.CASCADE);
        groupByOther.setText("Other");
        Menu otherMenu = new Menu(groupByOther);
        groupByOther.setMenu(otherMenu);

        /* Group: By State */
        final MenuItem groupByState = new MenuItem(otherMenu, SWT.RADIO);
        groupByState.setText(NewsGrouping.Type.GROUP_BY_STATE.getName());
        groupByState.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_STATE);
        groupByState.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByState.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_STATE)
              doGrouping(NewsGrouping.Type.GROUP_BY_STATE, true, true);
          }
        });

        /* Group: By Stickyness */
        final MenuItem groupByStickyness = new MenuItem(otherMenu, SWT.RADIO);
        groupByStickyness.setText(NewsGrouping.Type.GROUP_BY_STICKY.getName());
        groupByStickyness.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_STICKY);
        groupByStickyness.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByStickyness.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_STICKY)
              doGrouping(NewsGrouping.Type.GROUP_BY_STICKY, true, true);
          }
        });

        /* Separator */
        new MenuItem(otherMenu, SWT.SEPARATOR);

        /* Group: By Label */
        final MenuItem groupByLabel = new MenuItem(otherMenu, SWT.RADIO);
        groupByLabel.setText(NewsGrouping.Type.GROUP_BY_LABEL.getName());
        groupByLabel.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_LABEL);
        groupByLabel.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByLabel.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_LABEL)
              doGrouping(NewsGrouping.Type.GROUP_BY_LABEL, true, true);
          }
        });

        /* Group: By Rating */
        final MenuItem groupByRating = new MenuItem(otherMenu, SWT.RADIO);
        groupByRating.setText(NewsGrouping.Type.GROUP_BY_RATING.getName());
        groupByRating.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_RATING);
        groupByRating.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByRating.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_RATING)
              doGrouping(NewsGrouping.Type.GROUP_BY_RATING, true, true);
          }
        });

        /* Separator */
        new MenuItem(otherMenu, SWT.SEPARATOR);

        /* Group: By Feed */
        final MenuItem groupByFeed = new MenuItem(otherMenu, SWT.RADIO);
        groupByFeed.setText(NewsGrouping.Type.GROUP_BY_FEED.getName());
        groupByFeed.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_FEED);
        groupByFeed.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByFeed.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_FEED)
              doGrouping(NewsGrouping.Type.GROUP_BY_FEED, true, true);
          }
        });

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });

    fSecondToolBarManager.add(newsGroup);
  }

  void doGrouping(final NewsGrouping.Type type, boolean refresh, boolean saveSettings) {
    fFeedView.getGrouper().setType(type);
    fSecondToolBarManager.find(GROUP_ACTION).update(IAction.IMAGE);

    /* Refresh if set */
    if (refresh)
      fFeedView.refresh(true, false);

    /* Update Settings */
    if (saveSettings) {
      JobRunner.runInBackgroundThread(new Runnable() {
        public void run() {
          fGlobalPreferences.putInteger(DefaultPreferences.FV_GROUP_TYPE, type.ordinal());
        }
      });
    }
  }
}