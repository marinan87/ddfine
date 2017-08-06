package regressionfinder.core;

import regressionfinder.model.AffectedFile;

public interface RenderingVisitor {

	String visit(AffectedFile entity);
}
