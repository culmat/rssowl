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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationActionBarAdvisor;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.ArchiveNewsAction;
import org.rssowl.ui.internal.actions.AutomateFilterAction;
import org.rssowl.ui.internal.actions.CreateFilterAction.PresetAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.NavigationActionFactory;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.dialogs.SearchNewsDialog;
import org.rssowl.ui.internal.editors.feed.NewsBrowserLabelProvider.Dynamic;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.StickyOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.CBrowser;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author bpasero
 */
public class NewsBrowserViewer extends ContentViewer implements ILinkHandler {

  /* ID for Link Handlers */
  static final String TOGGLE_READ_HANDLER_ID = "org.rssowl.ui.ToggleRead"; //$NON-NLS-1$
  static final String TOGGLE_STICKY_HANDLER_ID = "org.rssowl.ui.ToggleSticky"; //$NON-NLS-1$
  static final String ARCHIVE_HANDLER_ID = "org.rssowl.ui.Archive"; //$NON-NLS-1$
  static final String DELETE_HANDLER_ID = "org.rssowl.ui.Delete"; //$NON-NLS-1$
  static final String ATTACHMENT_HANDLER_ID = "org.rssowl.ui.DownloadAttachment"; //$NON-NLS-1$
  static final String ATTACHMENTS_MENU_HANDLER_ID = "org.rssowl.ui.AttachmentsMenu"; //$NON-NLS-1$
  static final String LABELS_MENU_HANDLER_ID = "org.rssowl.ui.LabelsMenu"; //$NON-NLS-1$
  static final String NEWS_MENU_HANDLER_ID = "org.rssowl.ui.NewsMenu"; //$NON-NLS-1$
  static final String SHARE_NEWS_MENU_HANDLER_ID = "org.rssowl.ui.ShareNewsMenu"; //$NON-NLS-1$
  static final String NEXT_NEWS_HANDLER_ID = "org.rssowl.ui.NextNews"; //$NON-NLS-1$
  static final String NEXT_UNREAD_NEWS_HANDLER_ID = "org.rssowl.ui.NextUnreadNews"; //$NON-NLS-1$
  static final String PREVIOUS_NEWS_HANDLER_ID = "org.rssowl.ui.PreviousNews"; //$NON-NLS-1$
  static final String PREVIOUS_UNREAD_NEWS_HANDLER_ID = "org.rssowl.ui.PreviousUnreadNews"; //$NON-NLS-1$
  static final String TRANSFORM_HANDLER_ID = "org.rssowl.ui.TransformNews"; //$NON-NLS-1$
  static final String RELATED_NEWS_MENU_HANDLER_ID = "org.rssowl.ui.RelatedNewsMenu"; //$NON-NLS-1$

  private Object fInput;
  private CBrowser fBrowser;
  private IWorkbenchPartSite fSite;
  private boolean fIsEmbedded;
  private Menu fNewsContextMenu;
  private Menu fAttachmentsContextMenu;
  private Menu fLabelsContextMenu;
  private Menu fShareNewsContextMenu;
  private Menu fFindRelatedContextMenu;
  private IStructuredSelection fCurrentSelection = StructuredSelection.EMPTY;
  private ApplicationServer fServer;
  private String fId;
  private boolean fBlockRefresh;
  private IModelFactory fFactory;
  private IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();
  private INewsDAO fNewsDao = DynamicDAO.getDAO(INewsDAO.class);

  /* This viewer's sorter. <code>null</code> means there is no sorter. */
  private ViewerComparator fSorter;

  /* This viewer's filters (element type: <code>ViewerFilter</code>). */
  private List<ViewerFilter> fFilters;
  private NewsFilter fNewsFilter;

  /**
   * @param parent
   * @param style
   */
  public NewsBrowserViewer(Composite parent, int style) {
    this(parent, style, null);
  }

