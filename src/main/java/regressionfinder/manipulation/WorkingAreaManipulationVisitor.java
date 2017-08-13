package regressionfinder.manipulation;

import regressionfinder.model.AffectedFile;

public interface WorkingAreaManipulationVisitor {

	void visit(AffectedFile entity);
}
