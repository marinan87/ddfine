package regressionfinder.core.statistics.persistence.entities;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ImmutableMetadata {

	private String failedClassName;
	
	private String failedMethodName;
	
	@Column(length = 5000, columnDefinition = "LONGVARCHAR")
	private String stackTrace;
	
	
	protected ImmutableMetadata() {
		this(null, null, null);
	}
	
	public ImmutableMetadata(String failedClassName, String failedMethodName, String stackTrace) {
		this.failedClassName = failedClassName;
		this.failedMethodName = failedMethodName;
		this.stackTrace = stackTrace;
	}
	
	public String getFailedClassName() {
		return failedClassName;
	}

	public String getFailedMethodName() {
		return failedMethodName;
	}

	public String getStackTrace() {
		return stackTrace;
	}
}
