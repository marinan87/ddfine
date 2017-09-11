package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.DEVELOPMENT_MODE;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MultiModuleMavenJavaProject;
import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Component
public class EvaluationContext extends JUnitTester {

	private MultiModuleMavenJavaProject referenceProject, faultyProject, workingAreaProject;
	private String testClassName, testMethodName;
	private boolean developmentMode;
	private int trials;
	
	@Value("${working.directory}")
	private String preparedWorkingDirectory;

	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
	
	@Autowired
	private MavenCompiler mavenCompiler;
	
			
	public void initializeOnce(CommandLineArgumentsInterpreter arguments) {
		try {		
			/*
			  TODO: Multiple asserts to check that evaluation context is suitable for running delta debugger:
			- assert test OK in reference version?
			- assert test itself unchanged - otherwise do not continue
			- assert no changes in dependencies (to save time needed to collect them)
			*/
			
			initializeProjects(arguments);
			initializeTest(arguments);
			prepareWorkingArea(arguments);			
			reflectionalInvoker.initializeOnce();
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void initializeProjects(CommandLineArgumentsInterpreter arguments) {
		referenceProject = new MultiModuleMavenJavaProject(arguments.getValue(REFERENCE_VERSION));
		faultyProject = new MultiModuleMavenJavaProject(arguments.getValue(FAULTY_VERSION));
		
		boolean mavenModulesNotChanged = referenceProject.getMavenProjects().keySet().equals(faultyProject.getMavenProjects().keySet());
		Preconditions.checkState(mavenModulesNotChanged, "There are changes detected in the structure of project. Cannot continue.");
	}
	
	private void initializeTest(CommandLineArgumentsInterpreter arguments) {
		testClassName = arguments.getValue(FAILING_CLASS);
		testMethodName = arguments.getValue(FAILING_METHOD);
	}

	private void prepareWorkingArea(CommandLineArgumentsInterpreter arguments) {
		developmentMode = arguments.isPresent(DEVELOPMENT_MODE);
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
	
	public int getNumberOfTrials() {
		return trials;
	}
	
	@Override
	public int test(DeltaSet set) {
		trials++;
		
		@SuppressWarnings("unchecked")
		List<MinimalApplicableChange> selectedChangeSet = (List<MinimalApplicableChange>) set.stream().collect(toList());		
		return reflectionalInvoker.testAppliedChangeSet(selectedChangeSet); 
	}
	
	public void cleanUp() throws IOException {
		if (!developmentMode) {
			FileUtils.deleteDirectory(workingAreaProject.getRootDirectory().toFile());
		}
	}
}
