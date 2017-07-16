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
import regressionfinder.manipulation.FileUtils;
import regressionfinder.manipulation.SourceCodeManipulator;
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
	
	
	public void initFromProvidedArguments(CommandLineArgumentsInterpreter arguments) {
		try {			
			referenceVersion = FileUtils.getPathToJavaFile(arguments.getValue(REFERENCE_VERSION), SOURCE_OF_LOCALIZATION);		
			faultyVersion = FileUtils.getPathToJavaFile(arguments.getValue(FAULTY_VERSION), SOURCE_OF_LOCALIZATION);
			workingArea = arguments.getValue(WORKING_AREA);
			testClassName = arguments.getValue(FAILING_CLASS);
			testMethodName = arguments.getValue(FAILING_METHOD);
			
			SourceCodeManipulator.copyToWorkingAreaWithoutModifications(workingArea, faultyVersion);
			reflectionalInvoker.initializeOnce(testClassName, testMethodName);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			System.exit(1);
		}
	}

	public List<URL> getWorkingAreaClassPaths() {
		try {
			List<URL> urls = new ArrayList<>();
			urls.add(new URL("file:/" + getClassPath("classes") + "/"));
			urls.add(new URL("file:/" + getClassPath("test-classes") + "/"));
			return urls;
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error while initializing working area class paths");
		}
	}
		
	private String getClassPath(String targetSubfolder) {
		return Paths.get(workingArea, "target", targetSubfolder).toString().replace("\\", "/");
	}
		
	public String getReferenceVersion() {
		return referenceVersion;
	}
	
	public String getFaultyVersion() {
		return faultyVersion;
	}
	
	@Override
	public int test(DeltaSet set) {
		@SuppressWarnings("unchecked")
		List<SourceCodeChange> selectedChangeSet = (List<SourceCodeChange>) set.stream().collect(toList());
		// TODO: sourcecodemanipulator - singleton beans
		SourceCodeManipulator.copyToWorkingAreaWithModifications(workingArea, referenceVersion, selectedChangeSet);
		
		return reflectionalInvoker.testSelectedChangeSet(selectedChangeSet); 
	}
}
