package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
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

	private String testClassName;
	private String testMethodName;
	private MavenProject referenceVersionProject;
	private MavenProject faultyVersionProject;
	private MavenProject workingAreaProject;

	
	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
		
	@Autowired
	private SourceCodeManipulationService sourceCodeManipulationService;
	
	public void initFromProvidedArguments(CommandLineArgumentsInterpreter arguments) {
		try {		
			referenceVersionProject = new MavenProject(arguments.getValue(REFERENCE_VERSION));
			faultyVersionProject = new MavenProject(arguments.getValue(FAULTY_VERSION));
			// TODO: create temp directory and copy all artifacts from reference version there.
			// TODO: evaluation of failing version (obtaining original throwable should be done in the faulty folder). Compile and run test. Correct classpaths.
			// Files.createTempDirectory("regressionfinder");  automatically
			// TODO: remove constant from CommandLineOption
			workingAreaProject = new MavenProject(arguments.getValue(WORKING_AREA));
			testClassName = arguments.getValue(FAILING_CLASS);
			testMethodName = arguments.getValue(FAILING_METHOD);
			
			sourceCodeManipulationService.copyToWorkingAreaWithoutModifications(faultyVersionProject.getJavaFile().toString());
			reflectionalInvoker.initializeOnce(testClassName, testMethodName);
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
		sourceCodeManipulationService.copyToWorkingAreaWithModifications(referenceVersionProject.getJavaFile().toString(), selectedChangeSet);
		
		return reflectionalInvoker.testSelectedChangeSet(selectedChangeSet); 
	}
}
