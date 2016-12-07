package org.eclipse.epsilon.emc.cdt.propertygetter;

import org.eclipse.cdt.core.model.ITranslationUnit;

public class TranslationUnitGetter extends CElementGetter<Object> {
	private final static String ACCEPT_NAME		= "name";
		
	
	/**
	 * Class constructor
	 * @param clazz
	 * @param properties
	 */
	public TranslationUnitGetter() {
		super(ITranslationUnit.class, ACCEPT_NAME);
	}

}
