package regressionfinder.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class SourceTreeComparisonResults {

	private List<Path> modifiedFiles = new ArrayList<>();
	private List<Path> removedFiles = new ArrayList<>();
	private List<Path> addedFiles = new ArrayList<>();
	private List<Path> removedPackages = new ArrayList<>();
	private List<Path> addedPackages = new ArrayList<>();
	
	public void addModifiedFile(Path path) {
		modifiedFiles.add(path);
	}
	
	public void addRemovedFile(Path path) {
		removedFiles.add(path);
	}
	
	public void addAddedFile(Path path) {
		addedFiles.add(path);
	}
	
	public void addRemovedPackage(Path path) {
		removedPackages.add(path);
	}
	
	public void addAddedPackage(Path path) {
		addedPackages.add(path);
	}
		
	public List<Path> getModifiedFiles() {
		return ImmutableList.copyOf(modifiedFiles);
	}
	
	public List<Path> getRemovedFiles() {
		return ImmutableList.copyOf(removedFiles);
	}
	
	public List<Path> getAddedFiles() {
		return ImmutableList.copyOf(addedFiles);
	}
	
	public List<Path> getRemovedPackages() {
		return ImmutableList.copyOf(removedPackages);
	}
	
	public List<Path> getAddedPackages() {
		return ImmutableList.copyOf(addedPackages);
	}
}
