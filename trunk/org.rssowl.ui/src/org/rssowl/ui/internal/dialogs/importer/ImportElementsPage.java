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

package org.rssowl.ui.internal.dialogs.importer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IAbortable;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.LoginDialog;
import org.rssowl.ui.internal.dialogs.importer.ImportSourcePage.Source;
import org.rssowl.ui.internal.util.FolderChildCheckboxTree;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 *A {@link WizardPage} to select the elements to import.
 *
 * @author bpasero
 */
public class ImportElementsPage extends WizardPage {

  /* Initial Connection Timeout when looking for Feeds remotely */
  private static final int INITIAL_CON_TIMEOUT = 15000;

  /* Connection Timeout when testing for Feeds remotely */
  private static final int FEED_CON_TIMEOUT = 7000;

  private CheckboxTreeViewer fViewer;
  private FolderChildCheckboxTree fFolderChildTree;
  private Button fDeselectAll;
  private Button fSelectAll;
  private Button fFlattenCheck;
  private Button fHideExistingCheck;
  private ExistingBookmarkFilter fExistingFilter = new ExistingBookmarkFilter();

  /* Remember Current Import Values */
  private Source fCurrentSourceKind;
  private String fCurrentSourceResource;
  private String fCurrentSourceKeywords;
  private long fCurrentSourceFileModified;

  /* Imported Entities */
  private List<ILabel> fLabels = Collections.synchronizedList(new ArrayList<ILabel>());
  private List<ISearchFilter> fFilters = Collections.synchronizedList(new ArrayList<ISearchFilter>());
  private List<IPreference> fPreferences = Collections.synchronizedList(new ArrayList<IPreference>());

  /* Filter to Exclude Existing Bookmarks (empty folders are excluded as well) */
  private static class ExistingBookmarkFilter extends ViewerFilter {
    private IBookMarkDAO dao = DynamicDAO.getDAO(IBookMarkDAO.class);
    private Map<IFolderChild, Boolean> cache = new IdentityHashMap<IFolderChild, Boolean>();

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (element instanceof IFolderChild)
        return select((IFolderChild) element);

      return true;
    }

    void clear() {
      cache.clear();
    }

