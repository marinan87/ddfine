package regressionfinder.testrunner;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.deltadebugging.ddcore.tester.JUnitTester;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/*
 * Workaround for "No runnable methods" exception when running JUnit tests using JunitCore.run
 * See here: http://stackoverflow.com/questions/24319697/java-lang-exception-no-runnable-methods-exception-in-running-junits/24319836
 */
public class IsolatedClassLoaderTestRunner {
	
	private JUnitTester tester;
	private Throwable originalException;
	
	public IsolatedClassLoaderTestRunner(String testClassName, String testMethodName) throws ClassNotFoundException, NoTestsRemainException {
		ensureLoadedInIsolatedClassLoader(this);

		Class<?> testClass = Class.forName(testClassName);		
		JUnit4TestAdapter testAdapter = new JUnit4TestAdapter(testClass);
		testAdapter.filter(Filter.matchMethodDescription(Description.createTestDescription(testClass, testMethodName)));
		
		TestResult result = new TestResult();
		testAdapter.run(result);
		
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
	
	public IsolatedClassLoaderTestRunner(String testClassName, String testMethodName, Throwable throwable) throws ClassNotFoundException, NoTestsRemainException {
		ensureLoadedInIsolatedClassLoader(this);
		
		Class<?> testClass = Class.forName(testClassName);		
		JUnit4TestAdapter testAdapter = new JUnit4TestAdapter(testClass);
		testAdapter.filter(Filter.matchMethodDescription(Description.createTestDescription(testClass, testMethodName)));
				
		tester = new JUnitTester(testAdapter);
		tester.setThrowable(throwable);
	}

	public int runTest() {
		ensureLoadedInIsolatedClassLoader(this);
		return tester.test(null);
	}

	private void ensureLoadedInIsolatedClassLoader(Object o) {
		String objectClassLoader = o.getClass().getClassLoader().getClass().getName();
		if (!objectClassLoader.equals(IsolatedURLClassLoader.class.getName())) {
			throw new IllegalStateException(
					String.format("Instance of %s not loaded by a IsolatedURLClassLoader (loaded by %s)", 
							o.getClass(), objectClassLoader));
		}
	}
}