package regressionfinder.runner.datamanager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import regressionfinder.core.statistics.persistence.entities.Execution;
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
}