    private boolean select(IFolderChild element) {

      /* Bookmark (exclude if another Bookmark with same Link exists) */
      if (element instanceof IBookMark) {
        IBookMark bm = (IBookMark) element;
        Boolean select = cache.get(bm);
        if (select == null) {
          select = !dao.exists(bm.getFeedLinkReference());
          cache.put(bm, select);
        }

        return select;
      }

      /* Bin (exclude if another Bin with same name Exists at same Location) */
      else if (element instanceof INewsBin) {
        INewsBin bin = (INewsBin) element;
        Boolean select = cache.get(bin);
        if (select == null) {
          select = !CoreUtils.existsNewsBin(bin);
          cache.put(bin, select);
        }

        return select;
      }

      /* Search (exclude if another Search with same name Exists at same Location and same Conditions) */
      else if (element instanceof ISearchMark) {
        ISearchMark searchmark = (ISearchMark) element;
        Boolean select = cache.get(searchmark);
        if (select == null) {
          select = !CoreUtils.existsSearchMark(searchmark);
          cache.put(searchmark, select);
        }

        return select;
      }

      /* Folder */
      else if (element instanceof IFolder) {
        IFolder folder = (IFolder) element;
        Boolean select = cache.get(folder);
        if (select == null) {
          List<IFolderChild> children = folder.getChildren();
          for (IFolderChild child : children) {
            select = select(child);
            if (select)
              break;
          }

          cache.put(folder, select);
        }

        return select != null ? select : false;
      }

      return true;
    }
  }

  /**
   * @param pageName
   */
  protected ImportElementsPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please choose the elements to import.");
  }

  /* Get Elements to Import */
  List<IFolderChild> getFolderChildsToImport() {
    importSource(); //Ensure to be in sync with Source
    return fFolderChildTree.getCheckedElements();
  }

  /* Returns Labels available for Import */
  List<ILabel> getLabelsToImport() {
    importSource(); //Ensure to be in sync with Source
    return fLabels;
  }

  /* Returns Filters available for Import */
  List<ISearchFilter> getFiltersToImport() {
    importSource(); //Ensure to be in sync with Source
    return fFilters;
  }

  /* Returns the Preferences available for Import */
  List<IPreference> getPreferencesToImport() {
    importSource(); //Ensure to be in sync with Source
    return fPreferences;
  }

  /* Returns whether existing bookmarks should be ignored for the Import */
  boolean excludeExisting() {
    return fHideExistingCheck.getSelection();
  }

  /* Check if the Options Page should be shown from the Wizard */
  boolean showOptionsPage() {
    return !fLabels.isEmpty() || !fFilters.isEmpty() || !fPreferences.isEmpty();
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Viewer for Folder Child Selection */
    fFolderChildTree = new FolderChildCheckboxTree(container);
    fViewer = fFolderChildTree.getViewer();

    /* Filter (exclude existing) */
    fViewer.addFilter(fExistingFilter);

    /* Update Page Complete on Selection */
    fViewer.getTree().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
      }
    });

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(4, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setAllChecked(true);
        updatePageComplete();
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setAllChecked(false);
        updatePageComplete();
      }
    });

    /* Show as Flat List of News Marks */
    fFlattenCheck = new Button(buttonContainer, SWT.CHECK);
    fFlattenCheck.setText("Flatten Hierarchy");
    setButtonLayoutData(fFlattenCheck);
    ((GridData) fFlattenCheck.getLayoutData()).horizontalAlignment = SWT.END;
    ((GridData) fFlattenCheck.getLayoutData()).grabExcessHorizontalSpace = true;
    fFlattenCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFolderChildTree.setFlat(fFlattenCheck.getSelection());
        fViewer.expandToLevel(2);
      }
    });

    /* Hide Existing News Marks */
    fHideExistingCheck = new Button(buttonContainer, SWT.CHECK);
    fHideExistingCheck.setText("Hide Existing");
    fHideExistingCheck.setSelection(true);
    setButtonLayoutData(fHideExistingCheck);
    fHideExistingCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (fHideExistingCheck.getSelection())
          fViewer.addFilter(fExistingFilter);
        else
          fViewer.removeFilter(fExistingFilter);

        fViewer.expandToLevel(2);
        updateMessage(false);
      }
    });

    setControl(container);
  }

  private void updateMessage(boolean clearErrors) {
    List<?> input = (List<?>) fViewer.getInput();
    if (!input.isEmpty() && fViewer.getTree().getItemCount() == 0 && fViewer.getFilters().length > 0)
      setMessage("Some elemens are hidden because they already exist.", IMessageProvider.WARNING);
    else
      setMessage("Please choose the elements to import.");

    if (clearErrors)
      setErrorMessage(null);

    updatePageComplete();
  }

  private void updatePageComplete() {
    boolean complete = (showOptionsPage() || fViewer.getCheckedElements().length > 0);
    setPageComplete(complete);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (!visible)
      return;

    fViewer.getControl().setFocus();

    Runnable runnable = new Runnable() {
      public void run() {
        importSource();
      }
    };

    /* Load Elements to Import from Source on first time */
    ImportSourcePage importSourcePage = (ImportSourcePage) getPreviousPage();
    if (importSourcePage.isRemoteSource())
      JobRunner.runInUIThread(50, getShell(), runnable);
    else
      runnable.run();
  }

  private void importSource() {
    try {
      doImportSource();
    } catch (Exception e) {
      String message = e.getMessage();
      if (e instanceof InvocationTargetException && e.getCause() != null && StringUtils.isSet(e.getCause().getMessage()))
        message = e.getCause().getMessage();

      Activator.getDefault().logError(message, e);
      setErrorMessage(message);
    }
  }

  private void doImportSource() throws Exception {
    ImportSourcePage importSourcePage = (ImportSourcePage) getPreviousPage();
    Source source = importSourcePage.getSource();

    /* Return if the Source did not Change */
    if (source == Source.RECOMMENDED && fCurrentSourceKind == Source.RECOMMENDED)
      return;
    else if (source == Source.KEYWORD && fCurrentSourceKind == Source.KEYWORD && importSourcePage.getImportKeywords().equals(fCurrentSourceKeywords))
      return;
    else if (source == Source.RESOURCE && fCurrentSourceKind == Source.RESOURCE) {
      String importResource = importSourcePage.getImportResource();

      /* Same URL */
      if (importSourcePage.isRemoteSource() && importResource.equals(fCurrentSourceResource))
        return;

      /* Same Unmodified File */
      else if (importResource.equals(fCurrentSourceResource)) {
        File file = new File(importResource);
        if (file.exists() && file.lastModified() == fCurrentSourceFileModified)
          return;
      }
    }

    /* Remember Source */
    fCurrentSourceKind = source;
    fCurrentSourceResource = importSourcePage.getImportResource();
    File sourceFile = (source == Source.RESOURCE) ? new File(importSourcePage.getImportResource()) : null;
    fCurrentSourceFileModified = (sourceFile != null && sourceFile.exists()) ? sourceFile.lastModified() : 0;
    fCurrentSourceKeywords = importSourcePage.getImportKeywords();

    /* Reset Fields */
    fLabels.clear();
    fFilters.clear();
    fPreferences.clear();

    /* Reset Messages */
    setErrorMessage(null);
    setMessage("Please choose the elements to import.");

    /* Import from Supplied File */
    if (source == Source.RESOURCE && sourceFile != null && sourceFile.exists()) {
      importFromLocalResource(new FileInputStream(sourceFile));
    }

    /* Import from Supplied Online Resource */
    else if (source == Source.RESOURCE && URIUtils.looksLikeLink(fCurrentSourceResource)) {
      importFromOnlineResource(new URI(URIUtils.ensureProtocol(fCurrentSourceResource)));
    }

    /* Import by Keyword Search */
    else if (source == Source.KEYWORD) {
      importFromKeywordSearch(fCurrentSourceKeywords);
    }

    /* Import from Default OPML File */
    else if (source == Source.RECOMMENDED) {
      importFromLocalResource(getClass().getResourceAsStream("/default_feeds.xml")); //$NON-NLS-1$;
    }
  }

  /* Import from a Local Input Stream (no progress required) */
  private void importFromLocalResource(InputStream in) throws InterpreterException, ParserException {

    /* Show Folder Childs in Viewer */
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(in);
    setImportedElements(types);
  }

  private void importFromOnlineResource(final URI link) throws InvocationTargetException, InterruptedException {
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(final IProgressMonitor monitor) throws InvocationTargetException {
        InputStream in = null;
        boolean canceled = false;
        Exception error = null;
        boolean bruteForce = false;
        try {
          monitor.beginTask("Searching for Feeds ('Cancel' to stop)", IProgressMonitor.UNKNOWN);
          monitor.subTask("Connecting to " + link.getHost() + "...");

          /* Return on Cancellation */
          if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
            return;

          /* Open Stream */
          in = openStream(link, monitor, INITIAL_CON_TIMEOUT);

          /* Return on Cancellation */
          if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
            canceled = true;
            return;
          }

          /* Try to Import */
          try {
            final List<? extends IEntity> types = Owl.getInterpreter().importFrom(in);

            /* Return on Cancellation */
            if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
              canceled = true;
              return;
            }

            /* Show in UI */
            JobRunner.runInUIThread(getShell(), new Runnable() {
              public void run() {
                setImportedElements(types);
              }
            });
          }

          /* Error Importing from File - Try Bruteforce then */
          catch (Exception e) {
            error = e;
            bruteForce = true;
          }
        }

        /* Error finding a Handler for the Link - Rethrow */
        catch (Exception e) {
          final boolean showError[] = new boolean[] { true };

          /* Give user a chance to log in */
          if (e instanceof AuthenticationRequiredException && !monitor.isCanceled() && !Controller.getDefault().isShuttingDown()) {
            final Shell shell = OwlUI.getActiveShell();
            if (shell != null && !shell.isDisposed()) {
              Controller.getDefault().getLoginDialogLock().lock();
              try {
                final AuthenticationRequiredException authEx = (AuthenticationRequiredException) e;
                JobRunner.runSyncedInUIThread(shell, new Runnable() {
                  public void run() {
                    try {

                      /* Return on Cancelation or shutdown or deletion */
                      if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
                        return;

                      /* Credentials might have been provided meanwhile in another dialog */
                      try {
                        URI normalizedUri = URIUtils.normalizeUri(link, true);
                        if (Owl.getConnectionService().getAuthCredentials(normalizedUri, authEx.getRealm()) != null) {
                          importFromOnlineResource(link);
                          showError[0] = false;
                          return;
                        }
                      } catch (CredentialsException exe) {
                        Activator.getDefault().getLog().log(exe.getStatus());
                      }

                      /* Show Login Dialog */
                      LoginDialog login = new LoginDialog(shell, link, authEx.getRealm());
                      if (login.open() == Window.OK && !monitor.isCanceled() && !Controller.getDefault().isShuttingDown()) {
                        importFromOnlineResource(link);
                        showError[0] = false;
                      }
                    } catch (InvocationTargetException e) {
                      /* Ignore - Error will be handled outside already */
                    } catch (InterruptedException e) {
                      /* Ignore - Error will be handled outside already */
                    }
                  }
                });
              } finally {
                Controller.getDefault().getLoginDialogLock().unlock();
              }
            }
          }

          /* Rethrow Exception */
          if (showError[0])
            throw new InvocationTargetException(e);
        } finally {

          /* Close Input Stream */
          if (in != null) {
            try {
              if ((canceled || error != null) && in instanceof IAbortable)
                ((IAbortable) in).abort();
              else
                in.close();
            } catch (IOException e) {
              throw new InvocationTargetException(e);
            }
          }
        }

        /* Scan remote Resource for Links and valid Feeds */
        if (bruteForce && !monitor.isCanceled() && !Controller.getDefault().isShuttingDown()) {
          try {
            importFromOnlineResourceBruteforce(link, monitor, false);
          } catch (Exception e) {
            throw new InvocationTargetException(e);
          }
        }

        /* Done */
        monitor.done();
      }
    };

    /* Run Operation in Background and allow for Cancellation */
    getContainer().run(true, true, runnable);
  }

  private void importFromKeywordSearch(final String keywords) throws Exception {
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          monitor.beginTask("Searching for Feeds ('Cancel' to stop)", IProgressMonitor.UNKNOWN);
          monitor.subTask("Connecting...");

          /* Build Link for Keyword-Feed Search */
          StringBuilder linkVal = new StringBuilder("http://www.syndic8.com/feedlist.php?ShowMatch=");
          linkVal.append(URIUtils.urlEncode(keywords));
          linkVal.append("&ShowStatus=all");

          /* Return on Cancellation */
          if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
            return;

          /* Scan remote Resource for Links and valid Feeds */
          importFromOnlineResourceBruteforce(new URI(linkVal.toString()), monitor, true);
        } catch (Exception e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };

    /* Run Operation in Background and allow for Cancellation */
    getContainer().run(true, true, runnable);
  }

  @SuppressWarnings("null")
  private void importFromOnlineResourceBruteforce(final URI resourceLink, IProgressMonitor monitor, final boolean isKeywordSearch) throws ConnectionException, IOException {

    /* Read Content */
    String content = readContent(resourceLink, monitor);

    /* Extract Links from Content */
    List<String> links = Collections.emptyList();
    if (StringUtils.isSet(content))
      links = RegExUtils.extractLinksFromText(content, false);

    /* Sort List: First process likely feeds, then others */
    final String resourceLinkValue = resourceLink.toString();
    Collections.sort(links, new Comparator<String>() {
      public int compare(String o1, String o2) {

        /* Check common feed patterns in URL */
        if (URIUtils.looksLikeFeedLink(o1, false))
          return -1;
        else if (URIUtils.looksLikeFeedLink(o2, false))
          return 1;

        /* Check Origin from same Domain */
        if (!isKeywordSearch) {
          if (o1.contains(resourceLinkValue))
            return -1;
          else if (o2.contains(resourceLinkValue))
            return 1;
        }

        return -1;
      }
    });

    /* Reset Input to Empty in the Beginning  */
    JobRunner.runInUIThread(getShell(), new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        setImportedElements(Collections.EMPTY_LIST);
      }
    });

    /* Update Task Information */
    monitor.beginTask("Searching for Feeds ('Cancel' to stop)", links.size());
    if (isKeywordSearch)
      monitor.subTask("Connecting...");
    else
      monitor.subTask("Connecting to " + resourceLink.getHost() + "...");

    /* A Root to add Found Bookmarks into */
    final IFolder defaultRootFolder = Owl.getModelFactory().createFolder(null, null, "Bookmarks");
    defaultRootFolder.setProperty(ITypeImporter.TEMPORARY_FOLDER, true);

    /* For Each Link of the Queue - try to interpret as Feed */
    int counter = 0;
    final List<String> foundBookMarkNames = new ArrayList<String>();
    IBookMarkDAO dao = DynamicDAO.getDAO(IBookMarkDAO.class);
    for (String feedLinkVal : links) {
      monitor.worked(1);

      InputStream in = null;
      boolean canceled = false;
      Exception error = null;
      try {
        URI feedLink = new URI(feedLinkVal);

        /* Ignore if already present in Subscriptions List */
        if (dao.exists(new FeedLinkReference(feedLink)))
          continue;

        /* Report Progress Back To User */
        if (counter != 0)
          monitor.subTask(counter + (counter == 1 ? " Result" : " Results"));

        /* Return on Cancellation */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
          break;

        /* Open Stream to potential Feed */
        in = openStream(feedLink, monitor, FEED_CON_TIMEOUT);

        /* Return on Cancellation */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
          canceled = true;
          break;
        }

        /* Try to interpret as Feed */
        IFeed feed = Owl.getModelFactory().createFeed(null, feedLink);
        Owl.getInterpreter().interpret(in, feed, null);

        /* Return on Cancellation */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
          canceled = true;
          break;
        }

        /* Add as Result if Feed contains News */
        if (!feed.getNews().isEmpty() && StringUtils.isSet(feed.getTitle())) {
          String title = feed.getTitle();
          boolean sameTitleExists = foundBookMarkNames.contains(title);
          if (sameTitleExists && StringUtils.isSet(feed.getFormat()))
            title = title + " (" + feed.getFormat() + ")";

          final IBookMark bookmark = Owl.getModelFactory().createBookMark(null, defaultRootFolder, new FeedLinkReference(feedLink), title);
          foundBookMarkNames.add(bookmark.getName());
          counter++;

          if (StringUtils.isSet(feed.getDescription()))
            bookmark.setProperty(ITypeImporter.DESCRIPTION_KEY, feed.getDescription());

          if (feed.getHomepage() != null)
            bookmark.setProperty(ITypeImporter.HOMEPAGE_KEY, feed.getHomepage());

          /* Directly show in Viewer */
          JobRunner.runInUIThread(getShell(), new Runnable() {
            public void run() {
              addImportedElement(bookmark);
            }
          });
        }
      }

      /* Ignore Errors (likely not a Feed then) */
      catch (Exception e) {
        error = e;
      }

      /* Close Stream */
      finally {

        /* Close Input Stream */
        if (in != null) {
          try {
            if ((canceled || error != null) && in instanceof IAbortable)
              ((IAbortable) in).abort();
            else
              in.close();
          } catch (IOException e) {
            /* Ignore Silently */
          }
        }
      }
    }

    /* Inform if no feeds have been found */
    if (counter == 0) {
      JobRunner.runInUIThread(getShell(), new Runnable() {
        public void run() {
          setMessage("No Feeds Found during Search.", IMessageProvider.INFORMATION);
        }
      });
    }
  }

  private String readContent(URI link, IProgressMonitor monitor) throws ConnectionException, IOException {
    InputStream in = null;
    try {

      /* Return on Cancellation */
      if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
        return null;

      /* Open Stream */
      in = openStream(link, monitor, INITIAL_CON_TIMEOUT);

      /* Return on Cancellation */
      if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
        return null;

      /* Read Content */
      return StringUtils.readString(new InputStreamReader(in));
    } finally {
      if (in instanceof IAbortable)
        ((IAbortable) in).abort();
      else if (in != null)
        in.close();
    }
  }

  private InputStream openStream(URI link, IProgressMonitor monitor, int timeout) throws ConnectionException {
    IProtocolHandler handler = Owl.getConnectionService().getHandler(link);

    Map<Object, Object> properties = new HashMap<Object, Object>();
    properties.put(IConnectionPropertyConstants.CON_TIMEOUT, timeout);
    return handler.openStream(link, monitor, properties);
  }

  /* Updates Caches and Shows Elements */
  private void setImportedElements(List<? extends IEntity> types) {
    List<IFolderChild> folderChilds = new ArrayList<IFolderChild>();
    for (IEntity type : types) {
      if (type instanceof IFolderChild)
        folderChilds.add((IFolderChild) type);
      else if (type instanceof ILabel)
        fLabels.add((ILabel) type);
      else if (type instanceof ISearchFilter)
        fFilters.add((ISearchFilter) type);
      else if (type instanceof IPreference)
        fPreferences.add((IPreference) type);
    }

    /* Re-Add Filter if necessary */
    if (!fHideExistingCheck.getSelection()) {
      fHideExistingCheck.setSelection(true);
      fViewer.addFilter(fExistingFilter);
    }

    /* Apply as Input */
    fViewer.setInput(folderChilds);
    OwlUI.setAllChecked(fViewer.getTree(), true);
    fExistingFilter.clear();
    updateMessage(true);
  }

  /* Adds a IFolderChild to the Viewer and updates caches */
  @SuppressWarnings("unchecked")
  private void addImportedElement(IFolderChild child) {
    Object input = fViewer.getInput();
    ((List) input).add(child);
    fViewer.add(input, child);
    fViewer.setChecked(child, true);
    updateMessage(true);
  }
}