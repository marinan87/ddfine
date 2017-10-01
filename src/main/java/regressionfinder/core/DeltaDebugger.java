package regressionfinder.core;

import static java.util.stream.Collectors.toList;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.statistics.LogDuration;
import regressionfinder.core.statistics.StatisticsTracker;
import regressionfinder.model.AffectedUnit;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.TestOutcome;

@Component
public class DeltaDebugger extends JUnitTester {
	
	private static PrintStream DEFAULT_PRINT_STREAM = System.out;
	private ByteArrayOutputStream deltaDebuggerOutputStream;
	
	
	@Autowired
	private StatisticsTracker statisticsTracker;
	
	@Autowired
	private DeltaDebuggerWorker deltaDebuggerWorker;


	@LogDuration("Delta debugging phase completed.")
	public List<AffectedUnit> deltaDebug(List<MinimalApplicableChange> filteredChanges) {
		DeltaSet completeDeltaSet = new DeltaSet();
		completeDeltaSet.addAll(filteredChanges);
		statisticsTracker.logDeltaDebuggingChunks(filteredChanges);
		
		redirectSystemOutput();
				
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> result = (List<MinimalApplicableChange>) new DD(this).ddMin(completeDeltaSet).stream().collect(Collectors.toList());
		
		restoreSystemOutput();
		return AffectedUnit.fromListOfMinimalChanges(result);
	}
	
	private void redirectSystemOutput() {
		deltaDebuggerOutputStream = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(deltaDebuggerOutputStream);
	    System.setOut(ps);
	}
	
	private void restoreSystemOutput() {
	    System.out.flush();
	    System.setOut(DEFAULT_PRINT_STREAM);		
	}

	@Override
	public int test(DeltaSet set) {
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> selectedChangeSet = (List<MinimalApplicableChange>) set.stream().collect(toList());
		List<AffectedUnit> affectedUnits = AffectedUnit.fromListOfMinimalChanges(selectedChangeSet);
		
		int testOutcome = runNextTrial(affectedUnits);
		statisticsTracker.registerNextTrial(extractOutputFromStream(), selectedChangeSet.size(), TestOutcome.fromNumericCode(testOutcome));
		return testOutcome;
	}
	
	private String extractOutputFromStream() {
		String result = deltaDebuggerOutputStream.toString();
		String lookupCalledString = "Lookup called: ";
		result = result.substring(result.lastIndexOf(lookupCalledString) + lookupCalledString.length());
		deltaDebuggerOutputStream.reset();
		return result;
	}

	private int runNextTrial(List<AffectedUnit> affectedUnits) {
		deltaDebuggerWorker.prepareWorkingAreaForNextTrial(affectedUnits);
		deltaDebuggerWorker.recompileWorkingArea();
		int testOutcome = deltaDebuggerWorker.runFaultyTest();
		deltaDebuggerWorker.restoreWorkingArea(affectedUnits);
		return testOutcome;
	}
}
