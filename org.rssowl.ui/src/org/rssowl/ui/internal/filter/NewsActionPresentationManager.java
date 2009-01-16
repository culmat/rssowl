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

package org.rssowl.ui.internal.filter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.rssowl.core.INewsAction;
import org.rssowl.ui.filter.INewsActionPresentation;
import org.rssowl.ui.internal.Activator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Helper to load {@link INewsAction} and {@link INewsActionPresentation}
 * contributions.
 *
 * @author bpasero
 */
public class NewsActionPresentationManager {

  /* ID of the contributed News Actions */
  private static final String NEWS_ACTION_EXTENSION_POINT = "org.rssowl.core.NewsAction"; //$NON-NLS-1$

  /* ID of the contributed News Action Presentations */
  private static final String NEWS_ACTION_PRESENTATION_EXTENSION_POINT = "org.rssowl.ui.NewsActionPresentation"; //$NON-NLS-1$

  /* Singleton Instance */
  private static NewsActionPresentationManager fgInstance = new NewsActionPresentationManager();

  private Map<String, NewsActionDescriptor> fNewsActions = new HashMap<String, NewsActionDescriptor>();
  private Set<NewsActionDescriptor> fSortedNewsActions = new TreeSet<NewsActionDescriptor>();
  private Map<String, IConfigurationElement> fNewsActionPresentations = new HashMap<String, IConfigurationElement>();

  /**
   * @return the singleton instance.
   */
  public static NewsActionPresentationManager getInstance() {
    return fgInstance;
  }

  private NewsActionPresentationManager() {
    loadNewsActions();
    loadNewsActionPresentations();
  }

  /**
   * @param actionId the unique ID of the contributed {@link INewsAction}.
   * @return the {@link INewsActionPresentation} for the given ID or
   * <code>null</code> if none. The instance is created on every call and never
   * shared.
   */
  public INewsActionPresentation getPresentation(String actionId) {
    try {
      IConfigurationElement element = fNewsActionPresentations.get(actionId);
      if (element != null)
        return (INewsActionPresentation) element.createExecutableExtension("class");
    } catch (CoreException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    return null;
  }

  /**
   * @return a list of {@link NewsActionDescriptor} describing the contributed
   * {@link INewsAction}. The list is sorted by the sortKey as defined in the
   * contribution.
   */
  public Collection<NewsActionDescriptor> getSortedNewsActions() {
    return fSortedNewsActions;
  }

  /**
   * @param actionId the unique ID of the contributed {@link INewsAction}.
   * @return the {@link NewsActionDescriptor} for the contributed
   * {@link INewsAction}.
   */
  public NewsActionDescriptor getNewsActionDescriptor(String actionId) {
    return fNewsActions.get(actionId);
  }

  private void loadNewsActions() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(NEWS_ACTION_EXTENSION_POINT);
    for (IConfigurationElement element : elements) {
      try {
        String id = element.getAttribute("id");
        INewsAction newsAction = (INewsAction) element.createExecutableExtension("class");
        String name = element.getAttribute("name");
        String description = element.getAttribute("description");
        String sortKey = element.getAttribute("sortKey");
        String forcable = element.getAttribute("forcable");

        NewsActionDescriptor filterAction = new NewsActionDescriptor(id, newsAction, name, description, sortKey, Boolean.parseBoolean(forcable));
        fNewsActions.put(id, filterAction);
        fSortedNewsActions.add(filterAction);
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private void loadNewsActionPresentations() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(NEWS_ACTION_PRESENTATION_EXTENSION_POINT);
    for (IConfigurationElement element : elements) {
      try {
        String id = element.getAttribute("actionId");
        fNewsActionPresentations.put(id, element);
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }
}