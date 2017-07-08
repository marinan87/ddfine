package regressionfinder.handlers;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
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
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import regressionfinder.testrunner.DeltaSetEvaluator;
import regressionfinder.utils.JavaModelHelper;

public class FaultLocalizationHandler extends AbstractHandler {

	private static final String REFERENCE_VERSION = "BeforeRegression";
	private static final String FAULTY_VERSION = "Regression";
	private static final String SOURCE_OF_LOCALIZATION = "Example.java";
	private static final String TEST_CLASS_NAME = "simple.ExampleTest";
	private static final String TEST_METHOD_NAME = "tenMultipliedByTenIsOneHundred";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		SourceCodeChange[] failureInducingChanges = null;
		try {
			ICompilationUnit sourceCU = JavaModelHelper.getCompilationUnit(REFERENCE_VERSION, SOURCE_OF_LOCALIZATION);
			ICompilationUnit regressionCU = JavaModelHelper.getCompilationUnit(FAULTY_VERSION, SOURCE_OF_LOCALIZATION);

			List<SourceCodeChange> allChanges = extractDistilledChanges(sourceCU, regressionCU);
			List<SourceCodeChange> filteredChanges = filterOutSafeChanges(allChanges);
								
			DeltaSet completeDeltaSet = new DeltaSet();
			completeDeltaSet.addAll(filteredChanges);
						
			EvaluationTask task = new EvaluationTask(sourceCU, regressionCU, TEST_CLASS_NAME, TEST_METHOD_NAME);
			DeltaSetEvaluator evaluator = new DeltaSetEvaluator(task);			
			Object[] resultArray = new DD(evaluator).ddMin(completeDeltaSet).toArray();
			failureInducingChanges = Arrays.copyOf(resultArray, resultArray.length, SourceCodeChange[].class);
			
			applyFailureInducingChanges(sourceCU, failureInducingChanges);
			highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
		
		displayDoneDialog(event, failureInducingChanges);
		return null;
	}

	private List<SourceCodeChange> extractDistilledChanges(ICompilationUnit originalCU, ICompilationUnit regressionCU) throws JavaModelException {
		File left = JavaModelHelper.getFile(originalCU);
		File right = JavaModelHelper.getFile(regressionCU);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		return distiller.getSourceCodeChanges();
	}
	
	private List<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE)
				.collect(Collectors.toList());
	}
	
	private void applyFailureInducingChanges(ICompilationUnit sourceCU, SourceCodeChange[] failureInducingChanges) throws Exception {
		System.out.println(Arrays.toString(failureInducingChanges));
//		SourceCodeManipulator.copyAndModifyLocalizationSource(sourceCU, failureInducingChanges);
	}
	
	private void highlightFailureInducingChangesInEditor(ICompilationUnit cu, SourceCodeChange[] failureInducingChanges) throws Exception {
		if (failureInducingChanges == null || failureInducingChanges.length == 0) {
			return;
		}
		
		ITextEditor textEditor = JavaModelHelper.openTextEditor(cu);
		
		// Seems that there is no way to highlight all changes at once in Eclipse. 
		// Currently only highlighting the first change.
		SourceCodeChange firstChange = failureInducingChanges[0];
		int startPosition = firstChange.getChangedEntity().getStartPosition();
		int length = firstChange.getChangedEntity().getEndPosition() - startPosition;
		textEditor.setHighlightRange(startPosition, length, false);
		textEditor.selectAndReveal(startPosition, length);
	}
	
	private void displayDoneDialog(ExecutionEvent event, SourceCodeChange[] failureInducingChanges) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "RegressionFinder", getStringRepresentation(failureInducingChanges));
	}

	private String getStringRepresentation(SourceCodeChange[] failureInducingChanges) {
		if (failureInducingChanges == null || failureInducingChanges.length == 0) {
			return "No changes detected";
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("Regression is caused by the following changes:\r\n\r\n");
				
		for (SourceCodeChange change : failureInducingChanges) {
			// TODO: specify positions as well
			if (change instanceof Insert) {
				Insert insert = (Insert) change;
				builder.append("Inserted\t");
				builder.append(insert.getChangedEntity().getUniqueName());
			} else if (change instanceof Update) {
				Update update = (Update) change;
				builder.append("Updated\t");
				builder.append(update.getChangedEntity().getUniqueName());
				builder.append(" ===> ");
				builder.append(update.getNewEntity().getUniqueName());
			} else if (change instanceof Delete) {
				Delete delete = (Delete) change;
				builder.append("Deleted\t");
				builder.append(delete.getChangedEntity().getUniqueName());
			} else if (change instanceof Move) {
				Move move = (Move) change;
				builder.append("Moved\t");
				builder.append(String.format("entity %s in parent %s", move.getChangedEntity().getUniqueName(), move.getParentEntity().getUniqueName()));
				builder.append(String.format(", now becomes entity %s in parent %s", move.getNewEntity().getUniqueName(), move.getNewParentEntity().getUniqueName()));
			}
			
			builder.append("\r\n");
		}
		
		return builder.toString();
	}
}
