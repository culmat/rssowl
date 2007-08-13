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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.rssowl.ui.internal.util.TreeItemAdapter;

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

  private void updateSelectionAfterDelete(Runnable runnable) {
    Tree tree = (Tree) getControl();
    IStructuredSelection selection = (IStructuredSelection) getSelection();

    /* Nothing to do, since no selection */
    if (selection.isEmpty()) {
      runnable.run();
      return;
    }

    /* Look for the minimal Index of all selected Elements */
    int minSelectedIndex = Integer.MAX_VALUE;
    TreeItemAdapter parentOfMinSelected = new TreeItemAdapter(tree);

    /* For each selected Element */
    Object[] selectedElements = selection.toArray();
    for (Object selectedElement : selectedElements) {
      Widget widget = findItem(selectedElement);
      if (widget instanceof TreeItem) {
        TreeItem item = (TreeItem) widget;
        TreeItemAdapter parent = new TreeItemAdapter(item).getParent();

        int index = parent.indexOf(item);
        minSelectedIndex = Math.min(minSelectedIndex, index);
        if (index == minSelectedIndex)
          parentOfMinSelected.setItem(parent.getItem());
      }
    }

    /* Perform Deletion */
    runnable.run();

    Object data = null;

    /* Restore selection to next Element */
    if (parentOfMinSelected.getItemCount() > minSelectedIndex)
      data = parentOfMinSelected.getItem(minSelectedIndex).getData();

    /* Restore selection to last Element */
    else if (parentOfMinSelected.getItemCount() > 0)
      data = parentOfMinSelected.getItem(parentOfMinSelected.getItemCount() - 1).getData();

    if (data != null)
      setSelection(new StructuredSelection(data));
  }
}