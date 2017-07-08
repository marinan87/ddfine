package regressionfinder.testrunner;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.runner.manipulation.NoTestsRemainException;

import junit.framework.TestFailure;
import junit.framework.TestResult;

public class JUnitTestRunner extends IsolatedClassLoaderAwareJUnitTestRunner {

	public JUnitTestRunner(String testClassName, String testMethodName)
			throws ClassNotFoundException, NoTestsRemainException {
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
			throw new IllegalArgumentException("Problem not suitable for delta debugging.");
		}
		
		return allProblems.get(0).thrownException();
	}
}
