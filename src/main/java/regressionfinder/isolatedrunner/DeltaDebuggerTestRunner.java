package regressionfinder.isolatedrunner;

import org.deltadebugging.ddcore.tester.JUnitTester;

import junit.framework.Test;
import junit.framework.TestResult;

public class DeltaDebuggerTestRunner extends IsolatedClassLoaderAwareJUnitTestRunner {
	
	public DeltaDebuggerTestRunner(String testClassName, String testMethodName) {		
		super(testClassName, testMethodName);
	}

	public int runTest(Throwable throwable) {	
		alterStackTraceToEffectivelyDisableComparisonByLineNumbersAndDiscardNonSignificantPart(throwable);

		CustomizedJUnitTester tester = new CustomizedJUnitTester(jUnitTestAdapter);
		tester.setThrowable(throwable);
		return tester.test(null);
	}

	private void alterStackTraceToEffectivelyDisableComparisonByLineNumbersAndDiscardNonSignificantPart(
			Throwable throwable) {
		TestResult result = new TestResult();
		jUnitTestAdapter.run(result);
		
		 if (result.errorCount() != 1) {
			 return;
		 }
	      
		StackTraceElement[] currElems = result.errors().nextElement().thrownException().getStackTrace();
		StackTraceElement[] origElems = throwable.getStackTrace();
				
		int max = currElems.length;
		int i = 0;
        for ( ; i < origElems.length; i++) {
            if (i > max) {
            	break;
            }
                       	
            if (origElems[i].getFileName().compareTo(currElems[i].getFileName()) == 0
            		&& origElems[i].getClassName().compareTo(currElems[i].getClassName()) == 0
            		&& origElems[i].getMethodName().compareTo(currElems[i].getMethodName()) == 0) {
            	origElems[i] = new StackTraceElement(origElems[i].getClassName(), origElems[i].getMethodName(), origElems[i].getFileName(), currElems[i].getLineNumber());
            }
            
    		if (origElems[i].getClassName() != null && origElems[i].getClassName().equals("junit.framework.JUnit4TestAdapter")) {
        		break;
        	}
        }
        
        StackTraceElement[] newStackTrace = new StackTraceElement[i + 1];
        System.arraycopy(origElems, 0, newStackTrace, 0, i + 1);
        
        throwable.setStackTrace(newStackTrace);
	}
	
	class CustomizedJUnitTester extends JUnitTester {
		CustomizedJUnitTester(Test testObject) {
			super(testObject);
			fTime = 6000;
		}
	}
}