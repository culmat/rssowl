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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IAbortable;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.BookMarkAdapter;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.event.FeedAdapter;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.MarkEvent;
import org.rssowl.core.persist.event.NewsBinAdapter;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsBinListener;
import org.rssowl.core.persist.event.SearchConditionEvent;
import org.rssowl.core.persist.event.SearchConditionListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.ITreeNode;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TreeTraversal;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.Controller.BookMarkLoadListener;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.OwlUI.Layout;
import org.rssowl.ui.internal.actions.DeleteTypesAction;
import org.rssowl.ui.internal.actions.FindAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.RetargetActions;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.CBrowser;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;
import org.rssowl.ui.internal.util.WidgetTreeNode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The FeedView is an instance of <code>EditorPart</code> capable of displaying
 * News in a Table-Viewer and Browser-Viewer. It offers controls to Filter and
 * Group them.
 *
 * @author bpasero
 */
public class FeedView extends EditorPart implements IReusableEditor {

  /* Delay in ms to Mark *new* News to *unread* on Part-Deactivation */
  private static final int HANDLE_NEWS_SEEN_DELAY = 100;

  /* Millies before news seen are handled */
  private static final int HANDLE_NEWS_SEEN_BLOCK_DELAY = 800;

  /* Millies before the next clean up is allowed to run again */
  private static final int CLEAN_UP_BLOCK_DELAY = 1000;

  /* The last visible Feedview */
  private static FeedView fgLastVisibleFeedView = null;

  /* Flag to indicate if feed change events should be blocked or not */
  private static boolean fgBlockFeedChangeEvent;

  /** ID of this EditorPart */
  public static final String ID = "org.rssowl.ui.FeedView"; //$NON-NLS-1$

  /** List of UI-Events interesting for the FeedView */
  public enum UIEvent {

    /** Other Feed Displayed */
    FEED_CHANGE,

    /** Application Minimized */
    MINIMIZE,

    /** Application Closing */
    CLOSE,

    /** Tab Closed */
    TAB_CLOSE
  }

  /* Editor Data */
  private FeedViewInput fInput;
  private IEditorSite fEditorSite;
  private IFeedViewSite fFeedViewSite;

  /* Part to display News in Table */
  private NewsTableControl fNewsTableControl;

  /* Part to display News in Browser */
  private NewsBrowserControl fNewsBrowserControl;

  /* Bars */
  private FilterBar fFilterBar;
  private BrowserBar fBrowserBar;

  /* Shared Viewer classes */
  private NewsFilter fNewsFilter;
  private NewsGrouping fNewsGrouping;
  private NewsContentProvider fContentProvider;

  /* Container for the News Table Viewer */
  private Composite fNewsTableControlContainer;

  /* Container for the Browser Viewer */
  private Composite fBrowserViewerControlContainer;

  /* Listeners */
  private IPartListener2 fPartListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkAdapter fSearchMarkListener;
  private FeedAdapter fFeedListener;
  private SearchConditionListener fSearchConditionListener;
  private NewsBinListener fNewsBinListener;
  private FolderAdapter fFolderListener;
  private BookMarkLoadListener fBookMarkLoadListener;

  /* Settings */
  NewsFilter.Type fInitialFilterType;
  NewsGrouping.Type fInitialGroupType;
  NewsFilter.SearchTarget fInitialSearchTarget;
  Layout fInitialLayout;
  private int fInitialWeights[];
  private int fCacheWeights[];

  /* Global Actions */
  private IAction fReloadAction;
  private IAction fSelectAllAction;
  private IAction fDeleteAction;
  private IAction fCutAction;
  private IAction fCopyAction;
  private IAction fPasteAction;
  private IAction fPrintAction;
  private IAction fUndoAction;
  private IAction fRedoAction;
  private IAction fFindAction;

  /* Misc. */
  private Composite fParent;
  private Composite fRootComposite;
  private SashForm fSashForm;
  private Label fHorizontalTableBrowserSep;
  private Label fVerticalTableBrowserSep;
  private LocalResourceManager fResourceManager;
  private IPreferenceScope fPreferences;
  private long fOpenTime;
  private boolean fCreated;
  private final Object fCacheJobIdentifier = new Object();
  private ImageDescriptor fTitleImageDescriptor;
  private Label fHorizontalFilterTableSep;
  private Label fHorizontalBrowserSep;
  private Label fVerticalBrowserSep;
  private final INewsDAO fNewsDao = Owl.getPersistenceService().getDAOService().getNewsDAO();
  private boolean fIsDisposed;
  private AtomicLong fLastCleanUpRun = new AtomicLong();

  /*
   * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
    /* Not Supported */
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
    if (fIsDisposed || Controller.getDefault().isShuttingDown())
      return;

    /* Ask user for File */
    FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
    dialog.setOverwrite(true);
    if (fInput.getMark() instanceof IBookMark)
      dialog.setFilterExtensions(new String[] { ".html", ".xml" }); //$NON-NLS-1$ //$NON-NLS-2$
    else
      dialog.setFilterExtensions(new String[] { ".html" }); //$NON-NLS-1$

    String proposedName = Application.IS_WINDOWS ? CoreUtils.getSafeFileNameForWindows(fInput.getName()) : fInput.getName();
    proposedName += ".html"; //$NON-NLS-1$
    dialog.setFileName(proposedName);

    String fileName = dialog.open();
    if (fileName == null)
      return;

