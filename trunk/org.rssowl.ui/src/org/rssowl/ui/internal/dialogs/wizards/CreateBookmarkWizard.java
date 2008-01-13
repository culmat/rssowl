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

package org.rssowl.ui.internal.dialogs.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.actions.ReloadTypesAction;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * A {@link Wizard} to easily create a new {@link IBookMark}.
 *
 * @author bpasero
 */
public class CreateBookmarkWizard extends Wizard {
  private static final String HTTP = "http://";
  private static final String PROTOCOL_IDENTIFIER = "://";

  private FeedDefinitionPage fFeedDefinitionPage;
  private KeywordSubscriptionPage fKeywordPage;
  private BookmarkDefinitionPage fBookMarkDefinitionPage;
  private final IFolder fSelection;
  private final String fInitialLink;
  private final IMark fPosition;

  /**
   * @param parent
   * @param position
   * @param initialLink
   */
  public CreateBookmarkWizard(IFolder parent, IMark position, String initialLink) {
    fSelection = parent;
    fPosition = position;
    fInitialLink = initialLink;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setHelpAvailable(false);
    setWindowTitle("New Bookmark");

    /* Page 1: Enter Link or Keyword */
    fFeedDefinitionPage = new FeedDefinitionPage("Bookmark", fInitialLink);
    addPage(fFeedDefinitionPage);

    /* Page 2: Choose Provider for Keyword Subscription (optional) */
    fKeywordPage = new KeywordSubscriptionPage("Bookmark");
    addPage(fKeywordPage);

    /* Page 2: Define Name and Location */
    fBookMarkDefinitionPage = new BookmarkDefinitionPage("Bookmark", fSelection);
    addPage(fBookMarkDefinitionPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
   */
  @Override
  public IWizardPage getNextPage(IWizardPage page) {

    /* Special Treatment for Feed Definition Page */
    if (page == fFeedDefinitionPage) {

      /* Keyword Subscription */
      if (fFeedDefinitionPage.isKeywordSubscription())
        return fKeywordPage;

      return fBookMarkDefinitionPage;
    }

    return super.getNextPage(page);
  }

  void loadNameForFeed() {

    /* Keyword Subscription - Build from Search Engine */
    if (fFeedDefinitionPage.isKeywordSubscription()) {
      fBookMarkDefinitionPage.presetBookmarkName(fKeywordPage.getSelectedEngine().getLabel(fFeedDefinitionPage.getKeyword()));
    }

    /* Link Subscription - Load from Feed if requested */
    else if (fFeedDefinitionPage.loadTitleFromFeed()) {
      String linkVal = fFeedDefinitionPage.getLink();
      if (!linkVal.contains(PROTOCOL_IDENTIFIER))
        linkVal = HTTP + linkVal;

      final String linkText = linkVal;
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Please wait while loading the title from the feed...", IProgressMonitor.UNKNOWN);

          try {
            final URI link = new URI(linkText);
            String feedTitle = Owl.getConnectionService().getLabel(link);
            if (StringUtils.isSet(feedTitle))
              fBookMarkDefinitionPage.presetBookmarkName(feedTitle);
          } catch (final ConnectionException e) {

            /* Authentication Required */
            if (e instanceof AuthenticationRequiredException) {
              //TODO Rather show another Page for credentials then!
            }
          } catch (URISyntaxException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      };

      /* Perform Runnable in separate Thread and show progress */
      try {
        getContainer().run(true, false, runnable);
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {
    if (getContainer().getCurrentPage() != fBookMarkDefinitionPage)
      return false;

    return super.canFinish();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {
    try {
      internalPerformFinish();
    } catch (URISyntaxException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    /* Remember Settings */
    Owl.getPreferenceService().getGlobalScope().putBoolean(DefaultPreferences.BM_LOAD_TITLE_FROM_FEED, fFeedDefinitionPage.loadTitleFromFeed());

    return true;
  }

  private void internalPerformFinish() throws URISyntaxException {
    URI uriObj;
    if (fFeedDefinitionPage.isKeywordSubscription())
      uriObj = new URI(fKeywordPage.getSelectedEngine().toUrl(fFeedDefinitionPage.getKeyword()));
    else {
      String linkVal = fFeedDefinitionPage.getLink();
      if (!linkVal.contains(PROTOCOL_IDENTIFIER))
        linkVal = HTTP + linkVal;
      uriObj = new URI(linkVal);
    }

    IFeedDAO feedDAO = DynamicDAO.getDAO(IFeedDAO.class);

    /* Check if a Feed with the URL already exists */
    FeedReference feedRef = feedDAO.loadReference(uriObj);

    /* Create a new Feed then */
    if (feedRef == null) {
      IFeed feed = Owl.getModelFactory().createFeed(null, uriObj);
      feed = feedDAO.save(feed);
    }

    /* Create the BookMark */
    IFolder parent = fBookMarkDefinitionPage.getFolder();
    String title = fBookMarkDefinitionPage.getBookmarkName();

    FeedLinkReference feedLinkRef = new FeedLinkReference(uriObj);
    IBookMark bookmark = Owl.getModelFactory().createBookMark(null, parent, feedLinkRef, title, fPosition, fPosition != null ? true : null);

    /* Copy all Properties from Parent into this Mark */
    Map<String, ?> properties = parent.getProperties();

    for (Map.Entry<String, ?> property : properties.entrySet())
      bookmark.setProperty(property.getKey(), property.getValue());

    parent = DynamicDAO.save(parent);

    /* Auto-Reload added BookMark */
    for (IMark mark : parent.getMarks()) {
      if (mark.equals(bookmark)) {
        new ReloadTypesAction(new StructuredSelection(mark), getShell()).run();
        break;
      }
    }
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return true;
  }
}