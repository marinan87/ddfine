package regressionfinder.core;

public class EvaluationContext {

	private final String referenceVersion;
	private final String faultyVersion;
	private final String testClassName;
	private final String testMethodName;
	
	public EvaluationContext(String referenceVersion, String faultyVersion, String testClassName, String testMethodName) {
		this.referenceVersion = referenceVersion;
		this.faultyVersion = faultyVersion;
		this.testClassName = testClassName;
		this.testMethodName = testMethodName;
	}
	
	public String getReferenceVersion() {
		return referenceVersion;
	}
	
	public String getFaultyVersion() {
		return faultyVersion;
	}

	public String getTestClassName() {
		return testClassName;
	}
	
	public String getTestMethodName() {
		return testMethodName;
	}
}
