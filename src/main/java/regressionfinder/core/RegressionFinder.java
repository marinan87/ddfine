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
	private ResultViewer resultViewer;

	public void run() {
		// TODO: execute with first real example
		
		/*
		 * TODO: Multiple asserts to check that evaluation context is proper for running delta debugger
		- assert test exists in both version
		- assert test itself unchanged - otherwise not supported
		- assert contains only supported changes (changedistiller)
		- assert both versions compile
		*/
		
		// TODO: implement support of multimodule Maven projects
		
		SourceTreeDifferencer treeDifferencer = new SourceTreeDifferencer(evaluationContext.getReferenceProject(), evaluationContext.getFaultyProject());
		List<MinimalApplicableChange> filteredChanges = treeDifferencer.distillChanges();
		List<AffectedEntity> failureRelevantFiles = deltaDebug(filteredChanges);
		resultViewer.showResult(failureRelevantFiles);
		
		System.out.println("Total number of trials was: " + evaluationContext.getNumberOfTrials());
	}

	public List<AffectedEntity> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(evaluationContext).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		return AffectedEntity.fromListOfMinimalChanges(result);
	}
}
