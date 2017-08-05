package regressionfinder.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
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

import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.isolatedrunner.IsolatedClassLoaderAwareJUnitTestRunner;
import regressionfinder.isolatedrunner.IsolatedURLClassLoader;
import regressionfinder.isolatedrunner.JUnitTestRunner;
import regressionfinder.isolatedrunner.MethodDescriptor;
import regressionfinder.manipulation.FileManipulator;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.FileSourceCodeChange;

@Component
public class ReflectionalTestMethodInvoker {
	
	private Supplier<Stream<URL>> libraryClassPaths;
	private Throwable throwable;
	
	@Autowired
	private EvaluationContext evaluationContext;

	public void initializeOnce() {


		gatherLibraryClassPathsForIsolatedClassLoader();
		
		evaluationContext.getFaultyProject().triggerCompilationWithTests();
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

	public int testAppliedChangeSet(List<FileSourceCodeChange> sourceCodeChanges) {
		AffectedFile.fromListOfChanges(sourceCodeChanges).forEach(file -> {
			try {
				Path copyOfSource = evaluationContext.getWorkingAreaProject()
						.copyFromAnotherProject(evaluationContext.getReferenceProject(), file.getPath());
				new FileManipulator(copyOfSource).applyChanges(file.getFailureInducingChanges());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	
		evaluationContext.getWorkingAreaProject().triggerSimpleCompilation();
		
		return (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				Stream.concat(libraryClassPaths.get(), evaluationContext.getWorkingAreaProject().getClassPaths()).toArray(URL[]::new),
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
	}

	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz, 
			URL[] classPaths, MethodDescriptor methodDescriptor) {
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(classPaths)) {
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
