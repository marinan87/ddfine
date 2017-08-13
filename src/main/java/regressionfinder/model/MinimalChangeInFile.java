package regressionfinder.model;

import java.nio.file.Path;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class MinimalChangeInFile extends MinimalApplicableChange {
	
	private final SourceCodeChange sourceCodeChange;
	
	public MinimalChangeInFile(Path pathToFile, SourceCodeChange sourceCodeChange) {
		super(pathToFile);
		this.sourceCodeChange = sourceCodeChange;
	}

	public SourceCodeChange getSourceCodeChange() {
		return sourceCodeChange;
	}
}
