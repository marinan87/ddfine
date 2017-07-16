package regressionfinder.isolatedrunner;

import org.deltadebugging.ddcore.tester.JUnitTester;
import org.junit.runner.manipulation.NoTestsRemainException;

public class DeltaDebuggerTestRunner extends IsolatedClassLoaderAwareJUnitTestRunner {
	
	public DeltaDebuggerTestRunner(String testClassName, String testMethodName) 
			throws ClassNotFoundException, NoTestsRemainException {		
		super(testClassName, testMethodName);
	}

	public int runTest(Throwable throwable) {		
		JUnitTester tester = new JUnitTester(jUnitTestAdapter);
		tester.setThrowable(throwable);
		return tester.test(null);
	}
}