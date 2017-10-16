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
import javax.persistence.OneToOne;

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
	
	private Integer numberOfLinesToInspect;
	
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "EXECUTION_ID")
	private List<DistilledChange> distilledChanges = new ArrayList<>();

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "EXECUTION_ID")
	private List<Trial> trials = new ArrayList<>();
	
	@OneToOne(optional = false, cascade = CascadeType.ALL)
	private ExecutionMetadata executionMetadata;
	
	
	protected Execution() {
		this(null, null);
	}
	
	public Execution(String executionId, ImmutableMetadata immutableMetadata) {
		this.executionIdentifier = executionId;
		this.executionMetadata = new ExecutionMetadata(immutableMetadata);
	}
	
	public Long getId() {
		return id;
	}
	
	public Timestamp getStartTime() {
		return startTime;
	}
	
	public String getExecutionIdentifier() {
		return executionIdentifier;
	}

	public Long getPreparationPhaseDuration() {
		return preparationPhaseDuration;
	}

	public Long getChangeDistillingPhaseDuration() {
		return changeDistillingPhaseDuration;
	}

	public Long getDeltaDebuggingPhaseDuration() {
		return deltaDebuggingPhaseDuration;
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
	
	public Integer getNumberOfLinesToInspect() {
		return numberOfLinesToInspect;
	}
	
	public void setNumberOfLinesToInspect(Integer number) {
		this.numberOfLinesToInspect = number;
	}

	public void addDistilledChange(DistilledChange distilledChange) {
		this.distilledChanges.add(distilledChange);
	}
	
	public void addTrial(Trial trial) {
		this.trials.add(trial);
	}
	
	public ExecutionMetadata getExecutionMetadata() {
		return executionMetadata;
	}
	
	public boolean hasExecutionMetadata() {
		return executionMetadata.getResultSummary() != null;
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
