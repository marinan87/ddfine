package regressionfinder.core.manipulation;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import regressionfinder.core.EvaluationContext;
import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;

@Component
public class PrepareWorkingAreaVisitor implements WorkingAreaManipulationVisitor {

	@Autowired
	private EvaluationContext evaluationContext;
	
	@Override
	public void visit(AffectedFile entity) {
		try {
			new SourceCodeFileManipulator(entity, evaluationContext.getWorkingAreaProject()).applyChanges();				
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void visit(AffectedStructuralEntity entity) {
		// TODO Auto-generated method stub
	}
}
