package org.rssowl.contrib.podcast.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.model.NewsModel;
import org.rssowl.core.model.dao.PersistenceLayer;

public class Scan implements IWorkbenchWindowActionDelegate {

	private Shell fShell;

	public void dispose() {
		
	}

	public void init(IWorkbenchWindow window) {
		fShell = window.getShell();
	}

	public void run(IAction action) {
		// Add code here for scanning.
		PersistenceLayer lLayer = NewsModel.getDefault().getPersistenceLayer();
		
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}
}