  /**
   * @param parent
   * @param style
   * @param site
   */
  public NewsBrowserViewer(Composite parent, int style, IWorkbenchPartSite site) {
    fBrowser = new CBrowser(parent, style);
    fSite = site;
    fIsEmbedded = (fSite != null);
    hookControl(fBrowser.getControl());
    hookNewsContextMenu();
    hookAttachmentsContextMenu();
    hookLabelContextMenu();
    hookShareNewsContextMenu();
    hookFindRelatedContextMenu();
    fId = String.valueOf(hashCode());
    fServer = ApplicationServer.getDefault();
    fServer.register(fId, this);
    fFactory = Owl.getModelFactory();

    /* Register Link Handler */
    fBrowser.addLinkHandler(TOGGLE_READ_HANDLER_ID, this);
    fBrowser.addLinkHandler(TOGGLE_STICKY_HANDLER_ID, this);
    fBrowser.addLinkHandler(ARCHIVE_HANDLER_ID, this);
    fBrowser.addLinkHandler(DELETE_HANDLER_ID, this);
    fBrowser.addLinkHandler(ATTACHMENT_HANDLER_ID, this);
    fBrowser.addLinkHandler(ATTACHMENTS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(LABELS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEWS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(SHARE_NEWS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEXT_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEXT_UNREAD_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(PREVIOUS_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(PREVIOUS_UNREAD_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(TRANSFORM_HANDLER_ID, this);
    fBrowser.addLinkHandler(RELATED_NEWS_MENU_HANDLER_ID, this);
  }

  private void hookNewsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @SuppressWarnings("restriction")
      public void menuAboutToShow(IMenuManager manager) {

        /* Open */
        {
          boolean useSeparator = true;

          /* Open in FeedView */
          if (!fIsEmbedded) {
            manager.add(new Separator("internalopen")); //$NON-NLS-1$
            if (!fCurrentSelection.isEmpty()) {
              manager.appendToGroup("internalopen", new OpenNewsAction(fCurrentSelection, fBrowser.getControl().getShell())); //$NON-NLS-1$
              useSeparator = false;
            }
          }

          manager.add(useSeparator ? new Separator("open") : new GroupMarker("open")); //$NON-NLS-1$ //$NON-NLS-2$

          /* Show only when internal browser is used */
          if (!fCurrentSelection.isEmpty() && !OwlUI.useExternalBrowser())
            manager.add(new OpenInExternalBrowserAction(fCurrentSelection));
        }

        /* Attachments */
        {
          ApplicationActionBarAdvisor.fillAttachmentsMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        /* Mark / Label */
        {
          manager.add(new Separator("mark")); //$NON-NLS-1$

          /* Mark */
          MenuManager markMenu = new MenuManager(Messages.NewsBrowserViewer_MARK, "mark"); //$NON-NLS-1$
          manager.add(markMenu);

          /* Mark as Read */
          IAction action = new ToggleReadStateAction(fCurrentSelection);
          action.setEnabled(!fCurrentSelection.isEmpty());
          markMenu.add(action);

          /* Mark All Read */
          action = new MarkAllNewsReadAction();
          markMenu.add(action);

          /* Sticky */
          markMenu.add(new Separator());
          action = new MakeNewsStickyAction(fCurrentSelection);
          action.setEnabled(!fCurrentSelection.isEmpty());
          markMenu.add(action);

          /* Label */
          ApplicationActionBarAdvisor.fillLabelMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        /* Move To / Copy To */
        if (!fCurrentSelection.isEmpty()) {
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
          MenuManager moveMenu = new MenuManager(Messages.NewsBrowserViewer_MOVE_TO, "moveto"); //$NON-NLS-1$
          manager.add(moveMenu);

          for (INewsBin bin : newsbins) {
            if (contained(bin, fCurrentSelection))
              continue;

            moveMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, bin, true));
          }

          moveMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, null, true));
          moveMenu.add(new Separator());
          moveMenu.add(new AutomateFilterAction(PresetAction.MOVE, fCurrentSelection));

          /* Copy To */
          MenuManager copyMenu = new MenuManager(Messages.NewsBrowserViewer_COPY_TO, "copyto"); //$NON-NLS-1$
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            if (contained(bin, fCurrentSelection))
              continue;

            copyMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, bin, false));
          }

          copyMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, null, false));
          copyMenu.add(new Separator());
          copyMenu.add(new AutomateFilterAction(PresetAction.COPY, fCurrentSelection));

