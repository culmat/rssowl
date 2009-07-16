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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.CBrowser;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.ShareProvider;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.actions.SendLinkAction;
import org.rssowl.ui.internal.dialogs.ShareProvidersListDialog;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * The <code>WebBrowserView</code> is able to display a website in an embedded
 * Browser.
 *
 * @author bpasero
 */
public class WebBrowserView extends EditorPart implements IReusableEditor {

  /** ID of this Editor */
  public static final String EDITOR_ID = "org.rssowl.ui.WebBrowser";

  /* Navigate Back */
  private static final String BACK_ACTION = "org.rssowl.ui.internal.editors.feed.BackAction";

  /* Navigate Forward */
  private static final String FORWARD_ACTION = "org.rssowl.ui.internal.editors.feed.ForwardAction";

  private CBrowser fBrowser;
  private WebBrowserInput fInput;
  private IEditorSite fEditorSite;
  private Text fLocationInput;
  private ToolBarManager fNavigationToolBarManager;
  private Action fSelectAllAction;
  private Action fCutAction;
  private Action fCopyAction;
  private Action fPasteAction;
  private Action fPrintAction;
  private boolean fCreated;
  private boolean fLocationSelectAllOnce = true;
  private IPartListener2 fPartListener;

  /** Leave default for reflection */
  public WebBrowserView() {}

  /*
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    Assert.isTrue(input instanceof WebBrowserInput);
    fEditorSite = site;
    fInput = (WebBrowserInput) input;

    setSite(fEditorSite);
    setInput(fInput);

    /* Hook into Global Actions */
    createGlobalActions();
    setGlobalActions();

