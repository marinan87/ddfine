package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;
import static regressionfinder.runner.CommandLineOption.WORKING_AREA;

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
			// TODO: create temp directory and copy all artifacts from reference version there.
			// Files.createTempDirectory("deltadebugging")
			// TODO: remove constant from CommandLineOption
			// TODO: copy everything to working directory and trigger compilation with tests.
			workingAreaProject = new MavenProject(arguments.getValue(WORKING_AREA));
			
			reflectionalInvoker.initializeOnce(arguments);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
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
		sourceCodeManipulationService.applySelectedChanges(referenceVersionProject.getJavaFile(), selectedChangeSet);
		
		return reflectionalInvoker.testSelectedChangeSet(selectedChangeSet); 
	}
}
