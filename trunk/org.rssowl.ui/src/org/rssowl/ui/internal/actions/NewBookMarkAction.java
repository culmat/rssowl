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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.FolderChooser;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * TODO This is a rough-Action which is not polished or optimized and only for
 * developers purposes!
 *
 * @author bpasero
 */
public class NewBookMarkAction implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {
  private Shell fShell;
  private IFolder fParent;
  private String fPreSetLink;

  private static class NewBookMarkDialog extends TitleAreaDialog {
    private String fInitialLinkValue;
    private String fInitialNameValue;
    private Text fLinkInput;
    private Text fNameInput;
    private ResourceManager fResources;
    private String fLink;
    private String fName;
    private IFolder fFolder;
    private FolderChooser fFolderChooser;

    NewBookMarkDialog(Shell shell, IFolder folder, String initialLinkValue) {
      super(shell);
      fFolder = folder;
      fInitialLinkValue = initialLinkValue;
      fResources = new LocalResourceManager(JFaceResources.getResources());

      if (fInitialLinkValue != null) {
        try {
          URI uri = new URI(fInitialLinkValue);
          fInitialNameValue = uri.getHost();
        } catch (URISyntaxException e) {
          /* Ignore */
        }
      }
    }

    @Override
    public void create() {
      super.create();
      getButton(IDialogConstants.OK_ID).setEnabled(fInitialNameValue != null);
    }

    @Override
    protected void okPressed() {
      fLink = fLinkInput.getText();
      fName = fNameInput.getText();
      super.okPressed();
    }

    @Override
    public boolean close() {
      boolean res = super.close();
      fResources.dispose();
      return res;
    }

    @Override
    protected void configureShell(Shell newShell) {
      newShell.setText("New Bookmark");
      super.configureShell(newShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {

      /* Separator */
      new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      /* Title Image */
      setTitleImage(OwlUI.getImage(fResources, "icons/obj16/bkmrk_wiz.gif"));

      /* Title Message */
      setMessage("Please enter the name and link of the bookmark.", IMessageProvider.INFORMATION);

      Composite container = new Composite(parent, SWT.NONE);
      container.setLayout(LayoutUtils.createGridLayout(2, 5, 5));
      container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      Label l2 = new Label(container, SWT.NONE);
      l2.setText("Link: ");

      fLinkInput = new Text(container, SWT.SINGLE | SWT.BORDER);
      fLinkInput.setText(fInitialLinkValue != null ? fInitialLinkValue : "http://");
      fLinkInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fLinkInput.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          validateInput();
        }
      });

      Label l1 = new Label(container, SWT.NONE);
      l1.setText("Name: ");

      Composite nameContainer = new Composite(container, SWT.BORDER);
      nameContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      nameContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
      nameContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

