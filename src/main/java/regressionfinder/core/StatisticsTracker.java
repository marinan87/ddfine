package regressionfinder.core;

import static java.lang.String.format;
import static regressionfinder.runner.CommandLineOption.EXECUTION_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import regressionfinder.runner.CommandLineArgumentsInterpreter;

@Service
public class StatisticsTracker {
	
	private static final String RESULTS_FILE_NAME = "results.txt";

	
	@Value("${evaluationbase.location}")
	private String resultsBaseDirectory;
	
	private Path resultsPath;
	private int numberOfTrials, numberOfSourceCodeChanges, numberOfUnsafeSourceCodeChanges, numberOfStructuralChanges;
	private long startTime;

	
	public void incrementNumberOfTrials() {
		numberOfTrials++;
	}

	public void incrementNumberOfSourceCodeChangesBySize(int numberOfChanges) {
		numberOfSourceCodeChanges += numberOfChanges;
	}
	
	public void incrementNumberOfUnsafeSourceCodeChanges() {
		numberOfUnsafeSourceCodeChanges++;
	}
	
	public void incrementNumberOfStructuralChanges() {
		numberOfStructuralChanges++;
	}
		
	public void initializeStatistics(CommandLineArgumentsInterpreter arguments) {
		startTime = System.currentTimeMillis();
		
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
		
		log("Starting the execution...");
	}
	
	public void log(String line) {
		try {
			Files.write(resultsPath, line.concat("\r\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logDetectedChanges() {
		log(format("Number of detected changes: source code chunks - %s, structural changes - %s", 
				numberOfSourceCodeChanges, numberOfStructuralChanges));
		log(format("Number of changes to try after filtering out safe changes: %s", numberOfUnsafeSourceCodeChanges + numberOfStructuralChanges)); 
	}
	
	public void logExecutionSummary() {
		log(format("Total number of DD iterations was: %s", numberOfTrials));
		log(format("Total execution time was: %s", getFormattedDuration()));
		log("*****");
	}
	
	private String getFormattedDuration() {
		Duration executionDuration = Duration.of(System.currentTimeMillis() - startTime, ChronoUnit.MILLIS);
		long seconds = executionDuration.getSeconds();
		return format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
}
