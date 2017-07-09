package regressionfinder.handlers;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;

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
import regressionfinder.utils.FileUtils;

public class FaultLocalizationHandler {

	private static final String REFERENCE_VERSION = "BeforeRegression";
	private static final String FAULTY_VERSION = "Regression";
	private static final String SOURCE_OF_LOCALIZATION = "Example.java";
	private static final String TEST_CLASS_NAME = "simple.ExampleTest";
	private static final String TEST_METHOD_NAME = "tenMultipliedByTenIsOneHundred";
	
	private final String basePath;
	private final String pathToReferenceVersion;
	private final String pathToFaultyVersion;

	
	private FaultLocalizationHandler(String basePath) {
		this.basePath = basePath;
		pathToReferenceVersion = FileUtils.getPathToJavaFile(basePath, REFERENCE_VERSION, SOURCE_OF_LOCALIZATION);		
		pathToFaultyVersion = FileUtils.getPathToJavaFile(basePath, FAULTY_VERSION, SOURCE_OF_LOCALIZATION);
	}

	public static void main(String[] args) {
		SourceCodeChange[] failureInducingChanges = null;
		try {			
			FaultLocalizationHandler handler = new FaultLocalizationHandler(args[0]);
			List<SourceCodeChange> allChanges = handler.extractDistilledChanges();
			List<SourceCodeChange> filteredChanges = handler.filterOutSafeChanges(allChanges);
			failureInducingChanges = handler.runDeltaDebugging(filteredChanges);
			
			handler.applyFailureInducingChanges(failureInducingChanges);
//			highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
//		displayDoneDialog(event, failureInducingChanges);
	}

	private SourceCodeChange[] runDeltaDebugging(List<SourceCodeChange> filteredChanges) throws Exception {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
		
		EvaluationTask task = new EvaluationTask(pathToReferenceVersion, pathToFaultyVersion, TEST_CLASS_NAME, TEST_METHOD_NAME);
		DeltaSetEvaluator evaluator = new DeltaSetEvaluator(task, basePath);			
		Object[] resultArray = new DD(evaluator).ddMin(completeDeltaSet).toArray();
		return Arrays.copyOf(resultArray, resultArray.length, SourceCodeChange[].class);
	}

	private List<SourceCodeChange> extractDistilledChanges() {
		File left = FileUtils.getFile(pathToReferenceVersion);
		File right = FileUtils.getFile(pathToFaultyVersion);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		return distiller.getSourceCodeChanges();
	}
	
	private List<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE)
				.collect(Collectors.toList());
	}
	
	private void applyFailureInducingChanges(SourceCodeChange[] failureInducingChanges) throws Exception {
		System.out.println(Arrays.toString(failureInducingChanges));
//		SourceCodeManipulator.copyAndModifyLocalizationSource(pathToReferenceVersion, failureInducingChanges);
	}
	
//	private void highlightFailureInducingChangesInEditor(ICompilationUnit cu, SourceCodeChange[] failureInducingChanges) throws Exception {
//		if (failureInducingChanges == null || failureInducingChanges.length == 0) {
//			return;
//		}
//		
//		ITextEditor textEditor = JavaModelHelper.openTextEditor(cu);
//		
//		// Seems that there is no way to highlight all changes at once in Eclipse. 
//		// Currently only highlighting the first change.
//		SourceCodeChange firstChange = failureInducingChanges[0];
//		int startPosition = firstChange.getChangedEntity().getStartPosition();
//		int length = firstChange.getChangedEntity().getEndPosition() - startPosition;
//		textEditor.setHighlightRange(startPosition, length, false);
//		textEditor.selectAndReveal(startPosition, length);
//	}
	
//	private void displayDoneDialog(ExecutionEvent event, SourceCodeChange[] failureInducingChanges) throws ExecutionException {
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		MessageDialog.openInformation(window.getShell(), "RegressionFinder", getStringRepresentation(failureInducingChanges));
//	}

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
