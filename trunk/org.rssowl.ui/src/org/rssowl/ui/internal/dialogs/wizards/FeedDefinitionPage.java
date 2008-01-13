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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.OwlUI;

/**
 * @author bpasero
 */
public class FeedDefinitionPage extends WizardPage {
  private static final String HTTP = "http://";

  private Text fFeedLinkInput;
  private Text fKeywordInput;
  private Button fLoadTitleFromFeedButton;
  private Button fFeedByLinkButton;
  private Button fFeedByKeywordButton;
  private String fInitialLink;

  /**
   * @param pageName
   * @param initialLink
   */
  protected FeedDefinitionPage(String pageName, String initialLink) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/bkmrk_wiz.gif"));
    setMessage("Create a new Bookmark to read News from a Feed.");
    fInitialLink = initialLink;
  }

  boolean loadTitleFromFeed() {
    return fLoadTitleFromFeedButton.getSelection();
  }

  private String loadInitialLinkFromClipboard() {
    String initial = "http://";

    Clipboard cb = new Clipboard(getShell().getDisplay());
    TextTransfer transfer = TextTransfer.getInstance();
    String data = (String) cb.getContents(transfer);
    data = (data != null) ? data.trim() : null;
    cb.dispose();

    if (URIUtils.looksLikeLink(data)) {
      if (!data.contains("://"))
        data = initial + data;
      initial = data;
    }

    return initial;
  }

  String getLink() {
    return fFeedByLinkButton.getSelection() ? fFeedLinkInput.getText() : null;
  }

  String getKeyword() {
    return fFeedByKeywordButton.getSelection() ? fKeywordInput.getText() : null;
  }

  boolean isKeywordSubscription() {
    return StringUtils.isSet(getKeyword());
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return fFeedLinkInput.getText().length() > 0 || fKeywordInput.getText().length() > 0;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

//    Label infoLabel = new Label(container, SWT.WRAP);
//    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//    infoLabel.setText("You may either create a new Bookmark by supplying the Link to a Newsfeed or by supplying some Keywords or a Phrase (e.g. for Flickr or YouTube).");
//    ((GridData) infoLabel.getLayoutData()).widthHint = 200;

//    Composite contentMargin = new Composite(container, SWT.NONE);
//    contentMargin.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
//    contentMargin.setLayout(new GridLayout(1, false));
//    ((GridLayout) contentMargin.getLayout()).marginTop = 10;

    /* 1) Feed by Link */
    if (!StringUtils.isSet(fInitialLink))
      fInitialLink = loadInitialLinkFromClipboard();

    fFeedByLinkButton = new Button(container, SWT.RADIO);
    fFeedByLinkButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFeedByLinkButton.setText("Create a Feed by supplying a Link:");
    fFeedByLinkButton.setSelection(true);
    fFeedByLinkButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFeedLinkInput.setEnabled(fFeedByLinkButton.getSelection());
        fLoadTitleFromFeedButton.setEnabled(fFeedByLinkButton.getSelection());
      }
    });

    Composite textIndent = new Composite(container, SWT.NONE);
    textIndent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent.setLayout(new GridLayout(1, false));
    ((GridLayout) textIndent.getLayout()).marginLeft = 10;
    ((GridLayout) textIndent.getLayout()).marginBottom = 10;

    fFeedLinkInput = new Text(textIndent, SWT.BORDER);
    fFeedLinkInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFeedLinkInput.setFocus();

    if (StringUtils.isSet(fInitialLink) && !fInitialLink.equals(HTTP)) {
      fFeedLinkInput.setText(fInitialLink);
      fFeedLinkInput.selectAll();
    } else {
      fFeedLinkInput.setText(HTTP);
      fFeedLinkInput.setSelection(HTTP.length());
    }

    fLoadTitleFromFeedButton = new Button(textIndent, SWT.CHECK);
    fLoadTitleFromFeedButton.setText("Use the Title of the Feed as Name for the Bookmark");
    fLoadTitleFromFeedButton.setSelection(true); //TODO Load from Settings

    /* 2) Feed by Keyword */
    fFeedByKeywordButton = new Button(container, SWT.RADIO);
    fFeedByKeywordButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFeedByKeywordButton.setText("Create a Feed by supplying a Keyword or Phrase:");
    fFeedByKeywordButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fKeywordInput.setEnabled(fFeedByKeywordButton.getSelection());
      }
    });

    textIndent = new Composite(container, SWT.NONE);
    textIndent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent.setLayout(new GridLayout(1, false));
    ((GridLayout) textIndent.getLayout()).marginLeft = 10;

    fKeywordInput = new Text(textIndent, SWT.BORDER);
    fKeywordInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fKeywordInput.setEnabled(false);

    setControl(container);
  }
}