package regressionfinder.model;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.renderer.RenderingVisitor;

public class AffectedFile {

	private final Path path;
	private final List<SourceCodeChange> sortedChangesInFile;
	
	private AffectedFile(Path path, List<SourceCodeChange> changes) {
		this.path = path;
		sortChanges(changes);
		this.sortedChangesInFile = changes;
	}
	
	private void sortChanges(List<SourceCodeChange> sourceCodeChanges) {
		sourceCodeChanges.sort((o1, o2) -> {
			int firstStartPosition = o1.getChangedEntity().getStartPosition();
			int secondStartPosition = o2.getChangedEntity().getStartPosition();

			if (firstStartPosition == secondStartPosition && o1 instanceof Insert && o2 instanceof Insert) {
				return ((Insert) o1).getPosition() - ((Insert) o2).getPosition();
			}
			return firstStartPosition - secondStartPosition;
		});
	}
	
	public static List<AffectedFile> fromListOfChanges(List<MinimalChangeInFile> sourceCodeChanges) {
		return sourceCodeChanges.stream()
			.collect(
				toMap(
					MinimalChangeInFile::getPathToFile, 
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

	public List<SourceCodeChange> getChangesInFile() {
		return sortedChangesInFile;
	}
	
	public String readSourceCode(MavenProject project) {
		try {
			return new String(Files.readAllBytes(project.findAbsolutePath(path)));
		} catch (IOException ioe) {
			return StringUtils.EMPTY;
		}
	}
	
	public void writeSourceCode(MavenProject project, String sourceCode) throws IOException {
		Files.write(project.findAbsolutePath(path), sourceCode.getBytes());
	}
	
	@Override
	public String toString() {
		return String.format("%s: %s", path, sortedChangesInFile);
	}
	
	public String render(RenderingVisitor renderingVisitor) { 
		return renderingVisitor.visit(this);
	}
}
