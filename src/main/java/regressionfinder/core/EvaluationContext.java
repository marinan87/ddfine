package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.SourceCodeManipulationService;
import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Component
public class EvaluationContext extends JUnitTester {

	private MavenProject referenceVersionProject, faultyVersionProject, workingAreaProject;

	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
		
	@Autowired
	private SourceCodeManipulationService sourceCodeManipulationService;
	
	public void initializeOnce(CommandLineArgumentsInterpreter arguments) {
		try {		
			referenceVersionProject = new MavenProject(arguments.getValue(REFERENCE_VERSION));
			faultyVersionProject = new MavenProject(arguments.getValue(FAULTY_VERSION));
			
			Path temporaryDirectory = Files.createTempDirectory("deltadebugging");
			referenceVersionProject.copyEverythingTo(temporaryDirectory);
			workingAreaProject = new MavenProject(temporaryDirectory.toString());
			workingAreaProject.triggerCompilationWithTests();
			
			reflectionalInvoker.initializeOnce(arguments);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public MavenProject getReferenceProject() {
		return referenceVersionProject;
	}
	
	public MavenProject getFaultyProject() {
		return faultyVersionProject;
	}
	
	public MavenProject getWorkingAreaProject() {
		return workingAreaProject;
	}
	
	@Override
	public int test(DeltaSet set) {
		@SuppressWarnings("unchecked")
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) set.stream().collect(toList());
		
		String PATH_TO_PACKAGE = "package1";
		String SOURCE_OF_LOCALIZATION = "Example.java";
		Path examplePath = Paths.get(PATH_TO_PACKAGE, SOURCE_OF_LOCALIZATION);
		sourceCodeManipulationService.applySelectedChanges(examplePath, selectedChangeSet);
		
		return reflectionalInvoker.testSelectedChangeSet(selectedChangeSet); 
	}
}
