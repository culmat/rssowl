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

import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.service.PersistenceException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.channels.FileChannel;

public class IOHelper {

  public static Reader createFileReader(File file) {
    try {
      Reader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        reader = new FileReader(file);
      }
      return reader;
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  public static Writer createFileWriter(FileOutputStream outputStream) {
    Writer writer = null;
    try {
      writer = new OutputStreamWriter(outputStream, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      writer = new OutputStreamWriter(outputStream);
    }
    return writer;
  }

  public static Writer createFileWriter(File file) {
    try {
      return createFileWriter(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      throw new PersistenceException(e);
    }
  }

  public static void closeQuietly(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      } catch (IOException e) {
        Activator.getDefault().logError("Failed to close stream.", e);
      }
  }

  public static void writeToFile(File file, String text) {
    Writer writer = new BufferedWriter(createFileWriter(file));
    try {
      writer.write(text);
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeQuietly(writer);
    }
  }

  public static void rename(File origin, File destination) throws PersistenceException  {
    /* Try atomic rename first. If that fails, rely on delete + rename */
    if (!origin.renameTo(destination)) {
      destination.delete();
      if (!origin.renameTo(destination)) {
        throw new PersistenceException("Failed to rename: " + origin + " to: " + destination);
      }
    }
  }

  public static String readFirstLineFromFile(File file) {
    BufferedReader reader = new BufferedReader(createFileReader(file));
    try {
      String text = reader.readLine();
      return text;
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      DBHelper.closeQuietly(reader);
    }
  }

  public static final void copyFile(File originFile, File destinationFile) {
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;
    try {
      inputStream = new FileInputStream(originFile);
      FileChannel srcChannel = inputStream.getChannel();

      if (!destinationFile.exists())
        destinationFile.createNewFile();

      outputStream = new FileOutputStream(destinationFile);
      FileChannel dstChannel = outputStream.getChannel();

      long bytesToTransfer = srcChannel.size();
      long position = 0;
      while (bytesToTransfer > 0) {
        long bytesTransferred = dstChannel.transferFrom(srcChannel, position, bytesToTransfer);
        position += bytesTransferred;
        bytesToTransfer -= bytesTransferred;
      }

    } catch (IOException e) {
      Activator.getDefault().logError("Failed to copy file using NIO. Falling " +
            "back to traditional IO", e);
      copyFileWithoutNio(originFile, destinationFile);
    } finally {
      closeQuietly(inputStream);
      closeQuietly(outputStream);
    }
  }

  public static void delete(File file) {
    if (!file.delete())
      throw new PersistenceException("Failed to delete file: " + file);
  }

  private static void copyFileWithoutNio(File originFile, File destinationFile) {
    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;
    try {
      inputStream = new FileInputStream(originFile);

      if (!destinationFile.exists())
        destinationFile.createNewFile();
      outputStream = new FileOutputStream(destinationFile);

      byte[] buf = new byte[8192];
      int i = 0;
      while ((i = inputStream.read(buf)) != -1) {
        outputStream.write(buf, 0, i);
      }
    } catch (IOException e) {
      throw new PersistenceException(e);
    } finally {
      closeQuietly(inputStream);
      closeQuietly(outputStream);
    }
  }
}
