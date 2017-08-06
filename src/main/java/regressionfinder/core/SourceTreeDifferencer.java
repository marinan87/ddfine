package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.model.FileSourceCodeChange;
import regressionfinder.model.MavenProject;
import regressionfinder.model.SourceTreeComparisonResults;

public class SourceTreeDifferencer {

	private final MavenProject referenceProject, faultyProject;
	private final SourceTreeComparisonResults comparisonResults;

	
	public SourceTreeDifferencer(MavenProject referenceProject, MavenProject faultyProject) {
		this.referenceProject = referenceProject;
		this.faultyProject = faultyProject;
		this.comparisonResults = new SourceTreeComparisonResults();
	}

	public List<FileSourceCodeChange> distillChanges() {
		scanForChangedPaths(referenceProject.getSourceDirectory());
		
		return comparisonResults.getModifiedFiles().stream()
				.flatMap(this::distillChangesForPath)
				.collect(Collectors.toList());
	}	
		
	private void scanForChangedPaths(File directoryInReferenceProject) {
		Path directoryRelativePath = referenceProject.findRelativeToSourceRoot(directoryInReferenceProject.toPath());
		File directoryInFaultyProject = faultyProject.findFile(directoryRelativePath);

		Preconditions.checkArgument(directoryInReferenceProject.isDirectory(), 
				format("%s is not a directory!", directoryInReferenceProject));
		Preconditions.checkArgument(directoryInFaultyProject.exists() && directoryInFaultyProject.isDirectory(), 
				format("Directory %s does not exist in faulty project!", directoryInFaultyProject));
		
		// TODO: implement detection of all types of changes
		/*DetectedChange
		StructuralChange

		added, removed files? fileops
		modified files - first by size, if equal - then calculate hash, if changed -> Distiller
		added, removed dirs? fileops
		*/
		
		// TODO: implement fileops for delta debugging
		// TODO: support of file renaming/moving?
		
		List<Path> javaPathsInReference = referenceProject.javaPathsInDirectory(directoryInReferenceProject);
		List<Path> javaPathsInFaulty = faultyProject.javaPathsInDirectory(directoryInFaultyProject);
		
		Collections2.filter(javaPathsInReference, Predicates.not(Predicates.in(javaPathsInFaulty))).forEach(comparisonResults::addRemovedFile);
		Collections2.filter(javaPathsInFaulty, Predicates.not(Predicates.in(javaPathsInReference))).forEach(comparisonResults::addAddedFile);
		
		Collections2.filter(javaPathsInReference, Predicates.in(javaPathsInFaulty)).stream()				
				.filter(sizeHasChanged().or(checkSumHasChanged()))
				.forEach(comparisonResults::addModifiedFile);
		
		File[] subDirectories = directoryInReferenceProject.listFiles(File::isDirectory);
		for (File subDirectory : subDirectories) {
			scanForChangedPaths(subDirectory);
		}		
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
	
	private Stream<FileSourceCodeChange> distillChangesForPath(Path pathToFile) {
		File left = referenceProject.findFile(pathToFile);
		File right = faultyProject.findFile(pathToFile);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		
		return filterOutSafeChanges(distiller.getSourceCodeChanges())
			.map(change -> new FileSourceCodeChange(pathToFile, change));
	}
	
	private Stream<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE);
	}
}
