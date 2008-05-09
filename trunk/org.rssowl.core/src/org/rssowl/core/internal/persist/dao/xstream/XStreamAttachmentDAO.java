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
package org.rssowl.core.internal.persist.dao.xstream;

import org.rssowl.core.internal.persist.Attachment;
import org.rssowl.core.internal.persist.service.ManualEventManager;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.dao.IAttachmentDAO;
import org.rssowl.core.persist.event.AttachmentEvent;
import org.rssowl.core.persist.event.AttachmentListener;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.persist.service.PersistenceException;

import com.thoughtworks.xstream.XStream;

import java.io.File;
import java.util.Collection;

public class XStreamAttachmentDAO extends XStreamEntityDAO<IAttachment, AttachmentListener, AttachmentEvent> implements IAttachmentDAO {

  public XStreamAttachmentDAO(File baseDir, XStream xStream, IDGenerator idGenerator, ManualEventManager eventManager) {
    super(Attachment.class, baseDir, xStream, idGenerator, eventManager);
  }

  public boolean exists(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public IAttachment load(long id) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public long countAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void delete(IAttachment persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void deleteAll(Collection<IAttachment> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public Collection<IAttachment> loadAll() throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public IAttachment save(IAttachment persistable) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

  public void saveAll(Collection<IAttachment> persistables) throws PersistenceException {
    throw new UnsupportedOperationException();
  }

}
