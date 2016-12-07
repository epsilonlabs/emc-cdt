package org.eclipse.epsilon.emc.cdt.propertygetter;

import org.eclipse.cdt.core.model.ICElement;

public class CElementGetter<R> extends ObjectPropertyGetter<ICElement, R> {

	private final static String ACCEPT_NAME		= "name";

	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public CElementGetter(Class<?> clazz, String... properties) {
		super(clazz, properties);
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public R getValue(ICElement object, String property) {
		//return the name of this ICElement
		if (property.equals(ACCEPT_NAME)){
			return (R) object.getElementName();
		}

		return null;
	}
}
