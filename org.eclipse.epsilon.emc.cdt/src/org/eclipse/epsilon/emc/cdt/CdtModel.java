package org.eclipse.epsilon.emc.cdt;

import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.models.CachedModel;

public class CdtModel extends CachedModel<Object>{

	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeNameOf(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getElementById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getElementId(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setElementId(Object instance, String newId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean owns(Object instance) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstantiable(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean store(String location) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean store() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Collection<? extends Object> allContentsFromModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<? extends Object> getAllOfTypeFromModel(String type)
			throws EolModelElementTypeNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<? extends Object> getAllOfKindFromModel(String kind)
			throws EolModelElementTypeNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object createInstanceInModel(String type)
			throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void loadModel() throws EolModelLoadingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void disposeModel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected boolean deleteElementInModel(Object instance) throws EolRuntimeException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Object getCacheKeyForType(String type) throws EolModelElementTypeNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<String> getAllTypeNamesOf(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private void test(){
		ASTVisitor visitor = new ASTVisitor() {
		};
	}

}
