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

package org.rssowl.core.model.internal.types;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.model.types.IFolder;
import org.rssowl.core.model.types.IMark;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Folders store a number of Marks in an hierachical order. The hierachical
 * order is achieved by allowing to store Folders inside Folders.
 * <p>
 * In case a Blogroll Link is set for the Folder, it is to be interpreted as
 * root-folder of a "Synchronized Blogroll". This special kind of Folder allows
 * to synchronize its contents from a remote OPML file that contains a number of
 * Feeds.
 * </p>
 *
 * @author bpasero
 */
public class Folder extends AbstractEntity implements IFolder {

  /* Attributes */
  private String fName;
  private String fBlogrollLink;
  private IFolder fParent;
  private List<IMark> fMarks;
  private List<IFolder> fFolders;

  /**
   * Creates a new Folder with the given ID and Name as a Child of the given
   * FolderReference. In case the FolderReference is <code>NULL</code>, this
   * Folder is root-leveld.
   *
   * @param id The unique ID of this Folder.
   * @param parent The parent Folder this Folder belongs to, or
   * @param name The Name of this Folder. <code>NULL</code>, if this Folder
   * is a Root-Folder.
   */
  public Folder(Long id, IFolder parent, String name) {
    super(id);
    Assert.isNotNull(name, "The type Folder requires a Name that is not NULL"); //$NON-NLS-1$
    fParent = parent;
    fName = name;
    fMarks = new ArrayList<IMark>(3);
    fFolders = new ArrayList<IFolder>(3);
  }

  /**
   * Default constructor for deserialization
   */
  protected Folder() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#addMark(org.rssowl.core.model.reference.MarkReference)
   */
  public void addMark(IMark mark) {
    Assert.isNotNull(mark, "Exception adding NULL as Mark into Folder"); //$NON-NLS-1$
    Assert.isTrue(equals(mark.getFolder()), "The Mark has a different Folder set!"); //$NON-NLS-1$
    fMarks.add(mark);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#getMarks()
   */
  public List<IMark> getMarks() {
    return Collections.unmodifiableList(fMarks);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#addFolder(org.rssowl.core.model.reference.FolderReference)
   */
  public void addFolder(IFolder folder) {
    Assert.isNotNull(folder, "Exception adding NULL as Child Folder into Parent Folder"); //$NON-NLS-1$
    Assert.isTrue(equals(folder.getParent()), "The Folder has a different Parent Folder set!"); //$NON-NLS-1$
    fFolders.add(folder);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#getFolders()
   */
  public List<IFolder> getFolders() {
    return Collections.unmodifiableList(fFolders);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#setBlogrollLink(java.net.URI)
   */
  public void setBlogrollLink(URI blogrollLink) {
    fBlogrollLink = getURIText(blogrollLink);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#getName()
   */
  public String getName() {
    return fName;
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#setName(java.lang.String)
   */
  public void setName(String name) {
    Assert.isNotNull(name, "The type Folder requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#getParent()
   */
  public IFolder getParent() {
    return fParent;
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#removeMark(org.rssowl.core.model.types.IMark)
   */
  public void removeMark(IMark mark) {
    fMarks.remove(mark);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#removeFolder(org.rssowl.core.model.types.IFolder)
   */
  public void removeFolder(IFolder folder) {
    fFolders.remove(folder);
  }

  /*
   * @see org.rssowl.core.model.types.Reparentable#setParent(java.lang.Object)
   */
  public void setParent(IFolder newParent) {
    fParent = newParent;
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#getBlogrollLink()
   */
  public URI getBlogrollLink() {
    return createURI(fBlogrollLink);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#reorderFolders(java.util.List,
   * org.rssowl.core.model.types.IFolder, boolean)
   */
  public void reorderFolders(List<IFolder> folders, IFolder position, boolean after) {
    Assert.isTrue(fFolders.contains(position));
    Assert.isTrue(fFolders.containsAll(folders));

    /* First, remove the given Folders */
    fFolders.removeAll(folders);

    int index = fFolders.indexOf(position);

    /* Insert to end of List */
    if (index == fFolders.size() && after)
      fFolders.addAll(folders);

    /* Insert after Position */
    else if (after)
      fFolders.addAll(index + 1, folders);

    /* Insert before Position */
    else
      fFolders.addAll(index, folders);
  }

  /*
   * @see org.rssowl.core.model.types.IFolder#reorderMarks(java.util.List,
   * org.rssowl.core.model.types.IMark, boolean)
   */
  public void reorderMarks(List<IMark> marks, IMark position, boolean after) {
    Assert.isTrue(fMarks.contains(position));
    Assert.isTrue(fMarks.containsAll(marks));

    /* First, remove the given Marks */
    fMarks.removeAll(marks);

    int index = fMarks.indexOf(position);

    /* Insert to end of List */
    if (index == fMarks.size() && after)
      fMarks.addAll(marks);

    /* Insert after Position */
    else if (after)
      fMarks.addAll(index + 1, marks);

    /* Insert to before Position */
    else
      fMarks.addAll(index, marks);
  }

  /**
   * Compare the given type with this type for identity.
   *
   * @param folder to be compared.
   * @return whether this object and <code>folder</code> are identical. It
   * compares all the fields.
   */
  public boolean isIdentical(IFolder folder) {
    if (this == folder)
      return true;

    if (folder instanceof Folder == false)
      return false;

    Folder f = (Folder) folder;

    return  getId() == f.getId() && (fParent == null ? f.fParent == null : fParent.equals(f.fParent)) &&
            (fName == null ? f.fName == null : fName.equals(f.fName)) &&
            (getBlogrollLink() == null ? f.getBlogrollLink() == null : getBlogrollLink().equals(f.getBlogrollLink())) &&
            (fMarks == null ? f.fMarks == null : fMarks.equals(f.fMarks)) &&
            (fFolders == null ? f.fFolders == null : fFolders.equals(f.fFolders)) &&
            (getProperties() == null ? f.getProperties() == null : getProperties().equals(f.getProperties()));
  }

  @SuppressWarnings("nls")
  @Override
  public String toString() {
    return super.toString() + "Name = " + fName + ")";
  }

  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  @SuppressWarnings("nls")
  public String toLongString() {
    return super.toString() + "Name = " + fName + ", Blogroll Link = " + fBlogrollLink + ", Child Marks = " + fMarks.toString() + ", Child Folders = " + fFolders.toString() + ", Parent Folder = " + (fParent != null ? fParent.getId() : "none") + ")";
  }
}