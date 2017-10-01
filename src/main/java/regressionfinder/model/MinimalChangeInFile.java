package regressionfinder.model;

import static java.lang.String.format;

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
		return format("Change of type %s in path %s\r\n", sourceCodeChange.getChangeType(), pathToFile);
	}
}
