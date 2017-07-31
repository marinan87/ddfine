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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

import ch.uzh.ifi.seal.changedistiller.ChangeDistiller;
import ch.uzh.ifi.seal.changedistiller.ChangeDistiller.Language;
import ch.uzh.ifi.seal.changedistiller.distilling.FileDistiller;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SignificanceLevel;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.model.FileSourceCodeChange;

@Component
public class SourceTreeDifferencer {

	@Autowired
	private EvaluationContext evaluationContext;

	public List<FileSourceCodeChange> distillChanges() {
		return scanForChangedPaths(evaluationContext.getReferenceProject().getSourceDirectory())
				.flatMap(this::distillChangesForPath)
				.collect(Collectors.toList());
	}	
		
	private Stream<Path> scanForChangedPaths(File directory) {
		Preconditions.checkArgument(directory.isDirectory(), format("%s is not a directory!", directory));
		
		File[] javaFiles = directory.listFiles(isJavaFile());

		Stream<Path> streamOfRelativePaths = Stream.of(javaFiles).map(File::toPath)
				.map(evaluationContext.getReferenceProject()::findRelativeToSourceRoot)				
				.filter(sizeHasChanged().or(checkSumHasChanged()));
		
		File[] subDirectories = directory.listFiles((FileFilter) dirName -> dirName.isDirectory());
		for (File file : subDirectories) {
			streamOfRelativePaths = Stream.concat(streamOfRelativePaths, scanForChangedPaths(file));
		}
		
		return streamOfRelativePaths;
	}

	private FileFilter isJavaFile() {
		return fileName -> fileName.isFile() && fileName.getName().endsWith(".java");
	}

	private Predicate<Path> sizeHasChanged() {
		return relativePath -> {
			try {
				return evaluationContext.getReferenceProject().size(relativePath) != evaluationContext.getFaultyProject().size(relativePath);
			} catch (IOException e) {
				return true;
			}
		};
	}

	private Predicate<Path> checkSumHasChanged() {
		return relativePath -> {
			try {
				String referenceMd5 = evaluationContext.getReferenceProject().md5Hash(relativePath);
				String faultyMd5 = evaluationContext.getFaultyProject().md5Hash(relativePath);
				return !referenceMd5.equals(faultyMd5);
			} catch (IOException e) {
				return true;
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
