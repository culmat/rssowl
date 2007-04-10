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

package org.rssowl.core.interpreter.internal;

import org.jdom.Attribute;
import org.jdom.Element;
import org.rssowl.core.Owl;
import org.rssowl.core.interpreter.INamespaceHandler;
import org.rssowl.core.model.persist.ICategory;
import org.rssowl.core.model.persist.IEntity;
import org.rssowl.core.model.persist.IFeed;
import org.rssowl.core.model.persist.IGuid;
import org.rssowl.core.model.persist.IImage;
import org.rssowl.core.model.persist.INews;
import org.rssowl.core.model.persist.IPersistable;
import org.rssowl.core.model.persist.IPerson;
import org.rssowl.core.model.persist.ISource;
import org.rssowl.core.model.persist.ITextInput;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.URIUtils;

/**
 * Handler for the Dublin Core Namespace.
 * <p>
 * Namespace Prefix: dc<br>
 * Namespace URI: http://purl.org/dc/elements/1.1/
 * </p>
 * 
 * @author bpasero
 */
public class DublinCoreNamespaceHandler implements INamespaceHandler {

  /*
   * @see org.rssowl.core.interpreter.INamespaceHandler#processElement(org.jdom.Element,
   * org.rssowl.core.interpreter.types.IExtendableType)
   */
  public void processElement(Element element, IPersistable type) {
    String name = element.getName().toLowerCase();

    /* Title */
    if ("title".equals(name)) { //$NON-NLS-1$
      if (type instanceof IFeed)
        ((IFeed) type).setTitle(element.getText());
      else if (type instanceof INews)
        ((INews) type).setTitle(element.getText());
      else if (type instanceof IImage)
        ((IImage) type).setTitle(element.getText());
      else if (type instanceof ITextInput)
        ((ITextInput) type).setTitle(element.getText());
    }

    /* Description */
    else if ("description".equals(name)) { //$NON-NLS-1$
      if (type instanceof IFeed)
        ((IFeed) type).setDescription(element.getText());
      else if (type instanceof INews)
        ((INews) type).setDescription(element.getText());
      else if (type instanceof IImage)
        ((IImage) type).setDescription(element.getText());
      else if (type instanceof ITextInput)
        ((ITextInput) type).setDescription(element.getText());
    }

    /* Date */
    else if ("date".equals(name)) { //$NON-NLS-1$
      if (type instanceof IFeed)
        ((IFeed) type).setPublishDate(DateUtils.parseDate(element.getText()));
      else if (type instanceof INews)
        ((INews) type).setPublishDate(DateUtils.parseDate(element.getText()));
    }

    /* Creator / Publisher */
    else if ("creator".equals(name) || "publisher".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
      IPerson person = Owl.getInterpreter().getTypesFactory().createPerson(type);
      person.setName(element.getText());

      if (type instanceof IFeed)
        ((IFeed) type).setAuthor(person);
      else if (type instanceof INews)
        ((INews) type).setAuthor(person);
    }

    /* Language */
    else if ("language".equals(name) && type instanceof IFeed) //$NON-NLS-1$
      ((IFeed) type).setLanguage(element.getText());

    /* Rights */
    else if ("rights".equals(name) && type instanceof IFeed) //$NON-NLS-1$
      ((IFeed) type).setCopyright(element.getText());

    /* Subject */
    else if ("subject".equals(name) && type instanceof IEntity) { //$NON-NLS-1$
      ICategory category = Owl.getInterpreter().getTypesFactory().createCategory((IEntity) type);
      category.setName(element.getText());

      if (type instanceof IFeed)
        ((IFeed) type).addCategory(category);
      else if (type instanceof INews)
        ((INews) type).addCategory(category);
    }

    /* Identifier */
    else if ("identifier".equals(name) && type instanceof INews) { //$NON-NLS-1$
      IGuid guid = Owl.getInterpreter().getTypesFactory().createGuid((INews) type);
      guid.setValue(element.getText());
      ((INews) type).setGuid(guid);
    }

    /* Source */
    else if ("source".equals(name) && type instanceof INews) { //$NON-NLS-1$
      ISource source = Owl.getInterpreter().getTypesFactory().createSource((INews) type);
      source.setLink(URIUtils.createURI(element.getText()));
      ((INews) type).setSource(source);
    }
  }

  /*
   * @see org.rssowl.core.interpreter.INamespaceHandler#processAttribute(org.jdom.Attribute,
   * org.rssowl.core.interpreter.types.IExtendableType)
   */
  public void processAttribute(Attribute attribute, IPersistable type) {}
}