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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryPlugin;
import org.cloudfoundry.ide.eclipse.internal.server.core.CloudFoundryServer;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.CloudFoundryApplicationModule;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.LocalCloudService;
import org.cloudfoundry.ide.eclipse.internal.server.core.client.TunnelBehaviour;
import org.cloudfoundry.ide.eclipse.internal.server.ui.CloudFoundryImages;
import org.cloudfoundry.ide.eclipse.internal.server.ui.ICoreRunnable;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewColumn;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewerConfigurator;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServiceViewerSorter;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.ServicesTreeLabelProvider;
import org.cloudfoundry.ide.eclipse.internal.server.ui.editor.TreeContentProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;

public class CloudFoundryApplicationServicesWizardPage extends PartsWizardPage {

	// This page is optional and can be completed at any time
	private final boolean canFinish = true;

	private final String serverTypeId;

	private final CloudFoundryServer cloudServer;

	private CheckboxTableViewer servicesViewer;

	private static final String DESCRIPTION = "Bind or add new services";

	/**
	 * Services, either existing or new, that a user has checked for binding.
	 */
	private final Set<String> selectedServicesToBind = new HashSet<String>();

	/**
	 * This is a list of services to add to the CF server. This may not
	 * necessarily match all the services a user has selected to bind to an
	 * application, as a user may add a service, but uncheck it for binding.
	 */
	private final Set<String> servicesToAdd = new HashSet<String>();

	/**
	 * All services both existing and added, used to refresh the input of the
	 * viewer
	 */
	private final Map<String, CloudService> allServices = new HashMap<String, CloudService>();

	private final ApplicationWizardDescriptor descriptor;

	public CloudFoundryApplicationServicesWizardPage(CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule module, ApplicationWizardDescriptor descriptor) {
		super("Services", "Services selection", null);
		this.cloudServer = cloudServer;
		this.serverTypeId = module.getServerTypeId();
		this.descriptor = descriptor;
	}

	public boolean isPageComplete() {
		return canFinish;
	}

	public void createControl(Composite parent) {

		setDescription(DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite tableArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tableArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableArea);

		Composite toolBarArea = new Composite(tableArea, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(toolBarArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(toolBarArea);

		Label label = new Label(toolBarArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
		label.setText("Select services to bind to the application:");

		Table table = new Table(tableArea, SWT.BORDER | SWT.SINGLE | SWT.CHECK);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar bar = toolBarManager.createControl(toolBarArea);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).grab(true, false).applyTo(bar);

		servicesViewer = new CheckboxTableViewer(table);

		servicesViewer.setContentProvider(new TreeContentProvider());
		servicesViewer.setLabelProvider(new ServicesTreeLabelProvider(servicesViewer) {

			protected Image getColumnImage(CloudService service, ServiceViewColumn column) {
				if (column == ServiceViewColumn.Tunnel) {
					TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
					if (handler.hasCaldecottTunnel(service.getName())) {
						return CloudFoundryImages.getImage(CloudFoundryImages.CONNECT);
					}
				}
				return null;
			}

		});
		servicesViewer.setSorter(new ServiceViewerSorter(servicesViewer, cloudServer.hasCloudSpace()) {

			@Override
			protected int compare(CloudService service1, CloudService service2, ServiceViewColumn sortColumn) {
				if (sortColumn == ServiceViewColumn.Tunnel) {
					TunnelBehaviour handler = new TunnelBehaviour(cloudServer);
					if (handler.hasCaldecottTunnel(service1.getName())) {
						return -1;
					}
					else if (handler.hasCaldecottTunnel(service2.getName())) {
						return 1;
					}
					else {
						return 0;
					}
				}
				return super.compare(service1, service2, sortColumn);
			}

		});

		new ServiceViewerConfigurator().enableAutomaticViewerResizing().configureViewer(servicesViewer);

		servicesViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				Object[] services = servicesViewer.getCheckedElements();
				if (services != null) {
					selectedServicesToBind.clear();
					for (Object obj : services) {
						CloudService service = (CloudService) obj;
						selectedServicesToBind.add(service.getName());
					}
					setServicesToBindInDescriptor();
				}
			}
		});

		Action addServiceAction = new Action("Add Service", CloudFoundryImages.NEW_SERVICE) {

			public void run() {
				// Do not create the service right away.
				boolean deferAdditionOfService = true;
				CloudFoundryServiceWizard wizard = new CloudFoundryServiceWizard(cloudServer, deferAdditionOfService);
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				dialog.setBlockOnOpen(true);
				if (dialog.open() == Window.OK) {
					// This cloud service does not yet exist. It will be created
					// outside of the wizard
					CloudService addedService = wizard.getService();
					if (addedService != null) {
						addService(addedService);
					}
				}
			}

			public String getToolTipText() {
				return "Add a service to the server and automatically select it for the deployed application.";
			}
		};
		toolBarManager.add(addServiceAction);

