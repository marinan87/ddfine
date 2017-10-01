package regressionfinder.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import regressionfinder.core.manipulation.PrepareWorkingAreaVisitor;
import regressionfinder.core.manipulation.RestoreWorkingAreaVisitor;
import regressionfinder.core.statistics.LogTrialDuration;
import regressionfinder.model.AffectedUnit;

@Service
public class DeltaDebuggerWorker {
	
	@Autowired
	private EvaluationContext evaluationContext;
		
	@Autowired
	private ReflectionalTestMethodRunner testMethodRunner;
	
	@Autowired
	private MavenCompiler mavenCompiler;
	
	@Autowired
	private PrepareWorkingAreaVisitor prepareWorkingAreaVisitor;
	
	@Autowired
	private RestoreWorkingAreaVisitor restoreWorkingAreaVisitor;
	
	
	@LogTrialDuration
	public void prepareWorkingAreaForNextTrial(List<AffectedUnit> affectedUnits) {
		affectedUnits.forEach(unit -> unit.manipulate(prepareWorkingAreaVisitor));	
	}
	
	@LogTrialDuration
	public void recompileWorkingArea() {
		mavenCompiler.triggerSimpleCompilation(evaluationContext.getWorkingAreaProject());
	}
	
	@LogTrialDuration
	public int runFaultyTest() {
		return (int) testMethodRunner.runFaultyTest();
	}

	@LogTrialDuration
	public void restoreWorkingArea(List<AffectedUnit> affectedUnits) {
		affectedUnits.forEach(unit -> unit.manipulate(restoreWorkingAreaVisitor));
	}
}
