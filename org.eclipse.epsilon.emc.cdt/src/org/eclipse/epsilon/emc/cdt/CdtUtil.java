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
package org.eclipse.epsilon.emc.cdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class CdtUtil {
	/**
	 * Get a list of projects for the current workspace
	 * @return
	 */
	public static List<IProject> getIProjects(String[] projectNames){
		//get current workspace
		IWorkspace workspace 	= ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root		= workspace.getRoot();
		//get all projects in this workspace
		IProject[] projects = root.getProjects();
		//create a list to store the selected projects
		List<IProject> selectedProjects = new ArrayList<IProject>();
		for (IProject project : projects){
			selectedProjects.add(project);
		}
		return selectedProjects;
	}
	
	
	/** 
	 * Get a list of projects for the current workspace
	 */
	public static List<IProject> getIProjects(){
		//get current workspace
		IWorkspace workspace 	= ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root		= workspace.getRoot();
		//get all projects in this workspace
		IProject[] projects = root.getProjects();
		
		return Arrays.asList(projects);
	}
	
	
	/** 
	 * Get the IProject with the given project name
	 * @return 
	*/
	public static IProject getIProject (String projectName){
		//get current workspace
		IWorkspace workspace 	= ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root		= workspace.getRoot();
		//get all projects in this workspace
		IProject[] projects = root.getProjects();
		//fing the project with the given name
		for (IProject project : projects){
			if (project.getName().equals(projectName)){
				return project;
			}
		}
		throw new NoSuchElementException(String.format("Project %s not found", projectName));
	}
	
	
	/**
	 * Create an ICProject from an IProject
	 * @param project
	 * @return
	 * @throws CoreException
	 */
	public static ICProject getICProject (IProject project) throws CoreException{
		ICProject cProject;
		if (project.isOpen() && project.hasNature(org.eclipse.cdt.core.CProjectNature.C_NATURE_ID)){
			cProject = CoreModel.getDefault().create(project);
			return cProject;
		}
		return null;
	}

	
	/** 
	 * Get the ICProject with the given project name
	 * @return 
	 * @throws CoreException 
	*/
	public static ICProject getICProject (String projectName) throws CoreException{
		return CdtUtil.getICProject(getIProject(projectName));
	}
	
	/**
	 * Get a list of ICProjects
	 * @param projects
	 * @return
	 * @throws CoreException 
	 */
	public static List<ICProject> getICProjects (List<IProject> projects) throws CoreException{
		List<ICProject> sourceProjects = new ArrayList<ICProject>();
		for (IProject project : projects){
			ICProject cProject = getICProject(project);
			if(cProject != null){
				sourceProjects.add(cProject);
			}
		}
		return sourceProjects;
	}
	
	

	
}
