package regressionfinder.runner;

import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineArgumentsInterpreter {
	
	private final CommandLine commandLine;
	
	public CommandLineArgumentsInterpreter(String[] args) {
		Options options = new Options();
		Stream.of(CommandLineOption.values()).map(CommandLineOption::getOption).forEach(options::addOption);
		
		CommandLineParser parser = new DefaultParser();
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			System.out.println("Error: " + e.getMessage());
			new HelpFormatter().printHelp("regressionfinder", options);
			throw new RuntimeException(e);
		}
	}
	
	public String getValue(CommandLineOption option) {
		return commandLine.getOptionValue(option.getOption().getArgName());
	}
}
