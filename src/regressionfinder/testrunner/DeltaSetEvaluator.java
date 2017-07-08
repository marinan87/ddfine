package regressionfinder.testrunner;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
				.collect(toList());
		
		// This is required because IsolatedURLClassLoader should be able to locate DeltaDebuggerTestRunner and JUnitTestRunner class, 
		// which reside in the plugin project.
		urlList.add(new URL("file:" + DeltaDebuggerTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "bin/"));
		urlList.add(JUnitTester.class.getProtectionDomain().getCodeSource().getLocation());

		return (URL[]) urlList.toArray(new URL[urlList.size()]);
	}
	
	private Throwable obtainOriginalStacktrace() {
		SourceCodeManipulator.copyToStagingAreaWithModifications(task.getRegressionCU(), new ArrayList<>());
		
		return (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				new MethodDescriptor("getOriginalException", null, null));
	}
	
	@Override
	public int test(DeltaSet c) {
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) c.stream().collect(toList());
		return testSelectedChangeSet(selectedChangeSet); 
	}
	
	private int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		SourceCodeManipulator.copyToStagingAreaWithModifications(task.getSourceCU(), selectedSourceCodeChangeSet);
		
		return (int) runMethodInIsolatedTestRunner(DeltaDebuggerTestRunner.class, 
				new MethodDescriptor("runTest", new Class<?>[] { Throwable.class }, new Object[] { throwable }));
	}
	
	private <T extends IsolatedClassLoaderAwareJUnitTestRunner> Object runMethodInIsolatedTestRunner(Class<T> clazz,
			MethodDescriptor methodDescriptor) {
		try (IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(urls)) {
			Class<?> runnerClass = isolatedClassLoader.loadClass(clazz.getName());
			Constructor<?> constructor = runnerClass.getConstructor(String.class, String.class);
			
			Object isolatedTestRunner = constructor.newInstance(task.getTestClassName(), task.getTestMethodName());
		
			Method method = isolatedTestRunner.getClass().getMethod(methodDescriptor.getMethodName(), methodDescriptor.getParameterTypes());
			return method.invoke(isolatedTestRunner, methodDescriptor.getArgs());			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
