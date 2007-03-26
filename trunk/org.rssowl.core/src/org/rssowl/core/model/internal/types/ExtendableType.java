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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.rssowl.core.model.types.IExtendableType;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The abstract super-type of all Model Objects. It offers the following
 * features:
 * <ul>
 * <li>Lookup for <code>org.eclipse.core.runtime.IAdaptable</code> Adapters
 * using the Platform's Adapter Registry. This allows dynamic API extension on
 * Runtime without touching the Model Object's source.</li>
 * <li> A Map allowing to store Properties. The content of the Map is stored
 * into the DataBase and thereby kept persistant in case they are Serializable.
 * This is done using Java's Serialization Feature and it is strongly
 * recommended not to use the Properties to store complex Objects. It is very
 * good to use with Strings or primitive Arrays. For any complex type, please
 * consider to extend the DataBase with a new relation to store your data.</li>
 * </ul>
 * 
 * @author bpasero
 */
public abstract class ExtendableType implements IExtendableType {
  private HashMap<String, Serializable> fProperties;

  /**
   * Default constructor provided for deserialization purposes.
   */
  protected ExtendableType() {}

  /**
   * Set a Property identified by a unique Key to this Model. Because the value
   * is persisted into the DataBase, it is required that the value is
   * implementing <code>java.io.Serializable</code>
   * <p>
   * It is <em>not</em> recommended to store complex types as Properties, but
   * Strings and other basic Types.
   * </p>
   * <p>
   * Chose a key with <em>caution</em>. The key should be qualified like
   * classes, for instance "org.yourproject.yourpackage.YourProperty" in order
   * to avoid overriding another key that was set by a different person.
   * </p>
   * 
   * @param key The unique identifier of the Property.
   * @param value The value of the Property.
   * @see org.rssowl.core.model.types.IExtendableType#setProperty(java.lang.String,
   * java.lang.Object)
   */
  public void setProperty(String key, Object value) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      fProperties = new HashMap<String, Serializable>();

    /* Ignore any value not being a subtype of Serializable */
    if (value instanceof Serializable)
      fProperties.put(key, (Serializable) value);
  }

  /*
   * @see org.rssowl.core.model.types.IExtendableType#getProperty(java.lang.String)
   */
  public Object getProperty(String key) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      return null;

    return fProperties.get(key);
  }

  /*
   * @see org.rssowl.core.model.types.IExtendableType#removeProperty(java.lang.String)
   */
  public Object removeProperty(String key) {
    Assert.isNotNull(key, "Using NULL as Key is not permitted!"); //$NON-NLS-1$
    if (fProperties == null)
      return null;

    return fProperties.remove(key);
  }

  /*
   * @see org.rssowl.core.model.types.IExtendableType#getProperties()
   */
  public Map<String, ? > getProperties() {
    if (fProperties == null)
      return Collections.emptyMap();

    return Collections.unmodifiableMap(fProperties);
  }
  
  protected final boolean processListMergeResult(MergeResult mergeResult, ComplexMergeResult<?> listMergeResult) {
    mergeResult.addAll(listMergeResult);
    if (listMergeResult.isStructuralChange())
      return true;
    
    return false;
  }

  /**
   * If <code>uri</code> is <code>null</code>, returns <code>null</code>.
   * Otherwise, tries to create a URI from <code>uri</code> and throws an
   * IllegalStateException if this fails.
   * 
   * @param uri
   * @return a URI created from <code>uri</code>.
   * @throws IllegalStateException
   */
  protected final URI createURI(String uri) {
    if (uri == null)
      return null;

    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Somehow an invalid URI was stored", e); //$NON-NLS-1$
    }
  }

  /**
   * Returns the text representation of <code>uri</code> if <code>uri</code>
   * is not <code>null</code>. Returns <code>null</code> otherwise.
   * 
   * @param uri
   * @return the text representation of <code>uri</code> or <code>null</code>.
   */
  protected final String getURIText(URI uri) {
    return uri == null ? null : uri.toString();
  }

  /**
   * Returns an object which is an instance of the given class associated with
   * this object. Returns <code>null</code> if no such object can be found.
   * <p>
   * This implementation of the method declared by <code>IAdaptable</code>
   * passes the request along to the platform's adapter manager; roughly
   * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
   * Subclasses may override this method (however, if they do so, they should
   * invoke the method on their superclass to ensure that the Platform's adapter
   * manager is consulted).
   * </p>
   * 
   * @param adapter the class to adapt to
   * @return the adapted object or <code>null</code>
   * @see IAdaptable#getAdapter(Class)
   * @see Platform#getAdapterManager()
   */
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return Platform.getAdapterManager().getAdapter(this, adapter);
  }
}