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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.internal.DefaultPreferences;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.model.preferences.IPreferencesScope;
import org.rssowl.core.model.reference.FeedLinkReference;
import org.rssowl.core.model.reference.FeedReference;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.RSSOwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * General Properties.
 *
 * @author bpasero
 */
public class GeneralPropertyPage implements IEntityPropertyPage {

  /* Interval-Scopes in Seconds */
  private static final long DAY_IN_SECONDS = 24 * 60 * 60;
  private static final long HOUR_IN_SECONDS = 60 * 60;
  private static final long MINUTE_IN_SECONDS = 60;

  /* Interval-Indeces in Combo */
  private static final int MINUTES_SCOPE = 0;
  private static final int HOURS_SCOPE = 1;
  private static final int DAYS_SCOPE = 2;

  private IPropertyDialogSite fSite;
  private List<IEntity> fEntities;
  private Text fNameInput;
  private Text fFeedInput;
  private Button fOpenOnStartupCheck;
  private boolean fReloadRequired;
  private boolean fSettingsChanged;
  private boolean fIsSingleBookMark;

  /* Settings */
  private List<IPreferencesScope> fEntityPreferences;
  private boolean fPrefUpdateIntervalState;
  private long fPrefUpdateInterval;
  private boolean fPrefOpenOnStartup;
  private Spinner fReloadSpinner;
  private int fUpdateIntervalScope;
  private Button fUpdateCheck;
  private Combo fReloadCombo;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite,
   * java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(!entities.isEmpty());
    fSite = site;
    fEntities = entities;

    /* Load Entity Preferences */
    fEntityPreferences = new ArrayList<IPreferencesScope>(fEntities.size());
    for (IEntity entity : entities)
      fEntityPreferences.add(NewsModel.getDefault().getEntityScope(entity));

    /* Load initial Settings */
    loadInitialSettings();

