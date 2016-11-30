package org.eclipse.epsilon.emc.cdt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.codan.core.cxx.Activator;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.INodeFactory;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.index.IndexFilter;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class ASTUtilities {

	private ASTUtilities() {}

	
	/**
	 * Find the first node that is instance of <b>targetClass</b> and has the
	 * name <b>targetName</b>
	 * 
	 * @param node
	 * @param index
	 * @param targetClass
	 * @param targetName
	 * @return
	 */
	protected static IASTNode findNodeInTree(IASTNode node, int index, Class<?> targetClass, String targetMethod, String targetName) {
		try {
			if (targetClass == null || targetName == null)
				throw new IllegalArgumentException("Target class or target name is NULL!");

			// get node's children
			IASTNode[] children = node.getChildren();

			// navigate recursively
			for (IASTNode childNode : children) {
				IASTNode tempNode = findNodeInTree(childNode, index + 1, targetClass, targetMethod, targetName);
				if (tempNode != null)
					return tempNode;
			}

			// if (children.length==0)
			// System.out.println("Node:\t" + currentNode.getRawSignature());

			// find node of class targetClass with name targetName
			if (targetClass.isInstance(node)) {
				Method method = targetClass.getMethod(targetMethod, (Class<?>[]) null);
				if (targetName.equals(method.invoke(node).toString())) {
					System.out.println("FOUND: " + targetName);
					return node;
				}
			}
		} // try
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			 e.printStackTrace();
			return null;
		}
		return null;
	}

	
	/**
	 * Find all nodes that are instances of <b>targetClass</b>
	 * 
	 * @param node
	 * @param index
	 * @param targetClass
	 * @param targetName
	 * @return
	 */
	protected static void findAllNodesInTree(IASTNode node, int index, Class<?> targetClass, List<? super IASTNode> nodes) {
		try {
			if (targetClass == null)
				throw new IllegalArgumentException("Target class is NULL!");

			// get node's children
			IASTNode[] children = node.getChildren();

			// navigate recursively
			for (IASTNode childNode : children) {
				findAllNodesInTree(childNode, index + 1, targetClass, nodes);
			}

			// find node of class targetClass with name targetName
			if (targetClass.isInstance(node)) {
				// System.out.println("FOUND: " + node.getRawSignature());
				nodes.add(node);
			}
		} // try
		catch (SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		return;
	}

	
	/**
	 * Find the bindings for the given function
	 * TODO: Return these bindings
	 * @param functionName
	 * @param projectIndex
	 */
	protected static void findBindingsForFunction(String functionName, IIndex projectIndex) {
		try {
			// get the lock for this index
			projectIndex.acquireReadLock();
			// find bindings for the given function name
			IIndexBinding[] bindings = projectIndex.findBindings(functionName.toCharArray(), IndexFilter.ALL_DECLARED,
					new NullProgressMonitor());
			// find references for each binding
			for (IIndexBinding binding : bindings) {
				if (binding instanceof IFunction) {

					// get references
					IIndexName[] references = projectIndex.findReferences(binding);
					// do something with these references
					for (IIndexName reference : references) {
						IASTFileLocation fileLocation = reference.getFileLocation();
						// print
						System.out.println(fileLocation.getFileName() + " at offset " + fileLocation.getNodeOffset());
						// output name of enclosing function
						IIndexName function = reference.getEnclosingDefinition();
						if (function != null) {
							IBinding enclosing = projectIndex.findBinding(function);
							if (enclosing instanceof IFunction) {
								System.out.print(" within " + enclosing.getName());
							}
						}
					}
					System.out.println();
				}
			}
		} catch (InterruptedException | CoreException e) {
			e.printStackTrace();
		} finally {
			projectIndex.releaseReadLock();
		}
	}


	
	
	/**
	 * For a function call, tries to find a matching function declaration.
	 * Checks the argument count.
	 * 
	 * @param index the index
	 * @return a generated declaration or {@code null} if not suitable
	 */
	protected static IASTSimpleDeclaration tryInferTypeFromFunctionCall(IASTName astName, INodeFactory factory, IIndex index) {
		if (astName.getParent() instanceof IASTIdExpression && astName.getParent().getParent() instanceof IASTFunctionCallExpression
				&& astName.getParent().getPropertyInParent() == IASTFunctionCallExpression.FUNCTION_NAME) {
			IASTFunctionCallExpression call = (IASTFunctionCallExpression) astName.getParent().getParent();
			FunctionNameFinderVisitor visitor = new FunctionNameFinderVisitor();
			call.getFunctionNameExpression().accept(visitor);
			IASTName funcname = visitor.name;
			int expectedParametersNum = 0;
			int targetParameterNum = -1;
			for (IASTNode n : call.getChildren()) {
				if (n.getPropertyInParent() == IASTFunctionCallExpression.ARGUMENT) {
					if (n instanceof IASTIdExpression && n.getChildren()[0] == astName) {
						targetParameterNum = expectedParametersNum;
					}
					expectedParametersNum++;
				}
			}
			if (targetParameterNum == -1) {
				return null;
			}
			IBinding[] bindings;
			{
				IBinding binding = funcname.resolveBinding();
				if (binding instanceof IProblemBinding) {
					bindings = ((IProblemBinding) binding).getCandidateBindings();
				} else {
					bindings = new IBinding[] { binding };
				}
			}
			try {
				index.acquireReadLock();
				Set<IIndexName> declSet = new HashSet<>();
				// fill declSet with proper declarations
				for (IBinding b : bindings) {
					if (b instanceof IFunction) {
						IFunction f = (IFunction) b;
						if (f.getParameters().length == expectedParametersNum) {
							// Consider this overload
							IIndexName[] decls = index.findDeclarations(b);
							declSet.addAll(Arrays.asList(decls));
						}
					}
				}
				HashMap<ITranslationUnit, IASTTranslationUnit> astCache = new HashMap<>();
				for (IIndexName decl : declSet) {
					// for now, just use the first overload found
					ITranslationUnit tu = CdtUtilities.getTranslationUnitFromIndexName(decl);
					if (tu == null) {
						continue;
					}
					
					IASTTranslationUnit ast = null;
					if (astCache.containsKey(tu)) {
						ast = astCache.get(tu);
					} else {
						ast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
						astCache.put(tu, ast);
					}
					
					IASTName name = (IASTName) ast.getNodeSelector(null).findEnclosingNode(decl.getNodeOffset(), decl.getNodeLength());
					IASTNode fdecl = name;
					while (fdecl instanceof IASTName) {
						fdecl = fdecl.getParent();
					}
					assert (fdecl instanceof IASTFunctionDeclarator);
					// find the needed param number
					int nthParam = 0;
					for (IASTNode child : fdecl.getChildren()) {
						if (child instanceof IASTParameterDeclaration) {
							if (nthParam == targetParameterNum) {
								IASTParameterDeclaration pd = (IASTParameterDeclaration) child;
								IASTDeclSpecifier declspec = pd.getDeclSpecifier().copy(CopyStyle.withLocations);
								IASTDeclarator declarator = pd.getDeclarator().copy(CopyStyle.withLocations);
								setNameInNestedDeclarator(declarator, astName.copy(CopyStyle.withLocations));
								IASTSimpleDeclaration declaration = factory.newSimpleDeclaration(declspec);
								declaration.addDeclarator(declarator);
								return declaration;
							}
							nthParam++;
						}
					}
					name.getParent();
				}
			} catch (InterruptedException e) {
				// skip
			} catch (CoreException e) {
				Activator.log(e);
			} finally {
				index.releaseReadLock();
			}
		}
		return null;
	}

	
	private static void setNameInNestedDeclarator(IASTDeclarator declarator, IASTName astName) {
		while (declarator.getNestedDeclarator() != null) {
			declarator = declarator.getNestedDeclarator();
		}
		declarator.setName(astName);
	}
	
	
	public static class NameFinderVisitor extends ASTVisitor {
		public IASTName name;
		{
			shouldVisitNames = true;
		}

		@Override
		public int visit(IASTName name) {
			this.name = name;
			return PROCESS_ABORT;
		}
	}
	
	private static class FunctionNameFinderVisitor extends NameFinderVisitor {
		{
			shouldVisitExpressions = true;
		}
		
		@Override
		public int visit(IASTExpression expression) {
			if (expression instanceof IASTFieldReference) {
				this.name = ((IASTFieldReference) expression).getFieldName();
				return PROCESS_ABORT;
			}
			return super.visit(expression);
		}	
	}
	
	
	private static class FunctionNameFinderVisitor2 extends NameFinderVisitor{
		{
			shouldVisitExpressions = true;
		}
		
		@Override
		public int visit(IASTExpression expression) {
			if (expression instanceof IASTFunctionCallExpression) {
				this.name = ((IASTIdExpression)((IASTFunctionCallExpression) expression).getFunctionNameExpression()).getName();
				return PROCESS_ABORT;
			}
			return super.visit(expression);
		}	
		
	}

}
