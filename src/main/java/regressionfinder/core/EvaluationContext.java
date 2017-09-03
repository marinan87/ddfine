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
			referenceProject = new MultiModuleMavenJavaProject(arguments.getValue(REFERENCE_VERSION));
			faultyProject = new MultiModuleMavenJavaProject(arguments.getValue(FAULTY_VERSION));
			developmentMode = arguments.isPresent(DEVELOPMENT_MODE);
			
			if (developmentMode) {
				workingAreaProject = new MultiModuleMavenJavaProject(preparedWorkingDirectory);
			} else {
				Path workingDirectory = Files.createTempDirectory("deltadebugging");
				workingAreaProject = referenceProject.cloneToWorkingDirectory(workingDirectory);
				mavenCompiler.triggerCompilationWithTestClasses(workingAreaProject);
			}
			
			testClassName = arguments.getValue(FAILING_CLASS);
			testMethodName = arguments.getValue(FAILING_METHOD);
			
			reflectionalInvoker.initializeOnce();
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
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
