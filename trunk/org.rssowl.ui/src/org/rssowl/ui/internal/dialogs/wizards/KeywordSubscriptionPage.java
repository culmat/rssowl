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

package org.rssowl.ui.internal.dialogs.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.OwlUI;

/**
 * @author bpasero
 */
public class KeywordSubscriptionPage extends WizardPage {
  private static final String URL_INPUT_TOKEN = " ";

  /* Base class for Search Engines */
  static class SearchEngine {
    private final String fName;
    private final String fIconPath;
    private final String fUrl;

    SearchEngine(String name, String iconPath, String url) {
      fName = name;
      fIconPath = iconPath;
      fUrl = url;
    }

    String getName() {
      return fName;
    }

    String getLabel(String keywords) {
      return fName + " on '" + keywords + "'";
    }

    String getIconPath() {
      return fIconPath;
    }

    String toUrl(String keywords) {
      keywords = URIUtils.urlEncode(keywords);
      return StringUtils.replaceAll(fUrl, URL_INPUT_TOKEN, keywords);
    }
  }

  /* Supported Engines */
  private static final SearchEngine[] fgSearchEngines = new SearchEngine[] {

  /* Delicious */
  new SearchEngine("Delicious", "icons/obj16/fav_delicious.gif", "http://del.icio.us/rss/tag/" + URL_INPUT_TOKEN),

  /* Flickr Photo */
  new SearchEngine("Flickr Photo", "icons/obj16/fav_flickr.gif", "http://www.flickr.com/services/feeds/photos_public.gne?tags=" + URL_INPUT_TOKEN + "&format=rss_200"),

  /* Google Blog */
  new SearchEngine("Google Blog", "icons/obj16/fav_google.gif", "http://blogsearch.google.com/blogsearch_feeds?q=" + URL_INPUT_TOKEN + "&num=10&output=rss"),

  /* Google News */
  new SearchEngine("Google News", "icons/obj16/fav_google.gif", "http://news.google.com/news?q=" + URL_INPUT_TOKEN + "&output=rss"),

  /* Technorati */
  new SearchEngine("Technorati", "icons/obj16/fav_technorati.gif", "http://feeds.technorati.com/search/" + URL_INPUT_TOKEN),

  /* YouTube Video */
  new SearchEngine("YouTube Video", "icons/obj16/fav_youtube.gif", "http://www.youtube.com/rss/tag/" + URL_INPUT_TOKEN + ".rss")

  };

  private SearchEngine fSelectedEngine;

  /**
   * @return Returns the currently selected SearchEngine.
   */
  public SearchEngine getSelectedEngine() {
    return fSelectedEngine;
  }

  /**
   * @param pageName
   */
  protected KeywordSubscriptionPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/bkmrk_wiz.gif"));
    setMessage("Create a new Bookmark to read News from a Feed.");
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    Label infoLabel = new Label(container, SWT.NONE);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    infoLabel.setText("Please select from one of the following search engines: ");

    Composite contentMargin = new Composite(container, SWT.NONE);
    contentMargin.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    contentMargin.setLayout(new GridLayout(1, false));
    ((GridLayout) contentMargin.getLayout()).marginTop = 10;
    ((GridLayout) contentMargin.getLayout()).verticalSpacing = 15;

    for (int i = 0; i < fgSearchEngines.length; i++) {
      final SearchEngine engine = fgSearchEngines[i];

      Button button = new Button(contentMargin, SWT.RADIO);
      button.setText(engine.getName());
      button.setImage(OwlUI.getImage(button, engine.getIconPath()));
      button.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          fSelectedEngine = engine;
        }
      });

      if (i == 0) {
        button.setSelection(true);
        button.setFocus();
        fSelectedEngine = engine;
      }
    }

    setControl(container);
  }
}