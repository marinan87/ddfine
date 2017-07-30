package regressionfinder.core;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import htmlflow.HtmlView;
import htmlflow.elements.HtmlTable;
import regressionfinder.manipulation.FileSourceCodeChange;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;	
	
	@Autowired
	private SourceTreeDifferencer sourceTreeDifferencer;

	public void run() {
		// TODO: Next round: source code tree changes, fileops + sourcecodeops
		// added, removed files? fileops
		// modified files - sourcecodeops
		// added, removed dirs? fileops
		// folders with same name analyzed recursively. Repeat in each dir. 
		// git diff?
		
		List<FileSourceCodeChange> filteredChanges = sourceTreeDifferencer.distillChanges();
		Set<Map.Entry<Path, List<SourceCodeChange>>> mapOfFailureInducingChanges = runDeltaDebugging(filteredChanges);
		showResult(mapOfFailureInducingChanges);
//		highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
//		displayDoneDialog(event, failureInducingChanges);
	}

	public Set<Map.Entry<Path, List<SourceCodeChange>>> runDeltaDebugging(List<FileSourceCodeChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		FileSourceCodeChange[] resultArray = (FileSourceCodeChange[]) new DD(evaluationContext).ddMin(completeDeltaSet).toArray(new FileSourceCodeChange[0]);
		
		return FileSourceCodeChange.getMapOfChanges(Arrays.asList(resultArray)).entrySet();
	}
	
	public void showResult(Set<Map.Entry<Path, List<SourceCodeChange>>> failureInducingChanges) {		
		HtmlView<Set<Map.Entry<Path, List<SourceCodeChange>>>> fileView = fileView();
		try(PrintStream out = new PrintStream(new FileOutputStream("test.html"))) {
            fileView.setPrintStream(out).write(failureInducingChanges);
            Desktop.getDesktop().browse(URI.create("test.html"));
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
		
		// TODO: URI.create in other places, too.
//		SourceCodeManipulator.copyAndModifyLocalizationSource(pathToReferenceVersion, failureInducingChanges);
	}
	    
    private HtmlView<Set<Map.Entry<Path, List<SourceCodeChange>>>> fileView(){
        HtmlView<Set<Map.Entry<Path, List<SourceCodeChange>>>> fileView = new HtmlView<>();
        fileView
                .head()
                .title("Failure inducing changes")
                .linkCss("https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css");
        HtmlTable<Set<Map.Entry<Path, List<SourceCodeChange>>>> outerTable = fileView
        		.body().classAttr("container")
        		.heading(1, "Results")
        		.div()
                .table().classAttr("table");
        
        HtmlTable<List<SourceCodeChange>> nestedTable = new HtmlTable<>();
        outerTable
        		.trFromIterable(Map.Entry<Path, List<SourceCodeChange>>::getKey)
        		.tr().td().addChild(nestedTable);
        nestedTable    
                .trFromIterable(Map.Entry<Path, List<SourceCodeChange>>::getValue);
        return fileView;
    }

	 
//	private void highlightFailureInducingChangesInEditor(ICompilationUnit cu, SourceCodeChange[] failureInducingChanges) throws Exception {		
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
