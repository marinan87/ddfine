package regressionfinder.core.renderer;

import java.awt.Desktop;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import htmlflow.HtmlView;
import htmlflow.elements.HtmlDiv;
import htmlflow.elements.HtmlTable;
import htmlflow.elements.HtmlTr;
import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedUnit;

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
	
	@Autowired
	private ReferenceRenderingVisitor referenceRenderingVisitor;
	
	@Autowired
	private FaultyRenderingVisitor faultyRenderingVisitor;

	public void showResult(List<AffectedUnit> failureRelevantUnits) {		
		HtmlView<List<AffectedUnit>> fileView = fileView();
		try(PrintStream out = new PrintStream(new FileOutputStream(RESULT_HTML))) {
            fileView.setPrintStream(out).write(failureRelevantUnits);
            Desktop.getDesktop().browse(URI.create(RESULT_HTML));
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }				
	}
	    
	@SuppressWarnings("unchecked")
	private HtmlView<List<AffectedUnit>> fileView(){
        HtmlView<List<AffectedUnit>> fileView = new HtmlView<>();
        fileView
                .head()
                .title(PAGE_TITLE)
                .scriptLink(SYNTAXHIGHLIGHTER_JS)
                .linkCss(BOOTSTRAP_CSS)
                .linkCss(THEME_CSS);
        HtmlDiv<List<AffectedUnit>> div = fileView
        		.body().classAttr("container")
        		.heading(1, RESULTS_HEADER)
        		.heading(2, String.format(TEST_HEADER_FORMAT, evaluationContext.getTestClassName(), evaluationContext.getTestMethodName()))
        		.div();
        
        HtmlTable<List<AffectedUnit>> resultsTable = div.table().classAttr("table");
        HtmlTr<List<AffectedUnit>> header = resultsTable.tr();
        header.th().text("Reference version");
        header.th().text("Faulty version");	
        resultsTable.trFromIterable(
        	(AffectedUnit unit) -> unit.render(referenceRenderingVisitor), 
        	(AffectedUnit unit) -> unit.render(faultyRenderingVisitor));       
        
        div.table().classAttr("table")
        		.trFromIterable(entity -> entity.toString());
        
        // TODO: show only affected lines +- 10 lines
        
        return fileView;
    }	
}
