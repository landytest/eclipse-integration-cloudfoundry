/*******************************************************************************
 * Copyright (c) 2012, 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.cloudfoundry.ide.eclipse.internal.server.ui.wizards;

import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryServerUiPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.CloudSpaceHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Prompts for the password if an operation requires authentication.
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryCredentialsWizard extends Wizard {

	private final CloudFoundryServer server;

	private final IServerWorkingCopy serverWC;

	private CloudFoundryCloudSpaceWizardpage cloudSpacePage;

	private CloudFoundryCredentialsWizardPage credentialsPage;

	public CloudFoundryCredentialsWizard(CloudFoundryServer server) {
		serverWC = server.getServer().createWorkingCopy();
		this.server = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class, null);
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);

		// Will dynamically add the spaces page based on the URL selected. For
		// now, force the Next and Previous buttons to appear. Note that next
		// and previous
		// buttons are added automatically if there is more than one wizard page
		// added. However, to
		// avoid creating the controls for the spaces wizard page when the URL
		// is non-space server, only
		// add the spaces wizard page based on URL selection. Therefore, only
		// one page is
		// registered with the wizard: the credential page
		setForcePreviousAndNextButtons(true);
	}

	@Override
	public void addPages() {
		credentialsPage = new CloudFoundryCredentialsWizardPage(server);
		addPage(credentialsPage);
	}

	@Override
	public boolean canFinish() {

		if (cloudSpacePage == null || !cloudSpacePage.isPageComplete()) {
			return false;
		}

		return super.canFinish() && credentialsPage != null && credentialsPage.isPageComplete();
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == credentialsPage) {
			// Only create the page if there is a cloud space descriptor set
			// that has a list of orgs and spaces to choose from
			CloudSpaceHandler handler = credentialsPage.getSpaceChangeHandler();
			if (handler != null && handler.getCurrentSpacesDescriptor() != null) {
				cloudSpacePage = new CloudFoundryCloudSpaceWizardpage(server, handler);
				cloudSpacePage.setWizard(this);
				return cloudSpacePage;
			}

		}
		return super.getNextPage(page);
	}

	public IWizardPage getPreviousPage(IWizardPage page) {
		if (page instanceof CloudFoundryCloudSpaceWizardpage) {
			return credentialsPage;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			serverWC.save(true, null);
		}
		catch (CoreException e) {
			CloudFoundryServerUiPlugin.getDefault().getLog().log(e.getStatus());
		}
		return true;
	}

}
