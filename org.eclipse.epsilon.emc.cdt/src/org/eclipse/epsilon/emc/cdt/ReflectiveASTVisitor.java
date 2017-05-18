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

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;


@SuppressWarnings("restriction")
public class ReflectiveASTVisitor extends ASTGenericVisitor {

	/** project */
	protected ICProject cproject = null;

//	/** Resolve bindings flag */
//	@Deprecated
//	private boolean resolveBindings = false;

	/** Source file that includes a main function of the c program*/
 	private ITranslationUnit mainTU = null;

	/** Main AST */
	private IASTTranslationUnit mainAST = null;
	
	/** Index for this project*/
	protected IIndex projectIndex = null;
	
	/** Pairs of ITranslationUnit, IASTTranslationUnit **/
	protected HashMap<ITranslationUnit, IASTTranslationUnit> projectASTCache;

	/** List of ITranslationUnit for the selected project**/
	protected List<ITranslationUnit> tuList;
	
	/** Flag indicating whether superclasses (of kind) should be checked*/
	protected boolean checkOfKind = false;

	
	/**
	 * Class constructor: initialise a ReflectiveASTVisitor
	 * 
	 * @param aProject
	 */
	public ReflectiveASTVisitor(ICProject project, boolean resolveBindings) {
		super(true);
		try {
			this.cproject = project;
			this.projectIndex = CCorePlugin.getIndexManager().getIndex(cproject);			
//			this.resolveBindings = resolveBindings;
//			findMainFunction();
			this.projectASTCache 		= new HashMap<ITranslationUnit, IASTTranslationUnit>();
			
			this.tuList = CdtUtilities.getProjectTranslationUnits(cproject, new HashSet<String>());
			for (ITranslationUnit tu : tuList)
				projectASTCache.put(tu, null);
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}
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
	private IASTTranslationUnit getASTFromtTranslationUnit(ITranslationUnit tu) throws CoreException{
		//check if tu is null
		if (tu == null){
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageDialog.openInformation( shell, "Unexpected error in translation unit", "Unexpected error in translation unit (NULL)");
			throw new NullPointerException("Unexpected error in translation unit (NULL)");
		}
		//create index
		IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject() );
		projectIndex = index;

		// get AST
		IASTTranslationUnit ast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
//		IASTTranslationUnit ast = tu.getAST();//, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
		ast.accept(this);
		return ast;
	}	
				
	
	protected void setAST(ITranslationUnit tu) throws UnexpectedException, CoreException{
		this.mainAST = getASTFromtTranslationUnit(tu);
		this.mainTU  = tu;
	}
	
	///////////////////////////////////////////////
	//// Epsilon specific functions
	///////////////////////////////////////////////
	
	protected Collection<Object> getAllofKind(String kind){
		checkOfKind = true;
		return getAllVisitor(kind);
	}
	
	
	protected Collection<Object> getAllofType(String type){
		checkOfKind = false;
		return getAllVisitor(type);
	}
	
	
	private  Collection<Object> getAllVisitor(String type){
		List<Object> nodes = new ArrayList<Object>();
		try {
			for (ITranslationUnit tu : tuList){
				//check if tu is null
				if (tu == null){
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					MessageDialog.openInformation( shell, "Unexpected error in translation unit", "Unexpected error in translation unit (NULL)");
					throw new NullPointerException("Unexpected error in translation unit (NULL)");
				}
				//if its AST is not in the cache
				IASTTranslationUnit ast = projectASTCache.get(tu); 
				if (projectASTCache.get(tu)==null){
					// generate the AST and put it in cache
						ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
					projectASTCache.put(tu, ast);
				}
				
				ast.accept(new ASTGenericVisitor(true) {
					@Override
					protected int genericVisit(IASTNode node) {
						if (node.getClass().getSimpleName().equals(type)){							
							nodes.add(node);
						}
						else if (checkNode(node.getClass().getInterfaces(), type)){
							nodes.add(node);
						}
						else if (checkOfKind){
							Class<?> superClass = node.getClass().getSuperclass();
							while (superClass != Object.class) {
								if (superClass.getSimpleName().equals(type)) {
									nodes.add(node);
									break;
								}
								else if (checkNode(superClass.getInterfaces(), type)){
									nodes.add(node);
									break;
								}
								superClass = superClass.getSuperclass();
							}
						}
						return PROCESS_CONTINUE;
					}		
				});
			}
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}
		return nodes;		
	}
	
