package regressionfinder.model;

public class MinimalStructuralChange extends MinimalApplicableChange {
	
	private final StructuralChangeType structuralChange;

	public MinimalStructuralChange(CombinedPath pathToFile, StructuralChangeType structuralChange) {
		super(pathToFile);
		this.structuralChange = structuralChange;
	}

	public StructuralChangeType getStructuralChange() {
		return structuralChange;
	}
}
