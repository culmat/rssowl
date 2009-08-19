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

package org.rssowl.ui.internal.dialogs.importer;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link WizardPage} to select the source of import.
 *
 * @author bpasero
 */
public class ImportSourcePage extends WizardPage {

  /* Max number of Sources to remember */
  private static final int MAX_REMEMBER_SOURCES = 8;

  private Button fImportFromResourceRadio;
  private Combo fResourceInput;
  private Button fBrowseFileButton;
  private Button fImportFromKeyword;
  private Combo fKeywordInput;
  private Button fImportFromRecommendedRadio;
  private IPreferenceScope fPreferences;
  private boolean fIsAutoCompleteKeywordHooked;

  /* Sources for Import */
  enum Source {

    /* User has decided against importing */
    NONE,

    /* User has provided a resource (either local or online) */
    RESOURCE,

    /* User wants to search feeds matching a keyword */
    KEYWORD,

    /* User wants to import from recommended */
    RECOMMENDED
  }

  /**
   * @param pageName
   */
  protected ImportSourcePage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/import_wiz.png"));
    setMessage("Please choose the source of import.");
    fPreferences = Owl.getPreferenceService().getGlobalScope();
  }

  /* Returns the type of Source to use for the Import */
  Source getSource() {
    if (fImportFromResourceRadio.getSelection())
      return Source.RESOURCE;

    if (fImportFromKeyword.getSelection())
      return Source.KEYWORD;

    return Source.RECOMMENDED;
  }

  /* Returns the Resource to Import from or null if none */
  String getImportResource() {
    return fImportFromResourceRadio.getSelection() ? fResourceInput.getText() : null;
  }

  /* Returns the Keywords to Import from or null if none */
  String getImportKeywords() {
    return fImportFromKeyword.getSelection() ? fKeywordInput.getText() : null;
  }

  /* Returns whether the source is only remotely accessible */
  boolean isRemoteSource() {
    Source source = getSource();
    if (source == Source.KEYWORD)
      return true;

    if (source == Source.RESOURCE) {
      String resource = getImportResource();
      if (!new File(resource).exists() && URIUtils.looksLikeLink(resource))
        return true;
    }

    return false;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Import from File or Website */
    fImportFromResourceRadio = new Button(container, SWT.RADIO);
    fImportFromResourceRadio.setSelection(true);
    fImportFromResourceRadio.setText("Import Feeds from a File or Website:");
    fImportFromResourceRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
        boolean importFromResource = fImportFromResourceRadio.getSelection();
        fResourceInput.setEnabled(importFromResource);
        fBrowseFileButton.setEnabled(importFromResource);
        if (importFromResource)
          fResourceInput.setFocus();
      }
    });

    Composite sourceInputContainer = new Composite(container, SWT.None);
    sourceInputContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) sourceInputContainer.getLayout()).marginLeft = 15;
    ((GridLayout) sourceInputContainer.getLayout()).marginBottom = 10;
    sourceInputContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fResourceInput = new Combo(sourceInputContainer, SWT.DROP_DOWN | SWT.BORDER);
    fResourceInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    fResourceInput.setFocus();

    String[] previousResources = fPreferences.getStrings(DefaultPreferences.IMPORT_RESOURCES);
    if (previousResources != null) {
      for (String source : previousResources) {
        fResourceInput.add(source);
      }
    }

    fResourceInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePageComplete();
      }
    });

    fBrowseFileButton = new Button(sourceInputContainer, SWT.PUSH);
    fBrowseFileButton.setText("Browse...");
    setButtonLayoutData(fBrowseFileButton);
    fBrowseFileButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onBrowse();
      }
    });

    /* Import from Keyword Search */
    fImportFromKeyword = new Button(container, SWT.RADIO);
    fImportFromKeyword.setText("Import Feeds matching the following Keywords:");
    fImportFromKeyword.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
        boolean importFromKeyword = fImportFromKeyword.getSelection();
        fKeywordInput.setEnabled(importFromKeyword);
        if (importFromKeyword) {
          hookKeywordAutocomplete();
          fKeywordInput.setFocus();
        }
      }
    });

    Composite keywordInputContainer = new Composite(container, SWT.None);
    keywordInputContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) keywordInputContainer.getLayout()).marginLeft = 15;
    ((GridLayout) keywordInputContainer.getLayout()).marginBottom = 10;
    keywordInputContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fKeywordInput = new Combo(keywordInputContainer, SWT.DROP_DOWN | SWT.BORDER);
    fKeywordInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    fKeywordInput.setEnabled(false);

    String[] previousKeywords = fPreferences.getStrings(DefaultPreferences.IMPORT_KEYWORDS);
    if (previousKeywords != null) {
      for (String keyword : previousKeywords) {
        fKeywordInput.add(keyword);
      }
    }

    fKeywordInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updatePageComplete();
      }
    });

    /* Import from Recommended Feeds */
    fImportFromRecommendedRadio = new Button(container, SWT.RADIO);
    fImportFromRecommendedRadio.setText("Import Recommended Feeds");
    fImportFromRecommendedRadio.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updatePageComplete();
      }
    });

    setControl(container);
    updatePageComplete();
  }

  private void onBrowse() {
    FileDialog dialog = new FileDialog(getShell());
    dialog.setText("Choose Import File");

    /* Set Export Formats also for Import (we assume this is supported) */
    List<String> filterExtensions = new ArrayList<String>();
    filterExtensions.add("*.opml");
    filterExtensions.add("*.xml");

    Collection<String> exportFormats = Owl.getInterpreter().getExportFormats();
    for (String exportFormat : exportFormats) {
      String format = "*." + exportFormat.toLowerCase();
      if (!filterExtensions.contains(format))
        filterExtensions.add(format);
    }

    if (!filterExtensions.contains("*.*"))
      filterExtensions.add("*.*");

    dialog.setFilterExtensions(filterExtensions.toArray(new String[filterExtensions.size()]));
    if (StringUtils.isSet(fResourceInput.getText()))
      dialog.setFileName(fResourceInput.getText());

    String string = dialog.open();
    if (string != null)
      fResourceInput.setText(string);

    updatePageComplete();
  }

  private void updatePageComplete() {
    String errorMessage = null;

    /* Import Default */
    if (fImportFromRecommendedRadio.getSelection())
      setPageComplete(true);

    /* Import from Resource */
    else if (fImportFromResourceRadio.getSelection()) {
      if (!StringUtils.isSet(fResourceInput.getText()))
        setPageComplete(false);
      else {
        if (isRemoteSource())
          setPageComplete(true);
        else {
          String filePath = fResourceInput.getText();
          File fileToImport = new File(filePath);
          boolean fileExists = fileToImport.exists() && fileToImport.isFile();
          setPageComplete(fileExists);
          if (!fileExists)
            errorMessage = "Please select an existing file.";
        }
      }
    }

    /* Import From Keywords */
    else if (fImportFromKeyword.getSelection())
      setPageComplete(StringUtils.isSet(fImportFromKeyword.getText()));

    /* Set Error Message */
    if (errorMessage != null)
      setErrorMessage(errorMessage);

    /* Restore Normal Message */
    else {
      setErrorMessage(null);
      setMessage("Please choose the source of import.");
    }
  }

  /* Save Import Sources */
  void saveSettings() {

    /* Import Resources */
    saveComboSettings(fImportFromResourceRadio.getSelection() ? fResourceInput.getText() : null, DefaultPreferences.IMPORT_RESOURCES);

    /* Import Keywords */
    saveComboSettings(fImportFromKeyword.getSelection() ? fKeywordInput.getText() : null, DefaultPreferences.IMPORT_KEYWORDS);
  }

  private void saveComboSettings(String valueToAdd, String prefKey) {

    /* First fill current as new one */
    List<String> newValues = new ArrayList<String>();
    if (StringUtils.isSet(valueToAdd))
      newValues.add(valueToAdd);

    /* Then add up to N more old ones to remember */
    String[] oldValues = fPreferences.getStrings(prefKey);
    if (oldValues != null) {
      for (int i = 0; i < oldValues.length && newValues.size() < MAX_REMEMBER_SOURCES; i++) {
        if (!newValues.contains(oldValues[i]))
          newValues.add(oldValues[i]);
      }
    }

    /* Save List */
    if (!newValues.isEmpty())
      fPreferences.putStrings(prefKey, newValues.toArray(new String[newValues.size()]));
  }

  private void hookKeywordAutocomplete() {

    /* Only perform once */
    if (fIsAutoCompleteKeywordHooked)
      return;
    fIsAutoCompleteKeywordHooked = true;

    final Pair<SimpleContentProposalProvider, ContentProposalAdapter> autoComplete = OwlUI.hookAutoComplete(fKeywordInput, null, true);

    /* Load proposals in the Background */
    JobRunner.runDelayedInBackgroundThread(new Runnable() {
      public void run() {
        if (!fKeywordInput.isDisposed()) {
          Set<String> values = new TreeSet<String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
              return o1.compareToIgnoreCase(o2);
            }
          });

          /* Add all Categories */
          values.addAll(DynamicDAO.getDAO(ICategoryDAO.class).loadAllNames());

          /* Add all Labels */
          Collection<ILabel> labels = DynamicDAO.getDAO(ILabelDAO.class).loadAll();
          for (ILabel label : labels) {
            values.add(label.getName());
          }

          /* Apply Proposals */
          if (!fKeywordInput.isDisposed())
            OwlUI.applyAutoCompleteProposals(values, autoComplete.getFirst(), autoComplete.getSecond());
        }
      }
    });
  }
}