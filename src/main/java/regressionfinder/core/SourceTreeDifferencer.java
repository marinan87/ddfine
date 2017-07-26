package regressionfinder.core;

import static java.lang.String.format;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.List;
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
		return scanForChangedSourceCodeFiles(sourceDirectory)
				.map(File::toPath)
				.map(absolutePath -> sourceDirectory.toPath().relativize(absolutePath))
				.collect(Collectors.toList());
	}
		
	private Stream<File> scanForChangedSourceCodeFiles(File sourceDirectory) {
		Preconditions.checkArgument(sourceDirectory.isDirectory(), format("%s is not a directory!", sourceDirectory));
		
		File[] javaFiles = sourceDirectory.listFiles((FileFilter) fileName -> fileName.isFile() && fileName.getName().endsWith(".java"));
		// TODO: compare the size of java files, if different - run distiller, if same - calculate hash first, then run distiller, if necessary
		Stream<File> streamOfFiles = Stream.of(javaFiles);
		
		File[] subDirectories = sourceDirectory.listFiles((FileFilter) dirName -> dirName.isDirectory());
		for (File file : subDirectories) {
			streamOfFiles = Stream.concat(streamOfFiles, scanForChangedSourceCodeFiles(file));
		}
		
		return streamOfFiles;
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
