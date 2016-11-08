package org.eclipse.epsilon.emc.cdt;

import org.eclipse.cdt.core.dom. ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.runtime.IPath;

public class ReflectiveASTVisitor extends ASTVisitor {

	/** project */
	protected ICProject project = null; 

	/** parser*/
	protected IASTTranslationUnit parser = null;
	
	
	/**
	 * Class constructor: initialise a ReflectiveASTVisitor
	 * @param aProject
	 */
	public ReflectiveASTVisitor (ICProject aProject){
		this.project = aProject;
	}
	
	
	/**
	 * Return the AST for a project
	 * @return IASTTranslationUnit
	 */
	protected IASTTranslationUnit getASTParser(){
		try {
			for (IPathEntry child : project.getResolvedPathEntries()){
				System.out.println(child.getPath().toString());
			}
		} catch (CModelException e) {
			e.printStackTrace();
		}
		
//		if (parser == null){
//			FileContent fileContent 					= FileContent.createForExternalFileLocation(file.getRawLocation().toString());
//			IScannerInfo info							= new ScannerInfo(new HashMap<String, String>());
//			IncludeFileContentProvider emptyIncludes	= IncludeFileContentProvider.getEmptyFilesProvider();
//			IIndex index								= EmptyCIndex.INSTANCE;
//			int options									= ITranslationUnit.AST_SKIP_ALL_HEADERS | GPPLanguage.OPTION_NO_IMAGE_LOCATIONS | GPPLanguage.OPTION_IS_SOURCE_UNIT;
//	        IParserLogService log 						= new DefaultLogService();
//
//			parser = GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, index, options , log);
//		}
		return null;
	}
}
