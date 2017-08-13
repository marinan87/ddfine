package regressionfinder.core.manipulation;

import java.io.IOException;

import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;

public interface WorkingAreaManipulationVisitor {

	void visit(AffectedFile entity) throws IOException;
	
	void visit(AffectedStructuralEntity entity) throws IOException;
}
