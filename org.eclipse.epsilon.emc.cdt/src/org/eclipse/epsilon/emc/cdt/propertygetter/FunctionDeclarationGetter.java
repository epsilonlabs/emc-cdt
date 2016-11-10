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

public class FunctionDeclarationGetter extends ObjectPropertyGetter<IASTFunctionDeclarator, String> {

	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public FunctionDeclarationGetter() {
		super(IASTFunctionDeclarator.class, "name");
	}

	
	@Override
	public String getValue(IASTFunctionDeclarator object, String property) {
		return object.getName().toString();
	}

	
}
