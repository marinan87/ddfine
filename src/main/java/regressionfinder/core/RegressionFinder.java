package regressionfinder.core;

import java.util.Arrays;
import java.util.List;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.model.AffectedFile;
import regressionfinder.model.FileSourceCodeChange;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;	
	
	@Autowired
	private SourceTreeDifferencer sourceTreeDifferencer;
	
	@Autowired
	private ResultViewer resultViewer;

	public void run() {
		// TODO: Next round: source code tree changes, fileops + sourcecodeops
		// added, removed files? fileops
		// modified files - sourcecodeops
		// added, removed dirs? fileops
		// folders with same name analyzed recursively. Repeat in each dir. 
		// git diff?
		// TODO: execute with real example
		
		List<FileSourceCodeChange> filteredChanges = sourceTreeDifferencer.distillChanges();
		List<AffectedFile> failureRelevantFiles = deltaDebug(filteredChanges);
		resultViewer.showResult(failureRelevantFiles);
	}

	public List<AffectedFile> deltaDebug(List<FileSourceCodeChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		FileSourceCodeChange[] resultArray = (FileSourceCodeChange[]) new DD(evaluationContext).ddMin(completeDeltaSet).toArray(new FileSourceCodeChange[0]);
		
		return AffectedFile.fromListOfChanges(Arrays.asList(resultArray));
	}
}
