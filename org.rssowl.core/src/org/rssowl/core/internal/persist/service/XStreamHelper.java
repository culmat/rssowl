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
package org.rssowl.core.internal.persist.service;

import org.rssowl.core.persist.event.runnable.EventRunnable;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class XStreamHelper {

  private XStreamHelper() {
    super();
  }

  public static boolean fileExists(File file) {
    if (file.exists())
      return true;
    if (new File(file.getAbsolutePath() + ".new").exists())
      return true;
    return false;
  }

  public static <T> T fromXML(XStream xStream, Class<T> expectedClass, File file) {
    Reader reader = IOHelper.createFileReader(file);
    try {
      Object object = xStream.fromXML(reader);
      return cast(expectedClass, object);
    } catch (XStreamException e) {
      throw new PersistenceException(e);
    } finally {
      IOHelper.closeQuietly(reader);
    }
  }

  public static void toXML(XStream xStream, Object object, File file, boolean sync) {
    Writer writer = null;
    try {
      FileOutputStream outputStream = new FileOutputStream(file);
      writer = IOHelper.createFileWriter(outputStream);
      xStream.toXML(object, writer);
      if (sync)
        outputStream.getFD().sync();
    } catch (IOException e) {
      throw new PersistenceException(e);
    } catch (XStreamException e) {
      throw new PersistenceException(e);
    } finally {
      IOHelper.closeQuietly(writer);
    }
  }

  public static <T> T cast(Class<T> klass, Object object) throws PersistenceException {
    if (klass.isInstance(object))
      return klass.cast(object);

    throw new PersistenceException("Expected object of type: " + klass + ", but received: " + object);
  }

  public static void fireEvents(EventsMap2 eventsMap) {
    if (eventsMap != null) {
      for (EventRunnable<?> eventRunnable : eventsMap.getEventRunnables())
        eventRunnable.run();
    }
  }
}