	private boolean checkNode(Class<?>[] interfacesArrays, String type ){
		Queue<Class<?>> interfaces = new LinkedList<Class<?>>(Arrays.asList(interfacesArrays));
		while (!interfaces.isEmpty()){
			Class<?> inteface  = interfaces.remove(); 
//			System.out.println(inteface.getSimpleName());
			if (inteface.getSimpleName().equals(type)){
				return true;
			}
			interfaces.addAll(Arrays.asList(inteface.getInterfaces()));
		}
		return false;
	}
	
	
	protected Collection<Object> getAllofTypeRecursion(String type){
		List<Object> nodes = new ArrayList<Object>(); 
		try {
//			System.out.println(getClass().getSimpleName() +".getAllofType(..)");
			//check if it is of type IAST
			ASTUtilities.findAllNodesInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IAST" + type), nodes);
		} 
		catch (ClassNotFoundException e) {
			try {
				//check if its of type ICPPAST
				ASTUtilities.findAllNodesInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.cpp.ICPPAST" + type), nodes);
			} catch (ClassNotFoundException e1) {
				try {
					//check if it is instance of CElement
					Class<?> clazz = Class.forName("org.eclipse.cdt.core.model." + type);
					return getAllofTypeFromCModel(clazz);
				} catch (ClassNotFoundException e2) {
					e2.printStackTrace();
				}
			}
		}
		
		return nodes;
	}
	
	
	
	@SuppressWarnings("rawtypes")
	private Collection<Object> getAllofTypeFromCModel (Class clazz) throws ClassNotFoundException{
		List<Object> elements = new ArrayList<Object>();
		
		//if I am asking for a project
		if (Arrays.asList(cproject.getClass().getInterfaces()).contains(clazz)){
			elements.add(cproject);
		}
		else{
			CdtUtilities.getElementsFromProject(cproject, clazz, elements);			
		}
		return elements;
	}
	
	
	
