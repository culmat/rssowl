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

package org.rssowl.ui.internal.filter;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.rssowl.core.persist.ILabel;
import org.rssowl.ui.filter.INewsActionPresentation;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.Collection;

/**
 * An implementation of {@link INewsActionPresentation} to select a Label.
 *
 * @author bpasero
 */
public class LabelNewsActionPresentation implements INewsActionPresentation {
  private Combo fLabelCombo;
  private ComboViewer fViewer;
  private Composite fContainer;

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#create(org.eclipse.swt.widgets.Composite, java.lang.Object)
   */
  public void create(Composite parent, Object data) {
    fContainer = new Composite(parent, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    ((GridLayout) fContainer.getLayout()).marginLeft = 5;
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    fLabelCombo = new Combo(fContainer, SWT.READ_ONLY | SWT.BORDER);
    fLabelCombo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fLabelCombo.setVisibleItemCount(15);

    fViewer = new ComboViewer(fLabelCombo);
    fViewer.setContentProvider(new ArrayContentProvider());
    fViewer.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ILabel) element).getName();
      }
    });

    Collection<ILabel> labels = ModelUtils.loadSortedLabels();
    fViewer.setInput(labels);

    /* Set Selection */
    if (fLabelCombo.getItemCount() > 0) {
      if (data != null) {
        for (ILabel label : labels) {
          if (label.getId().equals(data)) {
            fViewer.setSelection(new StructuredSelection(label));
            break;
          }
        }
      }

      if (fLabelCombo.getSelectionIndex() == -1)
        fLabelCombo.select(0);
    }
  }

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#dispose()
   */
  public void dispose() {
    fContainer.dispose();
  }

  /*
   * @see org.rssowl.ui.IFilterActionPresentation#getData()
   */
  public Long getData() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    return ((ILabel) selection.getFirstElement()).getId();
  }
}