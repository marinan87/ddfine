package regressionfinder.testrunner;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;

import junit.framework.JUnit4TestAdapter;

public abstract class IsolatedClassLoaderAwareJUnitTestRunner {
	
	protected JUnit4TestAdapter jUnitTestAdapter;

	public IsolatedClassLoaderAwareJUnitTestRunner(String testClassName, String testMethodName) 
			throws ClassNotFoundException, NoTestsRemainException {
		ensureLoadedInIsolatedClassLoader(this);
		
		Class<?> testClass = Class.forName(testClassName);		
		jUnitTestAdapter = new JUnit4TestAdapter(testClass);
		jUnitTestAdapter.filter(Filter.matchMethodDescription(Description.createTestDescription(testClass, testMethodName)));		
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
