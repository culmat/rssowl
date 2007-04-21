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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * TODO This is just for Developers Purposes!
 *
 * @author bpasero
 */
public class ExportFeedsAction extends Action implements IWorkbenchWindowActionDelegate {
  private Shell fShell;

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    fShell = window.getShell();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    run();
  }

  @Override
  public void run() {
    FileDialog dialog = new FileDialog(fShell, SWT.SAVE);
    dialog.setText("Export all Feeds to OPML");
    dialog.setFilterExtensions(new String[] { "*.opml", "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    dialog.setFileName("feeds.opml");
    String string = dialog.open();
    if (string != null) {
      try {
        File file = new File(string);

        /* Ask for Confirmation if file exists */
        if (file.exists()) {
          MessageBox box = new MessageBox(fShell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
          box.setMessage("The folder already contains a file named '" + file.getName() + "'.\n\nReplace the existing file?");
          box.setText("Confirm Replace");
          if (box.open() != SWT.YES)
            return;
        }

        /* Proceed Exporting */
        Long selectedRootFolderID = Owl.getPersistenceService().getPreferencesDAO().getLong(BookMarkExplorer.PREF_SELECTED_BOOKMARK_SET);
        IFolder selectedRootFolder = DynamicDAO.load(IFolder.class, selectedRootFolderID);
        exportToOPML(file, selectedRootFolder);
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private void exportToOPML(File file, IFolder root) throws IOException, PersistenceException {
    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    writer.write("<opml version=\"1.1\">\n");
    writer.write("<body>\n");

    exportToOPML(root, writer);

    writer.write("</body>\n");
    writer.write("</opml>\n");

    writer.close();
  }

  private void exportToOPML(IFolder folder, OutputStreamWriter writer) throws IOException, PersistenceException {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        String name = mark.getName().replaceAll("&", "&amp;");
        String link = ((IBookMark) mark).getFeedLinkReference().getLink().toString().replaceAll("&", "&amp;");

        writer.write("<outline text=\"" + name + "\" xmlUrl=\"" + link + "\" />\n");
      }
    }

    List<IFolder> childFolders = folder.getFolders();
    for (IFolder childFolder : childFolders) {
      String name = childFolder.getName().replaceAll("&", "&amp;");
      writer.write("<outline text=\"" + name + "\">\n");
      exportToOPML(childFolder, writer);
      writer.write("</outline>\n");
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}
}