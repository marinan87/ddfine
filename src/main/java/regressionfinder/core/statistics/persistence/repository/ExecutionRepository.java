package regressionfinder.core.statistics.persistence.repository;

import org.springframework.data.repository.CrudRepository;

import regressionfinder.core.statistics.persistence.entities.Execution;

public interface ExecutionRepository extends CrudRepository<Execution, Long> {
	
    Execution findByExecutionId(String executionId);
}
