package regressionfinder.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.deltadebugging.ddcore.tester.JUnitTester;
import org.hamcrest.SelfDescribing;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.springframework.stereotype.Component;

import regressionfinder.isolatedrunner.DeltaDebuggerTestRunner;

@Component
public class EvaluationContext {

	private static final String SOURCE_OF_LOCALIZATION = "Example.java";

	private String referenceVersion;
	private String faultyVersion;
	private String workingArea;
	
	// TODO: pass as arguments
	private String testClassName = "simple.ExampleTest";
	private String testMethodName = "tenMultipliedByTenIsOneHundred";
	
	private URL[] classPathUrls;

	
	public void initFromArgs(String[] args) {
		try {
			CommandLine cmd = parseCommandLineArguments(args);
			
			referenceVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue("r"), SOURCE_OF_LOCALIZATION);		
			faultyVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue("f"), SOURCE_OF_LOCALIZATION);
			workingArea = cmd.getOptionValue("t");
			
			classPathUrls = gatherClassPathsForIsolatedClassLoader();
		} catch (Exception e) {
			System.out.println("Exception during initialization of evaluation context");
			e.printStackTrace();
			System.exit(1);
		}
	}

	private CommandLine parseCommandLineArguments(String[] args) {
		Options options = new Options();
		options.addOption(mandatoryOption("r", "path to reference version"));
		options.addOption(mandatoryOption("f", "path to faulty version"));
		// TODO: staging area - create in temp folder automatically.
		options.addOption(mandatoryOption("t", "path to working area"));
		
		CommandLineParser parser = new DefaultParser();
		try {
			return parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Usage: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	private Option mandatoryOption(String argName, String description) {
		return Option.builder(argName).argName(argName).desc(description)
				.required().hasArg()
				.build();
	}
	
	private URL[] gatherClassPathsForIsolatedClassLoader() throws MalformedURLException {	
		List<URL> urlList = new ArrayList<>();
		// These paths are required because DeltaDebuggerTestRunner needs to find JUnit test classes inside StagingArea subfolder.
		// See implementation of DeltaDebuggerTestRunner.runTest().
		urlList.add(new URL("file:/" + getWorkingAreaClassesPath() + "/"));
		urlList.add(new URL("file:/" + getWorkingAreaTestClassesPath() + "/"));
		
		// This is required because IsolatedURLClassLoader should be able to locate DeltaDebuggerTestRunner and JUnitTestRunner classes.
		urlList.add(DeltaDebuggerTestRunner.class.getProtectionDomain().getCodeSource().getLocation());		
		urlList.add(JUnitTester.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(NoTestsRemainException.class.getProtectionDomain().getCodeSource().getLocation());
		urlList.add(SelfDescribing.class.getProtectionDomain().getCodeSource().getLocation());

		return (URL[]) urlList.toArray(new URL[0]);
	}
	
	private String getWorkingAreaClassesPath() {
		return Paths.get(getWorkingArea(), "target", "classes").toString().replace("\\", "/");
	}
	
	private String getWorkingAreaTestClassesPath() {
		return Paths.get(getWorkingArea(), "target", "test-classes").toString().replace("\\", "/");
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
	
	public String getTestClassName() {
		return testClassName;
	}
	
	public String getTestMethodName() {
		return testMethodName;
	}

	public URL[] getClassPathURLs() {
		return classPathUrls;
	}
}
