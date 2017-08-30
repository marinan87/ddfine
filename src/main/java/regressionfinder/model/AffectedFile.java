package regressionfinder.model;

import java.io.IOException;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.manipulation.WorkingAreaManipulationVisitor;
import regressionfinder.core.renderer.RenderingVisitor;

public class AffectedFile extends AffectedEntity {

	private final List<SourceCodeChange> sortedChangesInFile;
	
	public AffectedFile(CombinedPath path, List<SourceCodeChange> changes) {
		super(path);
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
		try {
			manipulationVisitor.visit(this);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
