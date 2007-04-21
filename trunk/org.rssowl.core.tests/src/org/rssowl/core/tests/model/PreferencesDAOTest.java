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

package org.rssowl.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.model.dao.PersistenceException;
import org.rssowl.core.persist.dao.IPreferencesDAO;
import org.rssowl.core.persist.pref.PreferencesEvent;
import org.rssowl.core.persist.pref.PreferencesListener;
import org.rssowl.core.tests.TestUtils;

import java.util.Arrays;

/**
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
@SuppressWarnings("nls")
public class PreferencesDAOTest {
  private IPreferencesDAO fDao;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    Owl.getPersistenceService().getModelSearch().shutdown();
    fDao = Owl.getPersistenceService().getPreferencesDAO();
  }

  /**
   * Test adding and getting boolean Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetBoolean() throws Exception {
    String key1 = "key1";
    String key2 = "key2";
    String key3 = "key3";

    fDao.putBoolean(key1, true);
    fDao.putBoolean(key2, true);
    fDao.putBoolean(key3, false);

    assertEquals(Boolean.TRUE, fDao.getBoolean(key1));
    assertEquals(Boolean.TRUE, fDao.getBoolean(key2));
    assertEquals(Boolean.FALSE, fDao.getBoolean(key3));

    fDao.putBoolean(key2, false);

    assertEquals(Boolean.TRUE, fDao.getBoolean(key1));
    assertEquals(Boolean.FALSE, fDao.getBoolean(key2));
    assertEquals(Boolean.FALSE, fDao.getBoolean(key3));
  }

  /**
   * @throws Exception
   */
  @Test
  public final void testActivation() throws Exception   {
    String key = "key";
    fDao.putBoolean(key , true);
    System.gc();
    assertEquals(Boolean.TRUE, fDao.getBoolean(key));
    String anotherKey = "anotherKey";
    String[] longs = new String[] { "2", "3", "5"};
    fDao.putStrings(anotherKey, longs);
    longs = null;
    System.gc();
    assertEquals(3, fDao.getStrings(anotherKey).length);
  }

  /**
   * Test adding and getting Strings Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetStrings() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      String[] value1 = new String[] { "value1.1", "value1.2", "value1.3" };
      String[] value2 = new String[] { "value2.1", "value2.2", "value2.3" };
      String[] value3 = new String[] { "value3.1", "value3.2", "value3.3" };

      fDao.putStrings(key1, value1);
      fDao.putStrings(key2, value2);
      fDao.putStrings(key3, value3);

      assertEquals(value1, fDao.getStrings(key1));
      assertEquals(value2, fDao.getStrings(key2));
      assertEquals(value3, fDao.getStrings(key3));

      value2 = new String[] { "newvalue1.1", "newvalue1.2", "newvalue1.3" };
      fDao.putStrings(key2, value2);

      assertEquals(value1, fDao.getStrings(key1));
      assertEquals(value2, fDao.getStrings(key2));
      assertEquals(value3, fDao.getStrings(key3));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test adding and getting Longs Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetLongs() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      long[] value1 = new long[] { 11, 12, 13 };
      long[] value2 = new long[] { 21, 22, 23 };
      long[] value3 = new long[] { 31, 32, 33 };

      fDao.putLongs(key1, value1);
      fDao.putLongs(key2, value2);
      fDao.putLongs(key3, value3);

      assertEquals(true, Arrays.equals(value1, fDao.getLongs(key1)));
      assertEquals(true, Arrays.equals(value2, fDao.getLongs(key2)));
      assertEquals(true, Arrays.equals(value3, fDao.getLongs(key3)));

      value2 = new long[] { 110, 120, 130 };
      fDao.putLongs(key2, value2);

      assertEquals(true, Arrays.equals(value1, fDao.getLongs(key1)));
      assertEquals(true, Arrays.equals(value2, fDao.getLongs(key2)));
      assertEquals(true, Arrays.equals(value3, fDao.getLongs(key3)));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test adding and getting Ints Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetInts() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      int[] value1 = new int[] { 11, 12, 13 };
      int[] value2 = new int[] { 21, 22, 23 };
      int[] value3 = new int[] { 31, 32, 33 };

      fDao.putIntegers(key1, value1);
      fDao.putIntegers(key2, value2);
      fDao.putIntegers(key3, value3);

      assertEquals(true, Arrays.equals(value1, fDao.getIntegers(key1)));
      assertEquals(true, Arrays.equals(value2, fDao.getIntegers(key2)));
      assertEquals(true, Arrays.equals(value3, fDao.getIntegers(key3)));

      value2 = new int[] { 110, 120, 130 };
      fDao.putIntegers(key2, value2);

      assertEquals(true, Arrays.equals(value1, fDao.getIntegers(key1)));
      assertEquals(true, Arrays.equals(value2, fDao.getIntegers(key2)));
      assertEquals(true, Arrays.equals(value3, fDao.getIntegers(key3)));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test adding and getting Long Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetLong() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      long value1 = 10;
      long value2 = 15;
      long value3 = 20;

      fDao.putLong(key1, value1);
      fDao.putLong(key2, value2);
      fDao.putLong(key3, value3);

      assertEquals(Long.valueOf(value1), fDao.getLong(key1));
      assertEquals(Long.valueOf(value2), fDao.getLong(key2));
      assertEquals(Long.valueOf(value3), fDao.getLong(key3));

      value3 = 5;
      fDao.putLong(key3, value3);

      assertEquals(Long.valueOf(value1), fDao.getLong(key1));
      assertEquals(Long.valueOf(value2), fDao.getLong(key2));
      assertEquals(Long.valueOf(value3), fDao.getLong(key3));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test adding and getting String Preference.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetString() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      String value1 = "value1";
      String value2 = "value2";
      String value3 = "value3";

      fDao.putString(key1, value1);
      fDao.putString(key2, value2);
      fDao.putString(key3, value3);

      assertEquals(value1, fDao.getString(key1));
      assertEquals(value2, fDao.getString(key2));
      assertEquals(value3, fDao.getString(key3));

      value1 = "newValue1";
      fDao.putString(key1, value1);

      assertEquals(value1, fDao.getString(key1));
      assertEquals(value2, fDao.getString(key2));
      assertEquals(value3, fDao.getString(key3));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test adding and getting Integer Preferences.
   *
   * @throws Exception
   */
  @Test
  public final void testPutGetInteger() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";

      int value1 = 10;
      int value2 = 15;
      int value3 = 20;

      fDao.putInteger(key1, value1);
      fDao.putInteger(key2, value2);
      fDao.putInteger(key3, value3);

      assertEquals(Integer.valueOf(value1), fDao.getInteger(key1));
      assertEquals(Integer.valueOf(value2), fDao.getInteger(key2));
      assertEquals(Integer.valueOf(value3), fDao.getInteger(key3));

      value3 = 5;
      fDao.putInteger(key3, value3);

      assertEquals(Integer.valueOf(value1), fDao.getInteger(key1));
      assertEquals(Integer.valueOf(value2), fDao.getInteger(key2));
      assertEquals(Integer.valueOf(value3), fDao.getInteger(key3));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test Deleting Preferences
   *
   * @throws Exception
   */
  @Test
  public final void testDelete() throws Exception {
    try {
      String key1 = "key1";
      String key2 = "key2";
      String key3 = "key3";
      String key4 = "key4";
      boolean value1 = true;
      String value2 = "value2";
      int value3 = 34;
      String[] value4 = new String[] { "value4.1", "value4.2", "value4.3" };

      fDao.putBoolean(key1, value1);
      fDao.putString(key2, value2);
      fDao.putInteger(key3, value3);
      fDao.putStrings(key4, value4);

      assertEquals(Boolean.valueOf(value1), fDao.getBoolean(key1));
      assertEquals(value2, fDao.getString(key2));
      assertEquals(Integer.valueOf(value3), fDao.getInteger(key3));
      assertEquals(value4, fDao.getStrings(key4));

      boolean deleted = fDao.delete(key3);
      assertTrue(deleted);
      assertEquals(Boolean.valueOf(value1), fDao.getBoolean(key1));
      assertEquals(value2, fDao.getString(key2));
      assertNull("key3 should be null, but it is: " + key3, fDao.getInteger(key3));
      assertEquals(value4, fDao.getStrings(key4));

      deleted = fDao.delete(key1);
      assertTrue(deleted);
      assertNull(fDao.getBoolean(key1));
      assertEquals(value2, fDao.getString(key2));
      assertNull(fDao.getInteger(key3));
      assertEquals(value4, fDao.getStrings(key4));

      /* Call delete on key that has already been deleted */
      deleted = fDao.delete(key1);
      assertFalse(deleted);
      assertNull(fDao.getBoolean(key1));
      assertEquals(value2, fDao.getString(key2));
      assertNull(fDao.getInteger(key3));
      assertEquals(value4, fDao.getStrings(key4));

      deleted = fDao.delete(key4);
      assertTrue(deleted);
      assertNull(fDao.getBoolean(key1));
      assertEquals(value2, fDao.getString(key2));
      assertNull(fDao.getInteger(key3));
      assertNull(fDao.getStrings(key4));

      deleted = fDao.delete(key2);
      assertTrue(deleted);
      assertNull(fDao.getBoolean(key1));
      assertNull(fDao.getString(key2));
      assertNull(fDao.getInteger(key3));
      assertNull(fDao.getStrings(key4));
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Test the Events for getting Add, Update and Delete Events.
   *
   * @throws Exception
   */
  @Test
  public void testPreferenceEvents() throws Exception {
    PreferencesListener prefListener = null;
    try {
      final String key1 = "key1";
      boolean value1 = true;

      final String key2 = "key2";
      int value2 = 1;

      final String key3 = "key3";
      String value3 = "value";

      final String key4 = "key4";
      String value4[] = new String[] { "1", "2", "3", "4" };

      /* Event Handling */
      final boolean additionEvents[] = new boolean[4];
      final boolean updatedEvents[] = new boolean[4];
      final boolean deletionEvents[] = new boolean[4];

      prefListener = new PreferencesListener() {
        public void preferenceAdded(PreferencesEvent event) {
          String key = event.getKey();
          if (key1.equals(key))
            additionEvents[0] = event.getBoolean().booleanValue();
          else if (key2.equals(key))
            additionEvents[1] = event.getInteger().intValue() == 1;
          else if (key3.equals(key))
            additionEvents[2] = event.getString().equals("value");
          else if (key4.equals(key))
            additionEvents[3] = Arrays.equals(event.getStrings(), new String[] { "1", "2", "3", "4" });
        }

        public void preferenceUpdated(PreferencesEvent event) {
          String key = event.getKey();
          if (key1.equals(key))
            updatedEvents[0] = !event.getBoolean().booleanValue();
          else if (key2.equals(key))
            updatedEvents[1] = event.getInteger().intValue() == 0;
          else if (key3.equals(key))
            updatedEvents[2] = event.getString().equals("updated_value");
          else if (key4.equals(key))
            updatedEvents[3] = Arrays.equals(event.getStrings(), new String[] { "4", "3", "2", "1" });
        }

        public void preferenceDeleted(PreferencesEvent event) {
          String key = event.getKey();
          if (key1.equals(key))
            deletionEvents[0] = true;
          else if (key2.equals(key))
            deletionEvents[1] = true;
          else if (key3.equals(key))
            deletionEvents[2] = true;
          else if (key4.equals(key))
            deletionEvents[3] = true;
        }
      };
      Owl.getListenerService().addPreferencesListener(prefListener);

      /* Add some Preferences */
      fDao.putBoolean(key1, value1);
      fDao.putInteger(key2, value2);
      fDao.putString(key3, value3);
      fDao.putStrings(key4, value4);

      /* Update some Preferences */
      fDao.putBoolean(key1, false);
      fDao.putInteger(key2, 0);
      fDao.putString(key3, "updated_value");
      fDao.putStrings(key4, new String[] { "4", "3", "2", "1" });

      /* Delete some Preferences */
      fDao.delete(key1);
      fDao.delete(key2);
      fDao.delete(key3);
      fDao.delete(key4);

      /* Asserts */
      for (boolean element : additionEvents)
        assertTrue("Missing Preference Added Event", element);

      for (boolean element : updatedEvents)
        assertTrue("Missing Preference Updated Event", element);

      for (boolean element : deletionEvents)
        assertTrue("Missing Preference Deleted Event", element);
    } finally {
      if (prefListener != null)
        Owl.getListenerService().removePreferencesListener(prefListener);
    }
  }

  /**
   * Save a single-entry String-Array.
   *
   * @throws Exception
   */
  @Test
  public void testSaveSingleEntryStringArray() throws Exception {
    try {
      fDao.putStrings("Foo", new String[] { "Bar" });
      fDao.getStrings("Foo");
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * Save Strings that contain equal values.
   *
   * @throws Exception
   */
  @Test
  public void testSaveStringsDuplicate() throws Exception {
    fDao.putStrings("Foo", new String[] { "1", "2", "3", "1", "2", "3" });
    fDao.putStrings("Foo", new String[] { "1", "2", "3", "1", "2", "3" });
  }

  /**
   * Save an array of strings with duplicate elements.
   *
   * @throws Exception
   */
  @Test
  public void testSaveArrayWithDuplicateStrings() throws Exception {
    fDao.putStrings("Foo", new String[] { "1", "2", "3", "1", "2", "3" });
  }

  /**
   * Saves an array and then updates it.
   * @throws Exception
   */
  @Test
  public void testUpdateArray() throws Exception {
    String key = "Foo";
    fDao.putStrings(key, new String[] { "1", "2", "3", "1", "2", "3" });
    String[] updatedStrings = new String[] { "1", "3", "2" };
    fDao.putStrings(key, updatedStrings);
    String[] savedStrings = fDao.getStrings(key);
    assertEquals(updatedStrings, savedStrings);
  }
}