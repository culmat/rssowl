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

package org.rssowl.core.interpreter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.interpreter.internal.DefaultInterpreterTypesFactory;
import org.rssowl.core.interpreter.internal.DefaultSaxParserImpl;
import org.rssowl.core.model.types.IEntity;
import org.rssowl.core.model.types.IFeed;
import org.rssowl.core.util.ExtensionUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Main class of the Interpreter. A contributed or default-JDKs XML-Parser is
 * used to parse the given InputStream into a <code>org.jdom.Document</code>.
 * The Document is then passed to the Contribution responsible for the given
 * Format.
 * </p>
 * The following kind of Extensions are possible:
 * <ul>
 * <li>SAXParser allows to contribute the XML Parser to be used</li>
 * <li>FormatInterpreter allow to contribute Interpreters based on a XML Format</li>
 * <li>NamespaceHandler allow to contribute processing for Namespaces</li>
 * <li>ElementHandler allow to contribute custom processing for Elements</li>
 * <li>TypesFactory allows to contribute your own Model to be used by the
 * Interpreter</li>
 * </ul>
 * 
 * @author bpasero
 */
public class Interpreter {

  /* ID for SAXParser Contribution */
  private static final String SAXPARSER_EXTENSION_POINT = "org.rssowl.core.SAXParser"; //$NON-NLS-1$

  /* ID for FormatInterpreter Contributions */
  private static final String FORMATINTERPRETER_EXTENSION_POINT = "org.rssowl.core.FormatInterpreter"; //$NON-NLS-1$

  /* ID for TypeImporter Contributions */
  private static final String TYPEIMPORTER_EXTENSION_POINT = "org.rssowl.core.TypeImporter"; //$NON-NLS-1$

  /* ID for InterpreterTypesFactory Contributions */
  private static final String INTERPRETER_TYPESFACTORY_EXTENSION_POINT = "org.rssowl.core.InterpreterTypesFactory"; //$NON-NLS-1$

  /* ID for NamespaceHandler Contributions */
  private static final String NSHANDLER_EXTENSION_POINT = "org.rssowl.core.NamespaceHandler"; //$NON-NLS-1$

  /* ID for ElementHandler Contributions */
  private static final String ELHANDLER_EXTENSION_POINT = "org.rssowl.core.ElementHandler"; //$NON-NLS-1$

  /* Singleton Instance */
  private static Interpreter fInstance;

  private Map<String, IFormatInterpreter> fFormatInterpreters;
  private Map<String, ITypeImporter> fTypeImporters;
  private Map<String, INamespaceHandler> fNamespaceHandlers;
  private Map<String, IElementHandler> fElementHandlers;
  private IInterpreterTypesFactory fTypesFactory;
  private IXMLParser fXMLParserImpl;

  /* Interpreter only accessable via Singleton */
  private Interpreter() {
    startup();
  }

  /**
   * Parse the given InputStream into a <code>org.jdom.Document</code> and
   * delegate the Interpretation to the contributed FormatInterpreters.
   * 
   * @param inS The InputStream to Interpret as <code>IFeed</code>.
   * @param feed An instance of <code>IFeed</code> that stores the
   * interpretion.
   * @throws ParserException In case of an Error while Parsing.
   * @throws InterpreterException In case of an Error while Interpreting.
   */
  public void interpret(InputStream inS, IFeed feed) throws ParserException, InterpreterException {
    Document document = fXMLParserImpl.parse(inS);

    interpretJDomDocument(document, feed);
  }

  /**
   * Interpret the given <code>org.w3c.dom.Document</code> as Feed by
   * delegating the Interpretation to the contributed FormatInterpreters.
   * 
   * @param w3cDocument The Document to interpret as <code>IFeed</code>.
   * @param feed An instance of <code>IFeed</code> that stores the
   * interpretion.
   * @throws InterpreterException In case of an Error while Interpreting.
   */
  public void interpretW3CDocument(org.w3c.dom.Document w3cDocument, IFeed feed) throws InterpreterException {
    DOMBuilder domBuilder = new DOMBuilder();
    Document jDomDocument = domBuilder.build(w3cDocument);

    interpretJDomDocument(jDomDocument, feed);
  }

  /**
   * Interpret the given <code>org.jdom.Document</code> as Feed by delegating
   * the Interpretation to the contributed FormatInterpreters.
   * 
   * @param document The Document to interpret as <code>IFeed</code>.
   * @param feed An instance of <code>IFeed</code> that stores the
   * interpretion.
   * @throws InterpreterException In case of an Error while Interpreting.
   */
  public void interpretJDomDocument(Document document, IFeed feed) throws InterpreterException {

    /* A Root Element is required */
    if (!document.hasRootElement())
      throw new InterpreterException(Activator.getDefault().createErrorStatus("Document has no Root Element set!", null)); //$NON-NLS-1$

    /* Determine Format of the Feed */
    String format = document.getRootElement().getName().toLowerCase();

    /* A Interpreter is required */
    if (!fFormatInterpreters.containsKey(format))
      throw new UnknownFormatException(Activator.getDefault().createErrorStatus("No Interpreter found for Format \"" + format + "\"", null)); //$NON-NLS-1$//$NON-NLS-2$

    /* Interpret Document into a Feed */
    fFormatInterpreters.get(format).interpret(document, feed);
  }

