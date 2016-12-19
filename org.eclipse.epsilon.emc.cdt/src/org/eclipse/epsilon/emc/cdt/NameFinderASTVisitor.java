package org.eclipse.epsilon.emc.cdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPUnknownBinding;//for C++ templates

	

public class NameFinderASTVisitor extends ASTVisitor {
	/** List keeping all important IASTNames**/
//	private NamesSet namesSet;
	
	/** Set keeping all important IASTNames, but doesn't accept duplicates **/
	private List<IASTName> namesList;

	/** List keeping all important IBindings, but doesn't accept duplicates **/
//	private BindingsSet bindingsSet;
	
	/**Set of files belonging to the library to be refactored */
	private static String elements[] = {"tinyxml2"};
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Set REFACTORING_NAMESPACES = new HashSet(Arrays.asList(elements)); 
	
//	private List<IASTNode> nodesList;


	// static initialiser: executed when the class is loaded
	{
		shouldVisitNames 					= true;
		shouldVisitExpressions				= true;
		shouldVisitParameterDeclarations	= true;
		shouldVisitDeclarations 			= true;
		//
//		namesSet 		= new NamesSet();
//		bindingsSet  	= new BindingsSet();
		namesList		= new ArrayList<IASTName>();
//		nodesList		= new ArrayList<IASTNode>();
				
	}

	public List<IASTName> getList(){
		return this.namesList;
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
		return PROCESS_CONTINUE;
	}

	
	private void checkBinding(IASTName name, IBinding binding, IASTNode node) {
		try {
			
			// while not reached a namespace scope
			if ( (binding==null) || (binding.getScope()==null) 
					|| (binding instanceof ICPPUnknownBinding) ){
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
//				return true;

		} catch (DOMException | NullPointerException e) {
			e.printStackTrace();
		}
//		return false;
	}
	
	
	private void appendToLists(IASTName name, IBinding binding, IASTNode node){
//		boolean added = this.namesSet.add(name);
		this.namesList.add(name);
//		if (added){
//			this.bindingsSet.add(binding);
//			this.nodesList.add(node);
//		}
	}
}