package regressionfinder.model;

import java.io.IOException;
import java.nio.file.Path;

import regressionfinder.core.manipulation.WorkingAreaManipulationVisitor;
import regressionfinder.core.renderer.RenderingVisitor;

public class AffectedStructuralEntity extends AffectedEntity {

	private final StructuralChangeType structuralChangeType;
	
	public AffectedStructuralEntity(Path path, StructuralChangeType structuralChangeType) {
		super(path);
		this.structuralChangeType = structuralChangeType;
	}

	public StructuralChangeType getStructuralChangeType() {
		return structuralChangeType;
	}

	@Override
	public String render(RenderingVisitor renderingVisitor) {
		return renderingVisitor.visit(this);
	}

	@Override
	public void manipulate(WorkingAreaManipulationVisitor manipulationVisitor) {
		try {
			manipulationVisitor.visit(this);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
