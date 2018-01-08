package regressionfinder.core;

import static regressionfinder.runner.CommandLineOption.DEVELOPMENT_MODE;
import static regressionfinder.runner.CommandLineOption.EXECUTION_ID;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.codehaus.plexus.util.FileUtils;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.hamcrest.SelfDescribing;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import regressionfinder.core.statistics.ExecutionPhase;
import regressionfinder.core.statistics.LogDuration;
import regressionfinder.core.statistics.StatisticsTracker;
import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;
import regressionfinder.model.MultiModuleMavenJavaProject;
import regressionfinder.runner.ApplicationCommandLineRunner;

@Component
public class EvaluationContext {
	
	private static final String RESULTS_FILE_NAME = "results.txt";
	private static final String RESULT_HTML = "results.html";

	
	private String executionId;
	private MultiModuleMavenJavaProject referenceProject, faultyProject, workingAreaProject;
	private String testClassName, testMethodName;
	private boolean developmentMode;
	private Supplier<Stream<URL>> testRunnerClassPaths, mavenDependenciesClassPaths;
	private Throwable throwable;
	private Path resultsTxtPath, resultsHtmlPath;
	

	@Value("${working.directory}")
	private String preparedWorkingDirectory;
	
	@Value("${dependencies.file}")
	private String preparedDependenciesFile;
		
	@Value("${evaluationbase.location}")
	private String resultsBaseDirectory;

	@Autowired
	private MavenCompiler mavenCompiler;

	@Autowired
	private ApplicationCommandLineRunner applicationCommandLineRunner;
	
	@Autowired
	private ReflectionalTestMethodRunner testMethodRunner;
	
	@Autowired
	private StatisticsTracker statisticsTracker;
	
	
	@LogDuration(ExecutionPhase.PREPARATION)
	public void initOnce() {
		/*
		 * TODO: Multiple asserts to check that evaluation context is
		 * suitable for running delta debugger: - assert test OK in
		 * reference version? - assert test itself unchanged - otherwise do
		 * not continue - assert no changes in dependencies (to save time
		 * needed to collect them)
		 */
		executionId = applicationCommandLineRunner.getArgumentsHolder().getValue(EXECUTION_ID);
		
		initResultsDirectory();
		initializeProjects();
		initializeTest();
		prepareWorkingArea();
		obtainOriginalFault();
		
		statisticsTracker.init();
	}
	
	private void initResultsDirectory() {
		try {
			Path resultsDirectory = Paths.get(resultsBaseDirectory, executionId);
			
			if (!resultsDirectory.toFile().exists()) {
				Files.createDirectory(resultsDirectory);
			}
			resultsTxtPath = resultsDirectory.resolve(RESULTS_FILE_NAME);
			if (resultsTxtPath.toFile().exists()) {
				Files.delete(resultsTxtPath);
			}
			Files.createFile(resultsTxtPath);
			
			resultsHtmlPath = resultsDirectory.resolve(RESULT_HTML);
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize results file.", e);
		}
	}

	private void initializeProjects() {
		referenceProject = new MultiModuleMavenJavaProject(applicationCommandLineRunner.getArgumentsHolder().getValue(REFERENCE_VERSION));
		faultyProject = new MultiModuleMavenJavaProject(applicationCommandLineRunner.getArgumentsHolder().getValue(FAULTY_VERSION));

		boolean mavenModulesNotChanged = referenceProject.getMavenProjects().keySet()
				.equals(faultyProject.getMavenProjects().keySet());
		Preconditions.checkState(mavenModulesNotChanged,
				"There are changes detected in the structure of project. Cannot continue.");
	}

	private void initializeTest() {
		testClassName = applicationCommandLineRunner.getArgumentsHolder().getValue(FAILING_CLASS);
		testMethodName = applicationCommandLineRunner.getArgumentsHolder().getValue(FAILING_METHOD);
	}

	private void prepareWorkingArea() {
		developmentMode = applicationCommandLineRunner.getArgumentsHolder().isPresent(DEVELOPMENT_MODE);
		if (developmentMode) {
			workingAreaProject = new MultiModuleMavenJavaProject(preparedWorkingDirectory);
		} else {
			prepareWorkingAreaInEvaluationMode();
		}
	}

	private void prepareWorkingAreaInEvaluationMode() {
		tryCopyReferenceProjectToWorkingArea();
		mavenCompiler.triggerCompilationWithTestClasses(workingAreaProject);
	}

	private void tryCopyReferenceProjectToWorkingArea() {
		try {
			Path workingDirectory = Files.createTempDirectory("deltadebugging");
			workingAreaProject = referenceProject.cloneToWorkingDirectory(workingDirectory);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't perform required I/O operations", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void obtainOriginalFault() {
		gatherTestRunnerClassPathsForIsolatedClassLoader();
		
		if (!developmentMode) {
			mavenCompiler.triggerCompilationWithTestClasses(faultyProject);
		}
		
		final Set<URL> mavenDependencies;
//			if (developmentMode)  {
/*			try (	FileInputStream in = new FileInputStream(preparedDependenciesFile);
					ObjectInputStream ois = new ObjectInputStream(in);				) {
				mavenDependencies = ((Set<URL>) ois.readObject());
		    } catch (Exception e) {
		    	System.out.println("Problem with deserializing prepared dependencies file.");
		    	throw new RuntimeException(e);
		    }*/
//		} else {
			mavenDependencies = workingAreaProject.getMavenProjects().values().stream()
					.flatMap(mavenCompiler::getLocalMavenDependencies)
					.collect(Collectors.toSet());
//		}
		mavenDependenciesClassPaths = () -> mavenDependencies.stream();
		
		throwable = (Throwable) testMethodRunner.obtainOriginalException();
	}
	
	private void gatherTestRunnerClassPathsForIsolatedClassLoader() {			
		testRunnerClassPaths = () -> Stream.of(DeltaDebuggerTestRunner.class, JUnitTester.class, NoTestsRemainException.class, SelfDescribing.class)
				.map(Class::getProtectionDomain)
				.map(ProtectionDomain::getCodeSource)
				.map(CodeSource::getLocation);
	}
	
	public Set<URL> getClassPathsForObtainingOriginalFault() {
		return getClasspathsForTestExecution(faultyProject);
	}
	
	public Set<URL> getClasspathsForTestExecution() {
		return getClasspathsForTestExecution(workingAreaProject);
	}
	
	private Set<URL> getClasspathsForTestExecution(MultiModuleMavenJavaProject targetProject) {
		return Stream.of(testRunnerClassPaths.get(), targetProject.collectClassPaths(), mavenDependenciesClassPaths.get())
			.flatMap(Function.identity())
			.collect(Collectors.toSet());
	}

	public MultiModuleMavenJavaProject getReferenceProject() {
		return referenceProject;
	}

	public MultiModuleMavenJavaProject getFaultyProject() {
		return faultyProject;
	}

	public MultiModuleMavenJavaProject getWorkingAreaProject() {
		return workingAreaProject;
	}

	public String getTestClassName() {
		return testClassName;
	}

	public String getTestMethodName() {
		return testMethodName;
	}

	public Throwable getThrowable() {
		return throwable;
	}
	
	public String getExecutionId() {
		return executionId;
	}

	public Path getResultsTxt() {
		return resultsTxtPath;
	}
	
	public Path getResultsHtml() {
		return resultsHtmlPath;
	}
	
	@PreDestroy
	public void cleanUp() throws IOException {
		if (!developmentMode) {
			FileUtils.deleteDirectory(workingAreaProject.getRootDirectory().toFile());
		}
	}
}
