package regressionfinder.runner;

import org.springframework.boot.CommandLineRunner;

public class ApplicationCommandLineRunner implements CommandLineRunner {
	
	private CommandLineArgumentsInterpreter argumentsHolder;
				

	@Override
	public void run(String... args) throws Exception {
		argumentsHolder = new CommandLineArgumentsInterpreter(args);
	}
	
	public CommandLineArgumentsInterpreter getArgumentsHolder() {
		return argumentsHolder;
	}
}
