package regressionfinder.core.renderer;

import regressionfinder.model.AffectedFile;

public interface RenderingVisitor {

	String visit(AffectedFile entity);
}
