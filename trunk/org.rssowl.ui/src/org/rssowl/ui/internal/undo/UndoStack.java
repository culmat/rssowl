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

package org.rssowl.ui.internal.undo;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.osgi.util.NLS;
import org.rssowl.core.util.LoggingSafeRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link UndoStack} keeps a list of {@link IUndoOperation} and supports
 * undo/redo of these. The stack has a maximum capacity as defined by
 * <code>MAX_SIZE</code>.
 *
 * @author bpasero
 */
public class UndoStack {
  private static final int MAX_SIZE = 20;
  private static UndoStack singleton = new UndoStack();

  private final List<IUndoOperation> fOperations = new ArrayList<IUndoOperation>();
  private int fCurrentIndex = 0;
  private List<IUndoRedoListener> fListeners = new ArrayList<IUndoRedoListener>();

  private UndoStack() {}

  /**
   * @return the singleton instance of the {@link UndoStack}.
   */
  public static UndoStack getInstance() {
    if (singleton == null)
      singleton = new UndoStack();

    return singleton;
  }

  /**
   * @param listener the listener to be notified when Undo or Redo was
   * performed, or when an operation was added to the stack.
   */
  public void addListener(IUndoRedoListener listener) {
    if (!fListeners.contains(listener))
      fListeners.add(listener);
  }

  /**
   * @param listener the listener to remove from the list of listeners.
   */
  public void removeListener(IUndoRedoListener listener) {
    fListeners.remove(listener);
  }

  /**
   * Adds the given operation to the stack.
   *
   * @param operation the operation to add to the stack.
   */
  public void addOperation(IUndoOperation operation) {
    Assert.isNotNull(operation);

    /* Handle case where User executed Undo-Operation */
    if (fCurrentIndex < (fOperations.size() - 1)) {

      /* Remove all following Undo-Operations */
      for (int i = fCurrentIndex + 1; i < fOperations.size(); i++)
        fOperations.remove(i);
    }

    /* Add operation and constrain size */
    fOperations.add(operation);
    if (fOperations.size() > MAX_SIZE) {
      for (int i = 0; i < fOperations.size() - MAX_SIZE; i++)
        fOperations.remove(i);
    }

    /* Set pointer to last element */
    fCurrentIndex = fOperations.size() - 1;

    /* Notify Listeners */
    notifyOperationAdded();
  }

  /**
   * @return Returns the name for the next undo-operation or a generic one if
   * undo is not supported currently.
   */
  public String getUndoName() {
    if (!isUndoSupported())
      return Messages.UndoStack_UNDO;

    return NLS.bind(Messages.UndoStack_UNDO_N, fOperations.get(fCurrentIndex).getName());
  }

  /**
   * @return Returns the name for the next redo-operation or a generic one if
   * redo is not supported currently.
   */
  public String getRedoName() {
    if (!isRedoSupported())
      return Messages.UndoStack_REDO;

    return NLS.bind(Messages.UndoStack_REDO_N, fOperations.get(fCurrentIndex + 1).getName());
  }

  /**
   * @return Returns <code>true</code> if undo is supported and
   * <code>false</code> otherwise.
   */
  public boolean isUndoSupported() {
    return fCurrentIndex >= 0 && !fOperations.isEmpty();
  }

  /**
   * @return Returns <code>true</code> if redo is supported and
   * <code>false</code> otherwise.
   */
  public boolean isRedoSupported() {
    return fCurrentIndex < (fOperations.size() - 1);
  }

  /**
   * Navigates backwards in the list of operations if possible and undos the
   * operation.
   */
  public void undo() {
    if (!isUndoSupported())
      return;

    fOperations.get(fCurrentIndex).undo();
    fCurrentIndex--;
    notifyUndoPerformed();
  }

  /**
   * Navigates forwards in the list of operations if possible and redos the
   * operation.
   */
  public void redo() {
    if (!isRedoSupported())
      return;

    fCurrentIndex++;
    fOperations.get(fCurrentIndex).redo();
    notifyRedoPerformed();
  }

  private void notifyUndoPerformed() {
    for (final IUndoRedoListener listener : fListeners) {
      SafeRunnable.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.undoPerformed();
        }
      });
    }
  }

  private void notifyRedoPerformed() {
    for (final IUndoRedoListener listener : fListeners) {
      SafeRunnable.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.redoPerformed();
        }
      });
    }
  }

  private void notifyOperationAdded() {
    for (final IUndoRedoListener listener : fListeners) {
      SafeRunnable.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          listener.operationAdded();
        }
      });
    }
  }
}