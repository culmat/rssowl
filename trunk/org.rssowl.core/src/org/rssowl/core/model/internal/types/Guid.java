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

import org.rssowl.core.model.types.IGuid;
import org.rssowl.core.util.MergeUtils;

/**
 * Simple Implementation of this Type. Data is kept in fields and all Methods
 * are functional to set/get this Data.
 * 
 * @author bpasero
 */
public class Guid extends ExtendableType implements IGuid {

  /* Attributes */
  private String fValue;
  private boolean fIsPermaLink;

  /**
   * Constructor used by <code>DefaultModelTypesFactory</code>
   */
  public Guid() {}

  /**
   * @param value The unique identifier.
   */
  public Guid(String value) {
    fValue = value;
  }

  /*
   * @see org.rssowl.core.model.types.IGuid#setValue(java.lang.String)
   */
  public void setValue(String value) {
    fValue = value;
  }

  /*
   * @see org.rssowl.core.model.types.IGuid#setPermaLink(boolean)
   */
  public void setPermaLink(boolean isPermaLink) {
    fIsPermaLink = isPermaLink;
  }

  /*
   * @see org.rssowl.core.model.types.IGuid#isPermaLink()
   */
  public boolean isPermaLink() {
    return fIsPermaLink;
  }

  /*
   * @see org.rssowl.core.model.types.IGuid#getValue()
   */
  public String getValue() {
    return fValue;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + (fIsPermaLink ? 1231 : 1237);
    result = PRIME * result + ((fValue == null) ? 0 : fValue.hashCode());
    return result;
  }

  /**
   * Compare the given type with this type for identity.
   * 
   * @param guid to be compared.
   * @return whether this object and <code>guid</code> are identical. It
   * compares all the fields.
   */
  @Override
  public boolean equals(Object guid) {
    if (this == guid)
      return true;

    if (!(guid instanceof Guid))
      return false;

    Guid g = (Guid) guid;

    return (fValue == null ? g.fValue == null : fValue.equals(g.fValue)) && fIsPermaLink == g.isPermaLink();
  }

  @Override
  @SuppressWarnings("nls")
  public String toString() {
    return super.toString() + "Value = " + fValue + ", IsPermaLink = " + fIsPermaLink + ")";
  }

  /*
   * @see org.rssowl.core.model.types.MergeCapable#merge(java.lang.Object)
   */
  public MergeResult merge(IGuid objectToMerge) {
    boolean updated = fIsPermaLink != objectToMerge.isPermaLink();
    fIsPermaLink = objectToMerge.isPermaLink();
    updated |= !MergeUtils.equals(fValue, objectToMerge.getValue());
    fValue = objectToMerge.getValue();
    ComplexMergeResult<?> mergeResult = MergeUtils.mergeProperties(this, objectToMerge);
    if (updated || mergeResult.isStructuralChange())
      mergeResult.addRemovedObject(this);
    
    return mergeResult;
  }
}