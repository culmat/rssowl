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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.OpenInBrowserAction;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.net.URI;
import java.text.DateFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Information about selected Entities.
 *
 * @author bpasero
 */
public class InformationPropertyPage implements IEntityPropertyPage {
  private List<IEntity> fEntities;
  private final DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite, java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(!entities.isEmpty());
    fEntities = entities;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Status */
    createLabel(container, "Status: ", true);

    final IBookMark bm = (IBookMark) fEntities.get(0);
    String message;

    /* Error Loading */
    if (bm.isErrorLoading()) {
      message = (String) bm.getProperty(Controller.LOAD_ERROR_KEY);
      if (!StringUtils.isSet(message))
        message = "The last attempt to load this feed failed for an unknown reason.";
      else
        message = "The last attempt to load this feed failed. Reason: " + message;
    }

    /* Never Loaded */
    else if (bm.getMostRecentNewsDate() == null)
      message = "This feed was not loaded yet.";

    /* Successfully Loaded */
    else
      message = "The last attempt to load this feed was successful.";

    Label msgLabel = new Label(container, SWT.WRAP);
    msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    ((GridData) msgLabel.getLayoutData()).widthHint = 300;
    msgLabel.setText(message);

    /* Feed: Description */
    createLabel(container, "Description: ", true);

    final Label descriptionLabel = new Label(container, SWT.WRAP);
    descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
    ((GridData) descriptionLabel.getLayoutData()).widthHint = 300;

    /* Feed: Homepage */
    createLabel(container, "Homepage: ", true);

    final Link homepageLink = new Link(container, SWT.NONE);
    homepageLink.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Load from Background */
    JobRunner.runUIUpdater(new UIBackgroundJob(container) {
      private String description;
      private URI homepage;

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        IFeed feed = bm.getFeedLinkReference().resolve();
        if (feed != null) {
          description = StringUtils.stripTags(feed.getDescription(), true);
          homepage = feed.getHomepage();
        }
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {
        descriptionLabel.setText(StringUtils.isSet(description) ? description : "None");
        homepageLink.setText(homepage != null ? "<a>" + homepage.toString() + "</a>" : "None");
        if (homepage != null) {
          homepageLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              OpenInBrowserAction action = new OpenInBrowserAction();
              action.selectionChanged(null, new StructuredSelection(homepage));
              action.run();
            }
          });
        }
        descriptionLabel.getParent().layout(true, true);
      }
    });

    /* Created */
    createLabel(container, "Created: ", true);
    createLabel(container, fDateFormat.format(bm.getCreationDate()), false);

    /* Last Visited */
    createLabel(container, "Last Visited: ", true);
    if (bm.getLastVisitDate() != null)
      createLabel(container, fDateFormat.format(bm.getLastVisitDate()), false);
    else
      createLabel(container, "Never", false);

    /* News Count */
    createLabel(container, "News Count: ", true);
    int totalCount = bm.getNewsCount(INews.State.getVisible());
    int newCount = bm.getNewsCount(EnumSet.of(INews.State.NEW));
    int unreadCount = bm.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED));

    if (totalCount == 0)
      createLabel(container, "0", false);
    else
      createLabel(container, totalCount + " (" + newCount + " new, " + unreadCount + " unread)", false);

    return container;
  }

  private void createLabel(Composite parent, String text, boolean bold) {
    Label label = new Label(parent, SWT.None);
    label.setText(text);
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
    if (bold)
      label.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {}

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {
    return true;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {}
}