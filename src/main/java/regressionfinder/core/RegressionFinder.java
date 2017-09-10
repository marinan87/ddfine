package regressionfinder.core;

import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.renderer.ResultViewer;
import regressionfinder.model.AffectedEntity;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;	
		
	@Autowired
	private ResultViewer resultViewer;

	public void run() {				
		SourceTreeDifferencer treeDifferencer = new SourceTreeDifferencer(evaluationContext.getReferenceProject(), evaluationContext.getFaultyProject());
		List<MinimalApplicableChange> filteredChanges = treeDifferencer.distillChanges();
		assertContainsOnlySupportedChanges(filteredChanges);

		System.out.println("Number of applicable changes to try: " + filteredChanges.size());
		List<AffectedEntity> failureRelevantFiles = deltaDebug(filteredChanges);
		resultViewer.showResult(failureRelevantFiles);
		
		System.out.println("Total number of trials was: " + evaluationContext.getNumberOfTrials());
	}

	private void assertContainsOnlySupportedChanges(List<MinimalApplicableChange> filteredChanges) {
		List<MinimalChangeInFile> unsupportedChanges = filteredChanges.stream()
			.filter(change -> (change instanceof MinimalChangeInFile))
			.map(change -> (MinimalChangeInFile) change)
			.filter(changeInFile -> {
				SourceCodeChange sourceCodeChange = changeInFile.getSourceCodeChange();
				return !SupportedModificationsRegistry.supportsModification(sourceCodeChange.getClass(), sourceCodeChange.getChangeType());
			})
			.collect(Collectors.toList());
			
		Preconditions.checkState(unsupportedChanges.isEmpty(), 
				"Cannot continue. The following changes are not supported by the current prototype implementation:\r\n" 
				+ unsupportedChanges);
	}

	public List<AffectedEntity> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(evaluationContext).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		return AffectedEntity.fromListOfMinimalChanges(result);
	}
}
