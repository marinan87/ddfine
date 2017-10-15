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
import regressionfinder.core.statistics.persistence.StatisticsService;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;
import regressionfinder.model.TestOutcome;
import regressionfinder.runner.ApplicationCommandLineRunner;

@Service
public class StatisticsTracker {
	
	private static final String RESULTS_FILE_NAME = "results.txt";

	
	@Autowired
	private ApplicationCommandLineRunner applicationCommandLineRunner;
	
	@Autowired
	private StatisticsService statisticsService;
		
	@Value("${evaluationbase.location}")
	private String resultsBaseDirectory;
	
	private String executionId;
	private Path resultsPath;
	private int numberOfTrials, numberOfSourceCodeChanges, numberOfUnsafeSourceCodeChanges, numberOfStructuralChanges;
	private long startTime;
	private long[] lastTrialMetrics = new long[4];
	private int lastTrialCounter = 0;
	
	
	public void initOnce() {
		startTime = System.currentTimeMillis();
		executionId = applicationCommandLineRunner.getArgumentsHolder().getValue(EXECUTION_ID);
		
		initResultsFile();
		
		logStart();
	}

	private void initResultsFile() {
		try {
			Path resultsDirectory = Paths.get(resultsBaseDirectory, executionId);
			if (!resultsDirectory.toFile().exists()) {
				Files.createDirectory(resultsDirectory);
			}
			resultsPath = resultsDirectory.resolve(RESULTS_FILE_NAME);
			if (resultsPath.toFile().exists()) {
				Files.delete(resultsPath);
			}
			Files.createFile(resultsPath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to initialize results file.", e);
		}
	}
	
	private void logStart() {
		log("Starting the execution...");
		
		statisticsService.deleteOldExecutionIfExists(executionId);
		statisticsService.createNewExecution(executionId);
	}
	
	public void registerNextTrial(String setContent, int setSize, TestOutcome outcome) {
		log(format("DD trial #%s. Set size: %s. Outcome was: %s.", numberOfTrials + 1, setSize, outcome));
		log(format("Set content: %s", setContent));
		log(format("Timing: (prepare working area) - %s ms, (recompile working area) - %s ms, (run test) - %s ms, (restore working area) - %s ms.",
				lastTrialMetrics[0], lastTrialMetrics[1], lastTrialMetrics[2], lastTrialMetrics[3]));
		
		statisticsService.storeTrial(executionId, (numberOfTrials + 1), setContent, outcome.getNumCode(), lastTrialMetrics);
		
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
	
	public void logPhaseDuration(ExecutionPhase phase, long startTime) {
		String value = phase.displayName().concat(" completed. Took time: %s.");
		long duration = System.currentTimeMillis() - startTime;
		logDuration(value, duration);
		
		statisticsService.storePhaseExecutionTime(executionId, phase, duration);
	}
	
	public void updateLastTrialMetrics(long startTime) {
		lastTrialMetrics[lastTrialCounter++] = System.currentTimeMillis() - startTime;
		lastTrialCounter %= lastTrialMetrics.length;
	}
	
	private void logDuration(String line, long duration) {
		log(format(line, getFormattedDuration(duration)));
	}
	
	private String getFormattedDuration(long durationInMillis) {
		Duration executionDuration = Duration.of(durationInMillis, ChronoUnit.MILLIS);
		long seconds = executionDuration.getSeconds();
		return format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
	
	private void log(String line) {
		try {
			Files.write(resultsPath, line.trim().concat("\r\n").getBytes(), StandardOpenOption.APPEND);
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
		
		statisticsService.storeStructuralChanges(executionId, numberOfSourceCodeChanges);
		statisticsService.storeSourceCodeChanges(executionId, numberOfStructuralChanges);
	}
	
	public void logDeltaDebuggingChunks(List<MinimalApplicableChange> chunks) {
		int chunkNumber = 0;
		for (MinimalApplicableChange chunk : chunks) {
			log(format("[%s] %s", chunkNumber++, chunk));
		}
		
		statisticsService.storeDistilledChanges(executionId, chunks);
	}
	
	@PreDestroy
	public void logExecutionSummary() {
		log(format("Total number of DD iterations was: %s", numberOfTrials));
		statisticsService.storeDeltaDebuggingTrials(executionId, numberOfTrials);
		
		long totalDuration = System.currentTimeMillis() - startTime;
		logDuration("Total execution time was: %s", totalDuration);
		log("*****");
		
		statisticsService.storeTotalDuration(executionId, totalDuration);
	}
}
