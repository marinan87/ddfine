package regressionfinder.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import regressionfinder.core.RegressionFinder;
import regressionfinder.core.statistics.persistence.entities.Execution;
import regressionfinder.core.statistics.persistence.repository.ExecutionRepository;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = { RegressionFinder.class } )
@EnableJpaRepositories(basePackageClasses = ExecutionRepository.class)
@EntityScan(basePackageClasses = Execution.class)
public class ApplicationRunner {
	
	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";
	
	
	public static void main(String[] args) {
		configureHeadlessProperty();
		
		ApplicationContext applicationContext = SpringApplication.run(ApplicationRunner.class, args);
		RegressionFinder regressionFinder = applicationContext.getBean(RegressionFinder.class);
		regressionFinder.run();
	}
	
	private static void configureHeadlessProperty() {
        System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(false));
	}
	
	@Bean
	public ApplicationCommandLineRunner applicationCommandLineRunner() {
		return new ApplicationCommandLineRunner();
	}
	
	@Bean
	public ApplicationReadyListener applicationReadyListener() {
		return new ApplicationReadyListener();
	}
}