    /* Pre-Cache the Feed if Entity is a single BookMark */
    if (fEntities.size() == 1 && fEntities.get(0) instanceof IBookMark)
      fIsSingleBookMark = true;
  }

  private void loadInitialSettings() {

    /* Take the first scope as initial values */
    IPreferencesScope firstScope = fEntityPreferences.get(0);
    fPrefUpdateIntervalState = firstScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE);
    fPrefUpdateInterval = firstScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
    fPrefOpenOnStartup = firstScope.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP);

    /* For any other scope not sharing the initial values, use the default */
    IPreferencesScope defaultScope = NewsModel.getDefault().getDefaultScope();
    for (int i = 1; i < fEntityPreferences.size(); i++) {
      IPreferencesScope otherScope = fEntityPreferences.get(i);

      if (otherScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE) != fPrefUpdateIntervalState)
        fPrefUpdateIntervalState = defaultScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE);

      if (otherScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL) != fPrefUpdateInterval)
        fPrefUpdateInterval = defaultScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);

      if (otherScope.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP) != fPrefOpenOnStartup)
        fPrefOpenOnStartup = defaultScope.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP);
    }

    fUpdateIntervalScope = getUpdateIntervalScope();
  }

  /*
   * @see org.rssowl.ui.internal.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Fields for single Selection */
    if (fEntities.size() == 1) {
      IEntity entity = fEntities.get(0);

      /* Name */
      Label nameLabel = new Label(container, SWT.None);
      nameLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
      nameLabel.setText("Name: ");

      fNameInput = new Text(container, SWT.BORDER);
      fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fNameInput.setText(getName(entity));

      /* Link */
      if (entity instanceof IBookMark) {
        Label feedLabel = new Label(container, SWT.None);
        feedLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
        feedLabel.setText("Feed: ");

        fFeedInput = new Text(container, SWT.BORDER);
        fFeedInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        fFeedInput.setText(((IBookMark) entity).getFeedLinkReference().getLink().toString());
        ((GridData) fFeedInput.getLayoutData()).widthHint = fSite.getHorizontalPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
      }
    }

    /* Separator */
    if (fEntities.size() == 1)
      new Label(container, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

    /* Other Settings */
    Composite otherSettingsContainer = new Composite(container, SWT.NONE);
    otherSettingsContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    otherSettingsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true, 2, 1));

    /* Auto-Reload */
    Composite autoReloadContainer = new Composite(otherSettingsContainer, SWT.NONE);
    autoReloadContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));
    autoReloadContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true));

    fUpdateCheck = new Button(autoReloadContainer, SWT.CHECK);
    fUpdateCheck.setText("Automatically update the " + (fIsSingleBookMark ? "feed" : "feeds") + " every ");
    fUpdateCheck.setSelection(fPrefUpdateIntervalState);
    fUpdateCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fReloadSpinner.setEnabled(fUpdateCheck.getSelection());
        fReloadCombo.setEnabled(fUpdateCheck.getSelection());
      }
    });

    fReloadSpinner = new Spinner(autoReloadContainer, SWT.BORDER);
    fReloadSpinner.setMinimum(1);
    fReloadSpinner.setMaximum(999);
    fReloadSpinner.setEnabled(fPrefUpdateIntervalState);

    if (fUpdateIntervalScope == MINUTES_SCOPE)
      fReloadSpinner.setSelection((int) (fPrefUpdateInterval / MINUTE_IN_SECONDS));
    else if (fUpdateIntervalScope == HOURS_SCOPE)
      fReloadSpinner.setSelection((int) (fPrefUpdateInterval / HOUR_IN_SECONDS));
    else if (fUpdateIntervalScope == DAYS_SCOPE)
      fReloadSpinner.setSelection((int) (fPrefUpdateInterval / DAY_IN_SECONDS));

    fReloadCombo = new Combo(autoReloadContainer, SWT.READ_ONLY);
    fReloadCombo.add("Minutes");
    fReloadCombo.add("Hours");
    fReloadCombo.add("Days");
    fReloadCombo.select(fUpdateIntervalScope);
    fReloadCombo.setEnabled(fPrefUpdateIntervalState);

    /* Open on Startup */
    fOpenOnStartupCheck = new Button(otherSettingsContainer, SWT.CHECK);
    fOpenOnStartupCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fOpenOnStartupCheck.setText("Display the " + (fIsSingleBookMark ? "feed" : "feeds") + " on start-up");
    fOpenOnStartupCheck.setSelection(fPrefOpenOnStartup);

    return container;
  }

  private int getUpdateIntervalScope() {
    if (fPrefUpdateInterval % DAY_IN_SECONDS == 0)
      return DAYS_SCOPE;

    if (fPrefUpdateInterval % HOUR_IN_SECONDS == 0)
      return HOURS_SCOPE;

    return MINUTES_SCOPE;
  }

  private String getName(IEntity entity) {
    if (entity instanceof IFolder)
      return ((IFolder) entity).getName();

    if (entity instanceof IMark)
      return ((IMark) entity).getName();

    return "";
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {

    /* First handle single-entity Preferences */
    if (fEntities.size() == 1) {
      boolean proceed = internalPerformSingle(entitiesToSave);

      /* Check for abortion due to error here */
      if (!proceed)
        return false;
    }

    /* Now handle single/multi entity Preferences */
    for (IPreferencesScope scope : fEntityPreferences) {
      if (updatePreferences(scope)) {
        IEntity entityToSave = fEntities.get(fEntityPreferences.indexOf(scope));
        entitiesToSave.add(entityToSave);
        fSettingsChanged = true;
      }
    }

    /* Update changes to all Childs as well if Folder */
    for (IEntity entity : fEntities) {
      if (fSettingsChanged && entity instanceof IFolder)
        updateChildPreferences((IFolder) entity);
    }

    return true;
  }

  /* Perform Update on single Entity */
  private boolean internalPerformSingle(Set<IEntity> entitiesToSave) {
    IEntity entity = fEntities.get(0);

    /* Require a Name */
    if (fNameInput.getText().length() == 0) {
      fSite.select(this);
      fNameInput.setFocus();

      if (entity instanceof IFolder)
        fSite.setMessage("Please enter a name for the folder.", IPropertyDialogSite.MessageType.ERROR);
      else if (entity instanceof IBookMark)
        fSite.setMessage("Please enter a name for the bookmark.", IPropertyDialogSite.MessageType.ERROR);

      return false;
    }

    /* Update Folder */
    if (entity instanceof IFolder) {
      IFolder folder = (IFolder) entity;

      /* Name */
      if (!folder.getName().equals(fNameInput.getText())) {
        folder.setName(fNameInput.getText());
        entitiesToSave.add(folder);
      }
    }

    /* Update BookMark */
    else if (entity instanceof IBookMark) {
      IBookMark bookmark = (IBookMark) entity;

      /* Check for changed Name */
      if (!bookmark.getName().equals(fNameInput.getText())) {
        bookmark.setName(fNameInput.getText());
        entitiesToSave.add(bookmark);
      }

      /* Append "http" to Link if missing */
      String uriAsString = fFeedInput.getText();
      if (URIUtils.looksLikeLink(uriAsString)) {
        if (!uriAsString.contains("://"))
          uriAsString = "http://" + uriAsString;
      }

      /* Check for changed Feed */
      if (!bookmark.getFeedLinkReference().getLink().toString().equals(uriAsString)) {
        try {
          IApplicationLayer applicationLayer = NewsModel.getDefault().getPersistenceLayer().getApplicationLayer();

          /* Create URL */
          URI newFeedLink = new URI(uriAsString.trim());
          fReloadRequired = true;

          /* Try to load this Feed from the DB (might be existing already) */
          FeedReference feedRef = applicationLayer.loadFeedReference(newFeedLink);

          /* This is a new Feed, so create it! */
          if (feedRef == null) {
            IFeed feed = NewsModel.getDefault().getTypesFactory().createFeed(null, newFeedLink);
            feed = NewsModel.getDefault().getPersistenceLayer().getModelDAO().saveFeed(feed);
          }

          /* Remember the old Reference */
          FeedLinkReference oldFeedRef = bookmark.getFeedLinkReference();

          /* Apply the new Reference */
          bookmark.setFeedLinkReference(new FeedLinkReference(newFeedLink));
          entitiesToSave.add(bookmark);

          /* Check if the old reference can be deleted now */
          if (applicationLayer.loadBookMarks(oldFeedRef).size() == 1)
            NewsModel.getDefault().getPersistenceLayer().getModelDAO().deleteFeed(oldFeedRef);

          /* Delete the Favicon since the feed has changed */
          RSSOwlUI.deleteImage(bookmark.getId());
        }

        /* Supplied Feed Link not valid */
        catch (URISyntaxException e) {
          fSite.select(this);
          fFeedInput.selectAll();
          fFeedInput.setFocus();
          fSite.setMessage("The feed's link is not valid (e.g. 'http://www.link.com').", IPropertyDialogSite.MessageType.ERROR);
          return false;
        }
      }
    }

    return true;
  }

  /*
   * @see org.rssowl.ui.internal.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {

    /* Reload if required */
    if (fReloadRequired && fEntities.size() == 1)
      Controller.getDefault().reloadQueued((IBookMark) fEntities.get(0), getWorkbenchShell());
  }

  private Shell getWorkbenchShell() {
    IWorkbenchWindow wWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (wWindow != null)
      return wWindow.getShell();

    return null;
  }

  private boolean updatePreferences(IPreferencesScope scope) {
    boolean changed = false;

    /* Update Interval State */
    boolean bVal = fUpdateCheck.getSelection();
    if (fPrefUpdateIntervalState != bVal) {
      scope.putBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE, bVal);
      changed = true;
    }

    /* Update Interval */
    long lVal;
    fUpdateIntervalScope = fReloadCombo.getSelectionIndex();

    if (fUpdateIntervalScope == MINUTES_SCOPE)
      lVal = fReloadSpinner.getSelection() * MINUTE_IN_SECONDS;
    else if (fUpdateIntervalScope == HOURS_SCOPE)
      lVal = fReloadSpinner.getSelection() * HOUR_IN_SECONDS;
    else
      lVal = fReloadSpinner.getSelection() * DAY_IN_SECONDS;

    if (fPrefUpdateInterval != lVal) {
      scope.putLong(DefaultPreferences.BM_UPDATE_INTERVAL, lVal);
      changed = true;
    }

    /* Open on Startup */
    bVal = fOpenOnStartupCheck.getSelection();
    if (fPrefOpenOnStartup != bVal) {
      scope.putBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP, bVal);
      changed = true;
    }

    return changed;
  }

  private void updateChildPreferences(IFolder folder) {

    /* Update changes to Child-BookMarks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        IPreferencesScope scope = NewsModel.getDefault().getEntityScope(mark);
        updatePreferences(scope);
      }
    }

    /* Update changes to Child-Folders */
    List<IFolder> folders = folder.getFolders();
    for (IFolder childFolder : folders) {
      IPreferencesScope scope = NewsModel.getDefault().getEntityScope(childFolder);
      updatePreferences(scope);

      /* Recursively Proceed */
      updateChildPreferences(childFolder);
    }
  }
}