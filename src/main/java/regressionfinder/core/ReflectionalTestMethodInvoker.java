package regressionfinder.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
	private Supplier<Stream<URL>> libraryClassPaths;
	private Throwable throwable;
	
	@Autowired
	private EvaluationContext evaluationContext;

	public void initializeOnce(String testClassName, String testMethodName) {
		this.testClassName = testClassName;
		this.testMethodName = testMethodName;
		
		gatherLibraryClassPathsForIsolatedClassLoader();
		
		throwable = (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				Stream.concat(libraryClassPaths.get(), evaluationContext.getFaultyProject().getClassPaths()).toArray(URL[]::new),
				new MethodDescriptor("getOriginalException"));
	}
	
	private void gatherLibraryClassPathsForIsolatedClassLoader() {			
		libraryClassPaths = () -> Stream.of(DeltaDebuggerTestRunner.class, JUnitTester.class, NoTestsRemainException.class, SelfDescribing.class)
			.map(Class::getProtectionDomain)
			.map(ProtectionDomain::getCodeSource)
			.map(CodeSource::getLocation);
	}

	public int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		return (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				Stream.concat(libraryClassPaths.get(), evaluationContext.getWorkingAreaProject().getClassPaths()).toArray(URL[]::new),
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
	}

	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz, 
			URL[] classPaths, MethodDescriptor methodDescriptor) {
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(classPaths)) {
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
