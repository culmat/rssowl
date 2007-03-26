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

package org.rssowl.core.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.rssowl.core.tests.controller.ControllerTestLocal;
import org.rssowl.core.tests.controller.ReloadTestLocal;
import org.rssowl.core.tests.interpreter.ImporterTest;
import org.rssowl.core.tests.interpreter.InterpreterTest;
import org.rssowl.core.tests.model.ApplicationLayerTest;
import org.rssowl.core.tests.model.DBManagerTest;
import org.rssowl.core.tests.model.ModelSearchTest;
import org.rssowl.core.tests.model.ModelTest1;
import org.rssowl.core.tests.model.ModelTest2;
import org.rssowl.core.tests.model.ModelTest3;
import org.rssowl.core.tests.model.PreferencesDAOTest;
import org.rssowl.core.tests.model.PreferencesScopeTest;
import org.rssowl.core.tests.util.MergeUtilsTest;

/**
 * Test-Suite for Core-Tests that are not requiring Network-Access.
 * 
 * @author bpasero
 * @author Ismael Juma (ismael@juma.me.uk)
 */
@RunWith(Suite.class)
@SuiteClasses({
  InterpreterTest.class,
  ImporterTest.class,
  ControllerTestLocal.class,
  ReloadTestLocal.class,
  ModelTest1.class,
  ModelTest2.class,
  ModelTest3.class,
  PreferencesDAOTest.class,
  ApplicationLayerTest.class,
  ModelSearchTest.class,
  DBManagerTest.class,
  PreferencesScopeTest.class,
  MergeUtilsTest.class
})
public class LocalTests {}