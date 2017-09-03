package regressionfinder.model;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class MinimalChangeInFile extends MinimalApplicableChange {
	
	private final SourceCodeChange sourceCodeChange;
	
	public MinimalChangeInFile(CombinedPath pathToFile, SourceCodeChange sourceCodeChange) {
		super(pathToFile);
		this.sourceCodeChange = sourceCodeChange;
	}

	public SourceCodeChange getSourceCodeChange() {
		return sourceCodeChange;
	}
	
	@Override
	public String toString() {
		return String.format("Change of type %s in path %s\r\n", sourceCodeChange.getChangeType(), pathToFile);
	}
}
