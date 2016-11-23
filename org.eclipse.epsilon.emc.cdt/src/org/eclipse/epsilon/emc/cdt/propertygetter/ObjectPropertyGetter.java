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

import org.eclipse.cdt.core.dom.ast.IASTNode;

public abstract class ObjectPropertyGetter<T extends IASTNode, R> {
	
	/** Class of this object*/
	protected Class<?> clazz;
	
	/** Properties associated with this object*/
	protected List<String> properties = new ArrayList<String>();
	
	
	/**
	 * Class constructor:
	 * @param clazz
	 * @param properties
	 */
	public ObjectPropertyGetter(Class<?> clazz, String... properties) {
		this.clazz = clazz;
		this.properties.addAll(Arrays.asList(properties));
	}
	
	
	/**
	 * Check whether the given object and property combination belong to this class 
	 * @param object
	 * @param property
	 * @return
	 */
	public boolean appliesTo(Object object, String property) { 
		return properties.contains(property) && clazz.isInstance(object);
	}
	
	
	/**
	 * Get the value for this object and property combination
	 * @param object
	 * @param property
	 * @return
	 */
	public abstract R getValue(T object, String property);
	

	
}
