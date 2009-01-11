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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.rssowl.ui.filter.INewsActionPresentation;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.File;

/**
 * An implementation of {@link INewsActionPresentation} to select a sound to
 * play.
 *
 * @author bpasero
 */
public class PlaySoundActionPresentation implements INewsActionPresentation {
  private Link fSoundPathLink;
  private Composite fContainer;

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#create(org.eclipse.swt.widgets.Composite, java.lang.Object)
   */
  public void create(Composite parent, Object data) {
    fContainer = new Composite(parent, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    ((GridLayout) fContainer.getLayout()).marginLeft = 5;
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    fSoundPathLink = new Link(fContainer, SWT.PUSH);
    fSoundPathLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    updateLink(data);

    fSoundPathLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelect();
      }
    });
  }

  private void onSelect() {
    FileDialog dialog = new FileDialog(fSoundPathLink.getShell(), SWT.OPEN);
    dialog.setText("Select a Sound to Play");
    dialog.setFilterExtensions(new String[] { "*.wav" });

    if (fSoundPathLink.getData() != null) {
      File file = new File(fSoundPathLink.getData().toString());
      if (file.exists())
        dialog.setFileName(file.toString());
    }

    String fileName = dialog.open();
    if (fileName != null)
      updateLink(fileName);
  }

  private void updateLink(Object data) {
    if (data == null) {
      resetLink();
    } else {
      File file = new File(data.toString());
      if (file.exists()) {
        fSoundPathLink.setText("Sound to Play: <a>" + file.getName() + "</a>");
        fSoundPathLink.setToolTipText(data.toString());
        fSoundPathLink.setData(data);
      } else
        resetLink();
    }
  }

  private void resetLink() {
    fSoundPathLink.setText("<a>Select a Sound to play...</a>");
    fSoundPathLink.setData(null);
    fSoundPathLink.setToolTipText(null);
  }

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#dispose()
   */
  public void dispose() {
    fContainer.dispose();
  }

  /*
   * @see org.rssowl.ui.filter.INewsActionPresentation#getData()
   */
  public Object getData() {
    return fSoundPathLink.getData();
  }
}