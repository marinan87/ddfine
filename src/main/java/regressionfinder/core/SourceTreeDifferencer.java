package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.manipulation.FileSourceCodeChange;

@Component
public class SourceTreeDifferencer {

	@Autowired
	private EvaluationContext evaluationContext;

	public List<FileSourceCodeChange> distillChanges() {
		return scanForChangedPaths().stream()
				.flatMap(this::distillChangesForPath)
				.collect(Collectors.toList());
	}	
	
	private List<Path> scanForChangedPaths() {
		File sourceDirectory = evaluationContext.getReferenceProject().getSourceDirectory();
		return scanForChangedSourceCodeFiles(sourceDirectory).collect(Collectors.toList());
	}
		
	private Stream<Path> scanForChangedSourceCodeFiles(File directory) {
		Preconditions.checkArgument(directory.isDirectory(), format("%s is not a directory!", directory));
		
		File[] javaFiles = directory.listFiles((FileFilter) fileName -> fileName.isFile() && fileName.getName().endsWith(".java"));

		File referenceSourceDirectory = evaluationContext.getReferenceProject().getSourceDirectory();
		Stream<Path> streamOfRelativePaths = Stream.of(javaFiles)
				.map(File::toPath)
				.filter(sizeHasChanged(referenceSourceDirectory))
				.map(absolutePath -> referenceSourceDirectory.toPath().relativize(absolutePath));				
		
		File[] subDirectories = directory.listFiles((FileFilter) dirName -> dirName.isDirectory());
		for (File file : subDirectories) {
			streamOfRelativePaths = Stream.concat(streamOfRelativePaths, scanForChangedSourceCodeFiles(file));
		}
		
		return streamOfRelativePaths;
	}

	private Predicate<? super Path> sizeHasChanged(File referenceSourceDirectory) {
		return absoluteReferencePath -> {
			Path relativePath = referenceSourceDirectory.toPath().relativize(absoluteReferencePath);
			try {
				return Files.size(absoluteReferencePath) != Files.size(evaluationContext.getFaultyProject().findAbsolutePath(relativePath));
				// TODO: compare the size of java files, if same - compare by hash
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
	}

	private Stream<FileSourceCodeChange> distillChangesForPath(Path pathToFile) {
		File left = evaluationContext.getReferenceProject().findFile(pathToFile);
		File right = evaluationContext.getFaultyProject().findFile(pathToFile);
		
		FileDistiller distiller = ChangeDistiller.createFileDistiller(Language.JAVA);
		distiller.extractClassifiedSourceCodeChanges(left, right);
		
		return filterOutSafeChanges(distiller.getSourceCodeChanges())
			.map(change -> new FileSourceCodeChange(change, pathToFile));
	}
	
	private Stream<SourceCodeChange> filterOutSafeChanges(List<SourceCodeChange> allChanges) {
		return allChanges.stream()
				.filter(change -> change.getChangeType().getSignificance() != SignificanceLevel.NONE);
	}
}
