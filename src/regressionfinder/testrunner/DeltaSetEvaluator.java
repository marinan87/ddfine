package regressionfinder.testrunner;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.hamcrest.SelfDescribing;
import org.junit.runner.manipulation.NoTestsRemainException;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.handlers.EvaluationTask;
import regressionfinder.utils.SourceCodeManipulator;

public class DeltaSetEvaluator extends JUnitTester {
	
	public static final String WORK_AREA = "StagingArea";

	private final URL[] urls;
	private final Throwable throwable;
	private final EvaluationTask task;
	private final String basePath;
	
	public DeltaSetEvaluator(EvaluationTask task, String basePath) throws Exception {
		super();
		
		this.task = task;
		this.basePath = basePath;
		urls = collectClasspaths(); 
		throwable = obtainOriginalStacktrace();
	}
	
	private URL[] collectClasspaths() throws Exception {
		String stagingAreaBasePath = Paths.get(basePath, WORK_AREA).toString();
		String stagingAreaCodePath = Paths.get(stagingAreaBasePath, "target", "classes").toString();
		String stagingAreaTestsPath = Paths.get(stagingAreaBasePath, "target", "test-classes").toString();
		stagingAreaCodePath = stagingAreaCodePath.replace("\\", "/");
		stagingAreaTestsPath = stagingAreaTestsPath.replace("\\", "/");
		
		List<URL> urlList = new ArrayList<>();
		// These paths are required because DeltaDebuggerTestRunner needs to find JUnit test classes inside StagingArea subfolder.
		// See implementation of DeltaDebuggerTestRunner.runTest().
		urlList.add(new URL("file:/" + stagingAreaCodePath + "/"));
		urlList.add(new URL("file:/" + stagingAreaTestsPath + "/"));
		
		// This is required because IsolatedURLClassLoader should be able to locate DeltaDebuggerTestRunner and JUnitTestRunner class, 
		// which reside in the plugin project.
		urlList.add(new URL("file:" + DeltaDebuggerTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath()));
		urlList.add(JUnitTester.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(NoTestsRemainException.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(SelfDescribing.class.getProtectionDomain().getCodeSource().getLocation());

		return (URL[]) urlList.toArray(new URL[0]);
	}
	
	private Throwable obtainOriginalStacktrace() {
		SourceCodeManipulator.copyToStagingAreaWithModifications(basePath, task.getFaultyVersion(), new ArrayList<>());
		
		return (Throwable) runMethodInIsolatedTestRunner(JUnitTestRunner.class, 
				new MethodDescriptor("getOriginalException", null, null));
	}
	
	@Override
	public int test(DeltaSet c) {
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) c.stream().collect(toList());
		return testSelectedChangeSet(selectedChangeSet); 
	}
	
	private int testSelectedChangeSet(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		SourceCodeManipulator.copyToStagingAreaWithModifications(basePath, task.getReferenceVersion(), selectedSourceCodeChangeSet);
		
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
