/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.ITreeNode;
import org.rssowl.core.util.TreeTraversal;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.util.WidgetTreeNode;

/**
 * The actual TreeViewer responsible for displaying the Headlines of a Feed.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
public class NewsTableViewer extends TreeViewer {
  private boolean fBlockRefresh;

  /**
   * @param tree
   */
  public NewsTableViewer(Tree tree) {
    super(tree);
  }

  /*
   * @see org.eclipse.jface.viewers.StructuredViewer#refresh()
   */
  @Override
  public void refresh() {
    if (!fBlockRefresh)
      super.refresh();
  }

  /*
   * @see org.eclipse.jface.viewers.TreeViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
   */
  @Override
  public void setContentProvider(IContentProvider provider) {
    fBlockRefresh = true;
    try {
      super.setContentProvider(provider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object[])
   */
  @Override
  public void remove(final Object[] elements) {
    updateSelectionAfterDelete(new Runnable() {
      public void run() {
        internalRemove(elements);
      }
    });
  }

  void updateSelectionAfterDelete(Runnable runnable) {
    Tree tree = getTree();
    TreeItem[] oldSelection = tree.getSelection();

    /* Nothing to do, since no selection */
    if (oldSelection.length == 0) {
      runnable.run();
      return;
    }

    TreeItem lastSelectedItem = oldSelection[oldSelection.length - 1];

    /* Determine if we need to force an update of the selection (See Bug 1285) */
    boolean forceUpdateSelection = false;

    /* Force Update: Last Selected Item is an Entity Group */
    if (lastSelectedItem.getData() instanceof EntityGroup) {
      forceUpdateSelection = true;

      /* Given this group gets deleted, use the next or previous entity group as input for the WidgetTreeNode below */
      int indexOfEntityGroup = tree.indexOf(lastSelectedItem);
      if (tree.getItemCount() > indexOfEntityGroup + 1) //Try Next
        lastSelectedItem = tree.getItem(indexOfEntityGroup + 1);
      else if (indexOfEntityGroup > 0) //Try Previous
        lastSelectedItem = tree.getItem(indexOfEntityGroup - 1);
    }

    /* Force Update: Last Selected Item is last of Entity Group */
    else if (lastSelectedItem.getParentItem() != null && lastSelectedItem.getParentItem().getItemCount() == 1)
      forceUpdateSelection = true;

    /* Navigate to next News if possible */
    ITreeNode startingNode = new WidgetTreeNode(lastSelectedItem, this);
    ISelection newSelection = navigate(startingNode, true);

    /* Try previous News if possible then */
    if (newSelection == null)
      newSelection = navigate(startingNode, false);

    /* Perform Deletion */
    runnable.run();

    /* Ensure that an updated selection is required */
    boolean updateSelection = false;
    for (TreeItem oldSelectedItem : oldSelection) {
      if (oldSelectedItem.isDisposed()) {
        updateSelection = true;
        break;
      }
    }

    /* Set new Selection */
    if (updateSelection)
      setSelection(newSelection);

    /* Bug: For some reason, we need to force update of selection (see Bug 1285) */
    else if (forceUpdateSelection)
      setSelection(newSelection);
  }

  private ISelection navigate(ITreeNode startingNode, boolean next) {

    /* Create Traverse-Helper */
    TreeTraversal traverse = new TreeTraversal(startingNode) {
      @Override
      public boolean select(ITreeNode node) {
        return node.getData() instanceof INews;
      }
    };

    /* Retrieve and select new Target Node */
    ITreeNode targetNode = (next ? traverse.nextNode() : traverse.previousNode());
    if (targetNode != null) {
      ISelection selection = new StructuredSelection(targetNode.getData());
      return selection;
    }

    return null;
  }
}