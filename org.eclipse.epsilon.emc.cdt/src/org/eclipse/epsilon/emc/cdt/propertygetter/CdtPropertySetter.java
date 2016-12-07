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

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.epsilon.emc.cdt.CdtModel;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.introspection.java.JavaPropertySetter;

public class CdtPropertySetter extends JavaPropertySetter{

	private final static String ACCEPT_TU		= "tu";
	
	private CdtModel model;
	
	public CdtPropertySetter (CdtModel m){
		this.model = m;
	}
	
	
	@Override
	public void invoke(Object value) throws EolRuntimeException {
		System.out.println("Setter called\t" + value.getClass().getSimpleName());
		if ( (object instanceof ICProject) 
				&& (property.equals(ACCEPT_TU)) 
				&& (value instanceof ITranslationUnit)){
			model.setTranslationUnit((ITranslationUnit)value);
		}
		else
			super.invoke(value);
	}

}
