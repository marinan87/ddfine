package regressionfinder.core;

import static regressionfinder.runner.CommandLineOption.DEVELOPMENT_MODE;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PreDestroy;

import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import regressionfinder.model.MultiModuleMavenJavaProject;
import regressionfinder.runner.ApplicationCommandLineRunner;

@Component
public class EvaluationContext {
	
	private MultiModuleMavenJavaProject referenceProject, faultyProject, workingAreaProject;
	private String testClassName, testMethodName;
	private boolean developmentMode;

	@Value("${working.directory}")
	private String preparedWorkingDirectory;

	@Autowired
	private MavenCompiler mavenCompiler;

	@Autowired
	private ApplicationCommandLineRunner commandLineRunner;
	
	
	public void initOnce() {
		/*
		 * TODO: Multiple asserts to check that evaluation context is
		 * suitable for running delta debugger: - assert test OK in
		 * reference version? - assert test itself unchanged - otherwise do
		 * not continue - assert no changes in dependencies (to save time
		 * needed to collect them)
		 */

		initializeProjects();
		initializeTest();
		prepareWorkingArea();
	}

	private void initializeProjects() {
		referenceProject = new MultiModuleMavenJavaProject(commandLineRunner.getArgumentsHolder().getValue(REFERENCE_VERSION));
		faultyProject = new MultiModuleMavenJavaProject(commandLineRunner.getArgumentsHolder().getValue(FAULTY_VERSION));

		boolean mavenModulesNotChanged = referenceProject.getMavenProjects().keySet()
				.equals(faultyProject.getMavenProjects().keySet());
		Preconditions.checkState(mavenModulesNotChanged,
				"There are changes detected in the structure of project. Cannot continue.");
	}

	private void initializeTest() {
		testClassName = commandLineRunner.getArgumentsHolder().getValue(FAILING_CLASS);
		testMethodName = commandLineRunner.getArgumentsHolder().getValue(FAILING_METHOD);
	}

	private void prepareWorkingArea() {
		developmentMode = commandLineRunner.getArgumentsHolder().isPresent(DEVELOPMENT_MODE);
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

	public boolean isDevelopmentMode() {
		return developmentMode;
	}

	@PreDestroy
	public void cleanUp() throws IOException {
		if (!developmentMode) {
			FileUtils.deleteDirectory(workingAreaProject.getRootDirectory().toFile());
		}
	}
}
