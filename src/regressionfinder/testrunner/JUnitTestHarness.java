package regressionfinder.testrunner;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import dd.TestHarness;
import regressionfinder.utils.SourceCodeManipulator;
import regressionfinder.utils.JavaModelHelper;

public class JUnitTestHarness extends TestHarness<SourceCodeChange> {
	
	private static final String TEMP_PROJECT = "StagingArea";
	private static final String TEST_CLASS = "simple.ExampleTest"; // currently hard-coded value
	
	private final ICompilationUnit sourceCU;
	private final URL[] urls;
	
	public JUnitTestHarness(ICompilationUnit sourceCU) throws Exception {
		this.sourceCU = sourceCU;
		this.urls = collectClasspaths(TEMP_PROJECT);
	}
	
	private URL[] collectClasspaths(String projectName) throws Exception {
		IJavaProject project = JavaModelHelper.findJavaProjectInWorkspace(projectName);
		String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(project);
		// These paths are required because IsolatedClassLoaderTestRunner needs to find JUnit test classes in StagingArea project.
		// See implementation of IsolatedClassLoaderTestRunner.run_invokedReflectively().
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
		// which resides in the plug-in project.
		urlList.add(new URL("file:" + IsolatedClassLoaderTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "bin/"));
		
		return (URL[]) urlList.toArray(new URL[urlList.size()]);
	}

	@Override
	public int run(List<SourceCodeChange> selectedSourceCodeChangeSet) {
		boolean testPassed = true;
		try {
			// TODO: instead of obtaining new copy each time, try to undo previous changes?
			SourceCodeManipulator.copyAndModifyLocalizationSource(sourceCU, selectedSourceCodeChangeSet);
			testPassed = runUnitTest();
		} catch (Exception e) {
			return TestHarness.UNRESOLVED;
		} finally {
			// final clean-up
//			if (copyOfSource != null && copyOfSource.exists()) {
//				try {
//					copyOfSource.delete(true, null);
//					copyOfSource.makeConsistent(new NullProgressMonitor());
//				} catch (JavaModelException e) {
//					e.printStackTrace();
//				}
//			}
		}
		
		return testPassed ? TestHarness.PASS : TestHarness.FAIL;
	}

	private boolean runUnitTest() throws Exception {
		// Not very optimal, but currently all classes in StagingArea project are reloaded during each delta debugging split iterations.
		// Ideally one should reload only the affected class, to which changes were applied (like JRebel does). 
		// However, implementing this logic is clearly out of scope of the current work.
		IsolatedURLClassLoader isolatedClassLoader = new IsolatedURLClassLoader(urls);			
		Class<?> runnerClass = isolatedClassLoader.loadClass(IsolatedClassLoaderTestRunner.class.getName());
		Object testRunner = runnerClass.newInstance();
		
		Method method = testRunner.getClass().getMethod("run_invokedReflectively", List.class);
		List<String> testClasses = new ArrayList<>();
		testClasses.add(TEST_CLASS);
		int failureCount = (int) method.invoke(testRunner, testClasses);
		
		try {
			isolatedClassLoader.close();
		} catch (IOException e) {
			// ignore
		}
		
		return failureCount == 0;
	}
}
