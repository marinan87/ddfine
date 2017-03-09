package regressionfinder.handlers;

import java.io.File;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import dd.DeltaDebug;
import regressionfinder.testrunner.JUnitTestHarness;
import regressionfinder.utils.JavaModelHelper;
import regressionfinder.utils.SourceCodeManipulator;

/*
 * Starting point. 
 */
public class FaultLocalizationHandler extends AbstractHandler {

	private static final String VERSION_BEFORE_REGRESSION = "BeforeRegression";
	private static final String VERSION_WITH_REGRESSION = "Regression";
	private static final String LOCALIZATION_SOURCE = "Example.java";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			ICompilationUnit sourceCU = JavaModelHelper.getCompilationUnit(VERSION_BEFORE_REGRESSION, LOCALIZATION_SOURCE);
			ICompilationUnit regressionCU = JavaModelHelper.getCompilationUnit(VERSION_WITH_REGRESSION, LOCALIZATION_SOURCE);

			List<SourceCodeChange> allChanges = extractSourceCodeChanges(sourceCU, regressionCU);
						
			JUnitTestHarness testHarness = new JUnitTestHarness(sourceCU);
			List<SourceCodeChange> failureInducingChanges = DeltaDebug.ddmin(allChanges, testHarness);
			System.out.println(failureInducingChanges);
			
			applyFailureInducingChanges(sourceCU, failureInducingChanges);
			highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
		
		displayDoneDialog(event);
		return null;
	}

	private List<SourceCodeChange> extractSourceCodeChanges(ICompilationUnit originalCU, ICompilationUnit regressionCU) throws JavaModelException {
		File left = JavaModelHelper.getFile(originalCU);
		File right = JavaModelHelper.getFile(regressionCU);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		return distiller.getSourceCodeChanges();
	}
	
	private void applyFailureInducingChanges(ICompilationUnit sourceCU, List<SourceCodeChange> failureInducingChanges) throws Exception {
		SourceCodeManipulator.copyAndModifyLocalizationSource(sourceCU, failureInducingChanges);
	}
	
	private void highlightFailureInducingChangesInEditor(ICompilationUnit cu, List<SourceCodeChange> failureInducingChanges) throws Exception {
		if (failureInducingChanges.isEmpty()) {
			return;
		}
		
		ITextEditor textEditor = JavaModelHelper.openTextEditor(cu);
		
		// Seems that there is no way to highlight all changes at once. 
		// Currently only highlighting the first change.
		SourceCodeChange firstChange = failureInducingChanges.get(0);
		int startPosition = firstChange.getChangedEntity().getStartPosition();
		int length = firstChange.getChangedEntity().getEndPosition() - startPosition;
		textEditor.setHighlightRange(startPosition, length, false);
		textEditor.selectAndReveal(startPosition, length);
	}
	
	private void displayDoneDialog(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "RegressionFinder", "Done");
	}
}
