package regressionfinder.core;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import htmlflow.HtmlView;
import htmlflow.elements.HtmlDiv;
import htmlflow.elements.HtmlTable;
import htmlflow.elements.HtmlTr;
import regressionfinder.manipulation.FileManipulator;
import regressionfinder.model.AffectedFile;

@Component
public class ResultViewer {
	
	private static final String PAGE_TITLE = "Failure inducing changes";
	private static final String SYNTAXHIGHLIGHTER_JS = "syntaxhighlighter.js";
	private static final String BOOTSTRAP_CSS = "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css";
	private static final String THEME_CSS = "theme.css";
	private static final String RESULT_HTML = "results.html";
	private static final String RESULTS_HEADER = "Results";
	private static final String TEST_HEADER_FORMAT = "%s.%s()";

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
	    
	@SuppressWarnings("unchecked")
	private HtmlView<List<AffectedFile>> fileView(){
        HtmlView<List<AffectedFile>> fileView = new HtmlView<>();
        fileView
                .head()
                .title(PAGE_TITLE)
                .scriptLink(SYNTAXHIGHLIGHTER_JS)
                .linkCss(BOOTSTRAP_CSS)
                .linkCss(THEME_CSS);
        HtmlDiv<List<AffectedFile>> div = fileView
        		.body().classAttr("container")
        		.heading(1, RESULTS_HEADER)
        		.heading(2, String.format(TEST_HEADER_FORMAT, evaluationContext.getTestClassName(), evaluationContext.getTestMethodName()))
        		.div();
        
        HtmlTable<List<AffectedFile>> resultsTable = div.table().classAttr("table");
        HtmlTr<List<AffectedFile>> header = resultsTable.tr();
        header.th().text("Reference version");
        header.th().text("Faulty version");	
        resultsTable
        		.trFromIterable(this::referenceVersion, this::highlightedFaultyVersion);       
        
        div.table().classAttr("table")
        		.trFromIterable(AffectedFile::toString);
        
        // TODO: show only affected lines +- 10 lines
        
        return fileView;
    }
	
	private String referenceVersion(AffectedFile file) {
		StringBuilder result = new StringBuilder();
		result.append("<pre class=\"brush: java;\">");
		result.append(file.readSourceCode(evaluationContext.getReferenceProject()));
		result.append("</pre>");
		return result.toString();
	}

	private String highlightedFaultyVersion(AffectedFile file) {
		StringBuilder result = new StringBuilder();
		String sourceCode = file.readSourceCode(evaluationContext.getFaultyProject());
		result.append(String.format("<pre class=\"brush: java; highlight: %s\">", getLineNumbers(file, sourceCode)));
		result.append(sourceCode);
		result.append("</pre>");
		return result.toString();
	}

	private List<Integer> getLineNumbers(AffectedFile file, String sourceCode) {
		List<SourceCodeChange> remainingChanges = new ArrayList<>(file.getFailureInducingChanges());
		List<Integer> lines = new ArrayList<>();
		
		Scanner scanner = new Scanner(sourceCode);
		int line = 0;
		while (scanner.hasNextLine()) {
			line++;
			String nextLine = scanner.nextLine();
			
			for (SourceCodeChange change : remainingChanges) {
				String changeContent = FileManipulator.normalizeEntityValue(change.getChangedEntity().getContent());
				if (nextLine.contains(changeContent)) {
					lines.add(line);
					remainingChanges.remove(change);
					break;
				}	
			}
		}
		scanner.close();
		
		return lines;
	}	
}