      fNameInput = new Text(nameContainer, SWT.SINGLE);
      fNameInput.setText(fInitialNameValue != null ? fInitialNameValue : "");
      fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
      fNameInput.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          validateInput();
        }
      });

      ToolBar grabTitleBar = new ToolBar(nameContainer, SWT.FLAT);
      grabTitleBar.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

      ToolItem grabTitleItem = new ToolItem(grabTitleBar, SWT.PUSH);
      grabTitleItem.setImage(OwlUI.getImage(fResources, "icons/etool16/refresh.gif"));
      grabTitleItem.setToolTipText("Load name from feed");
      grabTitleItem.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          onGrabTitle();
        }
      });

      Label l3 = new Label(container, SWT.NONE);
      l3.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      l3.setText("Location: ");

      /* Folder Chooser */
      fFolderChooser = new FolderChooser(container, fFolder, SWT.BORDER);
      fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
      fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

      return container;
    }

    private void onGrabTitle() {
      if (StringUtils.isSet(fLinkInput.getText())) {
        try {
          URI link = new URI(fLinkInput.getText());
          String label = Owl.getConnectionService().getLabel(link);
          if (StringUtils.isSet(label))
            fNameInput.setText(label);
        } catch (ConnectionException e) {
          /* Ignore */
        } catch (URISyntaxException e) {
          /* Ignore */
        }
      }
    }

    @Override
    protected void initializeBounds() {
      super.initializeBounds();
      Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT);
      getShell().setSize(bestSize.x, bestSize.y);
      LayoutUtils.positionShell(getShell(), false);
    }

    private void validateInput() {
      boolean valid = fLinkInput.getText().length() > 0 && fNameInput.getText().length() > 0;
      Control button = getButton(IDialogConstants.OK_ID);
      button.setEnabled(valid);
    }

    IFolder getFolder() {
      return fFolderChooser.getFolder();
    }
  }

  /** Keep for Reflection */
  public NewBookMarkAction() {
    this(null, null);
  }

  /**
   * @param shell
   * @param parent
   */
  public NewBookMarkAction(Shell shell, IFolder parent) {
    this(shell, parent, null);
  }

  /**
   * @param shell
   * @param parent
   * @param preSetLink
   */
  public NewBookMarkAction(Shell shell, IFolder parent, String preSetLink) {
    fShell = shell;
    fParent = parent;
    fPreSetLink = preSetLink;
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    fShell = window.getShell();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    try {
      internalRun();
    } catch (URISyntaxException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }
  }

  private void internalRun() throws URISyntaxException, PersistenceException {
    String initial = null;
    if (StringUtils.isSet(fPreSetLink))
      initial = fPreSetLink;
    else {
      Clipboard cb = new Clipboard(fShell.getDisplay());
      TextTransfer transfer = TextTransfer.getInstance();
      String data = (String) cb.getContents(transfer);
      data = (data != null) ? data.trim() : fPreSetLink;
      cb.dispose();
      initial = "http://";

      if (URIUtils.looksLikeLink(data)) {
        if (!data.contains("://"))
          data = initial + data;
        initial = data;
      }
    }

    /* Get the parent Folder */
    IFolder parent = getParent();

    /* Show Dialog */
    NewBookMarkDialog dialog = new NewBookMarkDialog(fShell, parent, initial);
    if (dialog.open() == Window.OK) {
      String title = dialog.fName;
      parent = dialog.getFolder();

      URI uriObj = new URI(dialog.fLink.trim());

      IFeedDAO feedDAO = DynamicDAO.getDAO(IFeedDAO.class);

      /* Check if a Feed with the URL already exists */
      FeedReference feedRef = feedDAO.loadReference(uriObj);

      /* Create a new Feed then */
      if (feedRef == null) {
        IFeed feed = Owl.getModelFactory().createFeed(null, uriObj);
        feed = feedDAO.save(feed);
      }

      /* Create the BookMark */
      FeedLinkReference feedLinkRef = new FeedLinkReference(uriObj);
      IBookMark bookmark = Owl.getModelFactory().createBookMark(null, parent, feedLinkRef, title);

      /* Copy all Properties from Parent into this Mark */
      Map<String, ?> properties = parent.getProperties();

      for (Map.Entry<String, ?> property : properties.entrySet())
        bookmark.setProperty(property.getKey(), property.getValue());

      parent = DynamicDAO.save(parent);

      /* Auto-Reload added BookMark */
      for (IMark mark : parent.getMarks()) {
        if (mark.equals(bookmark)) {
          new ReloadTypesAction(new StructuredSelection(mark), fShell).run();
          break;
        }
      }
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {

    /* Delete the old Selection */
    fParent = null;

    /* Check Selection */
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structSel = (IStructuredSelection) selection;
      if (!structSel.isEmpty()) {
        Object firstElement = structSel.getFirstElement();
        if (firstElement instanceof IFolder)
          fParent = (IFolder) firstElement;
        else if (firstElement instanceof IMark)
          fParent = ((IMark) firstElement).getFolder();
      }
    }
  }

  /*
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   * org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    fShell = targetPart.getSite().getShell();
  }

  private IFolder getParent() throws PersistenceException {
    Long selectedRootFolderID = DynamicDAO.getDAO(IPreferenceDAO.class).load(BookMarkExplorer.PREF_SELECTED_BOOKMARK_SET).getLong();

    /* Check if available Parent is still valid */
    if (fParent != null) {
      if (hasParent(fParent, new FolderReference(selectedRootFolderID)))
        return fParent;
    }

    /* Otherwise return visible root-folder */
    return new FolderReference(selectedRootFolderID).resolve();
  }

  private boolean hasParent(IFolder folder, FolderReference folderRef) {
    if (folder == null)
      return false;

    if (folderRef.references(folder))
      return true;

    return hasParent(folder.getParent(), folderRef);
  }
}