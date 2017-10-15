package regressionfinder.core.statistics.persistence;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import regressionfinder.core.statistics.ExecutionPhase;
import regressionfinder.core.statistics.persistence.entities.DistilledChange;
import regressionfinder.core.statistics.persistence.entities.DistilledSourceCodeChange;
import regressionfinder.core.statistics.persistence.entities.Execution;
import regressionfinder.core.statistics.persistence.entities.Trial;
import regressionfinder.core.statistics.persistence.repository.DistilledChangeRepository;
import regressionfinder.core.statistics.persistence.repository.ExecutionRepository;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;

@Service
@Transactional(readOnly = true)
public class StatisticsService {
	
	@Autowired
	private ExecutionRepository executionRepository;
	
	@Autowired
	private DistilledChangeRepository distilledChangeRepository;

	
	@Transactional
	public void deleteOldExecutionIfExists(String executionId) {
		Execution previousExecution = findExecution(executionId);
		if (previousExecution != null) {
			executionRepository.delete(previousExecution);
		}
	}
	
	@Transactional
	public void createNewExecution(String executionId) {
		Execution execution = new Execution(executionId);
		executionRepository.save(execution);
	}

	@Transactional
	public void storePhaseExecutionTime(String executionId, ExecutionPhase phase, long duration) {
		findExecution(executionId).setPhaseExecutionTime(phase, duration);
	}
	
	@Transactional
	public void storeTotalDuration(String executionId, long duration) {
		findExecution(executionId).setTotalExecutionDuration(duration);
	}
	
	@Transactional
	public void storeDeltaDebuggingTrials(String executionId, int numTrials) {
		findExecution(executionId).setDdTrials(numTrials);
	}
	
	@Transactional
	public void storeSourceCodeChanges(String executionId, int numChanges) {
		findExecution(executionId).setDetectedSourceCodeChanges(numChanges);
	}
	
	@Transactional
	public void storeStructuralChanges(String executionId, int numChanges) {
		findExecution(executionId).setDetectedStructuralChanges(numChanges);
	}
	
	@Transactional
	public void storeDistilledChanges(String executionId, List<MinimalApplicableChange> chunks) {
		int chunkNumber = 0;
		for (MinimalApplicableChange chunk : chunks) {
			DistilledChange distilledChange;
			if (chunk instanceof MinimalChangeInFile) {
				distilledChange = getDistilledChange((MinimalChangeInFile) chunk, chunkNumber);
			} else {
				distilledChange = getDistilledChange(chunk, chunkNumber);
			}
			
			findExecution(executionId).addDistilledChange(distilledChange);

			chunkNumber++;
		}
	}
	
	@Transactional
	public void storeTrial(String executionId, int trialNumber, String setContent, int outcome, long[] lastTrialMetrics) {
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
				DistilledChange change = distilledChangeRepository.findByExecutionIdentifierAndChunkNumber(executionId, chunkNumber);
				trial.addDistilledChange(change);
			}
		}

		findExecution(executionId).addTrial(trial);
	}
	
	private DistilledChange getDistilledChange(MinimalChangeInFile chunk, int chunkNumber) {
		DistilledSourceCodeChange distilledChange = new DistilledSourceCodeChange();
		
		distilledChange.setChunkNumber(chunkNumber);
		distilledChange.setChangePath(chunk.getPathToFile().toString());
		distilledChange.setChangeType(chunk.getChangeTypeString());
		distilledChange.setLocation(chunk.getSourceCodeChange().getChangedEntity().getStartPosition());
		
		return distilledChange;
	}
	
	private DistilledChange getDistilledChange(MinimalApplicableChange chunk, int chunkNumber) {
		DistilledChange distilledChange = new DistilledChange();
		
		distilledChange.setChunkNumber(chunkNumber);
		distilledChange.setChangePath(chunk.getPathToFile().toString());
		distilledChange.setChangeType(chunk.getChangeTypeString());
		
		return distilledChange;
	}

	private Execution findExecution(String executionId) {
		return executionRepository.findByExecutionIdentifier(executionId);
	}
}
