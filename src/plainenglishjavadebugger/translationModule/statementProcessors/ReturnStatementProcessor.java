package plainenglishjavadebugger.translationModule.statementProcessors;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jdt.debug.core.IJavaThread;

import plainenglishjavadebugger.translationModule.StatementType;
import plainenglishjavadebugger.translationModule.TranslatedLine;

/*
 * This code belongs to:
 * Ahmet Emre Unal
 * S001974
 * emre.unal@ozu.edu.tr
 */

public class ReturnStatementProcessor extends StatementProcessor {
	private final String returnStatementInfoLink = "http://docs.oracle.com/javase/tutorial/java/javaOO/returnvalue.html";
	
	private IJavaThread thread;
	private TranslatedLine translatedLine;
	private String executedSourceLine;
	
	public ReturnStatementProcessor(IJavaThread thread, TranslatedLine translatedLine, String executedSourceLine) {
		this.thread = thread;
		this.translatedLine = translatedLine;
		this.executedSourceLine = executedSourceLine;
		process();
	}
	
	private void process() {
		translatedLine.setStatementType(StatementType.RETURN);
		translatedLine.setLongDescription("This statement is a return statement.");
		
		int returnValueStartIndex = executedSourceLine.indexOf(' ');
		if (returnValueStartIndex != -1) {
			// There is a return value.
			processReturnWithValue(returnValueStartIndex);
		} else {
			// Just an empty return statement.
			processReturnWithoutValue();
		}
		translatedLine.appendToLongDescription("For more information on return values, please visit:\n" + returnStatementInfoLink);
	}
	
	private void processReturnWithoutValue() {
		translatedLine.setShortDescription("You are returning to the calling method.");
		translatedLine.appendToLongDescription("This statement returns to the calling method.");
	}
	
	private void processReturnWithValue(int returnValueStartIndex) {
		// +1 to throw out the space at the beginning, -1 to throw out the semi-colon.
		String returned = executedSourceLine.substring(returnValueStartIndex + 1, (executedSourceLine.length() - 1));
		translatedLine.setShortDescription("You are returning \"" + returned + "\".");
		translatedLine.appendToLongDescription("This statement returns a value to the calling method, specifically \"" + returned + "\".");
		if (returned.matches(javaNameRegex)) {
			getReturnValue(returned);
		}
	}
	
	private void getReturnValue(String returned) {
		try {
			IValue returnValue = thread.findVariable(returned).getValue();
			translatedLine.appendToLongDescription("The returned variable has the value of \"" + returnValue.getValueString() + "\".");
		} catch (DebugException e) {
			// Don't append the value of the returned variable.
			System.err.println("Unable to get the value of the returned variable!");
		}
	}
}
