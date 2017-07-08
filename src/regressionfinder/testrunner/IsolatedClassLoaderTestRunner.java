package regressionfinder.testrunner;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.deltadebugging.ddcore.tester.JUnitTester;
import org.junit.runner.manipulation.NoTestsRemainException;

import junit.framework.TestFailure;
import junit.framework.TestResult;

public class IsolatedClassLoaderTestRunner extends IsolatedClassLoaderAwareJUnitTestRunner {
	
	private JUnitTester tester;
	private Throwable originalException;
	
	public IsolatedClassLoaderTestRunner(String testClassName, String testMethodName) 
			throws ClassNotFoundException, NoTestsRemainException {
		
		super(testClassName, testMethodName);
		
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
		
		originalException = allProblems.get(0).thrownException();
	}
	
	public Throwable getOriginalException() {
		return originalException;
	}
	
	public IsolatedClassLoaderTestRunner(String testClassName, String testMethodName, Throwable throwable) 
			throws ClassNotFoundException, NoTestsRemainException {		
		
		super(testClassName, testMethodName);
		
		tester = new JUnitTester(jUnitTestAdapter);
		tester.setThrowable(throwable);
	}

	public int runTest() {
		return tester.test(null);
	}
}