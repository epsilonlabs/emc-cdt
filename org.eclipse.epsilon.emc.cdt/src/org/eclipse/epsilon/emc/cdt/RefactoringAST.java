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

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IName;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPEnumeration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMember;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPUnknownBinding;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.epsilon.common.util.ListSet;
import org.eclipse.epsilon.emc.cdt.utilities.Digraph;
import org.eclipse.ltk.core.refactoring.Change;

public class RefactoringAST {

	/** project */
	protected ICProject cproject = null;

	/** project index */
	protected IIndex projectIndex = null;
	
	/** Pairs of ITranslationUnit, IASTTranslationUnit **/
	HashMap<ITranslationUnit, IASTTranslationUnit> astCache = new HashMap<ITranslationUnit, IASTTranslationUnit>();

	/** Pairs of elements-potential name from standard C++ library that should be included using #include directives*/
	LinkedHashMap<IASTName, String> includeDirectivesMap = new LinkedHashMap<IASTName, String>();


	private static String elements[] = { "tinyxml2" };
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Set REFACTORING_NAMESPACES = new HashSet(Arrays.asList(elements));

	private static final String myLIBRARYhpp = "LibXML.h";
	private static final String myLIBRARYcpp = "LibXML.cpp";
	private static final String myNAMESPACE  = "libxml";
	private static final String myDIR        = "src/LibXML";

	/** Keep refactoring information*/
	NamesSet 	namesSet 	 = new NamesSet();
	BindingsSet bindingsSet  = new BindingsSet();
	//FIXME: not correct, do not use this list
	List<IASTNode> nodesList = new ArrayList<IASTNode>();

	
	
