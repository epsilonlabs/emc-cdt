package org.eclipse.epsilon.emc.cdt.propertygetter;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.emc.cdt.NameFinderASTVisitor;

public class TranslationUnitGetter extends CElementGetter<Object> {
	private final static String ACCEPT_NAME		= "name";
		

	private final static String ACCEPT_LIB		= "lib";

	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public TranslationUnitGetter() {
		super(ITranslationUnit.class, ACCEPT_NAME, ACCEPT_LIB);
	}
	
	
	@Override
	public Object getValue(ICElement object, String property) {
		//name command --> call superclass
		if (property.equals(ACCEPT_NAME)){
			return super.getValue(object, property);
		}
		//lib command 
		else if (property.equals(ACCEPT_LIB)){
			if (object instanceof ITranslationUnit){
				try {
					//get tu
					ITranslationUnit tu = (ITranslationUnit)object;
					//create index
					IIndex index = CCorePlugin.getIndexManager().getIndex(tu.getCProject() );
					// get AST
					IASTTranslationUnit ast = tu.getAST(index, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
					//visitor
					NameFinderASTVisitor visitor = new NameFinderASTVisitor();
					ast.accept(visitor);
					return visitor.getList();
				} 
				catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
