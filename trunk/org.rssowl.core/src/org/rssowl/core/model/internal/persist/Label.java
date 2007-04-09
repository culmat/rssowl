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
import org.rssowl.core.model.persist.ILabel;
import org.rssowl.core.util.StringUtils;

/**
 * A Label for a News. Some predefined Labels could be "Important", "Work",
 * "Personal", "Todo". Labels should be added by the user and be shown in a
 * custom Color. Labels could also be used to represent AmphetaRate ratings.
 * 
 * @author bpasero
 */
public class Label extends AbstractEntity implements ILabel {

  private String fName;
  private String fColor;

  /**
   * Creates a new Element of type Label.
   * 
   * @param id The unique ID of this Label.
   * @param name The Name of this Label.
   */
  public Label(Long id, String name) {
    super(id);
    Assert.isNotNull(name, "The type Label requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }
  
  /**
   * Default constructor for deserialization
   */
  protected Label() {
    // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.impl.ILabel#getColor()
   */
  public String getColor() {
    return fColor;
  }

  /*
   * @see org.rssowl.core.model.types.ILabel#setColor(java.lang.String)
   */
  public void setColor(String color) {
    Assert.isLegal(StringUtils.isValidRGB(color), "Color must be using the format \"R,G,B\", for example \"255,255,127\""); //$NON-NLS-1$
    fColor = color;
  }

  /*
   * @see org.rssowl.core.model.types.impl.ILabel#getName()
   */
  public String getName() {
    return fName;
  }

  /*
   * @see org.rssowl.core.model.types.ILabel#setName(java.lang.String)
   */
  public void setName(String name) {
    Assert.isNotNull(name, "The type Label requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }
  
  /**
   * Compare the given type with this type for identity.
   * 
   * @param label to be compared.
   * @return whether this object and <code>label</code> are identical. It
   * compares all the fields.
   */
  public boolean isIdentical(ILabel label) {
    if (this == label)
      return true;

    if (label instanceof Label == false)
      return false;

    Label l = (Label) label;
    
    return getId() == l.getId() && fName.equals(l.fName) &&
        (fColor == null ? l.fColor == null : fColor.equals(l.fColor)) && 
        (getProperties() == null ? l.getProperties() == null : getProperties().equals(l.getProperties()));
  }
  
  @Override
  @SuppressWarnings("nls")
  public String toString() {
    return super.toString() + "Name = " + fName + ", Color = " + fColor + ")";
  }
}