	/** Class constructor */
	public RefactoringAST(ICProject project) {
		try {
			this.cproject = project;
			this.projectIndex = CCorePlugin.getIndexManager().getIndex(cproject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	
 	protected void refactor(final String[] excludedFiles) {
		try {
			/**Pairs of ITranslationUnit, List<IASTName>, where List
			 * <IASTName> keeps the IASTNames used from the legacy library **/
			HashMap<ITranslationUnit, List<IASTName>> libraryCache = new HashMap<>();
			
			List<ITranslationUnit> tusUsingLibList = new ArrayList<ITranslationUnit>();

			// find all translation units
			List<ITranslationUnit> tuList = CdtUtilities.getProjectTranslationUnits(cproject, excludedFiles);

			// for each translation unit get its AST
			for (ITranslationUnit tu : tuList) {
				// System.out.println(tu.getFile().getName() +"\t"+
				// tu.getFile().getLocation());

				// get AST for that translation unit
				IASTTranslationUnit ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);

				// cache the tu & ast pair
				astCache.put(tu, ast);

				NameFinderASTVisitor fcVisitor = new NameFinderASTVisitor();
				ast.accept(fcVisitor);
				
				// if the list is not empty, then it uses the legacy library --> add it to cached library
				if (fcVisitor.libraryCallsExist()) {
					libraryCache.put(tu, fcVisitor.namesList);
					namesSet.addAll(fcVisitor.namesSet);
					bindingsSet.addAll(fcVisitor.bindingsSet);
					nodesList.addAll(fcVisitor.nodesList);
					tusUsingLibList.add(tu);
				}
			}
			
			//FIXME: -ea VM flag does not work - how to enable assertions?
//			assert(namesSet.size() == bindingsSet.size());
//			assert(namesSet.size() == nodesList.size());
			
			System.out.println(Arrays.toString(tusUsingLibList.toArray()));
			
			//check for library uses within the same library
			checkReferences();

			System.out.println(namesSet.size() +"\t"+ bindingsSet.size() +"\t"+ nodesList.size());
			for (int i=0; i<namesSet.size(); i++){
				System.out.println(namesSet.getList().get(i) +"\t"+ bindingsSet.getList().get(i).getClass().getSimpleName());// +"\t"+ nodesList.get(i));
			}

			//find mappings class - members
			Map<ICPPClassType, List<ICPPMember>> classMembersMap = null; 
			classMembersMap = createClassMembersMapping();

			//create header file
			createHeader(classMembersMap);

			//create source file
			createSource(classMembersMap);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

 	
 	private void checkReferences () {
 		try {
 			BindingsSet bindings = new BindingsSet();
 			bindings.addAll(bindingsSet);

			projectIndex.acquireReadLock();
	 		for (IBinding binding : bindings){
	 			if (binding instanceof ICPPClassType){
	 				System.out.println(binding + "\t ICPPClassType");
	 				ICPPClassType classBinding = (ICPPClassType)binding;	 				
	 				checkClassInheritance(classBinding);
	 			
	 				for (ICPPConstructor constructor : classBinding.getConstructors()){
//	 					IIndexName[] methodDecls = projectIndex.findNames(constructor, IIndex.FIND_DECLARATIONS);
//	 					if (methodDecls.length > 0){						
//	 						ICPPASTFunctionDeclarator methodDecl = (ICPPASTFunctionDeclarator) findNodeFromIndex(methodDecls[0], ICPPASTFunctionDeclarator.class);
//	 						namesSet.add(methodDecl.getName());
//	 						bindingsSet.add(constructor);
//	 					}
	 					checkMethodSignature(constructor);
	 				}
	 			}
	 			else if (binding instanceof IEnumeration){
	 				System.out.println(binding + "\t IEnumeration");
	 			}
	 			else if (binding instanceof ICPPMethod){
	 				System.out.println(binding + "IMethod");
	 				checkMethodSignature((ICPPMethod)binding);
	 			}
	 		}	
 		} 
 		catch (InterruptedException | CoreException | DOMException  e) {		
 			e.printStackTrace();		
 		}
 		finally{
 			projectIndex.releaseReadLock();
 		}
 	}

 	
 	/**
 	 * Check inheritance for a given class
 	 * @param binding
 	 * @throws CoreException
 	 */
 	private void checkClassInheritance (ICPPClassType classBinding) throws CoreException{
		for (ICPPBase baseClazz : classBinding.getBases()){
			IBinding baseBinding = baseClazz.getBaseClass();
			
			if (baseBinding instanceof ICPPClassType){
				//if the base binding (base class) is not in the bindings set, 
				if (!bindingsSet.contains(baseBinding)){
					IIndexName[] cDefs = projectIndex.findNames(baseBinding, IIndex.FIND_DEFINITIONS);
					if (cDefs.length > 0){						
						bindingsSet.add(baseBinding);
						ICPPASTName nameNode = (ICPPASTName) findNodeFromIndex(cDefs[0], ICPPASTName.class);
						namesSet.add(nameNode);
					}
					//recursively check the base binding (parent class)
					checkClassInheritance((ICPPClassType)baseBinding);
				}
			}	
		}
 	}
 	
 
 	
 	private void checkMethodSignature(ICPPMethod methodBinding) throws CoreException, DOMException{
		IIndexName[] methodDecls = projectIndex.findNames(methodBinding, IIndex.FIND_DECLARATIONS);
		if (methodDecls.length > 0){						
			ICPPASTFunctionDeclarator methodDecl = (ICPPASTFunctionDeclarator) findNodeFromIndex(methodDecls[0], ICPPASTFunctionDeclarator.class);
			
			//check return type
			IASTNode parent = methodDecl.getParent(); 
			IASTDeclSpecifier returnDeclSpecifier = null;
			if (parent instanceof ICPPASTFunctionDefinition)
				returnDeclSpecifier = ((ICPPASTFunctionDefinition)parent).getDeclSpecifier();
			else if (parent instanceof IASTSimpleDeclaration)
				returnDeclSpecifier = ((IASTSimpleDeclaration)parent).getDeclSpecifier();
			
			if (returnDeclSpecifier instanceof ICPPASTNamedTypeSpecifier){
				checkDeclSpecifier(returnDeclSpecifier);
			}
			
			
			//check parameters
			ICPPASTParameterDeclaration paramDecls[] = methodDecl.getParameters();
			for (ICPPASTParameterDeclaration paramDecl :paramDecls ){
				IASTDeclSpecifier paramDeclSpecifier = paramDecl.getDeclSpecifier();
				
				checkDeclSpecifier(paramDeclSpecifier);
				
//				//if it's not not a simple specifier (void, int, double, etc.) 
//				//1) if it's part of the legacy library, include it in the set of elements to be migrated
//				//2) if it's part of a standard c++ library, add it to the set of include directives
//				if (paramDeclSpecifier instanceof ICPPASTNamedTypeSpecifier){
//					IASTName paramSpecifierName 	= ((ICPPASTNamedTypeSpecifier) paramDeclSpecifier).getName();
//					IBinding paramSpecifierBinding	= paramSpecifierName.resolveBinding();
//					//find where the param specifier is defined
//					IIndexName[] paramSpecifierDefs = projectIndex.findNames(paramSpecifierBinding, IIndex.FIND_DEFINITIONS); 
//					if (paramSpecifierDefs.length>0){
//						IIndexName paramSpecifierDef = paramSpecifierDefs[0];
//						IASTSimpleDeclaration node 	= (IASTSimpleDeclaration)findNodeFromIndex(paramSpecifierDef, IASTSimpleDeclaration.class);
//						
//						// while not reached a namespace scope
//						ICPPNamespaceScope scope = (ICPPNamespaceScope) paramSpecifierBinding.getScope();
//
//						while (!((scope != null) && (scope instanceof ICPPNamespaceScope))) {
//							scope = (ICPPNamespaceScope) scope.getParent();
//						}
//
//						IName scopeName = scope.getScopeName();
//						if ( (scopeName != null) &&
//						     (REFACTORING_NAMESPACES.contains(scopeName.toString())) ){
//								bindingsSet.add(paramSpecifierBinding);
//								namesSet.add(paramSpecifierName);	
//						}
//						else{//it is an include directive from the c++ libs				
//							System.out.println(node +"\t"+ node.getContainingFilename() +"\t"+ node.getTranslationUnit().getFilePath());
//							includeDirectivesMap.put(paramSpecifierName, node.getContainingFilename());
//						}
//					}
//				}
			}
		}

 	}
 	
 	
 	private void checkDeclSpecifier(IASTDeclSpecifier declSpecifier) throws CoreException, DOMException{
		//if it's not not a simple specifier (void, int, double, etc.) 
		//1) if it's part of the legacy library, include it in the set of elements to be migrated
		//2) if it's part of a standard c++ library, add it to the set of include directives
		if (declSpecifier instanceof ICPPASTNamedTypeSpecifier){
			IASTName declSpecifierName 	= ((ICPPASTNamedTypeSpecifier) declSpecifier).getName();
			IBinding declSpecifierBinding	= declSpecifierName.resolveBinding();
			//find where the param specifier is defined
			IIndexName[] paramSpecifierDefs = projectIndex.findNames(declSpecifierBinding, IIndex.FIND_DEFINITIONS); 
			if (paramSpecifierDefs.length>0){
				IIndexName paramSpecifierDef = paramSpecifierDefs[0];
				IASTSimpleDeclaration node 	= (IASTSimpleDeclaration)findNodeFromIndex(paramSpecifierDef, IASTSimpleDeclaration.class);
				
				// while not reached a namespace scope
				ICPPNamespaceScope scope = (ICPPNamespaceScope) declSpecifierBinding.getScope();

				while (!((scope != null) && (scope instanceof ICPPNamespaceScope))) {
					scope = (ICPPNamespaceScope) scope.getParent();
				}

				IName scopeName = scope.getScopeName();
				if ( (scopeName != null) &&
				     (REFACTORING_NAMESPACES.contains(scopeName.toString())) ){
						bindingsSet.add(declSpecifierBinding);
						namesSet.add(declSpecifierName);	
				}
				else{//it is an include directive from the c++ libs				
					System.out.println(node +"\t"+ node.getContainingFilename() +"\t"+ node.getTranslationUnit().getFilePath());
					includeDirectivesMap.put(declSpecifierName, node.getContainingFilename());
				}
			}
		}
 	}
 	
 	
 	/**
 	 * Check inheritance for a given class
 	 * @param binding
 	 * @throws CoreException
 	 */
 	@Deprecated
  	private void checkClassInheritanceDep (ICPPClassType binding) throws CoreException{
		IIndexName definitions[] = projectIndex.findDefinitions(binding);
		for (IIndexName def : definitions) {

			IASTNode fdecl = findNodeFromIndex(def, ICPPASTCompositeTypeSpecifier.class);
			
			if (fdecl != null){
//				System.out.println(fdecl.getRawSignature());
				final IASTNode node = fdecl; 
				node.accept(new ASTVisitor() {
					{shouldVisitBaseSpecifiers = true;
					 shouldVisitDecltypeSpecifiers = true;}
					@Override
					public int visit(ICPPASTBaseSpecifier baseSpecifier) {
//						System.out.println("Bingo");
						ICPPASTNameSpecifier n = baseSpecifier.getNameSpecifier();
						IBinding b = n.resolveBinding();
						b = n.resolvePreBinding();
						if (b instanceof ICPPClassType && !bindingsSet.contains(b))
							try {
								if (n instanceof IASTName){
									if (namesSet.add((IASTName) n)){
										bindingsSet.add(b);
										nodesList.add(node);	
									}
									//check recursively
									checkClassInheritance((ICPPClassType)b);
								}							
								else 
									throw new CoreException(new Status(IStatus.ERROR, "CDT", n.toString() + "is not instanceof IASTName"));
							}
							catch (CoreException e) {
								e.printStackTrace();
							}
						return PROCESS_CONTINUE;
					}
				});
			}
		}
 	}
 	 	
	
 	/**
 	 * Create the header for this library
 	 * @param classMembersMap
 	 */
	private void createHeader (Map<ICPPClassType, List<ICPPMember>> classMembersMap) {
		try {
			//
			IFile file = CdtUtilities.createNewFile(cproject, myDIR, myLIBRARYhpp);
			if (file == null)
				throw new NoSuchFileException("Could not create source file " + myDIR + "/" + myLIBRARYhpp);

			// Create translation unit for file
			ITranslationUnit libTU = CoreModelUtil.findTranslationUnit(file);
			// get ast
			IASTTranslationUnit headerAST = libTU.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
			// get rewriter
			ASTRewrite rewriter = ASTRewrite.create(headerAST);
			// get node factory
			ICPPNodeFactory nodeFactory = (ICPPNodeFactory) headerAST.getASTNodeFactory();

			//1) add include directives
			for (IASTName name : includeDirectivesMap.keySet()){
				String includeDirective = includeDirectivesMap.get(name);
				IASTName includeDir = nodeFactory.newName("#include <" + includeDirective +">");
				rewriter.insertBefore(headerAST, null, includeDir, null);
			}
			
			//2) add using directives
//			ICPPASTUsingDirective usingDirective = nodeFactory.newUsingDirective(nodeFactory.newName("tinyxml2"));
//			rewriter.insertBefore(libAST, null, usingDirective, null);

			//3) add namespace definition
			ICPPASTNamespaceDefinition nsDef = nodeFactory.newNamespaceDefinition(nodeFactory.newName(myNAMESPACE));
			
			//4) create forward declarations
			refactorForwardDeclarations(nsDef, nodeFactory, classMembersMap.keySet());
			
			//5) Refactor enumerations
			refactorEnumerations(nsDef);
			
			//6) Refactor classes and methods
			refactorClasses(nodeFactory, classMembersMap, nsDef);
			
			//7) add namespace to ast
			rewriter.insertBefore(headerAST, null, nsDef, null);
			rewriter.rewriteAST().perform(new NullProgressMonitor()); 
		} 
		catch (NoSuchFileException | CoreException e) {
			e.printStackTrace();
		}
	}
	
	
 	/**
 	 * Create the source for this library
 	 * @param classMembersMap
 	 */
	private void createSource (Map<ICPPClassType, List<ICPPMember>> classMembersMap) {
		try {
			//
			IFile file = CdtUtilities.createNewFile(cproject, myDIR, myLIBRARYcpp);
			if (file == null)
				throw new NoSuchFileException("Could not create source file " + myDIR + "/" + myLIBRARYcpp);

			// Create translation unit for file
			ITranslationUnit libTU = CoreModelUtil.findTranslationUnit(file);
			// get ast
			IASTTranslationUnit sourceAST = libTU.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
			// get rewriter
			ASTRewrite rewriter = ASTRewrite.create(sourceAST);
			// get node factory
			ICPPNodeFactory nodeFactory = (ICPPNodeFactory) sourceAST.getASTNodeFactory();

			//1) add include directives
			IASTName myLibInclude = nodeFactory.newName("#include \"" + myLIBRARYhpp +"\"");
			rewriter.insertBefore(sourceAST, null, myLibInclude, null);
			IASTName iostreamINclude = nodeFactory.newName("#include <iostream>");
			rewriter.insertBefore(sourceAST, null, iostreamINclude, null);
			
			//2) add using directives
//			ICPPASTUsingDirective usingDirective = nodeFactory.newUsingDirective(nodeFactory.newName("tinyxml2"));
//			rewriter.insertBefore(libAST, null, usingDirective, null);

			//3) add namespace definition
			ICPPASTNamespaceDefinition nsDef = nodeFactory.newNamespaceDefinition(nodeFactory.newName(myNAMESPACE));
			
			//4) Refactor classes and methods
			refactorFunctionImplementations(nodeFactory, classMembersMap, nsDef);
			
			//6) add namespace to ast
			rewriter.insertBefore(sourceAST, null, nsDef, null);
			rewriter.rewriteAST().perform(new NullProgressMonitor()); 
		} 
		catch (NoSuchFileException | CoreException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Create forward declarations
	 * @param nsDef
	 * @throws CoreException
	 */
	private void refactorForwardDeclarations (ICPPASTNamespaceDefinition nsDef, ICPPNodeFactory nodeFactory, Set<ICPPClassType> classBindings) throws CoreException{		
		for (ICPPClassType binding : classBindings){
			//add forward declaration for classes
			if (binding instanceof ICPPClassType){
				IASTName name 							 = nodeFactory.newName(binding.getName());
				ICPPASTElaboratedTypeSpecifier specifier = nodeFactory.newElaboratedTypeSpecifier(ICPPASTCompositeTypeSpecifier.k_class, name);				
				IASTSimpleDeclaration declaration		 = nodeFactory.newSimpleDeclaration(specifier);
				nsDef.addDeclaration(declaration);
			}
		}
	}
	
	
	/**
	 * Refactor enumeration
	 * @param nsDef
	 * @throws CoreException
	 */
	private void refactorEnumerations (ICPPASTNamespaceDefinition nsDef) throws CoreException{
		for (int index=0; index<bindingsSet.size(); index++){
			IBinding binding = bindingsSet.getList().get(index);
			
			//do something with the enumeration
			if (binding instanceof IEnumeration){
				
				//get enumeration binding
				IEnumeration enumeration = (IEnumeration)binding;
				
				//find definitions
				IIndexName[] defs = projectIndex.findDefinitions(enumeration);
				
				if (defs.length > 0){
					IIndexName def    = defs[0];
					
					IASTNode fdecl = findNodeFromIndex(def, IASTSimpleDeclaration.class);
					
					IASTSimpleDeclaration enumDeclaration = ((IASTSimpleDeclaration)fdecl).copy(CopyStyle.withLocations);
					
					//append enumeration to namespace
					nsDef.addDeclaration(enumDeclaration);
				}
			}
		}
	}
	
	
	/**
	 * Refactor classes and methods
	 * @param nsDef
	 * @param nodeFactory
	 * @throws CoreException
	 * @throws DOMException 
	 */
	private void refactorClasses (ICPPNodeFactory nodeFactory, Map<ICPPClassType, List<ICPPMember>> classMembersMap, ICPPASTNamespaceDefinition nsDef) throws CoreException{		
		for (ICPPClassType owningclass : classMembersMap.keySet()){			
			
			//create the class
			IIndexName[] classDefs = projectIndex.findNames(owningclass, IIndex.FIND_DEFINITIONS);
			if (classDefs.length != 1 )
				throw new NoSuchElementException("Class " + owningclass.getName() +" has "+ classDefs.length +" definitions!");
		
			ICPPASTCompositeTypeSpecifier newClass = nodeFactory.newCompositeTypeSpecifier(ICPPASTCompositeTypeSpecifier.k_class, nodeFactory.
																	newName(owningclass.getName()));
			
			//if the class inherits from other classes, append this
			ICPPASTCompositeTypeSpecifier classNode = (ICPPASTCompositeTypeSpecifier)findNodeFromIndex(classDefs[0], ICPPASTCompositeTypeSpecifier.class);
			for (ICPPASTBaseSpecifier superclass :classNode.getBaseSpecifiers()){
				newClass.addBaseSpecifier(superclass.copy(CopyStyle.withLocations));
			}
						
			
			//create members of this class, group the based on their visibilities (//TODO optimise this)
			List<ICPPMember> membersList = classMembersMap.get(owningclass);
			int visibilities[] = new int[]{ICPPASTVisibilityLabel.v_public, ICPPASTVisibilityLabel.v_protected, ICPPASTVisibilityLabel.v_private};
			
			for (int visibility : visibilities){
				boolean visLabelAdded = false;
				
				for (ICPPMember member : membersList){
					if (member.getVisibility() != visibility)
						continue;
					
					//add visibility label
					if (!visLabelAdded){
						newClass.addDeclaration(nodeFactory.newVisibilityLabel(member.getVisibility()));
						visLabelAdded = true;
					}
	
					//add declaration
					IIndexName[] memberDecls = projectIndex.findNames(member, IIndex.FIND_DECLARATIONS); //. Declarations(constructor);
					if (memberDecls.length > 0){ // its size should be 1
						IIndexName mDecl    = memberDecls[0];
						
						IASTSimpleDeclaration node = (IASTSimpleDeclaration)findNodeFromIndex(mDecl, IASTSimpleDeclaration.class);
						IASTSimpleDeclaration newDeclaration = (node).copy(CopyStyle.withLocations);
						
						newClass.addMemberDeclaration(newDeclaration);
					}
					else {//if no declaration exists, try to find a definition and extract the declaration
						IIndexName[] memberDefs = projectIndex.findNames(member, IIndex.FIND_DEFINITIONS);
						if (memberDefs.length > 0){ // its size should be 1
							IIndexName mDef    = memberDefs[0];
							
							ICPPASTFunctionDefinition node 	  = (ICPPASTFunctionDefinition) findNodeFromIndex(mDef, ICPPASTFunctionDefinition.class);
							IASTFunctionDeclarator declarator = node.getDeclarator().copy(CopyStyle.withLocations); 
							IASTDeclSpecifier      specifier  = node.getDeclSpecifier().copy(CopyStyle.withLocations);
							IASTSimpleDeclaration newDeclaration = nodeFactory.newSimpleDeclaration(specifier);
							
							newDeclaration.addDeclarator(declarator);						
							newClass.addMemberDeclaration(newDeclaration);	
						}
					}
				}//end member
			}//end visibitilies

			//add the new class to the namespace
			IASTSimpleDeclaration newDeclaration = nodeFactory.newSimpleDeclaration(newClass);
			nsDef.addDeclaration(newDeclaration);
		}
	}
	
	
	/**
	 * Refactor classes and methods
	 * @param nsDef
	 * @param nodeFactory
	 * @throws CoreException
	 * @throws DOMException 
	 */
	private void refactorFunctionImplementations (ICPPNodeFactory nodeFactory, Map<ICPPClassType, List<ICPPMember>> classMembersMap, 
												 	ICPPASTNamespaceDefinition nsDef) throws CoreException{		
		
		for (ICPPClassType owningclass : classMembersMap.keySet()){

			//create function definitions
			List<ICPPMember> membersList = classMembersMap.get(owningclass);
			for (ICPPMember member : membersList){
			
				if (member instanceof ICPPMethod){
					//add definition
					IIndexName[] methodDefs = projectIndex.findNames(member, IIndex.FIND_DEFINITIONS);
					
					if (methodDefs.length > 0){ // its size should be 1
						IIndexName mDef    = methodDefs[0];
						ICPPASTFunctionDefinition node = (ICPPASTFunctionDefinition)findNodeFromIndex(mDef, ICPPASTFunctionDefinition.class);
						
						//create new function definition
						//we need to do it manually because of the chain initialisers, added later in this function
//						ICPPASTFunctionDefinition newFunctionDef = node.copy(CopyStyle.withLocations);
//						newFunctionDef.setBody(nodeFactory.newCompoundStatement());
						ICPPASTFunctionDefinition newFunctionDef = nodeFactory.newFunctionDefinition(node.getDeclSpecifier().copy(CopyStyle.withLocations),
								  																	 node.getDeclarator().copy(CopyStyle.withLocations),
								  																	 nodeFactory.newCompoundStatement());
																		
						//manage function declarator qualified names so that are in the form Class::Function(...){}, in cpp file. 
						//if the specifier of this function does not include class name (e.g., defined in header) modify this
						ICPPASTFunctionDeclarator fDecl = (ICPPASTFunctionDeclarator) newFunctionDef.getDeclarator();
						if (!(fDecl.getName() instanceof ICPPASTQualifiedName)){
							ICPPASTQualifiedName qualName =  nodeFactory.newQualifiedName(new String[]{owningclass.getName()}, member.getName());
							fDecl.setName(qualName);
						}
						
						//manage initialiser lists: any constructor (superclass) initialisers should be added here 
						for (ICPPASTConstructorChainInitializer initialiser: node.getMemberInitializers()){
							IASTName initialiserName = initialiser.getMemberInitializerId();
							IBinding initialiserBinding = initialiserName.resolveBinding(); 
							
							if ( (initialiserBinding instanceof ICPPConstructor) ){
								IBinding bindingClass = initialiserBinding.getOwner();
								if (classMembersMap.keySet().contains(bindingClass))
									newFunctionDef.addMemberInitializer(initialiser.copy(CopyStyle.withLocations));
								else 
									throw new IllegalArgumentException("Class " + bindingClass + "not found!");	
							}
						}
						
						//manage return function specifiers
						IASTDeclSpecifier declSpecifier 	= node.getDeclSpecifier();
						IASTReturnStatement returnStatement	= nodeFactory.newReturnStatement(null);
						//if the return type is simple specifier except void --> add a null return statement
						if ( (declSpecifier instanceof IASTSimpleDeclSpecifier) && 
							 (((IASTSimpleDeclSpecifier)declSpecifier).getType() > IASTSimpleDeclSpecifier.t_void) ){
							returnStatement.setReturnValue(nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_nullptr, "NULL"));
						}
						//if the return type is a qualified name (e.g., class, enumeration etc) --> 
						//   if it is a class --> add a null return statement
						//   if it is an enumerator --> select a random enum item
						else if (declSpecifier instanceof ICPPASTNamedTypeSpecifier){
							IASTName declName 		= ((ICPPASTNamedTypeSpecifier) declSpecifier).getName();
							IBinding declBinding	= declName.resolveBinding();

							if (declBinding instanceof ICPPEnumeration){
								IIndexName[] enumDefs = projectIndex.findNames(declBinding, IIndex.FIND_DEFINITIONS);
								if (enumDefs.length > 0){ // its size should be 1
									IIndexName enumDef    = enumDefs[0];
									IASTEnumerationSpecifier enumNode 	  = (IASTEnumerationSpecifier) findNodeFromIndex(enumDef, IASTEnumerationSpecifier.class);
									if (enumNode.getEnumerators().length !=1)
										returnStatement.setReturnValue(nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_false, enumNode.getEnumerators()[0].getName().toString()));
									else 
										throw new IllegalArgumentException("Enumerator " + declBinding + "not found!");	
								}
							}
							else
								returnStatement.setReturnValue(nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_nullptr, "NULL"));
								
						}
						IASTCompoundStatement compoundStatement = (IASTCompoundStatement) newFunctionDef.getBody();
						compoundStatement.addStatement(returnStatement);


						//add the new definition to the namespace
						nsDef.addDeclaration(newFunctionDef);
					}
					
				}
			}
		}				
	}
	
	
	/**
	 * Given the binding set, generate a hash map that comprises the 
	 * @return
	 */
	private LinkedHashMap<ICPPClassType, List<ICPPMember>> createClassMembersMapping(){
		HashMap<ICPPClassType, List<ICPPMember>> classMembersMap = new HashMap<ICPPClassType, List<ICPPMember>>(); 
		
		for (IBinding binding : bindingsSet){			
			//if it is a member of a class (method or field) & its owner is actually a class
			if ( (binding instanceof ICPPMember) && (binding.getOwner() instanceof ICPPClassType) ){					
				ICPPMember 	  classMember = (ICPPMember)binding;
				ICPPClassType owningClass = (ICPPClassType)binding.getOwner();

				//if this class is not in the hashmap, create an arraylist and add the mapping
				if (!classMembersMap.containsKey(owningClass)){
					ArrayList<ICPPMember> membersList = new ArrayList<ICPPMember>();
					membersList.addAll(Arrays.asList(owningClass.getConstructors()));//add class constructors
					membersList.add(classMember); //add the member
					classMembersMap.put(owningClass, membersList);
				}
				//if the class exists in the hashmap, simply add the member to the list
				else{
					classMembersMap.get(owningClass).add(classMember);
				}
			}
			//if it is a class and does exist in the hashmap, create a mapping with an empty array list
			else if (binding instanceof ICPPClassType){
				ICPPClassType owningClass = (ICPPClassType)binding;
				if (!classMembersMap.containsKey(owningClass)){
					ArrayList<ICPPMember> membersList = new ArrayList<ICPPMember>();
					membersList.addAll(Arrays.asList(owningClass.getConstructors()));//add class constructors
					classMembersMap.put(owningClass, membersList);
				}
//				else //duplicates should not exists at this point
//					throw new IllegalArgumentException("Class " + owningClass.getName() + " already exists in hashmap");
			}
		}
		
		//once the mapping is done, do a dependency/inheritance topological sorting of the classes
		//do determine their insertion order in the header file
		//TODO: optimise this; it can be embedded into the previous for loop, or in checkClassInheritance()
		Digraph<ICPPClassType> bindingsGraph = new Digraph<ICPPClassType>();
		for (ICPPClassType classBinding : classMembersMap.keySet()){		
			bindingsGraph.add(classBinding);

			//find base classes and add them to the DAG
			for (ICPPBase baseClazz : classBinding.getBases()){
				IBinding baseBinding = baseClazz.getBaseClass();
					
				//if the base binding (base class) is not in the bindings set
				//then checkClassInheritance() failed
				if (!bindingsSet.contains(baseBinding))
					throw new NoSuchElementException("Base class " + baseBinding + "not exists in bindings set!");
					
				if (baseBinding instanceof ICPPClassType)
					bindingsGraph.add((ICPPClassType)baseBinding, classBinding);
			}
		}
//        System.out.println("In-degrees: " + bindingsGraph.inDegree());
//        System.out.println("Out-degrees: " + bindingsGraph.outDegree());
        System.out.println("\nA topological sort of the vertices: " + bindingsGraph.topSort());
        System.out.println("The graph " + (bindingsGraph.isDag()?"is":"is not") + " a dag\n");
		
		List<ICPPClassType> topSortedBindings = bindingsGraph.topSort();
		LinkedHashMap<ICPPClassType, List<ICPPMember>> sortedClassMembersMap = new LinkedHashMap<ICPPClassType, List<ICPPMember>>(); 
		for (ICPPClassType binding : topSortedBindings){
			sortedClassMembersMap.put(binding, classMembersMap.get(binding));
		}
		
		return sortedClassMembersMap;

	}
	
	
	/**
	 * Given an index name and a set of class names, this function searches the 
	 * parent of the node with that name until it finds the parent which is instance of
	 * 
	 */
	@SuppressWarnings("rawtypes")
	private IASTNode findNodeFromIndex(IIndexName indexName, Class...classes){
		try {
			//find translation unit & corresponding ast, cache ast if necessary
			ITranslationUnit tu;
			tu = CdtUtilities.getTranslationUnitFromIndexName(indexName);
			IASTTranslationUnit ast = null;
			if (astCache.containsKey(tu)){
				ast = astCache.get(tu);
			}
			else{
				ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
				astCache.put(tu, ast);
			}
			
			//find enumeration 
			IASTName name = (IASTName) ast.getNodeSelector(null).findEnclosingNode(indexName.getNodeOffset(), indexName.getNodeLength());
			IASTNode node = name;
			
			while ( (node != null) && !(nodeIsInstance(classes, node)) ){
				node =  node.getParent();
			}
			assert (nodeIsInstance(classes, node));
			return node;
		} 
		catch (CoreException e){
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * Checks if this node is instance of any of the given classes
	 * @param classes
	 * @param node
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private boolean nodeIsInstance (Class [] classes, IASTNode node){
		for (Class clazz: classes){
			if (clazz.isInstance(node))
				return true;
		}
		return false;
	}
	
	
	private class NameFinderASTVisitor extends ASTVisitor {
		/** List keeping all important IASTNames**/
		private NamesSet namesSet;
		
		/** Set keeping all important IASTNames, but doesn't accept duplicates **/
		private List<IASTName> namesList;

		/** List keeping all important IBindings, but doesn't accept duplicates **/
		private BindingsSet bindingsSet;
		
		
		private List<IASTNode> nodesList;


		// static initialiser: executed when the class is loaded
		{
			shouldVisitNames 					= true;
			shouldVisitExpressions				= true;
			shouldVisitParameterDeclarations	= true;
			shouldVisitDeclarations 			= true;
			//
			namesSet 		= new NamesSet();
			bindingsSet  	= new BindingsSet();
			namesList		= new ArrayList<IASTName>();
			nodesList		= new ArrayList<IASTNode>();
					
		}


		private boolean libraryCallsExist(){
			return namesSet.size()>0;
		}

		
		/**
		 * This is to capture (1) functions that belong to a class: e.g.,
		 * {@code xmlDoc.LoadFile(filename)} (2) function that <b>do not</b>
		 * belong to a class: e.g., {@code printf("%s", filename)}
		 */
		@Override
		public int visit(IASTExpression exp) {
			if (!(exp instanceof IASTFunctionCallExpression))
				return PROCESS_CONTINUE;
			IASTFunctionCallExpression funcCallExp = (IASTFunctionCallExpression) exp;

			IASTExpression funcExpression = funcCallExp.getFunctionNameExpression();

			IASTName name = null;
			IASTNode node = null; 
			
			// Functions that belong to a class: e.g.,
			// xmlDoc.LoadFile(filename);
			if (funcExpression instanceof IASTFieldReference) {
				// get the field reference for this: e.g., xmlDoc.LoadFile
				IASTFieldReference fieldRef = (IASTFieldReference) funcExpression;
				// get the name
				name = fieldRef.getFieldName();
				node = fieldRef;
			}
			// Functions that *do not* belong to a class: 
			//e.g., printf("%s", filename);
			else if (funcExpression instanceof IASTIdExpression) {
				// get the function name: e.g., printf
				IASTIdExpression idExp = (IASTIdExpression) funcExpression;
				// get the name
				name = idExp.getName();
				node = idExp;
			}

			if (name != null) {
				// get the binding
				IBinding binding = name.resolveBinding();
				// check whether this binding is part of the legacy library
				checkBinding(name, binding, node);
//				boolean inLibrary = checkBinding(binding);
//				if (inLibrary){
//					appendToLists(name, binding, node);
//				}
			}
			return PROCESS_CONTINUE;
		}

		/**
		 * This is to capture parameters in function definitions: e.g.,
		 * {@code void testParamDecl (XMLDocument doc, const char *filename)}
		 */
		@Override
		public int visit(IASTParameterDeclaration pDecl) {
			// parameters in function definitions: 
			//e.g., void testParamDecl (XMLDocument doc, const char *filename){
			if (!(pDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;

			// find bindings for the declaration specifier
			IASTName declSpecifierName = ((IASTNamedTypeSpecifier) pDecl.getDeclSpecifier()).getName();
			// get the binding
			IBinding binding = declSpecifierName.resolveBinding();

			// check whether this binding is part of the legacy library
			checkBinding(declSpecifierName, binding, pDecl);
//			boolean inLibrary = checkBinding(binding);
//			if (inLibrary) {
//				appendToLists(declSpecifierName, binding, pDecl);
//			}

			return PROCESS_CONTINUE;
		}

		/**
		 * This is to capture declarations (constructors) like
		 * {@code XMLDocument xmlDoc};
		 */
		@Override
		public int visit(IASTDeclaration decl) {
			if (!(decl instanceof IASTSimpleDeclaration))
				return PROCESS_CONTINUE;
			IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) decl;

			// check if there are any declarators: they should, otherwise there
			// would be a compilation error (e.g., int ;)
			if (!(simpleDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;

			// find bindings for the declaration specifier
			IASTName declSpecifierName = ((IASTNamedTypeSpecifier) simpleDecl.getDeclSpecifier()).getName();
			IBinding binding = declSpecifierName.resolveBinding();

			// check whether this binding is part of the legacy library
			checkBinding(declSpecifierName, binding, simpleDecl);
//			boolean inLibrary = checkBinding(binding);
//			if (inLibrary){
//				appendToLists(declSpecifierName, binding, simpleDecl);
//			}

			return PROCESS_CONTINUE;
		}

		
		private void checkBinding(IASTName name, IBinding binding, IASTNode node) {
			try {
				
				// while not reached a namespace scope
				if ( (binding==null) || (binding instanceof IProblemBinding) 
						|| (binding.getScope()==null) 
						|| (binding instanceof ICPPUnknownBinding) 
						){
					System.out.println("NULL\t" + name +"\t"+ binding.getClass().getSimpleName());
					return;
				}

				IScope scope = binding.getScope();
				while (!((scope != null) && (scope instanceof ICPPNamespaceScope))) {
					scope = scope.getParent();
				}
				// System.out.println(scope.getScopeName() +"\t");

				if ((scope.getScopeName() != null)
						&& (REFACTORING_NAMESPACES.contains(scope.getScopeName().toString())))
					appendToLists(name, binding, node);
//					return true;

			} catch (DOMException e) {
				e.printStackTrace();
			}
//			return false;
		}
		
		
		private void appendToLists(IASTName name, IBinding binding, IASTNode node){
			boolean added = this.namesSet.add(name);
			this.namesList.add(name);
			if (added){
				this.bindingsSet.add(binding);
				this.nodesList.add(node);
			}
		}
	}
	
	
	
	
	
	
	private class UnusedCode{
		IASTTranslationUnit mainAST=null;
		/**
		 * Test function addition
		 * 
		 * @param name
		 */
		@Deprecated
		protected void addNewFunction(String name) {
			try {
				IASTNode node = ASTUtilities.findNodeInTree(mainAST, 1,
						Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", "test");
				if (node != null && node instanceof IASTFunctionDeclarator) {
					// System.out.println(node.isFrozen());

					ASTRewrite rewriter = ASTRewrite.create(mainAST);
					ICPPNodeFactory nodeFactory = (ICPPNodeFactory) mainAST.getASTNodeFactory();

					IASTNode n = nodeFactory.newFunctionDefinition(
							((IASTFunctionDefinition) node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
							nodeFactory.newFunctionDeclarator(nodeFactory.newName(name)),
							((IASTFunctionDefinition) node.getParent()).getBody().copy(CopyStyle.withoutLocations));

					// rewriter.replace(node, n, null);
					rewriter.insertBefore(node.getParent(), node.getParent().getChildren()[0], n, null);

					Change c = rewriter.rewriteAST();
					c.perform(new NullProgressMonitor());
				}
			} catch (ClassNotFoundException | CoreException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Test function renaming
		 * 
		 * @param oldName
		 * @param newName
		 */
		@SuppressWarnings("restriction")
		@Deprecated
		protected void replaceFunction(String oldName, String newName) {
			try {
				IASTNode node = ASTUtilities.findNodeInTree(mainAST, 1,
						Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", oldName);
				if (node != null && node instanceof IASTFunctionDeclarator) {

					ASTRewrite rewriter = ASTRewrite.create(mainAST);
					ICPPNodeFactory nodeFactory = (ICPPNodeFactory) mainAST.getASTNodeFactory();

					// IASTNode n = nodeFactory.newFunctionDefinition(
					// ((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
					// nodeFactory.newFunctionDeclarator(nodeFactory.newName(newName)),
					// ((IASTFunctionDefinition)node.getParent()).getBody().copy(CopyStyle.withoutLocations)
					// );
					// rewriter.replace(node, n, null);
					// Change c = rewriter.rewriteAST();

					if (node instanceof IASTFunctionDeclarator) {
						IASTFunctionDeclarator newNode = (IASTFunctionDeclarator) node.copy(CopyStyle.withLocations);
						newNode.setName(nodeFactory.newName("renamedNode"));
						IASTName name = newNode.getName().copy(CopyStyle.withLocations);
						System.out.println(name);

						IASTFunctionDefinition funDef = (IASTFunctionDefinition) node.getParent();

						IASTFunctionDefinition newFunctionDef = funDef.copy(CopyStyle.withLocations);
						newFunctionDef.getDeclarator().setName(name);
						newFunctionDef.setBody(nodeFactory.newCompoundStatement());
						rewriter.insertBefore(node.getParent().getParent(), null, newFunctionDef, null);
						rewriter.rewriteAST().perform(new NullProgressMonitor());
					}
				}
			} catch (ClassNotFoundException | CoreException e) {
				e.printStackTrace();
			}
		}

		
		
		private void addFunctionDefinitions(ITranslationUnit tu, List<IASTName> list) {
			try {
				IASTTranslationUnit ast = tu.getAST();

				ASTRewrite rewriter = ASTRewrite.create(ast);
				ICPPNodeFactory nodeFactory = (ICPPNodeFactory) ast.getASTNodeFactory();

				for (IASTName name : list) {
					IBinding binding = projectIndex.findBinding(name);

					IIndexName[] defs = projectIndex.findDefinitions(binding);

					if (binding instanceof ICPPClassType) {
						ICPPClassType aClass = (ICPPClassType) binding;
						ICPPConstructor constructors[] = aClass.getConstructors();
						for (ICPPConstructor constructor : constructors) {
							System.out.println(constructor.toString());
						}

						// IASTNode node =
						// nodeFactory.newFunctionDefinition(declSpecifier,
						// declarator, bodyStatement)
						// IASTNode n = nodeFactory.newFunctionDefinition(
						// ((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
						// nodeFactory.newFunctionDeclarator(nodeFactory.newName(name)),
						// ((IASTFunctionDefinition)node.getParent()).getBody().copy(CopyStyle.withoutLocations)
						// );

					}
					System.out.println(binding);
				}

			} catch (CoreException e) {
				e.printStackTrace();
			}

		}

		private void checkBindings(List<IASTName> namesList) {
			// find bindings for all IASTNames in that list
			for (IASTName name : namesList) {
				// get the binding
				IBinding binding = name.resolveBinding();

				try {
					projectIndex.acquireReadLock();

					IIndexName declarations[] = projectIndex.findDeclarations(binding);
					for (IIndexName decl : declarations) {
						System.out.println(decl.getFile().toString());
						ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(decl);

						if (!tu.toString().equals("tinyxml2.cpp") && !tu.toString().equals("tinyxml2.h"))
							continue;

						IASTTranslationUnit ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
						IASTNode node = ast.getNodeSelector(null).findEnclosingNode(decl.getNodeOffset(),
								decl.getNodeLength());
						// IASTNode node = n;
						while (node instanceof IASTName) {
							System.out.println("\t" + node + "\t" + node.getClass().getSimpleName());
							node = node.getParent();
						}

						System.out.print("");

						assert (node instanceof IASTFunctionDeclarator);

						System.out.println(node.getRawSignature());
					}

				} catch (InterruptedException | CoreException e) {
					e.printStackTrace();
				} finally {
					projectIndex.releaseReadLock();
				}
			}
		}

		protected void refactorNameSpace(String nameSpace) {
			try {
				List<IASTName> resultList = null;

				// find all function calls
				FunctionCallFinderVisitor fcVisitor = new FunctionCallFinderVisitor();
				mainAST.accept(fcVisitor);
				resultList = new ArrayList<IASTName>(fcVisitor.getList());

				// find all declarations
				DeclarationFinderVisitor dVisitor = new DeclarationFinderVisitor();
				mainAST.accept(dVisitor);
				resultList.addAll(dVisitor.getList());

				IIndex projectIndex = CCorePlugin.getIndexManager()
						.getIndex(((ITranslationUnit) (mainAST.getOriginatingTranslationUnit())).getCProject());

				for (IASTName name : resultList) {
					if (name.toString().contains("circumference")) {
						System.out.println(name);
					}
					try {
						IBinding binding = name.resolveBinding();
						projectIndex.acquireReadLock();

						if ((binding instanceof IFunction) || (binding instanceof ICPPClassType)) {
							// find scope
							IScope scope = binding.getScope();
							// while not reached a namespace scope
							while (!((scope != null) && (scope instanceof ICPPNamespaceScope))) {
								scope = scope.getParent();
							}
							// if ( (scope.getScopeName() != null) && (
							// (scope.getScopeName().toString().equals("mathEquations"))
							// ||
							// (scope.getScopeName().toString().equals("testTinyXML2")))
							// )
							// System.out.println(name +"\t:Loc:"+
							// name.getFileLocation().getFileName() +"\t Scope\t" +
							// scope);//+ scope.getScopeName());

							// System.out.println();
							// if ( (scope.getScopeName() != null) &&
							// (scope.getScopeName().toString().equals(nameSpace)) )
							// System.out.println(name +"\t:Loc:"+
							// name.getFileLocation().getFileName() +"\t Scope\t" +
							// scope);//+ scope.getScopeName());

							// get references
							// IIndexName[] declarations =
							// projectIndex.findDeclarations(binding);
							// do something with these references
							// for (IIndexName declaration : declarations) {
							// System.out.println(declaration.getFileLocation()
							// +"\t"+ declaration.getf);
							// }
						}
					} catch (InterruptedException | NullPointerException e) {
						// e.printStackTrace();
					} finally {
						projectIndex.releaseReadLock();
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	
	private static class FunctionCallFinderVisitor extends ASTVisitor {
		/** List keeping all function calls **/
		private List<IASTName> functionCallsList;

		// static initialiser: executed when the class is loaded
		{
			shouldVisitNames = false;
			shouldVisitExpressions = true;
			// shouldVisitDeclarations = true;
			functionCallsList = new ArrayList<IASTName>();
		}

		private List<IASTName> getList() {
			return this.functionCallsList;
		}

		@Override
		public int visit(IASTExpression exp) {
			if (!(exp instanceof IASTFunctionCallExpression))
				return PROCESS_CONTINUE;
			IASTFunctionCallExpression funcCallExp = (IASTFunctionCallExpression) exp;

			IASTExpression funcExpression = funcCallExp.getFunctionNameExpression();

			if (funcExpression instanceof IASTFieldReference) {
				IASTFieldReference fieldRef = (IASTFieldReference) funcExpression;
				this.functionCallsList.add(fieldRef.getFieldName());
			} else if (funcExpression instanceof IASTIdExpression) {
				IASTIdExpression idExp = (IASTIdExpression) funcExpression;
				this.functionCallsList.add(idExp.getName());
			}
			return PROCESS_CONTINUE;
		}
	}

	
	private static class DeclarationFinderVisitor extends ASTVisitor {
		/** List keeping all function calls **/
		private List<IASTName> declarationsList;

		// static initialiser: executed when the class is loaded
		{
			shouldVisitDeclarations = true;
			declarationsList = new ArrayList<IASTName>();
		}

		private List<IASTName> getList() {
			return this.declarationsList;
		}

		/**
		 * This is to capture declarations like {@code int x};
		 */
		@Override
		public int visit(IASTDeclaration decl) {
			if (!(decl instanceof IASTSimpleDeclaration))
				return PROCESS_CONTINUE;
			IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration) decl;

			// check if there are any declarators: they should, otherwise there
			// would be a compilation error (e.g., int ;)
			if (!(simpleDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;

			this.declarationsList.add(((IASTNamedTypeSpecifier) simpleDecl.getDeclSpecifier()).getName());
			return PROCESS_CONTINUE;
		}
	}

	
	/*
	 * @SuppressWarnings("restriction") class MinimalRefactoring extends
	 * CRefactoring{
	 * 
	 * public MinimalRefactoring(ICElement element, ICProject project) { //
	 * Auto-generated constructor stub super (element, null, project); }
	 * 
	 * @Override protected RefactoringDescriptor getRefactoringDescriptor() { //
	 * }
	 * 
	 * @Override protected void collectModifications(IProgressMonitor pm,
	 * ModificationCollector collector) throws CoreException,
	 * OperationCanceledException { //
	 * ASTRewrite rewriter = collector.rewriterForTranslationUnit(mainAST);
	 * Change c = rewriter.rewriteAST(); c.perform(pm); }
	 * 
	 * }
	 */
	
	
	private static class NamesSet extends ListSet<IASTName> {
		@Override
		public boolean contains(Object o) {
			for (IASTName e : storage) {
				if (e == o || e.getLastName().toString().equals(((IASTName) o).getLastName().toString())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean add(IASTName e) {
			if (contains(e)) {
				return false;
			} else {
				return storage.add(e.getLastName());
			}
		}
	}
	
	private static class BindingsSet extends ListSet<IBinding> {
		@Override
		public boolean contains(Object o) {
			for (IBinding e : storage) {
				if (e == o || e.getName().equals(((IBinding) o).getName().toString())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean add(IBinding e) {
			if (contains(e)) {
				return false;
			} else {
				return storage.add(e);
			}
		}
	}
}
