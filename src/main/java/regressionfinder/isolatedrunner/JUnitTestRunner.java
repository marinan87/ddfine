package regressionfinder.isolatedrunner;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestFailure;
import junit.framework.TestResult;

public class JUnitTestRunner extends IsolatedClassLoaderAwareJUnitTestRunner {

	public JUnitTestRunner(String testClassName, String testMethodName) {
		super(testClassName, testMethodName);
	}

	public Throwable getOriginalException() {
		TestResult result = new TestResult();
		jUnitTestAdapter.run(result);
		
		List<TestFailure> allProblems = new ArrayList<>();
		Enumeration<TestFailure> failures = result.failures();
		while (failures.hasMoreElements()) {
			allProblems.add(failures.nextElement());
		}
		Enumeration<TestFailure> errors = result.errors();
		while (errors.hasMoreElements()) {
			allProblems.add(errors.nextElement());
		}
		
		if (allProblems.size() != 1) {
			throw new IllegalArgumentException("Problem not suitable for delta debugging. Input test does not fail or fails for multiple reasons (has to be exact one).");
		}
		
		Throwable throwable = allProblems.get(0).thrownException();
		if (throwable.getCause() instanceof ClassNotFoundException) {
			throw new IllegalArgumentException("Initialization error occurred, not all required dependencies are in classpath", throwable);
		}
		
		return throwable;
	}
}
