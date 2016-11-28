package org.eclipse.epsilon.emc.cdt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.index.IndexFilter;
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
	protected static IASTNode findNodeInTree(IASTNode node, int index, Class<?> targetClass, String targetName) {
		try {
			if (targetClass == null || targetName == null)
				throw new IllegalArgumentException("Target class or target name is NULL!");

			// get node's children
			IASTNode[] children = node.getChildren();

			// navigate recursively
			for (IASTNode childNode : children) {
				IASTNode tempNode = findNodeInTree(childNode, index + 1, targetClass, targetName);
				if (tempNode != null)
					return tempNode;
			}

			// if (children.length==0)
			// System.out.println("Node:\t" + currentNode.getRawSignature());

			// find node of class targetClass with name targetName
			if (targetClass.isInstance(node)) {
				Method method = targetClass.getMethod("getName", (Class<?>[]) null);
				if (targetName.equals(method.invoke(node).toString())) {
					System.out.println("FOUND: " + targetName);
					return node;
				}
			}
		} // try
		catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			// e.printStackTrace();
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
}
