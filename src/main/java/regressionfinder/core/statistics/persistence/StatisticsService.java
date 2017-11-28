package regressionfinder.core.statistics.persistence;


import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import regressionfinder.core.EvaluationContext;
import regressionfinder.core.statistics.ExecutionPhase;
import regressionfinder.core.statistics.persistence.entities.DistilledChange;
import regressionfinder.core.statistics.persistence.entities.DistilledSourceCodeChange;
import regressionfinder.core.statistics.persistence.entities.Execution;
import regressionfinder.core.statistics.persistence.entities.ImmutableMetadata;
import regressionfinder.core.statistics.persistence.entities.Trial;
import regressionfinder.core.statistics.persistence.repository.DistilledChangeRepository;
import regressionfinder.core.statistics.persistence.repository.ExecutionRepository;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;

@Service
@Transactional(readOnly = true)
public class StatisticsService {
	
	@Autowired
	private EvaluationContext evaluationContext;
			
	@Autowired
	private ExecutionRepository executionRepository;
	
	@Autowired
	private DistilledChangeRepository distilledChangeRepository;

	
	@Transactional
	public void deleteOldExecutionIfExists() {
		Execution previousExecution = findCurrentExecution();
		if (previousExecution != null) {
			executionRepository.delete(previousExecution);
		}
	}
	
	@Transactional
	public void createNewExecution() {
		ImmutableMetadata immutableMetadata = new ImmutableMetadata(
				evaluationContext.getTestClassName(), 
				evaluationContext.getTestMethodName(), 
				ExceptionUtils.getStackTrace(evaluationContext.getThrowable()));
		Execution execution = new Execution(evaluationContext.getExecutionId(), immutableMetadata);

		executionRepository.save(execution);
	}

	@Transactional
	public void storePhaseExecutionTime(ExecutionPhase phase, long duration) {
		findCurrentExecution().setPhaseExecutionTime(phase, duration);
	}
	
	@Transactional
	public void storeTotalDuration(long duration) {
		findCurrentExecution().setTotalExecutionDuration(duration);
	}
	
	@Transactional
	public void storeDeltaDebuggingTrials(int numTrials) {
		findCurrentExecution().setDdTrials(numTrials);
	}
	
	@Transactional
	public void storeSourceCodeChanges(int numChanges) {
		findCurrentExecution().setDetectedSourceCodeChanges(numChanges);
	}
	
	@Transactional
	public void storeStructuralChanges(int numChanges) {
		findCurrentExecution().setDetectedStructuralChanges(numChanges);
	}
	
	@Transactional
	public void storeDistilledChanges(List<MinimalApplicableChange> chunks) {
		int chunkNumber = 0;
		for (MinimalApplicableChange chunk : chunks) {
			DistilledChange distilledChange;
			if (chunk instanceof MinimalChangeInFile) {
				distilledChange = getDistilledChange((MinimalChangeInFile) chunk, chunkNumber);
			} else {
				distilledChange = getDistilledChange(chunk, chunkNumber);
			}
			
			findCurrentExecution().addDistilledChange(distilledChange);

			chunkNumber++;
		}
	}
	
	@Transactional
	public void storeTrial(int trialNumber, String setContent, int outcome, long[] lastTrialMetrics) {
		Trial trial = new Trial();
		trial.setTrialNumber(trialNumber);
		trial.setOutcome(outcome);
		trial.setPrepareDuration(lastTrialMetrics[0]);
		trial.setRecompileDuration(lastTrialMetrics[1]);
		trial.setRunDuration(lastTrialMetrics[2]);
		trial.setRestoreDuration(lastTrialMetrics[3]);
		
		setContent = setContent.trim();
		setContent = StringUtils.trimLeadingCharacter(setContent, '[');
		setContent = StringUtils.trimTrailingCharacter(setContent, ']');
		for (String str : setContent.split(", ")) {
			if (str.length() > 0) {
				int chunkNumber = Integer.valueOf(str);
				DistilledChange change = distilledChangeRepository.findByExecutionIdentifierAndChunkNumber(evaluationContext.getExecutionId(), chunkNumber);
				trial.addDistilledChange(change);
			}
		}

		findCurrentExecution().addTrial(trial);
	}
	
	private DistilledChange getDistilledChange(MinimalChangeInFile chunk, int chunkNumber) {
		DistilledSourceCodeChange distilledChange = new DistilledSourceCodeChange();
		
		distilledChange.setChunkNumber(chunkNumber);
		distilledChange.setChangePath(chunk.getPathToFile().toString());
		distilledChange.setChangeType(chunk.getChangeTypeString());
		distilledChange.setLocation(chunk.getPatch().start1);
		
		return distilledChange;
	}
	
	private DistilledChange getDistilledChange(MinimalApplicableChange chunk, int chunkNumber) {
		DistilledChange distilledChange = new DistilledChange();
		
		distilledChange.setChunkNumber(chunkNumber);
		distilledChange.setChangePath(chunk.getPathToFile().toString());
		distilledChange.setChangeType(chunk.getChangeTypeString());
		
		return distilledChange;
	}
	
	@Transactional 
	public void storeNumberOfLinesToInspect(int number) {
		findCurrentExecution().setNumberOfLinesToInspect(number);
	}
	
	public Execution findCurrentExecution() {
		return executionRepository.findByExecutionIdentifier(evaluationContext.getExecutionId());
	}
}
