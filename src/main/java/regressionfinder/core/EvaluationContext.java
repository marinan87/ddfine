package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.model.MavenJavaProject;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.ProjectSourceTreeScanner;
import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Component
public class EvaluationContext extends JUnitTester {

	private MavenJavaProject referenceVersionProject, faultyVersionProject, workingAreaProject;
	private String testClassName, testMethodName;
	private int trials;

	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
			
	public void initializeOnce(CommandLineArgumentsInterpreter arguments) {
		try {		
			List<MavenJavaProject> mavenProjects = new ArrayList<>();
			ProjectSourceTreeScanner.scanSourceTreeForMavenProjects(Paths.get(arguments.getValue(REFERENCE_VERSION)), mavenProjects);
			
			referenceVersionProject = MavenJavaProject.tryCreateMavenProject(arguments.getValue(REFERENCE_VERSION));
			faultyVersionProject = MavenJavaProject.tryCreateMavenProject(arguments.getValue(FAULTY_VERSION));
			
			Path workingDirectory = Files.createTempDirectory("deltadebugging");
			referenceVersionProject.copyEverythingTo(workingDirectory);
			workingAreaProject = MavenJavaProject.tryCreateMavenProject(workingDirectory.toString());
			workingAreaProject.triggerCompilationWithTests();
			
			testClassName = arguments.getValue(FAILING_CLASS);
			testMethodName = arguments.getValue(FAILING_METHOD);
			
			reflectionalInvoker.initializeOnce();
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public MavenJavaProject getReferenceProject() {
		return referenceVersionProject;
	}
	
	public MavenJavaProject getFaultyProject() {
		return faultyVersionProject;
	}
	
	public MavenJavaProject getWorkingAreaProject() {
		return workingAreaProject;
	}
	
	public String getTestClassName() {
		return testClassName;
	}

	public String getTestMethodName() {
		return testMethodName;
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
}
