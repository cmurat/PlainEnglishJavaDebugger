package plainenglishjavadebugger.translationModule;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;

import plainenglishjavadebugger.translationModule.fileReader.SourceFileReader;
import plainenglishjavadebugger.translationModule.statementProcessors.SourceCodeMatcher;
import plainenglishjavadebugger.views.translatorView.TranslatorViewModel;

/*
 * This code belongs to:
 * Ahmet Emre Unal
 * S001974
 * emre.unal@ozu.edu.tr
 */

/*
 * Scheduling a translation job: http://help.eclipse.org/kepler/topic/org.eclipse.platform.doc.isv/guide/runtime_jobs.htm?cp=2_0_3_5
 * 
 * When the status of the debugger changes, the translation job can be started, joined, executed and terminated.
 * Can get other info about other jobs through IJobManager.
 * Set a listener to the debugger, when it's status changes, start the translation job.
 * 
 * Translation job should be a system job:
 * (http://help.eclipse.org/kepler/topic/org.eclipse.platform.doc.isv/guide/runtime_jobs_progress.htm?cp=2_0_3_5_0)
 * 
 * This, most probably, is a SHORT job (Job Scheduling).
 */

public class Translator {
	private final TranslatorViewModel model;
	private final SourceFileReader sourceFileReader;
	private final SourceCodeMatcher sourceCodeProcessor;
	
	private IJavaThread thread;
	
	private TranslatedLine translatedLine;
	private IStackFrame topStackFrame;
	private Object debuggedClassSourceElement;
	private int debuggedLineNumber;
	private String debuggedClassPath;
	private int debugEventType;
	private String executedSourceLine;
	
	private String[] ignoredLines = { "}", "});" };
	
	public Translator(TranslatorViewModel model) {
		this.model = model;
		sourceFileReader = new SourceFileReader();
		sourceCodeProcessor = new SourceCodeMatcher();
		resetTranslator();
	}
	
	// The method that does the translation work
	public void translate(int debugEventType) {
		try {
			getCurrentFrameInfo();
			if (sourceCodeIsAvailable()) {
				this.debugEventType = debugEventType;
				getCurrentClassInfo();
				generateTranslatedLine();
			} else {
				System.out.println("Undebuggable, closed-source class.");
			}
		} catch (DebugException e) {
			System.err.println("Unable to get stack frame info from thread!");
			e.printStackTrace();
		}
	}
	
	private void getCurrentFrameInfo() throws DebugException {
		topStackFrame = thread.getTopStackFrame();
		debuggedClassSourceElement = topStackFrame.getLaunch().getSourceLocator().getSourceElement(topStackFrame);
	}
	
	private boolean sourceCodeIsAvailable() {
		/* Which means it's not a built-in, closed-source library class */
		return debuggedClassSourceElement != null;
	}
	
	private void getCurrentClassInfo() throws DebugException {
		debuggedLineNumber = topStackFrame.getLineNumber();
		debuggedClassPath = debuggedClassSourceElement.toString();
	}
	
	private void generateTranslatedLine() {
		executedSourceLine = sourceFileReader.readLine(topStackFrame, debuggedClassPath, debuggedLineNumber);
		if (!lineIsIgnored()) {
			createTranslatedLineObject();
			setTranslationInfo();
			returnLine();
			resetTranslator();
		}
	}
	
	private void createTranslatedLineObject() {
		translatedLine = new TranslatedLine(executedSourceLine, debugEventType);
	}
	
	private void setTranslationInfo() {
		sourceCodeProcessor.processStatement(thread, translatedLine, executedSourceLine);
	}
	
	private void returnLine() {
		model.addTranslatedLine(translatedLine);
	}
	
	private void resetTranslator() {
		translatedLine = null;
		topStackFrame = null;
		debuggedClassSourceElement = null;
		debuggedLineNumber = -1;
		debuggedClassPath = "";
		debugEventType = -1;
		executedSourceLine = "";
	}
	
	private boolean lineIsIgnored() {
		for (String ignoredLine : ignoredLines) {
			if (ignoredLine.equals(executedSourceLine)) {
				return true;
			}
		}
		return false;
	}
	
	public void setThread(IJavaThread thread) {
		this.thread = thread;
	}
	
	public void clearThread() {
		thread = null;
	}
}
