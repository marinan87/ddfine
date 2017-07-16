package regressionfinder.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApplicationRunner {
	
	@Autowired
	private EvaluationContext evaluationContext;
	
	@Autowired
	private RegressionFinder handler;

	public static void main(String[] args) {
		SpringApplication.run(ApplicationRunner.class, args);
	}
	
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext context) {
		return args -> {
			evaluationContext.initFromArgs(args);
			handler.run();
		};
	}
}