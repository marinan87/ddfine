package regressionfinder.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
		List<SourceCodeChange> failureInducingChanges = new ArrayList<>();
		try {
			ICompilationUnit sourceCU = JavaModelHelper.getCompilationUnit(VERSION_BEFORE_REGRESSION, LOCALIZATION_SOURCE);
			ICompilationUnit regressionCU = JavaModelHelper.getCompilationUnit(VERSION_WITH_REGRESSION, LOCALIZATION_SOURCE);

			List<SourceCodeChange> allChanges = extractSourceCodeChanges(sourceCU, regressionCU);
			List<SourceCodeChange> filteredChanges = excludeSafeChanges(allChanges);
						
			JUnitTestHarness testHarness = new JUnitTestHarness(sourceCU);
			failureInducingChanges = DeltaDebug.ddmin(filteredChanges, testHarness);
			System.out.println(failureInducingChanges);
			
			applyFailureInducingChanges(sourceCU, failureInducingChanges);
			highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
		
		displayDoneDialog(event, failureInducingChanges);
		return null;
	}

	private List<SourceCodeChange> extractSourceCodeChanges(ICompilationUnit originalCU, ICompilationUnit regressionCU) throws JavaModelException {
		File left = JavaModelHelper.getFile(originalCU);
		File right = JavaModelHelper.getFile(regressionCU);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		return distiller.getSourceCodeChanges();
	}
	
	private List<SourceCodeChange> excludeSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE)
				.collect(Collectors.toList());
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
	
	private void displayDoneDialog(ExecutionEvent event, List<SourceCodeChange> failureInducingChanges) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "RegressionFinder", getStringRepresentation(failureInducingChanges));
	}

	private String getStringRepresentation(List<SourceCodeChange> failureInducingChanges) {
		StringBuilder builder = new StringBuilder();
		builder.append("Regression is caused by the following changes:\r\n\r\n");
		
		for (SourceCodeChange change : failureInducingChanges) {
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