    /* Register Listeners */
    registerListeners();
  }

  private void registerListeners() {
    fPartListener = new IPartListener2() {

      public void partHidden(IWorkbenchPartReference partRef) {}

      /* Hook into Global Actions for this Editor */
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        if (WebBrowserView.this.equals(partRef.getPart(false))) {
          setGlobalActions();
          OwlUI.updateWindowTitle(getPartName());
        }
      }

      public void partClosed(IWorkbenchPartReference partRef) {
        IEditorReference[] editors = partRef.getPage().getEditorReferences();
        boolean equalsThis = WebBrowserView.this.equals(partRef.getPart(false));
        if (editors.length == 0 && equalsThis)
          OwlUI.updateWindowTitle(getPartName());
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {}

      public void partActivated(IWorkbenchPartReference partRef) {
        if (WebBrowserView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(getPartName());
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
        if (WebBrowserView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(getPartName());
      }

      public void partOpened(IWorkbenchPartReference partRef) {
        if (WebBrowserView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(getPartName());
      }

      public void partVisible(IWorkbenchPartReference partRef) {
        if (WebBrowserView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(getPartName());
      }
    };

    fEditorSite.getPage().addPartListener(fPartListener);
  }

  /**
   * @return Returns the <code>CBrowser</code> being used in this Editor to
   * display websites.
   */
  public CBrowser getBrowser() {
    return fBrowser;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
   */
  @Override
  public void setInput(IEditorInput input) {
    super.setInput(input);

    fInput = (WebBrowserInput) input;

    /* Update Part Name */
    if (fInput.getContext() != null && StringUtils.isSet(fInput.getContext().getTitle()))
      setPartName(fInput.getContext().getTitle());

    /* Update Browser with Input if already created */
    if (fCreated) {
      WebBrowserInput browserInput = (WebBrowserInput) input;
      if (browserInput.getUrl() != null)
        fBrowser.setUrl(browserInput.getUrl());
      fNavigationToolBarManager.find(BACK_ACTION).update(IAction.ENABLED);
      fNavigationToolBarManager.find(FORWARD_ACTION).update(IAction.ENABLED);
      if (fInput.getUrl() != null)
        fLocationInput.setText(fInput.getUrl());
      else
        fLocationInput.setText("");
    }
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    unregisterListeners();
    super.dispose();
    fCreated = false;
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removePartListener(fPartListener);
  }

  private void createGlobalActions() {

    /* Select All */
    fSelectAllAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Select All in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).selectAll();
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
        fBrowser.print();
      }
    };
  }

  private void setGlobalActions() {

    /* Define Retargetable Global Actions */
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), fPrintAction);

    /* Disable some Edit-Actions at first */
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    fCreated = true;
    parent.setLayout(LayoutUtils.createGridLayout(1, 0, 5, 0, 0, false));

    /* Browser Bar */
    createBrowserBar(parent);

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Browser */
    createBrowser(parent);

    /* Add Listeners */
    hookListeners();
  }

  private void createBrowserBar(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 3, 0));
    ((GridLayout) container.getLayout()).marginBottom = 2;
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Navigation ToolBar */
    createNavigationToolBar(container);

    /* Location Field */
    createLocationInput(container);
  }

  private void createNavigationToolBar(Composite parent) {
    fNavigationToolBarManager = new ToolBarManager(SWT.FLAT);

    /* New Browser Tab */
    IAction newBrowserTab = new Action("New Browser Tab") {
      @Override
      public void run() {
        BrowserUtils.openLinkInternal(URIUtils.ABOUT_BLANK, null);
      }
    };
    newBrowserTab.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/new_browser_tab.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(newBrowserTab);

    /* Navigate Backward */
    fNavigationToolBarManager.add(new Separator());
    IAction navBackward = new Action("Back") {
      @Override
      public void run() {
        if (fBrowser != null && fBrowser.getControl().isBackEnabled())
          fBrowser.back();
        else if (fInput.getContext() != null)
          openContext(fInput.getContext());
      }

      @Override
      public boolean isEnabled() {
        return (fInput.getContext() != null) || (fBrowser != null && fBrowser.getControl().isBackEnabled());
      }
    };
    navBackward.setId(BACK_ACTION);
    navBackward.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/backward.gif")); //$NON-NLS-1$
    navBackward.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/backward.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navBackward);

    /* Navigate Forward */
    IAction navForward = new Action("Forward") {
      @Override
      public void run() {
        fBrowser.forward();
      }

      @Override
      public boolean isEnabled() {
        return fBrowser != null && fBrowser.getControl().isForwardEnabled();
      }
    };
    navForward.setId(FORWARD_ACTION);
    navForward.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/forward.gif")); //$NON-NLS-1$
    navForward.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/forward.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navForward);

    /* Stop */
    IAction stopNav = new Action("Stop") {
      @Override
      public void run() {
        fBrowser.getControl().stop();
        setBusy(false);
      }
    };
    stopNav.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/cancel.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(stopNav);

    /* Reload */
    IAction reload = new Action("Reload") {
      @Override
      public void run() {
        fBrowser.getControl().refresh();
        setBusy(true);
      }
    };
    reload.setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/reload.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(reload);

    /* Home */
    IAction navHome = new Action("Home") {
      @Override
      public void run() {
        if (fInput.getUrl() != null)
          fBrowser.setUrl(fInput.getUrl());
      }
    };
    navHome.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/home.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navHome);

    /* Share */
    fNavigationToolBarManager.add(new Separator());
    IAction shareLink = new Action("Share Link", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {
        getMenuCreator().getMenu(fNavigationToolBarManager.getControl()).setVisible(true);
      }
    };
    fNavigationToolBarManager.add(shareLink);
    shareLink.setImageDescriptor(OwlUI.SHARE);
    shareLink.setMenuCreator(new IMenuCreator() {
      public Menu getMenu(Menu parent) {
        return null;
      }

      public Menu getMenu(Control parent) {
        MenuManager shareMenu = new MenuManager();

        String url = fBrowser.getControl().getUrl();
        final IStructuredSelection selection = URIUtils.ABOUT_BLANK.equals(url) ? StructuredSelection.EMPTY : new StructuredSelection(url);

        List<ShareProvider> providers = Controller.getDefault().getShareProviders();
        for (final ShareProvider provider : providers) {
          if (provider.isEnabled()) {
            shareMenu.add(new Action(provider.getName()) {
              @Override
              public void run() {
                if (SendLinkAction.ID.equals(provider.getId())) {
                  IActionDelegate action = new SendLinkAction();
                  action.selectionChanged(null, selection);
                  action.run(null);
                } else {
                  Object obj = selection.getFirstElement();
                  if (StringUtils.isSet((String) obj) && !URIUtils.ABOUT_BLANK.equals(obj)) {
                    String shareLink = provider.toShareUrl((String) obj, null);
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

        /* Configure Providers */
        shareMenu.add(new Separator());
        shareMenu.add(new Action("&Configure...") {
          @Override
          public void run() {
            new ShareProvidersListDialog(fBrowser.getControl().getShell()).open();
          };
        });

        return shareMenu.createContextMenu(parent);
      }

      public void dispose() {}
    });

    fNavigationToolBarManager.createControl(parent);
  }

  private void openContext(WebBrowserContext context) {
    NewsReference newsReference = context.getNewsReference();
    INewsMark newsMark = context.getNewsMark();

    PerformAfterInputSet perform = null;
    if (newsReference != null)
      perform = PerformAfterInputSet.selectNews(newsReference);

    if (newsMark != null)
      OwlUI.openInFeedView(fEditorSite.getPage(), new StructuredSelection(newsMark), true, perform);
  }

  private void createLocationInput(Composite parent) {
    if (Application.IS_WINDOWS || Application.IS_LINUX)
      fLocationInput = new Text(parent, SWT.BORDER | SWT.SINGLE | SWT.SEARCH);
    else
      fLocationInput = new Text(parent, SWT.BORDER | SWT.SINGLE);
    fLocationInput.setMessage("Enter a website or search phrase");
    fLocationInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    if (fInput.getUrl() != null)
      fLocationInput.setText(fInput.getUrl());
    fLocationInput.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        if (StringUtils.isSet(fLocationInput.getText())) {
          String link = URIUtils.getLink(fLocationInput.getText());
          fBrowser.setUrl(link);
          if (fInput != null)
            fInput.setCurrentUrl(link);
          fBrowser.getControl().setFocus();
        }
      }
    });

    /* Select All on Mouse Up */
    fLocationInput.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseUp(MouseEvent e) {
        if (fLocationSelectAllOnce && fLocationInput.getSelectionCount() == 0)
          fLocationInput.selectAll();

        fLocationSelectAllOnce = false;
      }
    });

    /* Register this Input Field to Context Service */
    Controller.getDefault().getContextService().registerInputField(fLocationInput);

    fLocationInput.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(true);
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(true);
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(true);
      }

      public void focusLost(FocusEvent e) {
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
        fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
        fLocationSelectAllOnce = true;
      }
    });
  }

  private void createBrowser(Composite parent) {
    fBrowser = new CBrowser(parent, SWT.NONE);
    if (fInput.getUrl() != null)
      fBrowser.setUrl(fInput.getUrl());
    if (StringUtils.isSet(fInput.getUrl()) && !URIUtils.ABOUT_BLANK.equals(fInput.getUrl()))
      setBusy(true);
    fBrowser.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
  }

  private void hookListeners() {

    /* Title Listener */
    fBrowser.getControl().addTitleListener(new TitleListener() {
      public void changed(TitleEvent event) {
        if (URIUtils.ABOUT_BLANK.equals(event.title))
          setPartName("Blank Page");
        else
          setPartName(event.title);

        OwlUI.updateWindowTitle(getPartName());
      }
    });

    /* Status Listener */
    fBrowser.getControl().addStatusTextListener(new StatusTextListener() {
      public void changed(StatusTextEvent event) {

        /* Don't show Status for the Handler Protocol */
        if (event.text != null && !event.text.contains(ILinkHandler.HANDLER_PROTOCOL))
          fEditorSite.getActionBars().getStatusLineManager().setMessage(event.text);
      }
    });

    /* Location Listener */
    fBrowser.getControl().addLocationListener(new LocationAdapter() {
      @Override
      public void changed(LocationEvent event) {
        fNavigationToolBarManager.find(BACK_ACTION).update(IAction.ENABLED);
        fNavigationToolBarManager.find(FORWARD_ACTION).update(IAction.ENABLED);
        setBusy(false);
      }

      @Override
      public void changing(LocationEvent event) {
        setBusy(true);
      }
    });

    /* Progress Listener */
    fBrowser.getControl().addProgressListener(new ProgressListener() {
      public void changed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
        }
      }

      /* Reset progress bar on completion */
      public void completed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
          else if (StringUtils.isSet(url)) {
            fLocationInput.setText(URIUtils.ABOUT_BLANK.equals(url) ? "" : url); //$NON-NLS-1$
            if (fInput != null)
              fInput.setCurrentUrl(url);
          }
        }
      }
    });
  }

  @SuppressWarnings("restriction")
  private void setBusy(boolean busy) {
    if (fCreated && getSite() instanceof org.eclipse.ui.internal.PartSite)
      ((org.eclipse.ui.internal.PartSite) getSite()).getPane().setBusy(busy);
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
    if (!fCreated || Controller.getDefault().isShuttingDown())
      return;

    if (URIUtils.ABOUT_BLANK.equals(fBrowser.getControl().getUrl()))
      return;

    /* Ask user for File */
    FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
    dialog.setOverwrite(true);
    dialog.setFilterExtensions(new String[] { ".html" });
    dialog.setFileName("site.html");

    String fileName = dialog.open();
    if (fileName == null)
      return;

    StringBuilder content = new StringBuilder();
    content.append(fBrowser.getControl().getText());
    if (content.length() == 0)
      return;

    /* Write into File */
    OutputStreamWriter writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
      writer.write(content.toString());
      writer.close();
    } catch (IOException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    }
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    if (fBrowser != null && !fBrowser.getControl().isDisposed()) {
      String url = fInput.getUrl();
      if (URIUtils.ABOUT_BLANK.equals(url))
        fLocationInput.setFocus();
      else
        fBrowser.getControl().setFocus();
    }
  }
}