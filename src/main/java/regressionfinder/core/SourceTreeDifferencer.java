package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

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
		Preconditions.checkArgument(directoryInReferenceProject.isDirectory(), format("%s is not a directory!", directoryInReferenceProject));
		
		// TODO:
		/*DetectedChange
		StructuralChange

		added, removed files? fileops
		modified files - first by size, if equal - then calculate hash, if changed -> Distiller
		added, removed dirs? fileops
		*/
		
		File[] javaFiles = directoryInReferenceProject.listFiles(isJavaFile());

		Stream.of(javaFiles).map(File::toPath)
				.map(referenceProject::findRelativeToSourceRoot)				
				.filter(sizeHasChanged().or(checkSumHasChanged()))
				.forEach(comparisonResults::addModifiedFile);
		
		File[] subDirectories = directoryInReferenceProject.listFiles(File::isDirectory);
		for (File subDirectory : subDirectories) {
			scanForChangedPaths(subDirectory);
		}		
	}

	private FileFilter isJavaFile() {
		return fileName -> fileName.isFile() && fileName.getName().endsWith(".java");
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
