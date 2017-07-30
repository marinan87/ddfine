package regressionfinder.manipulation;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;

public class FileSourceCodeChange {
	
	private final SourceCodeChange sourceCodeChange;
	private final Path pathToFile;
	
	public FileSourceCodeChange(SourceCodeChange sourceCodeChange, Path pathToFile) {
		this.sourceCodeChange = sourceCodeChange;
		this.pathToFile = pathToFile;
	}
	
	public SourceCodeChange getSourceCodeChange() {
		return sourceCodeChange;
	}

	public Path getPathToFile() {
		return pathToFile;
	}
	
	@Override
	public String toString() {
		return String.format("File %s: %s", pathToFile, sourceCodeChange.toString());
	}
	
	public static Map<Path, List<SourceCodeChange>> getMapOfChanges(List<FileSourceCodeChange> sourceCodeChanges) {
		return sourceCodeChanges.stream()
				.collect(toMap(
						FileSourceCodeChange::getPathToFile, 
						change -> newArrayList(change.getSourceCodeChange()),
						(a, b) -> { 
							a.addAll(b);
							return a;
						}));
	}
}
