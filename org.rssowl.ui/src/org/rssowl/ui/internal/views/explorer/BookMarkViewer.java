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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.IOpenEventListener;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.rssowl.ui.internal.util.ViewerOpenStrategy;

/**
 * A Subclass of <code>TreeViewer</code> to display Folders and all kinds of
 * Marks in the "BookMark Explorer".
 *
 * @author bpasero
 */
public class BookMarkViewer extends TreeViewer {
  private final BookMarkExplorer fExplorer;
  private ListenerList fOpenListeners = new ListenerList();
  private ViewerOpenStrategy fViewerOpenStrategy;

  /**
   * @param explorer
   * @param parent
   * @param style
   */
  public BookMarkViewer(BookMarkExplorer explorer, Composite parent, int style) {
    super(parent, style);
    fExplorer = explorer;
  }

  /*
   * @see org.eclipse.jface.viewers.TreeViewer#createChildren(org.eclipse.swt.widgets.Widget)
   */
  @Override
  public void createChildren(Widget widget) {
    super.createChildren(widget);
  }

  /*
   * @see org.eclipse.jface.viewers.StructuredViewer#refresh(boolean)
   */
  @Override
  public void refresh(boolean updateLabels) {
    getControl().getParent().setRedraw(false);
    try {
      super.refresh(updateLabels);
    } finally {
      getControl().getParent().setRedraw(true);
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ColumnViewer#refresh(java.lang.Object)
   */
  @Override
  public void refresh(Object element) {
    super.refresh(element);

    /* Avoid restoring expanded elements on refresh() */
    if (element == getRoot())
      return;

    /* TODO Revisit later */
    fExplorer.restoreExpandedElements();
  }

  /*
   * @see org.eclipse.jface.viewers.StructuredViewer#refresh(java.lang.Object,
   * boolean)
   */
  @Override
  public void refresh(Object element, boolean updateLabels) {
    super.refresh(element, updateLabels);

    /* TODO Revisit later */
    fExplorer.restoreExpandedElements();
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection)
   */
  @Override
  public void setSelection(ISelection selection) {
    super.setSelection(selection);
    fViewerOpenStrategy.clearExpandFlag(); // See Bug 164372
  }

  /*
   * @see org.eclipse.jface.viewers.StructuredViewer#setSelection(org.eclipse.jface.viewers.ISelection,
   * boolean)
   */
  @Override
  public void setSelection(ISelection selection, boolean reveal) {
    super.setSelection(selection, reveal);
    fViewerOpenStrategy.clearExpandFlag(); // See Bug 164372
  }

  /*
   * @see org.eclipse.jface.viewers.TreeViewer#hookControl(org.eclipse.swt.widgets.Control)
   */
  @Override
  protected void hookControl(Control control) {
    super.hookControl(control);

    /* Add a ViewerOpenStrategy */
    fViewerOpenStrategy = new ViewerOpenStrategy(control);
    fViewerOpenStrategy.addOpenListener(new IOpenEventListener() {
      public void handleOpen(SelectionEvent e) {
        internalHandleOpen();
      }
    });
  }

  /*
   * Overrides the open-listener to work with the ViewerOpenStrategy.
   *
   * @see org.eclipse.jface.viewers.StructuredViewer#addOpenListener(org.eclipse.jface.viewers.IOpenListener)
   */
  @Override
  public void addOpenListener(IOpenListener listener) {
    fOpenListeners.add(listener);
  }

  private void internalHandleOpen() {
    Control control = getControl();
    if (control != null && !control.isDisposed()) {
      ISelection selection = getSelection();
      internalFireOpen(new OpenEvent(this, selection));
    }
  }

  private void internalFireOpen(final OpenEvent event) {
    Object[] listeners = fOpenListeners.getListeners();
    for (int i = 0; i < listeners.length; ++i) {
      final IOpenListener listener = (IOpenListener) listeners[i];
      SafeRunnable.run(new SafeRunnable() {
        public void run() {
          listener.open(event);
        }
      });
    }
  }
}