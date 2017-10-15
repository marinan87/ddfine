package regressionfinder.runner.dbmanager;

import javax.annotation.PostConstruct;

import org.hsqldb.util.DatabaseManagerSwing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatabaseManagerRunner {

	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

	
	@Value("${spring.datasource.url}")
	private String databaseUrl;
	
	@Value("${spring.datasource.username}")
	private String databaseUser;
	
	@Value("${spring.datasource.password}")
	private String databasePassword;


	public static void main(String[] args) {
		configureHeadlessProperty();

		SpringApplication.run(DatabaseManagerRunner.class, args);
	}
	
	private static void configureHeadlessProperty() {
        System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(false));
	}
	
	@PostConstruct
	public void startDbManager() {
		DatabaseManagerSwing.main(new String[] { "--url", databaseUrl, "--user", databaseUser, "--password", databasePassword });
	}
}
