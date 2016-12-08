package org.eclipse.epsilon.emc.cdt;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.codan.core.cxx.CxxAstUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUsingDirective;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.epsilon.common.util.ListSet;
import org.eclipse.ltk.core.refactoring.Change;

public class RefactoringAST {

	/** project */
	protected ICProject cproject = null;

	/** project index */
	protected IIndex projectIndex = null;
	
	/** Pairs of ITranslationUnit, IASTTranslationUnit **/
	HashMap<ITranslationUnit, IASTTranslationUnit> astCache = new HashMap<>();


	private static String elements[] = { "tinyxml2" };
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Set REFACTORING_NAMESPACES = new HashSet(Arrays.asList(elements));

	private static final String myLIBRARY = "myTinyXmlLib.cpp";
	private static final String myDIR = "src/myLibDir";

	/** Keep refactoring information*/
	NamesSet 	namesSet 	 = new NamesSet();
	BindingsSet bindingsSet  = new BindingsSet();
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
					libraryCache.put(tu, fcVisitor.getNamesList());
					namesSet.addAll(fcVisitor.getNamesSet());
					bindingsSet.addAll(fcVisitor.getBindingsSet());
					nodesList.addAll(fcVisitor.nodesList);
				}
			}
			
			assert(namesSet.size() == bindingsSet.size());
			assert(namesSet.size() == nodesList.size());
			
			for (int i=0; i<namesSet.size(); i++){
				System.out.println(namesSet.getList().get(i) +"\t"+ bindingsSet.getList().get(i).getClass().getSimpleName());// +"\t"+ nodesList.get(i));
			}

			//check for library uses within the same library
			checkReferences();

			assert(namesSet.size() == bindingsSet.size());
			assert(namesSet.size() == nodesList.size());

			//create refactored code
			createRefactoredCode(namesSet, bindingsSet, nodesList);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

 	
 	private void checkReferences () {
 		try {
 			BindingsSet bindings = bindingsSet;
	 		for (IBinding binding : bindings){
 				projectIndex.acquireReadLock();
	 			if (binding instanceof ICPPClassType){						
	 				ICPPClassType aClass = (ICPPClassType)binding;
//	 				checkClassRefs(aClass);
	 			}
	 			else if (binding instanceof IEnumeration){
	 				System.out.println(binding + "\t IEnumeration");
	 			}
	 			else if (binding instanceof ICPPMethod){
	 				System.out.println(binding + "Method");
	 			}
	 		}	
 		} 
 		catch (InterruptedException e) {		
 			e.printStackTrace();		
 		}
 		finally{
 			projectIndex.releaseReadLock();
 		}
 	}
 	
 	
 	private void checkClassRefs(ICPPClassType binding) throws CoreException{
		IIndexName definitions[] = projectIndex.findDefinitions(binding);
		for (IIndexName def : definitions) {
			System.out.println(def.getFile());
			// for now, just use the first overload found
			ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(def);
			if (tu == null) {
				continue;
			}
			
			IASTTranslationUnit ast = null;
			if (astCache.containsKey(tu)) {
				ast = astCache.get(tu);
			} else {
				ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
				astCache.put(tu, ast);
			}
			
			IASTName name = (IASTName) ast.getNodeSelector(null).findEnclosingNode(def.getNodeOffset(), def.getNodeLength());
			IASTNode fdecl = name;
			while ( (fdecl!=null) && (!(fdecl instanceof ICPPASTCompositeTypeSpecifier)) ) {
				fdecl = fdecl.getParent();
			}
			if (fdecl != null){
//				System.out.println(fdecl.getRawSignature());
				final IASTNode node = fdecl; 
				node.accept(new ASTVisitor() {
					{shouldVisitBaseSpecifiers = true;
					 shouldVisitDecltypeSpecifiers = true;}
					@Override
					public int visit(ICPPASTBaseSpecifier baseSpecifier) {
						System.out.println("Bingo");
						ICPPASTNameSpecifier n = baseSpecifier.getNameSpecifier();
						IBinding b = n.resolveBinding(); 
						if (b instanceof ICPPClassType && !bindingsSet.contains(b))
							try {
								if (n instanceof IASTName){
									if (namesSet.add((IASTName) n)){
										bindingsSet.add(b);
										nodesList.add(node);	
									}
									checkClassRefs((ICPPClassType)b);
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
 	
 	
	
	
	// TODO: create Delegate pattern to handle changes?
	private void createRefactoredCode (ListSet<IASTName> namesSet, ListSet<IBinding> bindingsSet, List<IASTNode> nodesList) {
		try {
			//
			IFile file = CdtUtilities.createNewFile(cproject, myDIR, myLIBRARY);
			if (file == null)
				throw new NoSuchFileException("Could not create source file " + myDIR + "/" + myLIBRARY);

			// Create translation unit for file
			ITranslationUnit libTU = CoreModelUtil.findTranslationUnit(file);
			// get ast
			IASTTranslationUnit libAST = libTU.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
			// get rewriter
			ASTRewrite rewriter = ASTRewrite.create(libAST);
			// get node factory
			ICPPNodeFactory nodeFactory = (ICPPNodeFactory) libAST.getASTNodeFactory();

			// add include directive
			IASTName includeDir = nodeFactory.newName("#include \"tinyXML2.h\"");
			rewriter.insertBefore(libAST, null, includeDir, null);
			
			// add using directive
			ICPPASTUsingDirective usingDirective = nodeFactory.newUsingDirective(nodeFactory.newName("tinyxml2"));
			rewriter.insertBefore(libAST, null, usingDirective, null);

			// add namespace definition
			ICPPASTNamespaceDefinition nsDef = nodeFactory.newNamespaceDefinition(nodeFactory.newName("mytinyxml2"));

			
			// add function definitions
			for (IASTName name : namesSet) {
				IBinding binding = projectIndex.findBinding(name);
				if (binding instanceof ICPPClassType) {					
//					System.out.println("Class:\t" + binding);
					
					//create new class instance
					IASTCompositeTypeSpecifier newClass = nodeFactory.newCompositeTypeSpecifier(ICPPASTCompositeTypeSpecifier.k_class, nodeFactory.
																			newName(name.toString()) );
					newClass.addDeclaration(nodeFactory.newVisibilityLabel(ICPPASTVisibilityLabel.v_public));
					
										
					ICPPClassType aClass = (ICPPClassType)binding;
					for (ICPPConstructor constructor : aClass.getConstructors()){
						
						IIndexName[] decls = projectIndex.findDeclarations(constructor);
						if (decls.length > 0){
							IIndexName decl    = decls[0];
							ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(decl);
							
							IASTTranslationUnit ast = null;
							if (astCache.containsKey(tu)){
								ast = astCache.get(tu);
							}
							else{
								ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
								astCache.put(tu, ast);
							}
							IASTName n = (IASTName) ast.getNodeSelector(null).findEnclosingNode(decl.getNodeOffset(), decl.getNodeLength());
							IASTNode fdecl = n;
							while (!(fdecl instanceof IASTSimpleDeclaration)){
								fdecl =  fdecl.getParent();
							}
							assert (fdecl instanceof IASTSimpleDeclaration);						
							IASTSimpleDeclaration newDeclaration = ((IASTSimpleDeclaration)fdecl).copy(CopyStyle.withLocations);
							newClass.addMemberDeclaration(newDeclaration);
						}
					}
					
					for (IASTName name2 : namesSet){
						if (!name2.equals(name)){
							IBinding b = projectIndex.findBinding(name2);
							if (b instanceof ICPPMethod && ((ICPPMethod)b).getClassOwner().equals(aClass)) {
								IASTDeclaration newDeclaration =  CxxAstUtils.createDeclaration(name2, nodeFactory, projectIndex);
//								ICPPMethod aMethod = (ICPPMethod)b;
//								IIndexName[] decls = projectIndex.findDeclarations(aMethod);
//								if (decls.length > 0){
//									IIndexName decl    = decls[0];
//									ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(decl);
//									IASTTranslationUnit ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
//									IASTName n = (IASTName) ast.getNodeSelector(null).findEnclosingNode(decl.getNodeOffset(), decl.getNodeLength());
//									IASTNode fdecl = n;
//									while (!(fdecl instanceof IASTSimpleDeclaration)){
//										fdecl =  fdecl.getParent();
//									}
//									assert (fdecl instanceof IASTSimpleDeclaration);						
//									IASTSimpleDeclaration newDeclaration = ((IASTSimpleDeclaration)fdecl).copy(CopyStyle.withLocations);
									newClass.addMemberDeclaration(newDeclaration);
//								}
							}
						}
					}
					

					IASTSimpleDeclaration newDeclaration = nodeFactory.newSimpleDeclaration(newClass);
					nsDef.addDeclaration(newDeclaration);
				} 

			}

			
			rewriter.insertBefore(libAST, null, nsDef, null);
			rewriter.rewriteAST().perform(new NullProgressMonitor()); 
		} 
		catch (NoSuchFileException | CoreException e) {
			e.printStackTrace();
		}
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
			shouldVisitNames = true;
			shouldVisitExpressions = true;
			shouldVisitParameterDeclarations = true;
			shouldVisitDeclarations = true;
			//
			namesSet 		= new NamesSet();
			bindingsSet  	= new BindingsSet();
			namesList		= new ArrayList<IASTName>();
			nodesList		= new ArrayList<IASTNode>();
					
		}

		private NamesSet getNamesSet() {
			return this.namesSet;
		}
		
		private BindingsSet getBindingsSet(){
			return this.bindingsSet;
		}
		
		private List<IASTName> getNamesList() {
			return this.namesList;
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
			// Function that *do not* belong to a class: e.g., printf("%s",
			// filename);
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
				boolean inLibrary = checkBinding(binding);
				if (inLibrary){
					appendToLists(name, binding, node);
				}
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
			boolean inLibrary = checkBinding(binding);
			if (inLibrary) {
				appendToLists(declSpecifierName, binding, pDecl);
			}

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
			boolean inLibrary = checkBinding(binding);
			if (inLibrary){
				appendToLists(declSpecifierName, binding, simpleDecl);
			}

			return PROCESS_CONTINUE;
		}

		
		private boolean checkBinding(IBinding binding) {
			try {
				
				// while not reached a namespace scope
				IScope scope = binding.getScope();
				while (!((scope != null) && (scope instanceof ICPPNamespaceScope))) {
					scope = scope.getParent();
				}
				// System.out.println(scope.getScopeName() +"\t");

				if ((scope.getScopeName() != null)
						&& (REFACTORING_NAMESPACES.contains(scope.getScopeName().toString())))
					return true;

			} catch (DOMException e) {
				e.printStackTrace();
			}
			return false;
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
	 * public MinimalRefactoring(ICElement element, ICProject project) { // TODO
	 * Auto-generated constructor stub super (element, null, project); }
	 * 
	 * @Override protected RefactoringDescriptor getRefactoringDescriptor() { //
	 * TODO Auto-generated method stub return null; }
	 * 
	 * @Override protected void collectModifications(IProgressMonitor pm,
	 * ModificationCollector collector) throws CoreException,
	 * OperationCanceledException { // TODO Auto-generated method stub
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
