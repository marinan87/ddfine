package regressionfinder.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import regressionfinder.core.EvaluationContext;
import regressionfinder.core.RegressionFinder;

@SpringBootApplication
@ComponentScan(basePackageClasses = { RegressionFinder.class } )
public class ApplicationRunner {
	
	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Autowired
	private RegressionFinder handler;

	public static void main(String[] args) {
		configureHeadlessProperty();
		SpringApplication.run(ApplicationRunner.class, args);
	}
	
	private static void configureHeadlessProperty() {
        System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(false));
	}
	
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext context) {
		return args -> {
			CommandLineArgumentsInterpreter arguments = new CommandLineArgumentsInterpreter(args);
			evaluationContext.initializeOnce(arguments);
			handler.run();
		};
	}
}