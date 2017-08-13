package regressionfinder.core.renderer;

import regressionfinder.model.AffectedFile;
import regressionfinder.model.AffectedStructuralEntity;

public interface RenderingVisitor {

	String visit(AffectedFile entity);
	
	String visit(AffectedStructuralEntity entity);
}