	////////////////////////////////////////////////////////
	//
	//Reduntant functions
	//
	////////////////////////////////////////////////////////
//	/**
//	 * Find the source file with main() function
//	 * @param cProject
//	 * @return
//	 * @throws NoSuchMethodException
//	 * @throws UnexpectedException
//	 * @throws CoreException
//	 * @throws ClassNotFoundException 
//	 */
//	@Deprecated
//	private void findMainFunction() throws NoSuchMethodException, UnexpectedException, CoreException, ClassNotFoundException{
//		//get source folders
//		for (ISourceRoot sourceRoot : cproject.getSourceRoots()){
//			System.out.println(sourceRoot.getLocationURI().getPath());
//			//get all elements
//			for (ICElement element : sourceRoot.getChildren()){
////				System.out.println(element.getElementName() +"\t"+ element.getElementType() +"\t"+element.getClass());
//				//find a .c or cpp file
//				if (element instanceof ITranslationUnit && ((ITranslationUnit) element).isSourceUnit()){
//					ITranslationUnit tu		= (ITranslationUnit) element;
//					IASTTranslationUnit ast = getASTFromtTranslationUnit(tu);
//					IASTNode node = ASTUtilities.findNodeInTree(ast, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", "main");
//					if (node != null && mainTU == null){
//						mainTU 		= tu;
//						mainAST 	= ast;
//					}
//					else if (node != null && mainTU != null){
//						throw new NoSuchMethodException(String.format("Multiple main() methods in project %s", cproject.getProject().getName()));
//					} 
//				}
//				
//			}
//		}
//		
//		if (mainTU ==null)
//			throw new NoSuchMethodException(String.format("main() method not found in project %s", cproject.getProject().getName()));
//		return;
//	}


//	/**
//	 * Return the AST for a project
//	 * 
//	 * @return IASTTranslationUnit
//	 * @throws CoreException 
//	 * @throws UnexpectedException 
//	 */
//	@Deprecated
//	protected IASTTranslationUnit getASTForFile(IFile file) throws CoreException, UnexpectedException {		
//		// 1) get AST for a workspace file
//		// if (parser == null){
//		// //Create translation unit for file
//		// ITranslationUnit tu = CoreModelUtil.findTranslationUnit(file);
//		// if (tu == null){
//		// Shell shell
//		// =PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//		// MessageDialog.openInformation( shell, "Unexpected error in
//		// translation unit", "Unexpected error in translation unit (NULL)");
//		// throw new UnexpectedException("Unexpected error in translation unit
//		// (NULL)");
//		// }
//		// //get AST
//		// IASTTranslationUnit iast = tu.getAST();
//		// }
//
//		// 2) get an AST for a code reader (i.e., adding additional properties)
//		// if (parser == null){
//		// FileContent fileContent =
//		// FileContent.createForExternalFileLocation(file.getRawLocation().toString());
//		// IScannerInfo info = new ScannerInfo(new HashMap<String, String>());
//		// IncludeFileContentProvider emptyIncludes =
//		// IncludeFileContentProvider.getEmptyFilesProvider();
//		// IIndex index = EmptyCIndex.INSTANCE;
//		// int options = ITranslationUnit.AST_SKIP_ALL_HEADERS |
//		// GPPLanguage.OPTION_NO_IMAGE_LOCATIONS |
//		// GPPLanguage.OPTION_IS_SOURCE_UNIT;
//		// IParserLogService log = new DefaultLogService();
//		//
//		// parser = GPPLanguage.getDefault().getASTTranslationUnit(fileContent,
//		// info, emptyIncludes, index, options , log);
//		// }
//
//		// 3) get an index-based AST for a code reader
//		if (mainTU == null) {
//			// Create translation unit for file
//			ITranslationUnit tu = CoreModelUtil.findTranslationUnit(file);
//			if (tu == null) {
//				Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
//				MessageDialog.openInformation(shell, "Unexpected error in translation unit",
//						"Unexpected error in translation unit (NULL)");
//				throw new UnexpectedException("Unexpected error in translation unit (NULL)");
//			}
//			//create index
//			IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject());
//			// get AST
//			IASTTranslationUnit iast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
//			iast.accept(this);
//		}
//		return null;
//	}


//	/**
//	 * Return AST
//	 * @return
//	 */
//	@Deprecated
//	protected IASTTranslationUnit getAST(){
//		return this.mainAST;
//	}
	
	
//	protected Collection<Object> getAllofTypeVisitor(String type){
//	List<Object> nodes = new ArrayList<Object>();
//	mainAST.accept(new ASTVisitor() {
//		{
//			shouldVisitTranslationUnit = true;
//			shouldVisitNamespaces   = true;
//		}
//		
//		@Override
//		public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
//			nodes.add(namespaceDefinition);
//			return PROCESS_CONTINUE;
//		}
//		@Override
//		public int visit(IASTTranslationUnit iast) {
//			System.out.println(iast.getOriginatingTranslationUnit());
//			return PROCESS_CONTINUE;
//		}
//	});
//	return nodes;
//}
//
//
//@Override
//protected int genericVisit(IASTNode node) {
////		System.out.println(node.getRawSignature());
//	return PROCESS_CONTINUE;
//}
//
//@Override
//public int visit(IASTDeclarator declarator) {
//	if (declarator instanceof IASTFunctionDeclarator){
//		
//	}
//	return PROCESS_CONTINUE;
//}
//
//@Override
//public int visit(IASTExpression exp) {
//	if (! (exp instanceof IASTFunctionCallExpression))
//		return PROCESS_CONTINUE;
//	IASTFunctionCallExpression funcCallExp 	= (IASTFunctionCallExpression)exp;
////	System.out.println(funcCallExp.getRawSignature());
//	return PROCESS_CONTINUE;
//}

}