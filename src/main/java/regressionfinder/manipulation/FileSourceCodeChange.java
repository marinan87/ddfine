package regressionfinder.manipulation;

import java.nio.file.Path;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class FileSourceCodeChange {
	
	private final SourceCodeChange sourceCodeChange;
	private final Path pathToFile;
	
	public FileSourceCodeChange(SourceCodeChange sourceCodeChange, Path pathToFile) {
		this.sourceCodeChange = sourceCodeChange;
		this.pathToFile = pathToFile;
	}
	
	public SourceCodeChange getSourceCodeChange() {
		return sourceCodeChange;
	}

	public Path getPathToFile() {
		return pathToFile;
	}
	
	@Override
	public String toString() {
		return sourceCodeChange.toString();
	}
}
