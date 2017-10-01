package regressionfinder.core.statistics;

import static java.lang.String.format;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static regressionfinder.runner.CommandLineOption.EXECUTION_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.model.MinimalChangeInFile;
import regressionfinder.model.TestOutcome;
import regressionfinder.runner.ApplicationCommandLineRunner;

@Service
public class StatisticsTracker {
	
	private static final String RESULTS_FILE_NAME = "results.txt";

	
	@Autowired
	private ApplicationCommandLineRunner applicationCommandLineRunner;
	
	@Value("${evaluationbase.location}")
	private String resultsBaseDirectory;
	
	private Path resultsPath;
	private int numberOfTrials, numberOfSourceCodeChanges, numberOfUnsafeSourceCodeChanges, numberOfStructuralChanges;
	private long startTime;

	
	public void initOnce() {
		startTime = System.currentTimeMillis();
		
		try {
			Path resultsDirectory = Paths.get(resultsBaseDirectory, applicationCommandLineRunner.getArgumentsHolder().getValue(EXECUTION_ID));
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
	
	public void registerNextTrial(TestOutcome outcome) {
		log(format("DD trial #%s. Outcome was: %s.", numberOfTrials + 1, outcome));
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
	
	public void logDuration(String line, long durationInMillis) {
		log(format(line, getFormattedDuration(durationInMillis)));
	}
	
	private String getFormattedDuration(long durationInMillis) {
		Duration executionDuration = Duration.of(durationInMillis, ChronoUnit.MILLIS);
		long seconds = executionDuration.getSeconds();
		return format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
	
	private void log(String line) {
		try {
			Files.write(resultsPath, line.concat("\r\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void logDetectedChanges(List<MinimalChangeInFile> sourceCodeChanges) {
		Map<ChangeType, Long> statisticsOnChangeType = sourceCodeChanges.stream()
			.map(MinimalChangeInFile::getSourceCodeChange)
			.collect(groupingBy(SourceCodeChange::getChangeType, counting()))
			.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
		log("Following changes detected:");
		log(StringUtils.join(statisticsOnChangeType.entrySet().toArray(), "\r\n"));
		
		log(format("Number of detected changes: source code chunks - %s, structural changes - %s, total - %s", 
				numberOfSourceCodeChanges, numberOfStructuralChanges, numberOfSourceCodeChanges + numberOfStructuralChanges));
		log(format("Number of changes to try after filtering out safe changes: %s", numberOfUnsafeSourceCodeChanges + numberOfStructuralChanges)); 
	}
	
	@PreDestroy
	public void logExecutionSummary() {
		log(format("Total number of DD iterations was: %s", numberOfTrials));
		logDuration("Total execution time was: %s", System.currentTimeMillis() - startTime);
		log("*****");
	}
}
