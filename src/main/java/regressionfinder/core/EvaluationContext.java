package regressionfinder.core;

import static java.util.stream.Collectors.toList;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deltadebugging.ddcore.DeltaSet;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileUtils;
import regressionfinder.manipulation.SourceCodeManipulator;

@Component
public class EvaluationContext extends JUnitTester {

	private static final String SOURCE_OF_LOCALIZATION = "Example.java";
	
	private static final String OPTION_REFERENCE_VERSION = "r";
	private static final String OPTION_FAULTY_VERSION = "f";
	private static final String OPTION_WORKING_AREA = "t";
	private static final String OPTION_FAILING_TEST_CLASS = "cn";
	private static final String OPTION_FAILING_METHOD = "mn";
	

	private String referenceVersion;
	private String faultyVersion;
	private String workingArea;	
	private String testClassName;
	private String testMethodName;
	
	@Autowired
	private ReflectionalTestMethodInvoker reflectionalInvoker;
	
	
	public void initFromArgs(String[] args) {
		try {
			CommandLine cmd = parseCommandLineArguments(args);
			
			referenceVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue(OPTION_REFERENCE_VERSION), SOURCE_OF_LOCALIZATION);		
			faultyVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue(OPTION_FAULTY_VERSION), SOURCE_OF_LOCALIZATION);
			workingArea = cmd.getOptionValue(OPTION_WORKING_AREA);
			testClassName = cmd.getOptionValue(OPTION_FAILING_TEST_CLASS);
			testMethodName = cmd.getOptionValue(OPTION_FAILING_METHOD);
			
			SourceCodeManipulator.copyToWorkingAreaWithoutModifications(workingArea, faultyVersion);
			reflectionalInvoker.initializeOnce(testClassName, testMethodName);
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			System.exit(1);
		}
	}

	private CommandLine parseCommandLineArguments(String[] args) {
		Options options = new Options();
		options.addOption(mandatoryOption(OPTION_REFERENCE_VERSION, "path to reference version"));
		options.addOption(mandatoryOption(OPTION_FAULTY_VERSION, "path to faulty version"));
		// TODO: staging area - create in temp folder automatically.
		options.addOption(mandatoryOption(OPTION_WORKING_AREA, "path to working area"));
		options.addOption(mandatoryOption(OPTION_FAILING_TEST_CLASS, "fully qualified name of test class which contains failed test"));
		options.addOption(mandatoryOption(OPTION_FAILING_METHOD, "name of failed test method"));
		
		CommandLineParser parser = new DefaultParser();
		try {
			return parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("regressionfinder", options);
			throw new RuntimeException(e);
		}
	}

	private Option mandatoryOption(String argName, String description) {
		return Option.builder(argName).argName(argName).desc(description)
				.required().hasArg()
				.build();
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
