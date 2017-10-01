package regressionfinder.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.isolatedrunner.IsolatedClassLoaderAwareJUnitTestRunner;
import regressionfinder.isolatedrunner.IsolatedURLClassLoader;
import regressionfinder.isolatedrunner.JUnitTestRunner;
import regressionfinder.isolatedrunner.MethodDescriptor;

@Service
public class ReflectionalTestMethodRunner {

	@Autowired
	private EvaluationContext evaluationContext;
	
	
	public <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object obtainOriginalException() {
		return runMethodInIsolatedTestRunner(JUnitTestRunner.class, evaluationContext.getClassPathsForObtainingOriginalFault(),
				new MethodDescriptor("getOriginalException"));
	}
	
	public <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runFaultyTest() {
		return runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, evaluationContext.getClasspathsForTestExecution(),
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { evaluationContext.getThrowable() }));
	}
	
	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz, 
			Set<URL> classPaths, MethodDescriptor methodDescriptor) {
		URL[] classPathURLs = classPaths.toArray(new URL[0]);
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(classPathURLs)) {
			Class<?> runnerClass = isolatedClassLoader.loadClass(clazz.getName());
			Constructor<?> constructor = runnerClass.getConstructor(String.class, String.class);
			
			Object isolatedTestRunner = constructor.newInstance(evaluationContext.getTestClassName(), evaluationContext.getTestMethodName());
		
			Method method = isolatedTestRunner.getClass().getMethod(methodDescriptor.getMethodName(), methodDescriptor.getParameterTypes());
			return method.invoke(isolatedTestRunner, methodDescriptor.getArgs());			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
