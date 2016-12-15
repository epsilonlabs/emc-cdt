package org.eclipse.epsilon.emc.cdt.propertygetter;

import java.util.HashMap;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.emc.cdt.NameFinderASTVisitor;

public class TranslationUnitGetter extends CElementGetter<Object> {
	private final static String ACCEPT_NAME		= "name";
	
	private final static String ACCEPT_LIB		= "lib";

	/** Pairs of ITranslationUnit, IASTTranslationUnit **/
	private HashMap<ITranslationUnit, IASTTranslationUnit> astCache = new HashMap<ITranslationUnit, IASTTranslationUnit>();

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
					IIndex projectIndex = CCorePlugin.getIndexManager().getIndex(tu.getCProject() );
					
					//cache ast
					IASTTranslationUnit ast = null;
					if (astCache.containsKey(tu)){
						ast = astCache.get(tu);
					}
					else{
						ast = tu.getAST(projectIndex, ITranslationUnit.AST_SKIP_INDEXED_HEADERS);
						astCache.put(tu, ast);
					}
					
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
