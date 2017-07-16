package regressionfinder.core;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

@SpringBootApplication
public class ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationRunner.class, args);
	}
	
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext context) {
		return args -> {
			SourceCodeChange[] failureInducingChanges = null;
			try {			
				RegressionFinder handler = new RegressionFinder(args);
				List<SourceCodeChange> filteredChanges = handler.extractDistilledChanges();
				failureInducingChanges = handler.runDeltaDebugging(filteredChanges);
				
				handler.applyFailureInducingChanges(failureInducingChanges);
//				highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
//			displayDoneDialog(event, failureInducingChanges);
		};
	}
}