		toolBarManager.update(true);

		setControl(tableArea);
		setInput();
	}

	/**
	 * Also automatically selects the added service to be bound to the
	 * application.
	 * @param service that was added and will also be automatically selected to
	 * be bound to the application.
	 */
	protected void addService(CloudService service) {
		if (service == null) {
			return;
		}

		allServices.put(service.getName(), service);

		servicesToAdd.add(service.getName());

		selectedServicesToBind.add(service.getName());
		setServicesToBindInDescriptor();
		setServicesToCreateInDescriptor();
		setBoundServiceSelectionInUI();
	}

	protected void setInput() {

		ICoreRunnable runnable = new ICoreRunnable() {

			public void run(IProgressMonitor monitor) throws CoreException {

				try {
					List<CloudService> existingServices = cloudServer.getBehaviour().getServices(monitor);

					// Clear only after retrieving an update list without errors
					allServices.clear();
					servicesToAdd.clear();
					selectedServicesToBind.clear();

					// Only populate from the existing deployment info if
					// retrieving list of existing services was successful.
					// That way the services in the deployment info can be
					// verified if they exist, or if they need to be created.
					populateServicesFromDeploymentInfo();

					// Update the mapping with existing Cloud Services. Local
					// services
					// (services that have not yet been created) will be
					// unaffected by this.
					if (existingServices != null) {
						for (CloudService actualService : existingServices) {
							if (actualService != null) {
								allServices.put(actualService.getName(), actualService);
							}
						}
					}

					// At this stage, since the existing Cloud Service mapping
					// has been updated
					// above, any remaining Local cloud services can be assumed
					// to not exist and
					// will require being created. Only create services IF they
					// are to be bound to the app.
					for (String name : selectedServicesToBind) {
						CloudService service = allServices.get(name);
						if (service instanceof LocalCloudService) {
							servicesToAdd.add(name);
						}
					}

					setServicesToCreateInDescriptor();

					// Refresh UI
					Display.getDefault().asyncExec(new Runnable() {

						public void run() {
							// Clear any info in the dialogue
							setMessage(null);
							update(false, Status.OK_STATUS);

							setBoundServiceSelectionInUI();
						}

					});
				}
				catch (final CoreException e) {

					Display.getDefault().asyncExec(new Runnable() {

						public void run() {

							update(false,
									CloudFoundryPlugin
											.getErrorStatus(
													"Failed to verify existing services in the server. Only new services can be created at this time. Please check connection or credentials and try again. - "
															+ e.getMessage(), e));

						}
					});
				}
			}
		};
		runAsynchWithWizardProgress(runnable, "Verifying existing services");

	}

	protected void populateServicesFromDeploymentInfo() {

		List<CloudService> servicesToBind = descriptor.getDeploymentInfo().getServices();

		if (servicesToBind != null) {
			for (CloudService service : servicesToBind) {
				allServices.put(service.getName(), service);

				selectedServicesToBind.add(service.getName());
			}
		}
		setServicesToBindInDescriptor();
	}

	protected void setBoundServiceSelectionInUI() {
		servicesViewer.setInput(allServices.values().toArray(new CloudService[] {}));
		List<CloudService> checkedServices = getServicesToBindAsCloudServices();
		servicesViewer.setCheckedElements(checkedServices.toArray());
	}

	protected List<CloudService> getServicesToBindAsCloudServices() {
		List<CloudService> servicesToBind = new ArrayList<CloudService>();
		for (String serviceName : selectedServicesToBind) {
			CloudService service = allServices.get(serviceName);
			if (service != null) {
				servicesToBind.add(service);
			}
		}
		return servicesToBind;
	}

	protected void setServicesToBindInDescriptor() {
		List<CloudService> servicesToBind = getServicesToBindAsCloudServices();

		descriptor.getDeploymentInfo().setServices(servicesToBind);
	}

	protected void setServicesToCreateInDescriptor() {
		List<CloudService> toCreate = new ArrayList<CloudService>();
		for (String serviceName : servicesToAdd) {
			CloudService service = allServices.get(serviceName);
			if (service != null) {
				toCreate.add(service);
			}
		}

		descriptor.setCloudServicesToCreate(toCreate);
	}

	public void setErrorText(String newMessage) {
		// Clear the message
		setMessage("");
		super.setErrorMessage(newMessage);
	}

	public void setMessageText(String newMessage) {
		setErrorMessage("");
		super.setMessage(newMessage);
	}

}