          /* Archive */
          manager.add(new ArchiveNewsAction(fCurrentSelection));
        }

        /* Share */
        {
          ApplicationActionBarAdvisor.fillShareMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        manager.add(new Separator("filter")); //$NON-NLS-1$
        manager.add(new Separator("copy")); //$NON-NLS-1$
        manager.add(new GroupMarker("edit")); //$NON-NLS-1$
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        /* Fill Contributions if Context Menu not registered */
        if (fSite == null)
          org.eclipse.ui.internal.ObjectActionContributorManager.getManager().contributeObjectActions(null, manager, NewsBrowserViewer.this);
      }
    });

    /* Create and Register with Workbench */
    fNewsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());

    /* Register with Part Site if possible */
    if (fSite != null)
      fSite.registerContextMenu(manager, this);
  }

  private void hookAttachmentsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillAttachmentsMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fAttachmentsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void hookLabelContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillLabelMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fLabelsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void hookShareNewsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillShareMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fShareNewsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void hookFindRelatedContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        if (fCurrentSelection.size() == 1) {
          Object element = fCurrentSelection.getFirstElement();
          if (element instanceof INews) {
            final INews news = (INews) element;
            final String entity = INews.class.getName();

            /* Find Related by Title */
            manager.add(new Action(Messages.NewsBrowserViewer_SIMILAR_CONTENT) {
              @Override
              public void run() {
                List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
                String headline = CoreUtils.getHeadline(news, false);

                ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, entity);
                ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, headline);
                conditions.add(condition);

                doSearch(conditions, true);
              };
            });

            /* Find Related by Author */
            if (news.getAuthor() != null) {
              IPerson person = news.getAuthor();
              String name = person.getName();
              String email = (person.getEmail() != null) ? person.getEmail().toASCIIString() : null;

              final String author = StringUtils.isSet(name) ? name : email;
              if (StringUtils.isSet(author)) {
                manager.add(new Separator());
                manager.add(new Action(NLS.bind(Messages.NewsBrowserViewer_AUTHORED_BY, author)) {
                  @Override
                  public void run() {
                    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);

                    ISearchField field = fFactory.createSearchField(INews.AUTHOR, entity);
                    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, author);
                    conditions.add(condition);

                    doSearch(conditions, false);
                  };
                });
              }
            }

            /* Find Related by Category */
            if (!news.getCategories().isEmpty()) {

              /* Directly show for one category */
              if (news.getCategories().size() == 1) {
                final String name = news.getCategories().get(0).getName();
                if (StringUtils.isSet(name)) {
                  manager.add(new Action(NLS.bind(Messages.NewsBrowserViewer_CATEGORIZED_N, name)) {
                    @Override
                    public void run() {
                      List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);

                      ISearchField field = fFactory.createSearchField(INews.CATEGORIES, entity);
                      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, name);
                      conditions.add(condition);

                      doSearch(conditions, false);
                    };
                  });
                }
              }

              /* Use a Sub Menu for many categories */
              else {
                MenuManager categoriesMenu = new MenuManager(Messages.NewsBrowserViewer_BY_CATEGORY);
                for (ICategory category : news.getCategories()) {
                  final String name = category.getName();
                  if (StringUtils.isSet(name)) {
                    categoriesMenu.add(new Action(name) {
                      @Override
                      public void run() {
                        List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);

                        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, entity);
                        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, name);
                        conditions.add(condition);

                        doSearch(conditions, false);
                      };
                    });
                  }
                }
                manager.add(categoriesMenu);
              }
            }

            /* Find Related by Labels */
            if (!news.getLabels().isEmpty()) {
              manager.add(new Separator());
              for (final ILabel label : news.getLabels()) {
                manager.add(new Action(NLS.bind(Messages.NewsBrowserViewer_LABELED_N, label.getName())) {
                  @Override
                  public void run() {
                    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);

                    ISearchField field = fFactory.createSearchField(INews.LABEL, entity);
                    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, label.getName());
                    conditions.add(condition);

                    doSearch(conditions, false);
                  };
                });
              }
            }
          }
        }
      }
    });

    /* Create  */
    fFindRelatedContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void doSearch(final List<ISearchCondition> conditions, final boolean useLowScoreFilter) {
    if (conditions.size() >= 1 && !fBrowser.getControl().isDisposed()) {

      /* See Bug 747 - run asynced */
      delayInUI(new Runnable() {
        public void run() {
          SearchNewsDialog dialog = new SearchNewsDialog(fBrowser.getControl().getShell(), conditions, true, true);
          dialog.setUseLowScoreFilter(useLowScoreFilter);
          dialog.open();
        }
      });
    }
  }

  private boolean contained(INewsBin bin, IStructuredSelection selection) {
    if (selection == null || selection.isEmpty())
      return false;

    Object element = selection.getFirstElement();
    if (element instanceof INews) {
      INews news = (INews) element;
      return news.getParentId() == bin.getId();
    }

    return false;
  }

  void setBlockRefresh(boolean block) {
    fBlockRefresh = block;
  }

  /*
   * @see org.rssowl.ui.internal.ILinkHandler#handle(java.lang.String, java.net.URI)
   */
  public void handle(String id, URI link) {

    /* Extract Query Part and Decode */
    String query = link.getQuery();
    boolean queryProvided = StringUtils.isSet(query);
    if (queryProvided) {
      query = URIUtils.urlDecode(query).trim();
      queryProvided = StringUtils.isSet(query);
    }

    /*  Toggle Read */
    if (queryProvided && TOGGLE_READ_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.TOGGLE_READ_LINK.getId(news));

        /* Update State */
        INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD : INews.State.READ;
        Set<INews> singleNewsSet = Collections.singleton(news);
        boolean affectEquivalentNews = (newState != INews.State.UNREAD && OwlUI.markReadDuplicates());
        UndoStack.getInstance().addOperation(new NewsStateOperation(singleNewsSet, newState, affectEquivalentNews));
        fNewsDao.setState(singleNewsSet, newState, affectEquivalentNews, false);
      }
    }

    /*  Toggle Sticky */
    else if (queryProvided && TOGGLE_STICKY_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.TOGGLE_STICKY_LINK.getId(news));

        /* Toggle Sticky State */
        Set<INews> singleNewsSet = Collections.singleton(news);
        UndoStack.getInstance().addOperation(new StickyOperation(singleNewsSet, !news.isFlagged()));
        news.setFlagged(!news.isFlagged());
        Controller.getDefault().getSavedSearchService().forceQuickUpdate();
        DynamicDAO.saveAll(singleNewsSet);
      }
    }

    /*  Archive */
    else if (queryProvided && ARCHIVE_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        ArchiveNewsAction action = new ArchiveNewsAction(new StructuredSelection(news));
        action.run();
      }
    }

    /*  Delete */
    else if (queryProvided && DELETE_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        Set<INews> singleNewsSet = Collections.singleton(news);
        UndoStack.getInstance().addOperation(new NewsStateOperation(singleNewsSet, INews.State.HIDDEN, false));
        fNewsDao.setState(singleNewsSet, INews.State.HIDDEN, false, false);
      }
    }

    /*  Labels Menu */
    else if (queryProvided && LABELS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.LABELS_MENU_LINK.getId(news));

        /* Show Menu */
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fLabelsContextMenu.setLocation(cursorLocation);
        fLabelsContextMenu.setVisible(true);
      }
    }

    /*  Attachments Menu */
    else if (queryProvided && (ATTACHMENTS_MENU_HANDLER_ID.equals(id) || ATTACHMENT_HANDLER_ID.equals(id))) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        if (ATTACHMENT_HANDLER_ID.equals(id))
          blur(Dynamic.ATTACHMENT_LINK.getId(news));
        else if (ATTACHMENTS_MENU_HANDLER_ID.equals(id))
          blur(Dynamic.ATTACHMENTS_MENU_LINK.getId(news));

        /* Show Menu */
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fAttachmentsContextMenu.setLocation(cursorLocation);
        fAttachmentsContextMenu.setVisible(true);
      }
    }

    /* News Context Menu */
    else if (queryProvided && NEWS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.NEWS_MENU_LINK.getId(news));

        /* Show Menu */
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fNewsContextMenu.setLocation(cursorLocation);
        fNewsContextMenu.setVisible(true);
      }
    }

    /* Share News Context Menu */
    else if (queryProvided && SHARE_NEWS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.SHARE_MENU_LINK.getId(news));

        /* Show Menu */
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fShareNewsContextMenu.setLocation(cursorLocation);
        fShareNewsContextMenu.setVisible(true);
      }
    }

    /* Find Related Context Menu */
    else if (queryProvided && RELATED_NEWS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        /* Remove Focus from Link */
        blur(Dynamic.FIND_RELATED_MENU_LINK.getId(news));

        /* Show Menu */
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fFindRelatedContextMenu.setLocation(cursorLocation);
        fFindRelatedContextMenu.setVisible(true);
      }
    }

    /* Go to Next News */
    else if (NEXT_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.NEXT_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Next Unread News */
    else if (NEXT_UNREAD_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.NEXT_UNREAD_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Previous News */
    else if (PREVIOUS_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.PREVIOUS_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Previous Unread News */
    else if (PREVIOUS_UNREAD_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.PREVIOUS_UNREAD_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Transform News */
    else if (TRANSFORM_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null)
        transformNews(news);
    }
  }

  private void blur(String elementId) {
    StringBuilder js = new StringBuilder();
    js.append(getElementById(elementId).append(".blur();")); //$NON-NLS-1$
    fBrowser.execute(js.toString());
  }

  private void transformNews(final INews news) {
    String link = CoreUtils.getLink(news);
    if (!StringUtils.isSet(link))
      return;

    /* Indicate Progress */
    StringBuilder js = new StringBuilder();
    js.append(getElementById(Dynamic.FULL_CONTENT_LINK_TEXT.getId(news)).append(".innerText='").append(Messages.NewsBrowserViewer_LOADING).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
    js.append(getElementById(Dynamic.FULL_CONTENT_LINK.getId(news)).append(".blur(); ")); //$NON-NLS-1$
    fBrowser.execute(js.toString());

    /* First cancel all running jobs for this news if any */
    NewsReference reference = news.toReference();
    Job.getJobManager().cancel(reference);

    /* Load news content in background and update HTML afterwards */
    final String transformedUrl = Controller.getDefault().getEmbeddedTransformedUrl(link);
    UIBackgroundJob transformationJob = new UIBackgroundJob(fBrowser.getControl(), Messages.NewsBrowserViewer_RETRIEVING_ARTICLE_CONTENT, reference) {
      StringBuilder result = new StringBuilder();

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        try {
          URI uri = new URI(transformedUrl);
          IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);
          if (handler != null) {
            BufferedReader reader = null;
            try {
              InputStream inS = handler.openStream(uri, monitor, null);
              reader = new BufferedReader(new InputStreamReader(inS, "UTF-8")); //$NON-NLS-1$
              String line;
              while (!monitor.isCanceled() && (line = reader.readLine()) != null) {
                result.append(line);
              }
            } catch (IOException e) {
              Activator.getDefault().logError(e.getMessage(), e);
              monitor.setCanceled(true);
            } finally {
              if (reader != null) {
                try {
                  reader.close();
                } catch (IOException e) {
                  monitor.setCanceled(true);
                }
              }
            }
          }
        } catch (URISyntaxException e) {
          Activator.getDefault().logError(e.getMessage(), e);
          monitor.setCanceled(true);
        } catch (ConnectionException e) {
          Activator.getDefault().logError(e.getMessage(), e);
          monitor.setCanceled(true);
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {
        if (result.length() > 0 && !monitor.isCanceled() && !fBrowser.getControl().isDisposed())
          showTransformation(news, result.toString());
      }
    };

    JobRunner.runUIUpdater(transformationJob, true);
  }

  private void showTransformation(INews news, String result) {

    /* Make the result suitable to be used in JavaScript */
    result = StringUtils.replaceAll(result, "\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
    result = StringUtils.replaceAll(result, "'", "\\'"); //$NON-NLS-1$ //$NON-NLS-2$

    final StringBuilder js = new StringBuilder();
    js.append(getElementById(Dynamic.CONTENT.getId(news)).append(".innerHTML='").append(result).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
    js.append(getElementById(Dynamic.FULL_CONTENT_LINK_TEXT.getId(news)).append(".innerText='").append(Messages.NewsBrowserViewer_FULL_CONTENT).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
    js.append(getElementById(Dynamic.NEWS.getId(news))).append(".scrollIntoView(true); "); //$NON-NLS-1$

    /* Block external navigation while setting innerHTML */
    fBrowser.blockExternalNavigationWhile(new Runnable() {
      public void run() {
        fBrowser.execute(js.toString());
      }
    });
  }

  private void delayInUI(Runnable runnable) {
    JobRunner.runInUIThread(0, true, getControl(), runnable);
  }

  private INews getNews(String query) {
    try {
      long id = Long.parseLong(query);
      return fNewsDao.load(id);
    } catch (NullPointerException e) {
      return null;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setLabelProvider(org.eclipse.jface.viewers.IBaseLabelProvider)
   */
  @Override
  public void setLabelProvider(IBaseLabelProvider labelProvider) {
    fBlockRefresh = true;
    try {
      super.setLabelProvider(labelProvider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
   */
  @Override
  public void setContentProvider(IContentProvider contentProvider) {
    fBlockRefresh = true;
    try {
      super.setContentProvider(contentProvider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#refresh()
   */
  @Override
  public void refresh() {
    if (!fBlockRefresh) {
      fBrowser.refresh();
      onRefresh();
    }
  }

  /**
   * A special way of refreshing this viewer with additional options to control
   * the behavior.
   *
   * @param restoreInput if set to <code>true</code> will restore the initial
   * input that was set to the browser in case the user navigated to a different
   * URL.
   * @param moveToTop if <code>true</code> will scroll the browser to the top
   * position to reveal additional content.
   */
  public void refresh(boolean restoreInput, boolean moveToTop) {

    /* Browser not showing initial input anymore, so restore if asked for */
    if (restoreInput && !ApplicationServer.getDefault().isDisplayOperation(fBrowser.getControl().getUrl()))
      internalSetInput(fInput, true, false);

    /* Otherwise perform the normal refresh */
    else {

      /* Move scroll position to top if set */
      if (moveToTop)
        fBrowser.execute("scroll(0,0);"); //$NON-NLS-1$

      /* Refresh */
      refresh();
    }
  }

  /**
   * Method is called whenever the viewer is refreshed. Subclasses my override
   * to do something.
   */
  protected void onRefresh() {
    //Do nothing here.
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
   */
  @Override
  protected void handleDispose(DisposeEvent event) {
    fServer.unregister(fId);
    fCurrentSelection = null;
    fNewsContextMenu.dispose();
    fAttachmentsContextMenu.dispose();
    fLabelsContextMenu.dispose();
    fShareNewsContextMenu.dispose();
    super.handleDispose(event);
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setInput(java.lang.Object)
   */
  @Override
  public void setInput(Object input) {
    setInput(input, false);
  }

  /**
   * @param input the input to show in this news browser viewer.
   * @param blockExternalNavigation <code>true</code> to block any potential
   * external navigation when setting the input and <code>false</code> otherwise
   * (default).
   */
  public void setInput(Object input, boolean blockExternalNavigation) {
    internalSetInput(input, false, blockExternalNavigation);
  }

  private void internalSetInput(Object input, boolean force, boolean blockExternalNavigation) {

    /* Ignore this Input if its already set */
    if (!force && sameInput(input))
      return;

    /* Remember Input */
    fInput = input;

    /* Stop any other Website if required */
    String url = fBrowser.getControl().getUrl();
    if (!"".equals(url)) //$NON-NLS-1$
      fBrowser.getControl().stop();

    /* Input is a URL - display it */
    if (input instanceof String) {
      fBrowser.setUrl((String) input, !blockExternalNavigation);
      return;
    }

    /* Set URL if its not already showing and contains a display-operation */
    String inputUrl = fServer.toUrl(fId, input);
    if (fServer.isDisplayOperation(inputUrl) && !inputUrl.equals(url))
      fBrowser.setUrl(inputUrl);

    /* Hide the Browser as soon as the input is set to Null */
    if (input == null && fBrowser.getControl().getVisible())
      fBrowser.getControl().setVisible(false);
  }

  /* Checks wether the given Input is same to the existing one */
  private boolean sameInput(Object input) {
    if (fInput instanceof Object[])
      return input instanceof Object[] && Arrays.equals((Object[]) fInput, (Object[]) input);

    if (fInput != null)
      return fInput.equals(input);

    return false;
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#getInput()
   */
  @Override
  public Object getInput() {
    return fInput;
  }

  /**
   * Adds the given filter to this viewer.
   *
   * @param filter a viewer filter
   */
  public void addFilter(ViewerFilter filter) {
    if (fFilters == null)
      fFilters = new ArrayList<ViewerFilter>();

    fFilters.add(filter);
    if (filter instanceof NewsFilter)
      fNewsFilter = (NewsFilter) filter;
  }

  /**
   * Removes the given filter from this viewer, and triggers refiltering and
   * resorting of the elements if required. Has no effect if the identical
   * filter is not registered.
   *
   * @param filter a viewer filter
   */
  public void removeFilter(ViewerFilter filter) {
    Assert.isNotNull(filter);
    if (fFilters != null) {
      for (Iterator<ViewerFilter> i = fFilters.iterator(); i.hasNext();) {
        Object o = i.next();
        if (o == filter) {
          i.remove();
          refresh();
          if (fFilters.size() == 0)
            fFilters = null;

          return;
        }
      }
    }

    if (filter == fNewsFilter)
      fNewsFilter = null;
  }

  /**
   * @param comparator
   */
  public void setComparator(ViewerComparator comparator) {
    if (fSorter != comparator)
      fSorter = comparator;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getControl()
   */
  @Override
  public Control getControl() {
    return fBrowser.getControl();
  }

  /**
   * @return The wrapped Browser (CBrowser).
   */
  public CBrowser getBrowser() {
    return fBrowser;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getSelection()
   */
  @Override
  public ISelection getSelection() {
    return fCurrentSelection;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection,
   * boolean)
   */
  @Override
  public void setSelection(ISelection selection, boolean reveal) {
    fCurrentSelection = (IStructuredSelection) selection;
    fireSelectionChanged(new SelectionChangedEvent(this, selection));
  }

  /* Find a news from the selection and scroll it into view using JavaScript */
  void showSelection(ISelection selection) {
    if (!(selection instanceof StructuredSelection) || selection.isEmpty())
      return;

    /* Find the News to Show */
    NewsReference newsToShow = null;
    Object firstElement = ((StructuredSelection) selection).getFirstElement();
    if (firstElement instanceof INews)
      newsToShow = ((INews) firstElement).toReference();
    else if (firstElement instanceof NewsReference)
      newsToShow = (NewsReference) firstElement;

    /* Scroll the News into View if present */
    if (newsToShow != null) {
      StringBuffer js = new StringBuffer();
      js.append(getElementById(Dynamic.NEWS.getId(newsToShow))).append(".scrollIntoView(true);"); //$NON-NLS-1$
      fBrowser.execute(js.toString());
    }
  }

  /*
   * Executes JavaScript in the Browser to navigate between News.
   */
  void navigate(boolean next, boolean unread) {

    /* Create JavaScript to Execute */
    StringBuffer js = new StringBuffer();
    if (fBrowser.isIE())
      js.append("var scrollPosY = document.body.scrollTop; "); //$NON-NLS-1$
    else
      js.append("var scrollPosY = window.pageYOffset; "); //$NON-NLS-1$
    js.append("var body = document.getElementById(\"owlbody\"); "); //$NON-NLS-1$
    js.append("var divs = body.childNodes; "); //$NON-NLS-1$

    /* Next News */
    if (next) {
      js.append("  for (var i = 1; i < divs.length; i++) { "); //$NON-NLS-1$
      js.append("    if (divs[i].nodeType != 1) { "); //$NON-NLS-1$
      js.append("      continue; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("    var divPosY = divs[i].offsetTop; "); //$NON-NLS-1$
      if (unread) {
        js.append("  if (divPosY > scrollPosY && divs[i].className == \"newsitemUnread\") { "); //$NON-NLS-1$
      } else
        js.append("  if (divPosY > scrollPosY) { "); //$NON-NLS-1$
      js.append("      divs[i].scrollIntoView(true); "); //$NON-NLS-1$
      js.append("      break; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("  } "); //$NON-NLS-1$
    }

    /* Previous News */
    else {
      js.append("  for (var i = divs.length - 1; i >= 0; i--) { "); //$NON-NLS-1$
      js.append("    if (divs[i].nodeType != 1) { "); //$NON-NLS-1$
      js.append("      continue; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("    var divPosY = divs[i].offsetTop; "); //$NON-NLS-1$
      if (unread) {
        js.append("  if (divPosY < scrollPosY - 10 && divs[i].className == \"newsitemUnread\") { "); //$NON-NLS-1$
      } else
        js.append("  if (divPosY < scrollPosY - 10) { "); //$NON-NLS-1$
      js.append("      divs[i].scrollIntoView(true); "); //$NON-NLS-1$
      js.append("      break; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("  } "); //$NON-NLS-1$
    }

    /* See if the Scroll Position Changed at all and handle */
    String actionId;
    if (next) {
      if (unread)
        actionId = NewsBrowserViewer.NEXT_UNREAD_NEWS_HANDLER_ID;
      else
        actionId = NewsBrowserViewer.NEXT_NEWS_HANDLER_ID;
    } else {
      if (unread)
        actionId = NewsBrowserViewer.PREVIOUS_UNREAD_NEWS_HANDLER_ID;
      else
        actionId = NewsBrowserViewer.PREVIOUS_NEWS_HANDLER_ID;
    }

    if (fBrowser.isIE())
      js.append("var newScrollPosY = document.body.scrollTop; "); //$NON-NLS-1$
    else
      js.append("var newScrollPosY = window.pageYOffset; "); //$NON-NLS-1$

    js.append("if (scrollPosY == newScrollPosY) { "); //$NON-NLS-1$
    js.append("  window.location.href = \"").append(ILinkHandler.HANDLER_PROTOCOL + actionId).append("\"; "); //$NON-NLS-1$ //$NON-NLS-2$
    js.append("} "); //$NON-NLS-1$

    fBrowser.execute(js.toString());
  }

  /**
   * Shows the intial Input in the Browser.
   */
  public void home() {
    internalSetInput(fInput, true, false);
  }

  private Object[] getSortedChildren(Object parent) {
    Object[] result = getFilteredChildren(parent);
    if (fSorter != null) {

      /* be sure we're not modifying the original array from the model */
      result = result.clone();
      fSorter.sort(this, result);
    }
    return result;
  }

  private Object[] getFilteredChildren(Object parent) {
    Object[] result = getRawChildren(parent);

    /* Never filter a selected News, thereby return here */
    if (fInput instanceof INews)
      return result;

    /* Run Filters over result */
    if (fFilters != null) {
      for (Object filter : fFilters) {
        ViewerFilter f = (ViewerFilter) filter;
        result = f.filter(this, parent, result);
      }
    }
    return result;
  }

  private Object[] getRawChildren(Object parent) {
    Object[] result = null;
    if (parent != null) {
      IStructuredContentProvider cp = (IStructuredContentProvider) getContentProvider();
      if (cp != null)
        result = cp.getElements(parent);
    }
    return (result != null) ? result : new Object[0];
  }

  /**
   * @param input Can either be an Array of Feeds or News
   * @return An flattend array of Objects.
   */
  public Object[] getFlattendChildren(Object input) {

    /* Using NewsContentProvider */
    if (input != null && getContentProvider() instanceof NewsContentProvider) {
      NewsContentProvider cp = (NewsContentProvider) getContentProvider();

      /*
       * Flatten Children since Grouping is Enabled and the Parent is not
       * containing just News (so either Feed or ViewerGroups).
       */
      if (cp.isGroupingEnabled() && !isNews(input)) {
        List<Object> flatList = new ArrayList<Object>();

        /* Wrap into Object-Array */
        if (!(input instanceof Object[]))
          input = new Object[] { input };

        /* For each Group retrieve Children (sorted and filtered) */
        Object groups[] = (Object[]) input;
        for (Object group : groups) {

          /* Make sure this child has children */
          if (cp.hasChildren(group)) {
            Object sortedChilds[] = getSortedChildren(group);

            /* Only add if there are Childs */
            if (sortedChilds.length > 0) {
              flatList.add(group);
              if (group instanceof EntityGroup)
                ((EntityGroup) group).setSizeHint(sortedChilds.length);
              flatList.addAll(Arrays.asList(sortedChilds));
            }
          }

          /* Otherwise just add */
          else {
            flatList.add(group);
          }
        }

        return flatList.toArray();
      }

      /* Grouping is not enabled, just return sorted Children */
      return getSortedChildren(input);
    }

    /* Structured ContentProvider */
    else if (input != null && getContentProvider() instanceof IStructuredContentProvider)
      return getSortedChildren(input);

    /* No Element to show */
    return new Object[0];
  }

  /* Returns TRUE if the Input consists only of INews */
  private boolean isNews(Object input) {
    if (input instanceof Object[]) {
      Object elements[] = (Object[]) input;
      for (Object element : elements) {
        if (!(element instanceof INews))
          return false;
      }
    } else if (!(input instanceof INews))
      return false;

    return true;
  }

  /**
   * @param parentElement
   * @param childElements
   */
  public void add(Object parentElement, Object[] childElements) {
    Assert.isNotNull(parentElement);
    assertElementsNotNull(childElements);

    if (childElements.length > 0)
      refresh(); // TODO Optimize
  }

  /**
   * @param news
   */
  public void update(Set<NewsEvent> news) {

    /*
     * The update-event could have been sent out a lot faster than the Browser
     * having a chance to react. In this case, rather then refreshing a possible
     * blank page (or wrong page), re-set the input.
     */
    String inputUrl = fServer.toUrl(fId, fInput);
    String browserUrl = fBrowser.getControl().getUrl();
    boolean resetInput = browserUrl.length() == 0 || URIUtils.ABOUT_BLANK.equals(browserUrl);
    if (inputUrl.equals(browserUrl)) {
      if (!internalUpdate(news))
        refresh(); // Refresh if dynamic update failed
    } else if (fServer.isDisplayOperation(inputUrl) && resetInput)
      fBrowser.setUrl(inputUrl);
  }

  /**
   * @param objects
   */
  public void remove(Object[] objects) {
    assertElementsNotNull(objects);

    /* Refresh if dynamic removal failed */
    if (!internalRemove(objects))
      refresh();
  }

  /**
   * @param element
   */
  public void remove(Object element) {
    Assert.isNotNull(element);

    /* Refresh if dynamic removal failed */
    if (!internalRemove(new Object[] { element }))
      refresh();
  }

  private boolean internalUpdate(Set<NewsEvent> newsEvents) {
    boolean toggleJS = fBrowser.shouldDisableScript();
    try {
      if (toggleJS)
        fBrowser.setScriptDisabled(false);

      /* Update for each Event */
      for (NewsEvent newsEvent : newsEvents) {
        INews news = newsEvent.getEntity();

        StringBuilder js = new StringBuilder();

        /* State (Bold/Plain Title, Mark Read Tooltip) */
        if (CoreUtils.isStateChange(newsEvent)) {
          String markRead = Messages.NewsBrowserViewer_MARK_READ;
          String markUnread = Messages.NewsBrowserViewer_MARK_UNREAD;

          boolean isRead = (INews.State.READ == news.getState());
          js.append(getElementById(Dynamic.NEWS.getId(news)).append(isRead ? ".className='newsitemRead'; " : ".className='newsitemUnread'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.TITLE.getId(news)).append(isRead ? ".className='read'; " : ".className='unread'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.TOGGLE_READ_LINK.getId(news)).append(isRead ? ".title='" + markUnread + "'; " : ".title='" + markRead + "'; ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
          js.append(getElementById(Dynamic.TOGGLE_READ_IMG.getId(news)).append(isRead ? ".alt='" + markUnread + "'; " : ".alt='" + markRead + "'; ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }

        /* Sticky (Title Background, Footer Background, Mark Sticky Image) */
        if (CoreUtils.isStickyStateChange(newsEvent)) {
          boolean isSticky = news.isFlagged();
          js.append(getElementById(Dynamic.HEADER.getId(news)).append(isSticky ? ".className='headerSticky'; " : ".className='header'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.FOOTER.getId(news)).append(isSticky ? ".className='footerSticky'; " : ".className='footer'; ")); //$NON-NLS-1$ //$NON-NLS-2$

          String stickyImgUri;
          if (fBrowser.isIE())
            stickyImgUri = isSticky ? OwlUI.getImageUri("/icons/obj16/news_pinned_light.gif", "news_pinned_light.gif") : OwlUI.getImageUri("/icons/obj16/news_pin_light.gif", "news_pin_light.gif"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
          else
            stickyImgUri = isSticky ? ApplicationServer.getDefault().toResourceUrl("/icons/obj16/news_pinned_light.gif") : ApplicationServer.getDefault().toResourceUrl("/icons/obj16/news_pin_light.gif"); //$NON-NLS-1$ //$NON-NLS-2$

          js.append(getElementById(Dynamic.TOGGLE_STICKY_IMG.getId(news)).append(".src='").append(stickyImgUri).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /* Label (Title Foreground, Label List) */
        if (CoreUtils.isLabelChange(newsEvent)) {
          Set<ILabel> labels = CoreUtils.getSortedLabels(news);
          String defaultColor = CoreUtils.getLink(news) != null ? "#009" : "rgb(0,0,0)"; //$NON-NLS-1$ //$NON-NLS-2$
          String color = (labels.isEmpty()) ? defaultColor : "rgb(" + OwlUI.toString(OwlUI.getRGB(labels.iterator().next())) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
          if ("rgb(0,0,0)".equals(color)) //Don't let black override link color //$NON-NLS-1$
            color = defaultColor;
          js.append(getElementById(Dynamic.TITLE.getId(news)).append(".style.color='").append(color).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$

          if (labels.isEmpty()) {
            js.append(getElementById(Dynamic.LABELS_SEPARATOR.getId(news)).append(".style.display='none'; ")); //$NON-NLS-1$
            js.append(getElementById(Dynamic.LABELS.getId(news)).append(".innerHTML=''; ")); //$NON-NLS-1$
          } else {
            js.append(getElementById(Dynamic.LABELS_SEPARATOR.getId(news)).append(".style.display='inline'; ")); //$NON-NLS-1$

            StringBuilder labelsHtml = new StringBuilder(Messages.NewsBrowserViewer_LABELS);
            labelsHtml.append(" "); //$NON-NLS-1$
            int c = 0;
            for (ILabel label : labels) {
              c++;
              if (c < labels.size())
                span(labelsHtml, StringUtils.htmlEscape(label.getName()) + ", ", label.getColor()); //$NON-NLS-1$
              else
                span(labelsHtml, StringUtils.htmlEscape(label.getName()), label.getColor());
            }

            js.append(getElementById(Dynamic.LABELS.getId(news)).append(".innerHTML='").append(labelsHtml.toString()).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }

        if (js.length() > 0) {
          boolean res = fBrowser.getControl().execute(js.toString());
          if (!res)
            return false;
        }
      }
    } finally {
      if (toggleJS)
        fBrowser.setScriptDisabled(true);
    }

    return true;
  }

  private void span(StringBuilder builder, String content, String color) {
    builder.append("<span style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$
    builder.append(">").append(content).append("</span>"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private StringBuilder getElementById(String id) {
    return new StringBuilder("document.getElementById('" + id + "')"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private boolean internalRemove(Object[] elements) {
    boolean toggleJS = fBrowser.shouldDisableScript();
    try {
      if (toggleJS)
        fBrowser.setScriptDisabled(false);

      for (Object element : elements) {
        if (element instanceof INews) {
          INews news = (INews) element;
          StringBuilder js = new StringBuilder();
          js.append("var node = ").append(getElementById(Dynamic.NEWS.getId(news))).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
          js.append("if (node != null) { "); //$NON-NLS-1$
          js.append("node.className='hidden';"); //$NON-NLS-1$
          js.append(" } "); //$NON-NLS-1$

          boolean res = fBrowser.getControl().execute(js.toString());
          if (!res)
            return false;
        }
      }
    } finally {
      if (toggleJS)
        fBrowser.setScriptDisabled(true);
    }

    return true;
  }

  private void assertElementsNotNull(Object[] elements) {
    Assert.isNotNull(elements);
    for (Object element : elements) {
      Assert.isNotNull(element);
    }
  }

  /**
   * @return Returns a List of Strings that should get highlighted per News that
   * is displayed.
   */
  protected Collection<String> getHighlightedWords() {
    if (getContentProvider() instanceof NewsContentProvider && fPreferences.getBoolean(DefaultPreferences.FV_HIGHLIGHT_SEARCH_RESULTS)) {
      INewsMark mark = ((NewsContentProvider) getContentProvider()).getInput();
      Set<String> extractedWords;

      /* Extract from Conditions if any */
      if (mark instanceof ISearch) {
        List<ISearchCondition> conditions = ((ISearch) mark).getSearchConditions();
        extractedWords = CoreUtils.extractWords(conditions, true);
      } else
        extractedWords = new HashSet<String>(1);

      /* Fill Pattern if set */
      if (fNewsFilter != null && StringUtils.isSet(fNewsFilter.getPatternString())) {
        String pattern = fNewsFilter.getPatternString();

        /* News Filter always converts to wildcard query */
        if (!pattern.endsWith("*")) //$NON-NLS-1$
          pattern = pattern + "*"; //$NON-NLS-1$

        StringTokenizer tokenizer = new StringTokenizer(pattern);
        while (tokenizer.hasMoreElements())
          extractedWords.add(tokenizer.nextToken());
      }

      return extractedWords;
    }

    return Collections.emptyList();
  }
}