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
	private SourceTreeDifferencer treeDifferencer;
	
	@Autowired
	private ReflectionalTestMethodInvoker testInvoker;
			
	@Autowired
	private ResultViewer resultViewer;
	
	@Autowired
	private StatisticsTracker statisticsTracker;

	
	public void run() {
		statisticsTracker.markPreparationPhaseFinished();
		List<MinimalApplicableChange> filteredChanges = treeDifferencer.distillChanges();
		statisticsTracker.markDistillingPhaseFinished();
		List<AffectedEntity> failureRelevantFiles = deltaDebug(filteredChanges);
		statisticsTracker.markDeltaDebuggingPhaseFinished();
		resultViewer.showResult(failureRelevantFiles);
	}

	private List<AffectedEntity> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		testInvoker.initializeOnce();

		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(testInvoker).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		return AffectedEntity.fromListOfMinimalChanges(result);
	}
}
