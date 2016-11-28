package org.eclipse.epsilon.emc.cdt.propertygetter;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;

public class FunctionCallExpressionGetter extends ObjectPropertyGetter<IASTFunctionCallExpression, Object> {

	private final static String ACCEPT_BINDING		= "binding";
	private final static String ACCEPT_NAMESPACE 	= "namespace";

	
	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public FunctionCallExpressionGetter() {
		super(IASTFunctionCallExpression.class, ACCEPT_BINDING, ACCEPT_NAMESPACE);
	}

	

	@Override
	public Object getValue(IASTFunctionCallExpression object, String property) {
		//binding command --> return an IBinding object
		if (property.equals(ACCEPT_BINDING)){
			if (object instanceof ICPPASTFunctionCallExpression){
				Object obj = ((IASTFunctionCallExpression)object).getFunctionNameExpression(); 
				if (obj instanceof IASTIdExpression)
					return ((IASTIdExpression)obj).getName().resolveBinding();
			}
		}
		
		//namespace command --> return an IScope object
		if (property.equals(ACCEPT_NAMESPACE)){
			Object obj = getValue(object, ACCEPT_BINDING);
			if ( (obj != null) && (obj instanceof IBinding)){
				IScope o;
				try {
					o = ((IBinding)obj).getScope();
					while (!(o instanceof ICPPNamespaceScope)){
						o = o.getParent();
					}
					return o;
				} 
				catch (DOMException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
