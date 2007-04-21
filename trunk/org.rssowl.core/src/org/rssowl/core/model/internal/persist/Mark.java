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

package org.rssowl.core.model.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.reference.FolderReference;

import java.text.DateFormat;
import java.util.Date;

/**
 * The abstract super-type of <code>BookMark</code> and
 * <code>SearchMark</code>. Used to associate Bookmarks and Searchmarks with
 * a Folder. These Elements are considered to be leaves of the Tree.
 * 
 * @author bpasero
 */
public abstract class Mark extends AbstractEntity implements IMark {

  /* Attributes */
  private String fName;
  private Date fCreationDate;
  private Date fLastVisitDate;
  private int fPopularity;

  private IFolder fFolder;

  /**
   * Store ID, Name and Folder for this Mark.
   * 
   * @param id The unique id of this type.
   * @param folder The Folder this Mark belongs to.
   * @param name The Name of this Mark.
   */
  protected Mark(Long id, IFolder folder, String name) {
    super(id);
    Assert.isNotNull(folder, "The type Mark requires a Folder that is not NULL"); //$NON-NLS-1$
    fFolder = folder;
    Assert.isNotNull(name, "The type Mark requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }

  /**
   * Default constructor for deserialization
   */
  protected Mark() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#getLastVisitDate()
   */
  public Date getLastVisitDate() {
    return fLastVisitDate;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#setLastVisitDate(java.util.Date)
   */
  public void setLastVisitDate(Date lastVisitDate) {
    fLastVisitDate = lastVisitDate;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#getPopularity()
   */
  public int getPopularity() {
    return fPopularity;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#setPopularity(int)
   */
  public void setPopularity(int popularity) {
    fPopularity = popularity;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#getCreationDate()
   */
  public Date getCreationDate() {
    return fCreationDate;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#setCreationDate(java.util.Date)
   */
  public void setCreationDate(Date creationDate) {
    fCreationDate = creationDate;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#getName()
   */
  public String getName() {
    return fName;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#setName(java.lang.String)
   */
  public void setName(String name) {
    Assert.isNotNull(name, "The type Mark requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#getFolder()
   */
  public IFolder getFolder() {
    return fFolder;
  }

  /**
   * @return a uncached reference to the parent folder.
   */
  protected FolderReference getFolderReference() {
    return getFolder() == null ? null : new FolderReference(getFolder().getId());
  }
  
  public void setFolder(IFolder folder) {
    fFolder = folder;
  }

  @Override
  @SuppressWarnings("nls")
  public String toString() {
    return super.toString() + "Name = " + fName + ", ";
  }

  /**
   * Returns a String describing the state of this Entity.
   * 
   * @return A String describing the state of this Entity.
   */
  @SuppressWarnings("nls")
  public String toLongString() {
    String retValue = super.toString() + "Name = " + fName + ", Creation Date = " + fCreationDate + ", Popularity: " + getPopularity();
    if (getLastVisitDate() != null)
      retValue = retValue + (DateFormat.getDateTimeInstance().format(getLastVisitDate()));

    return retValue + ", Belongs to Folder = " + fFolder.getId() + ", ";
  }
}