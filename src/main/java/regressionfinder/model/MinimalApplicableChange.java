package regressionfinder.model;

public abstract class MinimalApplicableChange {

	private final CombinedPath pathToFile;

	public MinimalApplicableChange(CombinedPath pathToFile) {
		this.pathToFile = pathToFile;
	}
	
	public CombinedPath getPathToFile() {
		return pathToFile;
	}
}
