package regressionfinder.runner;

import org.apache.commons.cli.Option;

public enum CommandLineOption {
	
	REFERENCE_VERSION("r", "path to reference version", true, true),
	FAULTY_VERSION("f", "path to faulty version", true, true),
	FAILING_CLASS("cn", "fully qualified name of test class which contains failed test", true, true),
	FAILING_METHOD("mn", "name of failed test method which executes fault", true, true),
	DEVELOPMENT_MODE("dev", 
			"Used when working with real examples. If this flag is present, some context-driven optimizations will be done to speed-up the execution.", 
			false, false);

	private Option option;
	
	CommandLineOption(String name, String description, boolean isRequired, boolean hasArg) {
		Option.Builder optionBuilder = basicOptionBuilder(name, description);
		
		if (isRequired) {
			optionBuilder = optionBuilder.required();
		}
		
		if (hasArg) {
			optionBuilder = optionBuilder.hasArg();
		}
		
		this.option = optionBuilder.build();
	}

	private Option.Builder basicOptionBuilder(String argName, String description) {
		return Option.builder(argName).argName(argName).desc(description);
	}
	
	Option getOption() {
		return option;
	}
}
