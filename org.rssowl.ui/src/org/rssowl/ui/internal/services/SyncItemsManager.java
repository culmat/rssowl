/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2011 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.services;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.rssowl.ui.internal.Activator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The {@link SyncItemsManager} serializes and deserializes uncommitted
 * {@link SyncItem} to the file system.
 *
 * @author bpasero
 */
public class SyncItemsManager {

  /* File of uncommitted sync items */
  private static final String UNCOMMITTED_SYNCITEMS_FILE = "syncitems"; //$NON-NLS-1$

  private List<SyncItem> fItems = new ArrayList<SyncItem>();

  /* Deserialize Items from Filesystem */
  void startup() throws IOException, ClassNotFoundException {
    synchronized (fItems) {
      fItems = deserializeSyncItems();
    }
  }

  /* Serialize Items to Filesystem */
  void shutdown() throws IOException {
    synchronized (fItems) {
      serializeSyncItems(fItems);
    }
  }

  void addUncommitted(Collection<SyncItem> items) {
    synchronized (fItems) {
      fItems.addAll(items);
    }
  }

  List<SyncItem> getUncommittedItems() {
    synchronized (fItems) {
      return new ArrayList<SyncItem>(fItems);
    }
  }

  void removeUncommitted(Collection<SyncItem> items) {
    synchronized (fItems) {
      fItems.removeAll(items);
    }
  }

  void removeUncommitted(SyncItem item) {
    synchronized (fItems) {
      fItems.remove(item);
    }
  }

  private void serializeSyncItems(List<SyncItem> items) throws IOException {
    File store = getUncommittedSyncItemsFile();
    if (store == null)
      return;

    if (store.exists() && !store.delete())
      throw new IOException(NLS.bind("Synchronization: Unable to delete file ''{0}''", store.toString())); //$NON-NLS-1$

    if (fItems.isEmpty())
      return;

    ObjectOutputStream outS = null;
    try {
      outS = new ObjectOutputStream(new FileOutputStream(store));
      outS.writeObject(items);
    } finally {
      if (outS != null)
        outS.close();
    }
  }

  @SuppressWarnings("unchecked")
  private List<SyncItem> deserializeSyncItems() throws IOException, ClassNotFoundException {
    List<SyncItem> items = new ArrayList<SyncItem>();

    File store = getUncommittedSyncItemsFile();
    if (store == null || !store.exists())
      return items;

    ObjectInputStream inS = null;
    try {
      inS = new ObjectInputStream(new FileInputStream(store));

      Object obj = inS.readObject();
      if (obj instanceof List)
        items.addAll((List) obj);
    } finally {
      if (inS != null)
        inS.close();
    }

    return items;
  }

  private File getUncommittedSyncItemsFile() throws IOException {
    Activator activator = Activator.getDefault();
    if (activator == null)
      return null;

    IPath path = new Path(activator.getStateLocation().toOSString());

    File bundleRoot = new File(path.toOSString());
    if (!bundleRoot.exists() && !bundleRoot.mkdir())
      throw new IOException(NLS.bind("Synchronization: Unable to create folder ''{0}''", bundleRoot.toString())); //$NON-NLS-1$

    path = path.append(UNCOMMITTED_SYNCITEMS_FILE);

    return new File(path.toOSString());
  }
}