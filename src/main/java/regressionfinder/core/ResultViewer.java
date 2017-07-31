package regressionfinder.core;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import htmlflow.HtmlView;
import regressionfinder.model.AffectedFile;

@Component
public class ResultViewer {
	
	private static final String PAGE_TITLE = "Failure inducing changes";
	private static final String BOOTSTRAP_CSS = "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css";
	private static final String RESULT_HTML = "results.html";
	private static final String RESULTS_HEADER = "Results";

	@Autowired
	private EvaluationContext evaluationContext;

	public void showResult(List<AffectedFile> failureRelevantFiles) {		
		HtmlView<List<AffectedFile>> fileView = fileView();
		try(PrintStream out = new PrintStream(new FileOutputStream(RESULT_HTML))) {
            fileView.setPrintStream(out).write(failureRelevantFiles);
            Desktop.getDesktop().browse(URI.create(RESULT_HTML));
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }				
	}
	    
    private HtmlView<List<AffectedFile>> fileView(){
        HtmlView<List<AffectedFile>> fileView = new HtmlView<>();
        fileView
                .head()
                .title(PAGE_TITLE)
                .linkCss(BOOTSTRAP_CSS);
        fileView
        		.body().classAttr("container")
        		.heading(1, RESULTS_HEADER)
        		.div()
                .table().classAttr("table")
                .trFromIterable(AffectedFile::getPath, AffectedFile::getChanges, file -> "<pre>".concat(file.readSourceCode(evaluationContext.getFaultyProject())).concat("</pre>"));
        
        // TODO: syntax highlighter
        // TODO: line numbers 
        // TODO: highlight failure inducing change
        // TODO: show only affected lines +- 10 lines
        
        return fileView;
    }

	 
//	private void highlightFailureInducingChangesInEditor(ICompilationUnit regressionCU, SourceCodeChange[] failureInducingChanges) throws Exception {		
//		ITextEditor textEditor = JavaModelHelper.openTextEditor(regressionCU);
//		
//		// Seems that there is no way to highlight all changes at once in Eclipse. 
//		// Currently only highlighting the first change.
//		SourceCodeChange firstChange = failureInducingChanges[0];
//		int startPosition = firstChange.getChangedEntity().getStartPosition();
//		int length = firstChange.getChangedEntity().getEndPosition() - startPosition;
//		textEditor.setHighlightRange(startPosition, length, false);
//		textEditor.selectAndReveal(startPosition, length);
//	}

//	private String getStringRepresentation(SourceCodeChange[] failureInducingChanges) {
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
