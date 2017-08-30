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
}
