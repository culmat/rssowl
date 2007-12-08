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
package org.rssowl.core.internal.persist.migration;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.service.ConfigurationFactory;
import org.rssowl.core.internal.persist.service.Migration;
import org.rssowl.core.persist.ILabel;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;

import java.util.List;

/**
 * Migration from version 1 (2.0M6) to version 2 (2.0M7) -> Bug #551
 */
public class Migration1To2 implements Migration   {

  public int getDestinationFormat() {
    return 2;
  }

  public int getOriginFormat() {
    return 1;
  }

  public boolean migrate(ConfigurationFactory configFactory, String dbFileName, IProgressMonitor progressMonitor) {
    final int totalProgress = 100;
    int totalProgressIncremented = 0;
    progressMonitor.beginTask("Migrating data", totalProgress);

    ObjectContainer oc = Db4o.openFile(configFactory.createConfiguration(), dbFileName);

    List<News> newsList = oc.query(News.class);
    int newsCountPerIncrement = newsList.size() / totalProgress;

    int i = 0;
    for (News news : newsList) {
      oc.activate(news, Integer.MAX_VALUE);
      String LabelFieldName = "fLabel";
      ILabel label = (ILabel) MigrationHelper.getFieldValue(news, LabelFieldName);
      if (label != null) {
        news.addLabel(label);
      }
      MigrationHelper.setField(news, LabelFieldName, null);
      oc.ext().set(news, Integer.MAX_VALUE);
      ++i;
      if (newsCountPerIncrement == 0) {
        int progressIncrement = totalProgress / newsList.size();
        totalProgressIncremented += progressIncrement;
        progressMonitor.worked(progressIncrement);
      } else if (i % newsCountPerIncrement == 0) {
        totalProgressIncremented++;
        progressMonitor.worked(1);
      }
    }
    oc.commit();
    oc.close();

    /* Not sure if this is needed anymore, but not taking chances for now */
    /* Disable indices to force them to be re-created */
    Configuration config = configFactory.createConfiguration();
    config.objectClass(org.rssowl.core.internal.persist.BookMark.class).objectField("fFeedLink").indexed(false); //$NON-NLS-1$
    config.objectClass(org.rssowl.core.internal.persist.ConditionalGet.class).objectField("fLink").indexed(false); //$NON-NLS-1$
    config.objectClass(org.rssowl.core.internal.persist.Preference.class).objectField("fKey").indexed(false); //$NON-NLS-1$
    config.objectClass(org.rssowl.core.internal.persist.Feed.class).objectField("fLinkText").indexed(false);
    config.objectClass(org.rssowl.core.internal.persist.News.class).objectField("fLinkText").indexed(false); //$NON-NLS-1$
    config.objectClass(org.rssowl.core.internal.persist.News.class).objectField("fGuidValue").indexed(false);
    config.objectClass(org.rssowl.core.internal.persist.News.class).objectField("fFeedLink").indexed(false); //$NON-NLS-1$
    config.objectClass(org.rssowl.core.internal.persist.News.class).objectField("fStateOrdinal").indexed(false); //$NON-NLS-1$
    oc = Db4o.openFile(config, dbFileName);

    /* Access classes with index */
    ObjectSet<BookMark> markSet = oc.query(BookMark.class);
    if (markSet.hasNext())
      markSet.next().getName();

    ObjectSet<ConditionalGet> condGetSet = oc.query(ConditionalGet.class);
    if (condGetSet.hasNext())
      condGetSet.next().getLink();

    ObjectSet<Preference> prefSet = oc.query(Preference.class);
    if (prefSet.hasNext())
      prefSet.next().getKey();

    ObjectSet<Feed> feedSet = oc.query(Feed.class);
    if (feedSet.hasNext())
      feedSet.next().getLink();

    ObjectSet<News> newsSet = oc.query(News.class);
    if (newsSet.hasNext())
      newsSet.next().getLink();

    oc.close();
    progressMonitor.worked(totalProgress - totalProgressIncremented);
    return false;
  }

}
