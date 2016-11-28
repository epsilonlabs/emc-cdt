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

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;

public class FunctionDeclarationGetter extends ObjectPropertyGetter<IASTFunctionDeclarator, String> {

	private final static String ACCEPT_NAME		= "name";
//	private final static String ACCEPT_ALL		= "all";
//	private final static String ACCEPT_SYNTAX	= "syntax";
	private final static String ACCEPT_NAMESPACE = "namespace";

	
	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public FunctionDeclarationGetter() {
		super(IASTFunctionDeclarator.class);//, ACCEPT_NAME);//, ACCEPT_ALL, ACCEPT_SYNTAX);
	}

	
	@Override
	public String getValue(IASTFunctionDeclarator object, String property) {
		if (property.equals(ACCEPT_NAMESPACE)){
			if (object instanceof ICPPASTFunctionDefinition){
				
			}
			return null;
		}
		return object.getName().toString();
	}

	
}
