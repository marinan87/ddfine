package regressionfinder.model;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.renderer.RenderingVisitor;
import regressionfinder.manipulation.FileManipulator;

public class AffectedFile {

	private final Path path;
	private final List<SourceCodeChange> failureInducingChanges;
	
	private AffectedFile(Path path, List<SourceCodeChange> changes) {
		this.path = path;
		FileManipulator.sortChanges(changes);
		this.failureInducingChanges = changes;
	}
	
	public static List<AffectedFile> fromListOfChanges(List<FileSourceCodeChange> sourceCodeChanges) {
		return sourceCodeChanges.stream()
			.collect(
				toMap(
					FileSourceCodeChange::getPathToFile, 
					change -> newArrayList(change.getSourceCodeChange()),
					(a, b) -> { 
						a.addAll(b);
						return a;
					}))
			.entrySet().stream()
			.map(entry -> new AffectedFile(entry.getKey(), entry.getValue()))
			.collect(toList());
	}

	public Path getPath() {
		return path;
	}

	public List<SourceCodeChange> getFailureInducingChanges() {
		return failureInducingChanges;
	}
	
	public String readSourceCode(MavenProject project) {
		try {
			return new String(Files.readAllBytes(project.findAbsolutePath(path)));
		} catch (IOException ioe) {
			return StringUtils.EMPTY;
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s", path, failureInducingChanges);
	}
	
	public String render(RenderingVisitor renderingVisitor) { 
		return renderingVisitor.visit(this);
	}
}
