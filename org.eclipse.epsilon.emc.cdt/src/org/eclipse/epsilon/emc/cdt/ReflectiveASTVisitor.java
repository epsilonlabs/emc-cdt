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
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTGenericVisitor;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;


@SuppressWarnings("restriction")
public class ReflectiveASTVisitor extends ASTGenericVisitor {

	/** project */
	protected ICProject cproject = null;

	/** Resolve bindings flag */
	private boolean resolveBindings = false;

	/** Source file that includes a main function of the c program*/
	private ITranslationUnit mainTU = null;

	/** Main AST */
	private IASTTranslationUnit mainAST = null;
	
	/** Index for this project*/
	protected IIndex projectIndex = null;
	
	
	
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
			this.projectIndex = CCorePlugin.getIndexManager().getIndex(cproject);			
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
		if (mainTU == null) {
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
		return null;
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
		IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject() );
		projectIndex = index;

		// get AST
		IASTTranslationUnit ast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
//		IASTTranslationUnit ast = tu.getAST();//, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
		ast.accept(this);
		return ast;
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
	private void findMainFunction() throws NoSuchMethodException, UnexpectedException, CoreException, ClassNotFoundException{
		//get source folders
		for (ISourceRoot sourceRoot : cproject.getSourceRoots()){
			System.out.println(sourceRoot.getLocationURI().getPath());
			//get all elements
			for (ICElement element : sourceRoot.getChildren()){
//				System.out.println(element.getElementName() +"\t"+ element.getElementType() +"\t"+element.getClass());
				//find a .c or cpp file
				if (element instanceof ITranslationUnit && ((ITranslationUnit) element).isSourceUnit()){
					ITranslationUnit tu		= (ITranslationUnit) element;
					IASTTranslationUnit ast = getASTFromtTranslationUnit(tu);
					IASTNode node = ASTUtilities.findNodeInTree(ast, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", "main");
					if (node != null && mainTU == null){
						mainTU 		= tu;
						mainAST 	= ast;
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
	 * Save the ast
	 * @deprecated
	 * @return
	 */
	protected boolean saveAST(){
		try {			
			if (mainTU.hasUnsavedChanges())
				mainTU.save(new NullProgressMonitor(), true);
			return true;
		} 
		catch (CModelException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 * Return AST
	 * @return
	 */
	protected IASTTranslationUnit getAST(){
		return this.mainAST;
	}
	
	
	protected void setAST(ITranslationUnit tu) throws UnexpectedException, CoreException{
		this.mainAST = getASTFromtTranslationUnit(tu);
		this.mainTU  = tu;
	}
	
	///////////////////////////////////////////////
	//// Epsilon specific functions
	///////////////////////////////////////////////
	
	protected Collection<Object> getAllofType(String type){
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
	
	
	protected Collection<Object> getAllofTypeVisitor(String type){
		List<Object> nodes = new ArrayList<Object>();
		mainAST.accept(new ASTVisitor() {
			{
				shouldVisitTranslationUnit = true;
				shouldVisitNamespaces   = true;
			}
			
			@Override
			public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
				nodes.add(namespaceDefinition);
				return PROCESS_CONTINUE;
			}
			@Override
			public int visit(IASTTranslationUnit iast) {
				System.out.println(iast.getOriginatingTranslationUnit());
				return PROCESS_CONTINUE;
			}
		});
		return nodes;
	}
	
	
	@Override
	protected int genericVisit(IASTNode node) {
//			System.out.println(node.getRawSignature());
		return PROCESS_CONTINUE;
	}
	
	@Override
	public int visit(IASTDeclarator declarator) {
		if (declarator instanceof IASTFunctionDeclarator){
			
		}
		return PROCESS_CONTINUE;
	}
	
	@Override
	public int visit(IASTExpression exp) {
		if (! (exp instanceof IASTFunctionCallExpression))
			return PROCESS_CONTINUE;
		IASTFunctionCallExpression funcCallExp 	= (IASTFunctionCallExpression)exp;
//		System.out.println(funcCallExp.getRawSignature());
		return PROCESS_CONTINUE;
	}
}