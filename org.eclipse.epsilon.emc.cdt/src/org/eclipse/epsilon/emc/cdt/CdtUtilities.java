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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class CdtUtilities {
	
	private CdtUtilities() {}
	
	
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
		return CdtUtilities.getICProject(getIProject(projectName));
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
	
	
	
	public static IFile createNewFile (ICProject cproject, String folderName, String filename){
		try {
			//check if the project exists and is open, it should be, but check anyway
			IProject project = cproject.getProject();
			if (!project.exists())
				throw new NoSuchElementException ("Project " + project.getName() +" does not exist!");
			if (!project.isOpen())
				project.open(null);
				
			//get the folder
			IFolder folder  = project.getFolder(folderName);  
			if (!folder.exists())
				folder.create(IResource.NONE, true, null);
			
			//check if this directory is a source directory
			if (!cproject.isOnSourceRoot(folder))
				throw new IllegalArgumentException("Directory " + folderName + " is not a source directory");
	
			//create new IFile
			IFile file = folder.getFile(filename);
			if (!file.exists()){
				InputStream source = new ByteArrayInputStream("".getBytes());
			    file.create(source, IResource.NONE, null);
			}
			return file;
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static boolean appendToFile (IFile file, String output){
		try {
			if (!file.exists())
				return false;
			
			InputStream source = new ByteArrayInputStream(output.getBytes());

			file.appendContents(source, IFile.FORCE, null);
			return true;
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}
	

		
	
	/**
	 * Returns as List all the translation units for the given project.
	 * This function considers all the source directories and sub-directories of this projects
	 * and excludes any translation units whose name is within {@code excludedFiles} array
	 * @param cproject the current C/C++ project
	 * @param excludedFiles an array of filenames for which a translation unit <b>won't</b> be generated
	 * @return
	 */
	public static List<ITranslationUnit> getProjectTranslationUnits (ICProject cproject,  Set<String> excludedFilesSet) {
		List<ITranslationUnit> tuList = new ArrayList<ITranslationUnit>();
		
		//get source folders
		try {
			for (ISourceRoot sourceRoot : cproject. getSourceRoots()){
//				System.out.println(sourceRoot.getLocationURI().getPath());
				//get all elements
				for (ICElement element : sourceRoot.getChildren()){
					//if it is a container (i.e., a source folder)
//					System.out.println(element.getElementName() +"\t"+ element.getElementType() +"\t"+element.getClass());
					if (element.getElementType() == ICElement.C_CCONTAINER){
						recursiveContainerTraversal((ICContainer)element, tuList, excludedFilesSet);
					}
					else{
						ITranslationUnit tu		= (ITranslationUnit) element;
						if (! excludedFilesSet.contains((tu.getFile().getLocation().toString())))
							tuList.add(tu);
					}

				}
			}
		} catch (CModelException e) {
			e.printStackTrace();
		}
		return tuList;
	}
	
	
	private static void recursiveContainerTraversal (ICContainer container, List<ITranslationUnit> tuList, Set<String> excludedFilesSet) throws CModelException{
		for (ICContainer inContainer : container.getCContainers()){
			recursiveContainerTraversal(inContainer, tuList, excludedFilesSet);
		}
		
		for (ITranslationUnit tu : container.getTranslationUnits()){
			if (! excludedFilesSet.contains((tu.getFile().getLocation().toString())))
				tuList.add(tu);			
		}
	}
	
	
	
	/**
	 * Returns as List all the translation units for the given project.
	 * This function considers all the source directories and sub-directories of this project
	 * @param cproject the current C/C++ project
	 * @return
	 */
	public static List<Object> getElementsFromProject(IParent parent,  Class<?> clazz, List<Object> list) {				
		try {
			for (ICElement element : parent.getChildren()){
					if (clazz.isInstance(element)){
						list.add(element);
					}
					else if (element instanceof IParent && !element.getElementName().equals("Debug")){
						getElementsFromProject((IParent)element, clazz, list);
					}				
			}
		}
		catch (CModelException e){
			e.printStackTrace();
		}
		return list;			
	}
	
	
	/**
	 * Given an index name, return the corresponding translation unit
	 * @see CxxAstUtils.getTranslationUnitFromIndexName(IIndexName decl)
	 * @param decl
	 * @return
	 * @throws CoreException
	 */
	public static ITranslationUnit getTranslationUnitFromIndexName(IIndexName decl) throws CoreException {
		IIndexFile file = decl.getFile();
		if (file != null) {
			return CoreModelUtil.findTranslationUnitForLocation(file.getLocation().getURI(), null);	
		}
		return null;
	}

	
}
