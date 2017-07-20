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
package org.eclipse.epsilon.emc.cdt;

import java.util.Collection;

/**
 * Interface for future functionality extension of reflective visitors
 * @author sgerasimou
 *
 */
public interface IReflectiveVisitor {

	
	public Collection<Object> getAllofKind(String kind, boolean visitAST);
	
	
	public Collection<Object> getAllofType(String type, boolean visitAST);
}
