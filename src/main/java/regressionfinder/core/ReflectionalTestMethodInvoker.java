package regressionfinder.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.deltadebugging.ddcore.tester.JUnitTester;
import org.hamcrest.SelfDescribing;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.isolatedrunner.IsolatedClassLoaderAwareJUnitTestRunner;
import regressionfinder.isolatedrunner.IsolatedURLClassLoader;
import regressionfinder.isolatedrunner.JUnitTestRunner;
import regressionfinder.isolatedrunner.MethodDescriptor;

@Component
public class ReflectionalTestMethodInvoker {
	
	private String testClassName, testMethodName;
	private URL[] classPathUrls;
	private Throwable throwable;
	
	@Autowired
	private EvaluationContext evaluationContext;

	public void initializeOnce(String testClassName, String testMethodName) {
		this.testClassName = testClassName;
		this.testMethodName = testMethodName;
		
		gatherClassPathsForIsolatedClassLoader();
		
		throwable = (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				new MethodDescriptor("getOriginalException"));
	}
	
	private void gatherClassPathsForIsolatedClassLoader() {	
		// These paths are required because DeltaDebuggerTestRunner needs to find JUnit test classes inside StagingArea subfolder.
		// See implementation of DeltaDebuggerTestRunner.runTest().
		List<URL> urlList = evaluationContext.getWorkingAreaProject().getClassPaths();
		
		// This is required because IsolatedURLClassLoader should be able to locate DeltaDebuggerTestRunner and JUnitTestRunner classes.
		urlList.add(DeltaDebuggerTestRunner.class.getProtectionDomain().getCodeSource().getLocation());		
		urlList.add(JUnitTester.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(NoTestsRemainException.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(SelfDescribing.class.getProtectionDomain().getCodeSource().getLocation());

		classPathUrls = (URL[]) urlList.toArray(new URL[0]);
	}

	public int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		return (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
	}

	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz, MethodDescriptor methodDescriptor) {
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(classPathUrls)) {
			Class<?> runnerClass = isolatedClassLoader.loadClass(clazz.getName());
			Constructor<?> constructor = runnerClass.getConstructor(String.class, String.class);
			
			Object isolatedTestRunner = constructor.newInstance(testClassName, testMethodName);
		
			Method method = isolatedTestRunner.getClass().getMethod(methodDescriptor.getMethodName(), methodDescriptor.getParameterTypes());
			return method.invoke(isolatedTestRunner, methodDescriptor.getArgs());			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
