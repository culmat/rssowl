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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IConnectionService;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.dialogs.ConfirmDeleteDialog;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Preferences page to manage stored credentials for bookmarks.
 *
 * @author bpasero
 */
public class CredentialsPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
  private IConnectionService fConService = Owl.getConnectionService();
  private TableViewer fViewer;
  private Button fRemoveAll;
  private Button fRemoveSelected;

  /* Model used in the Viewer */
  private static class CredentialsModelData {
    private URI fNormalizedLink;
    private String fRealm;
    private String fUsername;

    CredentialsModelData(String username, URI normalizedLink, String realm) {
      fUsername = username;
      fNormalizedLink = normalizedLink;
      fRealm = realm;
    }

    URI getNormalizedLink() {
      return fNormalizedLink;
    }

    String getRealm() {
      return fRealm;
    }

    String getUsername() {
      return fUsername;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fNormalizedLink == null) ? 0 : fNormalizedLink.hashCode());
      result = prime * result + ((fRealm == null) ? 0 : fRealm.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;

      if (obj == null)
        return false;

      if (getClass() != obj.getClass())
        return false;

      CredentialsModelData other = (CredentialsModelData) obj;
      if (fNormalizedLink == null) {
        if (other.fNormalizedLink != null)
          return false;
      } else if (!fNormalizedLink.equals(other.fNormalizedLink))
        return false;

      if (fRealm == null) {
        if (other.fRealm != null)
          return false;
      } else if (!fRealm.equals(other.fRealm))
        return false;

      return true;
    }
  }

  /** Leave for reflection */
  public CredentialsPreferencesPage() {
    noDefaultAndApplyButton();
  }

  /**
   * @param title
   */
  public CredentialsPreferencesPage(String title) {
    super(title);
    noDefaultAndApplyButton();
  }

  /**
   * @param title
   * @param image
   */
  public CredentialsPreferencesPage(String title, ImageDescriptor image) {
    super(title, image);
    noDefaultAndApplyButton();
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {}

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    /* Label */
    Label infoLabel = new Label(container, SWT.NONE);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    infoLabel.setText("RSSOwl has saved login information for the following sites and realms:");

    /* Viewer to display Passwords */
    int style = SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER;

    CTable customTable = new CTable(container, style);
    customTable.getControl().setHeaderVisible(true);

    fViewer = new TableViewer(customTable.getControl());
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    fViewer.getControl().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

    /* Create Columns */
    TableViewerColumn col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 45), "Site", null, false, true);
    col.getColumn().setMoveable(false);

    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 30), "Realm", null, false, true);
    col.getColumn().setMoveable(false);

    col = new TableViewerColumn(fViewer, SWT.LEFT);
    customTable.manageColumn(col.getColumn(), new CColumnLayoutData(CColumnLayoutData.Size.FILL, 25), "Username", null, false, true);
    col.getColumn().setMoveable(false);

    /* Content Provider */
    fViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        Set<CredentialsModelData> credentials = loadCredentials();
        return credentials.toArray();
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fViewer.setLabelProvider(new CellLabelProvider() {
      @SuppressWarnings("unchecked")
      @Override
      public void update(ViewerCell cell) {
        CredentialsModelData data = (CredentialsModelData) cell.getElement();

        switch (cell.getColumnIndex()) {
          case 0:
            cell.setText(data.getNormalizedLink().toString());
            break;

          case 1:
            cell.setText(data.getRealm());
            break;

          case 2:
            cell.setText(data.getUsername());
            break;
        }
      }
    });

    /* Sorter */
    fViewer.setSorter(new ViewerSorter() {
      @SuppressWarnings("unchecked")
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        CredentialsModelData data1 = (CredentialsModelData) e1;
        CredentialsModelData data2 = (CredentialsModelData) e2;

        return data1.getNormalizedLink().toString().compareTo(data2.getNormalizedLink().toString());
      }
    });

    /* Set Dummy Input */
    fViewer.setInput(new Object());

    /* Offer Buttons to remove Credentials */
    fRemoveSelected = new Button(container, SWT.PUSH);
    fRemoveSelected.setText("&Remove");
    fRemoveSelected.setEnabled(false);
    fRemoveSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onRemove();
      }
    });

    fRemoveAll = new Button(container, SWT.PUSH);
    fRemoveAll.setText("Remove &All");
    fRemoveAll.setEnabled(fViewer.getTable().getItemCount() > 0);
    fRemoveAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onRemoveAll();
      }
    });

    setButtonLayoutData(fRemoveSelected);
    setButtonLayoutData(fRemoveAll);
    ((GridData) fRemoveAll.getLayoutData()).grabExcessHorizontalSpace = false;
    ((GridData) fRemoveAll.getLayoutData()).horizontalAlignment = SWT.BEGINNING;

    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        fRemoveSelected.setEnabled(!fViewer.getSelection().isEmpty());
      }
    });

    return container;
  }

  @SuppressWarnings("unchecked")
  private void onRemove() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    List<?> credentialsToRemove = selection.toList();
    for (Object obj : credentialsToRemove) {
      CredentialsModelData data = (CredentialsModelData) obj;
      remove(data);
    }

    /* Update in UI */
    fViewer.refresh();
    fRemoveSelected.setEnabled(!fViewer.getSelection().isEmpty());
    fRemoveAll.setEnabled(fViewer.getTable().getItemCount() > 0);
  }

  private void onRemoveAll() {

    /* Ask for Confirmation first */
    ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(getShell(), "Confirm Remove", "This action can not be undone", "Are you sure you want to remove all stored passwords?", null);
    if (dialog.open() != IDialogConstants.OK_ID)
      return;

    Set<CredentialsModelData> credentials = loadCredentials();
    for (CredentialsModelData data : credentials) {
      remove(data);
    }

    /* Update in UI */
    fViewer.refresh();
    fRemoveSelected.setEnabled(!fViewer.getSelection().isEmpty());
    fRemoveAll.setEnabled(fViewer.getTable().getItemCount() > 0);
  }

  private void remove(CredentialsModelData data) {

    /* Remove normalized link and realm */
    ICredentialsProvider provider = fConService.getCredentialsProvider(data.getNormalizedLink());
    if (provider != null) {
      try {
        provider.deleteAuthCredentials(data.getNormalizedLink(), data.getRealm());
      } catch (CredentialsException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    /* Remove all other stored Credentials matching normalized link and realm */
    Collection<IBookMark> bookmarks = DynamicDAO.loadAll(IBookMark.class);
    for (IBookMark bookmark : bookmarks) {
      String realm = (String) bookmark.getProperty(Controller.BM_REALM_PROPERTY);

      URI feedLink = bookmark.getFeedLinkReference().getLink();
      URI normalizedLink = URIUtils.normalizeUri(feedLink, true);

      /*
       * If realm is null, then this bookmark successfully loaded due to another bookmark
       * that the user successfully authenticated to. If the realm is not null, then we
       * have to compare the realm to ensure that no credentials from the same host but
       * a different realm gets removed.
       */
      if ((realm == null || realm.equals(data.getRealm())) && normalizedLink.equals(data.getNormalizedLink())) {
        provider = fConService.getCredentialsProvider(feedLink);
        if (provider != null) {
          try {
            provider.deleteAuthCredentials(feedLink, null); //Null as per contract in DefaultProtocolHandler
            bookmark.removeProperty(Controller.BM_REALM_PROPERTY);
          } catch (CredentialsException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      }
    }
  }

  private Set<CredentialsModelData> loadCredentials() {
    Set<CredentialsModelData> credentials = new HashSet<CredentialsModelData>();

    Collection<IBookMark> bookmarks = DynamicDAO.loadAll(IBookMark.class);
    for (IBookMark bookmark : bookmarks) {
      String realm = (String) bookmark.getProperty(Controller.BM_REALM_PROPERTY);

      /* Only support sites with realms */
      if (!StringUtils.isSet(realm))
        continue;

      URI feedLink = bookmark.getFeedLinkReference().getLink();
      URI normalizedLink = URIUtils.normalizeUri(feedLink, true);

      try {
        ICredentials authCredentials = fConService.getAuthCredentials(normalizedLink, realm);
        if (authCredentials != null) {
          CredentialsModelData data = new CredentialsModelData(authCredentials.getUsername(), normalizedLink, realm);
          credentials.add(data);
        }
      } catch (CredentialsException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return credentials;
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }
}