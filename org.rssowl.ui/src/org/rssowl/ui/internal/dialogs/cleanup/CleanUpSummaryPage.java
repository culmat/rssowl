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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class CleanUpSummaryPage extends WizardPage {
  private CheckboxTableViewer fViewer;
  private ResourceManager fResources;
  private Button fSelectAll;
  private Button fDeselectAll;

  /* Summary Label Provider */
  class SummaryLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
      return ((CleanUpTask) element).getLabel();
    }

    @Override
    public Image getImage(Object element) {
      return OwlUI.getImage(fResources, ((CleanUpTask) element).getImage());
    }
  }

  /**
   * @param pageName
   */
  protected CleanUpSummaryPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("Please review and approve the suggested operations.");
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  List<CleanUpTask> getTasks() {
    Object[] checkedElements = fViewer.getCheckedElements();
    List<CleanUpTask> tasks = new ArrayList<CleanUpTask>(checkedElements.length);

    for (Object checkedElement : checkedElements)
      tasks.add((CleanUpTask) checkedElement);

    return tasks;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Viewer to select particular Tasks */
    fViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTable().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

    /* ContentProvider */
    fViewer.setContentProvider(new ArrayContentProvider());

    /* LabelProvider */
    fViewer.setLabelProvider(new SummaryLabelProvider());

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fViewer.setAllChecked(true);
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fViewer.setAllChecked(false);
      }
    });

    setControl(container);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    /* Generate the Summary */
    if (visible) {
      FeedSelectionPage feedSelectionPage = (FeedSelectionPage) getPreviousPage();
      CleanUpOptionsPage cleanUpOptionsPage = (CleanUpOptionsPage) feedSelectionPage.getPreviousPage();

      final Set<IBookMark> selection = feedSelectionPage.getSelection();
      final CleanUpOperations operations = cleanUpOptionsPage.getOperations();

      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Please wait... Generating Summary", IProgressMonitor.UNKNOWN);
          onGenerateSummary(operations, selection);
        }
      };

      try {
        getContainer().run(true, false, runnable);
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private void onGenerateSummary(CleanUpOperations operations, Set<IBookMark> selection) {
    final CleanUpModel model = new CleanUpModel(operations, selection);
    model.generate();

    /* Show in Viewer */
    JobRunner.runInUIThread(fViewer.getTable(), new Runnable() {
      public void run() {
        fViewer.setInput(model.getTasks());
        fViewer.setAllChecked(true);
      }
    });
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return isCurrentPage();
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }
}