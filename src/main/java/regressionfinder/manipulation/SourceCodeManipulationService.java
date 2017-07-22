package regressionfinder.manipulation;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import regressionfinder.core.EvaluationContext;


@Service
public class SourceCodeManipulationService {
	
	@Autowired
	private EvaluationContext context;

	public void applySelectedChanges(List<FileSourceCodeChange> sourceCodeChanges) {
		Map<Path, List<SourceCodeChange>> mapOfChanges = sourceCodeChanges.stream()
				.collect(toMap(
						FileSourceCodeChange::getPathToFile, 
						change -> Lists.newArrayList(change.getSourceCodeChange()),
						(a, b) -> { 
							a.addAll(b);
							return a;
						}));	
		
		mapOfChanges.entrySet().forEach(entry -> {
			try {
				Path copyOfSource = context.getWorkingAreaProject().copyFromAnotherProject(context.getReferenceProject(), entry.getKey());
				new FileManipulator(copyOfSource).applyChanges(entry.getValue());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		context.getWorkingAreaProject().triggerSimpleCompilation();
	}
}
