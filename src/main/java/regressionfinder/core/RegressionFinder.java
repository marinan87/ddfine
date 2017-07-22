package regressionfinder.core;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileSourceCodeChange;

@Component
public class RegressionFinder {
	
	private static final String PATH_TO_PACKAGE = "package1";
	private static final String SOURCE_OF_LOCALIZATION = "Example.java";
	

	@Autowired
	private EvaluationContext evaluationContext;	
	
	public void run() {
		
		
		// TODO: First implement only a simple case, when tree structure does not change. 
		// Source code changes detected inside files, encapsulate information about changed file.
		// - navigate the source code tree (only inside src)  Tree structure (Guava?) 
		// - take into account only java files.
		// - compare the size of java files, if different - run distiller, if same - calculate hash first, then run distiller, if necessary
		
		// TODO: Next round: source code tree changes, fileops + sourcecodeops
		// added, removed files? fileops
		// modified files - sourcecodeops
		// added, removed dirs? fileops
		// folders with same name analyzed recursively. Repeat in each dir. 
		// git diff?
		
		List<FileSourceCodeChange> filteredChanges = extractDistilledChanges();
		FileSourceCodeChange[] failureInducingChanges = runDeltaDebugging(filteredChanges);
		applyFailureInducingChanges(failureInducingChanges);
//		highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
//		displayDoneDialog(event, failureInducingChanges);
	}

	public List<FileSourceCodeChange> extractDistilledChanges() {
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		
		Path examplePath = Paths.get(PATH_TO_PACKAGE, SOURCE_OF_LOCALIZATION);
		return extractDistillerChangesForPath(distiller, examplePath);
	}

	private List<FileSourceCodeChange> extractDistillerChangesForPath(FileDistiller distiller, Path examplePath) {
		// TODO: move to FileSourceCodeChange?
		File left = evaluationContext.getReferenceProject().findFile(examplePath);
		File right = evaluationContext.getFaultyProject().findFile(examplePath);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		List<SourceCodeChange> filteredChanges = filterOutSafeChanges(distiller.getSourceCodeChanges());		
		
		return transformToFileSourceCodeChanges(filteredChanges, examplePath);
	}
	
	
	
	private List<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE)
				.collect(Collectors.toList());
	}
	
	private List<FileSourceCodeChange> transformToFileSourceCodeChanges(List<SourceCodeChange> changes, Path pathToFile) {
		return changes.stream()
				.map(change -> new FileSourceCodeChange(change, pathToFile))
				.collect(Collectors.toList());
	}
	
	public FileSourceCodeChange[] runDeltaDebugging(List<FileSourceCodeChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		Object[] resultArray = new DD(evaluationContext).ddMin(completeDeltaSet).toArray();
		
		return Arrays.copyOf(resultArray, resultArray.length, FileSourceCodeChange[].class);
	}
	
	public void applyFailureInducingChanges(FileSourceCodeChange[] failureInducingChanges) {
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
