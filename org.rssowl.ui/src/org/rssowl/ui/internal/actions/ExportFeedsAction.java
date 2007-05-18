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
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * TODO This is just for Developers Purposes!
 *
 * @author bpasero
 */
public class ExportFeedsAction extends Action implements IWorkbenchWindowActionDelegate {
  private Shell fShell;
  private DateFormat fDateFormat = DateFormat.getDateInstance();

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
        Long selectedRootFolderID = DynamicDAO.getDAO(IPreferenceDAO.class).load(BookMarkExplorer.PREF_SELECTED_BOOKMARK_SET).getLong();
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
    writer.write("<opml version=\"1.1\" xmlns:rssowl=\"http://www.rssowl.org\">\n");
    writer.write("<body>\n");

    exportToOPML(root, writer);

    writer.write("</body>\n");
    writer.write("</opml>\n");

    writer.close();
  }

  private void exportToOPML(IFolder folder, OutputStreamWriter writer) throws IOException, PersistenceException {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      String name = escape(mark.getName());

      /* Export BookMark */
      if (mark instanceof IBookMark) {
        String link = escape(((IBookMark) mark).getFeedLinkReference().getLink().toString());

        writer.write("<outline text=\"" + name + "\" xmlUrl=\"" + link + "\" />\n");
      }

      /* Export SearchMark */
      else if (mark instanceof ISearchMark) {
        ISearchMark searchMark = (ISearchMark) mark;
        List<ISearchCondition> conditions = searchMark.getSearchConditions();

        writer.write("<rssowl:savedsearch name=\"" + name + "\" matchAllConditions=\"" + searchMark.matchAllConditions() + "\">\n");
        for (ISearchCondition condition : conditions) {
          writer.write("\t<rssowl:searchcondition>\n");
          writer.write(toXML(condition));
          writer.write("\t</rssowl:searchcondition>\n");
        }
        writer.write("</rssowl:savedsearch>\n\n");
      }
    }

    List<IFolder> childFolders = folder.getFolders();
    for (IFolder childFolder : childFolders) {
      String name = escape(childFolder.getName());
      writer.write("<outline text=\"" + name + "\">\n");
      exportToOPML(childFolder, writer);
      writer.write("</outline>\n");
    }
  }

  private String escape(String str) {
    str = StringUtils.replaceAll(str, "&", "&amp;");
    str = StringUtils.replaceAll(str, "<", "&lt;");
    str = StringUtils.replaceAll(str, ">", "&gt;");

    return str;
  }

  private String toXML(ISearchCondition condition) {
    StringBuilder str = new StringBuilder();

    /* Search Specifier */
    str.append("\t\t<rssowl:searchspecifier id=\"" + condition.getSpecifier().ordinal() + "\" />\n");

    /* Single Value */
    if (!EnumSet.class.isAssignableFrom(condition.getValue().getClass()))
      str.append("\t\t<rssowl:searchvalue value=\"" + getValueString(condition) + "\" type=\"" + condition.getField().getSearchValueType().getId() + "\" />\n");

    /* Multiple Values */
    else {
      EnumSet<?> values = ((EnumSet<?>) condition.getValue());

      str.append("\t\t<rssowl:searchvalue type=\"" + condition.getField().getSearchValueType().getId() + "\">\n");

      for (Enum<?> enumValue : values)
        str.append("\t\t\t<rssowl:newsstate value=\"" + enumValue.ordinal() + "\" />\n");

      str.append("\t\t</rssowl:searchvalue>\n");
    }

    /* Search Field */
    str.append("\t\t<rssowl:searchfield name=\"" + getSearchFieldName(condition.getField().getId()) + "\" entity=\"" + condition.getField().getEntityName() + "\" />\n");

    return str.toString();
  }

  private String getSearchFieldName(int fieldId) {
    switch (fieldId) {
      case (IEntity.ALL_FIELDS):
        return "allFields";
      case (INews.TITLE):
        return "title";
      case (INews.LINK):
        return "link";
      case (INews.DESCRIPTION):
        return "description";
      case (INews.PUBLISH_DATE):
        return "publishDate";
      case (INews.MODIFIED_DATE):
        return "modifiedDate";
      case (INews.RECEIVE_DATE):
        return "receiveDate";
      case (INews.AUTHOR):
        return "author";
      case (INews.COMMENTS):
        return "comments";
      case (INews.GUID):
        return "guid";
      case (INews.SOURCE):
        return "source";
      case (INews.HAS_ATTACHMENTS):
        return "hasAttachments";
      case (INews.ATTACHMENTS_CONTENT):
        return "attachments";
      case (INews.CATEGORIES):
        return "categories";
      case (INews.IS_FLAGGED):
        return "isFlagged";
      case (INews.STATE):
        return "state";
      case (INews.LABEL):
        return "label";
      case (INews.RATING):
        return "rating";
      case (INews.FEED):
        return "feed";
      case (INews.AGE_IN_DAYS):
        return "ageInDays";
      default:
        return "allFields";
    }
  }

  private String getValueString(ISearchCondition condition) {
    if (condition.getValue() instanceof Date)
      return fDateFormat.format((Date) condition.getValue());

    return escape(condition.getValue().toString());
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}
}