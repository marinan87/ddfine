package regressionfinder.model;

import static java.lang.String.format;

public class MinimalStructuralChange extends MinimalApplicableChange {
	
	private final StructuralChangeType structuralChange;

	public MinimalStructuralChange(CombinedPath pathToFile, StructuralChangeType structuralChange) {
		super(pathToFile);
		this.structuralChange = structuralChange;
	}

	public StructuralChangeType getStructuralChange() {
		return structuralChange;
	}
	
	@Override
	public String toString() {
		return format("Structural change of type %s in path %s\r\n", structuralChange, pathToFile);
	}
}
