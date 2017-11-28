package regressionfinder.model;

import java.io.IOException;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch.Patch;
import regressionfinder.core.manipulation.WorkingAreaManipulationVisitor;
import regressionfinder.core.renderer.RenderingVisitor;

public class AffectedFile extends AffectedUnit {

	private final List<Patch> sortedChangesInFile;
	
	public AffectedFile(CombinedPath path, List<Patch> changes) {
		super(path);
		sortChanges(changes);
		this.sortedChangesInFile = changes;
	}
	
	private void sortChanges(List<Patch> sourceCodeChanges) {
		sourceCodeChanges.sort((o1, o2) -> (o1.start1 - o2.start1));
	}

	public List<Patch> getChangesInFile() {
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
