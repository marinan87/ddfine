package regressionfinder.runner;

import org.apache.commons.cli.Option;

public enum CommandLineOption {
	
	REFERENCE_VERSION("r", "path to reference version"),
	FAULTY_VERSION("f", "path to faulty version"),
	// TODO: staging area - create in temp folder automatically.
	WORKING_AREA("t", "path to working area"),
	FAILING_CLASS("cn", "fully qualified name of test class which contains failed test"),
	FAILING_METHOD("mn", "name of failed test method which executes fault");

	private Option option;
	
	CommandLineOption(String name, String description) {
		this.option = mandatoryOption(name, description);
	}
	
	private Option mandatoryOption(String argName, String description) {
		return Option.builder(argName).argName(argName).desc(description)
				.required().hasArg()
				.build();
	}
	
	Option getOption() {
		return option;
	}
}
