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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.NewsFilterDialog;
import org.rssowl.ui.internal.dialogs.NewsFiltersListDialog;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An action to create a {@link ISearchFilter} from a selected {@link INews} or
 * {@link IFolderChild}.
 *
 * @author bpasero
 */
public class CreateFilterAction implements IObjectActionDelegate {
  private IStructuredSelection fSelection;

  /*
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    Shell shell = OwlUI.getActiveShell();
    if (shell != null && !fSelection.isEmpty()) {
      ISearch presetSearch = Owl.getModelFactory().createSearch(null);
      fillSearchConditions(presetSearch);
      presetSearch.setMatchAllConditions(true);

      NewsFilterDialog dialog = new NewsFilterDialog(shell, presetSearch);
      Collection<ISearchFilter> existingFilters = DynamicDAO.loadAll(ISearchFilter.class);
      if (existingFilters != null && !existingFilters.isEmpty())
        dialog.setFilterPosition(existingFilters.size());

      if (dialog.open() == IDialogConstants.OK_ID) {
        NewsFiltersListDialog filterListDialog = NewsFiltersListDialog.getVisibleInstance();
        if (filterListDialog == null) {
          filterListDialog = new NewsFiltersListDialog(shell);
          filterListDialog.setSelection(dialog.getFilter());
          filterListDialog.open();
        } else {
          filterListDialog.refresh();
          filterListDialog.setSelection(dialog.getFilter());
          filterListDialog.getShell().forceActive();
        }
      }
    }
  }

  private void fillSearchConditions(ISearch presetSearch) {
    List<?> selection = fSelection.toList();
    if (selection.get(0) instanceof INews)
      fillSearchConditionsForNews(selection, presetSearch);
    else if (selection.get(0) instanceof IFolderChild)
      fillSearchConditionsForFolderChild(selection, presetSearch);
  }

  @SuppressWarnings("unchecked")
  private void fillSearchConditionsForFolderChild(List<?> selection, ISearch presetSearch) {
    ISearchField locationField = Owl.getModelFactory().createSearchField(INews.LOCATION, INews.class.getName());
    Long[][] value = ModelUtils.toPrimitive((List<IFolderChild>) selection);

    ISearchCondition condition = Owl.getModelFactory().createSearchCondition(locationField, SearchSpecifier.IS, value);
    presetSearch.addSearchCondition(condition);
  }

  private void fillSearchConditionsForNews(List<?> selection, ISearch presetSearch) {
    INews news = (INews) selection.get(0);
    IModelFactory factory = Owl.getModelFactory();

    /* Location */
    {
      ISearchField locationField = factory.createSearchField(INews.LOCATION, INews.class.getName());

      FeedLinkReference feedReference = news.getFeedReference();
      Collection<IBookMark> bookmarks = DynamicDAO.getDAO(IBookMarkDAO.class).loadAll(feedReference);

      Long[][] value = ModelUtils.toPrimitive(new ArrayList<IFolderChild>(bookmarks));

      ISearchCondition condition = factory.createSearchCondition(locationField, SearchSpecifier.IS, value);
      presetSearch.addSearchCondition(condition);
    }

    /* Category (only first one) */
    List<ICategory> categories = news.getCategories();
    if (!categories.isEmpty()) {
      ICategory category = categories.get(0);
      if (StringUtils.isSet(category.getName())) {
        ISearchField categoryField = factory.createSearchField(INews.CATEGORIES, INews.class.getName());

        ISearchCondition condition = factory.createSearchCondition(categoryField, SearchSpecifier.IS, category.getName());
        presetSearch.addSearchCondition(condition);
      }
    }

    /* Author */
    IPerson author = news.getAuthor();
    if (author != null) {
      String value = author.getName();
      if (!StringUtils.isSet(value) && author.getEmail() != null)
        value = author.getEmail().toString();

      if (StringUtils.isSet(value)) {
        ISearchField authorField = factory.createSearchField(INews.AUTHOR, INews.class.getName());

        ISearchCondition condition = factory.createSearchCondition(authorField, SearchSpecifier.CONTAINS_ALL, value);
        presetSearch.addSearchCondition(condition);
      }
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }
}