package regressionfinder.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import regressionfinder.manipulation.PrepareWorkingAreaVisitor;
import regressionfinder.manipulation.RestoreWorkingAreaVisitor;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;

@Component
public class ReflectionalTestMethodInvoker {
	
	private Supplier<Stream<URL>> libraryClassPaths;
	private Throwable throwable;
	
	@Autowired
	private EvaluationContext evaluationContext;

	@Autowired
	private PrepareWorkingAreaVisitor prepareWorkingAreaVisitor;
	
	@Autowired
	private RestoreWorkingAreaVisitor restoreWorkingAreaVisitor;

	
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

	public int testAppliedChangeSet(List<MinimalApplicableChange> sourceCodeChanges) {
		List<MinimalChangeInFile> changesInFile = sourceCodeChanges.stream()
				.filter(change -> change instanceof MinimalChangeInFile)
				.map(change -> (MinimalChangeInFile) change)
				.collect(Collectors.toList());
		List<AffectedFile> affectedFiles = AffectedFile.fromListOfMinimalChanges(changesInFile);
		
		prepareWorkingAreaForNextTrial(affectedFiles);
		
		int testOutcome = (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				Stream.concat(libraryClassPaths.get(), evaluationContext.getWorkingAreaProject().getClassPaths()).toArray(URL[]::new),
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
		
		restoreWorkingArea(affectedFiles);
		
		return testOutcome;
	}

	private void prepareWorkingAreaForNextTrial(List<AffectedFile> affectedFiles) {
		affectedFiles.forEach(file -> file.manipulate(prepareWorkingAreaVisitor));	
		evaluationContext.getWorkingAreaProject().triggerSimpleCompilation();
	}

	private void restoreWorkingArea(List<AffectedFile> affectedFiles) {
		affectedFiles.forEach(file -> file.manipulate(restoreWorkingAreaVisitor));
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
