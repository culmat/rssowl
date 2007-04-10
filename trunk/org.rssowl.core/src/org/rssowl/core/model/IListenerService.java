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

package org.rssowl.core.model;

import org.rssowl.core.model.events.AttachmentEvent;
import org.rssowl.core.model.events.AttachmentListener;
import org.rssowl.core.model.events.BookMarkEvent;
import org.rssowl.core.model.events.BookMarkListener;
import org.rssowl.core.model.events.CategoryEvent;
import org.rssowl.core.model.events.CategoryListener;
import org.rssowl.core.model.events.FeedEvent;
import org.rssowl.core.model.events.FeedListener;
import org.rssowl.core.model.events.FolderEvent;
import org.rssowl.core.model.events.FolderListener;
import org.rssowl.core.model.events.LabelEvent;
import org.rssowl.core.model.events.LabelListener;
import org.rssowl.core.model.events.NewsEvent;
import org.rssowl.core.model.events.NewsListener;
import org.rssowl.core.model.events.PersonEvent;
import org.rssowl.core.model.events.PersonListener;
import org.rssowl.core.model.events.SearchConditionEvent;
import org.rssowl.core.model.events.SearchConditionListener;
import org.rssowl.core.model.events.SearchMarkEvent;
import org.rssowl.core.model.events.SearchMarkListener;
import org.rssowl.core.model.persist.pref.PreferencesEvent;
import org.rssowl.core.model.persist.pref.PreferencesListener;

import java.util.Set;

/**
 * @author bpasero
 */
public interface IListenerService {

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * an attachment is added, deleted or updated, by calling one of the methods
   * defined in the <code>AttachmentListener</code> interface.
   *
   * @param listener The Listener to add to the List of Listeners.
   */
  void addAttachmentListener(AttachmentListener listener);

  /**
   * Removes the listener from the collection of listeners who will be notified
   * when an attachment is added, deleted or updated.
   *
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeAttachmentListener(AttachmentListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyAttachmentAdded(final Set<AttachmentEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyAttachmentDeleted(final Set<AttachmentEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyAttachmentUpdated(final Set<AttachmentEvent> events);

  /**
   * Adds the listener to the collection of listeners who will be notified when
   * a bookmark is added, deleted or updated, by calling one of the methods
   * defined in the <code>BookMarkListener</code> interface.
   *
   * @param listener The Listener to add to the List of Listeners.
   */
  void addBookMarkListener(BookMarkListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeBookMarkListener(BookMarkListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyBookMarkAdded(final Set<BookMarkEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyBookMarkDeleted(final Set<BookMarkEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyBookMarkUpdated(final Set<BookMarkEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addCategoryListener(CategoryListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeCategoryListener(CategoryListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyCategoryAdded(final Set<CategoryEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyCategoryDeleted(final Set<CategoryEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyCategoryUpdated(final Set<CategoryEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addFeedListener(FeedListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeFeedListener(FeedListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFeedAdded(final Set<FeedEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFeedDeleted(final Set<FeedEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFeedUpdated(final Set<FeedEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addFolderListener(FolderListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeFolderListener(FolderListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFolderAdded(final Set<FolderEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFolderDeleted(final Set<FolderEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyFolderUpdated(final Set<FolderEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addLabelListener(LabelListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeLabelListener(LabelListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyLabelAdded(final Set<LabelEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyLabelDeleted(final Set<LabelEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyLabelUpdated(final Set<LabelEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addNewsListener(NewsListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeNewsListener(NewsListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyNewsAdded(final Set<NewsEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyNewsDeleted(final Set<NewsEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifyNewsUpdated(final Set<NewsEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addPersonListener(PersonListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removePersonListener(PersonListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyPersonAdded(final Set<PersonEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyPersonDeleted(final Set<PersonEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifyPersonUpdated(final Set<PersonEvent> events);

  /**
   * @param listener The Listener to add to the List of Listeners.
   */
  void addSearchMarkListener(SearchMarkListener listener);

  /**
   * @param listener The Listener to remove from the List of Listeners.
   */
  void removeSearchMarkListener(SearchMarkListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifySearchMarkAdded(final Set<SearchMarkEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifySearchMarkDeleted(final Set<SearchMarkEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  @SuppressWarnings("nls")
  void notifySearchMarkUpdated(final Set<SearchMarkEvent> events);

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferenceAdded(final PreferencesEvent event);

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferencesDeleted(final PreferencesEvent event);

  /**
   * @param event The <code>ModelEvent</code> for the affected type.
   */
  void notifyPreferencesUpdated(final PreferencesEvent event);

  /**
   * @param listener the PreferencesListener to add to the list of listeners.
   */
  void addPreferencesListener(PreferencesListener listener);

  /**
   * @param listener the PreferencesListener to remove to the list of listeners.
   */
  void removePreferencesListener(PreferencesListener listener);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifySearchConditionAdded(final Set<SearchConditionEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifySearchConditionDeleted(final Set<SearchConditionEvent> events);

  /**
   * @param events The <code>ModelEvent</code> for the affected type.
   */
  void notifySearchConditionUpdated(final Set<SearchConditionEvent> events);

  /**
   * <p>
   * Note: The default implementation of RSSOwl will <em>never</em> issue any
   * of Event of this kind. But it might be used in 3d-Party contributions.
   * </p>
   *
   * @param listener the SearchConditionListener to add to the list of
   * listeners.
   */
  void addSearchConditionListener(SearchConditionListener listener);

  /**
   * @param listener the SearchConditionListener to remove to the list of
   * listeners.
   */
  void removeSearchConditionListener(SearchConditionListener listener);
}