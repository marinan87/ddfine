package regressionfinder.testrunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.deltadebugging.ddcore.DD;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.handlers.EvaluationTask;
import regressionfinder.utils.JavaModelHelper;
import regressionfinder.utils.SourceCodeManipulator;

public class DeltaSetEvaluator extends JUnitTester {
	
	private static final String WORK_AREA = "StagingArea";

	private final URL[] urls;
	private final Throwable throwable;
	private final EvaluationTask task;
	
	public DeltaSetEvaluator(EvaluationTask task) throws Exception {
		super();
		
		this.task = task;
		urls = collectClasspaths(); 
		throwable = obtainOriginalStacktrace();
	}
	
	private URL[] collectClasspaths() throws Exception {
		IJavaProject project = JavaModelHelper.findJavaProjectInWorkspace(WORK_AREA);
		String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(project);
		// These paths are required because IsolatedClassLoaderTestRunner needs to find JUnit test classes in StagingArea project.
		// See implementation of IsolatedClassLoaderTestRunner.runTest().
		List<URL> urlList = Stream.of(classPathEntries)
				.map(entry -> {
					IPath path = new Path(entry);
					try {
						return path.toFile().toURI().toURL();
					} catch (MalformedURLException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		
		// This is required because IsolatedURLClassLoader should be able to find IsolatedClassLoaderTestRunner class, 
		// which resides in the plugin project.
		urlList.add(new URL("file:" + IsolatedClassLoaderTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "bin/"));
		urlList.add(DD.class.getProtectionDomain().getCodeSource().getLocation());

		return (URL[]) urlList.toArray(new URL[urlList.size()]);
	}
	
	private Throwable obtainOriginalStacktrace() {
		SourceCodeManipulator.copyToStagingAreaWithModifications(task.getRegressionCU(), new ArrayList<>());
		
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(urls)) {
			Class<?> runnerClass = isolatedClassLoader.loadClass(IsolatedClassLoaderTestRunner.class.getName());
			Constructor<IsolatedClassLoaderTestRunner> constructor = 
					(Constructor<IsolatedClassLoaderTestRunner>) runnerClass.getConstructor(String.class, String.class);
			
			Object isolatedTestRunner = constructor.newInstance(task.getTestClassName(), task.getTestMethodName());
			
			Method method = isolatedTestRunner.getClass().getMethod("getOriginalException");
			return (Throwable) method.invoke(isolatedTestRunner);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public int test(DeltaSet c) {
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) c.stream().collect(Collectors.toList());
		return testSelectedChangeSet(selectedChangeSet); 
	}
	
	private int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		SourceCodeManipulator.copyToStagingAreaWithModifications(task.getSourceCU(), selectedSourceCodeChangeSet);
		
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(urls)) {
			// TODO: reload only changed classes programmatically
			Class<?> runnerClass = isolatedClassLoader.loadClass(IsolatedClassLoaderTestRunner.class.getName());
			Constructor<IsolatedClassLoaderTestRunner> constructor = 
					(Constructor<IsolatedClassLoaderTestRunner>) runnerClass.getConstructor(String.class, String.class, Throwable.class);
			
			Object isolatedTestRunner = constructor.newInstance(task.getTestClassName(), task.getTestMethodName(), throwable);
		
			Method method = isolatedTestRunner.getClass().getMethod("runTest");
			return (int) method.invoke(isolatedTestRunner);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
