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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class ReflectiveASTVisitor extends ASTGenericVisitor {

	/** project */
	protected ICProject cproject = null;

	/** parser */
	protected IASTTranslationUnit parser = null;

	/** Resolve bindings flag */
	private boolean resolveBindings = false;

	/** Source file that includes a main function of the c program*/
	ITranslationUnit mainTU = null;

	/** Main AST */
	IASTTranslationUnit mainAST = null;

	
	/**
	 * Class constructor: initialise a ReflectiveASTVisitor
	 * 
	 * @param aProject
	 */
	public ReflectiveASTVisitor(ICProject project, boolean resolveBindings) {
		super(true);
		try {
			this.cproject = project;
			this.resolveBindings = resolveBindings;
			findMainFunction();
		} 
		catch (NoSuchMethodException | UnexpectedException | CoreException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Return the AST for a project
	 * 
	 * @return IASTTranslationUnit
	 * @throws CoreException 
	 * @throws UnexpectedException 
	 */
	protected IASTTranslationUnit getASTForFile(IFile file) throws CoreException, UnexpectedException {		
		// 1) get AST for a workspace file
		// if (parser == null){
		// //Create translation unit for file
		// ITranslationUnit tu = CoreModelUtil.findTranslationUnit(file);
		// if (tu == null){
		// Shell shell
		// =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		// MessageDialog.openInformation( shell, "Unexpected error in
		// translation unit", "Unexpected error in translation unit (NULL)");
		// throw new UnexpectedException("Unexpected error in translation unit
		// (NULL)");
		// }
		// //get AST
		// IASTTranslationUnit iast = tu.getAST();
		// }

		// 2) get an AST for a code reader (i.e., adding additional properties)
		// if (parser == null){
		// FileContent fileContent =
		// FileContent.createForExternalFileLocation(file.getRawLocation().toString());
		// IScannerInfo info = new ScannerInfo(new HashMap<String, String>());
		// IncludeFileContentProvider emptyIncludes =
		// IncludeFileContentProvider.getEmptyFilesProvider();
		// IIndex index = EmptyCIndex.INSTANCE;
		// int options = ITranslationUnit.AST_SKIP_ALL_HEADERS |
		// GPPLanguage.OPTION_NO_IMAGE_LOCATIONS |
		// GPPLanguage.OPTION_IS_SOURCE_UNIT;
		// IParserLogService log = new DefaultLogService();
		//
		// parser = GPPLanguage.getDefault().getASTTranslationUnit(fileContent,
		// info, emptyIncludes, index, options , log);
		// }

		// 3) get an index-based AST for a code reader
		if (parser == null) {
			// Create translation unit for file
			ITranslationUnit tu = CoreModelUtil.findTranslationUnit(file);
			if (tu == null) {
				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageDialog.openInformation(shell, "Unexpected error in translation unit",
						"Unexpected error in translation unit (NULL)");
				throw new UnexpectedException("Unexpected error in translation unit (NULL)");
			}
			//create index
			IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject());
			// get AST
			IASTTranslationUnit iast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
			iast.accept(this);
		}

		// try {
		// for (IPathEntry child : project.getResolvedPathEntries()){
		// System.out.println(child.getPath().toString());
		// }
		// } catch (CModelException e) {
		// e.printStackTrace();
		// }

		return null;
	}
	

	/**
	 * Find the source file with main() function
	 * @param cProject
	 * @return
	 * @throws NoSuchMethodException
	 * @throws UnexpectedException
	 * @throws CoreException
	 * @throws ClassNotFoundException 
	 */
	public void findMainFunction() throws NoSuchMethodException, UnexpectedException, CoreException, ClassNotFoundException{
		//get source folders
		for (ISourceRoot sourceRoot : cproject.getSourceRoots()){
//			System.out.println(sourceRoot.getLocationURI().getPath());
			//get all elements
			for (ICElement element : sourceRoot.getChildren()){
//				System.out.println(element.getElementName() +"\t"+ element.getClass());
				//find a .c or cpp file
				if (element instanceof ITranslationUnit && ((ITranslationUnit) element).isSourceUnit()){
					ITranslationUnit tu		= (ITranslationUnit) element;
					IASTTranslationUnit ast = getASTFromtTranslationUnit(tu);
					IASTNode node = findNodeInTree(ast, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "main");
					if (node != null && mainTU == null){
						mainTU = tu;
						mainAST = ast;
					}
					else if (node != null && mainTU != null){
						throw new NoSuchMethodException(String.format("Multiple main() methods in project %s", cproject.getProject().getName()));
					} 
				}
				
			}
		}
		
		if (mainTU ==null)
			throw new NoSuchMethodException(String.format("main() method not found in project %s", cproject.getProject().getName()));
		return;
	}
	
	
	/**
	 * Get AST from translation unit
	 * @param tu
	 * @param cproject
	 * @param resolveBindings
	 * @return
	 * @throws UnexpectedException
	 * @throws CoreException
	 */
	private IASTTranslationUnit getASTFromtTranslationUnit(ITranslationUnit tu) throws UnexpectedException, CoreException{
		//check if tu is null
		if (tu == null){
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog.openInformation( shell, "Unexpected error in translation unit", "Unexpected error in translation unit (NULL)");
			throw new UnexpectedException("Unexpected error in translation unit (NULL)");
		}
		//create index
		IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject());
		// get AST
		IASTTranslationUnit ast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
		ast.accept(this);
		return ast;
	}
	
	
	///////////////////////////////////////////////
	//// Tree analyser
	///////////////////////////////////////////////
//    private static IASTNode currentNode;
    private final static Logger LOG = Logger.getLogger("Analyser"); 

	/**
	 * Find the first node that is instance of <b>targetClass</b> and has the name <b>targetName</b>
     * @param node
     * @param index
     * @param targetClass
     * @param targetName
     * @return
     */
	protected  IASTNode findNodeInTree(IASTNode node, int index, Class<?> targetClass, String targetName) {
		try {
			if (targetClass== null || targetName==null)
				throw new IllegalArgumentException("Target class or target name is NULL!");
	
			//get node's children
			IASTNode[] children = node.getChildren();
		
			//navigate recursively
			for (IASTNode childNode : children){
				IASTNode tempNode = findNodeInTree(childNode, index+1, targetClass, targetName);
				if (tempNode != null)
					return tempNode;
			}
	
//			if (children.length==0)
//				System.out.println("Node:\t" + currentNode.getRawSignature());
	
			//find node of class targetClass with name targetName
			if (targetClass.isInstance(node)){
				Method method = targetClass.getMethod("getName", (Class<?>[]) null);
				if (targetName.equals(method.invoke(node).toString())){
					System.out.println("FOUND");
					return node;
				}
			}
		}//try
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	/**
	 * Find all nodes that are instances of <b>targetClass</b>
     * @param node
     * @param index
     * @param targetClass
     * @param targetName
     * @return
     */
	protected  void findAllNodesInTree(IASTNode node, int index, Class<?> targetClass, List<? super IASTNode> nodes) {
		try {
			if (targetClass== null)
				throw new IllegalArgumentException("Target class is NULL!");
	
			//get node's children
			IASTNode[] children = node.getChildren();
		
			//navigate recursively
			for (IASTNode childNode : children){
				findAllNodesInTree(childNode, index+1, targetClass, nodes);
			}
	
			//find node of class targetClass with name targetName
			if (targetClass.isInstance(node)){
				System.out.println("FOUND: " + node.getRawSignature());
				nodes.add(node);
			}
		}//try
		catch (SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		return;
	}
	
	
	///////////////////////////////////////////////
	//// Epsilon specific functions
	///////////////////////////////////////////////
	
	public Collection<Object> getAllofType(String type){
		List<Object> nodes = new ArrayList<Object>(); 
		try {
			System.out.println(getClass().getSimpleName() +".getAllofType(..)");
			findAllNodesInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IAST" + type), nodes);
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return nodes;
	}
	
	
	@Override
	protected int genericVisit(IASTNode node) {
//			System.out.println(node.getRawSignature());
		return PROCESS_CONTINUE;
	}

	
	@Override
	public int visit(IASTDeclarator declarator) {
//		if (declarator instanceof IASTFunctionDeclarator){
//			System.out.println(declarator.getRawSignature());
//			if (declarator.getName().toString().equals("main")){
//				if (mainTU == null)
//					mainTU = declarator.getTranslationUnit().getOriginatingTranslationUnit();
//				else{					
//					try {
//						throw new NoSuchMethodException(String.format("Multiple main() methods in project %s", cproject.getProject().getName()));
//					} 
//					catch (NoSuchMethodException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}
		return PROCESS_CONTINUE;
	}

}
