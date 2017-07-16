package regressionfinder.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

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
			
			List<SourceCodeChange> filteredChanges = handler.extractDistilledChanges();
			SourceCodeChange[] failureInducingChanges = handler.runDeltaDebugging(filteredChanges);
			
			//only inside src folders! expect standard Maven structure
			//
			//added, removed files? fileops
			//modified files - first by size, if equal - then calculate hash, if changed -> Distiller
			//added, removed dirs? fileops
			//then folders with same name recursively. Repeat in each dir. 
			//
			//Tree structure  (Guava?)
			//
			//
			//0) Diff between two versions - Git mode.
			//1) Textual diff first, compare only changed files. 
			//2) Make work with a set of files. Very simple example with only one change (applying operation is supported)
			//3) Evaluate only changed files via ChangeDistiller.
			
			handler.applyFailureInducingChanges(failureInducingChanges);
//			highlightFailureInducingChangesInEditor(regressionCU, failureInducingChanges);
//			displayDoneDialog(event, failureInducingChanges);
		};
	}
}