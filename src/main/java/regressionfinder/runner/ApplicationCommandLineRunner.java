package regressionfinder.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import regressionfinder.core.EvaluationContext;
import regressionfinder.core.StatisticsTracker;

public class ApplicationCommandLineRunner implements CommandLineRunner {
	
	@Autowired
	private EvaluationContext evaluationContext;
		
	@Autowired
	private StatisticsTracker statisticsTracker;
	

	@Override
	public void run(String... args) throws Exception {
		CommandLineArgumentsInterpreter arguments = new CommandLineArgumentsInterpreter(args);
		statisticsTracker.initializeStatistics(arguments);
		evaluationContext.initializeOnce(arguments);					
	}
}
