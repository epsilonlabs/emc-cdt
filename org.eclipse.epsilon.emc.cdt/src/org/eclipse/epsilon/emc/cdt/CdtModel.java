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

import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.cdt.propertygetter.CdtPropertyGetter;
import org.eclipse.epsilon.emc.cdt.propertygetter.CdtPropertySetter;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.introspection.IPropertySetter;
import org.eclipse.epsilon.eol.models.CachedModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;

public class CdtModel extends CachedModel<Object>{

	/** list of supported types*/
	protected List<String> supportedTypes = Arrays.asList("ICElement", "ICProject", "ITranslationUnit");
	
	/** denotes property projects*/
	public static final String PROPERTY_PROJECT = "cproject";
	
	/** denotes whether bindings should be resolved*/
	public static final String PROPERTY_RESOLVE_BINDINGS = "resolveBindings";

	/** C project*/
	protected ICProject cproject = null;
	
	/** Resolve bindings flag*/
	private boolean resolveBindings = false;

	/** Visitor element*/
	private ReflectiveASTVisitor visitor = null;
	
	/** Refactoring element*/
	private RefactoringAST refactor = null;
	
	/** Property getter */
	protected CdtPropertyGetter propertyGetter = new CdtPropertyGetter();

	/** Property setter */
	protected CdtPropertySetter propertySetter = new CdtPropertySetter(this);

	
	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException {
		System.out.println(getClass().getSimpleName() +".getEnumerationValue(..)");
		throw new UnsupportedOperationException("getEnumerationValue(..) not Implemented");
	}

	
	@Override
 	public String getTypeNameOf(Object instance) {
		System.out.println(getClass().getSimpleName() +".getTypeNameOf(..)");
		throw new UnsupportedOperationException("getTypeNameOf(..) not Implemented");
	}

	
	@Override
 	public Object getElementById(String id) {
		System.out.println(getClass().getSimpleName() +".getElementById(..)");
		throw new UnsupportedOperationException("getElementById(..) not Implemented");
	}

	
	@Override
 	public String getElementId(Object instance) {
		System.out.println(getClass().getSimpleName() +".getElementId(..)");
		throw new UnsupportedOperationException("getElementId(..) not Implemented");
	}

	@Override
 	public void setElementId(Object instance, String newId) {
		System.out.println(getClass().getSimpleName() +".setElementId(..)");
		throw new UnsupportedOperationException("setElementId(..) not Implemented");
	}


	@Override
 	public boolean isInstantiable(String type) {
		System.out.println(getClass().getSimpleName() +".isInstantiable(..)");
		throw new UnsupportedOperationException("isInstantiable(..) not Implemented");
	}

	
	@Override
 	public boolean hasType(String type) {
		try{
//			System.out.println(getClass().getSimpleName() +".hasType(..)");
			return supportedTypes.contains(type) 
					|| (Class.forName("org.eclipse.cdt.core.dom.ast.IAST" + type) != null);
		} 
		catch (ClassNotFoundException e) {
			try {
				return (Class.forName("org.eclipse.cdt.core.dom.ast.cpp.ICPPAST" + type) != null);
			} catch (ClassNotFoundException e1) {
//				e1.printStackTrace();
				return false;
			}
		}		
	}

	@Override
	protected Collection<Object> allContentsFromModel() {
		System.out.println(getClass().getSimpleName() +".allContentsFromModel(..)");
		throw new UnsupportedOperationException("allContentsFromModel(..) not Implemented");
	}	
	
