package regressionfinder.core.statistics.persistence.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import regressionfinder.core.statistics.persistence.entities.DistilledChange;

public interface DistilledChangeRepository extends CrudRepository<DistilledChange, Long> {

	@Query("SELECT dc FROM Execution e JOIN e.distilledChanges dc WHERE e.executionIdentifier = :executionId AND dc.chunkNumber = :chunkNumber")
	DistilledChange findByExecutionIdentifierAndChunkNumber(@Param("executionId") String executionId, @Param("chunkNumber") int chunkNumber);
}
