package regressionfinder.core;

import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.renderer.ResultViewer;
import regressionfinder.model.AffectedEntity;
import regressionfinder.model.MinimalApplicableChange;

@Component
public class RegressionFinder {

	@Autowired
	private EvaluationContext evaluationContext;
		
	@Autowired
	private SourceTreeDifferencer treeDifferencer;
			
	@Autowired
	private ResultViewer resultViewer;

	
	public void run() {				
		List<MinimalApplicableChange> filteredChanges = treeDifferencer.distillChanges();
		List<AffectedEntity> failureRelevantFiles = deltaDebug(filteredChanges);
		resultViewer.showResult(failureRelevantFiles);		
	}

	public List<AffectedEntity> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(evaluationContext).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		return AffectedEntity.fromListOfMinimalChanges(result);
	}
}
