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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.CBrowser;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * The <code>WebBrowserView</code> is able to display a website in an embedded
 * Browser.
 * 
 * @author bpasero
 */
public class WebBrowserView extends EditorPart {

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

  /** Leave default for reflection */
  public WebBrowserView() {}

  /*
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @SuppressWarnings("unused")
  @Override
  public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    Assert.isTrue(input instanceof WebBrowserInput);
    fEditorSite = site;
    fInput = (WebBrowserInput) input;

    setSite(fEditorSite);
    setInput(fInput);

    /* Hook into Global Actions */
    createGlobalActions();
    setGlobalActions();
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

    /* Navigate Backward */
    IAction navBackward = new Action("Back") {
      @Override
      public void run() {
        fBrowser.back();
      }

      @Override
      public boolean isEnabled() {
        return fBrowser != null && fBrowser.getControl().isBackEnabled();
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
      }
    };
    stopNav.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/cancel.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(stopNav);

    /* Reload */
    IAction reload = new Action("Reload") {
      @Override
      public void run() {
        fBrowser.getControl().refresh();
      }
    };
    reload.setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/reload.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(reload);

    /* Home */
    IAction navHome = new Action("Home") {
      @Override
      public void run() {
        fBrowser.setUrl(fInput.getUrl());
      }
    };
    navHome.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/home.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navHome);

    fNavigationToolBarManager.createControl(parent);
  }

  private void createLocationInput(Composite parent) {
    fLocationInput = new Text(parent, SWT.BORDER | SWT.SINGLE);
    fLocationInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fLocationInput.setText(fInput.getUrl());
    fLocationInput.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        if (StringUtils.isSet(fLocationInput.getText())) {
          fBrowser.setUrl(fLocationInput.getText());
          fBrowser.getControl().setFocus();
        }
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
      }
    });
  }

  private void createBrowser(Composite parent) {
    fBrowser = new CBrowser(parent, SWT.NONE);
    fBrowser.setUrl(fInput.getUrl());
    fBrowser.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
  }

  private void hookListeners() {

    /* Title Listener */
    fBrowser.getControl().addTitleListener(new TitleListener() {
      public void changed(TitleEvent event) {
        setPartName(event.title);
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
      }
    });

    /* Progress Listener */
    fBrowser.getControl().addProgressListener(new ProgressListener() {
      public void changed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
          else if (StringUtils.isSet(url))
            fLocationInput.setText(URIUtils.ABOUT_BLANK.equals(url) ? "" : url); //$NON-NLS-1$
        }
      }

      /* Reset progress bar on completion */
      public void completed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
          else if (StringUtils.isSet(url))
            fLocationInput.setText(URIUtils.ABOUT_BLANK.equals(url) ? "" : url); //$NON-NLS-1$
        }
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
    return false;
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
  /* Not Supported */
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    if (fBrowser != null && !fBrowser.getControl().isDisposed())
      fBrowser.getControl().setFocus();
  }
}