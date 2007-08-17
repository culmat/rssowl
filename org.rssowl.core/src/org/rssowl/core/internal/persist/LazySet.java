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
package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IEntity;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LazySet<E extends IEntity> implements Set<E>   {

  private final Set<Long> fIds;
  private final ObjectContainer fObjectContainer;

  public LazySet(ObjectSet<? extends E> entities, ObjectContainer objectContainer) {
    Assert.isNotNull(entities, "entities");
    Assert.isNotNull(objectContainer, "objectContainer");
    long[] ids = entities.ext().getIDs();
    fIds = new LinkedHashSet<Long>(ids.length);
    for (long id : ids) {
      fIds.add(id);
    }
    fObjectContainer = objectContainer;
  }

  public Object[] toArray() {
    Object[] array = new Object[size()];
    int index = 0;
    for (E e : this)
      array[index++] = e;

    return array;
  }

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    int size = size();
    T[] array = a;
    if (a.length < size)
      array = (T[]) Array.newInstance(a.getClass().getComponentType(), size);

    int index = 0;
    for (E e : this)
      array[index++] = (T) e;

    return array;
  }

  public Iterator<E> iterator() {
    return new Iterator<E>() {
      private final Iterator<Long> fDelegateIterator = fIds.iterator();
      public boolean hasNext() {
        return fDelegateIterator.hasNext();
      }

      public E next() {
        Long id = fDelegateIterator.next();
        E object = fObjectContainer.ext().getByID(id);
        fObjectContainer.activate(object, Integer.MAX_VALUE);
        return object;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection< ? extends E> c) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public boolean contains(Object o) {
    if (o instanceof IEntity) {
      return fIds.contains(((IEntity) o).getId());
    }
    return false;
  }

  public boolean containsAll(Collection< ? > c) {
    for (Object o : c) {
      if (!contains(o))
        return false;
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (!(o instanceof Set))
      return false;
    Collection< ? > c = (Collection< ? >) o;
    if (c.size() != size())
      return false;

    return containsAll(c);
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (E e : this) {
      if (e != null)
        h += e.hashCode();
    }
    return h;
  }

  public boolean isEmpty() {
    return fIds.isEmpty();
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public boolean removeAll(Collection< ? > c) {
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection< ? > c) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    return fIds.size();
  }
}
