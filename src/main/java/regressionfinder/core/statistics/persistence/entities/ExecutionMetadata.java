package regressionfinder.core.statistics.persistence.entities;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ExecutionMetadata {

	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
	
	@Embedded
	private ImmutableMetadata immutableMetadata;
	
	private String referenceSha;
	
	private String faultySha;
	
	private String fixedSha;
	
	private String commitMessage;
	
	private Boolean resultSummary;
	
	private String resultComment;
	
	
	protected ExecutionMetadata() {
		this(null);
	}
	
	public ExecutionMetadata(ImmutableMetadata immutableMetadata) {
		this.immutableMetadata = immutableMetadata;
	}
	
	public Long getId() {
		return id;
	}

	public String getReferenceSha() {
		return referenceSha;
	}

	public void setReferenceSha(String referenceSha) {
		this.referenceSha = referenceSha;
	}

	public String getFaultySha() {
		return faultySha;
	}

	public void setFaultySha(String faultySha) {
		this.faultySha = faultySha;
	}

	public String getFixedSha() {
		return fixedSha;
	}

	public void setFixedSha(String fixedSha) {
		this.fixedSha = fixedSha;
	}

	public String getCommitMessage() {
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	public Boolean getResultSummary() {
		return resultSummary;
	}

	public void setResultSummary(Boolean resultSummary) {
		this.resultSummary = resultSummary;
	}

	public String getResultComment() {
		return resultComment;
	}

	public void setResultComment(String resultComment) {
		this.resultComment = resultComment;
	}
	
	public String getFailedClassName() {
		return this.immutableMetadata.getFailedClassName();
	}
	
	public String getFailedMethodName() {
		return this.immutableMetadata.getFailedMethodName();
	}
}
