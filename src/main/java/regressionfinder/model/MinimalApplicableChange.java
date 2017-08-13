package regressionfinder.model;

import java.nio.file.Path;

public abstract class MinimalApplicableChange {

	private final Path pathToFile;

	public MinimalApplicableChange(Path pathToFile) {
		this.pathToFile = pathToFile;
	}
	
	public Path getPathToFile() {
		return pathToFile;
	}
}
