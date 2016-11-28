/*******************************************************************************
 * Copyright (c) 2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Simos Gerasimou - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.emc.cdt.dt;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.launching.dialogs.AbstractCachedModelConfigurationDialog;
import org.eclipse.epsilon.emc.cdt.CdtModel;
import org.eclipse.epsilon.emc.cdt.CdtUtilities;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

public class CdtModelConfigurationDialog extends AbstractCachedModelConfigurationDialog{
	
	/** List of projects*/
	private List list;

	/** Binding button*/
	private Button bindingBtn;
	
	/** Binding flag*/
	private boolean bindingFlag;
	
	
	/**
	 * Get name for this model
	 */
	@Override
	protected String getModelName() {
		return "C/C++ (CDT) Model";
	}

	
	/**
	 * Get the type of this model
	 */
	@Override
	protected String getModelType() {
		return "CDT";
	}


	/**
	 * Create dialog window
	 */
	@Override
	protected void createGroups (Composite control){
		super.createGroups(control);
		createProjectSelectionGroup(control);
		createBidingSwitchGroup(control);
		createLoadStoreOptionsGroup(control);
		readOnLoadCheckbox.setEnabled(false);
		readOnLoadLabel.setEnabled(false);
	}

	
	
	
	/**
	 * Find projects and create a selection group
	 * @param parent
	 */
	private void createProjectSelectionGroup(Composite parent) {
		final Composite groupContent = createGroupContainer(parent, "Projects",1);
		
		getProjectsList(groupContent);
		groupContent.layout();
	}
	
	
	/** 
	 * Get a list of C projects
	 * @param groupContent
	 */
	private void getProjectsList(Composite groupContent){
		list = new List(groupContent, SWT.BORDER | SWT.V_SCROLL);
		//find projects
		final Collection<String> projectNameList = new ArrayList<String>();
		Collection<ICProject> cProjects = getCProjects();
		for (ICProject cProject : cProjects){
			projectNameList.add(cProject.getProject().getName());
		}
		list.setItems(projectNameList.toArray(new String[projectNameList.size()]));
		list.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	

	/**
	 * Get C Projects
	 * @return
	 */
	private Collection<ICProject> getCProjects(){
		Collection<IProject> projects = CdtUtilities.getIProjects();
		try {
			return CdtUtilities.getICProjects(new ArrayList<IProject>(projects));
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	/**
	 * Find projects and create a selection group
	 * @param parent
	 */
	private void createBidingSwitchGroup(Composite parent) {
		final Composite groupContent = createGroupContainer(parent, "Bindings",2);
		getBindingButton(groupContent);
		groupContent.layout();
	}
	
	
	/**
	 * Get a button for binding
	 * @param groupContent
	 */
	private void getBindingButton (Composite groupContent){
		bindingBtn = new Button(groupContent, SWT.CHECK);
		
		bindingBtn.setText("Resolve Bindings?");
		bindingBtn.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (bindingBtn.getSelection() == true){
					bindingFlag = true;
				}
				else{
					bindingFlag = false;
				}
//				System.out.println(bindingFlag +"\t"+ bindingBtn.getSelection());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}
		
	
	/**
	 * Load properties (when?) 
	 */
	@Override
	protected void loadProperties(){
		super.loadProperties();
		if (properties == null)
			return;
		//get selected project name
		String projectName = properties.getProperty(CdtModel.PROPERTY_PROJECT);
		list.setSelection(new String[]{projectName});
		
		// binding switch
		String bindingStr = properties.getProperty(CdtModel.PROPERTY_RESOLVE_BINDINGS);
		if (bindingStr != "") {
			boolean bindingTemp = Boolean.parseBoolean(bindingStr);
			if (bindingTemp == true) {
				bindingFlag = true;
				bindingBtn.setSelection(true);
			} else {
				bindingFlag = false;
				bindingBtn.setSelection(false);
			}
		}
		
		readOnLoadCheckbox.setSelection(true);
	}
	
	
	/**
	 * Store properties (when OK button is pressed)
	 */
	@Override
	protected void storeProperties(){
		super.storeProperties();
		//check if multiple projects have been selected
		if (list.getSelection().length > 1){
			MessageDialog.openError(getShell(), "Selected miltiple projects", 
		    	    String.format("Expected a single project, but received %d.\n Please fix this.", list.getSelection().length));
			return;
		}
		//store project information
		String projectName = list.getSelection()[0];
		properties.put(CdtModel.PROPERTY_PROJECT, projectName);
		properties.put(CdtModel.PROPERTY_RESOLVE_BINDINGS, bindingFlag);
	}

}
