package regressionfinder.core;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.deltadebugging.ddcore.tester.JUnitTester;
import org.hamcrest.SelfDescribing;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import regressionfinder.core.manipulation.PrepareWorkingAreaVisitor;
import regressionfinder.core.manipulation.RestoreWorkingAreaVisitor;
import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.isolatedrunner.IsolatedClassLoaderAwareJUnitTestRunner;
import regressionfinder.isolatedrunner.IsolatedURLClassLoader;
import regressionfinder.isolatedrunner.JUnitTestRunner;
import regressionfinder.isolatedrunner.MethodDescriptor;
import regressionfinder.model.AffectedEntity;
import regressionfinder.model.MinimalApplicableChange;

@Component
public class ReflectionalTestMethodInvoker {
	
	private Supplier<Stream<URL>> testRunnerClassPaths, mavenDependenciesClassPaths;
	private Throwable throwable;
	
	@Value("${dependencies.file}")
	private String preparedDependenciesFile;
	
	@Autowired
	private EvaluationContext evaluationContext;

	@Autowired
	private PrepareWorkingAreaVisitor prepareWorkingAreaVisitor;
	
	@Autowired
	private RestoreWorkingAreaVisitor restoreWorkingAreaVisitor;
	
	@Autowired
	private MavenCompiler mavenCompiler;

	
	@SuppressWarnings("unchecked")
	public void initializeOnce() {
		gatherTestRunnerClassPathsForIsolatedClassLoader();
		
		if (!evaluationContext.isDevelopmentMode()) {
			mavenCompiler.triggerCompilationWithTestClasses(evaluationContext.getFaultyProject());
		}
		
		final Set<URL> mavenDependencies;
//		if (evaluationContext.isDevelopmentMode())  {
			try (	FileInputStream in = new FileInputStream(preparedDependenciesFile);
					ObjectInputStream ois = new ObjectInputStream(in);				) {
				mavenDependencies = ((Set<URL>) ois.readObject());
		    } catch (Exception e) {
		    	System.out.println("Problem with deserializing prepared dependencies file.");
		    	throw new RuntimeException(e);
		    }
/*		} else {
			mavenDependencies = evaluationContext.getWorkingAreaProject().getMavenProjects().values().stream()
					.flatMap(mavenCompiler::getLocalMavenDependencies)
					.collect(toSet());
		}*/
		mavenDependenciesClassPaths = () -> mavenDependencies.stream();
		
		throwable = (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				Stream.of(testRunnerClassPaths.get(), evaluationContext.getFaultyProject().collectClassPaths(), mavenDependenciesClassPaths.get())
					.flatMap(Function.identity())
					.collect(Collectors.toSet()),
				new MethodDescriptor("getOriginalException"));
	}
	
	private void gatherTestRunnerClassPathsForIsolatedClassLoader() {			
		testRunnerClassPaths = () -> Stream.of(DeltaDebuggerTestRunner.class, JUnitTester.class, NoTestsRemainException.class, SelfDescribing.class)
				.map(Class::getProtectionDomain)
				.map(ProtectionDomain::getCodeSource)
				.map(CodeSource::getLocation);
	}

	public int testAppliedChangeSet(List<MinimalApplicableChange> sourceCodeChanges) {
		List<AffectedEntity> affectedFiles = AffectedEntity.fromListOfMinimalChanges(sourceCodeChanges);
		
		prepareWorkingAreaForNextTrial(affectedFiles);
		
		int testOutcome = (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				Stream.of(testRunnerClassPaths.get(), evaluationContext.getWorkingAreaProject().collectClassPaths(), mavenDependenciesClassPaths.get())
					.flatMap(Function.identity())
					.collect(Collectors.toSet()),
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
		
		restoreWorkingArea(affectedFiles);
		
		return testOutcome;
	}

	private void prepareWorkingAreaForNextTrial(List<AffectedEntity> affectedFiles) {
		affectedFiles.forEach(file -> file.manipulate(prepareWorkingAreaVisitor));	
		mavenCompiler.triggerSimpleCompilation(evaluationContext.getWorkingAreaProject());
	}

	private void restoreWorkingArea(List<AffectedEntity> affectedFiles) {
		affectedFiles.forEach(file -> file.manipulate(restoreWorkingAreaVisitor));
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
