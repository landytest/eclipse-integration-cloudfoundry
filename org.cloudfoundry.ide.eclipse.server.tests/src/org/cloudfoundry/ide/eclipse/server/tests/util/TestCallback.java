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
package org.cloudfoundry.ide.eclipse.server.tests.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.ide.eclipse.internal.server.core.ApplicationAction;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryCallback;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.DeploymentInfoWorkingCopy;
import org.cloudfoundry.ide.eclipse.internal.server.core.tunnel.CaldecottTunnelDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author Steffen Pingel
 */
public class TestCallback extends CloudFoundryCallback {

	private final String appName;

	private final String url;

	public TestCallback() {
		this.appName = null;
		this.url = null;
	}

	public TestCallback(String appName) {
		this.appName = appName;
		this.url = null;
	}

	public TestCallback(String appName, String url) {
		this.appName = appName;
		this.url = url;
	}

	@Override
	public void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// ignore
	}

	@Override
	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void disconnecting(CloudFoundryServer server) {
		// ignore
	}

	@Override
	public void getCredentials(CloudFoundryServer server) {
		throw new OperationCanceledException();
	}

	@Override
	public void prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) {
		String appName;

		if (this.appName != null) {
			appName = this.appName;
		}
		else {
			appName = module.getName();
		}

		DeploymentInfoWorkingCopy copy = module.getDeploymentInfoWorkingCopy();
		copy.setDeploymentName(appName);
		copy.setMemory(128);
		copy.setDeploymentMode(ApplicationAction.START);

		if (url != null) {
			copy.setUris(Collections.singletonList(url));
		}

		copy.save();
	}

	@Override
	public void deleteServices(List<String> services, CloudFoundryServer cloudServer) {
		// ignore
	}

	private boolean autoDeployEnabled;

	@Override
	public boolean isAutoDeployEnabled() {
		return autoDeployEnabled;
	}

	public void setAutoDeployEnabled(boolean autoDeployEnabled) {
		this.autoDeployEnabled = autoDeployEnabled;
	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void displayCaldecottTunnelConnections(CloudFoundryServer server, List<CaldecottTunnelDescriptor> descriptors) {
		// ignore
	}

	@Override
	public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// TODO Auto-generated method stub
	}

	@Override
	public void startApplicationConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex) {
		// TODO Auto-generated method stub
	}

}
