package regressionfinder.core;

import static java.lang.String.format;
import static regressionfinder.runner.CommandLineOption.EXECUTION_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Service
public class StatisticsTracker {
	
	private static final String RESULTS_FILE_NAME = "results.txt";

	
	@Value("${evaluationbase.location}")
	private String resultsBaseDirectory;
	
	private Path resultsPath;
	
	private int trials;

	
	public void incrementNumberOfTrials() {
		trials++;
	}
	
	public int getNumberOfTrials() {
		return trials;
	}

	public void initializeResults(CommandLineArgumentsInterpreter arguments) {
		try {
			Path resultsDirectory = Paths.get(resultsBaseDirectory, arguments.getValue(EXECUTION_ID));
			if (!resultsDirectory.toFile().exists()) {
				Files.createDirectory(resultsDirectory);
			}
			resultsPath = resultsDirectory.resolve(RESULTS_FILE_NAME);
			if (!resultsPath.toFile().exists()) {
				Files.createFile(resultsPath);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize results file.", e);
		}
		
		logResult("Starting the execution...");
	}
	
	public void logResult(String line) {
		try {
			Files.write(resultsPath, line.concat("\r\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logSummary() {
		logResult(format("Total number of DD iterations was: %s", trials));
	}
}
