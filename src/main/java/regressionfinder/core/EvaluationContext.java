package regressionfinder.core;

import static java.util.stream.Collectors.toList;
import static regressionfinder.runner.CommandLineOption.FAILING_CLASS;
import static regressionfinder.runner.CommandLineOption.FAILING_METHOD;
import static regressionfinder.runner.CommandLineOption.FAULTY_VERSION;
import static regressionfinder.runner.CommandLineOption.REFERENCE_VERSION;
import static regressionfinder.runner.CommandLineOption.WORKING_AREA;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileSystemService;
import regressionfinder.manipulation.SourceCodeManipulationService;
import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Component
public class EvaluationContext extends JUnitTester {

	private static final String SOURCE_OF_LOCALIZATION = "Example.java";

	private String referenceVersion;
	private String faultyVersion;
	private String workingArea;	
	private String testClassName;
	private String testMethodName;
	
	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
	
	@Autowired
	private FileSystemService fileService;
	
	@Autowired
	private SourceCodeManipulationService sourceCodeManipulationService;
	
	public void initFromProvidedArguments(CommandLineArgumentsInterpreter arguments) {
		try {			
			referenceVersion = fileService.getPathToJavaFile(arguments.getValue(REFERENCE_VERSION), SOURCE_OF_LOCALIZATION);		
			faultyVersion = fileService.getPathToJavaFile(arguments.getValue(FAULTY_VERSION), SOURCE_OF_LOCALIZATION);
			workingArea = arguments.getValue(WORKING_AREA);
			testClassName = arguments.getValue(FAILING_CLASS);
			testMethodName = arguments.getValue(FAILING_METHOD);
			
			sourceCodeManipulationService.copyToWorkingAreaWithoutModifications(faultyVersion);
			reflectionalInvoker.initializeOnce(testClassName, testMethodName);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			System.exit(1);
		}
	}

	public List<URL> getWorkingAreaClassPaths() {
		try {
			List<URL> urls = new ArrayList<>();
			urls.add(new URL("file:/" + getWorkingAreaClassPath("classes") + "/"));
			urls.add(new URL("file:/" + getWorkingAreaClassPath("test-classes") + "/"));
			return urls;
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error while initializing working area class paths");
		}
	}
		
	private String getWorkingAreaClassPath(String targetSubfolder) {
		return Paths.get(workingArea, "target", targetSubfolder).toString().replace("\\", "/");
	}
		
	public String getReferenceVersion() {
		return referenceVersion;
	}
	
	public String getFaultyVersion() {
		return faultyVersion;
	}
	
	public String getWorkingArea() {
		return workingArea;
	}
	
	@Override
	public int test(DeltaSet set) {
		@SuppressWarnings("unchecked")
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) set.stream().collect(toList());
		sourceCodeManipulationService.copyToWorkingAreaWithModifications(referenceVersion, selectedChangeSet);
		
		return reflectionalInvoker.testSelectedChangeSet(selectedChangeSet); 
	}
}
