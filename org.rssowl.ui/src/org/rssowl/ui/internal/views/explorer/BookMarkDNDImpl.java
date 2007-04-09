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

package org.rssowl.ui.internal.views.explorer;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.ReparentInfo;
import org.rssowl.core.model.dao.IApplicationLayer;
import org.rssowl.core.model.dao.PersistenceLayer;
import org.rssowl.core.model.persist.IBookMark;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFolder;
import org.rssowl.core.model.persist.IMark;
import org.rssowl.core.model.persist.ISearchMark;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.actions.NewBookMarkAction;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>BookMarkDropImpl</code> is handling all drop-operations resulting
 * from a DND.
 *
 * @author bpasero
 */
public class BookMarkDNDImpl extends ViewerDropAdapter implements DragSourceListener {
  private final BookMarkExplorer fExplorer;
  private final PersistenceLayer fPersistence;

  /**
   * @param explorer
   * @param viewer
   */
  protected BookMarkDNDImpl(BookMarkExplorer explorer, Viewer viewer) {
    super(viewer);
    fExplorer = explorer;
    fPersistence = NewsModel.getDefault().getPersistenceLayer();
  }

  /*
   * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
   */
  public void dragStart(final DragSourceEvent event) {

    /* Set normalized selection into Transfer if not in grouping mode */
    if (!fExplorer.isGroupingEnabled()) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          LocalSelectionTransfer.getTransfer().setSelection(getNormalizedSelection());
          LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
          event.doit = true;
        }
      });
    }
  }

  private ISelection getNormalizedSelection() {
    IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
    List< ? > selectedObjects = selection.toList();

    /* Retrieve dragged Marks / Folders and separate */
    List<IEntity> draggedEntities = new ArrayList<IEntity>(selectedObjects.size());
    List<IFolder> draggedFolders = new ArrayList<IFolder>(selectedObjects.size());
    for (Object object : selectedObjects) {

      /* Dragged Mark */
      if (object instanceof IMark)
        draggedEntities.add((IMark) object);

      /* Dragged Folder */
      else if (object instanceof IFolder) {
        draggedEntities.add((IFolder) object);
        draggedFolders.add((IFolder) object);
      }
    }

    /* Normalize the dragged entities */
    for (IFolder folder : draggedFolders)
      ModelUtils.normalize(folder, draggedEntities);

    return new StructuredSelection(draggedEntities);
  }

  /*
   * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
   */
  public void dragSetData(final DragSourceEvent event) {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {

        /* Set Selection using LocalSelectionTransfer */
        if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType))
          event.data = LocalSelectionTransfer.getTransfer().getSelection();

        /* Set Text using TextTransfer */
        else if (TextTransfer.getInstance().isSupportedType(event.dataType))
          setTextData(event);
      }
    });
  }

  private void setTextData(DragSourceEvent event) {
    StringBuilder str = new StringBuilder("");
    IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
    List< ? > selectedObjects = selection.toList();
    for (Object selectedObject : selectedObjects) {

      /* IFolder */
      if (selectedObject instanceof IFolder) {
        IFolder folder = (IFolder) selectedObject;
        str.append(folder.getName()).append("\n");
      }

      /* IBookMark */
      else if (selectedObject instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) selectedObject;
        str.append(bookmark.getFeedLinkReference().getLink()).append("\n");
        str.append(bookmark.getName()).append("\n\n");
      }

      /* ISearchMark */
      else if (selectedObject instanceof ISearchMark) {
        ISearchMark searchmark = (ISearchMark) selectedObject;
        str.append(searchmark.getName()).append("\n");
      }

      /* Entity Group */
      else if (selectedObject instanceof EntityGroup) {
        EntityGroup entitygroup = (EntityGroup) selectedObject;
        str.append(entitygroup.getName()).append("\n");
      }
    }

    event.data = str.toString();
  }

  /*
   * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
   */
  public void dragFinished(DragSourceEvent event) {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        LocalSelectionTransfer.getTransfer().setSelection(null);
        LocalSelectionTransfer.getTransfer().setSelectionSetTime(0);
      }
    });
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
   */
  @Override
  public void dragOver(DropTargetEvent event) {
    super.dragOver(event);

    /* Un-Set some feedback if sorting by name */
    if (fExplorer.isSortByNameEnabled()) {
      event.feedback &= ~DND.FEEDBACK_INSERT_AFTER;
      event.feedback &= ~DND.FEEDBACK_INSERT_BEFORE;
    }

    /* Un-Set some feedback if grouping */
    if (fExplorer.isGroupingEnabled()) {
      event.feedback &= ~DND.FEEDBACK_INSERT_AFTER;
      event.feedback &= ~DND.FEEDBACK_INSERT_BEFORE;
      event.feedback &= ~DND.FEEDBACK_SELECT;
    }

    /* Never give Select as Feedback from a Mark */
    if (getCurrentTarget() instanceof IMark)
      event.feedback &= ~DND.FEEDBACK_SELECT;

    /* Unset some feedback when Text-Transfer is used */
    if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
      event.feedback &= ~DND.FEEDBACK_INSERT_AFTER;
      event.feedback &= ~DND.FEEDBACK_INSERT_BEFORE;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object,
   * int, org.eclipse.swt.dnd.TransferData)
   */
  @Override
  public boolean validateDrop(final Object target, int operation, TransferData transferType) {

    /* Grouping does not allow Drag and Drop */
    if (fExplorer.isGroupingEnabled())
      return false;

    /* Require Entity as Target */
    if (!(target instanceof IEntity))
      return false;

    /* Selection Transfer */
    if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
      final boolean[] result = new boolean[] { false };
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
          if (selection instanceof IStructuredSelection) {
            List< ? > draggedObjects = ((IStructuredSelection) selection).toList();
            result[0] = isValidDrop((IEntity) target, draggedObjects);
          }
        }
      });

      return result[0];
    }

    /* Text-Transfer (supported for all) */
    else if (TextTransfer.getInstance().isSupportedType(transferType)) {
      return true;
    }

    return false;
  }

  private boolean isValidDrop(IEntity dropTarget, List< ? > draggedObjects) {

    /* Check validity for each dragged Object */
    for (Object draggedObject : draggedObjects) {

      /* Shared rule: Do not allow to drop on same Entity */
      if (draggedObject.equals(dropTarget))
        return false;

      /* Dragged Folder */
      if (draggedObject instanceof IFolder) {
        IFolder draggedFolder = (IFolder) draggedObject;
        if (!isValidDrop(draggedFolder, dropTarget))
          return false;
      }

      /* Dragged Mark */
      else if (draggedObject instanceof IMark) {
        IMark draggedMark = (IMark) draggedObject;
        if (!isValidDrop(draggedMark, dropTarget))
          return false;
      }
    }

    return true;
  }

  private boolean isValidDrop(IFolder dragSource, IEntity dropTarget) {
    int loc = getCurrentLocation();

    /* Do not allow dropping on same Parent */
    if (loc == LOCATION_ON && dragSource.getParent().equals(dropTarget))
      return false;

    /* Do not allow Re-Ordering of Entities if sort by name */
    if (fExplorer.isSortByNameEnabled() && (loc == LOCATION_AFTER || loc == LOCATION_BEFORE)) {
      if (dropTarget instanceof IFolder) {
        IFolder target = (IFolder) dropTarget;
        if (target.getParent().getFolders().contains(dragSource))
          return false;
      }
    }

    /* Do not allow Re-Ordering over IMarks */
    if (dropTarget instanceof IMark) {
      IMark target = (IMark) dropTarget;
      if (target.getFolder().getFolders().contains(dragSource))
        return false;
    }

    /* Do not allow dropping in Child of Drag-Folder */
    if (ModelUtils.hasChildRelation(dragSource, dropTarget))
      return false;

    return true;
  }

  private boolean isValidDrop(IMark dragSource, IEntity dropTarget) {
    int loc = getCurrentLocation();

    /* Do not allow dropping on same Parent */
    if (loc == LOCATION_ON && dragSource.getFolder().equals(dropTarget))
      return false;

    /* Do not allow Re-Ordering of Entities if sort by name */
    if (fExplorer.isSortByNameEnabled()) {
      if (dropTarget instanceof IMark) {
        IMark target = (IMark) dropTarget;
        if (target.getFolder().getMarks().contains(dragSource))
          return false;
      }
    }

    /* Do not allow Re-Ordering over IFolder */
    if (dropTarget instanceof IFolder && (loc == LOCATION_AFTER || loc == LOCATION_BEFORE)) {
      IFolder target = (IFolder) dropTarget;
      if (target.getParent().getMarks().contains(dragSource))
        return false;
    }

    return true;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
   */
  @Override
  public boolean performDrop(final Object data) {

    /* Selection-Transfer */
    if (data instanceof IStructuredSelection) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          IStructuredSelection selection = (IStructuredSelection) data;
          List< ? > draggedObjects = selection.toList();
          perfromDrop(draggedObjects);
        }
      });

      return true;
    }

    /* Text-Transfer (check for URLs) */
    else if (data instanceof String) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          final List<String> urls = RegExUtils.extractLinksFromText((String) data, false);
          if (urls.size() > 0) {

            /* Determine parent folder */
            final IFolder parent;
            Object dropTarget = getCurrentTarget();
            if (dropTarget instanceof IFolder)
              parent = (IFolder) dropTarget;
            else if (dropTarget instanceof IMark)
              parent = ((IMark) dropTarget).getFolder();
            else
              parent = null;

            /* Open Dialog to add new BookMark (asyncly!) */
            JobRunner.runInUIThread(getViewer().getControl(), new Runnable() {
              public void run() {
                new NewBookMarkAction(getViewer().getControl().getShell(), parent, urls.get(0)).run(null);
              }
            });
          }
        }
      });

      return true;
    }

    return false;
  }

  private void perfromDrop(List< ? > draggedObjects) {
    Object dropTarget = getCurrentTarget();
    int location = getCurrentLocation();

    final IApplicationLayer appLayer = fPersistence.getApplicationLayer();
    IFolder parentFolder = null;
    boolean requireSave = false;
    boolean after = (location == ViewerDropAdapter.LOCATION_AFTER);
    boolean on = (location == ViewerDropAdapter.LOCATION_ON);

    /* Target is a Folder */
    if (dropTarget instanceof IFolder) {
      IFolder dropFolder = (IFolder) dropTarget;

      /* Target is the exact Folder */
      if (on)
        parentFolder = (IFolder) dropTarget;

      /* Target is below or above of the Folder */
      else
        parentFolder = dropFolder.getParent();
    }

    /* Target is a Mark */
    else if (dropTarget instanceof IMark) {
      IMark dropMark = (IMark) dropTarget;
      parentFolder = dropMark.getFolder();
    }

    /* Require a Parent-Folder */
    if (parentFolder == null)
      return;

    /* Separate into Reparented Marks and Folders and Re-Orders */
    List<IMark> markReordering = null;
    List<IFolder> folderReordering = null;
    List<ReparentInfo<IFolder, IFolder>> folderReparenting = null;
    List<ReparentInfo<IMark, IFolder>> markReparenting = null;

    /* For each dragged Object */
    for (Object object : draggedObjects) {

      /* Dragged Folder */
      if (object instanceof IFolder) {
        IFolder draggedFolder = (IFolder) object;

        /* Reparenting to new Parent */
        if (!draggedFolder.getParent().equals(parentFolder)) {
          if (folderReparenting == null)
            folderReparenting = new ArrayList<ReparentInfo<IFolder, IFolder>>(draggedObjects.size());

          IFolder position = (dropTarget instanceof IFolder && !on) ? (IFolder) dropTarget : null;
          Boolean afterB = (position != null) ? after : null;
          ReparentInfo<IFolder, IFolder> reparentInfo = new ReparentInfo<IFolder, IFolder>(draggedFolder, parentFolder, position, afterB);

          folderReparenting.add(reparentInfo);
        }

        /* Re-Ordering in same Parent */
        else {
          if (folderReordering == null)
            folderReordering = new ArrayList<IFolder>(draggedObjects.size());
          folderReordering.add(draggedFolder);
        }
      }

      /* Dragged Mark */
      else if (object instanceof IMark) {
        IMark draggedMark = (IMark) object;

        /* Reparenting to new Parent */
        if (!draggedMark.getFolder().equals(parentFolder)) {
          if (markReparenting == null)
            markReparenting = new ArrayList<ReparentInfo<IMark, IFolder>>(draggedObjects.size());

          IMark position = (dropTarget instanceof IMark) ? (IMark) dropTarget : null;
          Boolean afterB = (position != null) ? after : null;
          ReparentInfo<IMark, IFolder> reparentInfo = new ReparentInfo<IMark, IFolder>(draggedMark, parentFolder, position, afterB);

          markReparenting.add(reparentInfo);
        }

        /* Re-Ordering in same Parent */
        else {
          if (markReordering == null)
            markReordering = new ArrayList<IMark>(draggedObjects.size());
          markReordering.add(draggedMark);
        }
      }
    }

    /* Perform reparenting */
    final List<ReparentInfo<IFolder, IFolder>> finalFolderReparenting = folderReparenting;
    final List<ReparentInfo<IMark, IFolder>> finalMarkReparenting = markReparenting;
    if (folderReparenting != null || markReparenting != null) {
      BusyIndicator.showWhile(getViewer().getControl().getDisplay(), new Runnable() {
        public void run() {
          appLayer.reparent(finalFolderReparenting, finalMarkReparenting);
        }
      });
    }

    /* Perform Re-Ordering on Marks */
    if (markReordering != null && dropTarget instanceof IMark) {
      parentFolder.reorderMarks(markReordering, (IMark) dropTarget, after);
      requireSave = true;
    }

    /* Perform Re-Ordering on Folders */
    if (folderReordering != null && dropTarget instanceof IFolder) {
      parentFolder.reorderFolders(folderReordering, (IFolder) dropTarget, after);
      requireSave = true;
    }

    /* Save the Folder if required */
    if (requireSave)
      fPersistence.getModelDAO().saveFolder(parentFolder);
  }
}