package regressionfinder.handlers;

import org.eclipse.jdt.core.ICompilationUnit;

public class EvaluationTask {

	private final ICompilationUnit sourceCU;
	private final ICompilationUnit regressionCU;
	private final String testClassName;
	private final String testMethodName;
	
	public EvaluationTask(ICompilationUnit sourceCU, ICompilationUnit regressionCU, String testClassName, String testMethodName) {
		this.sourceCU = sourceCU;
		this.regressionCU = regressionCU;
		this.testClassName = testClassName;
		this.testMethodName = testMethodName;
	}
	
	public ICompilationUnit getSourceCU() {
		return sourceCU;
	}
	
	public ICompilationUnit getRegressionCU() {
		return regressionCU;
	}
	
	public String getTestClassName() {
		return testClassName;
	}
	
	public String getTestMethodName() {
		return testMethodName;
	}
}
