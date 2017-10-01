package regressionfinder.core;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.manipulation.PrepareWorkingAreaVisitor;
import regressionfinder.core.manipulation.RestoreWorkingAreaVisitor;
import regressionfinder.core.statistics.LogDuration;
import regressionfinder.model.AffectedUnit;
import regressionfinder.model.MinimalApplicableChange;

@Component
public class DeltaDebugger extends JUnitTester {
	
	@Autowired
	private EvaluationContext evaluationContext;
	
	@Autowired
	private PrepareWorkingAreaVisitor prepareWorkingAreaVisitor;
	
	@Autowired
	private RestoreWorkingAreaVisitor restoreWorkingAreaVisitor;
	
	@Autowired
	private MavenCompiler mavenCompiler;
	
	@Autowired
	private ReflectionalTestMethodRunner testMethodRunner;


	@LogDuration("Delta debugging phase completed.")
	public List<AffectedUnit> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(this).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		return AffectedUnit.fromListOfMinimalChanges(result);
	}
	
	@Override
	public int test(DeltaSet set) {
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> selectedChangeSet = (List<MinimalApplicableChange>) set.stream().collect(toList());
		List<AffectedUnit> affectedUnits = AffectedUnit.fromListOfMinimalChanges(selectedChangeSet);
		return runNextTrial(affectedUnits);
	}

	private int runNextTrial(List<AffectedUnit> affectedUnits) {
		prepareWorkingAreaForNextTrial(affectedUnits);
		int testOutcome = (int) testMethodRunner.runFaultyTest();
		restoreWorkingArea(affectedUnits);
		return testOutcome;
	}

	private void prepareWorkingAreaForNextTrial(List<AffectedUnit> affectedUnits) {
		affectedUnits.forEach(unit -> unit.manipulate(prepareWorkingAreaVisitor));	
		mavenCompiler.triggerSimpleCompilation(evaluationContext.getWorkingAreaProject());
	}

	private void restoreWorkingArea(List<AffectedUnit> affectedUnits) {
		affectedUnits.forEach(unit -> unit.manipulate(restoreWorkingAreaVisitor));
	}
}
