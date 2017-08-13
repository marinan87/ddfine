package regressionfinder.model;

import java.nio.file.Path;

import regressionfinder.core.manipulation.WorkingAreaManipulationVisitor;
import regressionfinder.core.renderer.RenderingVisitor;

public class AffectedStructuralEntity extends AffectedEntity {

	private final StructuralChangeType structuralChangeType;
	
	public AffectedStructuralEntity(Path path, StructuralChangeType structuralChangeType) {
		super(path);
		this.structuralChangeType = structuralChangeType;
	}

	@Override
	public String render(RenderingVisitor renderingVisitor) {
		return renderingVisitor.visit(this);
	}

	@Override
	public void manipulate(WorkingAreaManipulationVisitor manipulationVisitor) {
		manipulationVisitor.visit(this);
	}
	
	public StructuralChangeType getStructuralChangeType() {
		return structuralChangeType;
	}
}
