package regressionfinder.model;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.stream.Stream;

import regressionfinder.core.manipulation.WorkingAreaManipulationVisitor;
import regressionfinder.core.renderer.RenderingVisitor;

public abstract class AffectedUnit {

	protected final CombinedPath path;
	
	protected AffectedUnit(CombinedPath path) {
		this.path = path;
	}

	public static List<AffectedUnit> fromListOfMinimalChanges(List<MinimalApplicableChange> minimalChanges) {
		Stream<AffectedFile> streamOfAffectedFiles = extractAffectedFiles(minimalChanges);
		Stream<AffectedStructuralEntity> streamOfAffectedStructuralEntities = extractAffectedStructuralEntities(minimalChanges);
		
		return Stream.concat(streamOfAffectedFiles, streamOfAffectedStructuralEntities).collect(toList());
	}

	private static Stream<AffectedFile> extractAffectedFiles(List<MinimalApplicableChange> minimalChanges) {
		return minimalChanges.stream()
			.filter(change -> change instanceof MinimalChangeInFile)
			.map(change -> (MinimalChangeInFile) change)
			.collect(
				toMap(
					MinimalChangeInFile::getPathToFile, 
					change -> newArrayList(change.getSourceCodeChange()),
					(a, b) -> { 
						a.addAll(b);
						return a;
					}))
			.entrySet().stream()
			.map(entry -> new AffectedFile(entry.getKey(), entry.getValue()));
	}
	
	private static Stream<AffectedStructuralEntity> extractAffectedStructuralEntities(List<MinimalApplicableChange> minimalChanges) {
		return minimalChanges.stream()
			.filter(change -> change instanceof MinimalStructuralChange)
			.map(change -> (MinimalStructuralChange) change)
			.map(change -> new AffectedStructuralEntity(change.getPathToFile(), change.getStructuralChange()));
	}
	
	public CombinedPath getPath() {
		return path;
	}
	
	public abstract String render(RenderingVisitor renderingVisitor);
	
	public abstract void manipulate(WorkingAreaManipulationVisitor manipulationVisitor);
}
