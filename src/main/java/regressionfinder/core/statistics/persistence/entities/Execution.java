package regressionfinder.core.statistics.persistence.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Execution {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	@Column(unique = true, nullable = false)
	private String executionId;
	
	@Column(updatable = false)
	private Timestamp startTime = Timestamp.valueOf(LocalDateTime.now());
	
	private Integer preparationPhaseDuration;
	
	private Integer changeDistillingPhaseDuration;
	
	private Integer deltaDebuggingPhaseDuration;
	
	private Integer totalExecutionDuration;

	
	protected Execution() {
		super();
	}
	
	public Execution(String executionId) {
		this.executionId = executionId;
	}
	
	public Long getId() {
		return id;
	}
	
	public String getExecutionId() {
		return executionId;
	}

	public void setExecutionId(String executionId) {
		this.executionId = executionId;
	}

	public Integer getPreparationPhaseDuration() {
		return preparationPhaseDuration;
	}

	public void setPreparationPhaseDuration(Integer preparationPhaseDuration) {
		this.preparationPhaseDuration = preparationPhaseDuration;
	}

	public Integer getChangeDistillingPhaseDuration() {
		return changeDistillingPhaseDuration;
	}

	public void setChangeDistillingPhaseDuration(Integer changeDistillingPhaseDuration) {
		this.changeDistillingPhaseDuration = changeDistillingPhaseDuration;
	}

	public Integer getDeltaDebuggingPhaseDuration() {
		return deltaDebuggingPhaseDuration;
	}

	public void setDeltaDebuggingPhaseDuration(Integer deltaDebuggingPhaseDuration) {
		this.deltaDebuggingPhaseDuration = deltaDebuggingPhaseDuration;
	}

	public Integer getTotalExecutionDuration() {
		return totalExecutionDuration;
	}

	public void setTotalExecutionDuration(Integer totalExecutionDuration) {
		this.totalExecutionDuration = totalExecutionDuration;
	}
	
}
