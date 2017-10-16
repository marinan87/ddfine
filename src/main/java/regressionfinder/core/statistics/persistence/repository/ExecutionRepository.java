package regressionfinder.core.statistics.persistence.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import regressionfinder.core.statistics.persistence.entities.Execution;

public interface ExecutionRepository extends CrudRepository<Execution, Long> {
	
    Execution findByExecutionIdentifier(String executionId);
    
    List<Execution> findAllByOrderByStartTimeDesc();
}