    if (fileName.endsWith(".xml")) //$NON-NLS-1$
      saveAsXml(fileName);
    else
      saveAsHtml(fileName);
  }

  @SuppressWarnings("restriction")
  private void saveAsXml(final String fileName) {
    final IBookMark bm = (IBookMark) fInput.getMark();
    final URI feedLink = bm.getFeedLinkReference().getLink();
    try {
      final IProtocolHandler handler = Owl.getConnectionService().getHandler(feedLink);
      if (handler instanceof org.rssowl.core.internal.connection.DefaultProtocolHandler) {
        Job downloadJob = new Job(Messages.FeedView_DOWNLOADING_FEED) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(bm.getName(), IProgressMonitor.UNKNOWN);

            InputStream in = null;
            FileOutputStream out = null;
            boolean canceled = false;
            Exception error = null;
            try {
              byte[] buffer = new byte[8192];

              in = handler.openStream(feedLink, monitor, null);
              out = new FileOutputStream(fileName);
              while (true) {

                /* Check for Cancellation and Shutdown */
                if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
                  canceled = true;
                  return Status.CANCEL_STATUS;
                }

                /* Read from Stream */
                int read = in.read(buffer);
                if (read == -1)
                  break;

                out.write(buffer, 0, read);
              }
            } catch (FileNotFoundException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } catch (IOException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } catch (ConnectionException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } finally {
              monitor.done();

              if (out != null) {
                try {
                  out.close();
                } catch (IOException e) {
                  Activator.safeLogError(e.getMessage(), e);
                }
              }

              if (in != null) {
                try {
                  if ((canceled || error != null) && in instanceof IAbortable)
                    ((IAbortable) in).abort();
                  else
                    in.close();
                } catch (IOException e) {
                  Activator.safeLogError(e.getMessage(), e);
                }
              }
            }

            return Status.OK_STATUS;
          }
        };
        downloadJob.schedule();
      }
    } catch (ConnectionException e) {
      Activator.safeLogError(e.getMessage(), e);
    }
  }

  /* Build Content as String from Feed */
  private void saveAsHtml(String fileName) {
    StringBuilder content = new StringBuilder();
    NewsBrowserLabelProvider labelProvider = (NewsBrowserLabelProvider) fNewsBrowserControl.getViewer().getLabelProvider();

    /* Save from Table */
    if (isTableViewerVisible()) {
      Tree tree = fNewsTableControl.getViewer().getTree();
      TreeItem[] items = tree.getItems();
      if (items.length > 0) {
        List<INews> newsToSave = new ArrayList<INews>();

        /* Ungrouped */
        if (items[0].getItemCount() == 0) {
          for (TreeItem item : items) {
            if (item.getData() instanceof INews)
              newsToSave.add((INews) item.getData());
          }
        }

        /* Grouped */
        else {
          for (TreeItem parentItem : items) {
            TreeItem[] childItems = parentItem.getItems();
            for (TreeItem item : childItems) {
              if (item.getData() instanceof INews)
                newsToSave.add((INews) item.getData());
            }
          }
        }

        /* Create Content for each Item */
        content.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"); //$NON-NLS-1$
        content.append("<html>\n  <head>\n"); //$NON-NLS-1$
        content.append("\n  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"); //$NON-NLS-1$
        StringWriter css = new StringWriter();
        try {
          labelProvider.writeCSS(css, newsToSave.size() == 1, false);
        } catch (IOException e) {
          /* Ignore */
        }
        content.append(css.toString());
        content.append("  </head>\n  <body>\n"); //$NON-NLS-1$
        for (int i = 0; i < newsToSave.size(); i++) {
          String text = labelProvider.getText(newsToSave.get(i), false, false, i);
          content.append(text);
        }
        content.append("\n  </body>\n</html>"); //$NON-NLS-1$
      }
    }

    /* Save from Browser */
    else {
      content.append(fNewsBrowserControl.getViewer().getBrowser().getControl().getText());
    }

    if (content.length() == 0)
      return;

    /* Write into File */
    CoreUtils.write(fileName, content);
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);

    fEditorSite = site;
    fFeedViewSite = new FeedViewSite(this, site);
    setSite(site);
    fResourceManager = new LocalResourceManager(JFaceResources.getResources());

    /* Load Settings */
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    loadSettings((FeedViewInput) input);

    /* Apply Input */
    setInput(input);

    /* Hook into Global Actions */
    createGlobalActions();
    setGlobalActions();

    /* Register Listeners */
    registerListeners();
  }

  private boolean justOpened() {
    return System.currentTimeMillis() - fOpenTime < HANDLE_NEWS_SEEN_BLOCK_DELAY;
  }

  private void registerListeners() {
    fPartListener = new IPartListener2() {

      /* Mark *new* News as *unread* or *read* */
      public void partHidden(IWorkbenchPartReference partRef) {

        /* Return early if event is too close after opening the feed */
        if (justOpened())
          return;

        /* Remember this feedview as being the last visible one */
        if (FeedView.this.equals(partRef.getPart(false)))
          fgLastVisibleFeedView = FeedView.this;
      }

      /* Hook into Global Actions for this Editor */
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false))) {
          setGlobalActions();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);

          /* Notify last visible feedview about change */
          if (fgLastVisibleFeedView != null && fgLastVisibleFeedView != FeedView.this && !fgLastVisibleFeedView.fIsDisposed) {
            fgLastVisibleFeedView.notifyUIEvent(UIEvent.FEED_CHANGE);
            fgLastVisibleFeedView = null;
          }
        }

        /* Any other editor was brought to top, reset last visible feedview */
        else if (!ID.equals(partRef.getId()))
          fgLastVisibleFeedView = null;
      }

      public void partClosed(IWorkbenchPartReference partRef) {
        IEditorReference[] editors = partRef.getPage().getEditorReferences();
        boolean equalsThis = FeedView.this.equals(partRef.getPart(false));
        if (editors.length == 0 && equalsThis)
          OwlUI.updateWindowTitle((String) null);

        if (equalsThis) {
          if (fgLastVisibleFeedView == FeedView.this) //Avoids duplicate UI Event handling
            fgLastVisibleFeedView = null;
          notifyUIEvent(UIEvent.TAB_CLOSE);
        }
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {}

      public void partActivated(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      public void partOpened(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)) && isVisible()) {
          fOpenTime = System.currentTimeMillis();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
        }
      }

      public void partVisible(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }
    };

    fEditorSite.getPage().addPartListener(fPartListener);

    /* React on Bookmark Events */
    fBookMarkListener = new BookMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<BookMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<BookMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);

    /* React on Folder Events */
    fFolderListener = new FolderAdapter() {
      @Override
      public void entitiesDeleted(Set<FolderEvent> events) {
        onFoldersDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<FolderEvent> events) {
        onNewsFoldersUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);

    /* React on Searchmark Events */
    fSearchMarkListener = new SearchMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<SearchMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<SearchMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);

    /* Refresh on Condition Changes if SearchMark showing */
    fSearchConditionListener = new SearchConditionListener() {
      public void entitiesAdded(Set<SearchConditionEvent> events) {
        refreshIfRequired(events);
      }

      public void entitiesDeleted(Set<SearchConditionEvent> events) {
        /* Ignore Due to Bug 1140 (http://dev.rssowl.org/show_bug.cgi?id=1140) */
      }

      public void entitiesUpdated(Set<SearchConditionEvent> events) {
        /* Ignore Due to Bug 1140 (http://dev.rssowl.org/show_bug.cgi?id=1140) */
      }

      /* We rely on the implementation detail that updating a SM means deleting/adding conditions */
      private void refreshIfRequired(Set<SearchConditionEvent> events) {
        if (fInput.getMark() instanceof ISearchMark) {
          ISearchMarkDAO dao = DynamicDAO.getDAO(ISearchMarkDAO.class);
          for (SearchConditionEvent event : events) {
            ISearchCondition condition = event.getEntity();
            ISearchMark searchMark = dao.load(condition);
            if (searchMark != null && searchMark.equals(fInput.getMark())) {
              JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
                @Override
                protected void runInBackground(IProgressMonitor monitor) {
                  if (!Controller.getDefault().isShuttingDown())
                    fContentProvider.refreshCache(fInput.getMark(), false);
                }

                @Override
                protected void runInUI(IProgressMonitor monitor) {
                  if (!Controller.getDefault().isShuttingDown())
                    refresh(true, true);
                }

                @Override
                public boolean belongsTo(Object family) {
                  return fCacheJobIdentifier.equals(family);
                }
              });

              break;
            }
          }
        }
      }
    };
    DynamicDAO.addEntityListener(ISearchCondition.class, fSearchConditionListener);

    /* React on Newsbin Events */
    fNewsBinListener = new NewsBinAdapter() {
      @Override
      public void entitiesDeleted(Set<NewsBinEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<NewsBinEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(INewsBin.class, fNewsBinListener);

    /* Listen if Title Image is changing */
    fFeedListener = new FeedAdapter() {
      @Override
      public void entitiesUpdated(Set<FeedEvent> events) {

        /* Only supported for BookMarks */
        if (!(fInput.getMark() instanceof IBookMark) || events.size() == 0)
          return;

        /* Check if Feed-Event affecting us */
        for (FeedEvent event : events) {
          FeedLinkReference feedRef = ((IBookMark) fInput.getMark()).getFeedLinkReference();
          if (feedRef.references(event.getEntity())) {
            ImageDescriptor imageDesc = fInput.getImageDescriptor();

            /* Title Image Change - Update! */
            if (!fTitleImageDescriptor.equals(imageDesc)) {
              fTitleImageDescriptor = imageDesc;

              JobRunner.runInUIThread(fParent, new Runnable() {
                public void run() {
                  setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
                }
              });
            }

            break;
          }
        }
      }
    };
    DynamicDAO.addEntityListener(IFeed.class, fFeedListener);

    /* Show Busy when Input is loaded */
    fBookMarkLoadListener = new Controller.BookMarkLoadListener() {
      public void bookMarkAboutToLoad(IBookMark bookmark) {
        if (!fIsDisposed && bookmark.equals(fInput.getMark()))
          showBusyLoading(true);
      }

      public void bookMarkDoneLoading(IBookMark bookmark) {
        if (!fIsDisposed && bookmark.equals(fInput.getMark()))
          showBusyLoading(false);
      }
    };
    Controller.getDefault().addBookMarkLoadListener(fBookMarkLoadListener);
  }

  private void showBusyLoading(final boolean busy) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      @SuppressWarnings("restriction")
      public void run() {
        if (!fIsDisposed && getSite() instanceof org.eclipse.ui.internal.PartSite)
          ((org.eclipse.ui.internal.PartSite) getSite()).getPane().setBusy(busy);
      }
    });
  }

  private void onNewsFoldersUpdated(final Set<FolderEvent> events) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      public void run() {
        if (!(fInput.getMark() instanceof FolderNewsMark))
          return;

        final IEditorPart activeFeedView = fEditorSite.getPage().getActiveEditor();
        FolderNewsMark folderNewsMark = (FolderNewsMark) (fInput.getMark());
        for (FolderEvent event : events) {
          final IFolder folder = event.getEntity();
          if (folder.equals(folderNewsMark.getFolder())) {
            setPartName(folder.getName());
            if (activeFeedView == FeedView.this)
              OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });

            break;
          }
        }
      }
    });
  }

  private void onFoldersDeleted(Set<FolderEvent> events) {
    if (!(fInput.getMark() instanceof FolderNewsMark))
      return;

    FolderNewsMark folderNewsMark = (FolderNewsMark) (fInput.getMark());
    for (FolderEvent event : events) {
      final IFolder folder = event.getEntity();
      if (folder.equals(folderNewsMark.getFolder())) {
        fInput.setDeleted();
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            fEditorSite.getPage().closeEditor(FeedView.this, false);
          }
        });
        break;
      }
    }
  }

  private void onNewsMarksUpdated(final Set<? extends MarkEvent> events) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      public void run() {
        final IEditorPart activeFeedView = fEditorSite.getPage().getActiveEditor();
        for (MarkEvent event : events) {
          final IMark mark = event.getEntity();
          if (mark.getId().equals(fInput.getMark().getId())) {
            setPartName(mark.getName());
            if (activeFeedView == FeedView.this)
              OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });

            break;
          }
        }
      }
    });
  }

  private void onNewsMarksDeleted(Set<? extends MarkEvent> events) {
    for (MarkEvent event : events) {
      IMark mark = event.getEntity();
      if (fInput.getMark().getId().equals(mark.getId())) {
        fInput.setDeleted();
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            fEditorSite.getPage().closeEditor(FeedView.this, false);
          }
        });
        break;
      }
    }
  }

  private void loadSettings(FeedViewInput input) {

    /* Filter Settings */
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(input.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fInitialFilterType = NewsFilter.Type.values()[iVal];
    else
      fInitialFilterType = NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)];

    /* Group Settings */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fInitialGroupType = NewsGrouping.Type.values()[iVal];
    else
      fInitialGroupType = NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)];

    /* Other Settings */
    fInitialLayout = OwlUI.getLayout(preferences);
    fInitialWeights = fPreferences.getIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS);
    fInitialSearchTarget = NewsFilter.SearchTarget.values()[fPreferences.getInteger(DefaultPreferences.FV_SEARCH_TARGET)];
  }

  private void saveSettings() {

    /* Update Settings in DB */
    if (fCacheWeights != null && fCacheWeights[0] != fCacheWeights[1]) {
      int weightDiff = (fInitialWeights[0] - fCacheWeights[0]);
      if (Math.abs(weightDiff) > 5) {
        int strWeights[] = new int[] { fCacheWeights[0], fCacheWeights[1] };
        fPreferences.putIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS, strWeights);
      }
    }
  }

  private void createGlobalActions() {

    /* Hook into Reload */
    fReloadAction = new Action() {
      @Override
      public void run() {
        new ReloadTypesAction(new StructuredSelection(fInput.getMark()), getEditorSite().getShell()).run();
      }
    };

    /* Select All */
    fSelectAllAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Select All in Text Widget */
        if (focusControl instanceof Text) {
          ((Text) focusControl).selectAll();
        }

        /* Select All in Viewer Tree */
        else {
          ((Tree) fNewsTableControl.getViewer().getControl()).selectAll();
          fNewsTableControl.getViewer().setSelection(fNewsTableControl.getViewer().getSelection());
        }
      }
    };

    /* Delete */
    fDeleteAction = new Action() {
      @Override
      public void run() {
        new DeleteTypesAction(fParent.getShell(), (IStructuredSelection) fNewsTableControl.getViewer().getSelection()).run();
      }
    };

    /* Cut */
    fCutAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Cut in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).cut();
      }
    };

    /* Copy */
    fCopyAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Copy in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).copy();
      }
    };

    /* Paste */
    fPasteAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Paste in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).paste();
      }
    };

    /* Print */
    fPrintAction = new Action() {
      @Override
      public void run() {
        print();
      }
    };

    /* Undo (Eclipse Integration) */
    fUndoAction = new Action() {
      @Override
      public void run() {
        UndoStack.getInstance().undo();
      }
    };

    /* Redo (Eclipse Integration) */
    fRedoAction = new Action() {
      @Override
      public void run() {
        UndoStack.getInstance().redo();
      }
    };

    /* Find (Eclipse Integration) */
    fFindAction = new FindAction();
  }

  /**
   * Print the contents of the Browser if any.
   */
  public void print() {

    /* Return early if the browser is not visible at all */
    if (!isBrowserViewerVisible()) {
      MessageDialog.openInformation(fRootComposite.getShell(), Messages.FeedView_PRINT_NEWS, Messages.FeedView_PRINT_NEWS_HEADLINES_LAYOUT);
      return;
    }

    /* Pass request to browser */
    if (fNewsBrowserControl != null)
      fNewsBrowserControl.getViewer().getBrowser().print();
  }

  /**
   * The user performed the "Find" action.
   */
  public void find() {
    if (fFilterBar != null) {

      /* Make Feed Toolbar Visible if not visible yet */
      if (!fFilterBar.isVisible()) {
        fPreferences.putBoolean(DefaultPreferences.FV_FEED_TOOLBAR_HIDDEN, false);
        EditorUtils.updateToolbarVisibility();
      }

      fFilterBar.focusQuickSearch();
    }
  }

  private void setGlobalActions() {

    /* Define Retargetable Global Actions */
    fEditorSite.getActionBars().setGlobalActionHandler(RetargetActions.RELOAD, fReloadAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), fPrintAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), fUndoAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), fRedoAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), fFindAction);

    /* Disable some Edit-Actions at first */
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
  }

  /**
   * Sets the given <code>IStructuredSelection</code> to the News-Table showing
   * in the FeedView. Will ignore the selection, if the Table is minimized.
   *
   * @param selection The Selection to show in the News-Table.
   */
  public void setSelection(final IStructuredSelection selection) {

    /* Remove Filter if selection is hidden */
    if (fNewsFilter.getType() != NewsFilter.Type.SHOW_ALL) {
      boolean unfilter = false;
      List<?> elements = selection.toList();
      for (Object element : elements) {

        /* Resolve the actual News */
        if (element instanceof NewsReference)
          element = ((NewsReference) element).resolve();

        /* This Element is filtered */
        if (!fNewsFilter.select(fNewsTableControl.getViewer(), null, element)) {
          unfilter = true;
          break;
        }
      }

      /* Remove Filter if selection is hidden */
      if (unfilter) {
        fNewsBrowserControl.getViewer().setBlockRefresh(true);
        try {
          fFilterBar.doFilter(NewsFilter.Type.SHOW_ALL, true, false);
        } finally {
          fNewsBrowserControl.getViewer().setBlockRefresh(false);
        }
      }
    }

    /* Scroll News into View from Browser if maximized */
    if (!isTableViewerVisible()) {
      JobRunner.runInUIThread(500, fNewsBrowserControl.getViewer().getControl(), new Runnable() {
        public void run() { //Run delayed as the browser might still be busy loading the input
          fNewsBrowserControl.getViewer().showSelection(selection);
        }
      });
    }

    /* Apply selection to Table */
    else
      fNewsTableControl.getViewer().setSelection(selection, true);
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
   */
  @Override
  public void setInput(IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);

    /* Handle Old being hidden now */
    if (fInput != null) {
      notifyUIEvent(UIEvent.FEED_CHANGE);
      rememberSelection(fInput.getMark(), fNewsTableControl.getLastSelection());
    }

    /* Set New */
    super.setInput(input);
    fInput = (FeedViewInput) input;

    /* Update UI of Feed-View if new Editor */
    if (!fCreated)
      updateTab(fInput);

    /* Clear Filter Bar */
    if (fFilterBar != null)
      fFilterBar.clearQuickSearch(false);

    /* Editor is being reused */
    if (fCreated) {
      firePropertyChange(PROP_INPUT);

      /* Load Filter Settings for this Mark if present */
      updateFilterAndGrouping(false);

      /* Cancel any previous running Cache-Job */
      Job.getJobManager().cancel(fCacheJobIdentifier);

      /* Re-Create the ContentProvider to avoid concurrency problems with Cache */
      fContentProvider = new NewsContentProvider(fNewsTableControl.getViewer(), fNewsBrowserControl.getViewer(), this);
      fNewsTableControl.getViewer().setContentProvider(fContentProvider);
      fNewsTableControl.onInputChanged(fInput);
      fNewsBrowserControl.getViewer().setContentProvider(fContentProvider);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Reset the Quicksearch if active */
      if (fNewsFilter.isPatternSet())
        fNewsFilter.setPattern(""); //$NON-NLS-1$

      /* Update news mark in filter */
      fNewsFilter.setNewsMark(fInput.getMark());

      /* Apply Input */
      setInput(fInput.getMark(), true);
    }
  }

  /* Update Title and Image of the FeedView's Tab */
  private void updateTab(FeedViewInput input) {
    setPartName(input.getName());
    fTitleImageDescriptor = input.getImageDescriptor();
    setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
  }

  /**
   * Load Filter Settings for the Mark that is set as input if present
   * <p>
   * TODO Find a better solution once its possible to add listeners to
   * {@link IPreferenceScope} and then listen to changes of display-properties.
   * </p>
   *
   * @param refresh If TRUE, refresh the Viewer, FALSE otherwise.
   */
  public void updateFilterAndGrouping(boolean refresh) {
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(fInput.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fFilterBar.doFilter(NewsFilter.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doFilter(NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)], refresh, false);

    /* Load Group Settings for this Mark if present */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fFilterBar.doGrouping(NewsGrouping.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doGrouping(NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)], refresh, false);
  }

  /**
   * Refresh the visible columns of the opened news table control.
   */
  public void updateColumns() {
    if (fInput == null)
      return;

    if (isTableViewerVisible())
      fNewsTableControl.updateColumns(fInput.getMark());

    if (isBrowserViewerVisible())
      fNewsBrowserControl.updateSorting(fInput.getMark(), true);
  }

  /**
   * Notifies this editor about a UI-Event just occured. In dependance of the
   * event, the Editor might want to update the state on the displayed News.
   *
   * @param event The UI-Event that just occured as described in the
   * <code>UIEvent</code> enumeration.
   */
  public void notifyUIEvent(final UIEvent event) {
    final IMark inputMark = fInput.getMark();
    final IStructuredSelection lastSelection = fNewsTableControl.getLastSelection();

    /* Avoid any work in case RSSOwl is shutting down in an emergency */
    if (Controller.getDefault().isEmergencyShutdown())
      return;

    /* Specially Treat Restart Situation */
    if (Controller.getDefault().isRestarting()) {
      if (event == UIEvent.TAB_CLOSE && fInput.exists())
        rememberSelection(inputMark, lastSelection);

      return; // Ignore other events during restart
    }

    /* Specially Treat Closing Situation */
    else if (Controller.getDefault().isShuttingDown()) {
      if (event == UIEvent.TAB_CLOSE && fInput.exists())
        rememberSelection(inputMark, lastSelection);

      if (event != UIEvent.CLOSE)
        return; // Ignore other events than CLOSE that might get issued
    }

    /* Operate on a Copy of the Content Providers News */
    final Collection<INews> news = fContentProvider.getCachedNewsCopy();
    IPreferenceScope inputPreferences = Owl.getPreferenceService().getEntityScope(inputMark);

    /*
     * News can be NULL at this moment, if the Job that is to refresh the cache
     * in the Content Provider was never scheduled. This can happen when quickly
     * navigating between feeds. Also, the input could have been deleted and the
     * editor closed. Thereby do not react.
     */
    if (news == null || !fInput.exists())
      return;

    final boolean markReadOnFeedChange = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_CHANGE);
    final boolean markReadOnTabClose = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE);
    final boolean markReadOnMinimize = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE);

    /* Mark *new* News as *unread* when closing the entire application */
    if (event == UIEvent.CLOSE) {

      /* Perform the State Change */
      List<INews> newsToUpdate = new ArrayList<INews>();
      for (INews newsItem : news) {
        if (newsItem.getState() == INews.State.NEW)
          newsToUpdate.add(newsItem);
      }

      /* Perform Operation */
      fNewsDao.setState(newsToUpdate, INews.State.UNREAD, OwlUI.markReadDuplicates(), false);
    }

    /* Handle seen News: Feed Change (also closing the feed view), Closing or Minimize Event */
    else if (event == UIEvent.FEED_CHANGE || event == UIEvent.MINIMIZE || event == UIEvent.TAB_CLOSE) {

      /* Return early if this is a feed change which should be ignored */
      if (event == UIEvent.FEED_CHANGE && fgBlockFeedChangeEvent)
        return;

      /*
       * TODO This is a workaround to avoid potential race-conditions when closing a Tab. The problem
       * is that both FEED_CHANGE (due to hiding the tab) and TAB_CLOSE (due to actually closing
       * the tab) get sent when the user closes a tab. The workaround is to delay the processing of
       * TAB_CLOSE a bit to minimize the chance of a race condition.
       */
      int delay = HANDLE_NEWS_SEEN_DELAY;
      if (event == UIEvent.TAB_CLOSE)
        delay += 100;

      JobRunner.runInBackgroundThread(delay, new Runnable() {
        public void run() {

          /* Application might be in process of closing */
          if (Controller.getDefault().isShuttingDown())
            return;

          /* Check settings if mark as read should be performed */
          boolean markRead = false;
          switch (event) {
            case FEED_CHANGE:
              markRead = markReadOnFeedChange;
              break;

            case TAB_CLOSE:
              markRead = markReadOnTabClose;
              break;

            case MINIMIZE:
              markRead = markReadOnMinimize;
              break;
          }

          /* Perform the State Change */
          List<INews> newsToUpdate = new ArrayList<INews>();
          for (INews newsItem : news) {
            if (newsItem.getState() == INews.State.NEW)
              newsToUpdate.add(newsItem);
            else if (markRead && (newsItem.getState() == INews.State.UPDATED || newsItem.getState() == INews.State.UNREAD))
              newsToUpdate.add(newsItem);
          }

          if (!newsToUpdate.isEmpty()) {

            /* Force quick update on Feed-Change or Tab Close */
            if ((event == UIEvent.FEED_CHANGE || event == UIEvent.TAB_CLOSE))
              Controller.getDefault().getSavedSearchService().forceQuickUpdate();

            /* Support Undo */
            UndoStack.getInstance().addOperation(new NewsStateOperation(newsToUpdate, markRead ? INews.State.READ : INews.State.UNREAD, OwlUI.markReadDuplicates()));

            /* Perform Operation */
            fNewsDao.setState(newsToUpdate, markRead ? INews.State.READ : INews.State.UNREAD, OwlUI.markReadDuplicates(), false);
          }

          /* Retention Strategy */
          if (inputMark instanceof IBookMark)
            performCleanUp((IBookMark) inputMark, news);

          /* Also remember the last selected News */
          if (event == UIEvent.TAB_CLOSE)
            rememberSelection(inputMark, lastSelection);
        }
      });
    }
  }

  private void performCleanUp(IBookMark bookmark, Collection<INews> news) {
    if (System.currentTimeMillis() - fLastCleanUpRun.get() > CLEAN_UP_BLOCK_DELAY) {
      RetentionStrategy.process(bookmark, news);
      fLastCleanUpRun.set(System.currentTimeMillis());
    }
  }

  /* React on the Input being set */
  private void onInputSet() {

    /* Check if an action is to be performed */
    PerformAfterInputSet perform = fInput.getPerformOnInputSet();
    perform(perform);

    /* DB Roundtrips done in the background */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        if (fInput == null)
          return;

        IMark mark = fInput.getMark();

        /* Trigger a reload if this is the first time open or previously erroneous open */
        if (mark instanceof IBookMark) {
          IBookMark bookmark = (IBookMark) mark;
          if ((bookmark.getLastVisitDate() == null || bookmark.isErrorLoading()) && !fContentProvider.hasCachedNews())
            new ReloadTypesAction(new StructuredSelection(mark), getEditorSite().getShell()).run();
        }

        /* Trigger reload of not loaded included Bookmarks */
        else if (mark instanceof FolderNewsMark) {
          IFolder folder = ((FolderNewsMark) mark).getFolder();
          List<IBookMark> bookMarksToReload = new ArrayList<IBookMark>();
          fillBookMarksToReload(bookMarksToReload, folder);
          if (!bookMarksToReload.isEmpty())
            new ReloadTypesAction(new StructuredSelection(bookMarksToReload.toArray()), getEditorSite().getShell()).run();
        }

        /* Mark the Searchmark as visited */
        if (mark instanceof ISearchMark) {
          DynamicDAO.getDAO(ISearchMarkDAO.class).visited((ISearchMark) mark);
        }

        /* Mark the newsbin as visited */
        else if (mark instanceof INewsBin) {
          DynamicDAO.getDAO(INewsBinDAO.class).visited((INewsBin) mark);
        }

        /* Mark the Bookmark as visited */
        else if (!(mark instanceof FolderNewsMark)) {
          DynamicDAO.getDAO(IBookMarkDAO.class).visited((IBookMark) mark);
        }
      }
    });
  }

  /**
   * @param perform the action to perform on this editor.
   */
  public void perform(PerformAfterInputSet perform) {
    if (perform != null) {

      /* Select first News */
      if (perform.getType() == PerformAfterInputSet.Kind.SELECT_FIRST_NEWS)
        navigate(false, false, true, false);

      /* Select first unread News */
      else if (perform.getType() == PerformAfterInputSet.Kind.SELECT_UNREAD_NEWS)
        navigate(false, true, true, true);

      /* Select specific News */
      else if (perform.getType() == PerformAfterInputSet.Kind.SELECT_SPECIFIC_NEWS)
        setSelection(new StructuredSelection(perform.getNewsToSelect()));

      /* Make sure to activate this FeedView in case of an action */
      if (perform.shouldActivate())
        fEditorSite.getPage().activate(fEditorSite.getPart());
    }
  }

  private void fillBookMarksToReload(List<IBookMark> bookMarksToReload, IFolder folder) {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        if ((((IBookMark) mark).getMostRecentNewsDate() == null))
          bookMarksToReload.add((IBookMark) mark);
      }
    }

    List<IFolder> childs = folder.getFolders();
    for (IFolder child : childs) {
      fillBookMarksToReload(bookMarksToReload, child);
    }
  }

  /* Set Input to Viewers */
  private void setInput(final INewsMark mark, final boolean reused) {

    /* Update Cache in Background and then apply to UI */
    JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
      private IProgressMonitor fBgMonitor;

      @Override
      public boolean belongsTo(Object family) {
        return fCacheJobIdentifier.equals(family);
      }

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        fBgMonitor = monitor;
        if (!monitor.isCanceled())
          fContentProvider.refreshCache(mark, false);
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {
        IStructuredSelection oldSelection = null;
        IPreferenceScope entityPreferences = Owl.getPreferenceService().getEntityScope(mark);

        long value = entityPreferences.getLong(DefaultPreferences.NM_SELECTED_NEWS);
        if (value > 0) {
          boolean isHeadlinesLayout = (OwlUI.getLayout(entityPreferences) == Layout.HEADLINES);
          boolean openEmptyNews = entityPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS);
          boolean openAllNews = entityPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS);
          boolean useTransformer = entityPreferences.getBoolean(DefaultPreferences.BM_USE_TRANSFORMER);
          boolean useExternalBrowser = OwlUI.useExternalBrowser();

          /* Only re-select if this has not the potential of opening in external Browser */
          if (!useExternalBrowser || isHeadlinesLayout || useTransformer || (!openAllNews && !openEmptyNews))
            oldSelection = new StructuredSelection(new NewsReference(value));
        }

        /* Update Layout */
        if (reused)
          updateLayout(false);

        /* Set input to News-Table if Visible */
        if (!fBgMonitor.isCanceled() && isTableViewerVisible())
          stableSetInputToNewsTable(mark, oldSelection);

        /* Clear old Input from Table */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsTableControl.setPartInput(null);

        /* Set input to News-Browser if visible */
        if (!fBgMonitor.isCanceled() && !isTableViewerVisible())
          fNewsBrowserControl.setPartInput(mark);

        /* Reset old Input to Browser if available */
        else if (!fBgMonitor.isCanceled() && oldSelection != null && isBrowserViewerVisible()) {
          ISelection selection = fNewsTableControl.getViewer().getSelection();
          if (!selection.isEmpty()) //Could be filtered
            fNewsBrowserControl.setPartInput(oldSelection.getFirstElement());
        }

        /* Clear old Input from Browser */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsBrowserControl.setPartInput(null);

        /* Update Tab now */
        if (reused)
          updateTab(fInput);

        /* Handle Input being set now */
        onInputSet();
      }
    });
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isDirty()
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return true;
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {

    /* Focus Headlines */
    if (isTableViewerVisible())
      fNewsTableControl.setFocus();

    /* Focus Browser */
    else {
      Runnable runnable = new Runnable() {
        public void run() {
          fNewsBrowserControl.setFocus();
        }
      };

      /* Run setFocus() delayed if input not yet set */
      Browser browser = fNewsBrowserControl.getViewer().getBrowser().getControl();
      if (!StringUtils.isSet(browser.getUrl()))
        JobRunner.runDelayedInUIThread(browser, runnable);
      else
        runnable.run();
    }
  }

  @Override
  public void dispose() {
    saveSettings();
    unregisterListeners();

    super.dispose();
    fContentProvider.dispose();
    fNewsTableControl.dispose();
    fNewsBrowserControl.dispose();
    fResourceManager.dispose();
    fIsDisposed = true;
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removePartListener(fPartListener);
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
    DynamicDAO.removeEntityListener(IFeed.class, fFeedListener);
    DynamicDAO.removeEntityListener(ISearchCondition.class, fSearchConditionListener);
    DynamicDAO.removeEntityListener(INewsBin.class, fNewsBinListener);
    Controller.getDefault().removeBookMarkLoadListener(fBookMarkLoadListener);
  }

  /**
   * Update the Layout in the Feed View.
   */
  public void updateLayout() {
    fRootComposite.setRedraw(false);
    try {
      updateLayout(true);
    } finally {
      fRootComposite.setRedraw(true);
    }
  }

  private void updateLayout(boolean updateInput) {
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(fInput.getMark());
    Layout layout = OwlUI.getLayout(preferences);

    /* Return early if layout already up to date */
    if (fInitialLayout == layout)
      return;

    /* Classic Layout (default) */
    if (layout == Layout.CLASSIC) {
      restoreTable(updateInput);
      fSashForm.setOrientation(SWT.VERTICAL);
    }

    /* Vertical Layout */
    else if (layout == Layout.VERTICAL) {
      restoreTable(updateInput);
      fSashForm.setOrientation(SWT.HORIZONTAL);
    }

    /* Headlines Layout */
    else if (layout == Layout.HEADLINES) {
      maximizeTable(updateInput);
    }

    /* Newspaper Layout */
    else if (layout == Layout.NEWSPAPER) {
      maximizeBrowser(updateInput);
    }

    /* Update Separators */
    updateSeparators(layout);

    /* Hide the Info Bar if it is visible */
    fNewsBrowserControl.setInfoBarVisible(false);

    /* Update some tool items from the filter tool bar */
    fFilterBar.updateToolbars();

    /* Layout */
    fNewsTableControlContainer.layout();
    fBrowserViewerControlContainer.layout();

    /* Remember Layout */
    fInitialLayout = layout;
  }

  private void maximizeTable(boolean updateInput) {
    Control maximizedControl = fSashForm.getMaximizedControl();
    if (fNewsTableControlContainer.equals(maximizedControl))
      return;

    fSashForm.setMaximizedControl(fNewsTableControlContainer);

    if (updateInput) {
      if (fBrowserViewerControlContainer.equals(maximizedControl)) {
        fNewsTableControl.setPartInput(fInput.getMark());
        fNewsTableControl.adjustScrollPosition();
        if (fNewsGrouping.getType() != NewsGrouping.Type.NO_GROUPING)
          expandNewsTableViewerGroups(true, StructuredSelection.EMPTY);
      }
      fNewsBrowserControl.setPartInput(null);
    }

    fNewsTableControl.setFocus();
  }

  private void maximizeBrowser(boolean updateInput) {
    Control maximizedControl = fSashForm.getMaximizedControl();
    if (fBrowserViewerControlContainer.equals(maximizedControl))
      return;

    fSashForm.setMaximizedControl(fBrowserViewerControlContainer);

    if (updateInput) {
      fNewsTableControl.getViewer().setSelection(StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(fInput.getMark());
      fNewsTableControl.setPartInput(null);
    }

    fNewsBrowserControl.setFocus();
  }

  private void restoreTable(boolean updateInput) {
    Control maximizedControl = fSashForm.getMaximizedControl();
    if (maximizedControl == null)
      return;

    fSashForm.setMaximizedControl(null);

    if (updateInput) {
      fNewsTableControl.setPartInput(fInput.getMark());
      fNewsTableControl.adjustScrollPosition();
      if (fNewsGrouping.getType() != NewsGrouping.Type.NO_GROUPING)
        expandNewsTableViewerGroups(true, StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(null);
    }

    fNewsTableControl.setFocus();
  }

  private void updateSeparators(Layout layout) {

    /* Table Separators */
    boolean showFilterTableSeparator = false;
    if (!Application.IS_MAC || layout != Layout.CLASSIC)
      showFilterTableSeparator = fFilterBar.isVisible();
    ((GridData) fHorizontalFilterTableSep.getLayoutData()).exclude = !showFilterTableSeparator;
    ((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude = (layout != Layout.VERTICAL);
    ((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude = (layout != Layout.CLASSIC);

    /* Browser Separators */
    ((GridData) fVerticalBrowserSep.getLayoutData()).exclude = (layout != Layout.VERTICAL);

    /* Horizontal Layout */
    if (layout == Layout.CLASSIC) {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = false;
    }

    /* Verical Layout */
    else if (layout == Layout.VERTICAL) {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
    }

    /* Newspaper Layout */
    else if (layout == Layout.NEWSPAPER) {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
    }

    /* Update Visibility based on Layout Data */
    fHorizontalFilterTableSep.setVisible(!((GridData) fHorizontalFilterTableSep.getLayoutData()).exclude);
    fVerticalTableBrowserSep.setVisible(!((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude);
    fHorizontalTableBrowserSep.setVisible(!((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude);
    fVerticalBrowserSep.setVisible(!((GridData) fVerticalBrowserSep.getLayoutData()).exclude);
    fHorizontalBrowserSep.setVisible(!((GridData) fHorizontalBrowserSep.getLayoutData()).exclude);
  }

  /**
   * Update the visibility of the filter bar and browser bar.
   */
  public void updateToolbarVisibility() {
    fFilterBar.updateVisibility();
    fBrowserBar.updateVisibility();
    updateSeparators(fInitialLayout);
    fRootComposite.layout(true, true);
  }

  /**
   * Toggle between newspaper and classic/vertical layout.
   */
  public void toggleNewspaperLayout() {

    /* Lookup current layout from actual UI to support toggling */
    boolean isNewspaperLayout = !isTableViewerVisible();
    boolean isClassicLayout = (fSashForm.getOrientation() & SWT.VERTICAL) != 0;

    /* Determine new layout based on existing */
    Layout newLayout = isNewspaperLayout ? (isClassicLayout ? Layout.CLASSIC : Layout.VERTICAL) : Layout.NEWSPAPER;

    /* Save only into Entity if the Entity was configured with the given Settings before */
    FeedViewInput input = ((FeedViewInput) getEditorInput());
    IPreferenceScope entityPreferences;
    if (input.getMark() instanceof FolderNewsMark)
      entityPreferences = Owl.getPreferenceService().getEntityScope(((FolderNewsMark) input.getMark()).getFolder());
    else
      entityPreferences = Owl.getPreferenceService().getEntityScope(input.getMark());

    if (entityPreferences.hasKey(DefaultPreferences.FV_LAYOUT)) {
      entityPreferences.putInteger(DefaultPreferences.FV_LAYOUT, newLayout.ordinal());
      entityPreferences.flush();

      /* Update Layout (on current feed view) */
      updateLayout();
    }

    /* Save Globally */
    else {
      fPreferences.putInteger(DefaultPreferences.FV_LAYOUT, newLayout.ordinal());

      /* Update Layout (on all opened feed views) */
      EditorUtils.updateLayout();
    }
  }

  /**
   * Refreshes all parts of this editor.
   *
   * @param delayRedraw If <code>TRUE</code> delay redraw until operation is
   * done.
   * @param updateLabels If <code>TRUE</code> update all Labels.
   */
  void refresh(boolean delayRedraw, boolean updateLabels) {
    refreshTableViewer(delayRedraw, updateLabels);
    refreshBrowserViewer();
  }

  /**
   * A special key was pressed from the Quicksearch Input-Field. Handle it.
   *
   * @param traversal The Traversal that occured from the quicksearch.
   */
  void handleQuicksearchTraversalEvent(int traversal) {

    /* Enter was hit */
    if ((traversal & SWT.TRAVERSE_RETURN) != 0) {

      /* Select and Focus TreeViewer */
      if (isTableViewerVisible()) {
        Tree tree = (Tree) fNewsTableControl.getViewer().getControl();
        if (tree.getItemCount() > 0) {
          fNewsTableControl.getViewer().setSelection(new StructuredSelection(tree.getItem(0).getData()));
          fNewsTableControl.setFocus();
        }
      }

      /* Move Focus into BrowserViewer */
      else {
        fNewsBrowserControl.setFocus();
      }
    }

    /* Page Up / Down was hit */
    else if ((traversal & SWT.TRAVERSE_PAGE_NEXT) != 0 || (traversal & SWT.TRAVERSE_PAGE_PREVIOUS) != 0) {
      setFocus();
    }
  }

  /* Refresh Table-Viewer if visible */
  void refreshTableViewer(boolean delayRedraw, boolean updateLabels) {

    /* Return on Shutdown */
    if (Controller.getDefault().isShuttingDown())
      return;

    /* Only if Table Viewer is visible */
    if (isTableViewerVisible()) {
      boolean groupingEnabled = fNewsGrouping.getType() != NewsGrouping.Type.NO_GROUPING;

      /* Remember Selection if grouping enabled */
      ISelection selection = StructuredSelection.EMPTY;
      if (groupingEnabled)
        selection = fNewsTableControl.getViewer().getSelection();

      /* Delay redraw operations if requested */
      if (delayRedraw)
        fNewsTableControl.getViewer().getControl().getParent().setRedraw(false);
      try {

        /* Refresh */
        fNewsTableControl.getViewer().refresh(updateLabels);

        /* Expand all Groups if grouping is enabled */
        if (groupingEnabled)
          expandNewsTableViewerGroups(false, selection);
      }

      /* Redraw now if delayed before */
      finally {
        if (delayRedraw)
          fNewsTableControl.getViewer().getControl().getParent().setRedraw(true);
      }
    }
  }

  private void expandNewsTableViewerGroups(boolean delayRedraw, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Remember TopItem if required */
    TreeItem topItem = oldSelection.isEmpty() ? tree.getTopItem() : null;

    /* Expand All & Restore Selection with redraw false */
    if (delayRedraw)
      tree.getParent().setRedraw(false);
    try {
      viewer.expandAll();

      /* Restore selection if required */
      if (!oldSelection.isEmpty() && viewer.getSelection().isEmpty())
        viewer.setSelection(oldSelection, true);
      else if (topItem != null)
        tree.setTopItem(topItem);
    } finally {
      if (delayRedraw)
        tree.getParent().setRedraw(true);
    }
  }

  /* TODO This is a Workaround until Eclipse Bug #159586 is fixed */
  private void stableSetInputToNewsTable(Object input, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Set Input & Restore Selection with redraw false */
    tree.getParent().setRedraw(false);
    try {
      fNewsTableControl.setPartInput(input);

      /* Restore selection if required */
      if (oldSelection != null) {
        fNewsTableControl.setBlockNewsStateTracker(true);
        try {
          viewer.setSelection(oldSelection);
        } finally {
          fNewsTableControl.setBlockNewsStateTracker(false);
        }
      }

      /* Adjust Scroll Position */
      fNewsTableControl.adjustScrollPosition();
    } finally {
      tree.getParent().setRedraw(true);
    }
  }

  private void rememberSelection(final IMark inputMark, final IStructuredSelection selection) {
    SafeRunnable.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        IPreferenceScope inputPrefs = Owl.getPreferenceService().getEntityScope(inputMark);
        long oldSelectionValue = inputPrefs.getLong(DefaultPreferences.NM_SELECTED_NEWS);

        /* Find Selected News ID */
        long newSelectionValue = 0;
        if (!selection.isEmpty()) {
          Object obj = selection.getFirstElement();
          if (obj instanceof INews)
            newSelectionValue = ((INews) obj).getId();
        }

        boolean needToSave = false;

        /* Selection Provided */
        if (newSelectionValue > 0) {
          if (oldSelectionValue != newSelectionValue) {
            needToSave = true;
            inputPrefs.putLong(DefaultPreferences.NM_SELECTED_NEWS, newSelectionValue);
          }
        }

        /* No Selection Provided */
        else {
          if (oldSelectionValue > 0) {
            needToSave = true;
            inputPrefs.delete(DefaultPreferences.NM_SELECTED_NEWS);
          }
        }

        IEntity entityToSave;
        if (fInput.getMark() instanceof FolderNewsMark)
          entityToSave = ((FolderNewsMark) fInput.getMark()).getFolder();
        else
          entityToSave = fInput.getMark();

        if (needToSave)
          DynamicDAO.save(entityToSave);
      }
    });
  }

  /* Refresh Browser-Viewer */
  void refreshBrowserViewer() {

    /* Return on Shutdown */
    if (Controller.getDefault().isShuttingDown())
      return;

    /* Refresh if browser is visible */
    if (isBrowserViewerVisible())
      fNewsBrowserControl.getViewer().refresh();
  }

  /**
   * Check wether the News-Table-Part of this Editor is visible or not
   * (minmized).
   *
   * @return TRUE if the News-Table-Part is visible, FALSE otherwise.
   */
  boolean isTableViewerVisible() {
    return fSashForm.getMaximizedControl() == null || fSashForm.getMaximizedControl() == fNewsTableControlContainer;
  }

  /**
   * Check wether the Browser-Part of this Editor is visible or not (minmized).
   *
   * @return TRUE if the Browser-Table-Part is visible, FALSE otherwise.
   */
  boolean isBrowserViewerVisible() {
    return fSashForm.getMaximizedControl() == null || fSashForm.getMaximizedControl() == fBrowserViewerControlContainer;
  }

  /**
   * Get the shared ViewerFilter used to filter News.
   *
   * @return the shared ViewerFilter used to filter News.
   */
  NewsFilter getFilter() {
    return fNewsFilter;
  }

  /**
   * Get the shared Viewer-Grouper used to group News.
   *
   * @return the shared Viewer-Grouper used to group News.
   */
  NewsGrouping getGrouper() {
    return fNewsGrouping;
  }

  NewsBrowserControl getNewsBrowserControl() {
    return fNewsBrowserControl;
  }

  NewsTableControl getNewsTableControl() {
    return fNewsTableControl;
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    fCreated = true;
    fParent = parent;

    /* Shared Viewer Helper */
    fNewsFilter = new NewsFilter();
    fNewsFilter.setType(fInitialFilterType);
    fNewsFilter.setSearchTarget(fInitialSearchTarget);
    fNewsFilter.setNewsMark(fInput.getMark());

    fNewsGrouping = new NewsGrouping();
    fNewsGrouping.setType(fInitialGroupType);

    /* Top-Most root Composite in Editor */
    fRootComposite = new Composite(fParent, SWT.NONE);
    fRootComposite.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    ((GridLayout) fRootComposite.getLayout()).verticalSpacing = 0;

    /* FilterBar */
    fFilterBar = new FilterBar(this, fRootComposite);

    /* Separate Filter from Table */
    boolean showSeparator = false;
    if (!Application.IS_MAC || fInitialLayout != Layout.CLASSIC)
      showSeparator = fFilterBar.isVisible();
    fHorizontalFilterTableSep = new Label(fRootComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
    fHorizontalFilterTableSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    ((GridData) fHorizontalFilterTableSep.getLayoutData()).exclude = !showSeparator;
    fHorizontalFilterTableSep.setVisible(showSeparator);

    /* SashForm dividing Feed and News View */
    boolean useClassicLayout = (fInitialLayout == Layout.CLASSIC || fInitialLayout == Layout.NEWSPAPER || fInitialLayout == Layout.HEADLINES);
    fSashForm = new SashForm(fRootComposite, (useClassicLayout ? SWT.VERTICAL : SWT.HORIZONTAL) | SWT.SMOOTH);
    fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Table-Viewer to display headlines */
    NewsTableViewer tableViewer;
    {
      fNewsTableControlContainer = new Composite(fSashForm, SWT.None);
      fNewsTableControlContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
      fNewsTableControlContainer.addControlListener(new ControlAdapter() {
        @Override
        public void controlResized(ControlEvent e) {
          fCacheWeights = fSashForm.getWeights();
        }
      });

      fNewsTableControl = new NewsTableControl();
      fNewsTableControl.init(fFeedViewSite);
      fNewsTableControl.onInputChanged(fInput);

      /* Create Viewer */
      fNewsTableControl.createPart(fNewsTableControlContainer);
      tableViewer = fNewsTableControl.getViewer();

      /* Clear any quicksearch when ESC is hit from the Tree */
      tableViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Separate from Browser-Viewer (Vertically) */
      fVerticalTableBrowserSep = new Label(fNewsTableControlContainer, SWT.SEPARATOR | SWT.VERTICAL);
      fVerticalTableBrowserSep.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));
      ((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude = (fInitialLayout != Layout.VERTICAL);

      /* Separate from Browser-Viewer (Horizontally) */
      fHorizontalTableBrowserSep = new Label(fNewsTableControlContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
      fHorizontalTableBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
      ((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude = (fInitialLayout != Layout.CLASSIC);
    }

    /* Browser-Viewer to display news */
    NewsBrowserViewer browserViewer;
    {
      fBrowserViewerControlContainer = new Composite(fSashForm, SWT.None);
      fBrowserViewerControlContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
      fBrowserViewerControlContainer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

      /* Separate to Browser (Vertically) */
      fVerticalBrowserSep = new Label(fBrowserViewerControlContainer, SWT.SEPARATOR | SWT.VERTICAL);
      fVerticalBrowserSep.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false, 1, 3));
      ((GridData) fVerticalBrowserSep.getLayoutData()).exclude = (fInitialLayout != Layout.VERTICAL);

      /* Browser Bar for Navigation */
      fBrowserBar = new BrowserBar(this, fBrowserViewerControlContainer);

      /* Separate to Browser (Horizontally) */
      fHorizontalBrowserSep = new Label(fBrowserViewerControlContainer, SWT.SEPARATOR | SWT.HORIZONTAL);

      /* Horizontal Layout */
      if (fInitialLayout == Layout.CLASSIC) {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = false;
      }

      /* Verical Layout */
      else if (fInitialLayout == Layout.VERTICAL) {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
      }

      /* Browser Maximized */
      else if (fInitialLayout == Layout.NEWSPAPER) {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
      }

      fNewsBrowserControl = new NewsBrowserControl();
      fNewsBrowserControl.init(fFeedViewSite);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Create Viewer */
      fNewsBrowserControl.createPart(fBrowserViewerControlContainer);
      browserViewer = fNewsBrowserControl.getViewer();

      /* Clear any quicksearch when ESC is hit from the Tree */
      browserViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Init the Browser Bar with the CBrowser */
      fBrowserBar.init(browserViewer.getBrowser());
    }

    /* SashForm weights */
    fSashForm.setWeights(fInitialWeights);
    if (fInitialLayout == Layout.NEWSPAPER)
      fSashForm.setMaximizedControl(fBrowserViewerControlContainer);
    else if (fInitialLayout == Layout.HEADLINES)
      fSashForm.setMaximizedControl(fNewsTableControlContainer);

    /* Create the shared Content-Provider */
    fContentProvider = new NewsContentProvider(tableViewer, browserViewer, this);

    /* Init all Viewers */
    fNewsTableControl.initViewer(fContentProvider, fNewsFilter);
    fNewsBrowserControl.initViewer(fContentProvider, fNewsFilter);

    /* Set Input to Viewers */
    setInput(fInput.getMark(), false);
  }

  /**
   * Navigate to the next/previous read or unread News respecting the News-Items
   * that are displayed in the NewsTableControl.
   *
   * @param respectSelection If <code>TRUE</code>, respect the current selected
   * Item from the Tree as starting-node for the navigation, or
   * <code>FALSE</code> otherwise.
   * @param delay if <code>true</code> delay the navigation if the browser is
   * maximized and <code>false</code> if not.
   * @param next If <code>TRUE</code>, move to the next item, or previous if
   * <code>FALSE</code>.
   * @param unread If <code>TRUE</code>, only move to unread items, or ignore if
   * <code>FALSE</code>.
   * @return Returns <code>TRUE</code> in case navigation found a valid item, or
   * <code>FALSE</code> otherwise.
   */
  public boolean navigate(boolean respectSelection, boolean delay, final boolean next, final boolean unread) {

    /* Check for unread counter */
    if (unread && fInput.getMark().getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)) == 0)
      return false;

    /* Navigate in maximized Browser */
    if (!isTableViewerVisible()) {

      /* Delay navigation because input was just set and browser needs a little to render */
      if (delay) {
        JobRunner.runInUIThread(300, fNewsBrowserControl.getViewer().getControl(), new Runnable() {
          public void run() {
            fNewsBrowserControl.getViewer().navigate(next, unread);
          }
        });
      }

      /* Directly Navigate */
      else
        fNewsBrowserControl.getViewer().navigate(next, unread);
      return true;
    }

    Tree newsTree = fNewsTableControl.getViewer().getTree();

    /* Nothing to Navigate to */
    if (newsTree.getItemCount() == 0 || newsTree.isDisposed())
      return false;

    /* Navigate */
    return navigate(newsTree, respectSelection, next, unread);
  }

  private boolean navigate(Tree tree, boolean respectSelection, boolean next, boolean unread) {

    /* Selection is Present */
    if (respectSelection && tree.getSelectionCount() > 0) {

      /* Try navigating from Selection */
      ITreeNode startingNode = new WidgetTreeNode(tree.getSelection()[0], fNewsTableControl.getViewer());
      if (navigate(startingNode, next, unread))
        return true;
    }

    /* No Selection is Present */
    else {
      ITreeNode startingNode = new WidgetTreeNode(tree, fNewsTableControl.getViewer());
      return navigate(startingNode, true, unread);
    }

    return false;
  }

  private boolean navigate(ITreeNode startingNode, boolean next, final boolean unread) {

    /* Create Traverse-Helper */
    TreeTraversal traverse = new TreeTraversal(startingNode) {
      @Override
      public boolean select(ITreeNode node) {
        return isValidNavigation(node, unread);
      }
    };

    /* Retrieve and select new Target Node */
    ITreeNode targetNode = (next ? traverse.nextNode() : traverse.previousNode());
    if (targetNode != null) {
      ISelection selection = new StructuredSelection(targetNode.getData());
      fNewsTableControl.getViewer().setSelection(selection, true);
      return true;
    }

    return false;
  }

  private boolean isValidNavigation(ITreeNode node, boolean unread) {
    Object data = node.getData();

    /* Require a News */
    if (!(data instanceof INews))
      return false;

    /* Check if News is unread if set as flag */
    INews news = (INews) data;
    if (unread && !CoreUtils.isUnread(news.getState()))
      return false;

    return true;
  }

  /**
   * @return <code>true</code> if this feedview is currently visible or
   * <code>false</code> otherwise.
   */
  public boolean isVisible() {
    return fEditorSite.getPage().isPartVisible(fEditorSite.getPart());
  }

  /**
   * Returns the <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   *
   * @return The <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   */
  Composite getEditorControl() {
    return fParent;
  }

  /**
   * @param blockFeedChangeEvent <code>true</code> to block the processing of
   * feed change events and <code>false</code> otherwise.
   */
  public static void setBlockFeedChangeEvent(boolean blockFeedChangeEvent) {
    fgBlockFeedChangeEvent = blockFeedChangeEvent;
  }

  /**
   * @return <code>true</code> if the news browser viewer of this feed view is
   * showing the contents of a website and <code>false</code> otherwise.
   */
  public boolean isBrowserShowingNews() {
    if (fNewsBrowserControl != null && fNewsBrowserControl.getViewer() != null) {
      if (isBrowserViewerVisible()) {
        CBrowser browser = fNewsBrowserControl.getViewer().getBrowser();
        if (browser != null && browser.getControl() != null && !browser.getControl().isDisposed()) {
          String url = browser.getControl().getUrl();
          return StringUtils.isSet(url) && ApplicationServer.getDefault().isNewsServerUrl(url);
        }
      }
    }

    return false;
  }
}