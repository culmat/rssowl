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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.IModelDAO;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.model.reference.BookMarkReference;
import org.rssowl.core.model.reference.SearchMarkReference;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Action for Deleting a Selection of <code>ModelReferences</code>.
 *
 * @author bpasero
 */
public class DeleteTypesAction extends Action implements IObjectActionDelegate {
  private IStructuredSelection fSelection;
  private IModelDAO fModelDao;
  private IApplicationLayer fAppLayer;
  private Shell fShell;
  private boolean fConfirmed;

  /**
   * Keep default constructor for reflection.
   * <p>
   * Note: This Constructor should <em>not</em> directly be called. Use
   * <code>DeleteTypesAction(IStructuredSelection selection)</code> instead.
   * </p>
   */
  public DeleteTypesAction() {
    this(null, StructuredSelection.EMPTY);
  }

  /**
   * Creates a new Action for Deleting Types from the given Selection.
   *
   * @param shell The Shell to be used to show a confirmation dialog.
   * @param selection The Selection to Delete.
   */
  public DeleteTypesAction(Shell shell, IStructuredSelection selection) {
    fShell = shell;
    fSelection = selection;
    fModelDao = Owl.getPersistenceService().getModelDAO();
    fAppLayer = Owl.getPersistenceService().getApplicationLayer();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    if (confirmed()) {
      BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
        public void run() {
          internalRun();
        }
      });
    }
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    if (confirmed()) {
      BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(), new Runnable() {
        public void run() {
          internalRun();
        }
      });
    }
  }

  private boolean confirmed() {
    StringBuilder message = new StringBuilder("Are you sure you want to delete ");
    List< ? > elements = fSelection.toList();

    /* One Element */
    if (elements.size() == 1) {
      Object element = elements.get(0);
      if (element instanceof IFolder)
        message.append("the folder '").append(((IFolder) element).getName()).append("'?");
      else if (element instanceof IBookMark)
        message.append("the bookmark '").append(((IMark) element).getName()).append("'?");
      else if (element instanceof ISearchMark)
        message.append("the saved search '").append(((IMark) element).getName()).append("'?");
      else if (element instanceof INews)
        message.append("the selected News?");
      else if (element instanceof EntityGroup)
        message.append("all elements of the group '").append(((EntityGroup) element).getName()).append("'?");
    }

    /* N Elements */
    else {
      message.append("the selected elements?");
    }

    /* Create Dialog and open */
    String[] buttons = new String[] { "Delete", IDialogConstants.CANCEL_LABEL };
    MessageDialog dialog = new MessageDialog(fShell, "Confirm Delete", null, message.toString(), MessageDialog.QUESTION, buttons, 0);
    return dialog.open() == 0;
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }

  /*
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   * org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    fShell = targetPart.getSite().getShell();
  }

  private void internalRun() {
    fConfirmed = true;

    /* Only consider Entities */
    List<IEntity> entities = ModelUtils.getEntities(fSelection);

    /* Retrieve any Folder that is to be deleted */
    List<IFolder> folders = null;
    for (Object element : entities) {
      if (element instanceof IFolder) {
        if (folders == null)
          folders = new ArrayList<IFolder>();
        folders.add((IFolder) element);
      }
    }

    /* Normalize */
    if (folders != null)
      for (IFolder folder : folders)
        ModelUtils.normalize(folder, entities);

    /* Separate News */
    List<INews> newsToDelete = null;

    /* Separate Folder */
    List<IFolder> foldersToDelete = null;

    /* Delete each Entity */
    for (IEntity element : entities) {

      /* Separate Folder */
      if (element instanceof IFolder) {
        if (foldersToDelete == null)
          foldersToDelete = new ArrayList<IFolder>();

        foldersToDelete.add((IFolder) element);
      }

      /* Delete BookMark */
      if (element instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) element;
        fModelDao.deleteBookMark(new BookMarkReference(bookmark.getId()));
      }

      /* Delete SearchMark */
      else if (element instanceof ISearchMark) {
        ISearchMark searchmark = (ISearchMark) element;
        fModelDao.deleteSearchMark(new SearchMarkReference(searchmark.getId()));
      }

      /* Separate News */
      else if (element instanceof INews) {
        if (newsToDelete == null)
          newsToDelete = new ArrayList<INews>();

        newsToDelete.add((INews) element);
      }
    }

    /* Delete Folders in single Transaction */
    if (foldersToDelete != null)
      fAppLayer.deleteFolders(foldersToDelete);

    /* Delete News in single Transaction */
    if (newsToDelete != null)
      fAppLayer.setNewsState(newsToDelete, INews.State.DELETED, false, false);
  }

  /**
   * @return <code>TRUE</code> if the user confirmed the deletion and
   * <code>FALSE</code> otherwise.
   */
  public boolean isConfirmed() {
    return fConfirmed;
  }
}