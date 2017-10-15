package regressionfinder.core.statistics.persistence.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
		name = "TRIAL",
		uniqueConstraints = @UniqueConstraint(columnNames = {"EXECUTION_ID", "TRIAL_NUMBER"})
)
public class Trial {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	@Column(name = "TRIAL_NUMBER", nullable = false)
	private Integer trialNumber;
		
	private Integer outcome;
	
	private Long prepareDuration;
	
	private Long recompileDuration;
	
	private Long runDuration;
	
	private Long restoreDuration;

	@ManyToMany
	@JoinTable(
			name = "TRIAL_CHANGE",
			joinColumns = @JoinColumn(name = "TRIAL_ID"),
			inverseJoinColumns = @JoinColumn(name = "DISTILLED_CHANGE_ID")
	)
	private List<DistilledChange> distilledChanges = new ArrayList<>();
	
	
	public Long getId() {
		return id;
	}
	
	public Integer getTrialNumber() {
		return trialNumber;
	}

	public void setTrialNumber(Integer trialNumber) {
		this.trialNumber = trialNumber;
	}

	public Integer getOutcome() {
		return outcome;
	}

	public void setOutcome(Integer outcome) {
		this.outcome = outcome;
	}

	public Long getPrepareDuration() {
		return prepareDuration;
	}

	public void setPrepareDuration(Long prepareDuration) {
		this.prepareDuration = prepareDuration;
	}

	public Long getRecompileDuration() {
		return recompileDuration;
	}

	public void setRecompileDuration(Long recompileDuration) {
		this.recompileDuration = recompileDuration;
	}

	public Long getRunDuration() {
		return runDuration;
	}

	public void setRunDuration(Long runDuration) {
		this.runDuration = runDuration;
	}

	public Long getRestoreDuration() {
		return restoreDuration;
	}

	public void setRestoreDuration(Long restoreDuration) {
		this.restoreDuration = restoreDuration;
	}
	
	public void addDistilledChange(DistilledChange change) {
		this.distilledChanges.add(change);
	}
}
