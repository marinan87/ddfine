package regressionfinder.model;

import static java.lang.String.format;

import java.nio.file.Path;

public class CombinedPath {

	private final Path pathToModule, pathToResource;
	
	public CombinedPath(Path pathToModule, Path pathToResource) {
		this.pathToModule = pathToModule;
		this.pathToResource = pathToResource;
	}

	public Path getPathToModule() {
		return pathToModule;
	}

	public Path getPathToResource() {
		return pathToResource;
	}
	
	@Override
	public String toString() {
		return format("Module: %s, path: %s", pathToModule, pathToResource);
	}
}
