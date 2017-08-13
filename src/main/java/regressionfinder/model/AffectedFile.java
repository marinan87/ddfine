package regressionfinder.model;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.renderer.RenderingVisitor;
import regressionfinder.manipulation.WorkingAreaManipulationVisitor;

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
	
	public static List<AffectedFile> fromListOfMinimalChanges(List<MinimalChangeInFile> sourceCodeChanges) {
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
	
	@Override
	public String toString() {
		return String.format("%s: %s", path, sortedChangesInFile);
	}
	
	public String render(RenderingVisitor renderingVisitor) { 
		return renderingVisitor.visit(this);
	}
	
	public void manipulate(WorkingAreaManipulationVisitor manipulationVisitor) {
		manipulationVisitor.visit(this);
	}
}
