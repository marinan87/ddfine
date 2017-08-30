package regressionfinder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class SourceTreeComparisonResults {

	private Path currentProjectRelativePath;
	private List<CombinedPath> modifiedFiles = new ArrayList<>();
	private List<CombinedPath> removedFiles = new ArrayList<>();
	private List<CombinedPath> addedFiles = new ArrayList<>();
	private List<CombinedPath> removedPackages = new ArrayList<>();
	private List<CombinedPath> addedPackages = new ArrayList<>();
	
	public void setCurrentProjectRelativePath(Path path) {
		this.currentProjectRelativePath = path;
	}
	
	public void addModifiedFile(Path path) {
		modifiedFiles.add(new CombinedPath(currentProjectRelativePath, path));
	}
	
	public void addRemovedFile(Path path) {
		removedFiles.add(new CombinedPath(currentProjectRelativePath, path));
	}
	
	public void addAddedFile(Path path) {
		addedFiles.add(new CombinedPath(currentProjectRelativePath, path));
	}
	
	public void addRemovedPackage(Path path) {
		removedPackages.add(new CombinedPath(currentProjectRelativePath, path));
	}
	
	public void addAddedPackage(Path path) {
		addedPackages.add(new CombinedPath(currentProjectRelativePath, path));
	}
		
	public List<CombinedPath> getModifiedFiles() {
		return ImmutableList.copyOf(modifiedFiles);
	}
	
	public List<CombinedPath> getRemovedFiles() {
		return ImmutableList.copyOf(removedFiles);
	}
	
	public List<CombinedPath> getAddedFiles() {
		return ImmutableList.copyOf(addedFiles);
	}
	
	public List<CombinedPath> getRemovedPackages() {
		return ImmutableList.copyOf(removedPackages);
	}
	
	public List<CombinedPath> getAddedPackages() {
		return ImmutableList.copyOf(addedPackages);
	}
}
