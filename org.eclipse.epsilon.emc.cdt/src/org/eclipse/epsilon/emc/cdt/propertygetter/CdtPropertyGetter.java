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
package org.eclipse.epsilon.emc.cdt.propertygetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.introspection.java.JavaPropertyGetter;

@SuppressWarnings("rawtypes")
public class CdtPropertyGetter extends JavaPropertyGetter {

	/** Property getters list*/
	protected List<ObjectPropertyGetter> objectPropertyGetters = new ArrayList<ObjectPropertyGetter>();
	

	/**
	 * Class constructor
	 */
	public CdtPropertyGetter() {
		objectPropertyGetters.addAll(Arrays.asList(new ObjectPropertyGetter[]{
				new FunctionDeclarationGetter()
				//....
		}));
	}
	
	
	/**
	 * Find appropriate class and invoke method
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object invoke(Object object, String property) throws EolRuntimeException {
	
		Object result = null;
		
		for (ObjectPropertyGetter objectPropertyGetter : objectPropertyGetters) {
			//check if the given object and property combination belong to this class
			if (objectPropertyGetter.appliesTo(object, property)) {
				//if so, get the value for this property
				result = objectPropertyGetter.getValue((IASTNode) object, property);
				break;
			}
		}
		
		//if not, try to guess the the metho
		if (result == null) 
			result = super.invoke(object, property);
		
		if (result instanceof IASTName) {
			return result.toString();
		}
		else {
			return result;
		}
	}


}
