package regressionfinder.core;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileUtils;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;
	
	public void run() {
		List<SourceCodeChange> filteredChanges = extractDistilledChanges();
		SourceCodeChange[] failureInducingChanges = runDeltaDebugging(filteredChanges);
		
		//only inside src folders! expect standard Maven structure
		//
		//added, removed files? fileops
		//modified files - first by size, if equal - then calculate hash, if changed -> Distiller
		//added, removed dirs? fileops
		//then folders with same name recursively. Repeat in each dir. 
		//
		//Tree structure  (Guava?)
		//
		//
		//0) Diff between two versions - Git mode.
		//1) Textual diff first, compare only changed files. 
		//2) Make work with a set of files. Very simple example with only one change (applying operation is supported)
		//3) Evaluate only changed files via ChangeDistiller.
		
		applyFailureInducingChanges(failureInducingChanges);
//		highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
//		displayDoneDialog(event, failureInducingChanges);
	}

	public List<SourceCodeChange> extractDistilledChanges() {
		File left = FileUtils.getFile(evaluationContext.getReferenceVersion());
		File right = FileUtils.getFile(evaluationContext.getFaultyVersion());
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		
		return filterOutSafeChanges(distiller.getSourceCodeChanges());
	}
	
	private List<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE)
				.collect(Collectors.toList());
	}
	
	public SourceCodeChange[] runDeltaDebugging(List<SourceCodeChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		JUnitTester evaluator = new JUnitTester() {
			@Override
			public int test(DeltaSet c) {
				@SuppressWarnings("unchecked")
				List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) c.stream().collect(toList());
				return evaluationContext.testSelectedChangeSet(selectedChangeSet); 
			}
		};
		Object[] resultArray = new DD(evaluator).ddMin(completeDeltaSet).toArray();
		
		return Arrays.copyOf(resultArray, resultArray.length, SourceCodeChange[].class);
	}
	
	public void applyFailureInducingChanges(SourceCodeChange[] failureInducingChanges) {
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

//	private String getStringRepresentation(SourceCodeChange[] failureInducingChanges) {
//		if (failureInducingChanges == null || failureInducingChanges.length == 0) {
//			return "No changes detected";
//		}
//		
//		StringBuilder builder = new StringBuilder();
//		builder.append("Regression is caused by the following changes:\r\n\r\n");
//				
//		for (SourceCodeChange change : failureInducingChanges) {
//			// TODO: specify positions as well
//			if (change instanceof Insert) {
//				Insert insert = (Insert) change;
//				builder.append("Inserted\t");
//				builder.append(insert.getChangedEntity().getUniqueName());
//			} else if (change instanceof Update) {
//				Update update = (Update) change;
//				builder.append("Updated\t");
//				builder.append(update.getChangedEntity().getUniqueName());
//				builder.append(" ===> ");
//				builder.append(update.getNewEntity().getUniqueName());
//			} else if (change instanceof Delete) {
//				Delete delete = (Delete) change;
//				builder.append("Deleted\t");
//				builder.append(delete.getChangedEntity().getUniqueName());
//			} else if (change instanceof Move) {
//				Move move = (Move) change;
//				builder.append("Moved\t");
//				builder.append(String.format("entity %s in parent %s", move.getChangedEntity().getUniqueName(), move.getParentEntity().getUniqueName()));
//				builder.append(String.format(", now becomes entity %s in parent %s", move.getNewEntity().getUniqueName(), move.getNewParentEntity().getUniqueName()));
//			}
//			
//			builder.append("\r\n");
//		}
//		
//		return builder.toString();
//	}
}
