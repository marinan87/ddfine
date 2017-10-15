package regressionfinder.core.statistics.persistence.entities;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import regressionfinder.core.statistics.ExecutionPhase;

@Entity
public class Execution {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	@Column(unique = true, nullable = false)
	private String executionIdentifier;
	
	@Column(updatable = false)
	private Timestamp startTime = Timestamp.valueOf(LocalDateTime.now());
	
	private Long preparationPhaseDuration;
	
	private Long changeDistillingPhaseDuration;
	
	private Long deltaDebuggingPhaseDuration;
	
	private Long totalExecutionDuration;
	
	private Integer ddTrials;
	
	private Integer detectedStructuralChanges;
	
	private Integer detectedSourceCodeChanges;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "EXECUTION_ID")
	private List<DistilledChange> distilledChanges = new ArrayList<>();

	
	protected Execution() {
		super();
	}
	
	public Execution(String executionId) {
		this.executionIdentifier = executionId;
	}
	
	public Long getId() {
		return id;
	}
	
	public String getExecutionIdentifier() {
		return executionIdentifier;
	}

	public void setExecutionIdentifier(String executionId) {
		this.executionIdentifier = executionId;
	}

	public Long getPreparationPhaseDuration() {
		return preparationPhaseDuration;
	}

	public void setPreparationPhaseDuration(Long preparationPhaseDuration) {
		this.preparationPhaseDuration = preparationPhaseDuration;
	}

	public Long getChangeDistillingPhaseDuration() {
		return changeDistillingPhaseDuration;
	}

	public void setChangeDistillingPhaseDuration(Long changeDistillingPhaseDuration) {
		this.changeDistillingPhaseDuration = changeDistillingPhaseDuration;
	}

	public Long getDeltaDebuggingPhaseDuration() {
		return deltaDebuggingPhaseDuration;
	}

	public void setDeltaDebuggingPhaseDuration(Long deltaDebuggingPhaseDuration) {
		this.deltaDebuggingPhaseDuration = deltaDebuggingPhaseDuration;
	}

	public Long getTotalExecutionDuration() {
		return totalExecutionDuration;
	}

	public void setTotalExecutionDuration(Long totalExecutionDuration) {
		this.totalExecutionDuration = totalExecutionDuration;
	}
	
	public Integer getDdTrials() {
		return ddTrials;
	}

	public void setDdTrials(Integer ddTrials) {
		this.ddTrials = ddTrials;
	}
	
	public Integer getDetectedStructuralChanges() {
		return detectedStructuralChanges;
	}

	public void setDetectedStructuralChanges(Integer structuralChanges) {
		this.detectedStructuralChanges = structuralChanges;
	}

	public Integer getDetectedSourceCodeChanges() {
		return detectedSourceCodeChanges;
	}

	public void setDetectedSourceCodeChanges(Integer sourceCodeChanges) {
		this.detectedSourceCodeChanges = sourceCodeChanges;
	}

	public void addDistilledChange(DistilledChange distilledChange) {
		this.distilledChanges.add(distilledChange);
	}
	
	public void setPhaseExecutionTime(ExecutionPhase phase, long duration) {
		switch (phase) {
			case PREPARATION:
				this.preparationPhaseDuration = duration;
				break;
			case CHANGE_DISTILLING:
				this.changeDistillingPhaseDuration = duration;
				break;
			case DELTA_DEBUGGING:
				this.deltaDebuggingPhaseDuration = duration;
				break;
		}
	}
}
