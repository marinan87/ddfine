package regressionfinder.core.manipulation;

import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;

public interface WorkingAreaManipulationVisitor {

	void visit(AffectedFile entity);
	
	void visit(AffectedStructuralEntity entity);
}
