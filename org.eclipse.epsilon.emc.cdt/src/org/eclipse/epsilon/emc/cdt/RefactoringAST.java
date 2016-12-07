package org.eclipse.epsilon.emc.cdt;

import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
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
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IUsing;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;



public class RefactoringAST {
	
	/** project */
	protected ICProject cproject = null;
	
	/** project index*/
	protected IIndex projectIndex = null;

	/** Main AST */
	private IASTTranslationUnit mainAST = null;

	/** flag indicating whether a change has been made*/
	private boolean changedAST = false;

	/** List of changes in AST*/
	@Deprecated
	private List<Change> changesList = null;
	
	
	private static String elements[] = {"tinyxml2"};
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Set REFACTORING_NAMESPACES = new HashSet(Arrays.asList(elements)); 
	
	private static final String myLIBRARY 	= "myTinyXmlLib.cpp";
	private static final String myDIR		= "src/myLibDir";
	
	
	/** Class constructor */
	public RefactoringAST (ICProject project, IASTTranslationUnit ast) {
		try {
			this.cproject		= project;
			this.projectIndex 	= CCorePlugin.getIndexManager().getIndex(cproject);
			this.mainAST 		= ast;
			this.changedAST		= false;
			this.changesList	= new ArrayList<Change>();	
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}
	}

	
	/** 
	 * Store the modified AST if any changes have been made 
	*/
	@Deprecated
	protected boolean storeAST(){
		try {
			if (changedAST){
				for (Change c : changesList)
					c.perform(new NullProgressMonitor());
				
				//reset changes flag
				changesList.clear();
				changedAST = false;
				
				return true;
			}
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}		
		return false;
	}
		
	
	/**
	 * Test function addition
	 * @param name
	 */
	@Deprecated
	protected void addNewFunction(String name){
		try {
			IASTNode node = ASTUtilities.findNodeInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", "test");
			if (node != null && node instanceof IASTFunctionDeclarator){
//				System.out.println(node.isFrozen());
				
				ASTRewrite rewriter 		= ASTRewrite.create(mainAST);
				ICPPNodeFactory nodeFactory = (ICPPNodeFactory)mainAST.getASTNodeFactory();
				
				IASTNode n 	= nodeFactory.newFunctionDefinition(
								((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
								nodeFactory.newFunctionDeclarator(nodeFactory.newName(name)),
								((IASTFunctionDefinition)node.getParent()).getBody().copy(CopyStyle.withoutLocations)
						  );
				 
//				rewriter.replace(node, n, null);
				rewriter.insertBefore(node.getParent(), node.getParent().getChildren()[0], n, null);

				Change c = rewriter.rewriteAST();
				
				changesList.add(c);
				changedAST = true;
//				c.perform(new NullProgressMonitor());				
			}			
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	
	/**
	 * Test function renaming
	 * @param oldName
	 * @param newName
	 */
	@Deprecated
	protected void replaceFunction(String oldName, String newName){
		try {
			IASTNode node = ASTUtilities.findNodeInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", oldName);
			if (node != null && node instanceof IASTFunctionDeclarator){
				
				ASTRewrite rewriter 		= ASTRewrite.create(mainAST);
				ICPPNodeFactory nodeFactory = (ICPPNodeFactory)mainAST.getASTNodeFactory();
				
				IASTNode n 	= nodeFactory.newFunctionDefinition(
								((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
								nodeFactory.newFunctionDeclarator(nodeFactory.newName(newName)),
								((IASTFunctionDefinition)node.getParent()).getBody().copy(CopyStyle.withoutLocations)
						  );
				 
				rewriter.replace(node, n, null);

				Change c = rewriter.rewriteAST();
				
				changesList.add(c);
				changedAST = true;
			}			
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	

	
	protected void refactor (final String[] excludedFiles){
		try {
			/** Pairs of ITranslationUnit, IASTTranslationUnit **/
			HashMap<ITranslationUnit, IASTTranslationUnit> astCache = new HashMap<>();
			
			/** Pairs of ITranslationUnit, List<IASTName>, where List<IASTName> keeps the IASTNames used from the legacy library **/
			HashMap<ITranslationUnit, List<IASTName>> libraryCache = new HashMap<>();

			
			//find all translation units
			List<ITranslationUnit> tuList = CdtUtilities.getProjectTranslationUnits(cproject, excludedFiles);
						
			
			//for each translation unit get its AST
			for (ITranslationUnit tu : tuList){
//				System.out.println(tu.getFile().getName() +"\t"+ tu.getFile().getLocation());

				//keeps the IASTNames used from the legacy library 
				List<IASTName> resultList = new ArrayList<IASTName>();

//				/** Pairs of  IASTName,IBinding  **/
//				HashMap<IASTName, IBinding> bindingCache = new HashMap<>();
				
				//get AST for that translation unit
				IASTTranslationUnit ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
				
				//cache the tu & ast pair
				astCache.put(tu, ast);
				
				NameFinderASTVisitor fcVisitor = new NameFinderASTVisitor();
				ast.accept(fcVisitor);
				
				//cache tu & result list
				resultList.addAll(fcVisitor.getList());
				//if the list is not empty, then it uses the legacy library --> add it to cached library
				if (!resultList.isEmpty())
					libraryCache.put(tu, resultList);
				
//				System.out.println(tu +"\t"+ Arrays.toString(resultList.toArray()));
			}
									
			System.out.println(astCache.size());
			System.out.println(libraryCache.size());
			
//			createRefactoredCode(libraryCache);

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//TODO: create Delegate pattern to handle changes?
	private void createRefactoredCode(HashMap<ITranslationUnit, List<IASTName>> libraryCache){
		try {
			ITranslationUnit libTU = null;
			
			IFile file = CdtUtilities.createNewFile(cproject, myDIR, myLIBRARY);
			if (file == null)
				throw new NoSuchFileException("Could not create source file " + myDIR +"/"+ myLIBRARY);

			// Create translation unit for file
			libTU = CoreModelUtil.findTranslationUnit(file);			
			
			addNameSpace(libTU, myLIBRARY);
			for (ITranslationUnit tu : libraryCache.keySet()){
				addFunctionDefinitions (libTU, libraryCache.get(tu));
			}
			
			System.out.println(libTU.getElementName());
			System.out.println();
		}
		catch (NoSuchFileException e){
			e.printStackTrace();
		}
	}
	
	
	
	private void addNameSpace(ITranslationUnit tu, String nameSpaceName){
		try {
			IASTTranslationUnit ast = tu.getAST();
			
			ASTRewrite rewriter 		= ASTRewrite.create(ast);
			ICPPNodeFactory nodeFactory = (ICPPNodeFactory)ast.getASTNodeFactory();

			//add header
//			IAST headerNode = ast.getASTNodeFactory().new
//			nodeFactory.ne
//			tu.get
//			projectIndex
			
			IASTNode newNode = nodeFactory.newUsingDirective(nodeFactory.newName("tinyxml2"));
			rewriter.insertBefore(ast.getTranslationUnit(), null, newNode, null);
			rewriter.rewriteAST();				
//			c.perform(new NullProgressMonitor());				

			IASTNode newNode2 = nodeFactory.newUsingDirective(nodeFactory.newName("std"));
			rewriter.insertBefore(ast, null, newNode2, null);
			rewriter.rewriteAST();				
//			c2.perform(new NullProgressMonitor());			
			
			IASTNode newNode4 = nodeFactory.newNamespaceDefinition(nodeFactory.newName("myTinyXmlLib"));
			rewriter.insertBefore(ast, null, newNode4, null);
			Change c4 = rewriter.rewriteAST();				
			c4.perform(new NullProgressMonitor());
		}
		catch (CoreException e){
			e.printStackTrace();
		}
				
	}
	
	
	
	private void addFunctionDefinitions (ITranslationUnit tu, List<IASTName> list){
		try {
			IASTTranslationUnit ast = tu.getAST();
			
			ASTRewrite rewriter 		= ASTRewrite.create(ast);
			ICPPNodeFactory nodeFactory = (ICPPNodeFactory)ast.getASTNodeFactory();

			for (IASTName name : list){
				IBinding binding = projectIndex.findBinding(name);
				
				IIndexName []defs = projectIndex.findDefinitions(binding);
				
				if (binding instanceof ICPPClassType){
					ICPPClassType aClass = (ICPPClassType)binding;
					ICPPConstructor constructors[]  = aClass.getConstructors();
					for (ICPPConstructor constructor : constructors){
						System.out.println(constructor.toString());
					}
					
//					IASTNode node = nodeFactory.newFunctionDefinition(declSpecifier, declarator, bodyStatement)
//							IASTNode n 	= nodeFactory.newFunctionDefinition(
//									((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().copy(CopyStyle.withoutLocations),
//									nodeFactory.newFunctionDeclarator(nodeFactory.newName(name)),
//									((IASTFunctionDefinition)node.getParent()).getBody().copy(CopyStyle.withoutLocations)
//							  );

				}
				System.out.println(binding);
			}
			
		} 
		catch (CoreException e) {
			e.printStackTrace();
		}

	}
	
	
	
 	private void checkBindings(List<IASTName> namesList) {
		//find bindings for all IASTNames in that list
		for (IASTName name : namesList){
			//get the binding
			IBinding binding = name.resolveBinding();
			
			try {
				projectIndex.acquireReadLock();
				
				IIndexName declarations[] =  projectIndex.findDeclarations(binding);
				for (IIndexName decl : declarations){
					System.out.println(decl.getFile().toString());
					ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(decl);
					
					if (!tu.toString().equals("tinyxml2.cpp") && !tu.toString().equals("tinyxml2.h"))
						continue;
					
					IASTTranslationUnit ast =  tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
					IASTNode node = ast.getNodeSelector(null).findEnclosingNode(decl.getNodeOffset(), decl.getNodeLength());
//					IASTNode node = n;
					while (node instanceof IASTName) {
						System.out.println("\t"+ node +"\t"+ node.getClass().getSimpleName());
						node = node.getParent();
					}

					System.out.print("");

					assert(node instanceof IASTFunctionDeclarator);

					System.out.println(node.getRawSignature());
				}
				
			} 
			catch (InterruptedException | CoreException e) {
				e.printStackTrace();
			} 
			finally {
				projectIndex.releaseReadLock();
			}
		}
	}
	
	
	protected void refactorNameSpace(String nameSpace){
		try {
			List<IASTName> resultList = null;
			
			//find all function calls
			FunctionCallFinderVisitor fcVisitor = new FunctionCallFinderVisitor();
			mainAST.accept(fcVisitor);
			resultList = new ArrayList<IASTName> (fcVisitor.getList());
			
			//find all declarations
			DeclarationFinderVisitor dVisitor  = new DeclarationFinderVisitor();
			mainAST.accept(dVisitor);
			resultList.addAll(dVisitor.getList());
						
		
			IIndex projectIndex = CCorePlugin.getIndexManager().getIndex(((ITranslationUnit)(mainAST.getOriginatingTranslationUnit())).getCProject());
			
			for (IASTName name : resultList){
				if (name.toString().contains("circumference")){
					System.out.println(name);
				}
				try {
					IBinding binding = name.resolveBinding();
					projectIndex.acquireReadLock();
					
					if ( (binding instanceof IFunction) || (binding instanceof ICPPClassType) ){
						//find scope
						IScope scope = binding.getScope();
						//while not reached a namespace scope
						while (!( (scope!=null) && (scope instanceof ICPPNamespaceScope) )){
							scope = scope.getParent();
						}
//						if ( (scope.getScopeName() != null) && ( (scope.getScopeName().toString().equals("mathEquations")) || (scope.getScopeName().toString().equals("testTinyXML2"))) )
//							System.out.println(name +"\t:Loc:"+ name.getFileLocation().getFileName() +"\t Scope\t" + scope);//+ scope.getScopeName());
						
//						System.out.println();
//						if ( (scope.getScopeName() != null) &&  (scope.getScopeName().toString().equals(nameSpace)) )
//							System.out.println(name +"\t:Loc:"+ name.getFileLocation().getFileName() +"\t Scope\t" + scope);//+ scope.getScopeName());
						
						// get references
//						IIndexName[] declarations = projectIndex.findDeclarations(binding);
						// do something with these references
//						for (IIndexName declaration : declarations) {
//							System.out.println(declaration.getFileLocation() +"\t"+ declaration.getf);
//						}
					}
				}
				catch (InterruptedException | NullPointerException e) {
//					e.printStackTrace();
				} finally {
					projectIndex.releaseReadLock();
				}
				
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	private static class FunctionCallFinderVisitor extends ASTVisitor{
		/**List keeping all function calls **/
		private List<IASTName> functionCallsList;
		
		//static initialiser: executed when the class is loaded
		{
			shouldVisitNames 		= false;
			shouldVisitExpressions 	= true;
//			shouldVisitDeclarations	= true;
			functionCallsList		= new ArrayList<IASTName>();
		}
		
		
		private List<IASTName> getList(){
			return this.functionCallsList;
		}

		
		
		@Override
		public int visit (IASTExpression exp){
			if (! (exp instanceof IASTFunctionCallExpression))
				return PROCESS_CONTINUE;
			IASTFunctionCallExpression funcCallExp 	= (IASTFunctionCallExpression)exp;
			
			IASTExpression funcExpression = funcCallExp.getFunctionNameExpression();
	
			if (funcExpression instanceof IASTFieldReference){
				IASTFieldReference fieldRef = (IASTFieldReference)funcExpression;
				this.functionCallsList.add(fieldRef.getFieldName());
			}
			else if (funcExpression instanceof IASTIdExpression){
				IASTIdExpression idExp = (IASTIdExpression) funcExpression;
				this.functionCallsList.add(idExp.getName());
			}
			return PROCESS_CONTINUE;
		}
	}
	
	
	
	private static class DeclarationFinderVisitor extends ASTVisitor{
		/**List keeping all function calls **/
		private List<IASTName> declarationsList;
		
		//static initialiser: executed when the class is loaded
		{
			shouldVisitDeclarations	= true;
			declarationsList		= new ArrayList<IASTName>();
		}
		
		
		private List<IASTName> getList(){
			return this.declarationsList;
		}
		

		
		/**
		 * This is to capture declarations like {@code int x};
		 */
		@Override
		public int visit (IASTDeclaration decl){
			if (! (decl instanceof IASTSimpleDeclaration))
				return PROCESS_CONTINUE;
			IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration)decl;
			
			//check if there are any declarators: they should, otherwise there would be a compilation error (e.g., int ;)
			if (! (simpleDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;
			
			this.declarationsList.add(((IASTNamedTypeSpecifier)simpleDecl.getDeclSpecifier()).getName());
			return PROCESS_CONTINUE;			
		}
	}
	
	
	private class NameFinderASTVisitor extends ASTVisitor{
		/**List keeping all function calls **/
		private List<IASTName> namesList;
		
		//static initialiser: executed when the class is loaded
		{
			shouldVisitNames					= true;
			shouldVisitExpressions 				= true;
			shouldVisitParameterDeclarations	= true;
			shouldVisitDeclarations				= true;
//			
			namesList		= new ArrayList<IASTName>();
		}
				
		private List<IASTName> getList(){
			return this.namesList;
		}
		

		/**
		 * This is to capture (1) functions that belong to a class: e.g., {@code xmlDoc.LoadFile(filename)}
		 * (2) function that <b>do not</b> belong to a class: e.g., {@code printf("%s", filename)}
		 */
		@Override
		public int visit (IASTExpression exp){
			if (! (exp instanceof IASTFunctionCallExpression))
				return PROCESS_CONTINUE;
			IASTFunctionCallExpression funcCallExp 	= (IASTFunctionCallExpression)exp;
			
			IASTExpression funcExpression = funcCallExp.getFunctionNameExpression();
	
			IASTName name 		= null;
			//Functions that belong to a class: e.g., xmlDoc.LoadFile(filename);
			if (funcExpression instanceof IASTFieldReference){
				//get the field reference for this: e.g., xmlDoc.LoadFile
				IASTFieldReference fieldRef = (IASTFieldReference)funcExpression;
				//get the name
				name = fieldRef.getFieldName();
			}
			//Function that *do not* belong to a class: e.g., printf("%s", filename);
			else if (funcExpression instanceof IASTIdExpression){
				//get the function name: e.g., printf
				IASTIdExpression idExp = (IASTIdExpression) funcExpression;
				//get the name
				name = idExp.getName();
			}
			
			if (name != null){
				//get the binding
				IBinding binding = name.resolveBinding();
				//check whether this binding is part of the legacy library
				boolean inLibrary = checkBinding(binding);
				if (inLibrary)
					this.namesList.add(name);
			}
			
			return PROCESS_CONTINUE;
		}		
		
		
		/**
		 * This is to capture parameters in function definitions: 
		 * e.g., {@code void testParamDecl (XMLDocument doc, const char *filename)}
		 */
		@Override
		public int visit (IASTParameterDeclaration pDecl){
			//parameters in function definitions: e.g., void testParamDecl (XMLDocument doc, const char *filename){
			if (! (pDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;
			
			//find bindings for the declaration specifier
			IASTName declSpecifierName  = ((IASTNamedTypeSpecifier)pDecl.getDeclSpecifier()).getName();
			//get the binding
			IBinding binding = declSpecifierName.resolveBinding();
			
			//check whether this binding is part of the legacy library
			boolean inLibrary = checkBinding(binding); 
			if (inLibrary){
				this.namesList.add(declSpecifierName);
			}

			return PROCESS_CONTINUE;
		}
		
		
		/**
		 * This is to capture declarations (constructors) like {@code XMLDocument xmlDoc};
		 */
		@Override
		public int visit (IASTDeclaration decl){
			if (! (decl instanceof IASTSimpleDeclaration))
				return PROCESS_CONTINUE;
			IASTSimpleDeclaration simpleDecl = (IASTSimpleDeclaration)decl;
			
			//check if there are any declarators: they should, otherwise there would be a compilation error (e.g., int ;)
			if (! (simpleDecl.getDeclSpecifier() instanceof IASTNamedTypeSpecifier))
				return PROCESS_CONTINUE;
			
			//find bindings for the declaration specifier
			IASTName declSpecifierName  = ((IASTNamedTypeSpecifier)simpleDecl.getDeclSpecifier()).getName();
			IBinding binding = declSpecifierName.resolveBinding();
			
			//check whether this binding is part of the legacy library
			boolean inLibrary = checkBinding(binding); 
			if (inLibrary)
				this.namesList.add(declSpecifierName);
			
//			for (IASTDeclarator declarator : simpleDecl.getDeclarators()){
//				this.namesList.add(declarator.getName());				
//			}
			return PROCESS_CONTINUE;			
		}
		
		
		private boolean checkBinding(IBinding binding){
			
////			if (binding instanceof ICPPNamespaceScope)
////				System.out.print("Namespace:\t");
////			if (binding instanceof IScope)
////				System.out.print("Scope:\t");
////			if (binding instanceof ICPPUsingDeclaration)
////				System.out.print("Using:\t");
////			if (binding instanceof ICPPUsingDirective)
////				System.out.print("Using:\t");
////			if (binding instanceof ICPPClassType)
////				System.out.print("Class:\t");
////			if (binding instanceof ICPPConstructor)
////				System.out.print("Constructor:\t");
////			if (binding instanceof ICPPMethod)
////				System.out.print("Method:\t");
////			else if (binding instanceof IFunction)
////				System.out.print("Function:\t");
////			else if (binding instanceof IParameter)
////				System.out.print("Parameter:\t");
//			
//			System.out.print(binding.getName() +"\t");
			
			try {
				//while not reached a namespace scope
				IScope scope = binding.getScope();
				while (!( (scope!=null) && (scope instanceof ICPPNamespaceScope) )){
					scope = scope.getParent();
				}
//				System.out.println(scope.getScopeName() +"\t");

				if ( (scope.getScopeName()!=null)
						&& (REFACTORING_NAMESPACES.contains(scope.getScopeName().toString())) )
					return true;
				
//				IIndexName []defs = projectIndex.findDefinitions(binding);
//				for (IIndexName d : defs){
//					System.out.println(d.getFile());
//				}
				
//				System.out.println();
			} catch (DOMException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	
	/*
	@SuppressWarnings("restriction")
	class MinimalRefactoring extends CRefactoring{
	
		public MinimalRefactoring(ICElement element, ICProject project) {
			// TODO Auto-generated constructor stub
			super (element, null, project);
		}
	
		@Override
		protected RefactoringDescriptor getRefactoringDescriptor() {
			// TODO Auto-generated method stub
			return null;
		}
	
		@Override
		protected void collectModifications(IProgressMonitor pm, ModificationCollector collector)
				throws CoreException, OperationCanceledException {
			// TODO Auto-generated method stub
			ASTRewrite rewriter = collector.rewriterForTranslationUnit(mainAST);
			Change c = rewriter.rewriteAST();
			c.perform(pm);
		}
		
	}
	*/
}
