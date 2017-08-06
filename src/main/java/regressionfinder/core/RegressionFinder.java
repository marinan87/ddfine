package regressionfinder.core;

import java.util.Arrays;
import java.util.List;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.renderer.ResultViewer;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.FileSourceCodeChange;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;	
		
	@Autowired
	private ResultViewer resultViewer;

	public void run() {
		// TODO: execute with first real example
		
		SourceTreeDifferencer treeDifferencer = new SourceTreeDifferencer(evaluationContext.getReferenceProject(), evaluationContext.getFaultyProject());
		List<FileSourceCodeChange> filteredChanges = treeDifferencer.distillChanges();
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