  /**
   * Imports the given Document as OPML into Types and returns them.
   * 
   * @param inS The InputStream to Interpret as Document.
   * @return Returns the Types imported from the Document.
   * @throws InterpreterException In case of an Error while Interpreting.
   * @throws ParserException In case of an Error while Parsing.
   */
  public List< ? extends IEntity> importFrom(InputStream inS) throws InterpreterException, ParserException {
    Activator.getDefault().logInfo("Importing the InputStream into a Type... [ org.rssowl.core.interpreter#importOPML(InputStream inS) ]"); //$NON-NLS-1$
    Document document = fXMLParserImpl.parse(inS);

    /* A Root Element is required */
    if (!document.hasRootElement())
      throw new InterpreterException(Activator.getDefault().createErrorStatus("Document has no Root Element set!", null)); //$NON-NLS-1$

    /* Determine Format of the Feed */
    String format = document.getRootElement().getName().toLowerCase();

    /* An Importer is required */
    if (!fTypeImporters.containsKey(format))
      throw new UnknownFormatException(Activator.getDefault().createErrorStatus("No Importer found for Format \"" + format + "\"", null)); //$NON-NLS-1$//$NON-NLS-2$

    /* Import Type from the Document */
    return fTypeImporters.get(format).importFrom(document);
  }

  /**
   * Get the Factory responsible for creating the interpreter types.
   * 
   * @return The Factory responsible for creating the interpreter types.
   */
  public IInterpreterTypesFactory getTypesFactory() {
    return fTypesFactory;
  }

  /**
   * Get the Namespace Handler for the given Namespace or NULL if not
   * contributed.
   * 
   * @param namespaceUri The Namespace URI as String.
   * @return The Namespace Handler for the given Namespace or NULL if not
   * contributed.
   */
  public INamespaceHandler getNamespaceHandler(String namespaceUri) {
    return fNamespaceHandlers.get(namespaceUri);
  }

  /**
   * Get the Element Handler for the given Element and Namespace or NULL if not
   * contributed.
   * 
   * @param elementName The Name of the Element.
   * @param rootName The Name of the root element of the used Format.
   * @return The Namespace Handler for the given Namespace or NULL if not
   * contributed.
   */
  public IElementHandler getElementHandler(String elementName, String rootName) {
    if (fElementHandlers != null)
      return fElementHandlers.get(elementName.toLowerCase() + rootName.toLowerCase());
    return null;
  }

  /**
   * @return The Singleton Instance.
   */
  public static Interpreter getDefault() {
    if (fInstance == null)
      fInstance = new Interpreter();
    return fInstance;
  }

  private void startup() {

    /* Load and Init XMLParser */
    fXMLParserImpl = loadXMLParserImpl();
    Assert.isNotNull(fXMLParserImpl);
    SafeRunner.run(new ISafeRunnable() {

      /* Use Default XML Parser Impl */
      public void handleException(Throwable exception) {
        fXMLParserImpl = new DefaultSaxParserImpl();
        try {
          fXMLParserImpl.init();
        } catch (ParserException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }

      /* Try Contribution */
      public void run() throws Exception {
        fXMLParserImpl.init();
      }
    });

    /* Load Format Interpreters */
    fFormatInterpreters = new HashMap<String, IFormatInterpreter>();
    loadFormatInterpreters();

    /* Load Type Importers */
    fTypeImporters = new HashMap<String, ITypeImporter>();
    loadTypeImporters();

    /* Load Types Factory */
    fTypesFactory = loadTypesFactory();
    Assert.isNotNull(fTypesFactory);

    /* Load Namespace Handlers */
    fNamespaceHandlers = new HashMap<String, INamespaceHandler>();
    loadNamespaceHandlers();

    /* Load Element Handlers */
    loadElementHandlers();
  }

  private void loadNamespaceHandlers() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(NSHANDLER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String namespaceUri = element.getAttribute("namespaceURI"); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fNamespaceHandlers.containsKey(namespaceUri) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fNamespaceHandlers.put(namespaceUri, (INamespaceHandler) element.createExecutableExtension("class"));//$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  private void loadElementHandlers() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(ELHANDLER_EXTENSION_POINT);

    if (elements.length > 0)
      fElementHandlers = new HashMap<String, IElementHandler>();

    for (IConfigurationElement element : elements) {
      String elementName = element.getAttribute("elementName").toLowerCase(); //$NON-NLS-1$
      String rootName = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

      /* Let 3d-Party contributions override our contributions */
      if (fElementHandlers.containsKey(elementName + rootName) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
        continue;

      try {
        fElementHandlers.put(elementName + rootName, (IElementHandler) element.createExecutableExtension("class"));//$NON-NLS-1$
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  /* Load Interpreter Types Factory contribution */
  private IInterpreterTypesFactory loadTypesFactory() {
    IInterpreterTypesFactory defaultFactory = new DefaultInterpreterTypesFactory();
    return (IInterpreterTypesFactory) ExtensionUtils.loadSingletonExecutableExtension(INTERPRETER_TYPESFACTORY_EXTENSION_POINT, defaultFactory);
  }

  private void loadFormatInterpreters() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(FORMATINTERPRETER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String format = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fFormatInterpreters.containsKey(format) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fFormatInterpreters.put(format, (IFormatInterpreter) element.createExecutableExtension("class")); //$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  private void loadTypeImporters() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(TYPEIMPORTER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String format = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fTypeImporters.containsKey(format) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fTypeImporters.put(format, (ITypeImporter) element.createExecutableExtension("class")); //$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  /* Load XML Parser contribution */
  private IXMLParser loadXMLParserImpl() {
    IXMLParser defaultParser = new DefaultSaxParserImpl();
    return (IXMLParser) ExtensionUtils.loadSingletonExecutableExtension(SAXPARSER_EXTENSION_POINT, defaultParser);
  }
}