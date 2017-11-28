package regressionfinder.model;

import static java.lang.String.format;

import name.fraser.neil.plaintext.diff_match_patch.Patch;

public class MinimalChangeInFile extends MinimalApplicableChange {
	
	private final Patch patch;
	
	public MinimalChangeInFile(CombinedPath pathToFile, Patch patch) {
		super(pathToFile);
		this.patch = patch;
	}

	public Patch getPatch() {
		return patch;
	}
	
	@Override
	public String toString() {
		return format("Change of type %s in path %s. Location in reference version: %s.\r\n", 
				getChangeTypeString(), pathToFile, patch.start1);
	}

	@Override
	public String getChangeTypeString() {
		return "UNCLASSIFIED";
	}
}
