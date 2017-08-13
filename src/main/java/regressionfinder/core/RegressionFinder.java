package regressionfinder.core;

import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.renderer.ResultViewer;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;	
		
	@Autowired
	private ResultViewer resultViewer;

	public void run() {
		// TODO: execute with first real example
		
		SourceTreeDifferencer treeDifferencer = new SourceTreeDifferencer(evaluationContext.getReferenceProject(), evaluationContext.getFaultyProject());
		List<MinimalApplicableChange> filteredChanges = treeDifferencer.distillChanges();
		List<AffectedFile> failureRelevantFiles = deltaDebug(filteredChanges);
		resultViewer.showResult(failureRelevantFiles);
		
		System.out.println("Total number of trials was: " + evaluationContext.getNumberOfTrials());
	}

	public List<AffectedFile> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(evaluationContext).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		List<MinimalChangeInFile> changesInFile = result.stream()
				.filter(change -> change instanceof MinimalChangeInFile)
				.map(change -> (MinimalChangeInFile) change)
				.collect(Collectors.toList());
		return AffectedFile.fromListOfChanges(changesInFile);
	}
}
