package regressionfinder.model;

import java.nio.file.Path;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class FileSourceCodeChange {
	
	private final Path pathToFile;
	private final SourceCodeChange sourceCodeChange;
	
	public FileSourceCodeChange(Path pathToFile, SourceCodeChange sourceCodeChange) {
		this.pathToFile = pathToFile;
		this.sourceCodeChange = sourceCodeChange;
	}

	public Path getPathToFile() {
		return pathToFile;
	}
	
	public SourceCodeChange getSourceCodeChange() {
		return sourceCodeChange;
	}
}
