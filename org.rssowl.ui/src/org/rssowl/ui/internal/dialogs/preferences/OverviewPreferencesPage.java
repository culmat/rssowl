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

package org.rssowl.ui.internal.dialogs.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Overview preferences page with links to other pages.
 *
 * @author bpasero
 */
public class OverviewPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
  private LocalResourceManager fResources;

  /** ID of this Preference Page */
  public static final String ID = "org.eclipse.ui.preferencePages.Workbench";

  /** Leave for reflection */
  public OverviewPreferencesPage() {
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    final IWorkbenchPreferenceContainer preferences = (IWorkbenchPreferenceContainer) getContainer();

    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    container.setLayout(layout);
    container.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    container.setFont(parent.getFont());

    Label titleLabel = new Label(container, SWT.None);
    titleLabel.setText("The following links allow to open the related preferences page:");
    titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

    List<String> ids = new ArrayList<String>();
    List<Image> images = new ArrayList<Image>();
    List<String> labels = new ArrayList<String>();

    Composite linkContainer = new Composite(container, SWT.NONE);
    linkContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    linkContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 15, 8, false));
    ((GridLayout) linkContainer.getLayout()).marginTop = 10;

    /* Feeds */
    ids.add(FeedsPreferencePage.ID);
    images.add(OwlUI.getImage(fResources, OwlUI.BOOKMARK));
    labels.add("Configure Global Options for Feeds");

    /* Browser */
    ids.add(BrowserPreferencePage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/browser.gif"));
    labels.add("Configure the Integrated Web Browser");

    /* Sharing */
    ids.add(SharingPreferencesPage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/share.gif"));
    labels.add("Share Bookmarks and News with Others");

    /* Key Bindings */
    ids.add("org.rssowl.ui.preferences.Keys");
    images.add(OwlUI.getImage(fResources, "icons/elcl16/keyspref.gif"));
    labels.add("Assign Key Bindings for Common Actions");

    /* View */
    ids.add(MiscPreferencePage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/view.gif"));
    labels.add("Change View and System Tray Settings");

    /* Colors and Fonts */
    ids.add("org.rssowl.ui.preferences.ColorsAndFonts");
    images.add(OwlUI.getImage(fResources, "icons/elcl16/colors.gif"));
    labels.add("Configure Colors && Fonts");

    /* Network */
    ids.add("org.eclipse.ui.net.NetPreferences");
    images.add(OwlUI.getImage(fResources, "icons/elcl16/network.gif"));
    labels.add("Enable Connections via Proxy Server");

    /* Notifier */
    ids.add(NotifierPreferencesPage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/notification.gif"));
    labels.add("Configure Notifications for Incoming News");

    /* Labels */
    ids.add(ManageLabelsPreferencePage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/labels.gif"));
    labels.add("Organize Labels for News");

    /* Passwords */
    ids.add(CredentialsPreferencesPage.ID);
    images.add(OwlUI.getImage(fResources, "icons/elcl16/passwords.gif"));
    labels.add("Manage Stored Passwords for Feeds");

    /* Create */
    for (int i = 0; i < ids.size(); i++) {
      final String id = ids.get(i);

      Label imgLabel = new Label(linkContainer, SWT.None);
      imgLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
      imgLabel.setImage(images.get(i));

      Link link = new Link(linkContainer, SWT.None);
      link.setText("<a>" + labels.get(i) + "</a>");
      link.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
      link.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          preferences.openPage(id, null);
        }
      });
    }

    /* Search Info Container */
    Composite infoContainer = new Composite(container, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.END, true, true, 2, 1));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) infoContainer.getLayout()).marginBottom = 10;

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif"));
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    Label infoText = new Label(infoContainer, SWT.WRAP);
    infoText.setText("Tip: Use the text field on top to search over all preferences.");
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    return container;
  }
}