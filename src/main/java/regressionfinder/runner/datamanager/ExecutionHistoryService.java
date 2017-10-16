package regressionfinder.runner.datamanager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import regressionfinder.core.statistics.persistence.entities.Execution;
import regressionfinder.core.statistics.persistence.entities.ExecutionMetadata;
import regressionfinder.core.statistics.persistence.repository.ExecutionRepository;

@Service
@Transactional(readOnly = true)
public class ExecutionHistoryService {

	@Autowired
	private ExecutionRepository executionRepository;
	
	
	public List<Execution> findAllExecutionsOrdered() {
		return executionRepository.findAllByOrderByStartTimeDesc();
	}
	
	public Execution findExecution(String executionId) {
		return executionRepository.findByExecutionIdentifier(executionId);
	}
	
	@Transactional
	public void updateExecutionMetadata(String executionId, ExecutionMetadata metadata) {
		Execution execution = findExecution(executionId);
		ExecutionMetadata persistedMetadata = execution.getExecutionMetadata();
		persistedMetadata.setReferenceSha(metadata.getReferenceSha());
		persistedMetadata.setFaultySha(metadata.getFaultySha());
		persistedMetadata.setFixedSha(metadata.getFixedSha());
		persistedMetadata.setCommitMessage(metadata.getCommitMessage());
		persistedMetadata.setResultSummary(metadata.getResultSummary());
		persistedMetadata.setResultComment(metadata.getResultComment());
	}
}
