package regressionfinder.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import regressionfinder.core.RegressionFinder;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackageClasses = { RegressionFinder.class } )
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