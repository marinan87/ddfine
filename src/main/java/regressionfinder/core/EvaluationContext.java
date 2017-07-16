package regressionfinder.core;

import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.stereotype.Component;

@Component
public class EvaluationContext {

	private String referenceVersion;
	private String faultyVersion;
	private String workingArea;
	
	// TODO: pass as arguments
	private String testClassName = "simple.ExampleTest";
	private String testMethodName = "tenMultipliedByTenIsOneHundred";
	
	private static final String SOURCE_OF_LOCALIZATION = "Example.java";
	
	
	public void initFromArgs(String[] args) {		
		Options options = new Options();
		options.addOption(Option.builder("r")
				.argName("-r")
				.required()
				.hasArg()
				.desc("path to reference version")
				.build());
		options.addOption(Option.builder("f")
				.argName("-f")
				.required()
				.hasArg()
				.desc("path to faulty version")
				.build());
		options.addOption(Option.builder("t")
				.argName("-t")
				.required()
				.hasArg()
				.desc("path to working area")
				.build());
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Usage: " + e.getMessage());
			System.exit(1);
		}
		
		
		// TODO: staging area - create in temp folder automatically.
			
		referenceVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue("r"), SOURCE_OF_LOCALIZATION);		
		faultyVersion = FileUtils.getPathToJavaFile(cmd.getOptionValue("f"), SOURCE_OF_LOCALIZATION);
		workingArea = cmd.getOptionValue("t");
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
	
	public String getWorkingAreaClassesPath() {
		return Paths.get(getWorkingArea(), "target", "classes").toString().replace("\\", "/");
	}
	
	public String getWorkingAreaTestClassesPath() {
		return Paths.get(getWorkingArea(), "target", "test-classes").toString().replace("\\", "/");
	}
}
