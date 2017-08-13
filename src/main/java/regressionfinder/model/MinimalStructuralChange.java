package regressionfinder.model;

import java.nio.file.Path;

public class MinimalStructuralChange extends MinimalApplicableChange {
	
	private final StructuralChangeType structuralChange;

	public MinimalStructuralChange(Path pathToFile, StructuralChangeType structuralChange) {
		super(pathToFile);
		this.structuralChange = structuralChange;
	}

	public StructuralChangeType getStructuralChange() {
		return structuralChange;
	}
}
