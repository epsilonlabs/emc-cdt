package org.eclipse.epsilon.emc.cdt.dt;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class Utility {

	
	protected static MessageConsole findConsole (String consoleName){
		ConsolePlugin consolePlugin 	= ConsolePlugin.getDefault();
		IConsoleManager consoleManager	= consolePlugin.getConsoleManager();
		IConsole existingConsoles[]		= consoleManager.getConsoles();
		//try to find an existing console
		for (int i=0; i<existingConsoles.length; i++){
			if (consoleName.equals(existingConsoles[i].getName()))
				return (MessageConsole) existingConsoles[i];
		}
		
		//if it does not exist, create a new one
		MessageConsole newConsole = new MessageConsole(consoleName, null);
		consoleManager.addConsoles(new IConsole[]{newConsole});
		return newConsole;
	}
	
	
	public static void writeToConsole(String consoleName, String message){
		MessageConsole console 		= findConsole(consoleName);
		MessageConsoleStream out	= console.newMessageStream();
		out.println(message);
		
//		IDocument text = console.getDocument().set(message);;
		
		
	}
	
}