	@Override
 	public Collection<Object> getAllOfKind(String kind) throws EolModelElementTypeNotFoundException {
//		System.out.println(getClass().getSimpleName() +".getAllOfKind(..)");
		return super.getAllOfKind(kind);
	}

	
 	public Collection<Object> getAllOfType(String type) throws EolModelElementTypeNotFoundException {
		System.out.println(getClass().getSimpleName() +".getAllOfType(..)");
		return super.getAllOfType(type);
	}
	
	
	@Override
 	protected Collection<Object> getAllOfKindFromModel(String kind) throws EolModelElementTypeNotFoundException {
//		System.out.println(getClass().getSimpleName() +".getAllOfKindFromModel(..)");
		return getAllOfTypeFromModel(kind);
	}
	
	
	@Override
 	protected Collection<Object> getAllOfTypeFromModel(String type) throws EolModelElementTypeNotFoundException {
//		System.out.println(getClass().getSimpleName() +".getAllOfTypeFromModel(..)");
		return visitor.getAllofType(type);
	}

	
	@Override
 	protected Object createInstanceInModel(String type) throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		System.out.println(getClass().getSimpleName() +".createInstanceInModel(..)");
		throw new UnsupportedOperationException("createInstanceInModel(..) not Implemented");
	}

	
	@Override
 	protected void disposeModel() {
//		System.out.println(getClass().getSimpleName() +".disposeModel(..)");
		cproject 		= null;
		resolveBindings = false;
		visitor 		= null;
		refactor		= null;
	}

	
	@Override
 	protected boolean deleteElementInModel(Object instance) throws EolRuntimeException {
		System.out.println(getClass().getSimpleName() +".deleteElementInModel(..)");
		throw new UnsupportedOperationException("deleteElementInModel(..) not Implemented");
	}
 
	
	@Override
 	protected Object getCacheKeyForType(String type) throws EolModelElementTypeNotFoundException {
//		System.out.println(getClass().getSimpleName() +".getCacheKeyForType(..)");
		return type;
	}

	
	@Override
 	protected Collection<String> getAllTypeNamesOf(Object instance) {
		System.out.println(getClass().getSimpleName() +".getAllTypeNamesOf(..)");
		throw new UnsupportedOperationException("getAllTypeNamesOf(..) not Implemented");
	}
	
	
	/**
	 *  Load properties
	 */
	@Override
 	public void load(StringProperties properties, IRelativePathResolver resolver) throws EolModelLoadingException {
		super.load(properties, resolver);
		
		//get project name
		String projectName = properties.getProperty(CdtModel.PROPERTY_PROJECT);
		try {
			cproject = CdtUtilities.getICProject(projectName);
		} catch (CoreException e) {
			throw new EolModelLoadingException(e, this);
		}
		
		//get resolve bindings flag
		resolveBindings = Boolean.parseBoolean(properties.getProperty(PROPERTY_RESOLVE_BINDINGS));
		
		//init visitor
		visitor = new ReflectiveASTVisitor(cproject, resolveBindings);
		
		//init refactor
		refactor = new RefactoringAST(cproject);
		
		//finally load model
		load();
	}
	
	
	/**
	 * Load model
	 */
	@Override
 	protected void loadModel() throws EolModelLoadingException {
		
	}

	
	/**
	 * Check whether it is the given object is owned
	 */
	@Override
  	public boolean owns (Object object){
//		System.out.println(object.getClass().getSimpleName() +"\t"+ (object instanceof IASTNode));
		if ( (object instanceof ICElement) || (object instanceof IASTNode) ){
			return true;
		}
		return false;
	}

	
	/* (non-Javadoc) not implemented
	 * @see org.eclipse.epsilon.eol.models.IModel#store(java.lang.String)
	 */
	@Override
 	public boolean store(String location) {
		System.out.println(getClass().getSimpleName() +".store(..)");
		throw new UnsupportedOperationException("store(..) not Implemented");
	}

	
	/* (non-Javadoc) not implemented
	 * @see org.eclipse.epsilon.eol.models.IModel#store()
	 */
	@Override
 	public boolean store() {
		System.out.println(getClass().getSimpleName() +".store()");
//		refactor.addNewFunction("func");
//		refactor.replaceFunction("test", "testMe");
		refactor.refactor(new String[]{"tinyxml2.cpp", "tinyxml2.h"});
		return true;
	}

	
	/**
	 * Get property getter
	 */
	@Override
	public IPropertyGetter getPropertyGetter() {
//		System.out.println(getClass().getSimpleName() +".getPropertyGetter(..)");
		return propertyGetter;
	}
	
	
	/**
	 * Get property setter
	 * @return
	 */
	@Override
	public IPropertySetter getPropertySetter() {
		System.out.println(getClass().getSimpleName() +".getPropertySetter(..)");
		return propertySetter;
	}

	
	public void setTranslationUnit(ITranslationUnit tu){
		try {
			visitor.setAST(tu);
		} 
		catch (UnexpectedException | CoreException e) {
			e.printStackTrace();
		}
	}
	
}
