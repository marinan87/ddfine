package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.model.MavenJavaProject;
import regressionfinder.model.MinimalApplicableChange;
import regressionfinder.model.MinimalChangeInFile;
import regressionfinder.model.MinimalStructuralChange;
import regressionfinder.model.SourceTreeComparisonResults;
import regressionfinder.model.StructuralChangeType;

public class SourceTreeDifferencer {

	private static final Path ROOT_PATH = Paths.get(StringUtils.EMPTY);
	
	private final MavenJavaProject referenceProject, faultyProject;
	private final SourceTreeComparisonResults comparisonResults;

	
	public SourceTreeDifferencer(MavenJavaProject referenceProject, MavenJavaProject faultyProject) {
		this.referenceProject = referenceProject;
		this.faultyProject = faultyProject;
		this.comparisonResults = new SourceTreeComparisonResults();
	}

	public List<MinimalApplicableChange> distillChanges() {
		scanForChangedPaths(ROOT_PATH);
		
		Stream<MinimalApplicableChange> changesStream = Stream.empty();
		changesStream = Stream.concat(changesStream, 
				comparisonResults.getModifiedFiles().stream().flatMap(this::distillChangesForPath));
		changesStream = Stream.concat(changesStream,
				comparisonResults.getRemovedFiles().stream().map(path -> new MinimalStructuralChange(path, StructuralChangeType.FILE_REMOVED)));
		changesStream = Stream.concat(changesStream,
				comparisonResults.getAddedFiles().stream().map(path -> new MinimalStructuralChange(path, StructuralChangeType.FILE_ADDED)));
		changesStream = Stream.concat(changesStream,
				comparisonResults.getRemovedPackages().stream().map(path -> new MinimalStructuralChange(path, StructuralChangeType.PACKAGE_REMOVED)));
		changesStream = Stream.concat(changesStream,
				comparisonResults.getAddedPackages().stream().map(path -> new MinimalStructuralChange(path, StructuralChangeType.PACKAGE_ADDED)));
		
		return changesStream.collect(Collectors.toList());
	}	
		
	private void scanForChangedPaths(Path relativeToSourceRoot) {
		File directoryInReferenceProject = referenceProject.findFile(relativeToSourceRoot);
		File directoryInFaultyProject = faultyProject.findFile(relativeToSourceRoot);

		Preconditions.checkArgument(directoryInReferenceProject.isDirectory(), 
				format("%s is not a directory!", relativeToSourceRoot));
		Preconditions.checkArgument(directoryInFaultyProject.exists() && directoryInFaultyProject.isDirectory(), 
				format("Directory %s does not exist in faulty project!", directoryInFaultyProject));
		
		scanForChangedJavaPaths(relativeToSourceRoot);
		scanForChangedDirs(relativeToSourceRoot);	
		// TODO: support of file renaming/moving?
	}

	private void scanForChangedJavaPaths(Path relativeToSourceRoot) {
		List<Path> javaPathsInReference = referenceProject.javaPathsInDirectory(relativeToSourceRoot);
		List<Path> javaPathsInFaulty = faultyProject.javaPathsInDirectory(relativeToSourceRoot);
		
		Collections2.filter(javaPathsInReference, Predicates.not(Predicates.in(javaPathsInFaulty))).forEach(comparisonResults::addRemovedFile);
		Collections2.filter(javaPathsInFaulty, Predicates.not(Predicates.in(javaPathsInReference))).forEach(comparisonResults::addAddedFile);
		
		Collections2.filter(javaPathsInReference, Predicates.in(javaPathsInFaulty)).stream()				
				.filter(sizeHasChanged().or(checkSumHasChanged()))
				.forEach(comparisonResults::addModifiedFile);
	}

	private Predicate<Path> sizeHasChanged() {
		return relativePath -> {
			try {
				return referenceProject.size(relativePath) != faultyProject.size(relativePath);
			} catch (IOException e) {
				return true;
			}
		};
	}

	private Predicate<Path> checkSumHasChanged() {
		return relativePath -> {
			try {
				String referenceMd5 = referenceProject.md5Hash(relativePath);
				String faultyMd5 = faultyProject.md5Hash(relativePath);
				return !referenceMd5.equals(faultyMd5);
			} catch (IOException e) {
				return true;
			}			
		};
	}

	private void scanForChangedDirs(Path relativeToSourceRoot) {
		List<Path> subDirectoriesInReference = referenceProject.subDirectoryPathsInDirectory(relativeToSourceRoot);
		List<Path> subDirectoriesInFaulty = faultyProject.subDirectoryPathsInDirectory(relativeToSourceRoot);
		
		Collections2.filter(subDirectoriesInReference, Predicates.not(Predicates.in(subDirectoriesInFaulty))).forEach(comparisonResults::addRemovedPackage);
		Collections2.filter(subDirectoriesInFaulty, Predicates.not(Predicates.in(subDirectoriesInReference))).forEach(comparisonResults::addAddedPackage);

		Collections2.filter(subDirectoriesInReference, Predicates.in(subDirectoriesInFaulty)).forEach(this::scanForChangedPaths);
	}
	
	private Stream<MinimalChangeInFile> distillChangesForPath(Path pathToFile) {
		File left = referenceProject.findFile(pathToFile);
		File right = faultyProject.findFile(pathToFile);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		
		return filterOutSafeChanges(distiller.getSourceCodeChanges())
			.map(change -> new MinimalChangeInFile(pathToFile, change));
	}
	
	private Stream<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE);
	}
}
