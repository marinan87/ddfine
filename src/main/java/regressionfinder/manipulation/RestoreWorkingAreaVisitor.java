package regressionfinder.manipulation;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;

@Component
public class RestoreWorkingAreaVisitor implements WorkingAreaManipulationVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public void visit(AffectedFile entity) {
		try {
			evaluationContext.getReferenceProject().copyToAnotherProject(evaluationContext.getWorkingAreaProject(), entity.getPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
