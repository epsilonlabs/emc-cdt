package org.eclipse.epsilon.emc.cdt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNodeFactory;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;

public class RefactoringAST {
	
	/** Main AST */
	private IASTTranslationUnit mainAST = null;

	/** flag indicating whether a change has been made*/
	private boolean changedAST = false;

	/** List of changes in AST*/
	List<Change> changesList = null;
	
	
	/** Class constructor */
	public RefactoringAST (IASTTranslationUnit ast) {
		this.mainAST 		= ast;
		this.changedAST		= false;
		this.changesList	= new ArrayList<Change>();
	}

	
	/** Store the modified AST if any changes have been made */
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
	
	
	
	protected void addNewFunction(String name){
		//try to force a change
		try {
			IASTNode node = ASTUtilities.findNodeInTree(mainAST, 1, Class.forName("org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator"), "getName", "test");
			if (node != null && node instanceof IASTFunctionDeclarator){

				System.out.println(node.isFrozen());
//				node.copy(CopyStyle.)
				
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
